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
                },

                // this track has no track label or track menu, stub them out
                makeTrackLabel: function () {
                },
                makeTrackMenu: function () {
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
                    for(i in projectionObject.sequenceList){
                        var seq = projectionObject.sequenceList[i];
                        var start = seq.start ;
                        var end = seq.end ;
                        var length = end - start ;
                        console.log("boundary to project: " +i + ": " + leftBase  + "-" + rightBase + " seq: "+ start + "-"+end);
                        this.renderGridlines(block,leftBase,rightBase,seq);
                    }

                },

                fillBlock: function (args) {
                    var blockIndex = args.blockIndex;
                    var block = args.block;
                    var leftBase = args.leftBase;
                    var scale = args.scale;
                    var thisB = this;

                    // find the number that is within 2 px of the left boundary of
                    // the block that ends with the most zeroes, or a 5 if no
                    // zeroes
                    var labelNumber = this.chooseLeftLabel(args);
                    var labelOffset = (leftBase + 1 - labelNumber) * scale / 10;
                    // console.log( leftBase+1, labelNumber, labelOffset );

                    var posLabel = document.createElement("div");

                    var projectedValue = window.parent.getReverseProjection(this.refSeq.name,labelNumber);
                    labelNumber = projectedValue.reverseValue ;

                    if(labelNumber>=0){
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
