<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>JBrowse</title>
    <link rel="stylesheet" type="text/css" href="css/genome.css">

    <asset:javascript src="spring-websocket"/>
    <script type="text/javascript" language="javascript" src="../annotator/annotator.nocache.js"></script>
    <script>
        var rootUrl= '${applicationContext.servletContext.getContextPath()}';
        var Options = {
            showFrame: '${params.showFrame  && params.showFrame == 'true' ? 'true' : 'false' }'
            , userId: '${userKey}'
            , clientToken: '${clientToken}'
//            ,top: "10"
//            ,topUnit: "PCT" // PX, EM, PC, PT, IN, CM
//            ,height: "80"
//            ,heightUnit: "PCT" // PX, EM, PC, PT, IN, CM
        };
//    </script>
    <script type="text/javascript">
        // jshint unused: false
        var dojoConfig = {
            async: true,
            baseUrl: './src',
            has: {
                'host-node': false // Prevent dojo from being fooled by Electron
            }
        };
        // Move Electron's require out before loading Dojo
        if(window.process&&process.versions&&process.versions.electron) {
            window.electronRequire = require;
            delete window.require;
        }
    </script>
    <script type="text/javascript" src="src/dojo/dojo.js"></script>
    <script type="text/javascript" src="src/JBrowse/init.js"></script>
    <script type="text/javascript">
        window.onerror=function(msg){
            if( document.body )
                document.body.setAttribute("JSError",msg);
        }

        // puts the main Browser object in this for convenience.  feel
        // free to move it into function scope if you want to keep it
        // out of the global namespace
        var JBrowse;
        require(['JBrowse/Browser', 'dojo/io-query', 'dojo/json' ],
            function (Browser,ioQuery,JSON) {
                // the initial configuration of this JBrowse
                // instance

                // NOTE: this initial config is the same as any
                // other JBrowse config in any other file.  this
                // one just sets defaults from URL query params.
                // If you are embedding JBrowse in some other app,
                // you might as well just set this initial config
                // to something like { include: '../my/dynamic/conf.json' },
                // or you could put the entire
                // dynamically-generated JBrowse config here.

                // parse the query vars in the page URL
                var queryParams = ioQuery.queryToObject( window.location.search.slice(1) );

                var config = {
                    containerID: "GenomeBrowser",

                    dataRoot: queryParams.data,
                    queryParams: queryParams,
                    location: queryParams.loc,
                    forceTracks: queryParams.tracks,
                    initialHighlight: queryParams.highlight,
                    show_nav: queryParams.nav,
                    show_tracklist: queryParams.tracklist,
                    show_overview: queryParams.overview,
                    show_menu: queryParams.menu,
                    show_tracklabels: queryParams.tracklabels,
                    highResolutionMode: queryParams.highres,
                    stores: { url: { type: "JBrowse/Store/SeqFeature/FromConfig", features: [] } },
                    bookmarks: { },
                    makeFullViewURL: function( browser ) {

                        // the URL for the 'Full view' link
                        // in embedded mode should be the current
                        // view URL, except with 'nav', 'tracklist',
                        // and 'overview' parameters forced to 1.

                        return browser.makeCurrentViewURL({ nav: 1, tracklist: 1, overview: 1 });
                    },
                    updateBrowserURL: true
                };

                //if there is ?addFeatures in the query params,
                //define a store for data from the URL
                if( queryParams.addFeatures ) {
                    config.stores.url.features = JSON.parse( queryParams.addFeatures );
                }

                // if there is ?addTracks in the query params, add
                // those track configurations to our initial
                // configuration
                if( queryParams.addTracks ) {
                    config.tracks = JSON.parse( queryParams.addTracks );
                }

                // if there is ?addBookmarks, add those to configuration
                if( queryParams.addBookmarks ) {
                    config.bookmarks.features = JSON.parse( queryParams.addBookmarks );
                }

                // if there is ?addStores in the query params, add
                // those store configurations to our initial
                // configuration
                if( queryParams.addStores ) {
                    config.stores = JSON.parse( queryParams.addStores );
                }

                // create a JBrowse global variable holding the JBrowse instance
                JBrowse = new Browser( config );
            });
        alert('booh');
    </script>

</head>

<body>
<div class="jbrowse jbrowsecontainer" id="GenomeBrowser" style="height: 100%; width: 50%; padding: 0; border: 0;"></div>
<div id="ApolloPanel" style="width: 50%;height: 100%"></div>
</body>
</html>

