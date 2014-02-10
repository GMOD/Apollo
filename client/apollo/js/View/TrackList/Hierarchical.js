define([
	"dojo/_base/declare",
	'JBrowse/View/TrackList/Hierarchical'
],
function(declare,Hierarchical) {
	return declare('WebApollo.View.TrackList.Hierarchical',Hierarchical,
	{
		// Subclass method for track selector to remove webapollo specific tracks
		addTracks: function( tracks, inStartup ) {
			for(i=0;i<tracks.length;i++) {
				if(tracks[i].label=="Annotations" || tracks[i].label=="DNA") {
					tracks.splice(i,1);
					i=0;//restart for safety
				}
			}
			//call superclass
			this.inherited(arguments);
		}
	});
});

