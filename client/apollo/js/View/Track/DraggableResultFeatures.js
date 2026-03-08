define( [
         'dojo/_base/declare',
         'WebApollo/View/Track/DraggableHTMLFeatures',
         'WebApollo/JSONUtils'
         ],
         function( declare, DraggableFeatureTrack, JSONUtils ) {

var DraggableResultFeatures = declare( DraggableFeatureTrack, {   
    constructor: function(args)  {  },

    makeTrackMenu: function() {
        var track = this;
        this.inherited(arguments);
        var trackMenu = this.trackMenu;
        var annotTrack;
        for (var i = 0; i < this.genomeView.tracks.length; ++i) {
            if (this.genomeView.tracks[i].isWebApolloAnnotTrack) {
                annotTrack = this.genomeView.tracks[i];
                break;
            }
        }
        if (trackMenu && annotTrack) {
            var mitems = this.trackMenu.getChildren();
            for (var mindex=0; mindex < mitems.length; mindex++) {
                    if (mitems[mindex].type == "dijit/MenuSeparator")  { break; }
            }
            trackMenu.addChild(new dijit.MenuItem({
                label: "Promote all to annotations",
                iconClass: 'dijitIconEdit',
                onClick: function() {
                    if (confirm("Are you sure you want to promote all annotations?")) {
                        var featuresToAdd = new Array();
                        track.store.getFeatures({start: track.store.refSeq.start, end: track.store.refSeq.end}, function(feature) {
                            var afeat = JSONUtils.createApolloFeature(feature, "transcript");
                            featuresToAdd.push(afeat);
                        });
                        var postData = '{ "track": "' + annotTrack.getUniqueTrackName() + '", "features": ' + JSON.stringify(featuresToAdd) + ', "operation": "add_transcript" }';
                        annotTrack.executeUpdateOperation(postData);
                    }
                }
            }), mindex);
        }

    }
});

return DraggableResultFeatures;

});
