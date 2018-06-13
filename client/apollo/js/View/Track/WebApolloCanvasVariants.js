define([
        'dojo/_base/declare',
        'JBrowse/View/Track/CanvasVariants'
    ],
    function(
        declare,
        CanvasVariants
    ) {

        return declare( [ CanvasVariants ], {

            constructor: function() {
                this.browser.getPlugin( 'WebApollo', dojo.hitch( this, function(p) {
                    this.webapollo = p;
                }));
            },

            _defaultConfig: function() {
                var config = dojo.clone(this.inherited(arguments));
                var thisB=this;
                config.menuTemplate.push(            {
                      "label" : "Create a new annotation",
                      "action":  function() {
                         var atrack=thisB.webapollo.getAnnotTrack();
                         atrack.createVariant({x1:{feature:this.feature}}, true);
                      }
                    }
                );
                return config;
            }
        });
});