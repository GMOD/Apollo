Apollo
======

<a href="https://github.com/GMOD/Apollo/blob/master/README.md">On GitHub</a>

An instantaneous, collaborative, genome annotation editor.  The stack is a Java web application / database backend and a Javascript client that runs in a web browser as a JBrowse plugin.  

For general information on WebApollo, go to: 
[http://genomearchitect.org/](http://genomearchitect.org/)

For the full WebApollo installation and configuration instructions for 1.x, please see:
[http://webapollo.readthedocs.org](http://webapollo.readthedocs.org)

The WebApollo client is implemented as a plugin for JBrowse, for more information on JBrowse, please visit:
[http://jbrowse.org](http://jbrowse.org)

![Build status](https://travis-ci.org/GMOD/Apollo.svg?branch=master)

Note: For documentation of older Web Apollo versions, please see [http://gmod.org/wiki/WebApollo_Installation](http://gmod.org/wiki/WebApollo_Installation)

## Quick Update Guide to Version 1.0.x

If you already have Web Apollo instances running, you can easily update using these steps.

### Edit property files and config files before deploying

    cp ./sample_config.properties ./config.properties  # must set database parameters and data directories
    cp ./sample_config.xml ./config.xml # see configuration guide for more details
    cp ./sample_canned_comments.xml ./canned_comments.xml  # see configuration guide for more details 
    cp ./sample_blat_config.xml ./blat_config.xml     # optional
    cp ./sample_hibernate.xml ./hibernate.xml    # optional
    cp ./sample_log4j2.json ./log4j2.json     # optional
    cp ./sample_log4j2-test.json ./log4j2-test.json     # optional


You must edit config.properties to supply the jbrowse data and annotations directory. The datastore.directory property is where Web Apollo annotations are to be stored.  The jbrowse.data property is where the jbrowse tracks are stored.

**Note: the JBrowse data directory should not be stored in the Tomcat webapps directory. This can result in data loss when doing undeploy operations in Tomcat**.


### Generate a war file

Most users will only need to use Maven to generate a war file:

    apollo deploy 

Users wanting to compile a custom package can refer to the [developers guide](docs/Developer.md)

### Install jbrowse binaries

As part of the installation process, JBrowse scripts are installed to a local directory (./bin) using install\_jbrowse.sh:

    install_jbrowse.sh 

### Run locally

To run tomcat on 8080:

    apollo run

To run tomcat on 8080, but open up the debug port on 5005:

    apollo debug

