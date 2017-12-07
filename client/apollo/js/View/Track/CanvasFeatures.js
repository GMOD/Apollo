/**
 * Feature track that draws features using HTML5 canvas elements.
 */

define( [
            'dojo/_base/declare',
            'dojo/_base/array',
            'dojo/_base/lang',
            'dojo/_base/event',
            'dojo/mouse',
            'dojo/dom-construct',
            'dojo/Deferred',
            'dojo/on',
            'JBrowse/has',
            'JBrowse/Util',
            'WebApollo/ProjectionUtils',
            'JBrowse/View/Track/CanvasFeatures'
        ],
        function(
            declare,
            array,
            lang,
            domEvent,
            mouse,
            domConstruct,
            Deferred,
            on,
            has,
            Util,
            ProjectionUtils,
            CanvasFeatures
        ) {

/**
 *  inner class that indexes feature layout rectangles (fRects) (which
 *  include features) by unique ID.
 *
 *  We have one of these indexes in each block.
 */

return declare(
    [ CanvasFeatures ], {

        _defaultConfig: function() {
            return Util.deepUpdate(
                lang.clone( this.inherited(arguments) ),
                {
                    maxFeatureScreenDensity: 0.5,

                    // default glyph class to use
                    glyph: lang.hitch( this, 'guessGlyphType' ),

                    // maximum number of pixels on each side of a
                    // feature's bounding coordinates that a glyph is
                    // allowed to use
                    maxFeatureGlyphExpansion: 500,

                    // maximum height of the track, in pixels
                    maxHeight: 600,

                    histograms: {
                        description: 'feature density',
                        min: 0,
                        height: 100,
                        color: 'goldenrod',
                        clip_marker_color: 'red'
                    },

                    style: {
                        // not configured by users
                        _defaultHistScale: 4,
                        _defaultLabelScale: 30,
                        _defaultDescriptionScale: 120,

                        showLabels: true,
                        showTooltips: true,
                        label: 'name,id',
                        description: 'note, description'
                    },

                    displayMode: 'normal',

                    events: {
                        contextmenu: function( feature, fRect, block, track, evt ) {
                            evt = domEvent.fix( evt );
                            if( fRect && fRect.contextMenu )
                                fRect.contextMenu._openMyself({ target: block.featureCanvas, coords: { x: evt.pageX, y: evt.pageY }} );
                            domEvent.stop( evt );
                        }
                    },

                    menuTemplate: [
                        { label: 'View details',
                            title: '{type} {name}',
                            action: 'contentDialog',
                            iconClass: 'dijitIconTask',
                            content: dojo.hitch( this, 'defaultFeatureDetail' )
                        },
                        {
                            "label" : function() {
                                return 'Zoom to this '+( this.feature.get('type') || 'feature' );
                            },
                            "action" : function(){
                                var ref   = this.track.refSeq;
                                var paddingBp = Math.round( 10 /*pixels*/ / this.viewInfo.scale /* px/bp */ );

                                this.feature = ProjectionUtils.projectJSONFeature(this.feature,ref.name);
                                var start = Math.max( ref.start, this.feature.get('start') - paddingBp );
                                var end   = Math.min( ref.end, this.feature.get('end') + paddingBp );
                                this.track.genomeView.setLocation( ref, start, end );
                            },
                            "iconClass" : "dijitIconConnector"
                        },
                        {
                            label : function() {
                                return 'Highlight this '+( this.feature.get('type') || 'feature' );
                            },
                            action: function() {
                                var loc = new Location({ feature: this.feature, tracks: [this.track] });
                                this.track.browser.setHighlightAndRedraw(loc);
                            },
                            iconClass: 'dijitIconFilter'
                        }
                    ]
                });
        },



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

        var refSeqName = this.refSeq.name ;

        var region = { ref: refSeqName,
                       start: Math.max( 0, leftBase - bpExpansion ),
                       end: rightBase + bpExpansion
                     };
        this.store.getFeatures( region,
                                function( feature ) {
                                    if( thisB.destroyed || ! thisB.filterFeature( feature ) )
                                        return;
                                    fRects.push( null ); // put a placeholder in the fRects array
                                    featuresInProgress++;
                                    var rectNumber = fRects.length-1;

                                    feature = ProjectionUtils.projectJSONFeature(feature,refSeqName);

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
    }
});
});
