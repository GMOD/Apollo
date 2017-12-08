define([
        'dojo/_base/declare',
        'dojo/dom-construct',
        'dojo/_base/array',
        'dojo/Deferred',
        'JBrowse/Util',
        'WebApollo/JSONUtils',
        'WebApollo/View/Track/WebApolloCanvasFeatures',
        'WebApollo/View/Track/_RemoteCanvasMixin'
    ],
    function(
        declare,
        domConstruct,
        array,
        Deferred,
        Util,
        JSONUtils,
        WebApolloCanvasFeaturesTrack,
        RemoteCanvasMixin
    ) {

        return declare( [ WebApolloCanvasFeaturesTrack, RemoteCanvasMixin ], {

        });
});