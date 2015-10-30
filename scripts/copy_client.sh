#!/bin/bash
if [ -f web-app/jbrowse/index.html ]; then 
	rm -rf web-app/jbrowse/plugins/WebApollo
	cp -r client/apollo web-app/jbrowse/plugins/WebApollo
    echo "Apollo client installed" ;
else
    echo "ERROR!!!!: JBrowse not installed, can not install client." ; 
fi


