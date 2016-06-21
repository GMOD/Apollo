define( [
         'dojo/_base/declare',
         'WebApollo/View/Track/DraggableHTMLFeatures',
		 'jquery'
         ],
	function(declare, DraggableFeatureTrack,$) {
	    
	    var DraggableBLASTFeatures = declare(DraggableFeatureTrack, {   
		    constructor: function(args)  {  },
		    
		    bitscore_color: function (bitscore) {
			var colors = ["#5e5e5e","#53739c","#508f62","#845e93","#bd4939"];
			var thresholds = [72,104,136,168,200];
			for (var i = 0; i < colors.length - 1 && bitscore >= thresholds[i]; i++) {}
			return colors[i];
		    },
		    
		    renderSubfeature: function(feature, featDiv, subfeature, displayStart, displayEnd, block)  {
			var subfeatdiv = this.inherited(arguments);
			var bitscore = parseInt(subfeature.get('bitscore'));
			if (subfeatdiv && bitscore)  {  // just in case subFeatDiv doesn't actually get created
			    var $subfeatdiv = $(subfeatdiv);
			    $subfeatdiv.css("background-color", this.bitscore_color(bitscore));
			}
			return subfeatdiv;
		    }
		});
	    
	    return DraggableBLASTFeatures;
	});