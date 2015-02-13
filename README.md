Apollo
======

<a href="https://github.com/GMOD/Apollo/blob/master/README.md">On GitHub</a>

An instantaneous, collaborative, genome annotation editor.  The stack is a Java web application / database backend and a Javascript client that runs in a web browser as a JBrowse plugin.  

For general information on WebApollo, go to: 
[http://genomearchitect.org/](http://genomearchitect.org/)

Complete WebApollo installation and configuration instructions for 1.x, are available at:
[http://webapollo.readthedocs.org](http://webapollo.readthedocs.org)

The WebApollo client is implemented as a plugin for JBrowse, for more information on JBrowse, please visit:
[http://jbrowse.org](http://jbrowse.org)

![Build status](https://travis-ci.org/GMOD/Apollo.svg?branch=master)

Note: For documentation of older Web Apollo versions, please see [http://gmod.org/wiki/WebApollo_Installation](http://gmod.org/wiki/WebApollo_Installation)

## Quick Update Guide to Version 1.0.x

If you already have Web Apollo instances running, you can use these steps to update to Version 1.0.x.

### Remove any symlinks in your deploy directory
In your deployment / webapp directory, remove your symlinks.  Tomcat will remove data through the symlinks.  You won't need symlinks or to deploy the war file. 


### Edit property files and config files before deploying

    cp ./sample_config.properties ./config.properties  # must set database parameters and data directories
    cp ./sample_config.xml ./config.xml # see configuration guide for more details
    cp ./sample_canned_comments.xml ./canned_comments.xml  # see configuration guide for more details 
    cp ./sample_blat_config.xml ./blat_config.xml     # optional
    cp ./sample_hibernate.xml ./hibernate.xml    # optional
    cp ./sample_log4j2.json ./log4j2.json     # optional
    cp ./sample_log4j2-test.json ./log4j2-test.json     # optional


You must edit config.properties to supply the jbrowse data and annotations directory. The datastore.directory property is where Web Apollo annotations are to be stored.  The jbrowse.data property is where the jbrowse tracks are stored.   

If you specify the database properties in both the config.xml and config.properties, only the one in config.properties will be used.

**Important Note: the JBrowse data directory should not be stored in the Tomcat webapps directory. This can result in data loss when doing undeploy operations in Tomcat**.


### Generate a war file

Most users will only need to generate a war file (for example target/apollo-1.0.2.war) that will be copied into their tomcat webapps directory:

    apollo deploy 

Users wanting to compile a custom package can refer to the [developers guide](docs/Developer.md)

### Install jbrowse binaries

As part of the installation process, JBrowse scripts are installed to a local directory (./bin) using install\_jbrowse.sh:

    install_jbrowse.sh 

### Run locally

To run tomcat on 8080:

    apollo run

To run tomcat on 8080, but open up the debug port on 8000:

    apollo debug


### Thanks to
[![IntelliJ](https://lh6.googleusercontent.com/--QIIJfKrjSk/UJJ6X-UohII/AAAAAAAAAVM/cOW7EjnH778/s800/banner_IDEA.png)](http://www.jetbrains.com/idea/index.html)
