define([
    "dojo/_base/declare",
    'JBrowse/View/TrackList/Faceted'
],
function(declare,Faceted) {
    return declare('WebApollo.View.TrackList.Faceted',Faceted,
    {
        // Subclass method for track selector to remove webapollo specific tracks
        renderGrid:function() {
            //Remove the tracks from the metadata descriptions
            for(var index in this.trackDataStore.identIndex) {
                if(this.trackDataStore.identIndex[index]["track type"]=="WebApollo/View/Track/AnnotTrack" || 
                   this.trackDataStore.identIndex[index]["track type"]=="WebApollo/View/Track/AnnotSequenceTrack") {
                    delete this.trackDataStore.identIndex[index];
                    this.trackDataStore.facetIndexes.itemCount--;
                }
            }
            return this.inherited(arguments);
        }
    });
});

