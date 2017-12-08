define([
           'dojo/_base/declare',
           'dojo/_base/lang',
           'dojo/_base/array',
           'dojo/Deferred',
           'JBrowse/Model/XHRBlob',
           'JBrowse/Store/TabixIndexedFile',
           'WebApollo/Store/SeqFeature/GlobalStatsEstimationMixin',
           'WebApollo/ProjectionUtils',
           'JBrowse/Store/SeqFeature/GFF3/Parser',
           'JBrowse/Store/SeqFeature/GFF3Tabix',
           'JBrowse/Util/GFF3'
       ],
       function(
           declare,
           lang,
           array,
           Deferred,
           XHRBlob,
           TabixIndexedFile,
           GlobalStatsEstimationMixin,
           ProjectionUtils,
           Parser,
           GFF3Tabix,
           GFF3
       ) {


return declare( [ GlobalStatsEstimationMixin , GFF3Tabix ],
{

    constructor: function( args ) {
        var thisB = this;

        var tbiBlob = args.tbi ||
            new XHRBlob(
                this.resolveUrl(
                    this.getConf('tbiUrlTemplate',[]) || this.getConf('urlTemplate',[])+'.tbi'
                )
            );

        var fileBlob = args.file ||
            new XHRBlob(
                this.resolveUrl( this.getConf('urlTemplate',[]) )
            );

        this.indexedData = new TabixIndexedFile(
            {
                tbi: tbiBlob,
                file: fileBlob,
                browser: this.browser,
                chunkSizeLimit: args.chunkSizeLimit || 1000000
            });




        this.getHeader()
            .then( function( header ) {
                    thisB._deferred.features.resolve({success:true});
                    thisB._estimateGlobalStats()
                        .then(
                            function( stats ) {
                                thisB.globalStats = stats;
                                thisB._deferred.stats.resolve( stats );
                            },
                            lang.hitch( thisB, '_failAllDeferred' )
                        );
                },
                lang.hitch( thisB, '_failAllDeferred' )
            );
    },

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
            var refSeqName = query.ref || thisB.refSeq.name ;
            var min, max ;
            if(ProjectionUtils.isSequenceList(refSeqName)){
                // var sequenceListObject = ProjectionUtils.parseSequenceList(refSeqName);
                var unprojectedArray = ProjectionUtils.unProjectCoordinates(refSeqName,query.start,query.end);
                min = unprojectedArray[0];
                max = unprojectedArray[1];
            }
            else{
                min = query.start ;
                max = query.end ;
                // chrName = query.ref ;
            }

            console.log(min + ' ' + max);

            thisB.indexedData.getLines(
                refSeqName,
                min,
                max,
                function( line ) {
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
        if(ProjectionUtils.isSequenceList(refSeqName)){
            var sequenceListObject = ProjectionUtils.parseSequenceList(refSeqName);
            if(sequenceListObject.reverse){
                strand = -1 * strand ;
            }
            var unprojectedArray = ProjectionUtils.unProjectCoordinates(refSeqName,line.start,line.end);
            min = unprojectedArray[0];
            max = unprojectedArray[1];
        }
        else{
            min = line.start;
            max = line.end ;
            // chrName = query.ref ;
        }

        var featureData = {
            start:  min,
            end:    max,
            strand: strand,
            child_features: [],
            seq_id: line.ref,
            attributes: attributes,
            type:   type,
            source: source,
            remove_id: remove_id
        };

        return featureData;
    }



});
});
