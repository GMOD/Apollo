define([
           'dojo/_base/declare', 
           'dojo/_base/array',
           'dojo/_base/lang',
           'JBrowse/View/Track/Alignments',
           'WebApollo/View/Track/DraggableHTMLFeatures', 
           'JBrowse/Util',
            'WebApollo/JSONUtils'
       ],
       function(
           declare,
           array,
           lang,
           AlignmentsTrack,
           DraggableTrack, 
           Util,
           JSONUtils
       ) {

return declare([ DraggableTrack, AlignmentsTrack ], {

    constructor: function( args )  {
    // forcing store to create subfeatures, unless config.subfeatures explicitly set to false
    //     default is set to true in _defaultConfig()
        //  this.store.createSubfeatures = this.config.subfeatures;
        if (this.config.style.showSubfeatures) { this.store.createSubfeatures = true; }
    }, 

    _defaultConfig: function()  {
        var thisConfig = Util.deepUpdate(
//       return Util.deepUpdate(
            dojo.clone( this.inherited(arguments) ),
            {
                layoutPitchY: 2, 
//                subfeatures: true,
                maxFeatureScreenDensity: 0.5,
                style: {
                    className: "bam-read", 
                    renderClassName: null, 
                    arrowheadClass: null, 
                    centerChildrenVertically: false, 
                    showSubfeatures: true, 
                    showMismatches: true, 
                    showLabels: false, 
                    showMismatchResidues: true,  // when rendering mismatches, whether to render residues text when zoomed in and sizing works
                    subfeatureClasses: {
                        M: "cigarM", 
        //              D: "cigarD",
                        D: null, // not rendering deletions as subfeats, relying on drawMismatches instead
                        N: "cigarN",
                        E: "cigarEQ",  /* "=" converted to "E" in BAM/LazyFeature subfeature construction */
                        X: "cigarX", 
        //              I: "cigarI"
                        I: null // not rendering insertions as subfeats, relying on drawMismatches instead
                    }
                }
            }
        );
        return thisConfig;
    }, 

    /**
     * draw base-mismatches on the feature
     */
    _drawMismatches: function( feature, featDiv, scale, displayStart, displayEnd ) {
        var featLength = displayEnd - displayStart;
        // recall: scale is pixels/basepair
        // if ( featLength*scale > 1 && scale >= 1) {  // alternatively, also require zoomed in scale min (1px/bp  in this case)
        if ( featLength*scale > 1) {     
            var mismatches = this._getMismatches( feature );
            var charSize = this.getCharacterMeasurements();
            var drawChars = (this.config.style.showMismatchResidues && 
                             (scale >= charSize.w) && 
                             (charSize.h <= (this.glyphHeight + this.glyphHeightPad))
                             );
            array.forEach( mismatches, function( mismatch ) {
                var start = feature.get('start') + mismatch.start;
                // GAH: _MismatchesMixin._getMismatches is creating insertions with length = 1, 
                //   but length of insertion should really by 0, since JBrowse internally uses zero-interbase coordinates
                //   fixing here for now, since changing in _MismatchesMixin could have unexpected consequences
                // if (mismatch.type == 'insertion') { mismatch.length = 0; }
                var end = start + mismatch.length;

                // if the feature has been truncated to where it doesn't cover
                // this mismatch anymore, just skip this mismatch
                if ( end <= displayStart || start >= displayEnd )
                    return;

                var base = mismatch.base;
                var mDisplayStart = Math.max( start, displayStart );
                var mDisplayEnd = Math.min( end, displayEnd );
                var mDisplayWidth = mDisplayEnd - mDisplayStart;
                var overall = dojo.create('span',  {
                    className: 'align_'+mismatch.type + ' ' + mismatch.type + ' base_'+base.toLowerCase(),
                    style: {
                        position: 'absolute',
                        left: 100 * ( mDisplayStart - displayStart)/featLength + '%',
                        width: scale*mDisplayWidth>1 ? 100 * mDisplayWidth/featLength + '%' : '1px'
                    }
                }, featDiv );
                overall.mismatch = mismatch;

                // give the mismatch a mouseover if not drawing a character with the mismatch base
                if( ! drawChars ) {
                    if (mismatch.type == 'deletion') { overall.title = mismatch.length; }
                    else  { overall.title = base; }
                }

                if( drawChars && mismatch.length <= 20 ) {
                    for( var i = 0; i<mismatch.length; i++ ) {
                        var basePosition = start + i;
                        if( basePosition >= mDisplayStart && basePosition <= mDisplayEnd ) {
                            dojo.create('span',{
                                            className: 'base base_'+base.toLowerCase(),
                                            style: {
                                                position: 'absolute',
                                                width: scale+'px',
                                                left: (basePosition-mDisplayStart)/mDisplayWidth*100 + '%'
                                            },
                                            innerHTML: base
                                        }, overall );
                        }
                    }
                }
            }, this );
        }
    },

    /**
     * Override
     * @param args
     */
    fillFeatures: function( args ) {
        var blockIndex = args.blockIndex;
        var block = args.block;
        var leftBase = args.leftBase;
        var rightBase = args.rightBase;
        var scale = args.scale;
        var stats = args.stats;
        var containerStart = args.containerStart;
        var containerEnd = args.containerEnd;
        var finishCallback = args.finishCallback;
        var browser = this.browser;
        this.scale = scale;
        block.featureNodes = {};

        //determine the glyph height, arrowhead width, label text dimensions, etc.
        if( !this.haveMeasurements ) {
            this.measureStyles();
            this.haveMeasurements = true;
        }

        var labelScale       = this.config.style.labelScale       || stats.featureDensity * this.config.style._defaultLabelScale;
        var descriptionScale = this.config.style.descriptionScale || stats.featureDensity * this.config.style._defaultDescriptionScale;
        var curTrack = this;

        var featCallback = dojo.hitch(this,function( feature ) {
            var uniqueId = feature.id();
            // project feature after it is fetched from the store
            if ( !feature.isProjected ) {
                feature = JSONUtils.projectJSONFeature(feature, this.refSeq.name);
            }

            if( ! this._featureIsRendered( uniqueId ) ) {
                if( this.filterFeature( feature ) )  {
                    var render = 1;
                    if (typeof this.renderFilter === 'function')
                        render = this.renderFilter(feature);

                    if (render === 1) {
                        this.addFeatureToBlock( feature, uniqueId, block, scale, labelScale, descriptionScale, containerStart, containerEnd );
                    }
                }
            }
        });

        this.store.getFeatures( {
                ref: this.refSeq.name,
                start: leftBase,
                end: rightBase
            },
            featCallback,
            function ( args ) {
                curTrack.heightUpdate(curTrack._getLayout(scale).getTotalHeight(),
                    blockIndex);
                if ( args && args.maskingSpans ) {
                    //note: spans have to be inverted
                    var invSpan = [];
                    invSpan[0] = { start: leftBase };
                    var i = 0;
                    for ( var span in args.maskingSpans) {
                        if (args.maskingSpans.hasOwnProperty(span)) {
                            span = args.maskingSpans[span];
                            invSpan[i].end = span.start;
                            i++;
                            invSpan[i] = { start: span.end };
                        }
                    }
                    invSpan[i].end = rightBase;
                    if (invSpan[i].end <= invSpan[i].start) {
                        invSpan.splice(i,1); }
                    if (invSpan[0].end <= invSpan[0].start) {
                        invSpan.splice(0,1); }
                    curTrack.maskBySpans( invSpan, args.maskingSpans );
                }
                finishCallback();
            },
            function( error ) {
                console.error( error, error.stack );
                curTrack.fillBlockError( blockIndex, block, error );
                finishCallback();
            }
        );
    }

} );

});
