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

            _defaultConfig: function() {
                return Util.deepUpdate(
                    dojo.clone( this.inherited(arguments) ),
                    {
                        maxExportSpan: 500000,
                        style: {
                            height: 31,
                            pos_color: '#00f',
                            neg_color: '#f00',
                            bg_color: 'rgba(230,230,230,0.6)',
                            clip_marker_color: 'black'
                        }
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

            _calculatePixelScores: function( canvasWidth, features, featureRects ) {
                var scoreType = this.config.scoreType;
                var pixelValues = new Array( canvasWidth );
                if(!scoreType||scoreType=="maxScore") {
                    // make an array of the max score at each pixel on the canvas
                    dojo.forEach( features, function( f, i ) {
                        var store = f.source;
                        var fRect = featureRects[i];
                        var jEnd = fRect.r;
                        var score = f.get(scoreType)||f.get('score');
                        for( var j = Math.round(fRect.l); j < jEnd; j++ ) {
                            if ( pixelValues[j] && pixelValues[j]['lastUsedStore'] == store ) {
                                /* Note: if the feature is from a different store, the condition should fail,
                                 *       and we will add to the value, rather than adjusting for overlap */
                                pixelValues[j]['score'] = Math.max( pixelValues[j]['score'], score );
                            }
                            else if ( pixelValues[j] ) {
                                pixelValues[j]['score'] = pixelValues[j]['score'] + score;
                                pixelValues[j]['lastUsedStore'] = store;
                            }
                            else {
                                pixelValues[j] = { score: score, lastUsedStore: store, feat: f };
                            }
                        }
                    },this);
                    // when done looping through features, forget the store information.
                    for (var i=0; i<pixelValues.length; i++) {
                        if ( pixelValues[i] ) {
                            delete pixelValues[i]['lastUsedStore'];
                        }
                    }
                }
                else if(scoreType=="avgScore") {
                    // make an array of the average score at each pixel on the canvas
                    dojo.forEach( features, function( f, i ) {
                        var store = f.source;
                        var fRect = featureRects[i];
                        var jEnd = fRect.r;
                        var score = f.get('score');
                        for( var j = Math.round(fRect.l); j < jEnd; j++ ) {
                            // bin scores according to store
                            if ( pixelValues[j] && store in pixelValues[j]['scores'] ) {
                                pixelValues[j]['scores'][store].push(score);
                            }
                            else if ( pixelValues[j] ) {
                                pixelValues[j]['scores'][store] = [score];
                            }
                            else {
                                pixelValues[j] = { scores: {}, feat: f };
                                pixelValues[j]['scores'][store] = [score];
                            }
                        }
                    },this);
                    // when done looping through features, average the scores in the same store then add them all together as the final score
                    for (var i=0; i<pixelValues.length; i++) {
                        if ( pixelValues[i] ) {
                            pixelValues[i]['score'] = 0;
                            for ( var store in pixelValues[i]['scores']) {
                                var j, sum = 0, len = pixelValues[i]['scores'][store].length;
                                for (j = 0; j < len; j++) {
                                    sum += pixelValues[i]['scores'][store][j];
                                }
                                pixelValues[i]['score'] += sum / len;
                            }
                            delete pixelValues[i]['scores'];
                        }
                    }
                }
                return pixelValues;
            }

        });
    });