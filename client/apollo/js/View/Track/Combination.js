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

                // overriding configuration
                dojo.safeMixin(this.trackClasses, {
                    "set":  {
                        resultsTypes:   [{
                            name: "HTMLFeatures",
                            path: "JBrowse/View/Track/HTMLFeatures"
                        }
                        ],
                        store:        "WebApollo/Store/SeqFeature/Combination",
                        allowedOps:   ["&", "U", "X", "S"],
                        defaultOp :   "&"
                    },
                    "quantitative":        {
                        resultsTypes:   [{
                            name: "XYPlot",
                            path: "JBrowse/View/Track/Wiggle/XYPlot"
                        },
                            {
                                name: "Density",
                                path: "JBrowse/View/Track/Wiggle/Density"
                            }],
                        store:        "WebApollo/Store/SeqFeature/QuantitativeCombination",
                        allowedOps:   ["+", "-", "*", "/"],
                        defaultOp:    "+"
                    },
                    "mask": {
                        resultsTypes: [{
                            name: "XYPlot",
                            path: "JBrowse/View/Track/Wiggle/XYPlot"
                        },
                            {
                                name: "Density",
                                path: "JBrowse/View/Track/Wiggle/Density"
                            }],
                        store:          "WebApollo/Store/SeqFeature/Mask",
                        allowedOps: ["M", "N"],
                        defaultOp:      "M"
                    },
                    "BAM": {
                        resultsTypes: [{
                            name: "Detail",
                            path: "JBrowse/View/Track/Alignments2"
                        },
                            {
                                name: "Summary",
                                path: "JBrowse/View/Track/SNPCoverage"
                            }],
                        store:          "WebApollo/Store/SeqFeature/BAMCombination",
                        allowedOps: ["U"],
                        defaultOp:      "U"
                    }
                })
            }
        });
});
