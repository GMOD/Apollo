/*  Subclass of FeatureTrack that allows features to be dragged and dropped into the annotation track to create annotations. */
function DraggableFeatureTrack(trackMeta, url, refSeq, browserParams) {
    FeatureTrack.call(this, trackMeta, url, refSeq, browserParams);
}

// Inherit from FeatureTrack
DraggableFeatureTrack.prototype = new FeatureTrack();


/* ?? Do I need all this stuff if it's mostly the same as in FeatureTrack?  Maybe some of it could be broken out into other methods in FeatureTrack
 * so I can just subclass the method(s) I need. */
DraggableFeatureTrack.prototype.loadSuccess = function(trackInfo) {
    var startTime = new Date().getTime();
    this.count = trackInfo.featureCount;
    this.fields = {};
    for (var i = 0; i < trackInfo.headers.length; i++) {
	this.fields[trackInfo.headers[i]] = i;
    }
//    console.log("this.fields[start] = " + this.fields["start"]);
    this.subFields = {};
    if (trackInfo.subfeatureHeaders) {
        for (var i = 0; i < trackInfo.subfeatureHeaders.length; i++) {
            this.subFields[trackInfo.subfeatureHeaders[i]] = i;
        }
    }
    this.features.importExisting(trackInfo.featureNCList,
                                 trackInfo.sublistIndex,
                                 trackInfo.lazyIndex,
                                 this.baseUrl,
                                 trackInfo.lazyfeatureUrlTemplate);
    if (trackInfo.subfeatureArray)
        this.subfeatureArray = new LazyArray(trackInfo.subfeatureArray);

    this.histScale = 4 * (trackInfo.featureCount / this.refSeq.length);
    this.labelScale = 50 * (trackInfo.featureCount / this.refSeq.length);
    this.subfeatureScale = 80 * (trackInfo.featureCount / this.refSeq.length);
    this.className = trackInfo.className;
    this.subfeatureClasses = trackInfo.subfeatureClasses;
    this.arrowheadClass = trackInfo.arrowheadClass;
    this.urlTemplate = trackInfo.urlTemplate;
    this.histogramMeta = trackInfo.histogramMeta;
    for (var i = 0; i < this.histogramMeta.length; i++) {
        this.histogramMeta[i].lazyArray =
            new LazyArray(this.histogramMeta[i].arrayParams);
    }
    this.histStats = trackInfo.histStats;
    this.histBinBases = trackInfo.histBinBases;

    if (trackInfo.clientConfig) {
        var cc = trackInfo.clientConfig;
        var density = trackInfo.featureCount / this.refSeq.length;
        this.histScale = (cc.histScale ? cc.histScale : 4) * density;
        this.labelScale = (cc.labelScale ? cc.labelScale : 50) * density;
        this.subfeatureScale = (cc.subfeatureScale ? cc.subfeatureScale : 80)
                                   * density;
        if (cc.featureCss) this.featureCss = cc.featureCss;
        if (cc.histCss) this.histCss = cc.histCss;
        if (cc.featureCallback) this.featureCallback = cc.featureCallback;
    }

    //console.log((new Date().getTime() - startTime) / 1000);

    var fields = this.fields;
    var track = this;
//    if (! trackInfo.urlTemplate) {
        // !! This function should be pulled out as a separate function in FeatureTrack--then I could just overload
        // that function and not this whole method.
        this.onFeatureClick = function(event) {
            event = event || window.event;
	    if (event.shiftKey) return;
	    var elem = (event.currentTarget || event.srcElement);
            //depending on bubbling, we might get the subfeature here
            //instead of the parent feature
            if (!elem.feature) elem = elem.parentElement;
            if (!elem.feature) return; //shouldn't happen; just bail if it does
            var feat = elem.feature;
	    console.log("clicked on feature: start: " + feat[fields["start"]] +
	          ", end: " + feat[fields["end"]] +
	          ", strand: " + feat[fields["strand"]] +
	          ", label: " + feat[fields["name"]] +
	          ", ID: " + feat[fields["id"]]);
            // If it's selected, deselect it.  Otherwise, select it.
	    track.toggleSelection(elem);
        };
//    }

    this.setLoaded();
};

