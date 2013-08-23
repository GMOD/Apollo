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
        // console.log("in TrackConfigTransformer: track " + trackConfig.label + ", changing type to: " + trackConfig.type);
    };

    this["JBrowse/View/Track/Sequence"] = function(trackConfig) {
        // console.log("transforming Sequence track");
        trackConfig.type = "WebApollo/View/Track/AnnotSequenceTrack";
        trackConfig.storeClass = "WebApollo/Store/SeqFeature/ScratchPad";
        trackConfig.style = { className: "{type}", 
                              uniqueIdField : "id" };
        trackConfig.compress = 0;
        trackConfig.subfeatures = 1;
    };
    
    this["JBrowse/View/Track/Alignments"] = function(trackConfig) {
        trackConfig.type = "WebApollo/View/Track/DraggableAlignments";
    };

    this["JBrowse/View/Track/Alignments2"] = this["JBrowse/View/Track/Alignments"];

},

transform: function(trackConfig) {
    if (trackConfig.overridePlugins) {
        return;
    }
    if (this[trackConfig.type]) {
        var transformer = this[trackConfig.type];
        transformer(trackConfig);
    }
}
                    
});

});
