define( [
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/_base/Deferred',
        'dojo/_base/lang',
        'JBrowse/has',
        'JBrowse/Util',
        'WebApollo/JSONUtils',
        'WebApollo/ProjectionUtils',
        'JBrowse/Errors',
        'JBrowse/Model/XHRBlob',
        'JBrowse/Store/LRUCache',
        'JBrowse/Store/SeqFeature/BAM',
        'WebApollo/Store/SeqFeature/BAM/File'
    ],
    function(
        declare,
        array,
        Deferred,
        lang,
        has,
        Util,
        JSONUtils,
        ProjectionUtils,
        Errors,
        XHRBlob,
        LRUCache,
        BAMStore,
        WebApolloBAMFile
    ) {

    return declare(BAMStore, {

        constructor: function(args) {

            var bamBlob = args.bam ||
                new XHRBlob( this.resolveUrl(
                        args.urlTemplate || 'data.bam'
                    )
                );

            var baiBlob = args.bai ||
                new XHRBlob( this.resolveUrl(
                        args.baiUrlTemplate || ( args.urlTemplate ? args.urlTemplate+'.bai' : 'data.bam.bai' )
                    )
                );

            this.bam = new WebApolloBAMFile({
                store: this,
                data: bamBlob,
                bai: baiBlob,
                chunkSizeLimit: args.chunkSizeLimit
            });

            this.source = ( bamBlob.url  ? bamBlob.url.match( /\/([^/\#\?]+)($|[\#\?])/ )[1] :
                    bamBlob.blob ? bamBlob.blob.name : undefined ) || undefined;

            if( ! has( 'typed-arrays' ) ) {
                this._failAllDeferred( 'This web browser lacks support for JavaScript typed arrays.' );
                return;
            }

            this.bam.init({
                success: lang.hitch( this,
                    function() {
                        this._deferred.features.resolve({success:true});

                        this._estimateGlobalStats()
                            .then( lang.hitch(
                                this,
                                function( stats ) {
                                    this.globalStats = stats;
                                    this._deferred.stats.resolve({success:true});
                                }
                                ),
                                lang.hitch( this, '_failAllDeferred' )
                            );
                    }),
                failure: lang.hitch( this, '_failAllDeferred' )
            });

            this.storeTimeout = args.storeTimeout || 3000;


            // replace _fetchChunkFeatures with few changes
            this.bam._fetchChunkFeatures = function( chunks, chrId, min, max, featCallback, endCallback, errorCallback ) {
                var thisB = this;

                if( ! chunks.length ) {
                    endCallback();
                    return;
                }

                var chunksProcessed = 0;

                var cache = this.featureCache = this.featureCache || new LRUCache({
                        name: 'bamFeatureCache',
                        fillCallback: dojo.hitch( this, '_readChunk' ),
                        sizeFunction: function( features ) {
                            return features.length;
                        },
                        maxSize: 100000 // cache up to 100,000 BAM features
                    });

                // check the chunks for any that are over the size limit.  if
                // any are, don't fetch any of them
                for( var i = 0; i<chunks.length; i++ ) {
                    var size = chunks[i].fetchedSize();
                    if( size > this.chunkSizeLimit ) {
                        errorCallback( new Errors.DataOverflow('Too many BAM features. BAM chunk size '+Util.commifyNumber(size)+' bytes exceeds chunkSizeLimit of '+Util.commifyNumber(this.chunkSizeLimit)+'.' ) );
                        return;
                    }
                }

                var haveError;
                var pastStart;
                array.forEach( chunks, function( c ) {
                    cache.get( c, function( f, e ) {
                        if( e && !haveError )
                            errorCallback(e);
                        if(( haveError = haveError || e )) {
                            return;
                        }

                        for( var i = 0; i<f.length; i++ ) {
                            var feature = f[i];
                            if( feature._refID == chrId ) {
                                // on the right ref seq
                                var start = feature.isProjected ? feature.get('_original_start') : feature.get('start');
                                var end = feature.isProjected ? feature.get('_original_end') : feature.get('end');
                                if( start > max ) // past end of range, can stop iterating
                                    break;
                                else if( end >= min ) // must be in range
                                    featCallback( feature );
                            }
                        }
                        if( ++chunksProcessed == chunks.length ) {
                            endCallback();
                        }
                    });
                });
            }
        },

        /**
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

        /**
         * Override getFeatures to support BAM querying in a projected space.
         */
        getFeatures: function(query, featCallback, endCallback, errorCallback) {
            // parse sequenceList from query.ref
            var sequenceListObject = ProjectionUtils.parseSequenceList(query.ref);
            // unproject start and end
            var featureLocationArray = ProjectionUtils.unProjectCoordinates(query.ref, query.start, query.end);
            // rebuild the query
            query.ref = sequenceListObject[0].name;
            query.start = featureLocationArray[0];
            query.end = featureLocationArray[1];
            this._deferred.features.then(
                dojo.hitch( this, '_getFeatures', query, featCallback, endCallback, errorCallback ),
                errorCallback
            );
        }

    });

});
