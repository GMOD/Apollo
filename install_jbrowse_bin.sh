#!/bin/bash


# recommended install jbrowse bins to the system
# installs using cpan
# can also install using cpanm
# may need to be run as sudo
CURRENT=`pwd`
: ${APOLLO_ROOT_DIRECTORY:=`pwd`}
: ${APOLLO_BUILD_DIRECTORY:="$APOLLO_ROOT_DIRECTORY/build"}
: ${APOLLO_WEBAPP_DIRECTORY:="$APOLLO_ROOT_DIRECTORY/web-app"}
: ${APOLLO_JBROWSE_DIRECTORY:="$APOLLO_WEBAPP_DIRECTORY/jbrowse"}
if [ ! -d $APOLLO_JBROWSE_DIRECTORY ]; then
    echo "JBrowse has not been built yet. Please build jbrowse first with build.sh"
    exit 0
fi

cd $APOLLO_JBROWSE_DIRECTORY
if [[ $1 == cpanm ]]; then
    #allow installation via cpanm
    cpanm XML::DOM XML::Parser XML::Parser::PerlSAX
    cpanm --force Heap::Simple::XS
    cpanm .
else
    #install to system. also, force install Heap::Simple::XS due to failure on newer perl versions
    cpan XML::DOM XML::Parser XML::Parser::PerlSAX
    perl -MCPAN -e 'force install Heap::Simple::XS' 
    cpan .
fi
cd $CURRENT

