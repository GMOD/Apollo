
/*  Subclass of FeatureTrack that allows features to be dragged and dropped into the annotation track to create annotations. */
function DraggableFeatureTrack(trackMeta, url, refSeq, browserParams) {
    FeatureTrack.call(this, trackMeta, url, refSeq, browserParams);
//    thisObj = this;
    var thisObj = this;
    this.mouseOverFeat = function(event)  {thisObj.featMouseOver(event);}
    this.mouseDownFeat = function(event)  {thisObj.featMouseDown(event);}
    this.mouseUpFeat = function(event)  {thisObj.featMouseUp(event);}
    this.mouseDoubleClickFeat = function(event) {thisObj.featDoubleClick(event); }
//
}


// Inherit from FeatureTrack
DraggableFeatureTrack.prototype = new FeatureTrack();

// selected feature array is class variable (shared across all DraggableFeatureTrack objects)
DraggableFeatureTrack.sel_features = []; 
DraggableFeatureTrack.sel_divs = []; 
DraggableFeatureTrack.dragging = false;

DraggableFeatureTrack.setDragging = function(onOrOff) {
//        console.log("setDragging(" + onOrOff + ")");
    DraggableFeatureTrack.dragging = onOrOff;
}

DraggableFeatureTrack.isDragging = function() {
//        console.log("isDragging: " + this.dragging);
    return DraggableFeatureTrack.dragging;
}

/** class method */
DraggableFeatureTrack.addToSelection = function(featdiv) {  
    // Check if it's already in the selected array before adding it
    if (DraggableFeatureTrack.sel_divs.indexOf(featdiv) == -1)  {
	if (featdiv.feature || featdiv.subfeature)  {
	    console.log("addToSelection ");
            console.log(featdiv);
            DraggableFeatureTrack.sel_divs.push(featdiv);
	    if (featdiv.feature)  {
	    DraggableFeatureTrack.sel_features.push(featdiv.feature);
	    console.log("add feature to selection");
	    console.log(featdiv.feature);
	    }
	    else if (featdiv.subfeature)  {
		DraggableFeatureTrack.sel_features.push(featdiv.subfeature);
		console.log("add subfeature to selection");
		console.log(featdiv.subfeature);
	    }
	    dojo.addClass(featdiv, "selected-feature");
	}
	else {
	    console.log("no feature or subfeature associated with div: ");
	    console.log(featdiv);
	}
    }

}

/** class method */
DraggableFeatureTrack.clearSelection = function() {
    var idx;
    for (idx in DraggableFeatureTrack.sel_divs)  {
	dojo.removeClass(DraggableFeatureTrack.sel_divs[idx], "selected-feature");
    }
    DraggableFeatureTrack.sel_divs = [];
    DraggableFeatureTrack.sel_features = [];
}

/** class method */
DraggableFeatureTrack.removeFromSelection = function(featdiv) {
    // index of feat in sel_features will be same as index of featdiv in sel_divs
    var idx = DraggableFeatureTrack.sel_divs.indexOf(featdiv);
    if (idx > -1)  {
        DraggableFeatureTrack.sel_divs.splice(idx, 1);
	DraggableFeatureTrack.sel_features.splice(idx, 1);
	dojo.removeClass(featdiv, "selected-feature");
    }
    console.log("removeFromSelection " + featdiv);
}

/** class method */
// Returns raw array -- manipulation will change selection...
DraggableFeatureTrack.getSelectedFeatures = function() {
    return DraggableFeatureTrack.sel_features;
}

DraggableFeatureTrack.getSelectedDivs = function()  {
    return DraggableFeatureTrack.sel_divs;
}


/**
 *  overriding renderFeature to add event handling for mouseover, mousedown, mouseup
 */
