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

        var thisB = this;
        
        // this traps the event that happens directly after onCoarseMove function, where the label gets updates.
        dojo.subscribe("/jbrowse/v1/n/navigate", function(currRegion){
            console.log("Trap Navigate");
            var locationStr = Util.assembleLocStringWithLength( currRegion );
            console.log("locationStr="+locationStr);

            // is locationStr JSON?
            if (locationStr.charAt(0)=='{') {
                locationStr = locationStr.substring(0,locationStr.lastIndexOf('}')+1);
                var obj = JSON.parse(locationStr);
                
                // look for the "label" property
                if(obj.hasOwnProperty('label')) {
                    console.log("label="+obj.label);
                    
                    if (typeof thisB.browser.config.locationBox !== 'undefined' && thisB.browser.config.locationBox==="searchBox") {
                        thisB.browser.locationBox.set('value',obj.label, false);
                    }
                    
                    // update the id=location-box if it exists
                    require(["dojo/html", "dojo/ready","dojo/fx","dojo/dom-style","dojo/domReady!"], 
                    function(html, ready,coreFx,style){
                        ready(function(){
                            var node = dojo.byId("location-info");  
                            if (node) {
                                html.set(node, obj.label);
                            }
                        });
                    });    
                }
            }
            
        });        
    }
});
});