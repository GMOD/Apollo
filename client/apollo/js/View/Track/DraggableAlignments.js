define([
           'dojo/_base/declare', 
           'dojo/_base/array',
           'JBrowse/View/Track/Alignments',
           'WebApollo/View/Track/DraggableHTMLFeatures'
       ],
       function(
           declare,
           array, 
           AlignmentsTrack,
           DraggableTrack
       ) {

return declare([ DraggableTrack, AlignmentsTrack ], {

    constructor: function( args )  {
    // forcing store to create subfeatures, unless config.subfeatures explicitly set to false
    //     default is set to true in _defaultConfig()
        if (this.config.style.showSubfeatures) { this.store.createSubfeatures = true; }
    }

} );

});
