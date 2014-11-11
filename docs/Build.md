Building WebApollo
--------------------

To build WebApollo, we will first edit the configuration files, and then run a Makefile to create a WAR package.

### Before you build

You need to configure your instance using a config.properties and a
config.xml file, which are copied into the war file.

-   Copy the sample config / logging files to the right location.

    cp sample_config.properties config.properties
    cp sample_config.xml config.xml
    cp sample_log4j2.json log4j2.json
    cp sample_log4j2-test.json log4j2-test.json

-   Edit the config.properties file to point to the appropriate directories. A sample config.properties file might look like
    
    jbrowse.data=/apollo/data
    datastore.directory=/apollo/annotations
    database.url=jdbc:postgresql:web_apollo_users
    database.username=postgres_user
    database.password=postgres_password
    organism=Pythium ultimum

**IMPORTANT: the jbrowse.data directory should not be placed
anywhere inside the Tomcat webapps folder, not even using
symlinks!! To avoid data loss when doing Tomcat Undeploy operations,
users are advised not to be touching anything inside of the webapps
folder.**

### Building the servlet

We use a Makefile to create our build package. The easiest method is to use the
download-release target to get the pre-compiled JBrowse+WebApollo package:

     make clean download-release package

This runs three separate build steps
- Clean out any old packages
- Download the pre-compiled JBrowse+WebApollo client from github
- Create a WAR package with your custom properties and config files

Other options to the Makefile include:

     make download-debug # download the debug client instead of the download-release
     make release # compile the jbrowse+webapollo client manually instead of downloading pre-compiled version
     make unoptimized # compile the jbrowse+webapollo client manually instead of downloading the pre-compiled version (unoptimized)
     make run # run the the built package using a temporary tomcat server for testing
     make test # run the command line tests for webapollo
     make test-jbrowse # run the command line tests for jbrowse (requires extra perl pre-requisites)

Developers are advised to review our developer's guide for further instructions on creating their own builds using "make release" et al.
