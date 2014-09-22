#!/bin/bash
#https://github.com/cmdcolin/jbrowse/blob/master/.travis.yml
APOLLO_ROOT_DIRECTORY=`pwd`
APOLLO_BUILD_DIRECTORY=$APOLLO_ROOT_DIRECTORY
APOLLO_WEBAPP_DIRECTORY="$APOLLO_ROOT_DIRECTORY/src/main/webapp"
APOLLO_JBROWSE_DIRECTORY="$APOLLO_WEBAPP_DIRECTORY/jbrowse"
JBROWSE_GITHUB="https://github.com/GMOD/jbrowse"
JBROWSE_RELEASE="1.11.5-release"

if [ ! -d "$APOLLO_BUILD_DIRECTORY/jbrowse-github" ]; then
  echo "No jbrowse directory found at $APOLLO_JBROWSE_DIRECTORY, installing locally from $JBROWSE_GITHUB"
  cd $APOLLO_BUILD_DIRECTORY
  git clone --recursive $JBROWSE_GITHUB jbrowse-github
  
  cd $APOLLO_BUILD_DIRECTORY/jbrowse-github
  git checkout $JBROWSE_RELEASE
  cp -r $APOLLO_ROOT_DIRECTORY/client/apollo plugins/WebApollo
  rm -rf $APOLLO_JBROWSE_DIRECTORY
  if [[ $1 == release ]]; then
      echo "Using release jbrowse"
      ulimit -n 1000
      make -f build/Makefile release
      mv JBrowse-dev $APOLLO_JBROWSE_DIRECTORY
  elif [[ $1 == debug ]]; then
      ulimit -n 1000
      make -f build/Makefile release
      echo "Using debug jbrowse"
      mv JBrowse-dev-dev $APOLLO_JBROWSE_DIRECTORY
  else
      cp -R .  $APOLLO_JBROWSE_DIRECTORY
  fi

  cd $APOLLO_JBROWSE_DIRECTORY
  ./setup.sh
  cd $APOLLO_ROOT_DIRECTORY

else 
  echo "jbrowse installed"
fi




if [ -e "$APOLLO_ROOT_DIRECTORY/config.xml" ]; then
    # will either do a force copy
    cp  $APOLLO_ROOT_DIRECTORY/config.xml $APOLLO_WEBAPP_DIRECTORY/config/config.xml
else
   echo "No config.xml found, not copying."
fi

if [ -e "$APOLLO_ROOT_DIRECTORY/config.properties" ]; then
    # will either do a force copy
    cp  $APOLLO_ROOT_DIRECTORY/config.properties $APOLLO_WEBAPP_DIRECTORY/config/config.properties
else
   echo "No config.properties found, not copying."
fi
