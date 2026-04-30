#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
YICES_SRC="$(cygpath -u "${YICES_SRC:?set YICES_SRC}")"
YICES_PREFIX="${YICES_PREFIX:?set YICES_PREFIX}"
JAVA_HOME_UNIX="$(cygpath -u "${JAVA_HOME:?set JAVA_HOME}")"
JAVAC="${JAVA_HOME_UNIX}/bin/javac"
JAVA="${JAVA_HOME_UNIX}/bin/java"
JAR="${JAVA_HOME_UNIX}/bin/jar"

win_path() {
  cygpath -w "$1"
}

win_path_list() {
  cygpath -wp "$1"
}

build_gmp() {
  mkdir -p /tools/dynamic_gmp /tools/static_gmp
  pushd /tools >/dev/null

  local archive=gmp-6.3.0.tar.xz
  if [[ ! -f "${archive}" ]]; then
    curl -fL --retry 5 --retry-delay 2 --connect-timeout 20 --max-time 600 \
      https://ftp.gnu.org/gnu/gmp/${archive} -o "${archive}" || \
    wget --tries=5 --timeout=20 -O "${archive}" https://ftp.gnu.org/gnu/gmp/${archive} || \
    curl -fL --retry 5 --retry-delay 2 --connect-timeout 20 --max-time 600 \
      https://mirrors.kernel.org/gnu/gmp/${archive} -o "${archive}" || \
    wget --tries=5 --timeout=20 -O "${archive}" https://mirrors.kernel.org/gnu/gmp/${archive} || \
    curl -fL --retry 5 --retry-delay 2 --connect-timeout 20 --max-time 600 \
      https://gmplib.org/download/gmp/${archive} -o "${archive}" || \
    wget --tries=5 --timeout=20 -O "${archive}" https://gmplib.org/download/gmp/${archive}
  fi

  rm -rf gmp-6.3.0
  tar xf "${archive}"
  cd gmp-6.3.0
  ./configure --host=x86_64-w64-mingw32 --build=i686-pc-cygwin --enable-cxx --enable-shared --disable-static --prefix=/tools/dynamic_gmp
  make
  make install
  make clean
  ./configure --host=x86_64-w64-mingw32 --build=i686-pc-cygwin --enable-cxx --enable-static --disable-shared --prefix=/tools/static_gmp
  make
  make install

  popd >/dev/null
}

build_yices() {
  pushd "${YICES_SRC}" >/dev/null
  autoconf
  ./configure \
    --host=x86_64-w64-mingw32 \
    --enable-thread-safety \
    CPPFLAGS="-I/tools/dynamic_gmp/include" \
    LDFLAGS="-L/tools/dynamic_gmp/lib" \
    --with-static-gmp=/tools/static_gmp/lib/libgmp.a \
    --with-static-gmp-include-dir=/tools/static_gmp/include
  export LD_LIBRARY_PATH="/usr/local/lib/:${LD_LIBRARY_PATH:-}"
  make OPTION=mingw64 MODE=release dist

  local dist_dir
  dist_dir="$(find build -type d -name dist | head -n 1)"
  if [[ -z "${dist_dir}" ]]; then
    echo "failed to locate Yices dist directory" >&2
    exit 1
  fi

  rm -rf "${YICES_PREFIX}"
  mkdir -p "${YICES_PREFIX}"
  cp -R "${dist_dir}/." "${YICES_PREFIX}/"
  popd >/dev/null
}

build_java_bindings() {
  mkdir -p "${REPO_ROOT}/build/classes" "${REPO_ROOT}/build/test_classes" "${REPO_ROOT}/dist/lib"

  pushd "${REPO_ROOT}" >/dev/null
  find src/main/java/com/sri/yices -name '*.java' | sort > build/main-sources.txt
  "${JAVAC}" \
    -d "$(win_path "${REPO_ROOT}/build/classes")" \
    -h "$(win_path "${REPO_ROOT}/src/main/java/com/sri/yices")" \
    @"build/main-sources.txt"

  x86_64-w64-mingw32-g++ \
    -I "${JAVA_HOME_UNIX}/include" \
    -I "${JAVA_HOME_UNIX}/include/win32" \
    -I "${YICES_PREFIX}/include" \
    -I /tools/dynamic_gmp/include \
    -g -Wall -fpermissive \
    -c src/main/java/com/sri/yices/yicesJNIforWindows.cpp \
    -o build/yicesJNIforWindows.o

  x86_64-w64-mingw32-g++ \
    -shared \
    -o dist/lib/yices2java.dll \
    build/yicesJNIforWindows.o \
    -L "${YICES_PREFIX}/lib" \
    -L /tools/dynamic_gmp/lib \
    -lyices -lgmp

  "${JAR}" -cvfm \
    "$(win_path "${REPO_ROOT}/dist/lib/yices.jar")" \
    "$(win_path "${REPO_ROOT}/MANIFEST.txt")" \
    -C "$(win_path "${REPO_ROOT}/build/classes")" .
  popd >/dev/null
}

stage_runtime_dlls() {
  cp "${YICES_PREFIX}/bin/libyices.dll" "${REPO_ROOT}/dist/lib/"
  cp /tools/dynamic_gmp/bin/libgmp-10.dll "${REPO_ROOT}/dist/lib/"
  cp /usr/x86_64-w64-mingw32/sys-root/mingw/bin/libstdc++-6.dll "${REPO_ROOT}/dist/lib/"
  cp /usr/x86_64-w64-mingw32/sys-root/mingw/bin/libgcc_s_seh-1.dll "${REPO_ROOT}/dist/lib/"
  cp /usr/x86_64-w64-mingw32/sys-root/mingw/bin/libwinpthread-1.dll "${REPO_ROOT}/dist/lib/"
}

run_tests() {
  pushd "${REPO_ROOT}" >/dev/null
  find src/test/java -name '*.java' | sort > build/test-sources.txt

  local compile_cp
  compile_cp="$(win_path_list "${REPO_ROOT}/dist/lib/yices.jar:${REPO_ROOT}/lib/junit-4.12.jar:${REPO_ROOT}/lib/hamcrest-core-1.3.jar")"
  "${JAVAC}" \
    -cp "${compile_cp}" \
    -d "$(win_path "${REPO_ROOT}/build/test_classes")" \
    @"build/test-sources.txt"

  local runtime_cp runtime_path
  runtime_cp="$(win_path_list "${REPO_ROOT}/build/test_classes:${REPO_ROOT}/dist/lib/yices.jar:${REPO_ROOT}/lib/junit-4.12.jar:${REPO_ROOT}/lib/hamcrest-core-1.3.jar")"
  runtime_path="$(win_path_list "${REPO_ROOT}/dist/lib:/tools/dynamic_gmp/bin:/usr/x86_64-w64-mingw32/sys-root/mingw/bin")"

  env PATH="${runtime_path}" \
    "${JAVA}" \
    "-Djava.library.path=$(win_path "${REPO_ROOT}/dist/lib")" \
    -cp "${runtime_cp}" \
    org.junit.runner.JUnitCore \
    com.sri.yices.TestBigRationals \
    com.sri.yices.TestConstructor \
    com.sri.yices.TestContext \
    com.sri.yices.TestStatus \
    com.sri.yices.TestTypes \
    com.sri.yices.TestYices \
    com.sri.yices.TestModels \
    com.sri.yices.TestTermComponents
  popd >/dev/null
}

build_gmp
build_yices
build_java_bindings
stage_runtime_dlls
run_tests
