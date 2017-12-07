define([
           'dojo/_base/declare',
           'dojo/_base/lang',
           'dojo/Deferred',
           'WebApollo/ProjectionUtils',
           'JBrowse/Store/SeqFeature/VCFTabix'
       ],
       function(
           declare,
           lang,
           Deferred,
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
    }

});
});
