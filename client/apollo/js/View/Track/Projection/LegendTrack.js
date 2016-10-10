define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/_base/lang',
        'dojo/_base/event',
        'dojo/dom-construct',
        'dojo/dom-style',
        'JBrowse/View/Track/SVGTrackSimpleBase',
        'JBrowse/View/Track/SVG/SVGLayerCoords',
        'JBrowse/View/Track/SVG/SVGLayerBpSpace',
        'JBrowse/View/Track/SVG/SVGLayerPxSpace',
        'WebApollo/JSONUtils'
],
    function (
        declare,
        array,
        lang,
        domEvent,
        domConstruct,
        domStyle,
        SVGTrackBase,
        SVGLayerCoords,
        SVGLayerBpSpace,
        SVGLayerPxSpace,
        JSONUtils
    ) {

        return declare(
            [ SVGTrackBase ]
            ,
            /**
             * @lends JBrowse.View.Track.LocationScale.prototype
             */
            {

                /**
                 * This track is for (e.g.) position and sequence information that should
                 * always stay visible at the top of the view.
                 * @constructs
                 */

                // name, labelClass, posHeight
                constructor: function (args) {
                    console.log(args);
                    this.loaded = true;
                    this.labelClass = args.labelClass;
                    this.pinned = true;
                    this.posHeight = 30 ;
                    this.height = 30 ;
                    this.noCache = true ;
                },

                // this track has no track label or track menu, stub them out
                makeTrackLabel: function () {
                },
                makeTrackMenu: function () {
                },

                _trackMenuOptions: function(){
                },

                _defaultConfig: function () {
                    var thisConfig = this.inherited(arguments);
                    thisConfig.menuTemplate = null;
                    thisConfig.noExport = true;  // turn off default "Save track data" "
                    //thisConfig.style.centerChildrenVertically = false;
                    thisConfig.pinned = true;
                    thisConfig.noCache = true;
                    return thisConfig;
                },
                heightUpdate: function( height, blockIndex ) {
                    //console.log("SVGFeatures::heightUpdate("+height+")");
                    //console.dir(arguments);
                    //var err = new Error();
                    //console.log(err.stack);

                    this.inherited( arguments );
                    if( this.svgSpace){
                        //this.svgCanvas.height = this.svgCanvas.offsetHeight;
                        console.log('has height ');
                        this.svgSpace.height = 30;
                    }
                    else{
                        console.log('has no space ');
                        //this.svgCanvas.height = 200 ;
                    }
                    this.height = 30;
                },

                setViewInfo: function( genomeView, heightUpdate, numBlocks, trackDiv, widthPct, widthPx, scale ) {
                    console.log("SVGLollipop::setViewInfo");
                    console.log(numBlocks+" "+widthPct+" "+widthPx+" "+scale);

                    this.inherited( arguments );

                    //this.svgCoords = new SVGLayerCoords(this);
                    //this.svgCoords.setViewInfo( genomeView, heightUpdate, numBlocks, trackDiv, widthPct, widthPx, scale );

                    this.svgSpace = new SVGLayerPxSpace(this);      // px-space svg layer
                    //this.svgSpace = new SVGLayerBpSpace(this);    // bp-space svg layer
                    this.svgSpace.setViewInfo( genomeView, heightUpdate, numBlocks, trackDiv, widthPct, widthPx, scale );

                },

                showRange: function(first, last, startBase, bpPerBlock, scale, containerStart, containerEnd) {
                    console.log("SVGLollipop::showRange");
                    console.log(first+" "+last+" "+startBase+" "+bpPerBlock+" "+scale+" "+containerStart+" "+containerEnd);

                    // used to delete
                    //for (var bpCoord in this.svgSpace.svgCanvas.fItem) {
                    //    console.log(this.svgSpace.svgCanvas.fItem[bpCoord].id);
                    //    var svgId = this.svgSpace.svgCanvas.fItem[bpCoord].id;
                    //    if(svgId.startsWith('L3')){
                    //        this.svgSpace.svgCanvas.fItem[bpCoord].setAttribute("display","none");
                    //    }
                    //}

                    this.displayContext = {
                        first: first,
                        last: last,
                        startBase: startBase,
                        bpPerBlock: bpPerBlock,
                        scale: scale,
                        containerStart: containerStart,
                        containerEnd: containerEnd
                    };

                    this.svgScale = scale;


                    this.inherited(arguments);      // call the superclass's showRange

                    //this.svgCoords.showRange(first, last, startBase, bpPerBlock, scale, containerStart, containerEnd);
                    this.svgSpace.showRange(first, last, startBase, bpPerBlock, scale, containerStart, containerEnd);

                },
                /*
                 id = unique string of object
                 bpCoord = basepair coordinate of object
                 width = width of object
                 height = height of object
                 callback = function that returns object

                 */
                addSVGObject: function(id,bpCoord,width,height,callback) {

                    this.svgSpace.addSVGObject(id,bpCoord,width,height,callback);
                },
                fixId: function(val) {
                    return val.replace(",", "-");
                },
                computeHeight: function() {
                    return this.svgSpace.getHeight();
                },
                fillFeatures: function( args ) {

                    this.inherited(arguments);      // call the superclass's
                },

                //bp2Native: function(val) {
                //    return (val - this.svgParent.displayContext.startBase) * this.svgParent.displayContext.scale;
                //},

                renderGrid: function(context,fRect,offsetMultiplier){
                    var thisB = this;
                    // create svg element new
                    var feature = fRect.f;

                    // draw line
                    var svgSpace = this.svgSpace;

                    // compute the x coord given the bpCoord
                    var bpCoord = feature.get("start");
                    var label = feature.get("label");
                    var color = feature.get("color");
                    var cx = svgSpace.bp2Native(bpCoord);
                    var len = (feature.get("end") - feature.get("start") ) * 0.18 ;
                    len = svgSpace.getHeight() - len;
                    console.log("bpCoord="+bpCoord+" cx="+cx+" len="+len+" scale="+this.svgScale);

                    // draw stems
                    var id = "L-"+this.fixId(fRect.f.id());

                    this.addSVGObject(id,bpCoord,100,100,function () {
                        var svgItem = document.createElementNS('http://www.w3.org/2000/svg','line');
                        svgItem.setAttribute('x1',0);
                        //svgItem.setAttribute('y1',len);
                        svgItem.setAttribute('y1',40);
                        svgItem.setAttribute('x2',0);
                        //svgItem.setAttribute('y2',svgSpace.getHeight());
                        svgItem.setAttribute('y2',50);
                        svgItem.setAttribute('stroke','rgba(255,0,0,.5)');
                        svgItem.setAttribute('stroke-width',2);
                        svgItem.setAttribute('stroke-linecap','round');
                        return svgItem;
                    });

                    var id3 = "L3-"+this.fixId(fRect.f.id());
                    this.addSVGObject(id3,bpCoord,100,100,function () {
                        var svgItem = document.createElementNS('http://www.w3.org/2000/svg','line');
                        svgItem.setAttribute('x1',0);
                        //svgItem.setAttribute('y1',len);
                        svgItem.setAttribute('y1',0);
                        svgItem.setAttribute('x2',0);
                        //svgItem.setAttribute('y2',svgSpace.getHeight());
                        svgItem.setAttribute('y2',10);
                        svgItem.setAttribute('stroke','rgba(255,0,0,.5)');
                        svgItem.setAttribute('stroke-width',2);
                        svgItem.setAttribute('stroke-linecap','round');
                        return svgItem;
                    });


                    // draw delicious candy
                    var id2 = "C-"+this.fixId(fRect.f.id());

                    console.log("cx="+cx+" color="+color);
                    this.addSVGObject(id2,bpCoord,100,100,function () {
                        var apple = document.createElementNS('http://www.w3.org/2000/svg','text');
                        var xlength = 3 ; // for 0 case only
                        var formattedLabel = JSONUtils.numberWithCommas(label);
                        if(label!='0'){
                            xlength = - (formattedLabel.length-1) * offsetMultiplier ;
                        }
                        apple.setAttribute('x',xlength);
                        apple.setAttribute('y','30');
                        apple.setAttribute('fill',color);
                        apple.setAttribute('display','block');
                        apple.innerHTML =  formattedLabel;
                        return apple;
                    });
                },

                renderRegion: function(context,fRect){
                    var thisB = this;
                    // create svg element new
                    var feature = fRect.f;

                    // draw line
                    var svgSpace = this.svgSpace;

                    // compute the x coord given the bpCoord
                    var bpCoord = feature.get("start");
                    var label = feature.get("label");
                    var color = feature.get("color");
                    var cx = svgSpace.bp2Native(bpCoord);
                    var start = feature.get("start");
                    var end = feature.get("end");
                    var len = (end - start ) * 0.18 ;
                    len = svgSpace.getHeight() - len;
                    console.log("bpCoord="+bpCoord+" cx="+cx+" len="+len+" scale="+this.svgScale);
                    console.log("rendering region "+label + " from "+ start + " to "+end) ;

                    // draw stems
                    var id = "R-"+this.fixId(fRect.f.id());

                    this.addSVGObject(id,bpCoord,100,100,function () {
                        var svgItem = document.createElementNS('http://www.w3.org/2000/svg','rect');
                        svgItem.setAttribute('width',end-start);
                        //svgItem.setAttribute('y1',len);
                        svgItem.setAttribute('height',50);
                        svgItem.setAttribute('x',0);
                        svgItem.setAttribute('y',0);
                        svgItem.setAttribute('fill',color);
                        svgItem.setAttribute('fill-opacity',0.1);
                        return svgItem;
                    });

                    var id2 = "RL-"+this.fixId(fRect.f.id());

                    console.log("cx="+cx+" color="+color);
                    this.addSVGObject(id2,bpCoord,100,100,function () {
                        var apple = document.createElementNS('http://www.w3.org/2000/svg','text');
                        var xlength = (end-start)/ 2.0 ; // for 0 case only
                        //var xLoc = svgSpace.bp2Native(xlength);
                        apple.setAttribute('x',8);
                        apple.setAttribute('y',13);
                        //apple.setAttribute('fill','white');
                        apple.setAttribute('stroke','black');
                        apple.setAttribute('display','block');
                        apple.innerHTML =  label ;
                        return apple;
                    });

                },

                // TODO: 1 - extend based in SVGTrackBase and SVGTrackSimpleBase, 2 - append to the blockDomNode (and remove in thee same way?
                // TODO: 3 - revert to call the showRange one for svgCoords (does the lollipop track do the looping zoom properly?)
                //fillBlock: function( args ) {
                //    var blockIndex = args.blockIndex;
                //    var block = args.block;
                //    var leftBase = args.leftBase;
                //    var scale = args.scale;
                //    var thisB = this;
                //
                //    // find the number that is within 2 px of the left boundary of
                //    // the block that ends with the most zeroes, or a 5 if no
                //    // zeroes
                //    var labelNumber = this.chooseLabel( args );
                //    var labelOffset = (leftBase+1-labelNumber)*scale/10;
                //    // console.log( leftBase+1, labelNumber, labelOffset );
                //
                //    var posLabel = document.createElement("div");
                //    var numtext = Util.addCommas( labelNumber );
                //    posLabel.className = this.labelClass;
                //
                //    // give the position label a negative left offset in ex's to
                //    // more-or-less center it over the left boundary of the block
                //    posLabel.style.left = "-" + Number(numtext.length)/1.7 + labelOffset + "ex";
                //
                //    // TODO: put the ticks here only!!
                //    posLabel.appendChild( document.createTextNode( numtext ) );
                //    block.domNode.appendChild(posLabel);
                //
                //    var highlight = this.browser.getHighlight();
                //    if( highlight && highlight.ref == this.refSeq.name ) {
                //        this.renderRegionHighlight( args, highlight );
                //    }
                //
                //
                //    var bookmarks = this.browser.getBookmarks();
                //    if( bookmarks ) {
                //        this.renderRegionBookmark( args, bookmarks, this.refSeq.name, true );
                //    }
                //
                //    this.heightUpdate( Math.round( this.posHeight*1.2 ), blockIndex);
                //    args.finishCallback();
                //},

                renderRegionRight: function(context, fRect){
                    var thisB = this;
                    // create svg element new
                    var feature = fRect.f;

                    // draw line
                    var svgSpace = this.svgSpace;

                    // compute the x coord given the bpCoord
                    var bpCoord = feature.get("start");
                    var label = feature.get("label");
                    var color = feature.get("color");
                    var cx = svgSpace.bp2Native(bpCoord);
                    var start = feature.get("start");
                    var end = feature.get("end");
                    var len = (end - start ) * 0.18 ;
                    len = svgSpace.getHeight() - len;
                    console.log("bpCoord="+bpCoord+" cx="+cx+" len="+len+" scale="+this.svgScale);
                    console.log("rendering region "+label + " from "+ start + " to "+end) ;

                    var id3 = "RR-"+this.fixId(fRect.f.id());

                    console.log("cx="+cx+" color="+color);
                    this.addSVGObject(id3,bpCoord,100,100,function () {
                        var apple = document.createElementNS('http://www.w3.org/2000/svg','text');
                        var formattedLabel = JSONUtils.numberWithCommas(label);
                        var xlength = -((formattedLabel.length-1) * 8) ;
                        apple.setAttribute('x',xlength);
                        //var xLoc = svgSpace.bp2Native(xlength);
                        //apple.setAttribute('x',-30);
                        apple.setAttribute('y',13);
                        //apple.setAttribute('fill','white');
                        apple.setAttribute('stroke','black');
                        apple.setAttribute('display','block');
                        apple.innerHTML =  label ;
                        return apple;
                    });
                },

                // draw each feature
                renderFeature: function( context, fRect ) {

                    this.inherited(arguments);      // call the superclass

                    var feature = fRect.f;
                    var type = feature.get("type");
                    if(type=='grid'){
                        this.renderGrid(context,fRect,5);
                    }
                    else
                    if(type=='region'){
                        console.log('render region');
                        this.renderRegion(context,fRect,5);
                    }
                    else
                    if(type=='region-right'){
                        console.log('render region right');
                        this.renderRegionRight(context,fRect,3);
                    }
                    else
                    if(type=='grid-right'){
                        console.log('render region right');
                        this.renderGrid(context,fRect,7);
                    }
                }

            });
    });
