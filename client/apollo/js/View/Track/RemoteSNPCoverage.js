define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/_base/lang',
        'dojo/promise/all',
        'JBrowse/View/Track/Wiggle/_Scale',
        'WebApollo/View/Track/Wiggle/RemoteXYPlot',
        'JBrowse/Util',
        'WebApollo/ProjectionUtils',
        'JBrowse/View/Track/_AlignmentsMixin',
        'WebApollo/Store/SeqFeature/SNPCoverage'
    ],
    function(
        declare,
        array,
        lang,
        all,
        Scale,
        RemoteXYPlotTrack,
        Util,
        ProjectionUtils,
        AlignmentsMixin,
        SNPCoverageStore
    ) {

        return declare( [ RemoteXYPlotTrack, AlignmentsMixin ], {

            constructor: function() {
                delete this.config.bicolor_pivot;
                delete this.config.scale;
                delete this.config.align;

                var thisB = this;
                this.store = new SNPCoverageStore(
                    { store: this.store,
                        config: {
                            mismatchScale: this.config.mismatchScale
                        },
                        browser: this.browser,
                        filter: function( f ) {
                            return thisB.filterFeature( f );
                        }
                    });
            },

            _defaultConfig: function() {
                return Util.deepUpdate(
                    dojo.clone( this.inherited(arguments) ),
                    {
                        autoscale: 'local',
                        min_score: 0,

                        mismatchScale: 1/10,

                        hideDuplicateReads: true,
                        logScaleOption: false,
                        hideQCFailingReads: true,
                        hideSecondary: true,
                        hideSupplementary: true,
                        hideMissingMatepairs: false,
                        hideUnmapped: true
                    }
                );
            },

            /*
             * Draw a set of features on the canvas.
             * @private
             */
            _drawFeatures: function( scale, leftBase, rightBase, block, canvas, features, featureRects, dataScale ) {
                var thisB = this;
                var context = canvas.getContext('2d');
                var canvasHeight = canvas.height;

                var ratio = Util.getResolution( context, this.browser.config.highResolutionMode );
                var toY = dojo.hitch( this, function( val ) {
                    return canvasHeight * ( 1-dataScale.normalize(val) ) / ratio;
                });
                var originY = toY( dataScale.origin );

                // a canvas element below the histogram that will contain indicators of likely SNPs
                var snpCanvasHeight = 20;
                var snpCanvas = dojo.create('canvas',
                    {height: snpCanvasHeight,
                        width: canvas.width,
                        style: {
                            cursor: 'default',
                            width: "100%",
                            height: snpCanvasHeight + "px"
                        },
                        innerHTML: 'Your web browser cannot display this type of track.',
                        className: 'SNP-indicator-track'
                    }, block.domNode);
                var snpContext = snpCanvas.getContext('2d');

                // finally query the various pixel ratios
                var ratio = Util.getResolution( snpContext, this.browser.config.highResolutionMode );
                // upscale canvas if the two ratios don't match
                if ( this.browser.config.highResolutionMode !='disabled' && ratio!=1 ) {

                    var oldWidth = snpCanvas.width;
                    var oldHeight = snpCanvas.height;

                    snpCanvas.width = oldWidth * ratio;
                    snpCanvas.height = oldHeight * ratio;

                    //c.style.width = oldWidth + 'px';
                    snpCanvas.style.height = oldHeight + 'px';

                    // now scale the context to counter
                    // the fact that we've manually scaled
                    // our canvas element
                    snpContext.scale(ratio, ratio);
                }


                var negColor  = this.config.style.neg_color;
                var clipColor = this.config.style.clip_marker_color;
                var bgColor   = this.config.style.bg_color;
                var disableClipMarkers = this.config.disable_clip_markers;

                var drawRectangle = function(ID, yPos, height, fRect) {
                    if( yPos <= canvasHeight ) { // if the rectangle is visible at all
                        context.fillStyle = thisB.colorForBase(ID);
                        if( yPos <= originY ) {
                            // bar goes upward
                            thisB._fillRectMod( context, fRect.l, yPos, fRect.w, height);
                            if( !disableClipMarkers && yPos < 0 ) { // draw clip marker if necessary
                                context.fillStyle = clipColor || negColor;
                                thisB._fillRectMod( context, fRect.l, 0, fRect.w, 2 );
                            }
                        }
                        else {
                            // bar goes downward
                            thisB._fillRectMod( context, fRect.l, originY, fRect.w, height );
                            if( !disableClipMarkers && yPos >= canvasHeight ) { // draw clip marker if necessary
                                context.fillStyle = clipColor || thisB.colorForBase(ID);
                                thisB._fillRectMod( context, fRect.l, canvasHeight-3, fRect.w, 2 );
                            }
                        }
                    }
                };

                // Note: 'reference' is done first to ensure the grey part of the graph is on top
                dojo.forEach( features, function(f,i) {
                    var fRect = featureRects[i];
                    var score = f.get('score');

                    // draw the background color if we are configured to do so
                    if( bgColor ) {
                        context.fillStyle = bgColor;
                        thisB._fillRectMod( context, fRect.l, 0, fRect.w, canvasHeight );
                    }

                    drawRectangle( 'reference', toY( score.total() ), originY-toY( score.get('reference'))+1, fRect);
                });

                dojo.forEach( features, function(f,i) {
                    var fRect = featureRects[i];
                    var score = f.get('score');
                    var totalHeight = score.total();

                    // draw indicators of SNPs if base coverage is greater than 50% of total coverage
                    score.forEach( function( count, category ) {
                        if ( !{reference:true,skip:true,deletion:true}[category] && count > 0.5*totalHeight ) {
                            snpContext.save();
                            if( thisB.browser.config.highResolutionMode != 'disabled' )
                                snpContext.scale(ratio, 1);
                            snpContext.beginPath();
                            snpContext.arc( (fRect.l + 0.5*fRect.w),
                                0.40*snpCanvas.height/ratio,
                                0.20*snpCanvas.height/ratio,
                                1.75 * Math.PI,
                                1.25 * Math.PI,
                                false);
                            snpContext.lineTo(fRect.l + 0.5*fRect.w, 0);
                            snpContext.closePath();
                            snpContext.fillStyle = thisB.colorForBase(category);
                            snpContext.fill();
                            snpContext.lineWidth = 1;
                            snpContext.strokeStyle = 'black';
                            snpContext.stroke();
                            if( thisB.browser.config.highResolutionMode != 'disabled' )
                                snpContext.restore();
                        }
                    });

                    totalHeight -= score.get('reference');

                    score.forEach( function( count, category ) {
                        if ( category != 'reference' ) {
                            drawRectangle( category, toY(totalHeight), originY-toY( count )+1, fRect);
                            totalHeight -= count;
                        }
                    });
                }, this );
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

                var errorCallback = dojo.hitch(this, function(e) {
                    this._handleError(e, args);
                    finishCallback(e);
                });

                var features = [];
                var sequenceList = ProjectionUtils.parseSequenceList(this.refSeq.name);
                if (sequenceList[0].reverse) {
                    errorCallback(ProjectionUtils.NOT_YET_SUPPORTED_MESSAGE)
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
                    region.ref = ProjectionUtils.parseSequenceList(region.ref)[0].name;
                    region.start = Math.ceil( region.start );
                    region.end = Math.floor( region.end );
                    return this.getRegionStats.call( this, region, callback, errorCallback );
                }
                else {
                    return this.getGlobalStats.call( this, callback, errorCallback );
                }
            },

            // Overwrites the method from WiggleBase
            _draw: function( scale, leftBase, rightBase, block, canvas, features, featureRects, dataScale, pixels, spans ) {
                // Note: pixels currently has no meaning, as the function that generates it is not yet defined for this track
                this._preDraw(      scale, leftBase, rightBase, block, canvas, features, featureRects, dataScale );
                this._drawFeatures( scale, leftBase, rightBase, block, canvas, features, featureRects, dataScale );
                if ( spans ) {
                    this._maskBySpans( scale, leftBase, canvas, spans );
                }
                this._postDraw(     scale, leftBase, rightBase, block, canvas, features, featureRects, dataScale );
            },

            /* If it's a boolean track, mask accordingly */
            _maskBySpans: function( scale, leftBase, canvas, spans ) {
                var context = canvas.getContext('2d');
                var canvasHeight = canvas.height;
                var booleanAlpha = this.config.style.masked_transparancy || 0.17;
                this.config.style.masked_transparancy = booleanAlpha;

                // make a temporary canvas to store image data
                var tempCan = dojo.create( 'canvas', {height: canvasHeight, width: canvas.width} );
                var ctx2 = tempCan.getContext('2d');

                for ( var index in spans ) {
                    if (spans.hasOwnProperty(index)) {
                        var w = Math.round(( spans[index].end   - spans[index].start ) * scale );
                        var l = Math.round(( spans[index].start - leftBase ) * scale );
                        if (l+w >= canvas.width)
                            w = canvas.width-l; // correct possible rounding errors
                        if (w==0)
                            continue; // skip if there's no width.
                        ctx2.drawImage(canvas, l, 0, w, canvasHeight, l, 0, w, canvasHeight);
                        context.globalAlpha = booleanAlpha;
                        // clear masked region and redraw at lower opacity.
                        context.clearRect(l, 0, w, canvasHeight);
                        context.drawImage(tempCan, l, 0, w, canvasHeight, l, 0, w, canvasHeight);
                        context.globalAlpha = 1;
                    }
                }
            },

            /*
             * The following method is required to override the equivalent method in "WiggleBase.js"
             * It displays more complete data.
             */
            _showPixelValue: function( scoreDisplay, score ) {
                if( ! score || ! score.score )
                    return false;
                score = score.score;

                function fmtNum( num ) {
                    return parseFloat( num ).toPrecision(6).replace(/0+$/,'').replace(/\.$/,'');
                }
                function pctString( count ) {
                    count = Math.round(count/total*100);
                    if( typeof count == 'number' && ! isNaN(count) )
                        return count+'%';
                    return '';
                }
                if( score.snpsCounted ) {
                    var total = score.total();
                    var scoreSummary = '<table>';


                    score.forEach( function( count, category ) {
                        // if this count has more nested categories, do counts of those
                        var subdistribution = '';
                        if( count.forEach ) {
                            subdistribution = [];
                            count.forEach( function( count, category ) {
                                subdistribution.push( fmtNum(count) + ' '+category );
                            });
                            subdistribution = subdistribution.join(', ');
                            if( subdistribution )
                                subdistribution = '('+subdistribution+')';
                        }

                        category = { '*': 'del', reference: 'Ref', skip: 'Skip/intron' }[category] || category;
                        scoreSummary += '<tr><td>'+category + '</td><td class="count">' + fmtNum(count) + '</td><td class="pct">'
                            +pctString(count)+'</td><td class="subdist">'+subdistribution + '</td></tr>';
                    });
                    scoreSummary += '<tr class="total"><td>Total</td><td class="count">'+fmtNum(total)+'</td><td class="pct">&nbsp;</td><td class="subdist">&nbsp;</td></tr>';
                    scoreDisplay.innerHTML = scoreSummary+'</table>';
                    return true;
                } else {
                    scoreDisplay.innerHTML = '<table><tr><td>Total</td><td class="count">'+fmtNum(score)+'</td></tr></table>';
                    return true;
                }
            },

            _trackMenuOptions: function() {
                return all([ this.inherited(arguments), this._alignmentsFilterTrackMenuOptions() ])
                    .then( function( options ) {
                        var o = options.shift();
                        options.unshift({ type: 'dijit/MenuSeparator' } );
                        return o.concat.apply( o, options );
                    });
            }


        });

});