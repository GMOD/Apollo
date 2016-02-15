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
            FeatureSelectionManager,
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
        console.log("WA config");
        var config= this.inherited(arguments);
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
                     atrack.createGenericAnnotations([this.feature], "pseudogene", null, "gene");
                  }
                },
                {
                  "label" : "repeat_region",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "repeat_region", null, "gene");
                   }
                },
                {
                  "label" : "transposable element",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "transposable_element", null, "gene");
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
                  "label" : "miRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "miRNA", null, "gene");
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
                  "label" : "snoRNA",
                  "action" : function() {
                     var atrack=thisB.webapollo.getAnnotTrack();
                     atrack.createGenericAnnotations([this.feature], "snoRNA", null, "gene");
                   }
                }
              ]
            }
        );
        return config;
    }


});

});

