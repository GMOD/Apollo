define( [
            'dojo/_base/declare',
            'dojo/_base/lang',
            'dojo/_base/array',
            'dojo/_base/url',
            'JBrowse/Util',
            'JBrowse/Model/XHRBlob',
            'WebApollo/JSONUtils',
            'WebApollo/ProjectionUtils',
            './BigWig/Window',
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
            Window,
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
                console.log('region status name: '+this.refSeq.name );
                var sequenceListObject = ProjectionUtils.parseSequenceList(thisB.refSeq.name);
                var unprojectedArray = ProjectionUtils.unProjectCoordinates(thisB.refSeq.name, query.start, query.end);
                var unprojectedStart = unprojectedArray[0];
                var unprojectedEnd = unprojectedArray[1];
                console.log('start / end : '+start + ' ' + end + ' ' + sequenceListObject[0].name);
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
                        //console.log( '_getRegionStats done', s );
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

    getUnzoomedView: function() {
        if (!this.unzoomedView) {
            var cirLen = 4000;
            var nzl = this.zoomLevels[0];
            if (nzl) {
                cirLen = this.zoomLevels[0].dataOffset - this.unzoomedIndexOffset;
            }
            this.unzoomedView = new Window( this, this.unzoomedIndexOffset, cirLen, false );
        }
        return this.unzoomedView;
    },


    _getView: function( scale ) {
        var basesPerPx = 1/scale;
        //console.log('getting view for '+basesPerSpan+' bases per span');
        var maxLevel = this.zoomLevels.length;
        if( ! this.fileSize ) // if we don't know the file size, we can't fetch the highest zoom level :-(
            maxLevel--;
        for( var i = maxLevel; i > 0; i-- ) {
            var zh = this.zoomLevels[i];
            if( zh && zh.reductionLevel <= 2*basesPerPx ) {
                var indexLength = i < this.zoomLevels.length - 1
                    ? this.zoomLevels[i + 1].dataOffset - zh.indexOffset
                    : this.fileSize - 4 - zh.indexOffset;
                //console.log( 'using zoom level '+i);
                return new Window( this, zh.indexOffset, indexLength, true );
            }
        }
        //console.log( 'using unzoomed level');
        return this.getUnzoomedView();
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
        // TODO:
        // return false ;
        var thisB = this;
        seqName = thisB.browser.regularizeReferenceName( seqName );
        this._deferred.features.then(function() {
            callback( seqName in thisB.refsByName );
        }, errorCallback );
    },


    _getFeatures: function( query, featureCallback, endCallback, errorCallback ) {

        // parse sequenceList from query.ref
        var chrName, min, max ;
        console.log('REFREFREF');
        console.log(query.ref);
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

        chrName = this.browser.regularizeReferenceName(chrName);
        console.log(chrName+': reading min['+min+'] max['+max+'] from '+query.start + ' ' + query.end);
        v.readWigData( chrName, min, max, dojo.hitch( this, function( features ) {
            array.forEach( features || [], featureCallback );
            endCallback();
        }), errorCallback );
    }


});

});
