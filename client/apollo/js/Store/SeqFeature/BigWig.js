define( [
            'dojo/_base/declare',
            'dojo/_base/lang',
            'dojo/_base/array',
            'dojo/_base/url',
            'JBrowse/Util',
            'JBrowse/Model/XHRBlob',
            'WebApollo/JSONUtils',
            'WebApollo/ProjectionUtils',
            'JBrowse/Store/SeqFeature/BigWig'
        ],
        function(
            declare,
            lang,
            array,
            urlObj,
            Util,
            XHRBlob,
            JSONUtils,
            ProjectionUtils,
            BigWigStore
        ) {
return declare( BigWigStore,

 /**
  * @lends JBrowse.Store.BigWig
  */
{


    _getFeatures: function( query, featureCallback, endCallback, errorCallback ) {

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

        var v = query.basesPerSpan ? this.getView( 1/query.basesPerSpan ) :
                       query.scale ? this.getView( query.scale )          :
                                     this.getView( 1 );

        if( !v ) {
            endCallback();
            return;
        }

        chrName = this.browser.regularizeReferenceName(chrName);
        v.readWigData( chrName, min, max, dojo.hitch( this, function( features ) {
            array.forEach( features || [], featureCallback );
            endCallback();
        }), errorCallback );
    }


});

});
