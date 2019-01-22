
define( [
            'dojo/_base/declare',
            'dojo/_base/array',
            'NeatHTMLFeatures/View/Track/NeatFeatures',
            'WebApollo/View/Track/DraggableHTMLFeatures'
        ],
    function( declare,
        array,
        NeatFeatureTrack,
        HTMLFeatureTrack
        ) {


var draggableTrack = declare( [NeatFeatureTrack,HTMLFeatureTrack],{
    constructor: function(args)  {  }

});

    return draggableTrack;
});

