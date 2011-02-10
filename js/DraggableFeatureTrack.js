
/*  Subclass of FeatureTrack that allows features to be dragged and dropped into the annotation track to create annotations. */
function DraggableFeatureTrack(trackMeta, url, refSeq, browserParams) {
    FeatureTrack.call(this, trackMeta, url, refSeq, browserParams);
}

// Inherit from FeatureTrack
DraggableFeatureTrack.prototype = new FeatureTrack();

// selected feature array is class variable (shared across all DraggableFeatureTrack objects)
DraggableFeatureTrack.sel_features = []; 
DraggableFeatureTrack.sel_divs = []; 
DraggableFeatureTrack.dragging = false;

/** 
* class method 
* currently adds to selection info and sets style for selected div, 
*   but does not set draggability (this is done in featMouseDown
*  
*/
DraggableFeatureTrack.addToSelection = function(featdiv) {  
    if (featdiv.feature || featdiv.subfeature)  {
    // Check if it's already in the selected array before adding it
	if (DraggableFeatureTrack.sel_divs.indexOf(featdiv) == -1)  {
	    console.log("addToSelection ");
            console.log(featdiv);
            DraggableFeatureTrack.sel_divs.push(featdiv);
	    if (featdiv.feature)  {
		DraggableFeatureTrack.sel_features.push(featdiv.feature);
		//	    console.log("add feature to selection");
		//	    console.log(featdiv.feature);
	    }
	    else if (featdiv.subfeature)  {
		DraggableFeatureTrack.sel_features.push(featdiv.subfeature);
		// console.log("add subfeature to selection");
		// console.log(featdiv.subfeature);
	    }
	    $(featdiv).addClass("selected-feature");
	}
    }
    else {
	console.log("no feature or subfeature associated with div: ");
	console.log(featdiv);
    }
}

/** 
 * class method 
 *  in addition to clearing selection data, also removes draggability from the unselected divs
 */
DraggableFeatureTrack.clearSelection = function() {
//    $(DraggableFeatureTrack.sel_divs, ".ui-draggable").draggable("destroy");
    for (var idx in DraggableFeatureTrack.sel_divs)  {
	var featdiv = DraggableFeatureTrack.sel_divs[idx];
	$(featdiv).removeClass("selected-feature");
	if ($(featdiv).hasClass("ui-draggable"))  {
	    $(featdiv).draggable("destroy");
	}
    }
    DraggableFeatureTrack.sel_divs = [];
    DraggableFeatureTrack.sel_features = [];
}

/** 
 * class method 
 *  in addition to removing selection data, also removes draggability from the unselected div
*/
DraggableFeatureTrack.removeFromSelection = function(featdiv) {
    // index of feat in sel_features will be same as index of featdiv in sel_divs
    var idx = DraggableFeatureTrack.sel_divs.indexOf(featdiv);
    if (idx > -1)  {
        DraggableFeatureTrack.sel_divs.splice(idx, 1);
	DraggableFeatureTrack.sel_features.splice(idx, 1);
	$(featdiv).removeClass("selected-feature");
	if ($(featdiv).hasClass("ui-draggable"))  {
	    $(featdiv).draggable("destroy");
	}
    }
    console.log("removeFromSelection " + featdiv);
}

/**
*  for the give feature div, remove any descendant feature divs
*/ 
DraggableFeatureTrack.removeChildrenFromSelection = function(featdiv)  {
    console.log("in removeChildrenFromSelection");
    var child_divs = $(".selected-feature", featdiv);
    child_divs.each( function(idx, elem) {
	console.log("result " + idx);
	console.log(elem);
	DraggableFeatureTrack.removeFromSelection(elem);
    } );
/*  was trying something less elegant, for fear of recursive search in jquery for ".selected-feature" 
       following the featdiv.track link and getting _all_ selected-features, but jquery seems to 
       be smart enough to avoid that...
        
    var child_divs = $(featdiv).children("div[feature], div[subfeature]");
    console.log(child_divs);
    if (child_divs.length > 0)  {
	child_divs.each( function(idx, elem)  {
	    if ($(elem).hasClass("selected-feature"))  {
		DraggableFeatureTrack.removeFromSelection(elem);
	    }
	    console.log("result " + idx);
	    console.log(elem);
	    DraggableFeatureTrack.removeChildrenFromSelection(elem);;
	} );
    }
*/
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
    var featdiv = FeatureTrack.prototype.renderFeature.call(this, feature, uniqueId, block, scale,
                                                            containerStart, containerEnd);
    if (featdiv)  {  // just in case featDiv doesn't actually get created
	// adding pointer to track for each featdiv
	//   (could get this by DOM traversal, but shouldn't take much memory, and having it with each featdiv is more convenient)
	featdiv.track = this;
	// using JQuery bind() will normalize events to W3C spec (don't have to worry about IE inconsistencies, etc.)
	$(featdiv).bind("mousedown", this.featMouseDown);
	$(featdiv).bind("dblclick", this.featDoubleClick);
	// $(featdiv).bind("mouseenter", this.featMouseEnter);
	// $(featdiv).bind("mouseleave", this.featMouseLeave);
    }
    return featdiv;
}

