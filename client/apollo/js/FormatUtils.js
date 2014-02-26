define( [ ],
function() {

	var FormatUtils = {};
	
	FormatUtils.formatDate = function(millis) {
		var date = new Date(millis);
		var year = date.getFullYear();
		var month = ("" + date.getMonth()).length == 1 ? "0" + date.getMonth() : date.getMonth();
		var day = date.getDate();
		return year + "-" + month + "-" + day;
	};
	
	return FormatUtils;
});