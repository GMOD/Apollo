define( [
            'dojo/_base/declare',
            'dojo/_base/array',
            'JBrowse/View/Track/CanvasFeatures',
            'dijit/Menu',
            'dijit/MenuItem',
            'dijit/CheckedMenuItem',
            'dijit/MenuSeparator',
            'dijit/PopupMenuItem',
            'dijit/Dialog',
            'JBrowse/Util',
            'JBrowse/Model/SimpleFeature',
            'WebApollo/View/Projection/FASTA',
            'WebApollo/SequenceOntologyUtils'
        ],
        function( declare,
            array,
            CanvasFeaturesTrack,
            dijitMenu,
            dijitMenuItem,
            dijitCheckedMenuItem,
            dijitMenuSeparator,
            dijitPopupMenuItem,
            dijitDialog,
            Util,
            SimpleFeature,
            FASTAView,
            SeqOnto )
{

return declare( CanvasFeaturesTrack,

{
    constructor: function() {
        this.browser.getPlugin( 'WebApollo', dojo.hitch( this, function(p) {
            this.webapollo = p;
        }));
    },
    _defaultConfig: function() {
        console.log("WA config",document.body);
        var config = Util.deepUpdate(dojo.clone(this.inherited(arguments)),
            {
                style: {
                    textColor: function() { return dojo.hasClass(document.body,'Dark') ?'white': 'black'; },
                    text2Color: function() { return dojo.hasClass(document.body,'Dark')? 'LightSteelBlue': 'blue'; },
                    connectorColor: function() { return dojo.hasClass(document.body,'Dark')? 'lightgrey': 'black'; },
                    color: function() { return dojo.hasClass(document.body,'Dark')? 'orange': 'goldenrod'; }
                }
            });
        var thisB=this;
        config.menuTemplate.push(            {
              "label" : "Create new annotation",
              "children" : [
                {
                  "label" : "gene",
                  "action":  function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createAnnotations({x1:{feature:this.feature}});
                  }
                },
                {
                  "label" : "pseudogene",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "transcript", null, "pseudogene");
                  }
                },
                {
                  "label" : "tRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "tRNA", null, "gene");
                   }
                },
                {
                  "label" : "snRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "snRNA", null, "gene");
                   }
                },
                {
                  "label" : "snoRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "snoRNA", null, "gene");
                   }
                },
                {
                  "label" : "ncRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "ncRNA", null, "gene");
                   }
                },
                {
                  "label" : "rRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "rRNA", null, "gene");
                   }
                },
                {
                  "label" : "miRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "miRNA", null, "gene");
                   }
                },
                {
                  "label" : "repeat_region",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericOneLevelAnnotations([this.feature], "repeat_region", true);
                   }
                },
                {
                  "label" : "transposable element",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericOneLevelAnnotations([this.feature], "transposable_element", true);
                   }
                }
              ]
            }
        );
        return config;
    },

    _renderUnderlyingReferenceSequence: function( track, f, featDiv, container ) {
        // render the sequence underlying this feature if possible
        var field_container = dojo.create('div', { className: 'field_container feature_sequence' }, container );
        dojo.create( 'h2', { className: 'field feature_sequence', innerHTML: 'Region sequence', title: 'reference sequence underlying this '+(f.get('type') || 'feature') }, field_container );
        var valueContainerID = 'feature_sequence'+this._uniqID();
        var valueContainer = dojo.create(
            'div', {
                id: valueContainerID,
                innerHTML: '<div style="height: 12em">Loading...</div>',
                className: 'value feature_sequence'
            }, field_container);
        var maxSize = this.config.maxFeatureSizeForUnderlyingRefSeq;
        if( maxSize < (f.get('end') - f.get('start')) ) {
            valueContainer.innerHTML = 'Not displaying underlying reference sequence, feature is longer than maximum of '+Util.humanReadableNumber(maxSize)+'bp';
        } else {
            track.browser.getStore('refseqs', dojo.hitch(this,function( refSeqStore ) {
                valueContainer = dojo.byId(valueContainerID) || valueContainer;
                if( refSeqStore ) {
                    refSeqStore.getReferenceSequence(
                        {
                            ref: this.refSeq.name,
                            start: f.get('start'),
                            end: f.get('end')
                        },
                        // feature callback
                        dojo.hitch( this, function( seq ) {
                            valueContainer = dojo.byId(valueContainerID) || valueContainer;
                            valueContainer.innerHTML = '';
                            // the HTML is rewritten by the dojo dialog
                            // parser, but this callback may be called either
                            // before or after that happens.  if the fetch by
                            // ID fails, we have come back before the parse.
                            var textArea = new FASTAView({ track: this, width: 62, htmlMaxRows: 10 })
                                .renderHTML(
                                    { ref:   this.refSeq.name,
                                        start: f.get('start'),
                                        end:   f.get('end'),
                                        strand: f.get('strand'),
                                        type: f.get('type')
                                    },
                                    f.get('strand') == -1 ? Util.revcom(seq) : seq,
                                    valueContainer
                                );
                        }),
                        // end callback
                        function() {},
                        // error callback
                        dojo.hitch( this, function() {
                            valueContainer = dojo.byId(valueContainerID) || valueContainer;
                            valueContainer.innerHTML = '<span class="ghosted">reference sequence not available</span>';
                        })
                    );
                } else {
                    valueContainer.innerHTML = '<span class="ghosted">reference sequence not available</span>';
                }
            }));
        }
    }

});

});

