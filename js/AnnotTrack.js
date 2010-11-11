function AnnotTrack(trackMeta, url, refSeq, browserParams) {
    //trackMeta: object with:
    //            key:   display text track name
    //            label: internal track name (no spaces, odd characters)
    //url: URL of the track's JSON file
    //refSeq: object with:
    //         start: refseq start
    //         end:   refseq end
    //browserParams: object with:
    //                changeCallback: function to call once JSON is loaded
    //                trackPadding: distance in px between tracks
    //                baseUrl: base URL for the URL in trackMeta

  FeatureTrack.call(this, trackMeta, url, refSeq, browserParams);

    var thisObj = this;
    this.subfeatureCallback = function(i, val, param) {
        thisObj.renderSubfeature(param.feature, param.featDiv, val);
    };

    // define fields meta data
    this.fields = { "start": 0, "end": 1, "strand": 2, "name": 3 };
    
}

// Inherit from FeatureTrack
AnnotTrack.prototype = new FeatureTrack();
console.log("AnnotTrack created");  // DEL

AnnotTrack.prototype.setViewInfo = function(genomeView, numBlocks,
                                            trackDiv, labelDiv,
                                            widthPct, widthPx, scale) {
  Track.prototype.setViewInfo.apply(this, [genomeView, numBlocks,
                                           trackDiv, labelDiv,
                                           widthPct, widthPx, scale]);
  this.setLabel(this.key);
};

AnnotTrack.prototype.loadSuccess = function(trackInfo) {
	FeatureTrack.prototype.loadSuccess.call(this, trackInfo);
	
    var track = this;
    var features = this.features;
    
	dojo.xhrPost( {
	    	postData: '{ "operation": "get_features" }',
	    	url: "/ApolloWeb/AnnotationEditorService",
	    	handleAs: "text",
	    	timeout: 5000, // Time in milliseconds
	    	// The LOAD function will be called on a successful response.
	    	load: function(response, ioArgs) { //
	    	console.log("foolicious: " + response);
	    	var responseFeatures = eval('(' + response + ')').features;
	    	for (var i = 0; i <= responseFeatures.length; ++i) {
	    		var featureArray = JSONUtils.prototype.convertJsonToFeatureArray(responseFeatures[i]);
	    		features.add(featureArray, responseFeatures[0].uniquename);
	    		track.hideAll();
	    		track.changed();
	    		console.log("responseFeatures[0].uniquename: " + responseFeatures[0].uniquename);
	    	}
	    },
	    // The ERROR function will be called in an error case.
	    error: function(response, ioArgs) { // 
	    	console.log("Annotation server error--maybe you forgot to login to the server?")
	    	console.error("HTTP status code: ", ioArgs.xhr.status); //
	    	//dojo.byId("replace").innerHTML = 'Loading the resource from the server did not work'; //  
	    	return response; // 
	    }
	});

}

/*

Copyright (c) 2007-2010 The Evolutionary Software Foundation

Created by Mitchell Skinner <mitch_skinner@berkeley.edu>

This package and its accompanying libraries are free software; you can
redistribute it and/or modify it under the terms of the LGPL (either
version 2.1, or at your option, any later version) or the Artistic
License 2.0.  Refer to LICENSE for the full license text.

*/
