define( [ ],
function() {

    var FormatUtils = {};
    
    FormatUtils.formatDate = function(millis) {
        var date = new Date(millis);
        var year = date.getFullYear();
        var month = date.getMonth() + 1;
        var day = ("" + date.getDate()).length == 1 ? "0" + date.getDate() : date.getDate();
        if (String(month).length == 1) {
            month = "0" + month;
        }
        return year + "-" + month + "-" + day;
    };
    
    FormatUtils.formatTime = function(millis) {
        var date = new Date(millis);
        var hours = date.getHours();
        var minutes = ("" + date.getMinutes()).length == 1 ? "0" + date.getMinutes() : date.getMinutes();
        return + hours + ":" + minutes;
    };
    
    return FormatUtils;
});
