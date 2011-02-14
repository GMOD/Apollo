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
    this.fields = AnnotTrack.fields;
    this.comet_working = true;
    this.remote_edit_working = false;

    this.mouseDownAnnot = function(event)  {
	thisObj.annotMouseDown(event);
    }
}

// Inherit from FeatureTrack
AnnotTrack.prototype = new FeatureTrack();

/**
*  only set USE_COMET true if server supports Servlet 3.0 comet-style long-polling, and web app is propertly set up for async
*    otherwise if USE_COMET is set to true, will cause server-breaking errors
*  
*/
AnnotTrack.USE_COMET = false;

/**
*  set USE_LOCAL_EDITS = true to bypass editing calls to AnnotationEditorService servlet and attempt 
*    to create similar annotations locally
*  useful when AnnotationEditorService is having problems, or experimenting with something not yet completely implemented server-side
*/
AnnotTrack.USE_LOCAL_EDITS = false;

AnnotTrack.creation_count = 0;
AnnotTrack.selectedFeatures = [];

AnnotTrack.fields = {"start": 0, "end": 1, "strand": 2, "name": 3};

dojo.require("dijit.Menu");
dojo.require("dijit.MenuItem");
var annot_context_menu;
var context_path = "/ApolloWeb";
// var context_path = "";


