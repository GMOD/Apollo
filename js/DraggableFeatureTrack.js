/*  Subclass of FeatureTrack that allows features to be dragged and dropped into the annotation track to create annotations. */
function DraggableFeatureTrack(trackMeta, url, refSeq, browserParams) {
    FeatureTrack.call(this, trackMeta, url, refSeq, browserParams);
//    thisObj = this;
    var thisObj = this;
    this.mouseOverFeat = function(event)  {thisObj.onMouseOver(event);}
    this.mouseDownFeat = function(event)  {thisObj.onMouseDown(event);}
    this.mouseUpFeat = function(event)  {thisObj.onMouseUp(event);}

    this.dragging = false;
    this.sel = [];
}

// Inherit from FeatureTrack
DraggableFeatureTrack.prototype = new FeatureTrack();

DraggableFeatureTrack.prototype.setDragging = function(onOrOff) {
//        console.log("setDragging(" + onOrOff + ")");
    this.dragging = onOrOff;
}

DraggableFeatureTrack.prototype.isDragging = function() {
//        console.log("isDragging: " + this.dragging);
    return this.dragging;
}

DraggableFeatureTrack.prototype.addToSelection = function(newThing) {
    if (this.sel == undefined)
        this.sel = [newThing];
    else {
        // Check if it's already in the selected array before adding it
        var idx = this.sel.indexOf(newThing);
        if (idx == -1)
            this.sel.push(newThing);
    }
    console.log("addToSelection " + newThing + "; now sel = " + this.sel); // DEL
}

DraggableFeatureTrack.prototype.clearSelection = function() {
    this.sel = [];
}

DraggableFeatureTrack.prototype.removeFromSelection = function(thing) {
    var idx = this.sel.indexOf(thing);
    if (idx > -1)
        this.sel.splice(idx, 1);
    else
        console.error("Couldn't find " + thing + " in selection set " + this.sel);
    console.log("removeFromSelection " + thing + "; now sel = " + this.sel); // DEL
}

/* Returns an array */
DraggableFeatureTrack.prototype.getSelected = function() {
    return this.sel;
}

/**
 *  overriding renderFeature to add event handling for mouseover, mousedown, mouseup
 */
DraggableFeatureTrack.prototype.renderFeature = function(feature, uniqueId, block, scale,
                                                containerStart, containerEnd) {
    var featDiv = FeatureTrack.prototype.renderFeature.call(this, feature, uniqueId, block, scale,
                                                            containerStart, containerEnd);
    featDiv.onmouseover = this.mouseOverFeat;
    featDiv.onmousedown = this.mouseDownFeat;
    featDiv.onmouseup = this.mouseUpFeat;
    return featDiv;
}

/* I would think that you could add the draggable to the feature at mousedown, but it seems to be already too late--
 * had to add at mouseover.
 */
DraggableFeatureTrack.prototype.onMouseDown = function(event) {
    var elem = (event.currentTarget || event.srcElement);
    if (!elem.feature) elem = elem.parentElement;
    console.log("DFT.onMouseDown: started dragging " + elem.feature); // DEL
    DraggableFeatureTrack.prototype.addToSelection(elem.feature);
        // This only works the SECOND time you try to drag a feature.
//	event.stopPropagation();
//	track.makeDraggableAndDroppable(elem);
        
//        // Drag other selected features along with elem
//      // It turns out that JQuery doesn't directly support multi-drag.  I'm leaving in this commented-out attempt in case it's at all useful.
//        var selected = DraggableFeatureTrack.prototype.getSelected();
//        for (var i = 0; i < selected.length; i++) {
//                var feat = selected[i];
//	    	console.log("Trying to drag feature " + i + ": " + feat); // DEL
//                if (feat == elem.feature)
//                        console.log("Feature " + i + " is the feature that's being dragged");
//                else
//                        $(".draggable-feature").draggable().trigger(event); // Will this drag anything that's ever been selected?
//        }

        DraggableFeatureTrack.prototype.setDragging(true);
}

