define( [ ],
function() {

	var FormatUtils = {};
	
	FormatUtils.formatDate = function(millis) {
		var date = new Date(millis);
		var year = date.getFullYear();
		var month = ("" + date.getMonth()).length == 1 ? "0" + date.getMonth() : date.getMonth();
		var day = ("" + date.getDate()).length == 1 ? "0" + date.getDate() : date.getDate();
		return year + "-" + month + "-" + day;
	};
	
	FormatUtils.formatTime = function(millis) {
		var date = new Date(millis);
		var hours = date.getHours();
		var minutes = date.getMinutes();
		return + hours + ":" + minutes;
	};
	
	return FormatUtils;
});