DraggableFeatureTrack.prototype.renderSubfeature = function(feature, featDiv, subfeature,
							    displayStart, displayEnd) {
    var subfeatdiv = FeatureTrack.prototype.renderSubfeature.call(this, feature, featDiv, subfeature, 
								  displayStart, displayEnd);
    if (subfeatdiv)  {  // just in case subFeatDiv doesn't actually get created
	// adding pointer to track for each subfeatdiv 
	//   (could get this by DOM traversal, but shouldn't take much memory, and having it with each subfeatdiv is more convenient)
	subfeatdiv.track = this;
	$(subfeatdiv).bind("mousedown", this.featMouseDown);
	$(subfeatdiv).bind("dblclick", this.featDoubleClick);
	//	subfeatdiv.onmouseover = this.mouseOverFeat;
	//	subfeatdiv.onmouseup = this.mouseUpFeat;
    }
    return subfeatdiv;
}

/*
DraggableFeatureTrack.prototype.featMouseEnter = function(event)  {
    console.log("DFT.featMouseEnter");
    console.log(this);
    console.log("  dragging in progress: " + ($(".ui-draggable-dragging").length > 0));
}
*/

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
 *
 *   "this" should be a featdiv or subfeatdiv
 */
DraggableFeatureTrack.prototype.featMouseDown = function(event) {
    // event.stopPropagation();

    // checking for whether this is part of drag setup retrigger of mousedown -- if so then don't do selection or re-setup draggability)
    //     this keeps selection from getting confused, 
    //     and keeps trigger(event) in draggable setup from causing infinite recursion in event handling calls to featMouseDown
    if (this.drag_create)  { 
	console.log("DFT.featMouseDown re-triggered event for drag initiation, drag_create: " + this.drag_create);
	console.log(this);
	this.drag_create = null;
    }
    else  {
	console.log("DFT.featMouseDown actual event");
	console.log(this);
	var featdiv = this;
	var feat = featdiv.feature;
	if (feat)  {feat.isSubFeature = false;}
	else  {feat = featdiv.subfeature; feat.isSubFeature = true;}
	var already_selected = (DraggableFeatureTrack.getSelectedDivs().indexOf(featdiv) > -1);
	var parent_featdiv = DraggableFeatureTrack.prototype.getParentFeatureDiv(featdiv);
	var parent_selected = false;
	if (parent_featdiv && parent_featdiv !== null)  {
	    parent_selected = (DraggableFeatureTrack.getSelectedDivs().indexOf(parent_featdiv) > -1);
	}
	console.log("parent selected: " + parent_selected);
	console.log("already selected: " + already_selected);
	var trigger_draggable = false;
	// if parent is selected, allow propagation of event up to parent, 
	//     in order to ensure parent draggable setup and triggering
	// otherwise stop propagation
	if (! parent_selected)  {
	    event.stopPropagation();
	}
	if (event.shiftKey)  {
	    console.log("shift modifier detected");
	    if (already_selected) {  // if shift-mouse-down and this already selected, deselect this
		DraggableFeatureTrack.removeFromSelection(featdiv);
	    }
	    else if (parent_selected)  {   // if shift-mouse-down and parent selected, deselect parent
		DraggableFeatureTrack.removeFromSelection(parent_featdiv);
	    }
	    else  {  // if shift-mouse-down and neither this or parent selected, select this
		DraggableFeatureTrack.removeChildrenFromSelection(featdiv);  // make sure children are deselected
		DraggableFeatureTrack.addToSelection(featdiv);
		// selecting this, must remove selection of any child features
		trigger_draggable = true;
	    }
	}
	else  {  // no shift modifier
	    console.log("no shift modifier");
	    if (already_selected)  {  // if this selected, do nothing (this remains selected)
		console.log("already selected");
		trigger_draggable = true;  // rechecking for draggability and triggering if needed
	    }
	    else  {
		console.log("not yet selected");
		if (parent_selected)  {  
		    // if this not selected but parent selected, do nothing (parent remains selected)
		    //    event will propagate up (since parent_selected), so draggable check will be done in bubbled parent event
		}
		else  {  // if this not selected and parent not selected, select this
		    DraggableFeatureTrack.clearSelection();
		    DraggableFeatureTrack.addToSelection(featdiv);
		    trigger_draggable = true;
		}
	    }
	}
	/** 
	 *  ideally would only make $.draggable call once for each selected div
	 *  but having problems with draggability disappearing from selected divs that $.draggable was already called on
	 *  therefore whenever mousedown on a previously selected div also want to check that draggability and redo if missing 
	 */  
	if (trigger_draggable)  {
	    if (! $(this).hasClass("ui-draggable"))  {  
		console.log("setting up dragability");
		console.log(this);
		$(this).draggable(   // draggable() adds "ui-draggable" class to 
		    {helper: 'clone', 
		     opacity: 0.3, 
		     axis: 'y', 
		     create: function(event, ui)  { this.drag_create = true; }
		    } ).trigger(event);
	    }
	}
    }
//    DraggableFeatureTrack.setDragging(true);
}


	// using trigger(event) trick to assign draggability dynamically on mouse down
	//    (rather than assigning for every feat/subfeat div)
	// WARNING: _must_ check for already draggable, or trigger() below will cause infinite recursion
	// UPDATE:  avoidance of infinite recursion should be taken care of by checking "drag_create" property above, 
	//          but still checking for already draggable to avoid extra draggable() calls
	// TODO:  may want to unset draggability once dropped?  non needed though
