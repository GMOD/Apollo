define([
        'dojo/_base/declare',
        'dojo/dom-construct',
        'JBrowse/View/Track/BlockBased',
        'JBrowse/Util'],
    function (declare,
              dom,
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

                fillBlock: function (args) {
                    var blockIndex = args.blockIndex;
                    var block = args.block;
                    var leftBase = args.leftBase;
                    var scale = args.scale;
                    var thisB = this;

                    // find the number that is within 2 px of the left boundary of
                    // the block that ends with the most zeroes, or a 5 if no
                    // zeroes
                    var labelNumber = this.chooseLabel(args);
                    var labelOffset = (leftBase + 1 - labelNumber) * scale / 10;
                    // console.log( leftBase+1, labelNumber, labelOffset );

                    var posLabel = document.createElement("div");

                    var projectedValue = window.parent.getReverseProjection(this.refSeq.name,labelNumber);
                    labelNumber = projectedValue.reverseValue ;

                    if(labelNumber>=0){
                        var numtext = Util.addCommas(labelNumber);
                        posLabel.className = this.labelClass;

                        // give the position label a negative left offset in ex's to
                        // more-or-less center it over the left boundary of the block
                        posLabel.style.left = "-" + Number(numtext.length) / 1.7 + labelOffset + "ex";

                        var sequenceLabel = document.createElement("div");
                        sequenceLabel.innerHTML = projectedValue.sequence.name ;
                        sequenceLabel.style.display = "inline-block";
                        sequenceLabel.style.marginLeft  = "5px";
                        sequenceLabel.style.marginRight = "5px";

                        var arrowLabel = document.createElement("div");
                        arrowLabel.style.display = "inline-block";
                        if(projectedValue.sequence.reverse){
                            arrowLabel.innerHTML = "&larr;&nbsp;";
                            posLabel.appendChild(arrowLabel);
                            posLabel.appendChild(document.createTextNode(numtext));
                            posLabel.appendChild(sequenceLabel);
                        }
                        else{
                            arrowLabel.innerHTML = "&nbsp;&rarr;";
                            posLabel.appendChild(document.createTextNode(numtext));
                            posLabel.appendChild(sequenceLabel);
                            posLabel.appendChild(arrowLabel);
                        }
                        block.domNode.appendChild(posLabel);
                    }

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

                chooseLabel: function (viewArgs) {
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