DraggableFeatureTrack.prototype.renderFeature = function(feature, uniqueId, block, scale,
                                                containerStart, containerEnd) {
    var featDiv = FeatureTrack.prototype.renderFeature.call(this, feature, uniqueId, block, scale,
                                                            containerStart, containerEnd);
    if (featDiv)  {  // just in case featDiv doesn't actually get created
	featDiv.onmouseover = this.mouseOverFeat;
	featDiv.onmousedown = this.mouseDownFeat;
	featDiv.onmouseup = this.mouseUpFeat;
	featDiv.ondblclick = this.mouseDoubleClickFeat;
    }
    return featDiv;
}

DraggableFeatureTrack.prototype.renderSubfeature = function(feature, featDiv, subfeature,
							    displayStart, displayEnd) {
    var subFeatDiv = FeatureTrack.prototype.renderSubfeature.call(this, feature, featDiv, subfeature, 
								  displayStart, displayEnd);
    if (subFeatDiv)  {  // just in case subFeatDiv doesn't actually get created
	subFeatDiv.onmouseover = this.mouseOverFeat;
	subFeatDiv.onmousedown = this.mouseDownFeat;
	subFeatDiv.onmouseup = this.mouseUpFeat;
	subFeatDiv.ondblclick = this.mouseDoubleClickFeat;
    }
    return subFeatDiv;
}

/* 
 *  selection occurs on mouse down
 *  mouse-down on unselected feature -- deselect all & select feature
 *  mouse-down on selected feature -- no change to selection (but may start drag?)
 *  mouse-down on "empty" area -- deselect all 
 *        (WARNING: this is preferred behavior, but conflicts with dblclick for zoom -- zoom would also deselect)  
 *         therefore have mouse-click on empty area deselect all (no conflict with dblclick)
 *  shift-mouse-down on unselected feature -- add feature to selection
 *  shift-mouse-down on selected feature -- remove feature from selection
 *  shift-mouse-down on "empty" area -- no change to selection
  */
DraggableFeatureTrack.prototype.featMouseDown = function(event) {
    event = event || window.event;
    var elem = (event.currentTarget || event.srcElement);
    // if (!elem.feature) elem = elem.parentElement;
//    var featdiv = this.getTopLevelFeatureDiv(elem);
    var featdiv = this.getLowestFeatureDiv(elem);
    var feat = featdiv.feature;
    if (feat)  { feat.isSubFeature = false; }
    else  { feat = featdiv.subfeature; feat.isSubFeature = true; }
    var already_selected = (DraggableFeatureTrack.getSelectedDivs().indexOf(featdiv) > -1);
    if (event.shiftKey)  {
	if (already_selected) {
	    DraggableFeatureTrack.removeFromSelection(featdiv);
	}
	else  {
            DraggableFeatureTrack.addToSelection(featdiv);
	}
    }
    else  {
	if (!already_selected)  {
	    DraggableFeatureTrack.clearSelection();
	    DraggableFeatureTrack.addToSelection(featdiv);
	}
    }
    event.stopPropagation();
    // This only works the SECOND time you try to drag a feature.
    // this.makeDraggableAndDroppable(elem);
    console.log("DFT.featMouseDown: started dragging " + feat); // DEL
    DraggableFeatureTrack.setDragging(true);
}

DraggableFeatureTrack.prototype.featDoubleClick = function(event)  {
    // prevent event bubbling up to genome view and triggering zoom
    event.stopPropagation();

    event = event || window.event;
    var elem = (event.target || event.srcElement);
    var featdiv = this.getLowestFeatureDiv(elem);
    // only take action on double-click for subfeatures 
    //  (but stop propagation for both features and subfeatures)
    // GAH TODO:  make this work for feature hierarchies > 2 levels deep
    if (featdiv.subfeature)  {   
	var already_selected = (DraggableFeatureTrack.getSelectedDivs().indexOf(featdiv) > -1);
	// if subfeature already selected, deselect 
	if (already_selected)  { 
	    DraggableFeatureTrack.removeFromSelection(featdiv);
	}
	// select parent feature
	var parent_feat_div = this.getParentFeatureDiv(featdiv);
	if (parent_feat_div !== null)  {
	    DraggableFeatureTrack.addToSelection(parent_feat_div);
	    // deselect all children of parent feature??  
	    // regardless of whether shift-modifier or not???
	}
    }

}

