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

**Apollo 2 is still a project under development, it has not been formally released, and will lack some of the features and stability necessary for annotation.**

Version 1.0.4 is the latest, fully-functional version of Apollo and is available for download at https://github.com/GMOD/Apollo/releases/latest

## Quick Update Guide to Version 2.0.x 

If you already have Web Apollo instances running, you can use your current JBrowse data directories.  

See [the Apollo2 build guide](docs/Apollo2Build.md) to get the proper build guides up.

## Migrating data from &lt;2.0:

You should be able to migrate most of your annotation data from Apollo 1 to Apollo 2 using our [migration guide](docs/Migration.md) built upon our web services.  

### Remove any symlinks in your deploy directory if updating from &lt;1.0 version
In your deployment / webapp directory, remove your symlinks.  Tomcat will remove data through the symlinks.  You won't need symlinks or to deploy the war file. 

**Important Note: the JBrowse data directory should not be stored in the Tomcat webapps directory. This can result in data loss when doing undeploy operations in Tomcat**.


### Generate a war file

Most users will only need to generate a war file (for example target/apollo-1.0.2.war) that will be copied into their tomcat webapps directory:

    apollo deploy 

### Run locally 

To run tomcat on 8080:

    apollo run-local
    
   
### Run locally for GWT development

    apollo devmode 
   

### Thanks to
[![IntelliJ](https://lh6.googleusercontent.com/--QIIJfKrjSk/UJJ6X-UohII/AAAAAAAAAVM/cOW7EjnH778/s800/banner_IDEA.png)](http://www.jetbrains.com/idea/index.html)

[![YourKit] (https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/) 


Thanks to YourKit for providing us the use of their YourKit Java Profiler.  YourKit supports Open Source.
