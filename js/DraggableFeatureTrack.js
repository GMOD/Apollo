/*  Subclass of FeatureTrack that allows features to be dragged and dropped into the annotation track to create annotations. */
function DraggableFeatureTrack(trackMeta, url, refSeq, browserParams) {
    FeatureTrack.call(this, trackMeta, url, refSeq, browserParams);
    thisObj = this;
    this.dragging = false;
    this.currentSelection = null;
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

DraggableFeatureTrack.prototype.setCurrentSelection = function(selected) {
        console.log("setCurrentSelection " + selected); // DEL
        this.currentSelection = selected;
}

DraggableFeatureTrack.prototype.getCurrentSelection = function() {
        return this.currentSelection;
}

/* I would think that you could add the draggable to the feature at mousedown, but it seems to be already too late--
 * had to add at mouseover.
 */
DraggableFeatureTrack.prototype.onMouseDown = function(event) {
    var elem = (event.currentTarget || event.srcElement);
    if (!elem.feature) elem = elem.parentElement;
    var track = thisObj;
        console.log("DFT.onMouseDown: started dragging " + elem.feature); // DEL
        DraggableFeatureTrack.prototype.setCurrentSelection(elem.feature);
        // This only works the SECOND time you try to drag a feature.
//	event.stopPropagation();
//	track.makeDraggableAndDroppable(elem);
        DraggableFeatureTrack.prototype.setDragging(true);
}

DraggableFeatureTrack.prototype.onMouseOver = function(event) {
    if (DraggableFeatureTrack.prototype.isDragging()) {
//            console.log("DFT.onMouseOver: already dragging"); // DEL
            return;
    }

    event = event || window.event;
    var track = thisObj;
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
//        this.currentSelection = null;
}

DraggableFeatureTrack.prototype.onFeatureClick = function(event) {
    var track = thisObj;
    event = event || window.event;
    if (event.shiftKey) return;
    var elem = (event.currentTarget || event.srcElement);
    //depending on bubbling, we might get the subfeature here
    //instead of the parent feature
    if (!elem.feature) elem = elem.parentElement;
    if (!elem.feature) return; //shouldn't happen; just bail if it does

    // For debugging, but also to let a click on a feature potentially bring up other info (e.g. a web page)
    var fields = thisObj.fields;
//    console.log("DFT.onFeatureClick: fields = " + fields + ", fields[end] = " + fields["end"]);

    var feat = elem.feature;
    console.log("DFT: user clicked on draggablefeature " + elem.feature +"\nstart: " + feat[fields["start"]] +
	  ", end: " + feat[fields["end"]] +
	  ", strand: " + feat[fields["strand"]] +
	  ", label: " + feat[fields["name"]] +
	  ", ID: " + feat[fields["id"]]);

    // If feature is selected, deselect it.  Otherwise, select it.
    track.toggleSelection(elem);
};

/* If elem is selected, deselect it.  Otherwise, select it. 
 * (Right now, selection is fake--"selected" elements get a red border and become draggable, but
 * are not added to a selection list.  Should add to or remove from currentSelection.)
 */
DraggableFeatureTrack.prototype.toggleSelection = function(elem) {
    if (elem.style.border == "") {  // !! What if it had a border set by its style?
	elem.style.border = "3px solid red";
//	// Make it draggable  // Now happens elsewhere
//	this.makeDraggable(elem);
//        this.makeDroppable(elem);
    }
    // !! Else, need to take it off the "selected" list
    else 
	elem.style.border = "";
}

// Make this DraggableFeatureTrack draggable
DraggableFeatureTrack.prototype.makeDraggableAndDroppable = function(elem) {
        // Check whether we've already done it--look at class name and see if it includes "draggable-feature"
        if (DraggableFeatureTrack.prototype.hasString(elem.className, "draggable-feature")) {
//                console.log("makeDraggable: feature = " + elem.feature + ", className = " + elem.className + "--already has draggable-feature");
                return;
        }

    dojo.addClass(elem, "draggable-feature");
    console.log("makeDraggable: feature = " + elem.feature + ", className = " + elem.className);
    $(".draggable-feature").draggable({
	    helper:'clone',
		//	    containment: 'gridtrack',  // Need containment?  (Don't seem to)
            zindex: 200,
            opacity: 0.3,  // make the object semi-transparent when dragged
            axis: 'y'      // Allow only vertical dragging
    });
    // If a selected feature is being dragged, we'll handle the drag here--don't want it to go to the whole-canvas-drag handler.
    $(".draggable-feature").bind("mousedown", function(evt) {
//        $(".draggable-feature").draggable().trigger(evt); // ??
//        console.log("makeDraggable: evt.stopPropagation.  elem.feature = " + elem.feature + ", currentSelection = " + DraggableFeatureTrack.prototype.getCurrentSelection());  // DEL
	evt.stopPropagation();
    });

    var fields = this.fields;
    $("#track_Annotations").droppable({
       drop: function(ev, ui) {
//        console.log("makeDroppable: stopped dragging");  // DEL
        DraggableFeatureTrack.prototype.setDragging(false);
    	// Clone the dragged feature
    	var newAnnot=$(ui.draggable).clone();
//        console.log("makeDroppable: feat = " + elem.feature + ", currentSelection = " + DraggableFeatureTrack.prototype.getCurrentSelection());  // DEL
    	// Change its class to the appropriate annot class
    	DraggableFeatureTrack.prototype.setAnnotClassNameForFeature(newAnnot);

    	// Set vertical position of dropped item (left position is based on dragged feature)
    	newAnnot.css({"top": 0});
    	// Restore border of annot to its default (don't want selection border)
    	newAnnot.css({"border": null});

    	var track = this.track;
    	var features = this.track.features;
//    	var feat = elem.feature;
        var feat = DraggableFeatureTrack.prototype.getCurrentSelection();
    	var responseFeatures;
	
    	dojo.xhrPost( {
		// "http://10.0.1.24:8080/ApolloWeb/Login?username=foo&password=bar" to login
	    	postData: '{ "features": [{ "location": { "fmax": ' + feat[fields["end"]] + ', "fmin": ' + feat[fields["start"]] + ', "strand": ' + feat[fields["strand"]] + ' }, "type": { "cv": {"name": "SO"}, "name": "gene" }}], "operation": "add_feature" }',
	    	url: "/ApolloWeb/AnnotationEditorService",
	    	handleAs: "text",
	    	timeout: 5000, // Time in milliseconds
	    	// The LOAD function will be called on a successful response.
	    	load: function(response, ioArgs) { //
	    	console.log("API call worked!" + response)
	    	responseFeatures = eval('(' + response + ')').features;
	    	var featureArray = JSONUtils.prototype.convertJsonToFeatureArray(responseFeatures[0]);
	    	features.add(featureArray, responseFeatures[0].uniquename);
	    	track.hideAll();
	    	track.changed();
//	    	console.log("DFT: responseFeatures[0].uniquename = " + responseFeatures[0].uniquename);
	    },
	    // The ERROR function will be called in an error case.
	    error: function(response, ioArgs) { // 
	    	console.log("Error creating annotation--maybe you forgot to log into the server?");
	    	console.error("HTTP status code: ", ioArgs.xhr.status); //
	    	//dojo.byId("replace").innerHTML = 'Loading the ressource from the server did not work'; //  
	    	return response; // 
	    }
	    });

	    /*	    var xhrArgs = {
                url: "http://10.0.1.24:8080/ApolloWeb/AnnotationEditorService?jsessionid=66400E87DFA4801D75C87776194313AB",
                postData: "Some random text",
                handleAs: "text",
                load: function(data) {
		//dojo.byId("response2").innerHTML = "Message posted.";
		     console.log("It worked!");
                },
                error: function(error) {
                    //We'll 404 in the demo, but that's okay.  We don't have a 'postIt' service on the
                    //docs server.
                    //dojo.byId("response2").innerHTML = "Message posted.";
		    console.log("No dice!");
                }
            }
            //dojo.byId("response2").innerHTML = "Message being sent..."
            //Call the asynchronous xhrPost
            var deferred = dojo.xhrPost(xhrArgs);
	     */

//	console.log("itemDragged: " + newAnnot); //  + ", pos.left = " + pos.left + ", pos.top = " + pos.top + ", width = " + ui.draggable.width());
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


/* Change feature class name to annot class name */
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

Copyright (c) 2007-2010 The Evolutionary Software Foundation

Created by Mitchell Skinner <mitch_skinner@berkeley.edu>

This package and its accompanying libraries are free software; you can
redistribute it and/or modify it under the terms of the LGPL (either
version 2.1, or at your option, any later version) or the Artistic
License 2.0.  Refer to LICENSE for the full license text.

*/
