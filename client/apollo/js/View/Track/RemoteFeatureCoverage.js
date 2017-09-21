define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'WebApollo/View/Track/Wiggle/RemoteXYPlot',
        'JBrowse/Util',
        'JBrowse/Store/SeqFeature/Coverage'
    ],
    function(
        declare,
        array,
        RemoteWiggleXYPlotTrack,
        Util,
        CoverageStore
    ) {

        return declare( RemoteWiggleXYPlotTrack, {

            constructor: function( args ) {
                this.store = new CoverageStore( { store: this.store, browser: this.browser });
            },

            _defaultConfig: function() {
                return Util.deepUpdate(
                    dojo.clone( this.inherited(arguments) ),
                    {
                        autoscale: 'local'
                    }
                );
            }
        });
});