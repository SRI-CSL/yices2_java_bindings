#
# Makefile to build libyices2java.dylib or libyices2java.so
#
# Call with
#    make OS=darmin
# or make OS=linux
#
# We assume gmake
# We also assume that jni.h is installed in ${JAVA_HOME}/include,
# jni_md.h is in ${JAVA_HOME}/include/${OS}, and that ${YICES_JNI} exists
#

SHELL=/bin/sh

ifneq ($(MAKECMDGOALS),clean)

ifeq ($(OS),)
 guess=$(shell uname)
 ifeq ($(guess),Darwin)
  OS := darwin
 else
 ifeq ($(guess),Linux)
  OS := linux
 endif
 endif
endif

ifeq ($(OS),linux)
 EXTENSION=so
else
ifeq ($(OS),darwin)
 EXTENSION=dylib
else
ifeq ($(OS),)
 $(error "Please set $$OS")
else
 $(error "Unkown OS: $(OS)")
endif
endif
endif

ifneq ($(MAKECMDGOALS),install)
ifeq ($(JAVA_HOME),)
 $(error "Please set $$JAVA_HOME")
endif
endif

endif

# name of the library
libyices2java := libyices2java.$(EXTENSION)

# install name for darwin
libyices2java_install_name := $(YICES_JNI)/libyices2java.dylib

# we ignore versions and soname for now

# default include directories for jni.h and jni_md.h
CPPFLAGS := -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(OS)
CXXFLAGS := -g -fPIC
LIBS := -lyices -lgmp

CXX ?= g++


# JAVAH to create the include file(s)
JAVAH ?= javah

# JAVAC to compile the java
JAVAC ?= javac

# these defaults are for the ant build, and
# should/will be overidden when using the build.sh
YICES_CLASSPATH?=../../../../../../build/classes
YICES_JNI?=../../../../../../dist/lib

all: $(libyices2java)

$(YICES_CLASSPATH)/com/sri/yices/%.class: %.java
	$(JAVAC) -d $(YICES_CLASSPATH) *.java

com_sri_yices_Yices.h: $(YICES_CLASSPATH)/com/sri/yices/Yices.class
	$(JAVAH) -classpath $(YICES_CLASSPATH) com.sri.yices.Yices

yicesJNI.o: yicesJNI.cpp com_sri_yices_Yices.h
	$(CXX) $(CPPFLAGS) $(CXXFLAGS) -Wall -c yicesJNI.cpp

libyices2java.dylib: yicesJNI.o
	$(CXX) $(CPPFLAGS) $(CXXFLAGS) -dynamiclib -o $@ yicesJNI.o $(LIBS)

libyices2java.so: yicesJNI.o
	$(CXX) $(CFLAGS) $(LDFLAGS) -shared -o $@ yicesJNI.o $(LIBS)

#
# crude install to LIBDIR
# the directory $LIBDIR must exist
#
#LIBDIR := /usr/local/lib
LIBDIR := $(YICES_JNI)


install: install-$(OS)

install-linux:
	cp $(libyices2java) $(LIBDIR)
	ldconfig -n $(LIBDIR)

install-darwin:
	cp $(libyices2java) $(LIBDIR)

#
#
clean:
	rm -f *.o *.so *.dylib com_sri_yices_Yices.h

.PHONY: all clean install install-linux install-darwin
