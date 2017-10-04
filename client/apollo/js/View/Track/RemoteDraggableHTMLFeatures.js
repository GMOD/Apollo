define([
        'dojo/_base/declare',
        'dojo/dom-construct',
        'dojo/_base/array',
        'JBrowse/Util',
        'WebApollo/JSONUtils',
        'WebApollo/View/Track/DraggableHTMLFeatures',
        'WebApollo/View/Track/_RemoteHTMLMixin'
    ],
    function(
        declare,
        domConstruct,
        array,
        Util,
        JSONUtils,
        DraggableHTMLFeatureTrack,
        RemoteHTMLMixin
    ) {

        return declare( [ DraggableHTMLFeatureTrack, RemoteHTMLMixin ], {

        });

});