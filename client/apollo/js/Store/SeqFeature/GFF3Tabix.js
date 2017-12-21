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
                                       // console.log('doing feature callback');
                                       // console.log(feature);
                                       // console.log(feat);
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
                var unprojectedArray = ProjectionUtils.unProjectCoordinates(refSeqName,query.start,query.end);
                // console.log('input query: '+ refSeqName + ' ' + query.start + ' ' + query.end);
                min = unprojectedArray[0];
                max = unprojectedArray[1];
                // console.log('projected: '+ chrName+ ' ' + min + ' ' + max);
            }
            else{
                min = query.start ;
                max = query.end ;
                chrName = query.ref ;
            }

            // console.log(min + ' ' + max);

            var minLine = -1 ;
            var maxLine = -1 ;

            var estimabeBlocks = function(line){
                minLine = minLine === -1 || line.start < minLine ? line.start : minLine ;
                maxLine = maxLine === -1 || line.end > maxLine ? line.end : maxLine ;
            };

            var handleLines = function(line){
                parser._buffer_feature(  thisB.lineToFeature(line));
            };

            thisB.indexedData.getLines(
                chrName,
                min,
                max,
                estimabeBlocks,
                function() {
                    // parser.finish();
                    thisB.indexedData.getLines(
                        chrName,
                        minLine,
                        maxLine,
                        handleLines,
                        function() {
                            parser.finish();
                        },
                        errorCallback
                    );
                },
                errorCallback
            );


        }, errorCallback );
    }



});
});
