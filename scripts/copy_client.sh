#!/bin/bash
if [ -f web-app/jbrowse/index.html ]; then 
	rm -rf web-app/jbrowse/plugins/ApolloLabelProc
	rm -rf web-app/jbrowse/plugins/WebApollo
	rm -rf web-app/jbrowse/src/JBrowse
	cp -r jbrowse-download/src/JBrowse web-app/jbrowse/src/JBrowse
	cp -r client/apollo web-app/jbrowse/plugins/WebApollo
	cp -r client/ApolloLabelProc web-app/jbrowse/plugins/ApolloLabelProc
    echo "Apollo client installed" ;
else
    echo "ERROR!!!!: JBrowse not installed, can not install client." ; 
fi


