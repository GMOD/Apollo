define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/_base/lang',
        'JBrowse/Util',
        'WebApollo/ProjectionUtils',
        'JBrowse/View/Track/Wiggle/Density',
        'JBrowse/View/Track/Wiggle/_Scale'
    ],
    function(
        declare,
        array,
        lang,
        Util,
        ProjectionUtils,
        DensityTrack,
        Scale
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

            /* If boolean track, mask accordingly */
            _maskBySpans: function( scale, leftBase, rightBase, block, canvas, pixels, dataScale, spans ) {
                var context = canvas.getContext('2d');
                var canvasHeight = canvas.height;
                context.fillStyle = this.config.style.mask_color || 'rgba(128,128,128,0.6)';
                this.config.style.mask_color = context.fillStyle;

                for ( var index in spans ) {
                    if (spans.hasOwnProperty(index)) {
                        var w = Math.ceil(( spans[index].end   - spans[index].start ) * scale );
                        var l = Math.round(( spans[index].start - leftBase ) * scale );
                        context.fillRect( l, 0, w, canvasHeight );
                        context.clearRect( l, 0, w, canvasHeight/3);
                        context.clearRect( l, (2/3)*canvasHeight, w, canvasHeight/3);
                    }
                }
                dojo.forEach( pixels, function(p,i) {
                    if (!p) {
                        // if there is no data at a point, erase the mask.
                        context.clearRect( i, 0, 1, canvasHeight );
                    }
                });
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
                var finishCallback = args.finishCallback || function () {
                    };

                var canvasWidth = this._canvasWidth(args.block);

                var features = [];

                var errorCallback = dojo.hitch(this, function(e) {
                    this._handleError(e, args);
                    finishCallback(e);
                });

                // var sequenceList = ProjectionUtils.parseSequenceList(this.refSeq.name);
                // var refSeqName = sequenceList[0].name;
                var chrName ;
                if(ProjectionUtils.isSequenceList(this.refSeq.name)){
                    var sequenceListObject = ProjectionUtils.parseSequenceList(this.refSeq.name);
                    console.log(sequenceListObject);
                    chrName = sequenceListObject[0].name ;
                }
                else{
                    chrName = this.refSeq.name ;
                }
                this.getFeatures(
                    {
                        ref: chrName,
                        basesPerSpan: 1 / scale,
                        scale: scale,
                        start: leftBase,
                        end: rightBase + 1
                    },

                    function (f) {
                        if (thisB.filterFeature(f)){
                            features.push(f);
                        }
                    },
                    dojo.hitch(this, function (args) {

                        // if the block has been freed in the meantime,
                        // don't try to render
                        if (!(block.domNode && block.domNode.parentNode ))
                            return;

                        var featureRects = array.map(features, function (f) {
                            // if(!f.isProjected) {
                            //     f = ProjectionUtils.projectJSONFeature(f,this.refSeq.name);
                            // }
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

            // TODO: implement ;
            getRegionStats: function(args){
                console.log('getting region stats: '+args)
            },

            // TODO: implement ;
            getGlobalStats: function(args){
                console.log('getting global stats: '+ args)
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
                    // region.ref = ProjectionUtils.parseSequenceList(region.ref)[0].name;
                    // region.start = Math.ceil( region.start );
                    // region.end = Math.floor( region.end );
                    return this.getRegionStats.call( this, region, callback, errorCallback );
                }
                else {
                    return this.getGlobalStats.call( this, callback, errorCallback );
                }
            }
        });
    });