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


With this setup, we can try to compile the jbrowse+webapollo client package.

    make clean release package

This step will automatically clone jbrowse from github, copy WebApollo to the plugins folder, and run the required build scripts. If you have your own custom jbrowse repository, you can also set that using some custom environmental variables:


    make clean release package APOLLO_JBROWSE_GIT=/home/you/jbrowse

This will use your custom jbrowse instead of the default action of cloning the https://github.com/GMOD/jbrowse.git


