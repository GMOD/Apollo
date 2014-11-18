# Developer guide

<a href="https://github.com/GMOD/Apollo/blob/master/docs/Developer.md">On GitHub</a>

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

We will also need two extra perl packages to 

    # Used to make the build process smoother
    cpanm DateTime Text::Markdown

## Makefile based compilation of the client-side code

Web Apollo uses a Makefile to compile JBrowse along with the Web Apollo client. Example:

    make -f build/Makefile create-precompiled

This step will do the following

 * Clean any old builds
 * Automatically downloads jbrowse from github (unless JBROWSE_GIT_DIRECTORY is set)
 * Run the required build scripts to compile the JBrowse and WebApollo javascript files
 * Output two packages, a release and debug package
 
Note: you can set your own JBrowse repo using JBROWSE_GIT_DIRECTORY on the command line, e.g.:

    make -f build/Makefile clean release JBROWSE_GIT_DIRECTORY=/home/devel/jbrowse_repo/

This will use your custom jbrowse repository instead of taking the default action of cloning from [https://github.com/GMOD/jbrowse.git](http://github.com/GMOD/jbrowse.git).

Alternatively, a quick "unoptimized build" or other build options can be created using the Makefile

     # Custom build options to compile client
     make -f build/Makefile unoptimized      # create unoptimized jbrowse+webapollo build (no compilation step)
     make -f build/Makefile release          # compile jbrowse+webapollo build (with minified javascript)
     make -f build/Makefile debug            # compile jbrowse+webapollo build

Once the client is compiled, you can use `mvn package` to complete the custom build. See the [build guide](Build.md) for more details.
