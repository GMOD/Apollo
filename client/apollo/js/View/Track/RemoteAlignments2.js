define( [
        'dojo/_base/declare',
        'dojo/dom-construct',
        'dojo/_base/array',
        'dojo/Deferred',
        'dojo/promise/all',
        'JBrowse/Util',
        'WebApollo/JSONUtils',
        'JBrowse/View/Track/Alignments2'
    ],
    function(
        declare,
        domConstruct,
        array,
        Deferred,
        all,
        Util,
        JSONUtils,
        Alignments2
    ) {

        return declare( Alignments2, {

            /**
             * Override fillFeatures
             */
            fillFeatures: function (args) {
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

                var errorCallback = dojo.hitch(thisB, function (e) {
                    this._handleError(e, args);
                    finishCallback(e);
                });

                var layout = this._getLayout(scale);

                // query for a slightly larger region than the block, so that
                // we can draw any pieces of glyphs that overlap this block,
                // but the feature of which does not actually lie in the block
                // (long labels that extend outside the feature's bounds, for
                // example)
                var bpExpansion = Math.round(this.config.maxFeatureGlyphExpansion / scale);

                var region = {
                    ref: this.refSeq.name,
                    start: Math.max(0, leftBase - bpExpansion),
                    end: rightBase + bpExpansion
                };

                var sequenceList = JSONUtils.parseSequenceList(this.refSeq.name);
                if (sequenceList[0].reverse) {
                    errorCallback(JSONUtils.NOT_YET_SUPPORTED_MESSAGE);
                }
                else {
                    region.ref = sequenceList[0].name;
                    this.store.getFeatures(region,
                        function (feature) {
                            if (thisB.destroyed || !thisB.filterFeature(feature))
                                return;
                            fRects.push(null); // put a placeholder in the fRects array
                            featuresInProgress++;
                            var rectNumber = fRects.length - 1;

                            // get the appropriate glyph object to render this feature
                            thisB.getGlyph(
                                args,
                                feature,
                                function (glyph) {
                                    // have the glyph attempt
                                    // to add a rendering of
                                    // this feature to the
                                    // layout
                                    var fRect = glyph.layoutFeature(
                                        args,
                                        layout,
                                        feature
                                    );
                                    if (fRect === null) {
                                        // could not lay out, would exceed our configured maxHeight
                                        // mark the block as exceeding the max height
                                        block.maxHeightExceeded = true;
                                    }
                                    else {
                                        // laid out successfully
                                        if (!( fRect.l >= blockWidthPx || fRect.l + fRect.w < 0 ))
                                            fRects[rectNumber] = fRect;
                                    }

                                    // this might happen after all the features have been sent from the store
                                    if (!--featuresInProgress && allFeaturesRead) {
                                        featuresLaidOut.resolve();
                                    }
                                },
                                errorCallback
                            );
                        },

                        // callback when all features sent
                        function () {
                            if (thisB.destroyed)
                                return;

                            allFeaturesRead = true;
                            if (!featuresInProgress && !featuresLaidOut.isFulfilled()) {
                                featuresLaidOut.resolve();
                            }

                            featuresLaidOut.then(function () {

                                var totalHeight = layout.getTotalHeight();
                                var c = block.featureCanvas =
                                    domConstruct.create(
                                        'canvas',
                                        {
                                            height: totalHeight,
                                            width: block.domNode.offsetWidth + 1,
                                            style: {
                                                cursor: 'default',
                                                height: totalHeight + 'px',
                                                position: 'absolute'
                                            },
                                            innerHTML: 'Your web browser cannot display this type of track.',
                                            className: 'canvas-track'
                                        },
                                        block.domNode
                                    );
                                var ctx = c.getContext('2d');

                                // finally query the various pixel ratios
                                var ratio = Util.getResolution(ctx, thisB.browser.config.highResolutionMode);
                                // upscale canvas if the two ratios don't match
                                if (thisB.browser.config.highResolutionMode != 'disabled' && ratio >= 1) {

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


                                if (block.maxHeightExceeded)
                                    thisB.markBlockHeightOverflow(block);

                                thisB.heightUpdate(totalHeight,
                                    blockIndex);


                                thisB.renderFeatures(args, fRects);

                                thisB.renderClickMap(args, fRects);

                                finishCallback();
                            });
                        },
                        errorCallback
                    );
                }
            }

        });
});
