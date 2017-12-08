define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/Deferred',
        'dojo/when',
        'dojo/promise/all',
        'JBrowse/Store/SeqFeature/Mask',
        'WebApollo/ProjectionUtils'
    ],
    function(
        declare,
        array,
        Deferred,
        when,
        all,
        MaskStore,
        ProjectionUtils
    ) {
        return declare( MaskStore, {

            getFeatures: function( query, featCallback, doneCallback, errorCallback ) {
                var thisB = this;

                var sequenceList = ProjectionUtils.parseSequenceList(query.ref);
                if (sequenceList[0].reverse) {
                    errorCallback(ProjectionUtils.NOT_YET_SUPPORTED_MESSAGE)
                }
                else {
                    query.ref = sequenceList[0].name;
                    this.gotAllStores.then(
                        function() {
                            var featureArray = {};

                            // Get features from one particular store
                            var grabFeats = function( key )  {
                                var d = new Deferred( );
                                featureArray[key] = [];

                                thisB.stores[key].getFeatures( query,
                                    function(feature) {
                                        featureArray[key].push( feature );
                                    },
                                    function() { d.resolve( true ); },
                                    function() { d.reject( "failed to load features for " + key + " store" ); }
                                );
                                return d.promise;
                            };

                            when(all([grabFeats( "mask" ), grabFeats( "display" )]),
                                function() {
                                    // Convert mask features into simplified spans
                                    var spans = thisB.toSpans( featureArray.mask, query );
                                    // invert masking spans if necessary
                                    spans = thisB.inverse ? thisB.notSpan( spans, query ) : spans;
                                    var features = featureArray.display;

                                    thisB.maskFeatures( features, spans, featCallback, doneCallback );
                                }, errorCallback
                            );
                        },
                        errorCallback
                    );
                }
            }
        });
    });