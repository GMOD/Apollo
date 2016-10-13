define([
        'dojo/_base/declare',
        'dojo/dom-construct',
        'JBrowse/View/Track/BlockBased'
    ],
    function( declare, dom, BlockBased ) {
        return dojo.declare( BlockBased,
            /**
             * @lends JBrowse.View.Track.GridLines.prototype
             */
            {

                /**
                 * This track draws vertical gridlines, which are divs with height
                 * 100%, absolutely positioned at the very top of all the tracks.
                 * @constructs
                 * @extends JBrowse.View.Track.BlockBased
                 */
                constructor: function( args ) {
                    this.loaded = true;
                    this.name = 'gridlines';
                },

                // this track has no track label or track menu, stub them out
                makeTrackLabel: function() {},
                makeTrackMenu: function() {},

                fillBlock: function( args ) {
                    this.renderGridlines( args.block, args.leftBase, args.rightBase );

                    var highlight = this.browser.getHighlight();
                    if( highlight && highlight.ref == this.refSeq.name )
                        this.renderRegionHighlight( args, highlight );


                    var bookmarks = this.browser.getBookmarks();
                    if( bookmarks ) {
                        this.renderRegionBookmark( args, bookmarks );
                    }

                    args.finishCallback();
                    this.heightUpdate(100, args.blockIndex);
                },

                renderGridlines: function(block,leftBase,rightBase) {

                    var base_span = rightBase-leftBase;
                    // var minor_count =
                    //     !( base_span % 20 ) ? 20 :
                    //         !( base_span % 10 ) ? 10 :
                    //             !( base_span % 5  ) ? 5  :
                    //                 !( base_span % 2  ) ? 2  :
                    //                     5; // can happen at weird zoom levels (i.e. 13)
                    // var major_count = base_span == 20 ? 2 : base_span > 0 ? 1 : 0;

                    var new_gridline_left = function( borderObject ) {
                        var gridline = document.createElement("div");
                        // TODO: put all of the projectionsequence info here
                        gridline.appendChild(document.createTextNode("Left"));
                        gridline.style.cssText = "left: " + borderObject.position + "%; width: 0px; border: 5px red solid;";
                        gridline.className = "gridline ";
                        return gridline;
                    };

                    var new_gridline_right = function( borderObject ) {
                        var gridline = document.createElement("div");
                        // TODO: put all of the projectionsequence info here
                        gridline.appendChild(document.createTextNode("Right"));
                        gridline.style.cssText = "left: " + borderObject.position + "%; width: 0px; border: 5px blue solid;";
                        gridline.className = "gridline ";
                        return gridline;
                    };

                    var lastIndex = this.refSeq.name.lastIndexOf(":");
                    var nameString = this.refSeq.name.substring(0,lastIndex);
                    var projectionObject = JSON.parse(nameString);

                    var offset = 0 ;
                    for(i in projectionObject.sequenceList){
                        var seq = projectionObject.sequenceList[i];
                        var start = seq.start ;
                        var end = seq.end ;
                        var length = end - start ;
                        console.log("boundary to project: " +i + ": " + leftBase  + "-" + rightBase + " seq: "+ start + "-"+end);

                        // this.renderGridlines(block,leftBase,rightBase,seq);
                        // need to calculate the start% from left to right

                        // 1 - determine if a boundary exists in this area
                        // leftBase is the projected value
                        var borders = window.parent.getBorders(this.refSeq.name,leftBase,rightBase);
                        console.log("borders returned: "+borders.length);
                        for(var index in borders){
                            var border = borders[index];
                            if(border.type=="left"){
                                block.domNode.appendChild( new_gridline_left( border ) );
                            }
                            else{
                                block.domNode.appendChild( new_gridline_right( border ) );
                            }
                        }

                        // 2 - calculate the percentage for that region
                        // 3 - render that boundary correctly

                        // block.domNode.appendChild( new_gridline( "boundary_left", start) );
                        // block.domNode.appendChild( new_gridline( "boundary_left", end) );
                    }


                    // block.domNode.appendChild( new_gridline_left( "boundary_left", 7) );
                    // block.domNode.appendChild( new_gridline_right( "boundary_right", 93) );

                }
            });
    });

