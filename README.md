Apollo
======

[![Join the chat at https://gitter.im/GMOD/Apollo](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/GMOD/Apollo?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

<a href="https://github.com/GMOD/Apollo/blob/master/README.md">On GitHub</a>

An instantaneous, collaborative, genome annotation editor.  The stack is a Java web application / database backend and a Javascript client that runs in a web browser as a JBrowse plugin.  

For general information on Web Apollo, go to: 
[http://genomearchitect.org/](http://genomearchitect.org/)

Complete Web Apollo installation and configuration instructions are available at:
[http://webapollo.readthedocs.org](http://webapollo.readthedocs.org)

The Web Apollo client is implemented as a plugin for JBrowse, for more information on JBrowse, please visit:
[http://jbrowse.org](http://jbrowse.org)

![Build status](https://travis-ci.org/GMOD/Apollo.svg?branch=master)

## Please note repository update and be aware
As of 15-April-2015 the mainline is our 2.0 code. The 1.0 code has now moved to a 1.0 branch.

Version 2.0.0 is now released. See [the announcement](http://genomearchitect.org/Apollo2_first_release)

## Quick Update Guide to Version 2.0.x 

If you already have Web Apollo instances running, you can use your current JBrowse data directories.  

See [the Apollo2 build guide](docs/Apollo2Build.md) to get the proper build guides up.

## Migrating data from older versions to WA2

You can follow steps in our [migration guide](docs/Migration.md) to move annotations and data from older versions to WA2.

### Note about data directories

In WA2.0, all data directories are stored in locations outside of the tomcat webapps directory. Use the WA2.0 [quick-start guide](docs/Apollo2Build.md) to learn how to add new data directories for your organisms.


**Important Note: Data from your data loading pipeline should not be stored in the Tomcat webapps directory. This can result in data loss when doing undeploy operations in Tomcat. It will even delete data from inside symlinks instead of just removing the symlink itself.**.

### Run Apollo in a temporary server

Users can evaluate webapollo


### Run locally 

To launch Apollo with temporary settings, use the run-local command
 
    apollo run-local 8080
    
This will automatically launch Web Apollo 2 in a temporary server.


### Generate a war file

Users can generate a war file (for example target/apollo-1.0.2.war) that will be copied into their tomcat webapps directory for production deployments:

    apollo deploy 

Note: make sure to create an apollo-config.groovy file following the sample data (e.g. sample-postgres-apollo-config.groovy) to make sure you use your preferred database settings.


### Run locally for GWT development

    apollo devmode 
   

### Thanks to
[![IntelliJ](https://lh6.googleusercontent.com/--QIIJfKrjSk/UJJ6X-UohII/AAAAAAAAAVM/cOW7EjnH778/s800/banner_IDEA.png)](http://www.jetbrains.com/idea/index.html)

[![YourKit] (https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/) 


Thanks to YourKit for providing us the use of their YourKit Java Profiler.  YourKit supports Open Source.
