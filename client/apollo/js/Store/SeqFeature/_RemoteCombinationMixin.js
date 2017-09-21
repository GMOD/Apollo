define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/Deferred',
        'dojo/when',
        'dojo/promise/all',
        'JBrowse/Util',
        'WebApollo/JSONUtils'
    ],
    function(
        declare,
        array,
        Deferred,
        when,
        all,
        Util,
        JSONUtils
    ) {

        // Helper object that wraps a feature and which store it comes from
        var featureWrapper = Util.fastDeclare(
            {
                get: function( arg ) {
                    return this.feature.get(arg);
                },

                id: function() {
                    return this.feature.id()+this.storeName;
                },

                parent: function() {
                    return this.feature.parent();
                },

                children: function() {
                    return this.feature.children();
                },

                tags: function() {
                    return this.feature.tags();
                },

                constructor: function( feat, storeName ) {
                    this.feature = feat;
                    this.storeName = storeName;
                    this.source = feat ? feat.source : undefined;
                }
            }
        );

        return declare( null, {

            _getFeatures: function( query, featCallback, doneCallback, errorCallback ) {
                var thisB = this;
                if(this.stores.length == 1) {
                    this.stores[0].getFeatures( query, featCallback, doneCallback, errorCallback);
                    return;
                }

                if(this.regionLoaded) {
                    var spans = array.filter(this.regionLoaded.spans, function(span) {
                        return span.start <= query.end && span.end >= query.start;
                    });
                    var features = this.createFeatures(spans);
                    this.finish(features, spans, featCallback, doneCallback);
                    return;
                }

                // featureArrays will be a map from the names of the stores to an array of each store's features
                var featureArrays = {};

                var sequenceList = JSONUtils.parseSequenceList(query.ref);
                if (sequenceList[0].reverse) {
                    errorCallback(JSONUtils.NOT_YET_SUPPORTED_MESSAGE);
                }
                else {
                    query.ref = sequenceList[0].name;
                    // Generate map
                    var fetchAllFeatures = thisB.stores.map(
                        function (store) {
                            var d = new Deferred();
                            if ( !featureArrays[store.name] ) {
                                featureArrays[store.name] = [];
                                store.getFeatures(
                                    query,
                                    dojo.hitch( this, function( feature ) {
                                        var feat = new featureWrapper( feature, store.name );
                                        featureArrays[store.name].push( feat );
                                    }),
                                    function(){d.resolve( featureArrays[store.name] ); },
                                    function(){d.reject("Error fetching features for store " + store.name);}
                                );
                            } else {
                                d.resolve(featureArrays[store.name], true);
                            }
                            d.then(function(){}, errorCallback); // Makes sure that none of the rejected deferred promises keep propagating
                            return d.promise;
                        }
                    );

                    // Once we have all features, combine them according to the operation tree and create new features based on them.
                    when( all( fetchAllFeatures ), function() {
                        // Create a set of spans based on the evaluation of the operation tree
                        var spans = thisB.evalTree(featureArrays, thisB.opTree, query);
                        var features = thisB.createFeatures(spans);
                        thisB.finish(features, spans, featCallback, doneCallback);
                    }, errorCallback);
                }
            }
        });


    });