/*
	if (! $(this).hasClass("ui-draggable"))  {  
	    console.log("setting up dragability");
	    console.log(this);
	    $(this).draggable(   // draggable() adds "ui-draggable" class to 
		{helper: 'clone', 
		 opacity: 0.3, 
		 axis: 'y', 
		 create: function(event, ui)  { this.drag_create = true; }
		} ).trigger(event);
	}
*/	


DraggableFeatureTrack.prototype.featDoubleClick = function(event)  {
    event.stopPropagation();
    console.log("DFT.featDoubleClick");
    console.log(this);
    // prevent event bubbling up to genome view and triggering zoom
    var featdiv = this;
    // only take action on double-click for subfeatures 
    //  (but stop propagation for both features and subfeatures)
    // GAH TODO:  make this work for feature hierarchies > 2 levels deep
    if (featdiv.subfeature)  {
	console.log("double clicked on subfeature div");
	var already_selected = (DraggableFeatureTrack.getSelectedDivs().indexOf(featdiv) > -1);
	// if subfeature already selected, deselect 
	if (already_selected)  { 
	    DraggableFeatureTrack.removeFromSelection(featdiv);  // not really needed, given removeChildrenFromSelection call below...
	    // disable subfeature drag?
	}
	// select parent feature
	var parent_featdiv = DraggableFeatureTrack.prototype.getParentFeatureDiv(featdiv);
	if (parent_featdiv !== null)  {
	    DraggableFeatureTrack.removeChildrenFromSelection(parent_featdiv);  // make sure children are deselected
	    DraggableFeatureTrack.addToSelection(parent_featdiv);
	    // enable parent feature drag?
	    // not needed here, since mouse down _after_ double-click is required to initiate drag, 
	    //    and making sure selected divs are draggable is handled in mouse down

	    // deselect all children of parent feature??  
	    // regardless of whether shift-modifier or not???
	}
    }

}


/**
 *  feature click no-op (to override FeatureTrack.onFeatureClick, which conflicts with mouse-down selection
 */
