# Developer guide

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Developer.md">On GitHub</a>

In the [build guide](Build.md) we discussed briefly how to create a build package.
If you are a developer and have a custom setup of WebApollo or JBrowse, then you can use this guide to compile your own release package.

## Pre-requisites for developers
You will need the system [pre-requisites](Prerequisites.md) as well as some some additional dependencies for the javascript compilation (NodeJS).

The easiest way to get NodeJS on ubuntu is nodejs-legacy. On centos, it's easiest to use npm in the epel-release repository.

    # install nodejs (debian/ubuntu)
    sudo apt-get install git nodejs-legacy
    # install nodejs (centOS/redhat)
    sudo yum install epel-release
    sudo yum install git npm
    # install nodejs (macOSX/homebrew)
    brew install node

We will also need two extra perl packages to run the build

    # Used to make the build process smoother
    cpanm DateTime Text::Markdown


## Ant based compilation of the client-side code

The current preferred method for building a release is using the apollo script

    apollo release

This will automatically call javascript minimization scripts and create a release package of jbrowse in src/main/webapp/jbrowse and the compiled WAR file in target/apollo-1.0.2.war (for example).

If you make changes to the javascript, you will probably need to delete src/main/webapp/jbrowse and recompile the release with "apollo release". Other changes can be simply updated using "apollo deploy" which will update the war file.


## Makefile based compilation of the client-side code

As an alternative to the ant/maven workflow, Web Apollo client side code can be compiled using the Makefile which can be used for multiple Web Apollo deployments. Example:

    make -f build/Makefile create-precompiled

This step will do the following

 * Clean any old builds
 * Download jbrowse from github
 * Compile and minimize the JBrowse and WebApollo javascript files
 * Output two packages, a release and debug package
 
Note: you can set your own JBrowse repo using JBROWSE_GIT_DIRECTORY on the command line if you don't want the Makefile to download jbrowse, e.g.:

    make -f build/Makefile create-precompiled JBROWSE_GIT_DIRECTORY=/home/devel/jbrowse_repo/

This will use your custom jbrowse repository instead of taking the default action of cloning from [https://github.com/GMOD/jbrowse.git](http://github.com/GMOD/jbrowse.git).

Once the client is compiled, you can use `apollo deploy` or `apollo run` to complete the custom build.
See the [build guide](Build.md) for more details.
