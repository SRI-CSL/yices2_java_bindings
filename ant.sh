#!/bin/bash

#uses the build.sh to mimic what 'ant install' does without 'ant'
REPO_ROOT=${PWD}

YC=${REPO_ROOT}/build/classes
YI=${REPO_ROOT}/dist/lib

YICES_CLASSPATH=${YC} YICES_JNI=${YI} ./build.sh

jar -cvfm ${YI}/yices.jar ${REPO_ROOT}/MANIFEST.txt -C ${YC} ${YC}/com/sri/yices/*.class

java -Djava.library.path=${REPO_ROOT}/dist/lib -jar ${REPO_ROOT}/dist/lib/yices.jar
