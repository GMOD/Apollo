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

// Returns an array 
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
    event = event || window.event;
    var elem = (event.currentTarget || event.srcElement);
    // if (!elem.feature) elem = elem.parentElement;
    var featdiv = this.getTopLevelFeatureDiv(elem);
    var feat = featdiv.feature;
    console.log("DFT.onMouseDown: started dragging " + feat); // DEL
    DraggableFeatureTrack.prototype.addToSelection(feat);
        // This only works the SECOND time you try to drag a feature.
//	event.stopPropagation();
//	this.makeDraggableAndDroppable(elem);
        DraggableFeatureTrack.prototype.setDragging(true);
}

DraggableFeatureTrack.prototype.onMouseOver = function(event) {
    if (DraggableFeatureTrack.prototype.isDragging()) {
        console.log("DFT.onMouseOver: already dragging"); // DEL
        return;
    }
    event = event || window.event;
    var elem = (event.target || event.srcElement);
    var featdiv = this.getTopLevelFeatureDiv(elem);
    if (featdiv === null) return; //shouldn't happen; just bail if it does
    console.log("DFT: mouseover on draggablefeature " + featdiv.feature + ", elem.className = " + featdiv.className + ", event = " + event);  
    // Make this feature draggable & droppable
    this.makeDraggableAndDroppable(featdiv);
}

DraggableFeatureTrack.prototype.onMouseUp = function(event) {
    DraggableFeatureTrack.prototype.setDragging(false);
//    console.log("DFT.onMouseup: stopped dragging");  // DEL
}

/**
    (should switch to using dojo.connect or dojo.fixEvent for normalized events, so can remove IE specific code
*/
DraggableFeatureTrack.prototype.onFeatureClick = function(event) {

    event = event || window.event;  // window.event for IE, event for other browsers 
    var elem = (event.target || event.srcElement);  // e.srcElement for IE, e.target for other browsers
    // due to event bubbling, elem may be for a feature or a subfeature (assumes subfeature elems are _leaf_ divs)
    var featdiv = this.getTopLevelFeatureDiv(elem);
    if (featdiv === null)  { return; } //shouldn't happen; just bail if it does
    var feat = featdiv.feature;
    if (feat === null)  { return; } //shouldn't happen; just bail if it does
    console.log("DFT: user clicked on draggablefeature: start = " + feat[this.fields["start"]] + ", id = " + feat[this.fields["id"]]);

    // If feature was already selected, deselect it.  Otherwise, select it (add to current selected set).
    this.toggleSelection(featdiv);
};

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
	if (elem === document)  { return null; } 
    }
    // found a feature, now crawl up hierarchy till top feature (feature with no parent feature)
    while (elem.parentNode.feature)  {
	elem = elem.parentNode;
    }
    return elem;
}


/* If elem is selected, deselect it.  Otherwise, select it.
 * Appearance of selected elements depends on ".selected-feature" style in CSS stylesheet (usually genome.css)
 *    (assumes feature div styles are entirely handled in CSS stylesheet (so not overriden by styles in code))
 */
DraggableFeatureTrack.prototype.toggleSelection = function(elem) {
    //    if $(elem).hasClass("selected-feature"))  {  // start of jquery equivalent to dojo way...
    if (dojo.hasClass(elem, "selected-feature"))  {
	dojo.removeClass(elem, "selected-feature");
        DraggableFeatureTrack.prototype.removeFromSelection(elem.feature);
    }
    else  { 
	dojo.addClass(elem, "selected-feature");
	// DraggableFeatureTrack.prototype.addToSelection(elem.feature);   // already done
    }
}


// Make this DraggableFeatureTrack draggable
DraggableFeatureTrack.prototype.makeDraggableAndDroppable = function(elem) {
    // Check whether we've already done it--look at class name and see if it includes "draggable-feature"
    if (dojo.hasClass(elem, "draggable-feature")) {
        console.log("Already has draggable-feature class: " + elem.feature + ", className = " + elem.className );
        return;
    }

    dojo.addClass(elem, "draggable-feature");
    console.log("makeDraggable: feature = " + elem.feature + ", className = " + elem.className);
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
    // Note that this relies on the annotation track's name being "Annotations".  Need to make this more general.
    $("#track_Annotations").droppable({
       drop: function(ev, ui) {
//        console.log("makeDroppable: stopped dragging");  // DEL
        DraggableFeatureTrack.prototype.setDragging(false);
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
                	postData: '{ "track": "' + track.name + '", "features": [ ' + JSON.stringify(topLevelFeature) + '], "operation": "add_feature" }',
                	url: "/ApolloWeb/AnnotationEditorService",
                	handleAs: "text",
                	timeout: 5000, // Time in milliseconds
                	// The LOAD function will be called on a successful response.
                	load: function(response, ioArgs) { //
                	console.log("Successfully created annotation object: " + response)
                	responseFeatures = eval('(' + response + ')').features;
                	var featureArray = JSONUtils.convertJsonToFeatureArray(responseFeatures[0]);
                	features.add(featureArray, responseFeatures[0].uniquename);
                	track.hideAll();
                	track.changed();
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

/*
Copyright (c) 2010-2011 Berkeley Bioinformatics Open-source Projects & Lawrence Berkeley National Labs

This package and its accompanying libraries are free software; you can
redistribute it and/or modify it under the terms of the LGPL (either
version 2.1, or at your option, any later version) or the Artistic
License 2.0.  Refer to LICENSE for the full license text.

*/
