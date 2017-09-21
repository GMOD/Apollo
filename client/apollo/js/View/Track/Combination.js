define([
        'dojo/_base/declare',
        'dojo/_base/lang',
        'JBrowse/View/Track/Combination'
    ],
    function(
        declare,
        lang,
        CombinationTrack
    ) {

        return declare( CombinationTrack, {

            constructor: function() {
                // extending supported types
                dojo.safeMixin(this.supportedBy, {
                    "WebApollo/View/Track/DraggableHTMLFeatures": "set",
                    "WebApollo/View/Track/WebApolloCanvasFeatures": "set",
                    "WebApollo/View/Track/WebApolloHTMLVariants": "set",
                    "WebApollo/View/Track/WebApolloCanvasVariants": "set",
                    "WebApollo/View/Track/DraggableAlignments": "BAM",
                    "WebApollo/View/Track/RemoteDraggableHTMLFeatures": "set",
                    "WebApollo/View/Track/RemoteWebApolloCanvasFeatures": "set",
                    "WebApollo/View/Track/RemoteWebApolloHTMLVariants": "set",
                    "WebApollo/View/Track/RemoteWebApolloCanvasVariants": "set",
                    "WebApollo/View/Track/RemoteDraggableAlignments": "BAM",
                    "WebApollo/View/Track/RemoteAlignments2": "BAM",
                    "WebApollo/View/Track/RemoteSNPCoverage": "BAM",
                    "WebApollo/View/Track/RemoteFeatureCoverage": "BAM",
                    "WebApollo/View/Track/Wiggle/RemoteXYPlot": "quantitative",
                    "WebApollo/View/Track/Wiggle/RemoteDensity": "quantitative",
                    "WebApollo/Store/SeqFeature/Combination": "set"
                });
            }
        });
});
