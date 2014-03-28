define([
	"dojo/_base/declare",
	'JBrowse/View/TrackList/Faceted'
],
function(declare,Faceted) {
	return declare('WebApollo.View.TrackList.Faceted',Faceted,
	{
		// Subclass method for track selector to remove webapollo specific tracks
		constructor: function() {
            console.log('WebApollo/View/TrackList/Faceted');
			this.inherited(arguments);
			shift(this.config.trackConfigs);//Remove sequence track
			shift(this.config.trackConfigs);//Remove user-created annotations
		}
	});
});

