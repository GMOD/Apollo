define( [
            "dojo/_base/declare",
            "dojo/_base/array",
            "NeatHTMLFeatures/View/Track/NeatFeatures",
            "WebApollo/View/Track/DraggableBLASTFeatures"
        ],
    function( declare,
        array,
        NeatFeatureTrack,
        BLASTFeatureTrack
        ) {
var draggableTrack = declare( [NeatFeatureTrack,BLASTFeatureTrack],{
    constructor: function(args)  {  }

});

    return draggableTrack;
});
