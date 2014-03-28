define([
	"dojo/_base/declare",
	'JBrowse/View/TrackList/Faceted'
],
function(declare,Faceted) {
	return declare('WebApollo.View.TrackList.Faceted',Faceted,
	{
		// Subclass method for track selector to remove webapollo specific tracks
		constructor: function(args) {
            //Remove the tracks from the metadata descriptions
            for(var index in args.trackMetaData.identIndex) {
                if(args.trackMetaData.identIndex[index]["track type"]=="WebApollo/View/Track/AnnotTrack" || 
                   args.trackMetaData.identIndex[index]["track type"]=="WebApollo/View/Track/AnnotSequenceTrack") {
                    delete args.trackMetaData.identIndex[index];
                    args.trackMetaData.facetIndexes.itemCount--;
                }   
            }
		}
	});
});

