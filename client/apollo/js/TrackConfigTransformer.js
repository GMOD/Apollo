/**
 *  TrackConfigTransformer takes JBrowse track.config object and, if needed, 
 *      modifies in place to use WebApollo-specific track types, etc.
 */
define( [ 'dojo/_base/declare' ],
        function( declare ) {

return declare( null, {

constructor: function()  {
    console.log("in TrackConfigTransformer constructor");
    
    this["JBrowse/View/Track/HTMLFeatures"] = function(trackConfig) {
        trackConfig.type = "WebApollo/View/Track/DraggableHTMLFeatures"; 
        console.log("in TrackConfigTransformer: track " + trackConfig.label + ", changing type to: " + trackConfig.type);
    };

},

transform: function(trackConfig) {
    if (this[trackConfig.type]) {
        var transformer = this[trackConfig.type];
        transformer(trackConfig);
    }
}
                    
});

});
