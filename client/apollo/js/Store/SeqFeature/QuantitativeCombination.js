define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/Deferred',
        'JBrowse/Store/SeqFeature/QuantitativeCombination',
        'WebApollo/Store/SeqFeature/_RemoteCombinationMixin'
    ],
    function(
        declare,
        array,
        Deferred,
        QuantitativeCombinationStore,
        RemoteCombinationMixin
    ) {
        return declare([ QuantitativeCombinationStore, RemoteCombinationMixin ], {

        });
    });