/*  GAH should switch to using dojo.connect or dojo.fixEvent for normalized events, so can remove IE specific code */
DraggableFeatureTrack.prototype.featMouseOver = function(event) {
    if (DraggableFeatureTrack.isDragging()) {
//        console.log("DFT.featMouseOver: already dragging"); // DEL
        return;
    }
    event = event || window.event;
    var elem = (event.target || event.srcElement);
    // var featdiv = this.getTopLevelFeatureDiv(elem);
    var featdiv = this.getLowestFeatureDiv(elem);
    if (featdiv === null) return; //shouldn't happen; just bail if it does
//    console.log("DFT.featMouseOver on draggablefeature " + featdiv.feature + ", elem.className = " + featdiv.className + ", event = " + event);  
    // Make this feature draggable & droppable
    this.makeDraggableAndDroppable(featdiv);
}

DraggableFeatureTrack.prototype.featMouseUp = function(event) {
     DraggableFeatureTrack.setDragging(false); 
//    console.log("DFT.onMouseup: stopped dragging");  // DEL
}




/**
 *  feature click no-op (to override FeatureTrack.onFeatureClick, which conflicts with mouse-down selection
 */
DraggableFeatureTrack.prototype.onFeatureClick = function(event) {
    // do nothing
    //   event.stopPropagation();
}

/** 
 * get highest level feature in feature hierarchy 
 * should be able to handle current two-level feature/subfeature hierarchy 
 *     (including non-feature descendants of feature div, such as arrowhead div)
 *   but also anticipating shift to multi-level feature hierarchy
 *   and/or feature/subfeature elements that have non-feature div descendants, possibly nested
 * elem should be a div for a feature or subfeature, or descendant div of feature or subfeature
*/
DraggableFeatureTrack.prototype.getTopLevelFeatureDiv = function(elem)  {
    while (!elem.feature)  {
	elem = elem.parentNode;
	if (elem === document)  {return null;} 
    }
    // found a feature, now crawl up hierarchy till top feature (feature with no parent feature)
    while (elem.parentNode.feature)  {
	elem = elem.parentNode;
    }
    return elem;
}

/** returns parent feature div of subfeature div */
DraggableFeatureTrack.prototype.getParentFeatureDiv = function(elem)  {
    elem = elem.parentNode;
    return DraggableFeatureTrack.prototype.getLowestFeatureDiv(elem);
}

/** returns first feature or subfeature div (including itself) found when crawling towards root from branch in feature/subfeature/descendants div hierachy  */
DraggableFeatureTrack.prototype.getLowestFeatureDiv = function(elem)  {
    while (!elem.feature && !elem.subfeature)  {
	elem = elem.parentNode;
	if (elem === document)  {return null;} 
    }
    return elem;
}

