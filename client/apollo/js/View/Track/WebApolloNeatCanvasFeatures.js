define( [
            'dojo/_base/declare',
            'dojo/_base/array',
            'NeatCanvasFeatures/View/Track/NeatFeatures',
            'WebApollo/View/Track/WebApolloCanvasFeatures'
        ],
        function( declare,
            array,
            NeatFeatureTrack,
            CanvasFeaturesTrack
        )
{

return declare( [NeatFeatureTrack,CanvasFeaturesTrack], {
    constructor: function() {
        this.browser.getPlugin( 'WebApollo', dojo.hitch( this, function(p) {
            this.webapollo = p;
        }));
    }
});


});

