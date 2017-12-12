define( [
            'dojo/_base/declare',
            'dojo/_base/lang',
            'dojo/_base/array',
            'dojo/Deferred',
            'WebApollo/ProjectionUtils',
            'WebApollo/Store/SeqFeature/GlobalStatsEstimationMixin',
            'JBrowse/Store/SeqFeature/GFF3',
            'JBrowse/Store/SeqFeature/GFF3/Parser'
        ],
        function(
            declare,
            lang,
            array,
            Deferred,
            ProjectionUtils,
            GlobalStatsEstimationMixin,
            GFF3,
            Parser
        ) {

return declare([ GFF3  , GlobalStatsEstimationMixin],

 /**
  * @lends JBrowse.Store.SeqFeature.GFF3
  */
{


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
                // console.log(line) ;
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


    _search: function( query, featureCallback, finishCallback, errorCallback ) {
        // search in this.features, which are sorted
        // by ref and start coordinate, to find the beginning of the
        // relevant range
        var bare = this.bareFeatures;
        var converted = this.features;

        // parse sequenceList from query.ref
        var chrName ;
        if(ProjectionUtils.isSequenceList(query.ref)){
            var sequenceListObject = ProjectionUtils.parseSequenceList(query.ref);
            chrName = sequenceListObject[0].name;
        }
        else{
            chrName = query.ref ;
        }

        var refName = this.browser.regularizeReferenceName( chrName );

        var i = this.refSeqs[ refName ];
        if( !( i >= 0 )) {
            finishCallback();
            return;
        }

        var checkEnd = 'start' in query
            ? function(f) { return f.get('end') >= query.start; }
            : function() { return true; };


        for( ; i<bare.length; i++ ) {
            // lazily convert the bare feature data to JBrowse features
            var f = converted[i] ||
                ( converted[i] = function(b,i) {
                      bare[i] = false;
                      return this._formatFeature( b );
                  }.call( this, bare[i], i )
                );
            // f = ProjectionUtils.unprojectJSONFeature(f,query.ref);
            // features are sorted by ref seq and start coord, so we
            // can stop if we are past the ref seq or the end of the
            // query region
            if( f._reg_seq_id != refName || f.get('start') > query.end )
                break;

            if( checkEnd( f ) ) {
                featureCallback( f );
            }
        }

        finishCallback();
    }


});
});