/* If elem is selected, deselect it.  Otherwise, select it. 
 * (Right now, selection is fake--"selected" elements get a red border and become draggable, but
 * are not added to a selection list.)
 */
DraggableFeatureTrack.prototype.toggleSelection = function(elem) {
    if (elem.style.border == "") {  // !! What if it had a border set by its style?
	elem.style.border = "3px solid red";
	// Make it draggable
//        DraggableFeatureTrack.prototype.makeDraggable(elem);
		this.makeDraggable(elem);
//        DraggableFeatureTrack.prototype.makeDroppable(elem);
        this.makeDroppable(elem);
    }
    // !! Else, take it off the "selected" list
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
		//	    containment: 'gridtrack',  // Need containment?
            zindex: 200,
            opacity: 0.3,  // make the object semi-transparent when dragged
            axis: 'y'      // Allow only vertical dragging
    });
    // If a selected feature is being dragged, we'll handle the drag--don't want it to go to the whole-canvas-drag handler.
    $(".draggable-feature").bind("mousedown", function(evt) {
	evt.stopPropagation();
    });
}

/* Make dragged feature droppable in Annot row */
DraggableFeatureTrack.prototype.makeDroppable = function(elem) {
	
	var fields = this.fields;
	
    $("#track_Annotations").droppable({
       drop: function(ev, ui) {

    	var feat = elem.feature;
    	
    	var pos = $(ui.helper).offset();
    	// Clone the dragged feature
    	var newAnnot=$(ui.draggable).clone();
    	// Change its class
    	DraggableFeatureTrack.prototype.setAnnotClassNameForFeature(newAnnot);

    	// Set vertical position of dropped item (left position is based on dragged feature)
    	newAnnot.css({"top": 0});
    	// Restore border of annot to its default (don't want selection border)
    	newAnnot.css({"border": null});
    	// Find the right block to put the new annot in
    	// The field that specifies the start base of this feature
    	var startField = 0;  // hardcode for now
//  	var track = $(this).track;  // doesn't work
//  	var fields = track.fields();
//  	console.log("Before calling findBlock, this.track.fields[start] = " + fields["start"]);
    	var block = DraggableFeatureTrack.prototype.findBlock($(ui.draggable)[0], $(this).children(), startField);
    	newAnnot.appendTo(block);

    	var responseFeatures;
            
	    dojo.xhrPost( {
		// "http://10.0.1.24:8080/ApolloWeb/Login?username=foo&password=bar" to login
	    	postData: '{ "features": [{ "location": { "fmax": ' + feat[fields["end"]] + ', "fmin": ' + feat[fields["start"]] + ', "strand": ' + feat[fields["strand"]] + ' }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "' + feat[fields["name"]] + '" }], "operation": "add_feature" }',
	    	url: "/ApolloWeb/AnnotationEditorService",
	    	handleAs: "text",
	    	timeout: 5000, // Time in milliseconds
	    	// The LOAD function will be called on a successful response.
	    	load: function(response, ioArgs) { //
	    	console.log("API call worked!" + response)
	    	responseFeatures = eval('(' + response + ')').features;
	    	console.log(responseFeatures[0].uniquename);
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

	    console.log("itemDragged: " + newAnnot + ", pos.left = " + pos.left + ", pos.top = " + pos.top + ", width = " + ui.draggable.width());

	    console.log(newAnnot);
	    console.log(pos);
    }
    });
}

/* Find and return the block that this feature goes in */
DraggableFeatureTrack.prototype.findBlock = function(feature, blocks, startField) {
    var feat = feature.feature;
    var startBase = feat[startField];
    // !! What if it's on minus strand?  (Seems ok)
    // !! Can we safely skip the first few blocks, since they're always blank?
    for (i = 0; i < blocks.length; i++) {
//            console.log("Block " + i + ": end = " + blocks[i].endBase);
            if (startBase <= blocks[i].endBase) {
                    console.log("Feature with startBase=" + startBase + " goes in block " + i + ", which has endBase=" + blocks[i].endBase);
                    return blocks[i];
            }
    }
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
