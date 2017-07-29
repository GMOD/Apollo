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
    },

    requestGenotypes: function( f ) {
        var thisB = this;
        var ref = this.refSeq.name;
        var start = f.get('start');
        var end = f.get('end');

        var query = this._assembleQuery({ref: ref, start: start, end: end});
        var url = this._makeURL('feature/getGenotypes', query);
        return dojoRequest(url, {
            method: 'GET',
            handleAs: 'json'
        }).then(
            function(genotypes) {
                return genotypes;
            }
        );
    }

});

});
