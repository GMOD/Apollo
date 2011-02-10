function JSONUtils() {
}

// Convert JSON feature object from server into feature array (fa) for JBrowse.  fa[0] is an array of field definitions
// with each subsequent element being the data
JSONUtils.convertJsonToFeatureArray = function(jsonFeature) {
	var featureArray = new Array();
	featureArray[0] = jsonFeature.location.fmin;
	featureArray[1] = jsonFeature.location.fmax;
	featureArray[2] = jsonFeature.location.strand;
	featureArray[3] = jsonFeature.uniquename;
	return featureArray;
}

// Create a JSON object
JSONUtils.createJsonFeature = function(fmin, fmax, strand, cv, cvterm) {
	var feature = { "location": { "fmin": fmin, "fmax": fmax, "strand": strand }, "type": { "cv": {"name": cv }, "name": cvterm }};
	return feature;
}

/**
*  creates a feature in JBrowse JSON format
*  takes as arguments:
*      afeature: feature in ApolloEditorService JSON format,
*      fields: array specifying order of fields for JBrowse feature 
*      subfields:  array specifying order of fields for subfeatures of JBrowse feature 
*/
JSONUtils.createJBrowseFeature = function(afeature, fields, subfields)  {
    var jfeature = new Array();
    var loc = afeature.location;
    jfeature[fields["start"]] = loc.fmin;
    jfeature[fields["end"]] = loc.fmax;
    jfeature[fields["strand"]] = loc.strand;
    if (fields["id"])  { jfeature[fields["id"]] = afeature.uniquename; }
    if (fields["name"])  { jfeature[fields["name"]] = afeature.uniquename; }
    if (fields["type"])  { 
	var type = afeature.type.name;
	if (type == "exon")  { type = "UTR"; }
	jfeature[fields["type"]] = type;
    }
    if (fields["subfeatures"] && afeature.children)  {
	jfeature[fields["subfeatures"]] = new Array();
	for (var i in afeature.children)  {
	    var achild = afeature.children[i];
	    var jchild =  JSONUtils.createJBrowseFeature(achild, subfields, subfields);
	    jfeature[fields["subfeatures"]][i] =jchild;
	}
    }
    return jfeature;
}

/** 
*  creates a feature in ApolloEditorService JSON format
*  takes as argument:
*       jfeature: a feature in JBrowse JSON format, 
*       fields: array specifying order of fields in jfeature
*       subfields: array specifying order of fields in subfeatures of jfeature
*       specified_type (optional): type passed in that overrides type info for jfeature
*  ApolloEditorService format:
*    { 
*       "location" : { "fmin": fmin, "fmax": fmax, "strand": strand }, 
*       "type": { "cv": { "name":, cv },   // typical cv name: "SO" (Sequence Ontology)
*                 "name": cvterm },        // typical name: "transcript"
*       "children": { __recursive ApolloEditorService feature__ }
*    }
* 
*   For ApolloEditorService "add_feature" call to work, need to have "gene" as toplevel feature, 
*         then "transcript", then ???
*                 
*    JBrowse JSON fields example: ["start", "end", "strand", "id", "subfeatures"]
*
*    type handling
*    if specified_type arg present, it determines type name
*    else if fields has a "type" field, use that to determine type name
*    else don't include type 
*
*    ignoring JBrowse ID / name fields for now
*    currently, for features with lazy-loaded children, ignores children 
*/
JSONUtils.createApolloFeature = function(jfeature, fields, subfields, specified_type)   {
    var afeature = new Object();
    afeature.location = { "fmin": jfeature[fields["start"]], "fmax": jfeature[fields["end"]], "strand": jfeature[fields["strand"]] };

    var typename;
    if (specified_type)  { typename = specified_type; }
    else if (fields["type"])  { typename = jfeature[fields["type"]]; }
    if (typename)  {
	afeature.type = { "cv": { "name": "SO" }};
	afeature.type.name = typename;
    }
    if (fields["subfeatures"])  {
	var subfeats = jfeature[fields["subfeatures"]];
	// GAH TODO: get this working for lazy-loaded subfeatures 
	// lazy-loaded subfeatures will only have single-valued index into LazyArray, rather than an actual array
	//    currently detecting for this and bailing on children if no array found
	if (subfeats && subfeats.length > 0 && (subfeats[0] instanceof Array))  {
	    afeature.children = new Array();
	    //	for (var i=0; i<subfeats.length; i++)  {
	    for (i in subfeats) {
		var subfeat = subfeats[i];
		if (subfields)  {
		    // afeature.children[i] = JSONUtils.createApolloFeature(subfeat, subfields); 
		    afeature.children[i] = JSONUtils.createApolloFeature(subfeat, subfields, subfields, "exon"); 
		}
		else  {
		    afeature.children[i] = JSONUtils.createApolloFeature(subfeat, fields, fields); 
		}
	    }
	}
    }
    return afeature;
}

/*
JSONUtils.createJBrowseFeature = function(apollo_feature, fields)  {
    
}
*/

/*
*  takes a feature from a source_track and returns equivalent feature for a target_track, 
*       based on inspection of feature fields and subfields
*  only doing data conversion:
*       returned feature is _not_ added to target_track (if desired, must do elsewhere)
*       no rendering or div creation
*
*  GAH TODO:  need to make this more generic 
*       maybe loop through all fields of newfeat, populate any that have corresponding fields in feat, 
*          if no corresponding field then set to null (or undefined?)
*/
JSONUtils.convertToTrack = function(feat, source_track, target_track)  {
    var newfeat = new Array();
    var source_fields = source_track.fields;
    var source_subfields = source_track.subFields;
    var target_fields = target_track.fields;
    var target_subfields = target_track.subFields;
    
    newfeat[target_fields["start"]] = feat[source_fields["start"]];
    newfeat[target_fields["end"]] = feat[source_fields["end"]];
    newfeat[target_fields["strand"]] = feat[source_fields["strand"]];
    if (target_fields["id"])  { newfeat[target_fields["id"]] = feat[source_fields["id"]]; }
    if (target_fields["name"])  { newfeat[target_fields["name"]] = feat[source_fields["id"]]; } // assign ID to name
    if (target_fields["subfeatures"] && source_fields["subfeatures"])   { 
	var newsubfeats = new Array();
	var subfeats = feat[source_fields["subfeatures"]];
	if (subfeats)  {
	    for (var i in subfeats)  {
		var oldsub = subfeats[i];
		var newsub = new Array();
		newsub[target_subfields["start"]] = oldsub[source_subfields["start"]];
		newsub[target_subfields["end"]] = oldsub[source_subfields["end"]];
		newsub[target_subfields["strand"]] = oldsub[source_subfields["strand"]];
		newsub[target_subfields["type"]] = oldsub[source_subfields["type"]];
		newsubfeats[i] = newsub;
	    }
	}
	newfeat[target_fields["subfeatures"]]  = newsubfeats;
    }
    return newfeat;
}
