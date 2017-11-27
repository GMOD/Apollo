define( [
            'dojo/_base/declare',
            'dojo/_base/lang',
            'dojo/_base/array',
            'dojo/_base/url',
            'JBrowse/Util',
            'JBrowse/Model/XHRBlob',
            'WebApollo/JSONUtils',
            'WebApollo/ProjectionUtils',
            'JBrowse/Store/SeqFeature/BigWig'
        ],
        function(
            declare,
            lang,
            array,
            urlObj,
            Util,
            XHRBlob,
            JSONUtils,
            ProjectionUtils,
            BigWigStore
        ) {
return declare( BigWigStore,

 /**
  * @lends JBrowse.Store.BigWig
  */
{

    /**
     * Data backend for reading wiggle data from BigWig or BigBed files.
     *
     * Adapted by Robert Buels from bigwig.js in the Dalliance Genome
     * Explorer which is copyright Thomas Down 2006-2010
     * @constructs
     */
    constructor: function( args ) {

        this.data = args.blob ||
            new XHRBlob( this.resolveUrl(
                             args.urlTemplate || 'data.bigwig'
                         )
                       );

        this.name = args.name || ( this.data.url && new urlObj( this.data.url ).path.replace(/^.+\//,'') ) || 'anonymous';

        this.storeTimeout = 3000;


        this._load();
    },



    _getRegionStats: function( query, successCallback, errorCallback ) {
        var thisB = this;
        var cache = thisB._regionStatsCache = thisB._regionStatsCache || new LRUCache({
            name: 'regionStatsCache',
            maxSize: 1000, // cache stats for up to 1000 different regions
            sizeFunction: function( stats ) { return 1; },
            fillCallback: function( query, callback ) {
                //console.log( '_getRegionStats', query );
                var s = {
                    scoreMax: -Infinity,
                    scoreMin: Infinity,
                    scoreSum: 0,
                    scoreSumSquares: 0,
                    basesCovered: query.end - query.start,
                    featureCount: 0
                };
                thisB.getFeatures( query,
                    function( feature ) {
                        var score = feature.get('score') || 0;
                        s.scoreMax = Math.max( score, s.scoreMax );
                        s.scoreMin = Math.min( score, s.scoreMin );
                        s.scoreSum += score;
                        s.scoreSumSquares += score*score;
                        s.featureCount++;
                    },
                    function() {
                        s.scoreMean = s.featureCount ? s.scoreSum / s.featureCount : 0;
                        s.scoreStdDev = thisB._calcStdFromSums( s.scoreSum, s.scoreSumSquares, s.featureCount );
                        s.featureDensity = s.featureCount / s.basesCovered;
                        //console.log( '_getRegionStats done', s );
                        callback( s );
                    },
                    function(error) {
                        callback( null, error );
                    }
                );
            }
        });

        cache.get( query,
            function( stats, error ) {
                if( error )
                    errorCallback( error );
                else
                    successCallback( stats );
            });

    },

    getReferenceSequence: function( query, seqCallback, errorCallback ) {

        // insert the `replacement` string into `str` at the given
        // `offset`, putting in `length` characters.
        function replaceAt( str, offset, replacement ) {
            var rOffset = 0;
            if( offset < 0 ) {
                rOffset = -offset;
                offset = 0;
            }

            var length = Math.min( str.length - offset, replacement.length - rOffset );

            return str.substr( 0, offset ) + replacement.substr( rOffset, length ) + str.substr( offset + length );
        }

        // pad with spaces at the beginning of the string if necessary
        var len = query.end - query.start;
        var sequence = '';
        while( sequence.length < len )
            sequence += ' ';

        var thisB = this;
        this.getFeatures( lang.mixin({ reference_sequences_only: true }, query ),
            function( f ) {
                var seq = f.get('residues') || f.get('seq');
                if( seq )
                    sequence = replaceAt( sequence, f.get('start')-query.start, seq );
            },
            function() {
                seqCallback( sequence );
            },
            errorCallback
        );
    },

    /**
     * Interrogate whether a store has data for a given reference
     * sequence.  Calls the given callback with either true or false.
     *
     * Implemented as a binary interrogation because some stores are
     * smart enough to regularize reference sequence names, while
     * others are not.
     */
    hasRefSeq: function( seqName, callback, errorCallback ) {
        return false ;
        // var thisB = this;
        // seqName = thisB.browser.regularizeReferenceName( seqName );
        // this._deferred.features.then(function() {
        //     callback( seqName in thisB.refsByName );
        // }, errorCallback );
    },

    _getFeatures: function( query, featureCallback, endCallback, errorCallback ) {

        // parse sequenceList from query.ref
        var chrName, min, max ;
        if(ProjectionUtils.isSequenceList(query.ref)){
            var sequenceListObject = ProjectionUtils.parseSequenceList(query.ref);
            console.log(sequenceListObject);
            // unproject start and end
            var featureLocationArray = ProjectionUtils.unProjectCoordinates(query.ref, query.start, query.end);
            // rebuild the query
            chrName = sequenceListObject[0].name;
            min = featureLocationArray[0];
            max = featureLocationArray[1];
        }
        else{
            chrName = query.ref ;
            min = query.start ;
            max = query.end ;
        }

        var v = query.basesPerSpan ? this.getView( 1/query.basesPerSpan ) :
                       query.scale ? this.getView( query.scale )          :
                                     this.getView( 1 );

        if( !v ) {
            endCallback();
            return;
        }

        v.readWigData( chrName, min, max, dojo.hitch( this, function( features ) {
            array.forEach( features || [], featureCallback );
            endCallback();
        }), errorCallback );
    }


});

});