DraggableFeatureTrack.prototype.onFeatureClick = function(event) {
    // event.stopPropagation();
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
  /*  GAH following is no longer necessary, event propagation is stopped in featMouseDown ??
  $(".draggable-feature").bind("mousedown", function(evt) {
//        console.log("makeDraggable: evt.stopPropagation.  elem.feature = " + elem.feature + ", currentSelection = " + DraggableFeatureTrack.prototype.getCurrentSelection());  // DEL
	evt.stopPropagation();
    });
*/

    var source_track = this;      
    var source_fields = this.fields;
    var source_subFields = this.subFields;

    // Note that this relies on the annotation track's name being "Annotations".  Need to make this more general.
    $("#track_Annotations").droppable({
	drop: function(ev, ui) {
	    // within drop(), "this" is trackDiv that feature div is dropped on
	    // console.log("makeDroppable: stopped dragging");  // DEL
//            DraggableFeatureTrack.setDragging(false);
    	    var target_track = this.track;
	   
    	var features = target_track.features;
        // This creates a new annotation for each currently selected feature (not a multi-exon feature comprised of the selected features, as we'd like).
        // Also, the drag-ghost is only of the most recently selected feature--it's not capable of showing the drag-ghosts of all selected features.
           var selected = DraggableFeatureTrack.getSelectedFeatures();
	   // New Plan: for each entry in selected, entry = { "feature" : feature, "div" : featDiv, "track" : track }
	   
           for (var i in selected)  {
            var feat = selected[i];
	       console.log("old feature: ");
	       console.log(feat);

	       if (AnnotTrack.USE_LOCAL_EDITS)  {
		   var newfeat = JSONUtils.convertToTrack(feat, source_track, target_track);
		   // vare newfeat = convertToTrack(feat, selection.track, target_track);
		   //		   var id = newfeat[target_track.fields["id"]];
		   var id = "annot_" + AnnotTrack.creation_count++; 
		   //		   if (id && id != null)  { id = id + "_annot_" + AnnotTrack.creation_count++; }
		   //		   else  { id = "annot_" + AnnotTrack.creation_count++; }
		   newfeat[target_track.fields["id"]] = id;
		   newfeat[target_track.fields["name"]] = id;
		   console.log("new feature: ");
		   console.log(newfeat);
		   features.add(newfeat, id);
		   target_track.hideAll();
		   target_track.changed();
	       }
	       else  {
            var responseFeature;
	    // creating JSON feature data struct that WebApollo server understands, based on JSON feature data struct that JBrowse understands
            var topLevelFeature = JSONUtils.createJsonFeature(feat[source_fields["start"]], feat[source_fields["end"]], feat[source_fields["strand"]], "SO", "gene");
	    //console.log("createJsonFeature: " + topLevelFeature);
	    console.log("createJsonFeature: ");
	    console.log(topLevelFeature);
	    var testFeature = JSONUtils.createApolloFeature(feat, source_fields, source_subFields, "transcript");
	    console.log("createApolloFeature: ");
	    console.log(testFeature);
	    console.log(source_fields);
	
                dojo.xhrPost( {
                    // "http://10.0.1.24:8080/ApolloWeb/Login?username=foo&password=bar" to login
                    // postData: '{ "track": "' + track.name + '", "features": [ ' + JSON.stringify(topLevelFeature) + '], "operation": "add_feature" }',
                    // postData: '{ "track": "' + track.name + '", "features": [ ' + JSON.stringify(testFeature) + '], "operation": "add_transcript" }',
                    postData: '{ "track": "' + target_track.name + '", "features": [ ' + JSON.stringify(testFeature) + '], "operation": "add_feature" }',
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
			    if (!target_track.comet_working)  {
               			// responseFeatures = responseFeatures.features;
                		responseFeatures = response.features;
				/*
				for (var j in responseFeatures)  {
				    var newfeat = JSONUtils.createJBrowseFeature(responseFeatures[i], fields);
				    features.add(newfeat, newfeat.id);
				} 
				*/ 
				var featureArray = JSONUtils.convertJsonToFeatureArray(responseFeatures[0]);
				features.add(featureArray, responseFeatures[0].uniquename);

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
