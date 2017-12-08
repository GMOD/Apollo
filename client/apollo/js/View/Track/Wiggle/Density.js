define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/_base/lang',
        'JBrowse/Util',
        'WebApollo/ProjectionUtils',
        'JBrowse/View/Track/Wiggle/Density'
    ],
    function(
        declare,
        array,
        lang,
        Util,
        ProjectionUtils,
        DensityTrack
    ) {

        return declare( DensityTrack, {


            /**
             * Override _getBlockFeatures
             */
            _getBlockFeatures: function (args) {
                var thisB = this;
                var blockIndex = args.blockIndex;
                var block = args.block;

                var leftBase = args.leftBase;
                var rightBase = args.rightBase;
                var refSeqName = this.refSeq.name ;

                var scale = args.scale;
                var finishCallback = args.finishCallback || function () {
                    };

                var canvasWidth = this._canvasWidth(args.block);

                var features = [];

                var errorCallback = dojo.hitch(this, function(e) {
                    this._handleError(e, args);
                    finishCallback(e);
                });

                this.getFeatures(
                    {
                        ref: refSeqName,
                        basesPerSpan: 1 / scale,
                        scale: scale,
                        start: leftBase,
                        end: rightBase + 1
                    },

                    function (f) {
                        if (thisB.filterFeature(f)){
                            f = ProjectionUtils.projectJSONFeature(f,refSeqName);
                            features.push(f);
                        }
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
            },


        });
    });