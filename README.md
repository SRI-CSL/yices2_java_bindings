[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Build Status](https://travis-ci.com/SRI-CSL/yices2_java_bindings.svg?token=77vj4Wxx3rNcgSb3dqRC&branch=master)

#  Java Bindings for Yices 2

## Installation

You will need a recent installation of yices2 (>= 2.6.1), java (>= 8 or 1.8).

These instructions are for Unix style operating systems. There is a seperate
[file](https://github.com/SRI-CSL/yices2_java_bindings/blob/master/WindowsInstructions.md)
that describes the procedure on Windows.

You can build either using ant (>= 1.10.0), or directly using the `build.sh` that
we provide.

### Building with ant

```
>ant
```
will list the targets.
```
ant install
```
will create the jar, and the JNI dynamic shared library in the
directory `./dist/lib`.
```
ant test
```
will also run some tests.

You can also directly run the build products on the command line via:
```
java -Djava.library.path=./dist/lib -jar ./dist/lib/yices.jar

```
Which will print out version information about the jar, the shared library, as
well as your current yices installation. For example:
```
Yices Java Bindings Version 1.0.1
Build date: April 2 2020
using Yices dynamic library version 2.6.2
Built for x86_64-apple-darwin18.7.0
Build mode: release
Build date: 2019-11-08
MCSat supported: true
```

### Building with build.sh

One can also avoid `ant` by using the [build.sh](https://github.com/SRI-CSL/yices2_java_bindings/blob/master/build.sh)
provided you set two environment variables

* `YICES_CLASSPATH`  the directory where you want the class files to be placed.
* `YICES_JNI` the directory where you want the dynamic JNI library and jar file to be placed.

As an example we include [ant.sh](https://github.com/SRI-CSL/yices2_java_bindings/blob/master/ant.sh)
which mimics the functionality of the `ant` build via the `build.sh` script, and the `jar` utility.

## Examples

