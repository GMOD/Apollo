# Developer guide

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Developer.md">On GitHub</a>

In the [build guide](Build.md) we discussed briefly how to create a build package.
If you are a developer and have a custom setup of WebApollo or JBrowse, then you can use this guide to compile your own release package.

## Pre-requisites for developers
You will need the system [pre-requisites](Prerequisites.md) as well as some some additional dependencies for the javascript compilation (NodeJS).

    # install nodejs (debian/ubuntu)
    sudo apt-get install git nodejs
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

This will automatically call javascript minimization scripts and create a release package of jbrowse in src/main/webapp/jbrowse and the compiled WAR file in target/apollo-2.0.2.war (for example).

If you make changes to the javascript, you will probably need to re-run "apollo release".

