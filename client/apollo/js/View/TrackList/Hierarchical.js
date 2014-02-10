define([
	"dojo/_base/declare",
	'JBrowse/View/TrackList/Hierarchical'
],
function(declare,Hierarchical) {
	return declare('WebApollo.View.TrackList.Hierarchical',Hierarchical,
	{
		addTracks: function( tracks, inStartup ) {
			console.log(tracks);
			alert('Hello World');
			this.inherited(arguments);
		}
	});
});

