define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/_base/lang',
        'JBrowse/Util',
        'WebApollo/JSONUtils',
        'JBrowse/View/Track/Wiggle/XYPlot',
        'JBrowse/View/Track/Wiggle/_Scale'
    ],

    function(
        declare,
        array,
        lang,
        Util,
        JSONUtils,
        XYPlotTrack,
        Scale
    ) {

    return declare( XYPlotTrack, {

        _defaultConfig: function() {
            return Util.deepUpdate(
                dojo.clone( this.inherited(arguments) ),
                {
                    style: {
                        pos_color: 'blue',
                        neg_color: 'red',
                        origin_color: '#888',
                        variance_band_color: 'rgba(0,0,0,0.3)'
                    },
                    autoscale: 'global',
                    variance_band: 1,
                    logScaleOption: true
                }
            );
        },

        /**
         * Override _getBlockFeatures
         */
        _getBlockFeatures: function (args) {
            var thisB = this;
            var blockIndex = args.blockIndex;
            var block = args.block;

            var leftBase = args.leftBase;
            var rightBase = args.rightBase;

            var scale = args.scale;
            var finishCallback = args.finishCallback || function () {};

            var canvasWidth = this._canvasWidth(args.block);

            var features = [];

            var errorCallback = dojo.hitch(this, function (e) {
                this._handleError(e, args);
                finishCallback(e);
            });

            var sequenceList = JSONUtils.parseSequenceList(this.refSeq.name);
            if (sequenceList[0].reverse) {
                errorCallback(JSONUtils.NOT_YET_SUPPORTED_MESSAGE)
            }
            else {
                var refSeqName = sequenceList[0].name;
                this.getFeatures(
                    {
                        ref: refSeqName,
                        basesPerSpan: 1 / scale,
                        scale: scale,
                        start: leftBase,
                        end: rightBase + 1
                    },

                    function (f) {
                        if (thisB.filterFeature(f))
                            features.push(f);
                    },
                    dojo.hitch(this, function (args) {

                        // if the block has been freed in the meantime,
                        // don't try to render
                        if (!(block.domNode && block.domNode.parentNode ))
                            return;

                        var featureRects = array.map(features, function (f) {
                            return this._featureRect(scale, leftBase, canvasWidth, f);
                        }, this);

                        block.features = features;
                        block.featureRects = featureRects;
                        block.pixelScores = this._calculatePixelScores(this._canvasWidth(block), features, featureRects);

                        if (args && args.maskingSpans)
                            block.maskingSpans = args.maskingSpans; // used for masking

                        finishCallback();
                    }),
                    errorCallback
                );
            }
        },

        /**
         * Override _getScalingStats
         */
        _getScalingStats: function( viewArgs, callback, errorCallback ) {
            if( ! Scale.prototype.needStats( this.config ) ) {
                callback( null );
                return null;
            }
            else if( this.config.autoscale == 'local' ) {
                var region = lang.mixin( { scale: viewArgs.scale }, this.browser.view.visibleRegion() );
                region.ref = JSONUtils.parseSequenceList(region.ref)[0].name;
                region.start = Math.ceil( region.start );
                region.end = Math.floor( region.end );
                return this.getRegionStats.call( this, region, callback, errorCallback );
            }
            else {
                return this.getGlobalStats.call( this, callback, errorCallback );
            }
        }
    });
});