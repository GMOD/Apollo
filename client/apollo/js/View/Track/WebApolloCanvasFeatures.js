define( [
            'dojo/_base/declare',
            'dojo/_base/array',
            'dojo/dom-construct',
            'dojo/Deferred',
            'JBrowse/View/Track/CanvasFeatures',
            'dijit/Menu',
            'dijit/MenuItem',
            'dijit/CheckedMenuItem',
            'dijit/MenuSeparator',
            'dijit/PopupMenuItem',
            'dijit/Dialog',
            'JBrowse/Util',
            'JBrowse/Model/SimpleFeature',
            'WebApollo/View/Projection/FASTA',
            'WebApollo/ProjectionUtils',
            'WebApollo/SequenceOntologyUtils'
        ],
        function( declare,
            array,
            domConstruct,
            Deferred,
            CanvasFeaturesTrack,
            dijitMenu,
            dijitMenuItem,
            dijitCheckedMenuItem,
            dijitMenuSeparator,
            dijitPopupMenuItem,
            dijitDialog,
            Util,
            SimpleFeature,
            FASTAView,
            ProjectionUtils,
            SeqOnto )
{

return declare( CanvasFeaturesTrack,

{
    constructor: function() {
        this.browser.getPlugin( 'WebApollo', dojo.hitch( this, function(p) {
            this.webapollo = p;
        }));
    },
    _defaultConfig: function() {
        var config = Util.deepUpdate(dojo.clone(this.inherited(arguments)),
            {
                style: {
                    textColor: function() { return dojo.hasClass(document.body,'Dark') ?'white': 'black'; },
                    text2Color: function() { return dojo.hasClass(document.body,'Dark')? 'LightSteelBlue': 'blue'; },
                    connectorColor: function() { return dojo.hasClass(document.body,'Dark')? 'lightgrey': 'black'; },
                    color: function() { return dojo.hasClass(document.body,'Dark')? 'orange': 'goldenrod'; }
                }
            });
        var thisB=this;
        config.menuTemplate.push(            {
              "label" : "Create new annotation",
              "children" : [
                {
                  "label" : "gene",
                  "action":  function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createAnnotations({x1:{feature:this.feature}});
                  }
                },
                {
                  "label" : "pseudogene",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "transcript", null, "pseudogene");
                  }
                },
                {
                  "label" : "tRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "tRNA", null, "gene");
                   }
                },
                {
                  "label" : "snRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "snRNA", null, "gene");
                   }
                },
                {
                  "label" : "snoRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "snoRNA", null, "gene");
                   }
                },
                {
                  "label" : "ncRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "ncRNA", null, "gene");
                   }
                },
                {
                  "label" : "rRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "rRNA", null, "gene");
                   }
                },
                {
                  "label" : "miRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "miRNA", null, "gene");
                   }
                },
                {
                  "label" : "repeat_region",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericOneLevelAnnotations([this.feature], "repeat_region", true);
                   }
                },
                {
                  "label" : "transposable element",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericOneLevelAnnotations([this.feature], "transposable_element", true);
                   }
                }
              ]
            }
        );
        return config;
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
    },

    _renderUnderlyingReferenceSequence: function( track, f, featDiv, container ) {
        // render the sequence underlying this feature if possible
        var field_container = dojo.create('div', { className: 'field_container feature_sequence' }, container );
        dojo.create( 'h2', { className: 'field feature_sequence', innerHTML: 'Region sequence', title: 'reference sequence underlying this '+(f.get('type') || 'feature') }, field_container );
        var valueContainerID = 'feature_sequence'+this._uniqID();
        var valueContainer = dojo.create(
            'div', {
                id: valueContainerID,
                innerHTML: '<div style="height: 12em">Loading...</div>',
                className: 'value feature_sequence'
            }, field_container);
        var maxSize = this.config.maxFeatureSizeForUnderlyingRefSeq;
        if( maxSize < (f.get('end') - f.get('start')) ) {
            valueContainer.innerHTML = 'Not displaying underlying reference sequence, feature is longer than maximum of '+Util.humanReadableNumber(maxSize)+'bp';
        } else {
            track.browser.getStore('refseqs', dojo.hitch(this,function( refSeqStore ) {
                valueContainer = dojo.byId(valueContainerID) || valueContainer;
                if( refSeqStore ) {
                    refSeqStore.getReferenceSequence(
                        {
                            ref: this.refSeq.name,
                            start: f.get('start'),
                            end: f.get('end')
                        },
                        // feature callback
                        dojo.hitch( this, function( seq ) {
                            valueContainer = dojo.byId(valueContainerID) || valueContainer;
                            valueContainer.innerHTML = '';
                            // the HTML is rewritten by the dojo dialog
                            // parser, but this callback may be called either
                            // before or after that happens.  if the fetch by
                            // ID fails, we have come back before the parse.
                            var textArea = new FASTAView({ track: this, width: 62, htmlMaxRows: 10 })
                                .renderHTML(
                                    { ref:   this.refSeq.name,
                                        start: f.get('start'),
                                        end:   f.get('end'),
                                        strand: f.get('strand'),
                                        type: f.get('type')
                                    },
                                    f.get('strand') == -1 ? Util.revcom(seq) : seq,
                                    valueContainer
                                );
                        }),
                        // end callback
                        function() {},
                        // error callback
                        dojo.hitch( this, function() {
                            valueContainer = dojo.byId(valueContainerID) || valueContainer;
                            valueContainer.innerHTML = '<span class="ghosted">reference sequence not available</span>';
                        })
                    );
                } else {
                    valueContainer.innerHTML = '<span class="ghosted">reference sequence not available</span>';
                }
            }));
        }
    }

});

});