dojo.addOnLoad( function()  {
    annot_context_menu = new dijit.Menu({});
    annot_context_menu.addChild(new dijit.MenuItem(
    {
    	label: "Delete",
    	onClick: function() {
    		AnnotTrack.deleteSelectedFeatures();
        }
    }
    ));
    annot_context_menu.addChild(new dijit.MenuItem( 
    {
    	label: "..."
    }
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
	url: context_path + "/AnnotationEditorService",
	handleAs: "json",
	timeout: 5 * 1000, // Time in milliseconds
	// The LOAD function will be called on a successful response.
	load: function(response, ioArgs) { //
	    var responseFeatures = response.features;
	    for (var i = 0; i < responseFeatures.length; i++) {
		var jfeat = JSONUtils.createJBrowseFeature(responseFeatures[i], track.fields, track.subFields);
		features.add(jfeat, responseFeatures[i].uniquename);
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
	
    if (AnnotTrack.USE_COMET)  {
	this.createAnnotationChangeListener();
    }
    this.makeTrackDroppable();
}

AnnotTrack.prototype.createAnnotationChangeListener = function() {
    var track = this;
    var features = this.features;

    dojo.xhrGet( {
	url: context_path + "/AnnotationChangeNotificationService",
	content: {
	    track: track.name
	},
	handleAs: "json",
	timeout: 1000 * 1000, // Time in milliseconds
	// The LOAD function will be called on a successful response.
	load: function(response, ioArgs) {
	    if (response.operation == "ADD") {
	    	var responseFeatures = response.features;
//	    	var featureArray = JSONUtils.convertJsonToFeatureArray(responseFeatures[0]);
	    	var featureArray = JSONUtils.createJBrowseFeature(responseFeatures[0], track.fields, track.subFields);

	    	var id = responseFeatures[0].uniquename;
	    	if (features.featIdMap[id] == null) {
	    		// note that proper handling of subfeatures requires annotation trackData.json resource to
	    		//    set sublistIndex one past last feature array index used by other fields
	    		//    (currently Annotations always have 6 fields (0-5), so sublistIndex = 6
	    		features.add(featureArray, id);
	    	}
	    }
		else if (response.operation == "DELETE") {

			var responseFeatures = response.features;
                        for (var i = 0; i < responseFeatures.length; ++i) {
                              var id_to_delete = responseFeatures[i].uniquename;
                              features.delete(id_to_delete);
			}
		}
		track.hideAll();
		track.changed();
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

AnnotTrack.annot_under_mouse = null;

/**
 *  overriding renderFeature to add event handling right-click context menu
 */
AnnotTrack.prototype.renderFeature = function(feature, uniqueId, block, scale,
    containerStart, containerEnd) {
	var track = this;
    var featDiv = FeatureTrack.prototype.renderFeature.call(this, feature, uniqueId, block, scale,
	containerStart, containerEnd);
	console.log("rendered feature: ");
	console.log(feature);
	console.log(featDiv);
    if (featDiv && featDiv != null)  {
      annot_context_menu.bindDomNode(featDiv);
      //    var track = this;
      $(featDiv).bind("mouseenter", function(event)  {
	  /* "this" in mousenter function will be featdiv */
	  AnnotTrack.annot_under_mouse = this;
	  console.log("annot under mouse: ");
	  console.log(AnnotTrack.annot_under_mouse);
	} );
      $(featDiv).bind("mouseleave", function(event)  {
	  console.log("no annot under mouse: ");
	  AnnotTrack.annot_under_mouse = null;
	} );
      // console.log("added context menu to featdiv: ", uniqueId);
    dojo.connect(featDiv, "oncontextmenu", this, function(e) {
    	if (AnnotTrack.selectedFeatures.length == 1) {
    		AnnotTrack.selectedFeatures = [];
    	}
    	AnnotTrack.selectedFeatures.push([feature, track.name]);
    });
    // console.log("added context menu to featdiv: ", uniqueId);
    $(featDiv).droppable(  {
	accept: ".selected-feature",   // only accept draggables that are selected feature divs	
	    tolerance: "pointer", 
	    hoverClass: "annot-drop-hover", 
	    drop: function(event, ui)  {
	    console.log("dropped feature on annot:");
	    console.log(this);
	  }
	
	} );
    }
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
    if (subdiv && subdiv != null)  {
      subdiv.onmousedown = this.annotMouseDown;
    }
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
	    $(featdiv).resizable( {
		handles: "e, w",
		helper: "ui-resizable-helper",
		autohide: false
	    } );
	    
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

AnnotTrack.prototype.addToAnnotation = function(annotdiv, newfeat)  {
    console.log("existing annotation");
    var existing_annot = annotdiv.feature;
    console.log(existing_annot);
    var existing_subs = existing_annot[this.fields["subfeatures"]];
    existing_subs.push(newfeat);
    // hardwiring start as f[0], end as f[1] for now -- 
    //   to fix this need to whether newfeat is a subfeat, etc.
    if (newfeat[0] < existing_annot[0])  { existing_annot[0] = newfeat[0]; }
    if (newfeat[1] > existing_annot[1])  { existing_annot[1] = newfeat[1]; }
    console.log("added to annotation: ");
    console.log(existing_annot);
    this.hideAll();
    this.changed();
}

AnnotTrack.prototype.makeTrackDroppable = function() {
    console.log("making track a droppable target: ");
    var target_track = this;
    var target_trackdiv = target_track.div;
    var features_nclist = target_track.features;
    console.log(this);
    $(target_trackdiv).droppable(  {
	accept: ".selected-feature",   // only accept draggables that are selected feature divs
	drop: function(event, ui)  {
	    // "this" is the div being dropped on, so same as target_trackdiv
	    console.log("draggable dropped on AnnotTrack");
	    console.log(ui);
	    // getSelectedFeatures() and getSelectedDivs() always return same size with corresponding  feat / div
	    //	    var feats = DraggableFeatureTrack.getSelectedFeatures();
	    var dragdivs = DraggableFeatureTrack.getSelectedDivs();1
	    for (var i in dragdivs)  {
		var dragdiv = dragdivs[i];
		var is_subfeature = dragdiv.subfeature;
		var dragfeat = dragdiv.feature || dragdiv.subfeature;
		console.log(dragfeat);
		var source_track = dragdiv.track;
		var newfeat = JSONUtils.convertToTrack(dragfeat, is_subfeature, source_track, target_track);
		console.log("local feat conversion: " )
		console.log(newfeat);
		if (AnnotTrack.annot_under_mouse != null)  {
		    console.log("adding to annot: ");
		    console.log(AnnotTrack.annot_under_mouse);
		    target_track.addToAnnotation(AnnotTrack.annot_under_mouse, newfeat);
		    console.log("finished adding to annot: ");
		}
		else  {
		if (AnnotTrack.USE_LOCAL_EDITS)  {
		    var id = "annot_" + AnnotTrack.creation_count++;
		    newfeat[target_track.fields["id"]] = id;
		    newfeat[target_track.fields["name"]] = id;
		    console.log("new feature: ");
		    console.log(newfeat);
		    features_nclist.add(newfeat, id);
		    target_track.hideAll();
		    target_track.changed();
		}
		else  {
		    var responseFeature;
		    var source_fields = source_track.fields;
		    var source_subFields = source_track.subFields;
		    var target_fields = target_track.fields;
		    var target_subFields = target_track.subFields;
		    // creating JSON feature data struct that WebApollo server understands, 
		    //    based on JSON feature data struct that JBrowse understands
		    /*
		    var topLevelFeature = JSONUtils.createJsonFeature(dragfeat[source_fields["start"]], 
								      dragfeat[source_fields["end"]], 
								      dragfeat[source_fields["strand"]], "SO", "gene");
		    console.log("createJsonFeature: ");
		    console.log(topLevelFeature);
		    */
		    var testFeature = JSONUtils.createApolloFeature(dragfeat, source_fields, source_subFields, "transcript");
		    console.log("createApolloFeature: ");
		    console.log(testFeature);
		    console.log(source_fields);
	
		    dojo.xhrPost( {
			postData: '{ "track": "' + target_track.name + '", "features": [ ' + JSON.stringify(testFeature) + '], "operation": "add_feature" }',
			url: context_path + "/AnnotationEditorService",
			handleAs: "json",
			timeout: 5000, // Time in milliseconds
			// The LOAD function will be called on a successful response.
			load: function(response, ioArgs) { //
			    console.log("Successfully created annotation object: " + response)
			    // response processing is now handled by the long poll thread (when using servlet 3.0)
			    //  if comet-style long pollling is not working, then create annotations based on 
			    //     AnnotationEditorResponse
			    if (!AnnotTrack.USE_COMET || !target_track.comet_working)  {
				// responseFeatures = responseFeatures.features;
				responseFeatures = response.features;
				/*
				for (var j in responseFeatures)  {
				    var newfeat = JSONUtils.createJBrowseFeature(responseFeatures[i], fields);
				    features_nclist.add(newfeat, newfeat.id);
				} 
				*/ 
//				var featureArray = JSONUtils.convertJsonToFeatureArray(responseFeatures[0]);
				var jfeat = JSONUtils.createJBrowseFeature(responseFeatures[0], target_fields, target_subFields);
//				features_nclist.add(featureArray, responseFeatures[0].uniquename);
				console.log("new JBrowse feature:");
				console.log(jfeat);
				features_nclist.add(jfeat, responseFeatures[0].uniquename);

				target_track.hideAll();
				target_track.changed();
			    // console.log("DFT: responseFeatures[0].uniquename = " + responseFeatures[0].uniquename);
			    }
			},
			// The ERROR function will be called in an error case.
			error: function(response, ioArgs) { //
			    console.log("Error creating annotation--maybe you forgot to log into the server?");
			    console.error("HTTP status code: ", ioArgs.xhr.status); //
			    //dojo.byId("replace").innerHTML = 'Loading the ressource from the server did not work'; //
			    return response;
			}
		    });
		}
		}
	    }

	}
    } );
}

AnnotTrack.deleteSelectedFeatures = function() {
	var trackName;
	var features = '"features": [';
	for (var i = 0; i < AnnotTrack.selectedFeatures.length; ++i) {
		var data = AnnotTrack.selectedFeatures[i];
		var feat = data[0];
		var uniqueName = feat[AnnotTrack.fields["name"]];
		if (trackName == null) {
			trackName = data[1];
		}
		if (i > 0) {
			features += ',';
		}
		features += ' { "uniquename": "' + uniqueName + '" } ';
	}
	features += ']';
	
	dojo.xhrPost( {
		postData: '{ "track": "' + trackName + '", ' + features + ', "operation": "delete_feature" }',
		url: context_path + "/AnnotationEditorService",
		handleAs: "json",
		timeout: 5000 * 1000, // Time in milliseconds
		load: function(response, ioArgs) {
			var responseFeatures = response.features;
		},
		// The ERROR function will be called in an error case.
		error: function(response, ioArgs) { // 
			console.log("Annotation server error--maybe you forgot to login to the server?")
			console.error("HTTP status code: ", ioArgs.xhr.status); //
			//dojo.byId("replace").innerHTML = 'Loading the resource from the server did not work'; //  
			return response; // 
		}
		
	});
	AnnotTrack.selectedFeatures = [];
}

/*
Copyright (c) 2010-2011 Berkeley Bioinformatics Open Projects (BBOP)

This package and its accompanying libraries are free software; you can
redistribute it and/or modify it under the terms of the LGPL (either
version 2.1, or at your option, any later version) or the Artistic
License 2.0.  Refer to LICENSE for the full license text.

*/
