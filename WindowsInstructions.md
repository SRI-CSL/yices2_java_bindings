# Build Instructions for Windows/Mingw

## Background

https://github.com/SRI-CSL/yices2_java_bindings defines Java wrappers to the Yices 2 API.

The build system for these wrappers use ant and Makefiles. It works well on Linux and MacOS.
It fails on Windows (or Cygwin or MinGW).

These instructions explain how to build these Java binding by hand using Windows and MinGW.

## Steps

### Compile the Java JNI files

A first step was to compile the Java wrapper files.
We used a directory build/classes which was located at the root directory of the project,
since that is what the Makefile does,
but this is probably pretty arbitrary if you are building things manually.
At some point we set YICES_CLASSPATH in the environment to build/classes because that variable
is used in the Makefile, but afterwards we just used build/classes directly.

Then we generated a required C header file for the Yices wrapper methods.
The Makefile uses a Java 8 utility javah for this purpose, which ran over the .class Java files.
However, in Java 10, javah has been replaced by a switch -h in javac.
We used the following command
javac -classpath build/classes -h . Yices.java
(the classpath here is probably not needed, but I haven't tested removing it)

The next step was to build the DLL. To compile the cpp core, we used mingw64.
There was some talk about using native Windows compilers to generate the DLL instead
(thus making it unnecessary to use Cygwin).
However, Bruno mentioned that compiling the gmp in Windows is very hard.
Besides, if we want to re-establish the possibility of adapting and using the ant/Makefile set up to build the DLL,
then sticking with Cygwin makes sense.

We needed to install the following in Cygwin:

```
Install mingw64-x86_64-binutils 2.29.1.787c9873-1
Install mingw64-x86_64-gcc-core 7.4.0-1
Install mingw64-x86_64-gcc-g++ 7.4.0-1
Install mingw64-x86_64-headers 7.0.0-1
Install mingw64-x86_64-runtime 7.0.0-1
```

Below are the commands for building the DLL. Note that we used quotes around $JAVA_HOME.

```sh
export CXX=/bin/x86_64-w64-mingw32-g++
$CXX -I "$JAVA_HOME\include" -I "$JAVA_HOME\include\win32" -I "C:\Users\E26638\Programs\yices-2.6.1-x86_64-pc-mingw32-static-gmp\yices-2.6.1\include" -fpermissive -c yicesJNI.cpp
```
to generate ``yicesJNI.o``


The next step is
```
$CXX -L"C:\Users\E26638\Programs\yices-2.6.1-x86_64-pc-mingw32-static-gmp\yices-2.6.1\lib" -shared -o libyices2java.dll yicesJNI.o -lyices -lgmp
```
That generates the DLL.

Note that we used the name libyices2java.dll. This is not ideal because the Java source code for loading the library is
```java
System.loadLibrary("yices2java");
```
In Linux, Java adds a prefix "lib to the name and the appropriate extension.
In Windows it does NOT add a prefix "lib" and looks for yices2java, so using the name yices2java.dll makes more sense.

### Finding Dependent DLLs

The DLL requires other DLLs to work. You can inspect it with

```sh
objdump -p libyices2java.dll | less
```

to see what those require DLLs are. We saw libgmp-10.dll, libstdc++-6.dll and libgcc_s_seh-1.dll listed as dependencies.

Since objdump does not know the path where to find the required DLLs,
it was useful to use the following to find them in Cygwin:

```sh
find / -name 'libgmp-10*'
```
We identified and copied them to the current directory:

```sh
cp /usr/x86_64-w64-mingw32/sys-root/mingw/bin/libgmp-10.dll .
cp /usr/x86_64-w64-mingw32/sys-root/mingw/bin/libstdc++-6.dll .
cp /usr/x86_64-w64-mingw32/sys-root/mingw/bin/libgcc_s_seh-1.dll .
```

We also had to copy ``libyices.dll`` from the bin directory in the Windows Yices binary (https://yices.csl.sri.com/).
Careful: there is also a ``libyices.dll.a`` in the lib directory of the Windows Yices binary, but that's not the right file.

Note that the DLLs are found to load by Java by looking at the java.library.path system property,
which is set to the PATH environment variable by default. So the directory in which the DLLs are must be in PATH.

I then created an Eclipse project to run the Yices bindings test.
This still did not work due to a "missing dependent libraries" error message issued by System.loadLibrary.
Even though we had included the DLLs shown by objdump, the included DLLs have themselves their own dependencies.
I used an old Microsoft utility called Dependency Walker, which shows dependencies across multiple libraries
in a recursive fashion (a more modern alternative, which I did not use, is at https://github.com/lucasg/Dependencies).
I then identified the missing library libwinpthread-1.dll, which I included with:

```sh
cp /usr/x86_64-w64-mingw32/sys-root/mingw/bin/libwinpthread-1.dll .
```
and the tests worked.


### What now?

Ideally one can repair the ant/Makefile setup to work directly under Cygwin.

Then we can prepare a binary Windows distribution of the bindings containing the DLLs and a jar file containing the Java wrappers.

An even better solution would be to deploy the Java wrappers to Maven Central so that users can include them
in their Java projects with a couple of lines in the pom.xml Maven configuration file.

I suppose it is possible to deploy even the binary libraries themselves (for both Linux and Windows) through Maven Central, but I am not sure.
