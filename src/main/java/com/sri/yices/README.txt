To build the Yices JNI code. It's a manual process for now.

1) compile all the java files in this directory

  (It's easiest to do from IntelliJ: build menu + build module 'utils').

2) from the terminal type

     make
  or make CXX=g++

  (this assumes that you have installed yices and gmp on your 
   machine).

3) copy libyices2java.so  or libyices2java.dylib into a reasonable
   directory. I use /usr/local/lib.


Testing
-------

To run the tests from IntelliJ: 

  open GeneralUtils/test/java/TestYices.java

  edit the 'TestYices' configuration and add the following to the VM
  options:
 
   -Djava.library.path=/usr/local/lib

  then click "run" 
  
That should run all the tests and print a lot of data.


Testing from the terminal
-------------------------

Assuming you're in directory GeneralUtils/src/main/java/com/sri/yicesJNI, 
type something equivalent to this:

java -cp ../../../../../../target/classes:../../../../../../target/test-classes:${STITCHES_Libraries}/repository/junit/junit/4.12/junit-4.12.jar:$(STITCHES_Libraries}/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar  org.junit.runner.JUnitCore  TestYices
