define([
           'dojo/_base/declare', 
           'dojo/_base/array',
           'JBrowse/View/Track/Alignments',
           'WebApollo/View/Track/DraggableHTMLFeatures', 
           'JBrowse/Util'
       ],
       function(
           declare,
           array, 
           AlignmentsTrack,
           DraggableTrack, 
           Util
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
    }

} );

});