DraggableFeatureTrack.prototype.onMouseOver = function(event) {
    if (DraggableFeatureTrack.prototype.isDragging()) {
//            console.log("DFT.onMouseOver: already dragging"); // DEL
        return;
    }

    event = event || window.event;
    var track = this;
    if (event.shiftKey) return;
    var elem = (event.currentTarget || event.srcElement);
    // !! What if we don't do this?
    if (!elem.feature) elem = elem.parentElement;
    if (!elem.feature) return; //shouldn't happen; just bail if it does
//    console.log("DFT: mouseover on draggablefeature " + elem.feature + ", elem.className = " + elem.className + ", event = " + event);  // DEL
        // Make this feature draggable & droppable
    track.makeDraggableAndDroppable(elem);
}

DraggableFeatureTrack.prototype.onMouseUp = function(event) {
    DraggableFeatureTrack.prototype.setDragging(false);
//    console.log("DFT.onMouseup: stopped dragging");  // DEL
}

DraggableFeatureTrack.prototype.onFeatureClick = function(event) {
    var track = this;
    event = event || window.event;
//    if (event.shiftKey) return;

    var elem = (event.currentTarget || event.srcElement);
    //depending on bubbling, we might get the subfeature here
    //instead of the parent feature
    if (!elem.feature) elem = elem.parentElement;
    if (!elem.feature) return; //shouldn't happen; just bail if it does

    // For debugging, but also to let a click on a feature potentially bring up other info (e.g. a web page)
    var fields = track.fields;
//    console.log("DFT.onFeatureClick: fields = " + fields + ", fields[end] = " + fields["end"]);

    var feat = elem.feature;
    console.log("DFT: user clicked on draggablefeature " + elem.feature +"\nstart: " + feat[fields["start"]] +
	  ", end: " + feat[fields["end"]] +
	  ", strand: " + feat[fields["strand"]] +
	  ", label: " + feat[fields["name"]] +
	  ", ID: " + feat[fields["id"]]);

    // If feature was already selected, deselect it.  Otherwise, select it (add to current selected set).
    track.toggleSelection(elem);
};

/* If elem is selected, deselect it.  Otherwise, select it.
 * Selected elements get a red border and become draggable.
 */
DraggableFeatureTrack.prototype.toggleSelection = function(elem) {
    if (elem.style.border == "") {  // !! What if it had a border set by its style?
//        DraggableFeatureTrack.prototype.addToSelection(elem.feature);   // already done
	dojo.addClass(elem, "selected");
	elem.style.border = "3px solid red";
    }
    // Else, need to take it off the "selected" list
    else {
        DraggableFeatureTrack.prototype.removeFromSelection(elem.feature);
	dojo.removeClass(elem, "selected");
	elem.style.border = "";
    }
//    console.log("toggleSelection: now elem.style.border = " + elem.style.border); // DEL
}

