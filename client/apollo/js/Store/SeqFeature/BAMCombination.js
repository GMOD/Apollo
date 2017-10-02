define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/Deferred',
        'JBrowse/Store/SeqFeature/BAMCombination',
        'WebApollo/Store/SeqFeature/_RemoteCombinationMixin'
    ],
    function(
        declare,
        array,
        Deferred,
        BAMCombinationStore,
        RemoteCombinationMixin
    ) {
        return declare([ BAMCombinationStore, RemoteCombinationMixin ], {

        });
    });