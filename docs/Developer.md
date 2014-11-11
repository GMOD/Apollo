Developer guide
-----------------

In the [#Install|install guide] we discussed briefly how to setup the server
from scratch using some recommended setups. If you are a developer and have a
custom setup of WebApollo of JBrowse then you may need to take extra steps.
This is particularly true during the building of the javascript.

You will need the system pre-requisites discussed in the install guide, plus
some additional ones for creating the javascript compilation such as NodeJS
and the Perl packages Text::Markdown and DateTime.

    # install system prerequisites (debian/ubuntu)
    sudo apt-get install git nodejs-legacy
    # install system prerequisites (centOS/redhat)
    sudo yum install epel-release
    sudo yum install git npm
    # install system prerequisites (macOSX/homebrew), read the postgresql start guide
    brew install maven node

    # setup cpanm and install jbrowse and webapollo perl prerequisites
    cpanm DateTime Text::Markdown


Then we can try to produce compiled release versions of the codebase

    make clean release package

This will attempt to download the JBrowse codebase from github and then compile the WebApollo and JBrowse javascript into a compressed release package. The alternative option is to download a pre-compiled version using the "download-release" instead of the "release" target, but if you are making any code changes to the client, then recompiling this way will be necessary.

The Makefile also recognizes several variables including

    JBROWSE_GITHUB # if set, will use this repository as a clone (e.g. http://github.com/yourname/jbrowse.git)
    APOLLO_JBROWSE_GITHUB # if set, will use this folder as the jbrowse git clone (e.g. /home/you/jbrowse/, note that if this is set, then JBROWSE_GITHUB isn't necessary)
    JBROWSE_RELEASE # if set, then download-release target will download a pre-compiled package from this folder (e.g. http://someurl.com/webapollo_jbrowse_precompiled_package.zip)
    JBROWSE_DEBUG # if set, then download-debug target will download a pre-compiled package from this folder (e.g. http://someurl.com/webapollo_jbrowse_precompiled_package-debug.zip)


An example of using these is if you have a custom JBrowse git repository then you could use:

    make clean release package APOLLO_JBROWSE_GIT=/home/you/jbrowse

This will use your custom jbrowse instead of the default action of cloning the https://github.com/GMOD/jbrowse.git

