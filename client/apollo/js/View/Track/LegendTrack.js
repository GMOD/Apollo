define([
           'dojo/_base/declare',
           'dojo/dom-construct',
           'JBrowse/View/Track/BlockBased',
           'JBrowse/Util'],
       function(
           declare,
           dom,
           BlockBased,
           Util
       ) {
return declare(BlockBased,
 /**
  * @lends JBrowse.View.Track.LocationScale.prototype
  */
{

    /**
     * This track is for (e.g.) position and sequence information that should
     * always stay visible at the top of the view.
     * @constructs
     */

    constructor: function( args ) {//name, labelClass, posHeight) {
		console.log(args);
        this.loaded = true;
        this.labelClass = args.labelClass;
        this.posHeight = args.posHeight;
        this.height = Math.round( args.posHeight * 1.2 );
    },

    // this track has no track label or track menu, stub them out
    makeTrackLabel: function() {},
    makeTrackMenu: function() {},

    fillBlock: function( args ) {
        console.log(args);
        var blockIndex = args.blockIndex;
        var block = args.block;
        var leftBase = args.leftBase;
        var rightBase = args.rightBase;
        var scale = args.scale;
        var thisB = this;

        // find the number that is within 2 px of the left boundary of
        // the block that ends with the most zeroes, or a 5 if no
        // zeroes
        var labelNumber = this.chooseLabel( args );
        var labelOffset = (leftBase+1-labelNumber)*scale/10;
        // console.log( leftBase+1, labelNumber, labelOffset );

        var posLabel = document.createElement("div");
        var numtext = Util.addCommas( labelNumber );
        posLabel.className = this.labelClass;

        // give the position label a negative left offset in ex's to
        // more-or-less center it over the left boundary of the block
        posLabel.style.left = "-" + Number(numtext.length)/1.7 + labelOffset + "ex";

        posLabel.appendChild( document.createTextNode( numtext ) );
        block.domNode.appendChild(posLabel);

        var highlight = this.browser.getHighlight();
        if( highlight && highlight.ref == this.refSeq.name ) {
            this.renderRegionHighlight( args, highlight );
        }


        var bookmarks = this.browser.getBookmarks();
        if( bookmarks ) {
            this.renderRegionBookmark( args, bookmarks, this.refSeq.name, true );
        }

        var curTrack = this;

        var featCallback = dojo.hitch(this,function( feature ) {
            var uniqueId = feature.id();
            //if( ! this._featureIsRendered( uniqueId ) ) {
            //    /* feature render, adding to block, centering refactored into addFeatureToBlock() */
            //    // var filter = this.browser.view.featureFilter;
            //    if( this.filterFeature( feature ) )  {
            //        this.addFeatureToBlock( feature, uniqueId, block, scale, labelScale, descriptionScale,
            //            containerStart, containerEnd );
                //}
            //}
        });
        //
        this.store.getFeatures( { ref: this.refSeq.name,
                start: leftBase,
                end: rightBase
            },
            featCallback,
            function ( args ) {
                console.log(args);
                //curTrack.heightUpdate(curTrack._getLayout(scale).getTotalHeight(),
                //    blockIndex);
                if ( args && args.maskingSpans ) {
                    //note: spans have to be inverted
                    var invSpan = [];
                    invSpan[0] = { start: leftBase };
                    var i = 0;
                    for ( var span in args.maskingSpans) {
                        if (args.maskingSpans.hasOwnProperty(span)) {
                            span = args.maskingSpans[span];
                            invSpan[i].end = span.start;
                            i++;
                            invSpan[i] = { start: span.end };
                        }
                    }
                    invSpan[i].end = rightBase;
                    if (invSpan[i].end <= invSpan[i].start) {
                        invSpan.splice(i,1); }
                    if (invSpan[0].end <= invSpan[0].start) {
                        invSpan.splice(0,1); }
                    curTrack.maskBySpans( invSpan, args.maskingSpans );
                }
                //finishCallback();
            },
            function( error ) {
                console.error( error, error.stack );
                curTrack.fillBlockError( blockIndex, block, error );
                finishCallback();
            }
        );

        this.heightUpdate( Math.round( this.posHeight*1.2 ), blockIndex);
        //args.finishCallback();
    },

    _defaultConfig: function () {
        var thisConfig = this.inherited(arguments);
        thisConfig.menuTemplate = null;
        thisConfig.noExport = true;  // turn off default "Save track data" "
        //thisConfig.style.centerChildrenVertically = false;
        thisConfig.pinned = true;
        return thisConfig;
    },

    chooseLabel: function( viewArgs ) {
        var left = viewArgs.leftBase + 1;
        var width = viewArgs.rightBase - left + 1;
        var scale = viewArgs.scale;
        //return 7 ;
        for( var mod = 1000000; mod > 0; mod /= 10 ) {
            if( left % mod * scale <= 3 )
                return left - left%mod;
        }
        return left;
    }

});
});
