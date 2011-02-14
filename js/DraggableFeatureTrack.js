
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
DraggableFeatureTrack.USE_MULTIDRAG = false;
DraggableFeatureTrack.featToDiv = [];
DraggableFeatureTrack.featToSubDivs = [];

// feat may be a feature or subfeature?
DraggableFeatureTrack.showEdgeMatches = function(feat)  {
    /* experimenting with highlighting edges of features that match selected features (or their subfeatures) */
    //	    var ftracks = $("div.track[features]");
    console.log("finding feature tracks that match:");
    //	    var feat = feature || subfeature;
    var first_left_hit = true;
    var first_right_hit = true;
    // TODO remove hardwiring of min and max index (need track info to do this)
    var min = feat[0];
    var max = feat[1];
    var ftracks = $("div.track").each( function(index, elem)  {
	var ftrak = elem.track;
	if (ftrak && ftrak.features)  {
	    var nclist = ftrak.features;
	    // iterate calls function only for features that overlap min/max coords
	    nclist.iterate(min, max, function(rfeat, path) {
		// TODO remove hardwiring of subfeature index
		var subfeats = feat[4];
		var rsubfeats = rfeat[4];
		if (subfeats instanceof Array && rsubfeats instanceof Array && rsubfeats[0] instanceof Array)  {
		    //			    console.log("found overlap");
		    //			    console.log(rfeat);
		    var id = feat[3];
		    var rid = rfeat[3];
		    var rdiv = DraggableFeatureTrack.featToDiv[rid];
		    var rsubdivs = DraggableFeatureTrack.featToSubDivs[rid];
		    if (rdiv && rsubdivs)  {
			// console.log(rdiv);
			// console.log(rsubdivs);
			for (var i in subfeats)  {
			    var sfeat = subfeats[i];
			    var smin = sfeat[0];
			    var smax = sfeat[1];
			    for (var j in rsubfeats)  {
				var rfeat = rsubfeats[j];
				var rmin = rfeat[0];
				var rmax = rfeat[1];
				if (smin === rmin)  {
				    var rsubdiv = rsubdivs[j];
				    if (rsubdiv)  {
					$(rsubdiv).addClass("left-edge-match");
					if (first_left_hit)  {
					    console.log("left match:");
					    console.log(rfeat);
					    console.log("left match div: ");
					    console.log(rsubdiv);
					    first_left_hit = false;
					}
				    }
				}
				if (smax === rmax)  {
				    var rsubdiv = rsubdivs[j];
				    if (rsubdiv)  {
					$(rsubdiv).addClass("right-edge-match");
					if (first_right_hit)  {
					    console.log("right match:");
					    console.log(rfeat);
					    console.log("right match div: ");
					    console.log(rsubdiv);
					    first_right_hit = false;
					}
				    }

				}
			    }
			}
		    }
		}
	    }, function() {} );  // empty function for no-op on finishing
	}
    } );
}

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
	    var feat;
	    if (featdiv.feature)  {
		feat = featdiv.feature;
		DraggableFeatureTrack.sel_features.push(featdiv.feature);
		// console.log("add feature to selection");
		// console.log(featdiv.feature);
	    }
	    else if (featdiv.subfeature)  {
		feat = featdiv.subfeature;
		DraggableFeatureTrack.sel_features.push(featdiv.subfeature);
		// console.log("add subfeature to selection");
		// console.log(featdiv.subfeature);
	    }
	    $(featdiv).addClass("selected-feature");
	    if (feat)  { DraggableFeatureTrack.showEdgeMatches(feat); }
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
	if ($(featdiv).hasClass("ui-multidraggable"))  {
	    $(featdiv).multidraggable("destroy");
	}
    }
    DraggableFeatureTrack.sel_divs = [];
    DraggableFeatureTrack.sel_features = [];
    $(".left-edge-match").removeClass("left-edge-match");
    $(".right-edge-match").removeClass("right-edge-match");
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
	if ($(featdiv).hasClass("ui-multidraggable"))  {
	    $(featdiv).multidraggable("destroy");
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

	// TODO: need to clear this if re-rendering (or clear on a per-div basis? maybe when blocks are destroyed?)
	DraggableFeatureTrack.featToDiv[feature[this.fields["id"]]] = featdiv;
	// DraggableFeatureTrack.featToSubDivs[feature[this.fields["id"]]] = [];
    }
    return featdiv;
}

DraggableFeatureTrack.prototype.renderSubfeature = function(feature, featDiv, subfeature,
							    displayStart, displayEnd )  {
							    // subindex) {
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
	var id = feature[this.fields["id"]];
	if (id && id !== null)  {
	    var subdivs = DraggableFeatureTrack.featToSubDivs[id];
	    if (! subdivs)  {subdivs = [];DraggableFeatureTrack.featToSubDivs[id] = subdivs;}
	    //	console.log(subindex);
	    // subdivs[subindex] = subfeatdiv;
//	    console.log(subdivs.length);
//	    console.log(feature);
//	    console.log(subfeature);
	    subdivs.push(subfeatdiv);
//	    console.log(DraggableFeatureTrack.featToSubDivs[id][subdivs.length-1]);
	}
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
	else  {feat = featdiv.subfeature;feat.isSubFeature = true;}
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
	    if (DraggableFeatureTrack.USE_MULTIDRAG)  {
		if (! $(this).hasClass("ui-multidraggable"))  {  
		    console.log("setting up multi-dragability");
		    console.log(this);
		    $(this).multidraggable(   // multidraggable() adds "ui-multidraggable" class to siv
			{helper: 'clone', 
			 opacity: 0.3, 
			 axis: 'y', 
			 create: function(event, ui)  {this.drag_create = true;}
			} ).trigger(event);
		}
	    }
	    else if (! $(this).hasClass("ui-draggable"))  {  
		console.log("setting up dragability");
		console.log(this);
		$(this).draggable(   // draggable() adds "ui-draggable" class to div
		    {
			helper: 'clone', 
		 /* experimenting for pseudo-multi-drag 
  		        helper: function() { 
			    var holder = document.createElement("div");
			    var seldivs = DraggableFeatureTrack.getSelectedDivs();
			    for (var i in seldivs)  {
				var featclone = $(seldivs[i]).clone();
				console.log("drag helper experiment");
				console.log(holder);
				console.log(featclone);
				holder.appendChild(featclone[0]);
			    }
			    //  var featclone = $(this).clone();
			    console.log(holder);
			    return holder;
			}, 
*/
		     opacity: 0.5, 
		     axis: 'y', 
		     create: function(event, ui)  {this.drag_create = true;}
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

/*
Copyright (c) 2010-2011 Berkeley Bioinformatics Open-source Projects & Lawrence Berkeley National Labs

This package and its accompanying libraries are free software; you can
redistribute it and/or modify it under the terms of the LGPL (either
version 2.1, or at your option, any later version) or the Artistic
License 2.0.  Refer to LICENSE for the full license text.

*/
