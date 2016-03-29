/*
 * ApolloLabelProc 
 * Update location bar with label (if it exists), when the locationBar content is JSON, which is expected from Apollo.
 */

define([
           'dojo/_base/declare',
           'dojo/_base/lang',
           'dojo/Deferred',
           'JBrowse/Plugin',
           'JBrowse/Util'
       ],
       function(
           declare,
           lang,
           Deferred,
           JBrowsePlugin,
           Util        
       ) {
return declare( JBrowsePlugin,
{
    constructor: function( args ) {
        console.log("plugin: ApolloLabelProc");
        var counter = 0 ;

        var thisB = this;
        
        // this traps the event that happens directly after onCoarseMove function, where the label gets updates.
        dojo.subscribe("/jbrowse/v1/n/navigate", function(currRegion){
            var locationStr = Util.assembleLocStringWithLength( currRegion );
            //console.log("locationStr="+locationStr);

            // is locationStr JSON?
            if (locationStr.charAt(0)=='{') {
                locationStr = locationStr.substring(0,locationStr.lastIndexOf('}')+1);
                var obj = JSON.parse(locationStr);
                
                // look for the "label" property
                if(obj.hasOwnProperty('label')) {
                    //console.log("label="+obj.label);
                    
                    // if( thisB.browser.locationBox ){
                    //     // thisB.browser.locationBox.set('value',obj.label, false);
                    //     // dojo.style(dojo.byId('widget_location'), "display", "none");
                    //     dojo.style(dojo.byId('widget_location'), "width", "0");
                    // }

                    // update the id=location-box if it exists
                    //var node = dojo.byId("location-info");
                    //if (node) {
                    //    html.set(node, obj.label);
                    //    thisB.browser.locationBox.set('value',"", false);
                    //}
                    // dojo.style(dojo.byId('search-box'), "display", "none");
                    // borderContainer.resize();
                    dojo.addOnLoad(function() {
                        // console.log(borderContainer);

                        if(counter==0){
                            var searchBox = dojo.byId('search-box');
                            dojo.style(searchBox, "display", "none");
                            var borderContainer = dijit.byId('GenomeBrowser');
                            borderContainer.resize();
                        }
                        counter = 1 ;

                        // dojo.style(dojo.byId('search-refseq'), "display", "none");
                        // dojo.style(searchBox, "width", "0");
                        // searchBox.resize();
                        // var navbox = dojo.byId('navbox');
                        // navbox.resize();
                    });

                }
            }
            
        });        
    }
});
});