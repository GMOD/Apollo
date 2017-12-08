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

return declare([ GlobalStatsEstimationMixin , GFF3 ],

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
                }
            });
        var fail = lang.hitch( this, '_failAllDeferred' );
        // parse the whole file and store it
        this.data.fetchLines(
            function( line ) {
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
