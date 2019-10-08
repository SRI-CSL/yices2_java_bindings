#!/bin/bash

set -e

function error() {
    echo "**********************************************************************"
    echo "*********************  ERROR OCCURED !!!! ****************************"
    echo "**********************************************************************"
    echo "$1"
    echo "**********************************************************************"
    exit -1;
}

if [ $EUID -eq 0 ]; then echo "Should not run this script as root"; exit -1; fi

# Yices JNI
if [[ -z "$YICES_JNI" ]]; then
    echo "Please set environment variable YICES_JNI where the libyices2java dynamic library will live."
    exit -1
fi

# Yices CLASSPATH
if [[ -z "$YICES_CLASSPATH" ]]; then
    echo "Please set environment variable YICES_CLASSPATH where the yices java classes will live."
    exit -1
fi

echo
echo "Building the Yices2Java wrapper..."

if [[ ( ! -z "$YICES_JNI")  &&  ( ! -d "$YICES_JNI" ) ]] ; then
    echo "Creating directory ${YICES_JNI}"
    (mkdir -p $YICES_JNI || (echo "Failed to create directory ${YICES_JNI}" ; exit -1 ))
fi

if [[ ( ! -z "$YICES_CLASSPATH")  &&  ( ! -d "$YICES_CLASSPATH" ) ]] ; then
    echo "Creating directory ${YICES_CLASSPATH}"
    (mkdir -p $YICES_CLASSPATH || (echo "Failed to create directory ${YICES_CLASSPATH}" ; exit -1 ))
fi

cd ./src/main/java/com/sri/yices/

if CLASSPATH=${YICES_CLASSPATH} make && make install ; then
    echo "Installed the Yices2Java wrapper in ${YICES_JNI}"
else
    echo "Could not install the Yices2Java wrapper"
    exit -1
fi
echo "DONE"


