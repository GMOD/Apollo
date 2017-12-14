define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'WebApollo/View/Track/Wiggle/XYPlot',
        'JBrowse/Util',
        'WebApollo/Store/SeqFeature/Coverage'
    ],
    function(
        declare,
        array,
        XYPlot,
        Util,
        CoverageStore
    ) {

        return declare( XYPlot, {

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