// Make this DraggableFeatureTrack draggable
DraggableFeatureTrack.prototype.makeDraggableAndDroppable = function(elem) {
    // Check whether we've already done it--look at class name and see if it includes "draggable-feature"
    if (dojo.hasClass(elem, "draggable-feature")) {
	//        console.log("Already has draggable-feature class: " + elem.feature + ", className = " + elem.className );
        return;
    }

    dojo.addClass(elem, "draggable-feature");
//    console.log("makeDraggable: feature = " + elem.feature + ", className = " + elem.className);
    $(".draggable-feature").draggable({
	    helper:'clone',
            zindex: 200,
            opacity: 0.3,  // make the object semi-transparent when dragged
            axis: 'y'      // Allow only vertical dragging
    });
    // If a selected feature is being dragged, we'll handle the drag here--don't want the drag event to go to the whole-canvas-drag handler.
    $(".draggable-feature").bind("mousedown", function(evt) {
//        console.log("makeDraggable: evt.stopPropagation.  elem.feature = " + elem.feature + ", currentSelection = " + DraggableFeatureTrack.prototype.getCurrentSelection());  // DEL
	evt.stopPropagation();
    });
      
    var fields = this.fields;
    var subFields = this.subFields;
    // Note that this relies on the annotation track's name being "Annotations".  Need to make this more general.
    $("#track_Annotations").droppable({
       drop: function(ev, ui) {
	   // within drop(), "this" is trackDiv that feature div is dropped on
//        console.log("makeDroppable: stopped dragging");  // DEL
        DraggableFeatureTrack.setDragging(false);
    	var track = this.track;  
    	var features = track.features;
        // This creates a new annotation for each currently selected feature (not a multi-exon feature comprised of the selected features, as we'd like).
        // Also, the drag-ghost is only of the most recently selected feature--it's not capable of showing the drag-ghosts of all selected features.
           var selected = DraggableFeatureTrack.getSelectedFeatures();
           for (i in selected)  {
            var feat = selected[i];
            console.log("Creating new annotation for feature " + i + ": " + feat); // DEL
            var responseFeature;
	    // creating JSON feature data struct that WebApollo server understands, based on JSON feature data struct that JBrowse understands
            var topLevelFeature = JSONUtils.createJsonFeature(feat[fields["start"]], feat[fields["end"]], feat[fields["strand"]], "SO", "gene");
	    //console.log("createJsonFeature: " + topLevelFeature);
	    console.log("createJsonFeature: ");
	    console.log(topLevelFeature);
	    var testFeature = JSONUtils.createApolloFeature(feat, fields, subFields, "transcript");
	    console.log("createApolloFeature: ");
	    console.log(testFeature);
	    console.log(fields);
	
                dojo.xhrPost( {
                    // "http://10.0.1.24:8080/ApolloWeb/Login?username=foo&password=bar" to login
                    // postData: '{ "track": "' + track.name + '", "features": [ ' + JSON.stringify(topLevelFeature) + '], "operation": "add_feature" }',
                    // postData: '{ "track": "' + track.name + '", "features": [ ' + JSON.stringify(testFeature) + '], "operation": "add_transcript" }',
                    postData: '{ "track": "' + track.name + '", "features": [ ' + JSON.stringify(testFeature) + '], "operation": "add_feature" }',
                	url: "/ApolloWeb/AnnotationEditorService",
                	handleAs: "json",
                	timeout: 5000, // Time in milliseconds
                	// The LOAD function will be called on a successful response.
                	load: function(response, ioArgs) { //
                	console.log("Successfully created annotation object: " + response)
                	// response processing is now handled by the long poll thread (when using servlet 3.0)
                	// uncomment code to get it working with servlet 2.5
			    //  if comet-style long pollling is not working, then create annotations based on 
			    //     AnnotationEditorResponse
			    if (!track.comet_working)  {
               			// responseFeatures = responseFeatures.features;
                		responseFeatures = response.features;
				/*
				for (j in responseFeatures)  {
				    var newfeat = JSONUtils.createJBrowseFeature(responseFeatures[i], fields);
				    features.add(newfeat, newfeat.id);
				} 
				*/ 
				var featureArray = JSONUtils.convertJsonToFeatureArray(responseFeatures[0]);
				features.add(featureArray, responseFeatures[0].uniquename);

                		track.hideAll();
                		track.changed();
              			console.log("DFT: responseFeatures[0].uniquename = " + responseFeatures[0].uniquename);
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
//	   DraggableFeatureTrack.clearSelection();
	   
//      console.log("itemDragged: " + newAnnot); //  + ", pos.left = " + pos.left + ", pos.top = " + pos.top + ", width = " + ui.draggable.width());
    }
    });
}

/*
Copyright (c) 2010-2011 Berkeley Bioinformatics Open-source Projects & Lawrence Berkeley National Labs

This package and its accompanying libraries are free software; you can
redistribute it and/or modify it under the terms of the LGPL (either
version 2.1, or at your option, any later version) or the Artistic
License 2.0.  Refer to LICENSE for the full license text.

*/
