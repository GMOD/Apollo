define([
        'dojo/_base/declare',
        'dojo/dom-construct',
        'JBrowse/View/Track/BlockBased',
        'JBrowse/Util'],
    function (declare,
              domConstruct,
              BlockBased,
              Util) {
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

                constructor: function (args) {//name, labelClass, posHeight) {
                    this.loaded = true;
                    this.labelClass = args.labelClass;
                    this.posHeight = args.posHeight;
                    this.height = Math.round(args.posHeight * 1.2);
                    // this.style.cssText = "top: 0px;height: 100%";
                },

                // this track has no track label or track menu, stub them out
                makeTrackLabel: function () {
                },
                makeTrackMenu: function () {
                },

                /**
                 *  Stub.
                 */
                startZoom: function (destScale, destStart, destEnd) {
                    this.clear();
                },

                /**
                 * Stub.
                 */
                endZoom: function (destScale, destBlockBases) {
                    this.clear();
                },

                renderGridlines: function(block,leftBase,rightBase,seq) {
                    var base_span = rightBase-leftBase;
                    var minor_count =
                        !( base_span % 20 ) ? 20 :
                            !( base_span % 10 ) ? 10 :
                                !( base_span % 5  ) ? 5  :
                                    !( base_span % 2  ) ? 2  :
                                        5; // can happen at weird zoom levels (i.e. 13)
                    var major_count = base_span == 20 ? 2 : base_span > 0 ? 1 : 0;

                    var new_gridline = function( glclass, position ) {
                        var gridline = document.createElement("div");
                        gridline.style.cssText = "left: " + position + "%; width: 10px; color: green; border: 5px solid blue;";
                        gridline.className = "gridline "+glclass;
                        return gridline;
                    };

                    // for( var i=0; i<minor_count; i++ ) {
                    //     var pos = 100/minor_count*i;
                    //     var cls = pos == 0 || (minor_count == 20 && i == 10)
                    //         ? "gridline_major"
                    //         : "gridline_minor";
                    //
                    //     block.domNode.appendChild( new_gridline( cls, pos) );
                    // }
                    // var child = new_gridline( "boundary_right", seq.end);
                    var child = new_gridline( "boundary_right", 7);
                    block.domNode.appendChild( child );

                },

                renderBoundary: function (args, projectedValue) {
                    var blockIndex = args.blockIndex;
                    var block = args.block;
                    var leftBase = args.leftBase;
                    var rightBase = args.rightBase;
                    var scale = args.scale;
                    var thisB = this;


                    // TODO: may need to pre-calculate offsets
                    var lastIndex = this.refSeq.name.lastIndexOf(":");
                    var nameString = this.refSeq.name.substring(0,lastIndex);
                    var projectionObject = JSON.parse(nameString);

                    // copy from BlockBased / Bookmark

                    var offset = 0 ;
                    for(var i in projectionObject.sequenceList){
                        var seq = projectionObject.sequenceList[i];
                        var start = seq.start ;
                        var end = seq.end ;
                        var length = end - start ;
                        // console.log("boundary to project: " +i + ": " + leftBase  + "-" + rightBase + " seq: "+ start + "-"+end);
                        this.renderGridlines(block,leftBase,rightBase,seq);
                    }

                },

                getApollo: function(){
                    return window.parent;
                },

                fillBlock: function (args) {
                    var blockIndex = args.blockIndex;
                    var block = args.block;
                    var leftBase = args.leftBase;
                    var rightBase = args.rightBase;
                    var scale = args.scale;
                    var thisB = this;

                    // find the number that is within 2 px of the left boundary of
                    // the block that ends with the most zeroes, or a 5 if no
                    // zeroes
                    var labelNumber = this.chooseLeftLabel(args);
                    var labelOffset = (leftBase + 1 - labelNumber) * scale / 10;

                    var posLabel = document.createElement("div");

                    var refSeqString = JSON.stringify(this.refSeq);
                    var projectedValue = this.getApollo().getOriginalProjection(refSeqString,labelNumber);
                    labelNumber = projectedValue.reverseValue ;

                    var filterNumber = -1 ;
                    if(projectedValue.sequence){
                        filterNumber = (projectedValue.sequence.end - labelNumber) * scale ;
                    }

                    if(labelNumber>=0 && (filterNumber<0 || filterNumber > 100) ){
                        var numtext = Util.addCommas(labelNumber);


                        var sequenceLabel = document.createElement("div");
                        sequenceLabel.innerHTML = projectedValue.sequence.name ;
                        sequenceLabel.style.display = "inline-block";
                        sequenceLabel.style.marginLeft  = "5px";
                        sequenceLabel.style.marginRight = "5px";

                        // give the position label a negative left offset in ex's to
                        // more-or-less center it over the left boundary of the block
                        // first is for the arrow

                        var arrowLabel = document.createElement("div");
                        arrowLabel.style.display = "inline-block";
                        // posLabel.appendChild(document.createTextNode("[ "));
                        if(projectedValue.sequence.reverse){
                            posLabel.appendChild(document.createTextNode(numtext));
                            arrowLabel.innerHTML = "&nbsp;&larr;";
                            posLabel.appendChild(arrowLabel);
                            posLabel.appendChild(sequenceLabel);
                        }
                        else{
                            posLabel.appendChild(document.createTextNode(numtext));
                            posLabel.appendChild(sequenceLabel);
                            arrowLabel.innerHTML = "&rarr;";
                            posLabel.appendChild(arrowLabel);
                        }
                        block.domNode.appendChild(posLabel);

                        // create a tick here
                        // we can just move around a certain number of ticks
                        // var gridLine = dojo.create('div',{ className: 'projectionGridline'}, this.browser.view.staticTrack.div);
                        // gridLine.style.display = 'block';
                        //
                        // var shiftValue = labelNumber * scale ;
                        // // alert(scale);
                        // // alert(shiftValue);
                        // gridLine.style.left = shiftValue + 'px';

                        // var new_gridline = function( glclass, position ) {
                        //     var gridline = document.createElement("div");
                        //     gridline.style.cssText = "left: " + position + "%; width: 10px";
                        //     gridline.className = "projectionGridline "+glclass;
                        //     return gridline;
                        // };
                        //
                        // block.domNode.appendChild( new_gridline('projectionGridline_major',20 ) ) ;
                        // block.domNode.appendChild( new_gridline('projectionGridline_minor',70 ) ) ;

                        // var gridTrackDiv = document.createElement("div");
                        // // gridTrackDiv.className = "track";
                        // gridTrackDiv.style.cssText = "left: 20%; width: 10px; display: block; top: 0px; height: 100%;";
                        // gridTrackDiv.className = "projectionGridline" ;

                        // this.browser.view.trackContainer.appendChild(gridTrackDiv);
                        // block.domNode.appendChild(gridTrackDiv);
                        // this.trackContainer.appendChild(gridTrackDiv);

                        // gridline.style.cssText = "left: 20%; width: 10px; display: block; className: 'projectionGridline'; ";
                        // // gridline.style.display = "block";
                        // // gridline.className = "projectionGridline" ;
                        // block.domNode.appendChild(gridline);
                        // var gridLine = dojo.create('div',{ className: 'projectionGridline'}, block.domNode );
                        // gridLine.style.cssText = "left: 20%; width: 10px; display: block; className: 'projectionGridline'; ";


                        // var scaleTrackPos = dojo.position( this.browser.view.scaleTrackDiv );
                        // gridLine.style.top =  scaleTrackPos.y + 'px';
                        // gridLine.style.top =  '50px';
                    }

                    // can we render a highlight for the start / end?
                    // this.renderBoundary(args,projectedValue);


                    var highlight = this.browser.getHighlight();
                    if (highlight && highlight.ref == this.refSeq.name) {
                        this.renderRegionHighlight(args, highlight);
                    }


                    var bookmarks = this.browser.getBookmarks();
                    if (bookmarks) {
                        this.renderRegionBookmark(args, bookmarks, this.refSeq.name, true);
                    }

                    // this.heightUpdate(Math.round(this.posHeight * 1.2), blockIndex);

                    args.finishCallback();
                },

                chooseLeftLabel: function (viewArgs) {
                    // if(true) return ;
                    var left = viewArgs.leftBase + 1;
                    var width = viewArgs.rightBase - left + 1;
                    var scale = viewArgs.scale;
                    for (var mod = 1000000; mod > 0; mod /= 10) {
                        if (left % mod * scale <= 3)
                            return left - left % mod;
                    }
                    return left;
                }

            });
    });
