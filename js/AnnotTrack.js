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
/*
    this.subfeatureCallback = function(i, val, param) {
        thisObj.renderSubfeature(param.feature, param.featDiv, val);
    };
*/
    // define fields meta data
    this.fields = {"start": 0, "end": 1, "strand": 2, "name": 3};
    this.comet_working = true;
    this.remote_edit_working = true;

    this.mouseDownAnnot = function(event)  {thisObj.annotMouseDown(event);}
}

// Inherit from FeatureTrack
AnnotTrack.prototype = new FeatureTrack();

AnnotTrack.creation_count = 0;
AnnotTrack.currentAnnot = null;
AnnotTrack.USE_LOCAL_EDITS = true;


dojo.require("dijit.Menu");
dojo.require("dijit.MenuItem");
var annot_context_menu;



dojo.addOnLoad( function()  {
    annot_context_menu = new dijit.Menu({});
    annot_context_menu.addChild(new dijit.MenuItem( 
	{ label: "Delete" 
	   // , onclick: AnnotTrack.deleteCurrentAnnotation()
	}
    ));
    annot_context_menu.addChild(new dijit.MenuItem( 
	{ label: "..." }
    ));
    annot_context_menu.startup();
} );

console.log("annot context menu created...");

AnnotTrack.prototype.loadSuccess = function(trackInfo) {
    FeatureTrack.prototype.loadSuccess.call(this, trackInfo);
	
    var track = this;
    var features = this.features;
    
	dojo.xhrPost( {
	    	postData: '{ "track": "' + track.name + '", "operation": "get_features" }',
	    	url: "/ApolloWeb/AnnotationEditorService",
	    	handleAs: "json",
	    	timeout: 5 * 1000, // Time in milliseconds
	    	// The LOAD function will be called on a successful response.
	    	load: function(response, ioArgs) { //
	    	var responseFeatures = response.features;
	    	for (var i = 0; i < responseFeatures.length; i++) {
	    		var featureArray = JSONUtils.convertJsonToFeatureArray(responseFeatures[i]);
	    		features.add(featureArray, responseFeatures[0].uniquename);
	    		// console.log("responseFeatures[0].uniquename: " + responseFeatures[0].uniquename);
	    	}
    		track.hideAll();
    		track.changed();
	    },
	    // The ERROR function will be called in an error case.
	    error: function(response, ioArgs) { // 
	    	console.log("Annotation server error--maybe you forgot to login to the server?")
	    	console.error("HTTP status code: ", ioArgs.xhr.status); //
	    	//dojo.byId("replace").innerHTML = 'Loading the resource from the server did not work'; // 
		track.remote_edit_working = false;
	    	return response; // 
	    }
	});
	
	this.createAnnotationChangeListener();

}

AnnotTrack.prototype.createAnnotationChangeListener = function() {
    var track = this;
    var features = this.features;

	dojo.xhrGet( {
    	url: "/ApolloWeb/AnnotationChangeNotificationService",
    	content: { track: track.name },
    	handleAs: "json",
    	timeout: 1000 * 1000, // Time in milliseconds
    	// The LOAD function will be called on a successful response.
    	load: function(response, ioArgs) {
    		if (response.operation == "ADD") {
    			var responseFeatures = response.features;
    			var featureArray = JSONUtils.convertJsonToFeatureArray(responseFeatures[0]);
    			var id = responseFeatures[0].uniquename;
    			if (features.featIdMap[id] == null) {
			    // note that proper handling of subfeatures requires annotation trackData.json resource to 
			    //    set sublistIndex one past last feature array index used by other fields 
			    //    (currently Annotations always have 6 fields (0-5), so sublistIndex = 6
    				features.add(featureArray, id);
    				track.hideAll();
    				track.changed();
    			}
    		}
    		track.createAnnotationChangeListener();
    	},
	    // The ERROR function will be called in an error case.
	    error: function(response, ioArgs) { // 
	    	console.error("HTTP status code: ", ioArgs.xhr.status); //
		track.comet_working = false;
	    	return response;
	    }
	});

}

/**
 *  overriding renderFeature to add event handling right-click context menu
 */
AnnotTrack.prototype.renderFeature = function(feature, uniqueId, block, scale,
                                                containerStart, containerEnd) {
    var featDiv = FeatureTrack.prototype.renderFeature.call(this, feature, uniqueId, block, scale,
                                                            containerStart, containerEnd);
    annot_context_menu.bindDomNode(featDiv);
    // console.log("added context menu to featdiv: ", uniqueId);
    return featDiv;
}

/** AnnotTrack subfeatures are similar to DAS subfeatures, so handled similarly */
AnnotTrack.prototype.handleSubFeatures = function(feature, featDiv, 
						     displayStart, displayEnd)  {
    var subfeatures = this.fields["subfeatures"];
    for (var i = 0; i < feature[subfeatures].length; i++) {
	var subfeature = feature[subfeatures][i];
	this.renderSubfeature(feature, featDiv, subfeature, displayStart, displayEnd);
    }
}

AnnotTrack.prototype.renderSubfeature = function(feature, featDiv, subfeature,
                                                   displayStart, displayEnd) {
    var subdiv = FeatureTrack.prototype.renderSubfeature.call(this, feature, featDiv, subfeature, 
							      displayStart, displayEnd);
    subdiv.onmousedown = this.annotMouseDown;
}

AnnotTrack.prototype.annotMouseDown = function(event)  {
    event = event || window.event;
    var elem = (event.currentTarget || event.srcElement);
    var featdiv = DraggableFeatureTrack.prototype.getLowestFeatureDiv(elem);
    if (featdiv && (featdiv != null))  {
	if (dojo.hasClass(featdiv, "ui-resizable"))  {
	    console.log("already resizable");
	    console.log(featdiv);
	}
	else {
	    console.log("making annotation resizable");
	    console.log(featdiv);
	    $(featdiv).resizable( { handles: "e, w", 
				    helper: "ui-resizable-helper" } );
//				    opacity: 0.5 } );
				    // ghost: true } );
	    
	}
    }
    event.stopPropagation();
}

/**
 *  feature click no-op (to override FeatureTrack.onFeatureClick, which conflicts with mouse-down selection
 */
AnnotTrack.prototype.onFeatureClick = function(event) {
    console.log("in AnnotTrack.onFeatureClick");
    event = event || window.event;
    var elem = (event.currentTarget || event.srcElement);
    var featdiv = DraggableFeatureTrack.prototype.getLowestFeatureDiv(elem);
    if (featdiv && (featdiv != null))  {
	console.log(featdiv);
    }

    // do nothing
    //   event.stopPropagation();

}

/*
Copyright (c) 2010-2011 Berkeley Bioinformatics Open Projects (BBOP)

This package and its accompanying libraries are free software; you can
redistribute it and/or modify it under the terms of the LGPL (either
version 2.1, or at your option, any later version) or the Artistic
License 2.0.  Refer to LICENSE for the full license text.

*/
