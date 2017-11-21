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

    /**
     * Data backend for reading wiggle data from BigWig or BigBed files.
     *
     * Adapted by Robert Buels from bigwig.js in the Dalliance Genome
     * Explorer which is copyright Thomas Down 2006-2010
     * @constructs
     */
    constructor: function( args ) {

        this.data = args.blob ||
            new XHRBlob( this.resolveUrl(
                             args.urlTemplate || 'data.bigwig'
                         )
                       );

        this.name = args.name || ( this.data.url && new urlObj( this.data.url ).path.replace(/^.+\//,'') ) || 'anonymous';

        this.storeTimeout = 3000;


        this._load();
    },




    _getFeatures: function( query, featureCallback, endCallback, errorCallback ) {

        // parse sequenceList from query.ref
        var sequenceListObject = ProjectionUtils.parseSequenceList(query.ref);
        console.log(sequenceListObject)
        // unproject start and end
        var featureLocationArray = ProjectionUtils.unProjectCoordinates(query.ref, query.start, query.end);
        // rebuild the query
        console.log(featureLocationArray)
        var chrName = sequenceListObject[0].name;
        var min = featureLocationArray[0];
        var max = featureLocationArray[1];



        var v = query.basesPerSpan ? this.getView( 1/query.basesPerSpan ) :
                       query.scale ? this.getView( query.scale )          :
                                     this.getView( 1 );

        if( !v ) {
            endCallback();
            return;
        }

        v.readWigData( chrName, min, max, dojo.hitch( this, function( features ) {
            array.forEach( features || [], featureCallback );
            endCallback();
        }), errorCallback );
    }


});

});
