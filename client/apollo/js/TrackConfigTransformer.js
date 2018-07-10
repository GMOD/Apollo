/**
 *  TrackConfigTransformer takes JBrowse track.config object and, if needed, 
 *      modifies in place to use WebApollo-specific track types, etc.
 */
define( [ 'dojo/_base/declare' ],
        function( declare ) {

return declare( null, {


constructor: function( args )  {
   
    this.transformers=[];
    var browser=args.browser;
    this.overridePlugins=browser.config.overridePlugins;
    this.transformers["JBrowse/View/Track/HTMLFeatures"] = function(trackConfig) {
        trackConfig.type = "WebApollo/View/Track/DraggableHTMLFeatures";
    };

    this.transformers["JBrowse/View/Track/CanvasFeatures"] = function(trackConfig) {
        trackConfig.type = "WebApollo/View/Track/WebApolloCanvasFeatures";
    };

    this.transformers["JBrowse/View/Track/HTMLVariants"] = function(trackConfig) {
        trackConfig.type = "WebApollo/View/Track/DraggableHTMLVariants";
    };

    this.transformers["JBrowse/View/Track/CanvasVariants"] = function(trackConfig) {
        trackConfig.type = "WebApollo/View/Track/WebApolloCanvasVariants";
    };

    this.transformers["JBrowse/View/Track/Sequence"] = function(trackConfig) {
        trackConfig.type = "WebApollo/View/Track/AnnotSequenceTrack";
        trackConfig.storeClass = "WebApollo/Store/SeqFeature/ScratchPad";
        trackConfig.style = { className: "{type}", 
                              uniqueIdField : "id" };
        trackConfig.compress = 0;
        trackConfig.subfeatures = 1;
    };
    
    this.transformers["JBrowse/View/Track/Alignments"] = function(trackConfig) {
        if(!trackConfig.overrideDraggable&&!browser.config.overrideDraggable) {
            trackConfig.type = "WebApollo/View/Track/DraggableAlignments";
        }
    };

    this.transformers["JBrowse/View/Track/Alignments2"] = function(trackConfig) {
        trackConfig.type = "WebApollo/View/Track/WebApolloAlignments2";
    };

},

transform: function(trackConfig) {
    if (trackConfig.overridePlugins||this.overridePlugins) {
        return;
    }
    if (this.transformers[trackConfig.type]) {
        var transformer = this.transformers[trackConfig.type];
        transformer(trackConfig);
    }
}
                    
});

});
