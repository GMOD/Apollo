define([
           'dojo/_base/declare',
           'dojo/_base/lang',
           'dojo/_base/array',
           'dojo/Deferred',
           'dojo/query',
           'JBrowse/Model/XHRBlob',
           'WebApollo/Store/TabixIndexedFile',
           'WebApollo/Store/SeqFeature/GlobalStatsEstimationMixin',
           'WebApollo/ProjectionUtils',
           'WebApollo/Store/SeqFeature/GFF3/Parser',
           'JBrowse/Store/SeqFeature/GFF3Tabix',
           'JBrowse/Util/GFF3'
       ],
       function(
           declare,
           lang,
           array,
           Deferred,
           query,
           XHRBlob,
           TabixIndexedFile,
           GlobalStatsEstimationMixin,
           ProjectionUtils,
           Parser,
           GFF3Tabix,
           GFF3
       ) {


return declare( [  GFF3Tabix , GlobalStatsEstimationMixin ],
{

    // constructor: function( args ) {
    //     var thisB = this;
    //
    //     var tbiBlob = args.tbi ||
    //         new XHRBlob(
    //             this.resolveUrl(
    //                 this.getConf('tbiUrlTemplate',[]) || this.getConf('urlTemplate',[])+'.tbi'
    //             )
    //         );
    //
    //     var fileBlob = args.file ||
    //         new XHRBlob(
    //             this.resolveUrl( this.getConf('urlTemplate',[]) )
    //         );
    //
    //     this.indexedData = new TabixIndexedFile(
    //         {
    //             tbi: tbiBlob,
    //             file: fileBlob,
    //             browser: this.browser,
    //             chunkSizeLimit: args.chunkSizeLimit || 1000000
    //         });
    //
    //
    //
    //
    //     this.getHeader()
    //         .then( function( header ) {
    //                 thisB._deferred.features.resolve({success:true});
    //                 thisB._estimateGlobalStats()
    //                     .then(
    //                         function( stats ) {
    //                             thisB.globalStats = stats;
    //                             thisB._deferred.stats.resolve( stats );
    //                         },
    //                         lang.hitch( thisB, '_failAllDeferred' )
    //                     );
    //             },
    //             lang.hitch( thisB, '_failAllDeferred' )
    //         );
    // },


    _getFeatures: function( query, featureCallback, finishedCallback, errorCallback ) {
        var thisB = this;
        var f=featureCallback;
        var parser = new Parser(
            {
                featureCallback: function(fs) {
                    array.forEach( fs, function( feature ) {
                                       var feat = thisB._formatFeature(feature);
                                       f(feat);
                                   });
                },
                endCallback: function() {
                    finishedCallback();
                }
            });

        thisB.getHeader().then( function() {
            // var refSeqName = query.ref || thisB.refSeq.name ;
            var refSeqName = thisB.refSeq.name ;
            var min, max ;
            var reverse = false ;
            var chrName ;
            if(ProjectionUtils.isSequenceList(refSeqName)){
                var sequenceListObject = ProjectionUtils.parseSequenceList(refSeqName);
                chrName = sequenceListObject[0].name ;
                reverse = sequenceListObject[0].reverse;
            }
            else{
                chrName = query.ref ;
            }
            min = query.start ;
            max = query.end ;

            // console.log(min + ' ' + max);

            thisB.indexedData.getLines(
                chrName,
                min,
                max,
                function( line ) {
                    if(ProjectionUtils.isSequenceList(refSeqName)) {
                        // console.log(refSeqName);
                        // console.log(line);
                        var unprojectedArray = ProjectionUtils.unProjectCoordinates(refSeqName, line.start, line.end);
                        min = unprojectedArray[0];
                        max = unprojectedArray[1];
                        line.start = min;
                        line.end = max;
                        line.fields[3] = min;
                        line.fields[4] = max;
                        if (reverse) {
                            line.fields[6] = ProjectionUtils.flipStrand(line.fields[6]);
                        }
                        // console.log('- VS -');
                        // console.log(line);
                    }

                    parser._buffer_feature( thisB.lineToFeature(line) );
                },
                function() {
                    parser.finish();
                },
                errorCallback
            );
        }, errorCallback );
    },

    lineToFeature: function( line ) {
        var attributes = GFF3.parse_attributes( line.fields[8] );
        var ref    = line.fields[0];
        var source = line.fields[1];
        var type   = line.fields[2];
        var strand = {'-':-1,'.':0,'+':1}[line.fields[6]];
        var remove_id;
        if( !attributes.ID ) {
            attributes.ID = [line.fields.join('/')];
            remove_id = true;
        }

        var refSeqName = this.refSeq.name ;
        var min, max ;
        var chrName;
        if(ProjectionUtils.isSequenceList(refSeqName)){
            var sequenceListObject = ProjectionUtils.parseSequenceList(refSeqName);
            chrName = sequenceListObject[0].name ;
            if(sequenceListObject[0].reverse){
                strand = ProjectionUtils.flipStrand(strand);
                // strand = -1 * strand ;
            }
            // var unprojectedArray = ProjectionUtils.unProjectCoordinates(refSeqName,line.start,line.end);
            // min = unprojectedArray[0];
            // max = unprojectedArray[1];
        }
        else{
            chrName = query.ref ;
        }
        min = line.start;
        max = line.end ;

        var featureData = {
            start:  min,
            end:    max,
            strand: strand,
            child_features: [],
            seq_id: chrName,
            attributes: attributes,
            type:   type,
            source: source,
            remove_id: remove_id
        };

        return featureData;
    }



});
});
