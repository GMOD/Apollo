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
                var sequenceListObject = ProjectionUtils.parseSequenceList(thisB.refSeq.name);
                var unprojectedArray = ProjectionUtils.unProjectCoordinates(thisB.refSeq.name, query.start, query.end);
                var unprojectedStart = unprojectedArray[0];
                var unprojectedEnd = unprojectedArray[1];
                var s = {
                    scoreMax: -Infinity,
                    scoreMin: Infinity,
                    scoreSum: 0,
                    scoreSumSquares: 0,
                    basesCovered: unprojectedEnd - unprojectedStart,
                    featureCount: 0
                };
                thisB.getFeatures(
                    {
                        ref: sequenceListObject[0].name,
                        start: unprojectedStart,
                        end: unprojectedEnd
                    }
                    ,
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
                        callback( s );
                    },
                    function(error) {
                        callback( null, error );
                    }
                );
            }
        });

        cache.get(
            {
                ref: sequenceListObject[0].name,
                start: unprojectedStart,
                end: unprojectedEnd
            }
            ,
            function( stats, error ) {
                if( error )
                    errorCallback( error );
                else
                    successCallback( stats );
            });

    },



    _getFeatures: function( query, featureCallback, endCallback, errorCallback ) {

        // parse sequenceList from query.ref
        var chrName, min, max ;
        if(ProjectionUtils.isSequenceList(query.ref)){
            var sequenceListObject = ProjectionUtils.parseSequenceList(query.ref);
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

        chrName = this.browser.regularizeReferenceName(chrName);
        v.readWigData( chrName, min, max, dojo.hitch( this, function( features ) {
            array.forEach( features || [], featureCallback );
            endCallback();
        }), errorCallback );
    }


});

});
