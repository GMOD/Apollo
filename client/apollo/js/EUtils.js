define( [
            'dojo/_base/declare'
], 
function( declare ) {

function EUtils(contextPath, errorHandler) {
	this.url = contextPath + "/ProxyService?proxy=eutils";
	this.errorHandler = errorHandler;
};

EUtils.prototype.validateId = function(db, id) {
	var valid;
	dojo.xhrGet( {
		url: this.url + "&db=" + db + "&id=" + id,
		handleAs: "json",
		timeout: 5000 * 1000, // Time in milliseconds
		sync: true,
		load: function(response, ioArgs) {
			valid = !response.eSearchResult.ErrorList;
		}, 
		error: function(response, ioArgs) {
			errorHandler(response);
		}
	});
	return valid;
};

return EUtils;

});
	
/*
define( [
            'dojo/_base/declare',
            'dojo/io/script'
], 
function( declare, dojoScript ) {

var eutilsUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi"; //?retmode=xml&";

function EUtils() {
};

EUtils.validateId = function(db, id, success, error) {
	dojoScript.get({
		url: "http://jsonproxy.appspot.com/proxy",
		callbackParamName: "callback",
		content: {
			url: eutilsUrl + "?db=" + db + "&term=" + id + "[uid]",
		},
		load: function(response) {
			response.eSearchResult.ErrorList ? error("Invalid " + db + " ID: " + id) : success();
		}
	});

};

return EUtils;
	
});

*/