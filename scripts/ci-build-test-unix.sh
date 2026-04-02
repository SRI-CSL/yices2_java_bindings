#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
YICES_SRC="${YICES_SRC:?set YICES_SRC}"
YICES_PREFIX="${YICES_PREFIX:?set YICES_PREFIX}"

configure_brew_env() {
  if [[ "$(uname)" != "Darwin" ]]; then
    return
  fi

  local brew_prefix
  if [[ -d /opt/homebrew ]]; then
    brew_prefix=/opt/homebrew
  else
    brew_prefix=/usr/local
  fi

  export CPPFLAGS="-I${brew_prefix}/include ${CPPFLAGS:-}"
  export LDFLAGS="-L${brew_prefix}/lib ${LDFLAGS:-}"
  export PKG_CONFIG_PATH="${brew_prefix}/lib/pkgconfig${PKG_CONFIG_PATH:+:${PKG_CONFIG_PATH}}"
  export DYLD_LIBRARY_PATH="${brew_prefix}/lib${DYLD_LIBRARY_PATH:+:${DYLD_LIBRARY_PATH}}"
}

build_yices() {
  rm -rf "${YICES_PREFIX}"
  mkdir -p "${YICES_PREFIX}"
  pushd "${YICES_SRC}" >/dev/null
  autoconf
  ./configure --prefix="${YICES_PREFIX}" --enable-thread-safety
  make MODE=release
  make MODE=release install
  popd >/dev/null
}

run_java_ci() {
  export CPPFLAGS="-I${YICES_PREFIX}/include ${CPPFLAGS:-}"
  export LDFLAGS="-L${YICES_PREFIX}/lib ${LDFLAGS:-}"

  if [[ "$(uname)" == "Darwin" ]]; then
    export DYLD_LIBRARY_PATH="${YICES_PREFIX}/lib${DYLD_LIBRARY_PATH:+:${DYLD_LIBRARY_PATH}}"
  else
    export LD_LIBRARY_PATH="${YICES_PREFIX}/lib${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}"
  fi

  export YICES_JNI="${REPO_ROOT}/dist/lib"
  export YICES_CLASSPATH="${REPO_ROOT}/build/classes"

  pushd "${REPO_ROOT}" >/dev/null
  ant clean test
  popd >/dev/null
}

configure_brew_env
build_yices
run_java_ci
