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
*  creates a feature in ApolloEditorService JSON format
*  takes as argument a feature in JBrowse JSON format, and an array specifying order of fields
*  ApoloEditorService format:
*    { 
*       "location" : { "fmin": fmin, "fmax": fmax, "strand": strand }, 
*       "type": { "cv": { "name":, cv },   // typical cv name: "SO" (Sequence Ontology)
*                 "name": cvterm },        // typical name: "transcript"
*       "children": { __recursive ApolloEditorService feature__ }
*    }
*                 
*    fields example: ["start", "end", "strand", "id", "subfeatures"]
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
    console.log("subfeatures: " + fields["subfeatures"]);
    if (fields["subfeatures"])  {
	console.log("found subfeatures field");
	var subfeats = jfeature[fields["subfeatures"]];
	console.log(subfeats);
	// GAH TODO: get this working for lazy-loaded subfeatures 
	// lazy-loaded subfeatures will only have single-valued index into LazyArray, rather than an actual array
	//    currently detecting for this and bailing on children if no array found
	if (subfeats && subfeats.length > 0 && (subfeats[0] instanceof Array))  {
	    afeature.children = new Array();
	    //	for (var i=0; i<subfeats.length; i++)  {
	    for (i in subfeats) {
		var subfeat = subfeats[i];
		if (subfields)  {
		    afeature.children[i] = JSONUtils.createApolloFeature(subfeat, subfields); 
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