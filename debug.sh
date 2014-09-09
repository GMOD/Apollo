#!/bin/bash
#mvnDebug tomcat7:run
#MAVEN_DEBUG_OPTS=" -Xdebug -Djava.compiler=NONE "
#MAVEN_DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8044"
#MAVEN_OPTS=""
./build.sh

OLD_MAVEN_OPTS=$MAVEN_OPTS
export MAVEN_OPTS=-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n

#MAVEN_DEBUG_OPTS="-Xmx512m -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5000"

mvn tomcat7:run   
#mvnDebug tomcat7:run   
export MAVEN_OPTS=$OLD_MAVEN_OPTS
unset OLD_MAVEN_OPTS

#MAVEN_DEBUG_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
#mvn $MAVEN_DEBUG_OPTS tomcat7:run   

