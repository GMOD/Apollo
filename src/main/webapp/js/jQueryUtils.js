function jQueryUtils() {

};

jQueryUtils.getURLParameter = function(parameter) {
	var results = new RegExp('[\\?&]' + parameter + '=([^&#]*)').exec(window.location.href);
	if (results && results[1]) {
		return decodeURIComponent(results[1]);
	}
	return null;
}