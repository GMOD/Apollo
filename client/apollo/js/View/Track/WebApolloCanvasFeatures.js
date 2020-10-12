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
                      console.log('add track this way',thisB);
                      var official = atrack.getApollo().isOfficialTrack(thisB.key);
                     atrack.createAnnotations({x1:{feature:this.feature}},true,official);
                  }
                },
                {
                  "label" : "pseudogene",
                  "action" : function() {
                      var atrack=thisB.webapollo.getAnnotTrack();
                      var official = atrack.getApollo().isOfficialTrack(thisB.key);
                     atrack.createGenericAnnotations([this.feature], "transcript", null, "pseudogene",official);
                  }
                },
                {
                  "label" : "tRNA",
                  "action" : function() {
                      var atrack=thisB.webapollo.getAnnotTrack();
                      var official = atrack.getApollo().isOfficialTrack(thisB.key);
                     atrack.createGenericAnnotations([this.feature], "tRNA", null, "gene",official);
                   }
                },
                {
                  "label" : "snRNA",
                  "action" : function() {
                      var atrack=thisB.webapollo.getAnnotTrack();
                      var official = atrack.getApollo().isOfficialTrack(thisB.key);
                     atrack.createGenericAnnotations([this.feature], "snRNA", null, "gene",official);
                   }
                },
                {
                  "label" : "snoRNA",
                  "action" : function() {
                      var atrack=thisB.webapollo.getAnnotTrack();
                      var official = atrack.getApollo().isOfficialTrack(thisB.key);
                     atrack.createGenericAnnotations([this.feature], "snoRNA", null, "gene",official);
                   }
                },
                {
                  "label" : "ncRNA",
                  "action" : function() {
                      var atrack=thisB.webapollo.getAnnotTrack();
                      var official = atrack.getApollo().isOfficialTrack(thisB.key);
                     atrack.createGenericAnnotations([this.feature], "ncRNA", null, "gene",official);
                   }
                },
                {
                  "label" : "rRNA",
                  "action" : function() {
                      var atrack=thisB.webapollo.getAnnotTrack();
                      var official = atrack.getApollo().isOfficialTrack(thisB.key);
                     atrack.createGenericAnnotations([this.feature], "rRNA", null, "gene",official);
                   }
                },
                {
                  "label" : "miRNA",
                  "action" : function() {
                      var atrack=thisB.webapollo.getAnnotTrack();
                      var official = atrack.getApollo().isOfficialTrack(thisB.key);
                     atrack.createGenericAnnotations([this.feature], "miRNA", null, "gene",official);
                   }
                },
                  {
                      "label": "guide_RNA",
                      "action": function(event) {
                          var atrack = thisB.webapollo.getAnnotTrack();
                          atrack.createGenericAnnotations([this.feature],"guide_RNA",null,"gene");
                      }
                  },
                  {
                      "label": "RNase_MRP_RNA",
                      "action": function(event) {
                          var atrack = thisB.webapollo.getAnnotTrack();
                          atrack.createGenericAnnotations([this.feature],"RNase_MRP_RNA",null,"gene");
                      }
                  },
                  {
                      "label": "telomerase_RNA",
                      "action": function(event) {
                          var atrack = thisB.webapollo.getAnnotTrack();
                          atrack.createGenericAnnotations([this.feature],"telomerase_RNA",null,"gene");
                      }
                  },
                  {
                      "label": "SRP_RNA",
                      "action": function(event) {
                          var atrack = thisB.webapollo.getAnnotTrack();
                          atrack.createGenericAnnotations([this.feature],"SRP_RNA",null,"gene");
                      }
                  },
                  {
                      "label": "lnc_RNA",
                      "action": function(event) {
                          var atrack = thisB.webapollo.getAnnotTrack();
                          atrack.createGenericAnnotations([this.feature],"lnc_RNA",null,"gene");
                      }
                  },
                  {
                      "label": "RNase_P_RNA",
                      "action": function(event) {
                          var atrack = thisB.webapollo.getAnnotTrack();
                          atrack.createGenericAnnotations([this.feature],"RNase_P_RNA",null,"gene");
                      }
                  },
                  {
                      "label": "scRNA",
                      "action": function(event) {
                          var atrack = thisB.webapollo.getAnnotTrack();
                          atrack.createGenericAnnotations([this.feature],"scRNA",null,"gene");
                      }
                  },
                  {
                      "label": "piRNA",
                      "action": function(event) {
                          var atrack = thisB.webapollo.getAnnotTrack();
                          atrack.createGenericAnnotations([this.feature],"piRNA",null,"gene");
                      }
                  },
                  {
                      "label": "tmRNA",
                      "action": function(event) {
                          var atrack = thisB.webapollo.getAnnotTrack();
                          atrack.createGenericAnnotations([this.feature],"tmRNA",null,"gene");
                      }
                  },
                  {
                      "label": "enzymatic_RNA",
                      "action": function(event) {
                          var atrack = thisB.webapollo.getAnnotTrack();
                          atrack.createGenericAnnotations([this.feature],"enzymatic_RNA",null,"gene");
                      }
                  },
                  {
                      "label" : "Repeat region",
                      "action" : function() {
                          var atrack=thisB.webapollo.getAnnotTrack();
                          var official = atrack.getApollo().isOfficialTrack(thisB.key);
                          atrack.createGenericOneLevelAnnotations([this.feature], "repeat_region", true,official);
                      }
                  },
                  {
                  "label" : "Terminator",
                  "action" : function() {
                      var atrack=thisB.webapollo.getAnnotTrack();
                      var official = atrack.getApollo().isOfficialTrack(thisB.key);
                      atrack.createGenericOneLevelAnnotations([this.feature], "terminator", true,official);
                  }
                },
              // {
              //     "label" : "Shine Dalgarno sequence",
              //     "action" : function() {
              //         var atrack=thisB.webapollo.getAnnotTrack();
              //         atrack.createGenericOneLevelAnnotations([this.feature], "Shine_Dalgarno_sequence", true);
              //     }
              // },
                {
                  "label" : "Transposable element",
                  "action" : function() {
                      var atrack=thisB.webapollo.getAnnotTrack();
                      var official = atrack.getApollo().isOfficialTrack(thisB.key);
                     atrack.createGenericOneLevelAnnotations([this.feature], "transposable_element", true,official);
                   }
                }
              ]
            }
        );
        return config;
    }


});

});

