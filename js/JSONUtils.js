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