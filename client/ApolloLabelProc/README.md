ApolloLabelProc is a JBrowse plugin.
Update location bar with 'label' (if it exists), when the locationBar content is JSON, which is expected from Apollo.
Install / Activate:

For JBrowse 1.11.6+, copy the ApolloLabelProc directory to the 'plugins' directory.
Add this to appropriate trackList.json under the plugins section (create one if it doesn't exist):

   "plugins": [ 
        "ApolloLabelProc"
    ],

For Apollo 2.x, copy the ApolloLabelProc directory to the web-apps/jbrowse/plugins directory.
Add this to web-apps/jbrowse/plugins/WebApollo/json/annot.json:

    "plugins" : [
      {
         "location" : "./plugins/WebApollo",
         "name" : "WebApollo"
      },
	  {
		 "location" : "./plugins/ApolloLabelProc",
		 "name" : "ApolloLabelProc"
	  }
   ],
