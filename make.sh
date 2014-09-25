#!/bin/bash


# install jbrowse bins to the system
# may need to be run as sudo
CURRENT=`pwd`
: ${APOLLO_ROOT_DIRECTORY:=`pwd`}
: ${APOLLO_BUILD_DIRECTORY:=$APOLLO_ROOT_DIRECTORY}
: ${APOLLO_WEBAPP_DIRECTORY:="$APOLLO_ROOT_DIRECTORY/src/main/webapp"}
: ${APOLLO_JBROWSE_DIRECTORY:="$APOLLO_WEBAPP_DIRECTORY/jbrowse"}
if [ ! -d $APOLLO_JBROWSE_DIRECTORY ]; then
    echo "JBrowse has not been built yet. Please build jbrowse first with build.sh"
    exit 0
fi

cd $APOLLO_JBROWSE_DIRECTORY
perl Makefile.PL $*
make
if [[ $1 == install ]]; then
    make install
fi
cd $CURRENT

