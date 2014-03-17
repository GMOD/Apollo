define( [   'dojo/_base/declare',
            'dojo/_base/lang',
            'dojo/_base/array',
            'dojo/Deferred',
            'JBrowse/Model/SimpleFeature',
            'JBrowse/Store/SeqFeature',
            'JBrowse/Store/DeferredFeaturesMixin',
            'JBrowse/Store/DeferredStatsMixin',
            'JBrowse/Store/SeqFeature/GlobalStatsEstimationMixin',
            'JBrowse/Store/SeqFeature/GFF3/Parser', 
            'JBrowse/Store/SeqFeature/GFF3', 
            'WebApollo/SequenceOntologyUtils'
        ],
        function(
            declare,
            lang,
            array,
            Deferred,
            SimpleFeature,
            SeqFeatureStore,
            DeferredFeatures,
            DeferredStats,
            GlobalStatsEstimationMixin,
            Parser, 
            GFF3, 
            SeqOnto
        ) 
        {
            
return declare( [ GFF3 ], 
{
    /* overriding _loadFeatures to handle common case of three-level gene-transcript-exon hierarchy, 
     *   when three-level hierarchy is fully represented in the data model, currently HTMLFeatures and DraggableHTMLFeatures can't render it correctly 
     *   (always renders the top two levels of a feature hierarchy)
     *   therefore when detecting this case, ApolloGFF3 removes the top "gene" level of the hierarchy
     */
    _loadFeatures: function() {
        //        this.inherited( arguments );
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
                    var newfeats = [];
                    for (var i=0; i<features.length; i++) {
                        var oldfeat = features[i];
                        var type = oldfeat.type;
                        var transformed = false;
                        if (SeqOnto.geneTerms[type] && oldfeat.child_features)  {
                            // special-casing for typical gene-mRNA-exon hierarchy:
                            // if it's a gene, and find transcript term underneath, promote all children to top and strip out parent attribute
                            var subfeats = oldfeat.child_features;
                            var has_transcript = false;
                            for (var k=0; k<subfeats.length; k++)  {
                                var subfeat = subfeats[k];
                                if (lang.isArray(subfeat)) { 
                                    // hack due to weirdness in current GFF3 parser data struct where all subfeats are wrapped with an extra array
                                    subfeat = subfeat[0];
                                }
                                if (SeqOnto.transcriptTerms[subfeat.type]) { has_transcript = true; }
                                break;
                            }
                            if (has_transcript) {
                                for (k=0; k<subfeats.length; k++) {
                                    var subfeat = subfeats[k];
                                    // hack due to weirdness in current GFF3 parser data struct where all subfeats are wrapped with an extra array
                                    if (lang.isArray(subfeat)) { subfeat = subfeat[0]; }
                                    var test = true;
                                    if (subfeat.attributes.Parent) {
                                        subfeat.attributes.ParentGene = subfeat.attributes.Parent; 
                                        // just nulling out causes errors when GFF3._formatData tries to join attributes
                                        // so need to delete, even though delete is a much slower operation
                                        // subfeat.attributes.Parent = null;
                                        delete subfeat.attributes.Parent;
                                    }
                                    newfeats.push( subfeat );
                                }
                                transformed = true;
                            }
                        }
                        if (! transformed) {
                            newfeats.push( oldfeat );
                        }
                    }
                    thisB.bareFeatures = newfeats;
                    features = newfeats;
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

        // parse the whole file and store it
        this.data.fetchLines(
            function( line ) {
                parser.addLine(line);
            },
            lang.hitch( parser, 'finish' ),
            lang.hitch( this, '_failAllDeferred' )
        );
    }

});
});
