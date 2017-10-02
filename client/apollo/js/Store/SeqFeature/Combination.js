define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/Deferred',
        'JBrowse/Store/SeqFeature/Combination',
        'WebApollo/Store/SeqFeature/_RemoteCombinationMixin'
    ],
    function(
        declare,
        array,
        Deferred,
        CombinationStore,
        RemoteCombinationMixin
    ) {
        return declare([ CombinationStore, RemoteCombinationMixin ], {

        });
});