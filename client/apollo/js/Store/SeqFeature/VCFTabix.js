define([
           'dojo/_base/declare',
            'dojo/_base/array',
           'dojo/_base/lang',
           'dojo/Deferred',
            'JBrowse/Errors',
           'WebApollo/ProjectionUtils',
           'JBrowse/Store/SeqFeature/VCFTabix'
       ],
       function(
           declare,
           array,
           lang,
           Deferred,
           Errors,
           ProjectionUtils,
           VCFTabix
       ) {


// subclass the VCFTabix to modify the parsed items a little
// bit so that the range filtering in TabixIndexedFile will work.  VCF
// files don't actually have an end coordinate, so we have to make it
// here.  also convert coordinates to interbase.
return declare( [ VCFTabix ],
{
    _getFeatures: function( query, featureCallback, finishedCallback, errorCallback ) {
        var thisB = this;
        thisB.getVCFHeader().then( function() {
            var min , max , chrName ;
            var refSeqName = query.ref || thisB.refSeq.name;
            if(ProjectionUtils.isSequenceList(refSeqName)){
                var sequenceListObject = ProjectionUtils.parseSequenceList(query.ref);
                chrName = sequenceListObject[0].name ;
                var unprojectedArray = ProjectionUtils.unProjectCoordinates(refSeqName,query.start,query.end);
                min = unprojectedArray[0];
                max = unprojectedArray[1];
            }
            else{
                min = query.start ;
                max = query.end ;
                chrName = query.ref ;
            }


            thisB.indexedData.getLines(
                chrName,
                min,
                max,
                function( line ) {
                    var f = thisB.lineToFeature( line );
                    //console.log(f);
                    featureCallback( f );
                    //return f;
                },
                finishedCallback,
                errorCallback
            );
        }, errorCallback );
    },


    /**
     * TODO: use different mixin, thi is from BAM.js
     *
     * Override _estimateGlobalStats
     */
    _estimateGlobalStats: function(refseq) {
        var deferred = new Deferred();
        refseq = refseq || this.refSeq;
        var sequenceListObject = ProjectionUtils.parseSequenceList(refseq.name);
        var timeout = this.storeTimeout || 3000;
        var startTime = new Date();

        var statsFromInterval = function( length, callback ) {
            var thisB = this;
            var sampleCenter;
            if (sequenceListObject[0].reverse) {
                sampleCenter = refseq.end * 0.75 + refseq.start * 0.25;
            }
            else {
                sampleCenter = refseq.start * 0.75 + refseq.end * 0.25;
            }
            var start = Math.max( 0, Math.round( sampleCenter - length/2 ) );
            var end = Math.min( Math.round( sampleCenter + length/2 ), refseq.end );
            var unprojectedArray = ProjectionUtils.unProjectCoordinates(refseq.name, start, end);
            var unprojectedStart = unprojectedArray[0];
            var unprojectedEnd = unprojectedArray[1];
            var features = [];
            this._getFeatures({
                    ref: sequenceListObject[0].name,
                    start: unprojectedStart,
                    end: unprojectedEnd
                },
                function( feature ) {
                    features.push(feature);
                },
                function( error ) {
                    features = array.filter(
                        features,
                        function(f) {
                            return f.get('start') >= unprojectedStart && f.get('end') <= unprojectedEnd;
                        }
                    );
                    callback.call( thisB, length,
                        {
                            featureDensity: features.length / length,
                            _statsSampleFeatures: features.length,
                            _statsSampleInterval: { ref: refseq.name, start: start, end: end, length: length }
                        });
                },
                function( error ) {
                    callback.call( thisB, length,  null, error );
                });
        };

        var maybeRecordStats = function( interval, stats, error ) {
            if( error ) {
                if( error.isInstanceOf(Errors.DataOverflow) ) {
                    console.log( 'Store statistics found chunkSizeLimit error, using empty: '+(this.source||this.name) );
                    deferred.resolve( { featureDensity: 0, error: 'global stats estimation found chunkSizeError' } );
                }
                else {
                    deferred.reject( error );
                }
            } else {
                var refLen = refseq.end - refseq.start;
                if( stats._statsSampleFeatures >= 300 || interval * 2 > refLen || error ) {
                    console.log( 'Store statistics: '+(this.source||this.name), stats );
                    deferred.resolve( stats );
                } else if( ((new Date()) - startTime) < timeout ) {
                    statsFromInterval.call( this, interval * 2, maybeRecordStats );
                } else {
                    console.log( 'Store statistics timed out: '+(this.source||this.name) );
                    deferred.resolve( { featureDensity: 0, error: 'global stats estimation timed out' } );
                }
            }
        };

        statsFromInterval.call( this, 100, maybeRecordStats );
        return deferred;
    },

});
});
