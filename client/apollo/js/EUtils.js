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
    var thisB = this;
    dojo.xhrGet( {
        url: this.url + "&operation=search&db=" + db + "&id=" + id,
        handleAs: "json",
        timeout: 5000 * 1000, // Time in milliseconds
        sync: true,
        load: function(response, ioArgs) {
            valid = !response.eSearchResult.ErrorList;
        }, 
        error: function(response, ioArgs) {
            thisB.errorHandler(response);
        }
    });
    return valid;
};

EUtils.prototype.fetch = function(db, id) {
    var record;
    var thisB = this;
    dojo.xhrGet( {
        url: this.url + "&operation=fetch&db=" + db + "&id=" + id,
        handleAs: "json",
        timeout: 5000 * 1000, // Time in milliseconds
        sync: true,
        load: function(response, ioArgs) {
            record = response.PubmedArticleSet.PubmedArticle ? response : null;
        }, 
        error: function(response, ioArgs) {
            thisB.errorHandler(response);
        }
    });
    return record;
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
