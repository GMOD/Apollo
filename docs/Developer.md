# Developer guide


In the [build guide](Build.md) we discussed briefly how to create a build package.
If you are a developer and have a custom setup of WebApollo or JBrowse, then you can use this guide to compile your own release package.

## Pre-requisites for developers
You will need the system [pre-requisites](Prerequisites.md) as well as some
some additional dependencies for the javascript compilation (NodeJS). The easiest way
to get NodeJS on ubuntu is nodejs-legacy, and on centos, from npm in the epel-release repository.

    # install nodejs (debian/ubuntu)
    sudo apt-get install git nodejs-legacy
    # install nodejs (centOS/redhat)
    sudo yum install epel-release
    sudo yum install git npm
    # install nodejs (macOSX/homebrew)
    brew install node

We will also need two extra perl packages

    # Used to make the build process smoother
    cpanm DateTime Text::Markdown

## Makefile based compilation of the client-side code

Web Apollo uses a Makefile to compile JBrowse along with the Web Apollo client. Example:

    make clean release package

This step will do the following

 * Clean any old builds
 * Automatically downloads jbrowse from github
 * Run the required build scripts to compile the JBrowse and WebApollo javascript
 * Build a WAR package file ready for deployment using Maven
 
Note: If you have your own custom jbrowse repository, you can use it by specifying JBROWSE_GIT_DIRECTORY on the command line, e.g.:

    make clean release JBROWSE_GIT_DIRECTORY=/home/devel/jbrowse_repo/

This will use your custom jbrowse repository instead of taking the default action of cloning from [https://github.com/GMOD/jbrowse.git]().

Here are some other options that are available for the Makefile

     # Custom build options to compile client
     make release           # compile the jbrowse+webapollo client with nodejs
     make debug             # compile the jbrowse+webapollo client with nodejs
     make unoptimized       # create unoptimized jbrowse+webapollo build (no compilation step)

     # Tests
     make test              # run the command line tests for webapollo
     make test-jbrowse      # run the command line tests for jbrowse (requires extra perl pre-requisites)

Once the client is compiled, you can use `mvn package` to complete the custom build
