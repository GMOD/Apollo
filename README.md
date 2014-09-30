Apollo
======

A genome annotation editor.  The stack is a Java web application / database backend and a Javascript client that runs in a web browser as a JBrowse plugin.  

For general information on WebApollo, go to: 
http://genomearchitect.org/

For the full WebApollo installation and configuration instructions for 1.x, go to:
http://gmod.org/wiki/WebApollo_Installation_1.x

The WebApollo client is implemented as a plugin for JBrowse, for more information on JBrowse, please visit:
http://jbrowse.org

https://travis-ci.org/GMOD/Apollo.svg?branch=master

=====

Quick build steps.  

## Edit property files and config files before deploying

    cp ./sample_config.properties ./config.properties 
    cp ./sample_config.xml ./config.xml 
    cp ./sample_log4j2.json ./log4j2.json 
    cp ./sample_log4j2-test.json ./log4j2-test.json 

 
Edit the property files to point to the appropriate directory. You ```must edit the jbrowse data directory``` in the config.properties: jbrowse.data=/opt/apollo/jbrowse/data 
 

## Generate a war file

Generates proper build:

    deploy.sh release

Generates a "debug" build, used in development:

    deploy.sh debug 

Generates an unoptimized war file using github code:

    deploy.sh github

## Install jbrowse binaries

As a convenience, JBrowse binaries can be installed to the system:

    install_jbrowse_bin.sh [cpanm]

Having jbrowse binaries installed to the system makes it easier to create data directories in appropriate system directories instead of inside the tomcat webapps directory. This script can optionally install using cpanm using the cpanm argument.

## Deploy

To run tomcat on 8080:

    run.sh
To run tomcat on 8080, list to debug port on 8000:

    debug.sh

Runs all unit tests (optionally including the jbrowse unit tests):

    test.sh [jbrowse]


