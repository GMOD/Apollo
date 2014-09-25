#!/bin/bash
#https://github.com/cmdcolin/jbrowse/blob/master/.travis.yml
: ${APOLLO_ROOT_DIRECTORY:=`pwd`}
: ${APOLLO_BUILD_DIRECTORY:=$APOLLO_ROOT_DIRECTORY}
: ${APOLLO_WEBAPP_DIRECTORY:="$APOLLO_ROOT_DIRECTORY/src/main/webapp"}
: ${APOLLO_JBROWSE_DIRECTORY:="$APOLLO_WEBAPP_DIRECTORY/jbrowse"}
: ${APOLLO_JBROWSE_GITHUB:="$APOLLO_ROOT_DIRECTORY/jbrowse-github"}
: ${JBROWSE_GITHUB:="https://github.com/GMOD/jbrowse"}
: ${JBROWSE_RELEASE:="master"}


if [ ! -d "$APOLLO_JBROWSE_GITHUB" ]; then
  echo "No jbrowse repo found at $APOLLO_JBROWSE_GITHUB, cloning from $JBROWSE_GITHUB"
  cd $APOLLO_BUILD_DIRECTORY
  git clone --recursive $JBROWSE_GITHUB $APOLLO_JBROWSE_GITHUB 
fi


if [ ! -d "$APOLLO_JBROWSE_DIRECTORY" ]; then
  cd "$APOLLO_JBROWSE_GITHUB"
  git pull
  git checkout $JBROWSE_RELEASE
  rm -rf plugins/WebApollo
  cp -r $APOLLO_ROOT_DIRECTORY/client/apollo plugins/WebApollo

  if [[ $1 == release ]]; then
      echo "Using release jbrowse"
      ulimit -n 1000
      make -f build/Makefile release
      mv JBrowse-dev $APOLLO_JBROWSE_DIRECTORY
  elif [[ $1 == debug ]]; then
      echo "Using debug jbrowse"
      ulimit -n 1000
      make -f build/Makefile release
      mv JBrowse-dev-dev $APOLLO_JBROWSE_DIRECTORY
  elif [[ $1 == release-notest ]]; then
      echo "Using release jbrowse, building with no test"
      ulimit -n 1000
      make -f build/Makefile release-notest
      mv JBrowse-dev $APOLLO_JBROWSE_DIRECTORY
  elif [[ $1 == debug-notest ]]; then
      echo "Using debug jbrowse, building with no test"
      ulimit -n 1000
      make -f build/Makefile release-notest
      mv JBrowse-dev-dev $APOLLO_JBROWSE_DIRECTORY
  else
      echo "Using github jbrowse"
      cp -R .  $APOLLO_JBROWSE_DIRECTORY
  fi

  cd $APOLLO_JBROWSE_DIRECTORY
  ./setup.sh
  cd $APOLLO_ROOT_DIRECTORY

else 
  echo "Found jbrowse installed at $APOLLO_JBROWSE_DIRECTORY, no additional jbrowse install steps taken"
fi




if [ -e "$APOLLO_ROOT_DIRECTORY/config.xml" ]; then
    cp  $APOLLO_ROOT_DIRECTORY/config.xml $APOLLO_WEBAPP_DIRECTORY/config/config.xml
else
   echo "No config.xml found, not copying."
fi

if [ -e "$APOLLO_ROOT_DIRECTORY/config.properties" ]; then
    cp  $APOLLO_ROOT_DIRECTORY/config.properties $APOLLO_WEBAPP_DIRECTORY/config/config.properties
else
   echo "No config.properties found, not copying."
fi

if [ -e "$APOLLO_ROOT_DIRECTORY/log4j2.json" ]; then
    cp  $APOLLO_ROOT_DIRECTORY/log4j2.json $APOLLO_ROOT_DIRECTORY/src/main/resources/log4j2.json
else
   echo "No log4j2.json found, not copying."
fi

if [ -e "$APOLLO_ROOT_DIRECTORY/log4j2-test.json" ]; then
    cp  $APOLLO_ROOT_DIRECTORY/log4j2-test.json $APOLLO_ROOT_DIRECTORY/src/test/resources/log4j2-test.json
else
   echo "No log4j2-test.json found, not copying."
fi

