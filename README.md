Apollo
======

A genome annotation editor.  The stack is a Java web application / database backend and a Javascript client that runs in a web browser as a JBrowse plugin.  

For general information on WebApollo, go to: 
http://genomearchitect.org/

For the full WebApollo installation and configuration instructions for 1.x, go to:
http://gmod.org/wiki/WebApollo_Installation_1.x

The WebApollo client is implemented as a plugin for JBrowse, for more information go to: 
http://jbrowse.org

=====

Quick build steps.  

    cp ./sample_config.properties ./config.properties 
    cp ./sample_config.xml ./config.xml 
 
Edit the property files to point to the appropriate directory. You ```must edit the jbrowse data directory``` in the config.properties: jbrowse.data=/opt/apollo/jbrowse/data 
 

To run tomcat on 8080:

    run.sh 
To run tomcat on 8080, list to debug port on 8000:

    debug.sh 
Runs all unit tests:

    test.sh 
Generates a proper build, but doesn't test some legacy perl features:

    deploy.sh release-notest      
Generates proper build, but runs all tests, which may require additional configuration on your system:

    deploy.sh release
Generates a "debug" build, used in development. Does not test some legacy perl features (as above):

    deploy.sh debug-notest 
Generates a "debug" build, used in development:

    deploy.sh debug 
Generates an optimized war file (similar to previous releases):

    deploy.sh 




