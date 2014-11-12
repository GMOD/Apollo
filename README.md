Apollo
======

A genome annotation editor.  The stack is a Java web application / database backend and a Javascript client that runs in a web browser as a JBrowse plugin.  

For general information on WebApollo, go to: 
http://genomearchitect.org/

For the full WebApollo installation and configuration instructions for 1.x, go to:
http://gmod.org/wiki/WebApollo_Installation_1.x

The WebApollo client is implemented as a plugin for JBrowse, for more information on JBrowse, please visit:
http://jbrowse.org

![Build status](https://travis-ci.org/GMOD/Apollo.svg?branch=master)



## Quick build steps.


### Edit property files and config files before deploying

    cp ./sample_config.properties ./config.properties 
    cp ./sample_config.xml ./config.xml 
    cp ./sample_canned_comments.xml ./canned_comments.xml 
    cp ./sample_blat_config.xml ./blat_config.xml     # optional
    cp ./sample_hibernate.xml ./hibernate.xml    # optional
    cp ./sample_log4j2.json ./log4j2.json     # optional
    cp ./sample_log4j2-test.json ./log4j2-test.json     # optional

 
Edit the property files to point to the appropriate directory. You must edit the jbrowse data and annotations directory in the config.properties: jbrowse.data=/opt/apollo/jbrowse/data and  datastore.directory=/opt/apollo/annotations

The datastore.directory is where annotations are to be stored.  The jbrowse.data is where the jbrowse tracks are stored.   **JBrowse data should not be in your tomcat / web-apps directory as in previous versions.**. However, the jbrowse application will still be installed into your tomcat / web-apps directory.

If BLAT is not installed, just copy the default blat file over.
 

### Generate a war file

Generates build using pre-compiled client code:

    make clean download-release package

Generates a "debug" build, using pre-compiled client code:

    make clean download-debug package 

Generates an unoptimized war file using code straight from github (easy):

    make clean unoptimized package

Generates a release package using custom compilation of the client (requires pre-requisites):

    make clean release package

Generates a release package using custom compilation of the client (requires pre-requisites):

    make clean debug package

### Install jbrowse binaries

As a convenience, JBrowse binaries can be installed to the system:

    install_jbrowse_bin.sh [cpanm]

Having jbrowse binaries installed to the system makes it easier to create data directories in appropriate system directories instead of inside the tomcat webapps directory. This script can optionally install using cpanm using the cpanm argument.

### Deploy

To run tomcat on 8080:

    run.sh
To run tomcat on 8080, list to debug port on 8000:

    debug.sh

Runs all unit tests (optionally including the jbrowse unit tests):

    make test
    make test-jbrowse


