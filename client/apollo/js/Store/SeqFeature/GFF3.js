define( [
            'dojo/_base/declare',
            'dojo/_base/lang',
            'dojo/_base/array',
            'dojo/Deferred',
            'WebApollo/ProjectionUtils',
            'WebApollo/Store/SeqFeature/GlobalStatsEstimationMixin',
            'JBrowse/Store/SeqFeature/GFF3',
            'JBrowse/Store/SeqFeature/GFF3/Parser',
            'WebApollo/Model/XHRBlob',
            'JBrowse/Util/GFF3',
            'JBrowse/Errors'
        ],
        function(
            declare,
            lang,
            array,
            Deferred,
            ProjectionUtils,
            GlobalStatsEstimationMixin,
            GFF3,
            Parser,
            XHRBlob,
            GFF3Parser,
            Errors
        ) {

return declare([ GFF3 ],

 /**
  * @lends JBrowse.Store.SeqFeature.GFF3
  */
{

    constructor: function( args ) {
        console.log('over-ridden constructor with');
        console.log(args);
        // this.data = args.blob ||
        //     new XHRBlob( this.resolveUrl(
        //         this._evalConf(args.urlTemplate)
        //         )
        //     );
        this.data = new XHRBlob( this.resolveUrl(
                this._evalConf(args.urlTemplate)
                )
            );
        this.features = [];
        this._loadFeatures();
    },

    // _estimateGlobalStats: function(refseq) {
    //     var deferred = new Deferred();
    //     refseq = refseq || this.refSeq;
    //     var sequenceListObject = ProjectionUtils.parseSequenceList(refseq.name);
    //     var timeout = this.storeTimeout || 3000;
    //     var startTime = new Date();
    //
    //     var statsFromInterval = function( length, callback ) {
    //         var thisB = this;
    //         var sampleCenter;
    //         if (sequenceListObject[0].reverse) {
    //             sampleCenter = refseq.end * 0.75 + refseq.start * 0.25;
    //         }
    //         else {
    //             sampleCenter = refseq.start * 0.75 + refseq.end * 0.25;
    //         }
    //         var start = Math.max( 0, Math.round( sampleCenter - length/2 ) );
    //         var end = Math.min( Math.round( sampleCenter + length/2 ), refseq.end );
    //         var unprojectedArray = ProjectionUtils.unProjectCoordinates(refseq.name, start, end);
    //         var unprojectedStart = unprojectedArray[0];
    //         var unprojectedEnd = unprojectedArray[1];
    //         var features = [];
    //         this._getFeatures({
    //                 ref: sequenceListObject[0].name,
    //                 start: unprojectedStart,
    //                 end:unprojectedEnd
    //             },
    //             function( feature ) {
    //                 features.push(feature);
    //             },
    //             function( error ) {
    //                 features = array.filter(
    //                     features,
    //                     function(f) {
    //                         return f.get('start') >= unprojectedStart && f.get('end') <= unprojectedEnd;
    //                     }
    //                 );
    //                 callback.call( thisB, length,
    //                     {
    //                         featureDensity: features.length / length,
    //                         _statsSampleFeatures: features.length,
    //                         _statsSampleInterval: { ref: refseq.name, start: unprojectedStart, end: unprojectedEnd, length: length }
    //                     });
    //             },
    //             function( error ) {
    //                 callback.call( thisB, length,  null, error );
    //             });
    //     };
    //
    //     var maybeRecordStats = function( interval, stats, error ) {
    //         if( error ) {
    //             if( error.isInstanceOf(Errors.DataOverflow) ) {
    //                 console.log( 'Store statistics found chunkSizeLimit error, using empty: '+(this.source||this.name) );
    //                 deferred.resolve( { featureDensity: 0, error: 'global stats estimation found chunkSizeError' } );
    //             }
    //             else {
    //                 deferred.reject( error );
    //             }
    //         } else {
    //             var refLen = refseq.end - refseq.start;
    //             if( stats._statsSampleFeatures >= 300 || interval * 2 > refLen || error ) {
    //                 console.log( 'WA Store statistics: '+(this.source||this.name), stats );
    //                 deferred.resolve( stats );
    //             } else if( ((new Date()) - startTime) < timeout ) {
    //                 statsFromInterval.call( this, interval * 2, maybeRecordStats );
    //             } else {
    //                 console.log( 'Store statistics timed out: '+(this.source||this.name) );
    //                 deferred.resolve( { featureDensity: 0, error: 'global stats estimation timed out' } );
    //             }
    //         }
    //     };
    //
    //     statsFromInterval.call( this, 100, maybeRecordStats );
    //     return deferred;
    // },

    _loadFeatures: function() {
        var thisB = this;
        var features = this.bareFeatures = [];

        var featuresSorted = true;
        var seenRefs = this.refSeqs = {};
        var parser = new Parser(
            {
                featureCallback: function(fs) {
                    array.forEach( fs, function( feature ) {
                                       var prevFeature = features[ features.length-1 ];
                                       var regRefName = thisB.browser.regularizeReferenceName( feature.seq_id );
                                       if( regRefName in seenRefs && prevFeature && prevFeature.seq_id != feature.seq_id )
                                           featuresSorted = false;
                                       if( prevFeature && prevFeature.seq_id == feature.seq_id && feature.start < prevFeature.start )
                                           featuresSorted = false;

                                       if( !( regRefName in seenRefs ))
                                           seenRefs[ regRefName ] = features.length;

                                       features.push( feature );
                                   });
                },
                endCallback:     function()  {
                    if( ! featuresSorted ) {
                        features.sort( thisB._compareFeatureData );
                        // need to rebuild the refseq index if changing the sort order
                        thisB._rebuildRefSeqs( features );
                    }

                    thisB._estimateGlobalStats()
                         .then( function( stats ) {
                                    thisB.globalStats = stats;
                                    thisB._deferred.stats.resolve();
                                });

                    thisB._deferred.features.resolve( features );
                    console.log('Parse final: ' +features.length);
                }
            });
        var fail = lang.hitch( this, '_failAllDeferred' );
        // parse the whole file and store it
        var sequenceListObject = ProjectionUtils.parseSequenceList(thisB.refSeq.name);
        var reverse = sequenceListObject[0].reverse ;
        var lines = [];
        // this.data.reverseFetchLines(
        this.data.fetchLines(
            function( line ) {
                if(reverse && line.length>30) {
                    // console.log('reversing ->['+line+']');
                    line = line.split("").reverse().join("").replace(" ", "\t").slice(0, -1).replace('\n', '');
                }
                if(line.length > 30){
                    // console.log('unprojecting ->['+line+']');
                    line = ProjectionUtils.unProjectGFF3(thisB.refSeq.name,line);
                    // console.log('unprojectED ->['+line+']');
                }
                console.log(line) ;
                lines.push(line);
                try {
                    parser.addLine(line);
                } catch(e) {
                    fail('Error parsing GFF3.');
                    throw e;
                }
            },
            lang.hitch( parser, 'finish' ),
            fail
        );
    },

    // _getFeatures: function( query, featureCallback, finishedCallback, errorCallback ) {
    //     var thisB = this;
    //     thisB._deferred.features.then( function() {
    //         thisB._search( query, featureCallback, finishedCallback, errorCallback );
    //     });
    // },

    _search: function( query, featureCallback, finishCallback, errorCallback ) {
        // search in this.features, which are sorted
        // by ref and start coordinate, to find the beginning of the
        // relevant range
        var bare = this.bareFeatures;
        var converted = this.features;

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

        var refName = this.browser.regularizeReferenceName( chrName );

        var i = this.refSeqs[ refName ];
        if( !( i >= 0 )) {
            finishCallback();
            return;
        }

        var checkEnd = 'start' in query
            ? function(f) { return f.get('end') >= min; }
            : function() { return true; };

        // var checkEnd = 'start' in query
        //     ? function(f) {
        //         f = ProjectionUtils.projectJSONFeature(f,query.ref);
        //         return f.get('end') >= min;
        //     }
        //     : function() { return true; };

        for( ; i<bare.length; i++ ) {
            // lazily convert the bare feature data to JBrowse features
            var f = converted[i] ||
                ( converted[i] = function(b,i) {
                      bare[i] = false;
                      return this._formatFeature( b );
                  }.call( this, bare[i], i )
                );
            f = ProjectionUtils.unprojectJSONFeature(f,query.ref);
            // features are sorted by ref seq and start coord, so we
            // can stop if we are past the ref seq or the end of the
            // query region
            if( f._reg_seq_id != refName || f.get('start') > max )
                break;

            if( checkEnd( f ) ) {
                featureCallback( f );
            }
        }

        finishCallback();
    }


});
});