// Make this DraggableFeatureTrack draggable
DraggableFeatureTrack.prototype.makeDraggableAndDroppable = function(elem) {
    // Check whether we've already done it--look at class name and see if it includes "draggable-feature"
    if (DraggableFeatureTrack.prototype.hasString(elem.className, "draggable-feature")) {
//            console.log("makeDraggable: feature = " + elem.feature + ", className = " + elem.className + "--already has draggable-feature");
        return;
    }

    dojo.addClass(elem, "draggable-feature");
    console.log("makeDraggable: feature = " + elem.feature + ", className = " + elem.className);
    $(".draggable-feature").draggable({
	    helper:'clone',
//      // It turns out that JQuery doesn't directly support multi-drag.  I'm leaving in this commented-out attempt in case it's at all useful.
// 	    helper: function(event, ui) {
// 		var selected = DraggableFeatureTrack.prototype.getSelected();
// 		for (var i = 0; i < selected.length; i++) {
// 		    var feat = selected[i];
// 		    console.log("Trying to drag feature " + i + ": " + feat); // DEL
// 		    $(feat).clone();
// 		    $(feat).addClass('ui-draggable-dragging');
// 		}
// 		//		return $(this).clone(); // ?
// 	    },
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
    // Note that this relies on the annotation track's name being "Annotations".  Need to make this more general.
    $("#track_Annotations").droppable({
       drop: function(ev, ui) {
//        console.log("makeDroppable: stopped dragging");  // DEL
        DraggableFeatureTrack.prototype.setDragging(false);
    	// Clone the dragged feature
	   // GAH not sure if newAnnot is really needed (think some of it's functionality was replaced by list of selected elems?)
        // var newAnnot=$(ui.draggable).clone();
//        console.log("makeDroppable: feat = " + elem.feature + ", currentSelection = " + DraggableFeatureTrack.prototype.getCurrentSelection());  // DEL
    	// Change its class to the appropriate annot class
    	// DraggableFeatureTrack.prototype.setAnnotClassNameForFeature(newAnnot);

    	// Set vertical position of dropped item (left position is based on dragged feature)
        // newAnnot.css({"top": 0});
    	// Restore border of annot to its default (don't want selection border)
        // newAnnot.css({"border": null});

    	var track = this.track;
    	var features = this.track.features;
        // This creates a new annotation for each currently selected feature (not a multi-exon feature comprised of the selected features, as we'd like).
        // Also, the drag-ghost is only of the most recently selected feature--it's not capable of showing the drag-ghosts of all selected features.
        var selected = DraggableFeatureTrack.prototype.getSelected();
        for (var i = 0; i < selected.length; i++) {
                var feat = selected[i];
                console.log("Creating new annotation for feature " + i + ": " + feat); // DEL
                var responseFeatures;
	    // creating JSON feature data struct that WebApollo server understands, based on JSON feature data struct that JBrowse understands
                var topLevelFeature = JSONUtils.createJsonFeature(feat[fields["start"]], feat[fields["end"]], feat[fields["strand"]], "SO", "gene");
	
                dojo.xhrPost( {
                	// "http://10.0.1.24:8080/ApolloWeb/Login?username=foo&password=bar" to login
                	//postData: '{ "track": "' + track.name + '", "features": [{ "location": { "fmax": ' + feat[fields["end"]] + ', "fmin": ' + feat[fields["start"]] + ', "strand": ' + feat[fields["strand"]] + ' }, "type": { "cv": {"name": "SO"}, "name": "gene" }}], "operation": "add_feature" }',
                	postData: '{ "track": "' + track.name + '", "features": [ ' + JSON.stringify(topLevelFeature) + '], "operation": "add_feature" }',
                	url: "/ApolloWeb/AnnotationEditorService",
                	handleAs: "json",
                	timeout: 5000, // Time in milliseconds
                	// The LOAD function will be called on a successful response.
                	load: function(response, ioArgs) { //
                	console.log("Successfully created annotation object: " + response)
                	// response processing is now handled by the long poll thread (when using servlet 3.0)
                	// uncomment code to get it working with servlet 2.5
//                	responseFeatures = responseFeatures.features;
//                	var featureArray = JSONUtils.convertJsonToFeatureArray(responseFeatures[0]);
//                	features.add(featureArray, responseFeatures[0].uniquename);
//                	track.hideAll();
//                	track.changed();
//              	console.log("DFT: responseFeatures[0].uniquename = " + responseFeatures[0].uniquename);
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
	   DraggableFeatureTrack.prototype.clearSelection();
	   
//      console.log("itemDragged: " + newAnnot); //  + ", pos.left = " + pos.left + ", pos.top = " + pos.top + ", width = " + ui.draggable.width());
    }
    });
}

/* Check whether an array includes the string in question */
DraggableFeatureTrack.prototype.hasString = function(array, string) {
    if (array == null)
        return false;
    var arr2str = array.toString();
    if (arr2str.search(string) >= 0)
        return true;
    return false;
}


/* Change feature class name to the corresponding annot class name */
DraggableFeatureTrack.prototype.setAnnotClassNameForFeature = function(feature) {
    var arrList = feature.attr("class").split(' ');
    for ( var i = 0; i < arrList.length; i++ ) {
        var aclass = arrList[i];
        if (aclass.match("us-")) {  // plus-* or minus-*
            arrList[i] = aclass.replace(/us-\w+/, "us-annot");
            feature.removeClass(aclass).addClass(arrList[i]);
        }
    }
}

/*

Copyright (c) 2007-2011 The Evolutionary Software Foundation

This package and its accompanying libraries are free software; you can
redistribute it and/or modify it under the terms of the LGPL (either
version 2.1, or at your option, any later version) or the Artistic
License 2.0.  Refer to LICENSE for the full license text.

*/
