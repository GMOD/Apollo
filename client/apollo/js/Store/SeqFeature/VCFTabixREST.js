define([
            'dojo/_base/declare',
            'dojo/_base/lang',
            'dojo/_base/array',
            'dojo/io-query',
            'dojo/request',
            'dojo/Deferred',
            'JBrowse/Store/LRUCache',
            'JBrowse/Store/SeqFeature/REST',
            'JBrowse/Util'
        ],
        function(
            declare,
            lang,
            array,
            ioquery,
            dojoRequest,
            Deferred,
            LRUCache,
            REST,
            Util
        ) {

return declare( REST,
{

    getVCFHeader: function() {
        var thisB = this;
        var query = this._assembleQuery({});
        var url = this._makeURL('stats/getVcfHeader', query);

        return dojoRequest(url, {
            method: 'GET',
            handleAs: 'json'
        }).then(
            function(header) {
                return header;
            }
        );
    }

});

});
