#!/bin/bash
#https://github.com/cmdcolin/jbrowse/blob/master/.travis.yml
APOLLO_ROOT_DIRECTORY=`pwd`
APOLLO_WEBAPP_DIRECTORY="$APOLLO_ROOT_DIRECTORY/src/main/webapp"
APOLLO_JBROWSE_DIRECTORY="$APOLLO_WEBAPP_DIRECTORY/jbrowse"
JBROWSE_GITHUB="https://github.com/GMOD/jbrowse"
if [ ! -d "$APOLLO_JBROWSE_DIRECTORY" ]; then
  echo "No jbrowse directory found at $APOLLO_JBROWSE_DIRECTORY, installing locally from $JBROWSE_GITHUB"
  cd $APOLLO_WEBAPP_DIRECTORY
  git clone --recursive $JBROWSE_GITHUB
  cd $APOLLO_JBROWSE_DIRECTORY

  $APOLLO_JBROWSE_DIRECTORY/setup.sh

  cp -r $APOLLO_ROOT_DIRECTORY/client/apollo $APOLLO_JBROWSE_DIRECTORY/plugins/WebApollo
#  ulimit -n 1000
#
#  make -f build/Makefile release
  cd $APOLLO_ROOT_DIRECTORY

  # Control will enter here if $DIRECTORY doesn't exist.
else
  echo "jbrowse installed"
fi


