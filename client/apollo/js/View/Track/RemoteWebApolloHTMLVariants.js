define([
        'dojo/_base/declare',
        'dojo/dom-construct',
        'dojo/_base/array',
        'JBrowse/Util',
        'WebApollo/JSONUtils',
        'WebApollo/View/Track/WebApolloHTMLVariants',
        'WebApollo/View/Track/_RemoteHTMLMixin'
    ],

    function(
        declare,
        domConstruct,
        array,
        Util,
        JSONUtils,
        WebApolloHTMLVariants,
        RemoteHTMLMixin
    ) {

        return declare( [ WebApolloHTMLVariants, RemoteHTMLMixin ], {

        });
});