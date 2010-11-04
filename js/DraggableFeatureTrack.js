/*  Subclass of FeatureTrack that allows features to be dragged and dropped into the annotation track to create annotations. */
function DraggableFeatureTrack(trackMeta, url, refSeq, browserParams) {
    FeatureTrack.call(this, trackMeta, url, refSeq, browserParams);
    thisObj = this;
}

// Inherit from FeatureTrack
DraggableFeatureTrack.prototype = new FeatureTrack();

DraggableFeatureTrack.prototype.onFeatureClick = function(event) {
    var track = thisObj;
    event = event || window.event;
    if (event.shiftKey) return;
    var elem = (event.currentTarget || event.srcElement);
    //depending on bubbling, we might get the subfeature here
    //instead of the parent feature
    if (!elem.feature) elem = elem.parentElement;
    if (!elem.feature) return; //shouldn't happen; just bail if it does

    var fields = thisObj.fields;
//    console.log("DFT.onFeatureClick: fields = " + fields + ", fields[end] = " + fields["end"]);

    var feat = elem.feature;
    console.log("DFT: user clicked on draggablefeature\nstart: " + feat[fields["start"]] +
	  ", end: " + feat[fields["end"]] +
	  ", strand: " + feat[fields["strand"]] +
	  ", label: " + feat[fields["name"]] +
	  ", ID: " + feat[fields["id"]]);

    // If feature is selected, deselect it.  Otherwise, select it.
    track.toggleSelection(elem);
};

/* If elem is selected, deselect it.  Otherwise, select it. 
 * (Right now, selection is fake--"selected" elements get a red border and become draggable, but
 * are not added to a selection list.)
 */
DraggableFeatureTrack.prototype.toggleSelection = function(elem) {
    if (elem.style.border == "") {  // !! What if it had a border set by its style?
	elem.style.border = "3px solid red";
	// Make it draggable
	this.makeDraggable(elem);
        this.makeDroppable(elem);
    }
    // !! Else, need to take it off the "selected" list
    else 
	elem.style.border = "";

//    console.log("toggleSelection: now elem.style.border = " + elem.style.border);
}

// Make this DraggableFeatureTrack draggable
DraggableFeatureTrack.prototype.makeDraggable = function(elem) {
    dojo.addClass(elem, "draggable-feature");
//    console.log("makeDraggable: elem = " + elem + ", elem.className = " + elem.className + ", elem.id = " + elem.id);  // DEL
    $(".draggable-feature").draggable({
	    helper:'clone',
		//	    containment: 'gridtrack',  // Need containment?  (Don't seem to)
            zindex: 200,
            opacity: 0.3,  // make the object semi-transparent when dragged
            axis: 'y'      // Allow only vertical dragging
    });
    // If a selected feature is being dragged, we'll handle the drag here--don't want it to go to the whole-canvas-drag handler.
    $(".draggable-feature").bind("mousedown", function(evt) {
	evt.stopPropagation();
    });
}

/* Make dragged feature droppable in Annot row */
DraggableFeatureTrack.prototype.makeDroppable = function(elem) {
    var fields = this.fields;
	
    $("#track_Annotations").droppable({
       drop: function(ev, ui) {
    	var pos = $(ui.helper).offset();  // Need?
    	// Clone the dragged feature
    	var newAnnot=$(ui.draggable).clone();
    	// Change its class to the appropriate annot class
    	DraggableFeatureTrack.prototype.setAnnotClassNameForFeature(newAnnot);

    	// Set vertical position of dropped item (left position is based on dragged feature)
    	newAnnot.css({"top": 0});
    	// Restore border of annot to its default (don't want selection border)
    	newAnnot.css({"border": null});

        // This was what we did before actual creation of annotation object on server--don't need anymore
    	// Find the right block to put the new annot in
//     	var startField = fields["start"];      	// The field that specifies the start base of this feature
//  	console.log("Before calling findBlock, startfield = " + fields["start"]);
//    	var block = DraggableFeatureTrack.prototype.findBlock($(ui.draggable)[0], $(this).children(), startField);
//    	newAnnot.appendTo(block); 

    	var track = this.track;
    	var features = this.track.features;
    	var feat = elem.feature;
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
	    	console.log("DFT: responseFeatures[0].uniquename = " + responseFeatures[0].uniquename);
	    },
	    // The ERROR function will be called in an error case.
	    error: function(response, ioArgs) { // 
	    	console.log("API didn't work!")
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
//	    console.log("newAnnot: " + newAnnot);
//	    console.log("pos: " + pos);
    }
    });
}

/* Find and return the block that this feature goes in */
/* (No longer needed) */
// DraggableFeatureTrack.prototype.findBlock = function(feature, blocks, startField) {
//     var feat = feature.feature;
//     var startBase = feat[startField];
//     // !! What if it's on minus strand?  (Seems ok)
//     // !! Can we safely skip the first few blocks, since they're always blank?
//     for (i = 0; i < blocks.length; i++) {
// //            console.log("Block " + i + ": end = " + blocks[i].endBase);
//             if (startBase <= blocks[i].endBase) {
//                     console.log("Feature with startBase=" + startBase + " goes in block " + i + ", which has endBase=" + blocks[i].endBase);
//                     return blocks[i];
//             }
//     }
// }

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
