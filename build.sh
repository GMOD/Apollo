#!/bin/bash
#https://github.com/cmdcolin/jbrowse/blob/master/.travis.yml
CURRENT=`pwd`
: ${APOLLO_ROOT_DIRECTORY:=`pwd`}
: ${APOLLO_BUILD_DIRECTORY:=$APOLLO_ROOT_DIRECTORY}
: ${APOLLO_WEBAPP_DIRECTORY:="$APOLLO_ROOT_DIRECTORY/src/main/webapp"}
: ${APOLLO_JBROWSE_DIRECTORY:="$APOLLO_WEBAPP_DIRECTORY/jbrowse"}
: ${APOLLO_JBROWSE_GITHUB:="$APOLLO_ROOT_DIRECTORY/jbrowse-github"}
: ${JBROWSE_GITHUB:="https://github.com/GMOD/jbrowse"}
: ${JBROWSE_RELEASE:="master"}
: ${JBROWSE_VERSION:="dev"}
: ${GIT_VERSION:=`git rev-parse --verify HEAD`}
: ${POM_VERSION:=`mvn validate | grep Building | cut -d' ' -f4`}

echo "Building ${POM_VERSION} from ${GIT_VERSION}"

if [[ $1 == help || $1 == --help ]]; then
    echo "Usage: build.sh [release|debug|github]"
    echo ""
    echo "Options:"
    echo "release: builds in release mode (minimized javascript)"
    echo "debug: builds in debug mode (non-minimized javascript)"
    echo "github: builds straight from github (no processing of javascript)"
    echo "clean: removes any existing jbrowse builds from build directory"
    echo ""
    echo "Additional environment variables:"
    echo "JBROWSE_GITHUB: URL of git repository for JBrowse ($JBROWSE_GITHUB)"
    echo "JBROWSE_RELEASE: Release tag, commit tag, or branch for JBrowse ($JBROWSE_RELEASE)"
    echo "JBROWSE_VERSION: Release version stored in package.json for JBrowse ($JBROWSE_VERSION)"
    echo "APOLLO_JBROWSE_GITHUB: Location of local JBrowse repo ($APOLLO_JBROWSE_GITHUB)"
    echo "APOLLO_ROOT_DIRECTORY: Location of local WebApollo repo ($APOLLO_ROOT_DIRECTORY)"
    exit 0
fi

# create version.jsp 
echo "<a href='https://github.com/GMOD/Apollo/commit/${GMOD_VERSION}' target='_blank'>Version: ${POM_VERSION}</a>" > $APOLLO_WEBAPP_DIRECTORY/version.jsp


if [ ! -d "$APOLLO_JBROWSE_GITHUB" ]; then
  echo "No jbrowse repo found at $APOLLO_JBROWSE_GITHUB, cloning from $JBROWSE_GITHUB"
  cd $APOLLO_BUILD_DIRECTORY
  git clone --depth 1 --recursive $JBROWSE_GITHUB $APOLLO_JBROWSE_GITHUB
  cd $CURRENT
fi


if [ ! -d "$APOLLO_JBROWSE_DIRECTORY" ]; then
  cd "$APOLLO_JBROWSE_GITHUB"
  rm -rf JBrowse-$JBROWSE_VERSION-dev
  rm -rf JBrowse-$JBROWSE_VERSION
  git checkout -- release-notes.txt
  git checkout -- src/JBrowse/Browser.js
  git fetch
  git reset --hard origin/$JBROWSE_RELEASE
  cp -r $APOLLO_ROOT_DIRECTORY/client/apollo plugins/WebApollo

  if [[ $1 == release ]]; then
      echo "Using release jbrowse"
      ulimit -n 1000
      make -f build/Makefile release-notest
      mv JBrowse-$JBROWSE_VERSION $APOLLO_JBROWSE_DIRECTORY
  elif [[ $1 == debug ]]; then
      echo "Using debug jbrowse"
      ulimit -n 1000
      make -f build/Makefile release-notest
      mv JBrowse-$JBROWSE_VERSION-dev $APOLLO_JBROWSE_DIRECTORY
  elif [[ $1 == github ]]; then
      echo "Using github jbrowse"
      cp -R .  $APOLLO_JBROWSE_DIRECTORY
  fi

elif [[ $1 == clean ]]; then
    rm -rf "$APOLLO_JBROWSE_DIRECTORY" 
else
    echo "Found jbrowse installed at $APOLLO_JBROWSE_DIRECTORY"
fi



if [ -e "$APOLLO_ROOT_DIRECTORY/config.xml" ]; then
    cp  $APOLLO_ROOT_DIRECTORY/config.xml $APOLLO_WEBAPP_DIRECTORY/config/config.xml
else
   echo "No config.xml found, not copying."
   echo "You must copy and sample_config.xml to config.xml in order to build."
   exit ;
fi

if [ -e "$APOLLO_ROOT_DIRECTORY/config.properties" ]; then
    cp  $APOLLO_ROOT_DIRECTORY/config.properties $APOLLO_WEBAPP_DIRECTORY/config/config.properties
else
   echo "No config.properties found, not copying."
   echo "You must copy and sample_config.properties to config.properties in order to build."
   exit ;
fi

# optional
if [ -e "$APOLLO_ROOT_DIRECTORY/blat_config.xml" ]; then
    cp $APOLLO_ROOT_DIRECTORY/blat_config.xml $APOLLO_WEBAPP_DIRECTORY/config/blat_config.xml
else
    echo "No blat_config.xml found, not copying."
fi

# optional
if [ -e "$APOLLO_ROOT_DIRECTORY/hibernate.xml" ]; then
    cp $APOLLO_ROOT_DIRECTORY/hibernate.xml $APOLLO_WEBAPP_DIRECTORY/config/hibernate.xml
else
    echo "No hibernate.xml found, not copying."
fi


# optional
if [ -e "$APOLLO_ROOT_DIRECTORY/log4j2.json" ]; then
    cp  $APOLLO_ROOT_DIRECTORY/log4j2.json $APOLLO_ROOT_DIRECTORY/src/main/resources/log4j2.json
else
   echo "No log4j2.json found, not copying."
fi

# optional
if [ -e "$APOLLO_ROOT_DIRECTORY/log4j2-test.json" ]; then
    cp  $APOLLO_ROOT_DIRECTORY/log4j2-test.json $APOLLO_ROOT_DIRECTORY/src/test/resources/log4j2-test.json
else
   echo "No log4j2-test.json found, not copying."
fi

