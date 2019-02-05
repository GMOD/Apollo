# Apollo
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.2254462.svg)](https://doi.org/10.5281/zenodo.2254462)
[![Build](https://travis-ci.org/GMOD/Apollo.svg?branch=master)](https://travis-ci.org/GMOD/Apollo?branch=master)
[![Coverage](https://coveralls.io/repos/github/GMOD/Apollo/badge.svg?branch=master)](https://coveralls.io/github/GMOD/Apollo?branch=master)
[![Documentation](https://readthedocs.org/projects/genomearchitect/badge/?version=latest)](https://genomearchitect.readthedocs.org/en/latest/)
[![Chat at Gitter](https://badges.gitter.im/GMOD/Apollo.svg)](https://gitter.im/GMOD/Apollo?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)



### [![](https://github.com/GMOD/Apollo/blob/master/docs/images/download_small.png)&nbsp;Download the latest release](https://github.com/GMOD/Apollo/releases/latest)

A collaborative, real-time, genome annotation editor.  The stack is a Java web application / database backend and a
Javascript client that runs in a web browser as a JBrowse plugin.  

For general information on Apollo, go to [http://genomearchitect.org/](http://genomearchitect.org/).

Questions / Comments / Community contact can be sent to our [Apollo user mailing list](mailto:apollo@lists.lbl.gov). To subscribe send [this email to our mail server](mailto:sympa@lists.lbl.gov?subject=subscribe apollo) or review additional [sympa commands](http://www.sympa.org/manual/sympa-commands). Old questions are [archived on Nabble](http://gmod.827538.n3.nabble.com/Apollo-f815553.html).

Complete Apollo installation and configuration instructions are available from the [Apollo documentation pages](http://genomearchitect.readthedocs.io/en/latest/)

The Apollo client is implemented as a plugin for [JBrowse](http://jbrowse.org).  Additional JBrowse plugins may be found in the [JBrowse registry](https://gmod.github.io/jbrowse-registry/) and configured in ```apollo-config.groovy```.

[Planning Board](https://waffle.io/GMOD/Apollo?milestone=2.3)


## Setup guide

[Setup guide](docs/Setup.md) for deploying on production and [custom configuration guide](docs/Configure.md).  
Launchable public Amazon EC2 images may also be found in most regions under the name 'Apollo' as well as [instructions for docker](docs/Setup.md#configure-for-docker). 

The [quick-start guide for developers](docs/Apollo2Build.md) shows how to easily get started with Apollo. 


## Migrating data from older versions

You can follow steps in our [migration guide](https://github.com/GMOD/Apollo/blob/master/docs/Migration.md) to move annotations and data from older versions.

### Note about data directories

Apollo 2.0 allows you to add multiple data directories to your webapp, and it expects the data directories to be stored
outside of the tomcat webapps directory. Use the WA2.0 [quick-start guide](docs/Apollo2Build.md) to learn how to add new
data directories for your organisms.


**Important Note: All data from a webapps directory will disappear when doing tomcat "undeploy" operations, even if
it is a symlink.**.


### Launch Apollo in a temporary server

To launch Apollo with temporary settings, use the `apollo run-local` command, which will initialize your server
automatically with an H2 (zero-configuration) database.
 
    apollo run-local 8080

It will also use your custom settings if an apollo-config.groovy file has been setup.

### Generate a war file

Users can generate a war file (for example target/apollo-1.0.2.war) that will be copied into their tomcat webapps
directory for production deployments:

  apollo deploy 

Note: make sure to create an apollo-config.groovy file following the sample data (e.g.
sample-postgres-apollo-config.groovy) to make sure you use your preferred database settings.


### Run locally for GWT development

    apollo devmode 
   

### Thanks to
[![IntelliJ](https://lh6.googleusercontent.com/--QIIJfKrjSk/UJJ6X-UohII/AAAAAAAAAVM/cOW7EjnH778/s800/banner_IDEA.png)](
http://www.jetbrains.com/idea/index.html)

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/) 

Thanks to YourKit for providing us the use of their YourKit Java Profiler.  YourKit provides great supports for Open Source Projects.

[![Waffle.io - Columns and their card count](https://badge.waffle.io/GMOD/Apollo.svg?columns=all)](https://waffle.io/GMOD/Apollo)


