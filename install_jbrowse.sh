#!/bin/bash


# install jbrowse bins to the system
# may need to be run as sudo
CURRENT=`pwd`
: ${APOLLO_ROOT_DIRECTORY:=`pwd`}
: ${APOLLO_BUILD_DIRECTORY:=$APOLLO_ROOT_DIRECTORY}
: ${APOLLO_WEBAPP_DIRECTORY:="$APOLLO_ROOT_DIRECTORY/src/main/webapp"}
: ${APOLLO_JBROWSE_DIRECTORY:="$APOLLO_WEBAPP_DIRECTORY/jbrowse"}
cd $APOLLO_JBROWSE_DIRECTORY
perl Makefile.PL $*
make
make install
cd $CURRENT

