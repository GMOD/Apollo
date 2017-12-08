define( [
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/dom-construct',
        'dojo/promise/all',
        'dojo/Deferred',
        'dijit/Menu',
        'dijit/MenuItem',
        'dijit/CheckedMenuItem',
        'dijit/MenuSeparator',
        'dijit/PopupMenuItem',
        'dijit/Dialog',
        'JBrowse/Util',
        'JBrowse/View/Track/Alignments2',
        'JBrowse/Store/SeqFeature/_MismatchesMixin',
        'WebApollo/ProjectionUtils'
    ],
    function(
        declare,
        array,
        domConstruct,
        all,
        Deferred,
        dijitMenu,
        dijitMenuItem,
        dijitCheckedMenuItem,
        dijitMenuSeparator,
        dijitPopupMenuItem,
        dijitDialog,
        Util,
        Alignments2,
        MismatchesMixin,
        ProjectionUtils
    ) {

        MismatchesMixin.extend({

            _mdToMismatches: function( feature, mdstring, cigarOps, cigarMismatches ) {
                var sequenceList = ProjectionUtils.parseSequenceList(this.browser.refSeq.name);
                var mismatchRecords = [];
                var curr = { start: 0, base: '', length: 0, type: 'mismatch' };

                // convert a position on the reference sequence to a position
                // on the template sequence, taking into account hard and soft
                // clipping of reads
                function getTemplateCoord( refCoord, cigarOps ) {
                    var templateOffset = 0;
                    var refOffset = 0;
                    for( var i = 0; i < cigarOps.length && refOffset <= refCoord ; i++ ) {
                        var op  = cigarOps[i][0];
                        var len = cigarOps[i][1];
                        if( op == 'S' || op == 'I' ) {
                            templateOffset += len;
                        }
                        else if( op == 'D' || op == 'P' ) {
                            refOffset += len;
                        }
                        else {
                            templateOffset += len;
                            refOffset += len;
                        }
                    }
                    return templateOffset - ( refOffset - refCoord );
                }

                function nextRecord() {
                    // correct the start of the current mismatch if it comes after a cigar skip
                    var skipOffset = 0;
                    array.forEach( cigarMismatches || [], function( mismatch ) {
                        if( mismatch.type == 'skip' && curr.start >= mismatch.start ) {
                            curr.start += mismatch.length;
                        }
                    });

                    // record it
                    mismatchRecords.push( curr );

                    // get a new mismatch record ready
                    curr = { start: curr.start + curr.length, length: 0, base: '', type: 'mismatch'};
                };

                var seq = feature.get('seq');
                if (feature.isProjected) {
                    if (sequenceList[0].reverse) {
                        var md_array = mdstring.match(/(\d+|\^[a-z]+|[a-z])/ig);
                        md_array.reverse();
                        mdstring = md_array.join('');

                        seq = seq.split('').reverse().join('');
                    }
                }

                // now actually parse the MD string
                array.forEach( mdstring.match(/(\d+|\^[a-z]+|[a-z])/ig), function( token ) {
                    if( token.match(/^\d/) ) { // matching bases
                        curr.start += parseInt( token );
                    }
                    else if( token.match(/^\^/) ) { // insertion in the template
                        curr.length = token.length-1;
                        curr.base   = '*';
                        curr.type   = 'deletion';
                        curr.seq    = token.substring(1);
                        nextRecord();
                    }
                    else if( token.match(/^[a-z]/i) ) { // mismatch
                        for( var i = 0; i<token.length; i++ ) {
                            curr.length = 1;
                            curr.base = seq ? seq.substr( cigarOps ? getTemplateCoord( curr.start, cigarOps)
                                    : curr.start,
                                1
                            )
                                : 'X';
                            curr.altbase = token;
                            nextRecord();
                        }
                    }
                });
                return mismatchRecords;
            },

            _parseCigar: function(cigar) {
                var sequenceListObject = ProjectionUtils.parseSequenceList(this.browser.refSeq.name);
                if (sequenceListObject[0].reverse) {
                    var cigar_array = cigar.match(/\d+\D/g);
                    cigar_array.reverse();
                    cigar = cigar_array.join('');
                }

                return array.map( cigar.toUpperCase().match(/\d+\D/g), function( op ) {
                    return [ op.match(/\D/)[0], parseInt( op ) ];
                });
            }
        });

        return declare(Alignments2, {

            /**
             * An extension to JBrowse/View/Track/Alignments2 to allow for creating annotations
             * from read alignments.
             */

            constructor: function() {
                this.browser.getPlugin('WebApollo', dojo.hitch(this, function(plugin) {
                    this.webapollo = plugin;
                }));
            },

            _defaultConfig: function() {
                var thisB = this;
                var config = this.inherited(arguments);

                config.menuTemplate.push({
                    "label": "Create new annotation",
                    "children": [
                        {
                            "label": "gene",
                            "action":  function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createAnnotations({selection: {feature: this.feature}});
                            }
                        },
                        {
                            "label": "pseudogene",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "transcript", null, "pseudogene");
                            }
                        },
                        {
                            "label": "tRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "tRNA", null, "gene");
                            }
                        },
                        {
                            "label": "snRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "snRNA", null, "gene");
                            }
                        },
                        {
                            "label": "snoRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "snoRNA", null, "gene");
                            }
                        },
                        {
                            "label": "ncRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "ncRNA", null, "gene");
                            }
                        },
                        {
                            "label": "rRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "rRNA", null, "gene");
                            }
                        },
                        {
                            "label": "miRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "miRNA", null, "gene");
                            }
                        },
                        {
                            "label": "repeat_region",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericOneLevelAnnotations([this.feature], "repeat_region", true);
                            }
                        },
                        {
                            "label": "transposable_element",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericOneLevelAnnotations([this.feature], "transposable_element", true);
                            }
                        }
                    ]
                });
                return config;
            },

            /**
             * Override
             */
            fillFeatures: function( args ) {
                var thisB = this;

                var blockIndex = args.blockIndex;
                var block = args.block;
                var blockWidthPx = block.domNode.offsetWidth;
                var scale = args.scale;
                var leftBase = args.leftBase;
                var rightBase = args.rightBase;
                var finishCallback = args.finishCallback;

                var fRects = [];

                // count of how many features are queued up to be laid out
                var featuresInProgress = 0;
                // promise that resolved when all the features have gotten laid out by their glyphs
                var featuresLaidOut = new Deferred();
                // flag that tells when all features have been read from the
                // store (not necessarily laid out yet)
                var allFeaturesRead = false;

                var errorCallback = dojo.hitch( thisB, function( e ) {
                    this._handleError( e, args );
                    finishCallback(e);
                });

                var layout = this._getLayout( scale );

                // query for a slightly larger region than the block, so that
                // we can draw any pieces of glyphs that overlap this block,
                // but the feature of which does not actually lie in the block
                // (long labels that extend outside the feature's bounds, for
                // example)
                var bpExpansion = Math.round( this.config.maxFeatureGlyphExpansion / scale );

                var region = { ref: this.refSeq.name,
                    start: Math.max( 0, leftBase - bpExpansion ),
                    end: rightBase + bpExpansion
                };
                this.store.getFeatures( region,
                    function( feature ) {
                        if (!feature.isProjected) {
                            feature = ProjectionUtils.projectJSONFeature(feature, this.JBrowse.refSeq.name);
                        }
                        if( thisB.destroyed || ! thisB.filterFeature( feature ) )
                            return;
                        fRects.push( null ); // put a placeholder in the fRects array
                        featuresInProgress++;
                        var rectNumber = fRects.length-1;
                        // get the appropriate glyph object to render this feature
                        thisB.getGlyph(
                            args,
                            feature,
                            function( glyph ) {
                                // have the glyph attempt
                                // to add a rendering of
                                // this feature to the
                                // layout
                                var fRect = glyph.layoutFeature(
                                    args,
                                    layout,
                                    feature
                                );
                                if( fRect === null ) {
                                    // could not lay out, would exceed our configured maxHeight
                                    // mark the block as exceeding the max height
                                    block.maxHeightExceeded = true;
                                }
                                else {
                                    // laid out successfully
                                    if( !( fRect.l >= blockWidthPx || fRect.l+fRect.w < 0 ) )
                                        fRects[rectNumber] = fRect;
                                }

                                // this might happen after all the features have been sent from the store
                                if( ! --featuresInProgress && allFeaturesRead ) {
                                    featuresLaidOut.resolve();
                                }
                            },
                            errorCallback
                        );
                    },

                    // callback when all features sent
                    function () {
                        if( thisB.destroyed )
                            return;

                        allFeaturesRead = true;
                        if( ! featuresInProgress && ! featuresLaidOut.isFulfilled() ) {
                            featuresLaidOut.resolve();
                        }

                        featuresLaidOut.then( function() {

                            var totalHeight = layout.getTotalHeight();
                            var c = block.featureCanvas =
                                domConstruct.create(
                                    'canvas',
                                    { height: totalHeight,
                                        width:  block.domNode.offsetWidth+1,
                                        style: {
                                            cursor: 'default',
                                            height: totalHeight+'px',
                                            position: 'absolute'
                                        },
                                        innerHTML: 'Your web browser cannot display this type of track.',
                                        className: 'canvas-track'
                                    },
                                    block.domNode
                                );
                            var ctx = c.getContext('2d');

                            // finally query the various pixel ratios
                            var ratio = Util.getResolution( ctx, thisB.browser.config.highResolutionMode );
                            // upscale canvas if the two ratios don't match
                            if ( thisB.browser.config.highResolutionMode != 'disabled' && ratio >= 1 ) {

                                var oldWidth = c.width;
                                var oldHeight = c.height;

                                c.width = oldWidth * ratio;
                                c.height = oldHeight * ratio;

                                c.style.width = oldWidth + 'px';
                                c.style.height = oldHeight + 'px';

                                // now scale the context to counter
                                // the fact that we've manually scaled
                                // our canvas element
                                ctx.scale(ratio, ratio);
                            }



                            if( block.maxHeightExceeded )
                                thisB.markBlockHeightOverflow( block );

                            thisB.heightUpdate( totalHeight,
                                blockIndex );


                            thisB.renderFeatures( args, fRects );

                            thisB.renderClickMap( args, fRects );

                            finishCallback();
                        });
                    },
                    errorCallback
                );
            },

            /**
             * Override
             */
            _drawHistograms: function( viewArgs, histData ) {

                var maxScore = 'max' in this.config.histograms ? this.config.histograms.max : histData.stats.max;

                // don't do anything if we don't know the score max
                if( maxScore === undefined ) {
                    console.warn( 'no stats.max in hist data, not drawing histogram for block '+viewArgs.blockIndex );
                    return;
                }

                // don't do anything if we have no hist features
                var features;
                if(!( ( features = histData.features )
                        || histData.bins && ( features = this._histBinsToFeatures( viewArgs, histData ) )
                    ))
                    return;

                var block = viewArgs.block;
                var height = this.config.histograms.height;
                var scale = viewArgs.scale;
                var leftBase = viewArgs.leftBase;
                var minVal = this.config.histograms.min;

                domConstruct.empty( block.domNode );
                var c = block.featureCanvas =
                    domConstruct.create(
                        'canvas',
                        { height: height,
                            width:  block.domNode.offsetWidth+1,
                            style: {
                                cursor: 'default',
                                height: height+'px',
                                position: 'absolute'
                            },
                            innerHTML: 'Your web browser cannot display this type of track.',
                            className: 'canvas-track canvas-track-histograms'
                        },
                        block.domNode
                    );
                this.heightUpdate( height, viewArgs.blockIndex );
                var ctx = c.getContext('2d');

                // finally query the various pixel ratios
                var ratio = Util.getResolution( ctx, this.browser.config.highResolutionMode );
                // upscale canvas if the two ratios don't match
                if ( this.browser.config.highResolutionMode != 'disabled' && ratio >= 1 )
                {
                    var oldWidth = c.width;
                    var oldHeight = c.height;

                    c.width = oldWidth * ratio;
                    c.height = oldHeight * ratio;

                    c.style.width = oldWidth + 'px';
                    c.style.height = oldHeight + 'px';

                    // now scale the context to counter
                    // the fact that we've manually scaled
                    // our canvas element
                    ctx.scale(ratio, ratio);
                }

                ctx.fillStyle = this.config.histograms.color;
                for( var i = 0; i<features.length; i++ ) {
                    var feature = features[i];
                    // project feature
                    feature = ProjectionUtils.projectJSONFeature(feature, this.refSeq.name);
                    var barHeight = feature.get('score')/maxScore * height;
                    var barWidth = Math.ceil( ( feature.get('end')-feature.get('start') )*scale );
                    var barLeft = Math.round(( feature.get('start') - leftBase )*scale );
                    ctx.fillRect(
                        barLeft,
                        height-barHeight,
                        barWidth,
                        barHeight
                    );
                    if( barHeight > height ) {
                        ctx.fillStyle = this.config.histograms.clip_marker_color;
                        ctx.fillRect( barLeft, 0, barWidth, 3 );
                        ctx.fillStyle = this.config.histograms.color;
                    }
                }

                // make the y-axis scale for our histograms
                this.makeHistogramYScale( height, minVal, maxScore );
            }
        });
    }
);