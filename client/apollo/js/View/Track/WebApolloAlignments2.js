define( [
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/promise/all',
        'dijit/Menu',
        'dijit/MenuItem',
        'dijit/CheckedMenuItem',
        'dijit/MenuSeparator',
        'dijit/PopupMenuItem',
        'dijit/Dialog',
        'JBrowse/Util',
        'JBrowse/View/Track/Alignments2'
    ],
    function(
        declare,
        array,
        all,
        dijitMenu,
        dijitMenuItem,
        dijitCheckedMenuItem,
        dijitMenuSeparator,
        dijitPopupMenuItem,
        dijitDialog,
        Util,
        Alignments2
    ) {
        return declare(Alignments2, {

            /**
             * An extension to JBrowse/View/Track/Alignments2 to allow for creating annotations
             * from read alignments.
             */

            constructor: function() {
                this.browser.getPlugin('WebApollo', dojo.hitch(this, function(plugin) {
                    this.webapollo = plugin;
                }));
            },

            _defaultConfig: function() {
                var thisB = this;
                var config = this.inherited(arguments);

                config.menuTemplate.push({
                    "label": "Create new annotation",
                    "children": [
                        {
                            "label": "gene",
                            "action":  function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createAnnotations({selection: {feature: this.feature}},true);
                            }
                        },
                        {
                            "label": "pseudogene",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "transcript", null, "pseudogene");
                            }
                        },
                        {
                            "label": "tRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "tRNA", null, "gene");
                            }
                        },
                        {
                            "label": "snRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "snRNA", null, "gene");
                            }
                        },
                        {
                            "label": "snoRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "snoRNA", null, "gene");
                            }
                        },
                        {
                            "label": "ncRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "ncRNA", null, "gene");
                            }
                        },
                        {
                            "label": "rRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "rRNA", null, "gene");
                            }
                        },
                        {
                            "label": "miRNA",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericAnnotations([this.feature], "miRNA", null, "gene");
                            }
                        },
                        {
                            "label": "repeat_region",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericOneLevelAnnotations([this.feature], "repeat_region", true);
                            }
                        },
                        {
                            "label": "transposable_element",
                            "action": function() {
                                var atrack = thisB.webapollo.getAnnotTrack();
                                atrack.createGenericOneLevelAnnotations([this.feature], "transposable_element", true);
                            }
                        }
                    ]
                });
                return config;
            }
        });
    }
);