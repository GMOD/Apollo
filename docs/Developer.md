Developer guide
-----------------

In the [build guide](Build.md) we discussed briefly how to build the release package
using a pre-compiled genome browser package. If you are a developer and have a
custom setup of WebApollo or JBrowse, then you can use this guide to compile your own release package.

You will need the system [pre-requisites](Prerequisites.md) as well as some
some additional dependencies for the javascript compilation (NodeJS). The easiest way
to get this on ubuntu is nodejs-legacy, and on centos, from npm in the epel-release repository.

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

Now we can compile a new client with JBrowse and the WebApollo plugin.

    make clean release package

This step will do the following

 - Remove any old packages
 - Automatically download jbrowse from github
 - Run the required build scripts to compile the javascript 
 - Build a WAR package file ready for deployment
 
Note: If you have your own custom jbrowse repository, you can use it by custom environmental variables on the command line, e.g.:

    make clean release package JBROWSE_GIT_DIRECTORY=/home/devel/jbrowse_repo/

This will use your custom jbrowse repository instead of taking the default action of cloning from https://github.com/GMOD/jbrowse.git

Some other options that are available for the Makefile

     # Build options using pre-compiled client
     make download-debug    # download the latest pre-compiled debug client
     make download-release  # download the latest pre-compiled release client (minified javascript)

     # Custom build options to compile client
     make download-jbrowse  # download jbrowse from github (this is done automatically unless JBROWSE_GITHUB_FOLDER)
     make release           # compile the jbrowse+webapollo client with nodejs
     make debug             # compile the jbrowse+webapollo client with nodejs
     make unoptimized       # create unoptimized jbrowse+webapollo  (no compilation step)

     # Tests
     make test              # run the command line tests for webapollo
     make test-jbrowse      # run the command line tests for jbrowse (requires extra perl pre-requisites)
