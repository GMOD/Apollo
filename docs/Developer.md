Developer guide
-----------------

In the [build guide](Build.md) we discussed briefly how to build the release package
using a pre-compiled genome browser package. If you are a developer and have a
custom setup of WebApollo of JBrowse then you can use supply extra options to compile your own release package.

You will need some of the system [pre-requisites](Prerequisites.md) as well as some
some additional dependencies for the javascript compilation (NodeJS). The easiest way
to get this on ubuntu is nodejs-legacy, and on centos, from the epel-release repository.

    # install system prerequisites (debian/ubuntu)
    sudo apt-get install git nodejs-legacy
    # install system prerequisites (centOS/redhat)
    sudo yum install epel-release
    sudo yum install git npm
    # install system prerequisites (macOSX/homebrew), read the postgresql start guide
    brew install maven node


We will also need these two extra perl packages

    # setup cpanm and install jbrowse and webapollo perl prerequisites
    cpanm DateTime Text::Markdown


Now we can try to compile a new client package.

    make clean release package

This step will do the following

 - automatically clone jbrowse from github
 - copy webapollo's plugin code to the jbrowse plugins folder
 - run the required build scripts to compress the javascript
 
 
Note: If you have your own custom jbrowse repository, you can use it by custom environmental variables on the command line, e.g.:


    make clean release package APOLLO_JBROWSE_GIT=/home/you/jbrowse

This will use your custom jbrowse instead of the default action of cloning the https://github.com/GMOD/jbrowse.git

Some other options that are available for the Makefile

     # Build options using pre-compiled client
     make download-debug    # download the latest pre-compiled debug client
     make download-release  # download the latest pre-compiled release client (minified javascript)

     # Custom build options to compile client
     make download-jbrowse  # download jbrowse (and dependencies from submodules) from github. this automatically done if APOLLO_JBROWSE_GIT is not set. 
     make release           # compile the jbrowse+webapollo client manually
     make debug             # compile the jbrowse+webapollo client manually
     make unoptimized       # create unoptimized jbrowse+webapollo  (no compilation step, no nodejs required)

     # Tests
     make test              # run the command line tests for webapollo
     make test-jbrowse      # run the command line tests for jbrowse (requires extra perl pre-requisites)
