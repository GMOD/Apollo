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
        'WebApollo/View/Track/LegendCoordinates'
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
        LegendCoordinates
    ) {

        function numberWithCommas(x) {
            return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        }

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

                constructor: function (args) {//name, labelClass, posHeight) {
                    console.log(args);
                    this.loaded = true;
                    this.labelClass = args.labelClass;
                    this.pinned = true;
                    //this.posHeight = args.posHeight;
                    this.posHeight = 30 ;
                    //this.height = Math.round(args.posHeight * 1.2);
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

                    this.svgCoords = new LegendCoordinates(this);
                    this.svgCoords.setViewInfo( genomeView, heightUpdate, numBlocks, trackDiv, widthPct, widthPx, scale );

                    this.svgSpace = new SVGLayerPxSpace(this);      // px-space svg layer
                    //this.svgSpace = new SVGLayerBpSpace(this);    // bp-space svg layer
                    this.svgSpace.setViewInfo( genomeView, heightUpdate, numBlocks, trackDiv, widthPct, widthPx, scale );

                },

                showRange: function(first, last, startBase, bpPerBlock, scale, containerStart, containerEnd) {
                    console.log("SVGLollipop::showRange");
                    console.log(first+" "+last+" "+startBase+" "+bpPerBlock+" "+scale+" "+containerStart+" "+containerEnd);

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

                    this.svgCoords.showRange(first, last, startBase, bpPerBlock, scale, containerStart, containerEnd);
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
                    var len = (feature.get("end") - feature.get("start") ) * .18 ;
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


                    // draw on right side?
                    var id2 = "C-"+this.fixId(fRect.f.id());
                    console.log("cx="+cx+" color="+color);
                    this.addSVGObject(id2,bpCoord,100,100,function () {
                        var labelSVG = document.createElementNS('http://www.w3.org/2000/svg','text');
                        var xlength = 3 ; // for 0 case only
                        var formattedLabel = numberWithCommas(label);
                        if(label!='0'){
                            xlength = - (formattedLabel.length-1) * (offsetMultiplier ? offsetMultiplier : 1) ;
                        }
                        console.log('xlength: '+xlength + ' label: '+label  + ' formatted lnegh: ' + formattedLabel + 'offset: '+ offsetMultiplier);
                        labelSVG.setAttribute('x',xlength);
                        labelSVG.setAttribute('y','30');
                        labelSVG.setAttribute('fill',color);
                        labelSVG.setAttribute('display','block');
                        labelSVG.innerHTML =  formattedLabel;
                        return labelSVG;
                    });
                },

                renderRegion: function(context,fRect){
                    var thisB = this;
                    // create svg element new
                    var feature = fRect.f;
                    var data = feature.data;


                    // draw line
                    var svgSpace = this.svgSpace;

                    // compute the x coord given the bpCoord
                    var start = feature.get("start");
                    var label = feature.get("label");
                    var color = feature.get("color");
                    var cx = svgSpace.bp2Native(start);
                    var end = feature.get("end");
                    var len = (end - start ) * .18 ;
                    len = svgSpace.getHeight() - len;
                    console.log("bpCoord="+start+" cx="+cx+" len="+len+" scale="+this.svgScale);
                    console.log("rendering region "+label + " from "+ start + " to "+end) ;


                    // draw stems
                    var id = "R-"+this.fixId(fRect.f.id());

                    this.addSVGObject(id,start,100,100,function () {
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
                    this.addSVGObject(id2,start,100,100,function () {
                        var leftLabelSvg = document.createElementNS('http://www.w3.org/2000/svg','text');
                        //var xlength = (end-start)/ 2.0 ; // for 0 case only
                        //var xLoc = svgSpace.bp2Native(xlength);
                        leftLabelSvg.setAttribute('x',28);
                        leftLabelSvg.setAttribute('y',13);
                        //apple.setAttribute('fill','white');
                        leftLabelSvg.setAttribute('stroke','black');
                        leftLabelSvg.setAttribute('display','block');
                        //leftLabelSvg.innerHTML =  label+" ("+numberWithCommas(start)+")" ;
                        leftLabelSvg.innerHTML =  label ;
                        return leftLabelSvg;
                    });

                    var id4 = "RLL-"+this.fixId(fRect.f.id());
                    console.log("cx="+cx+" color="+color);
                    if(start>0){
                        this.addSVGObject(id4,start,100,100,function () {
                            var rightEdgeText = document.createElementNS('http://www.w3.org/2000/svg','text');
                            var formattedLabel = numberWithCommas(start) ;
                            var xlength = -((formattedLabel.length-1) * 5) ;
                            //rightEdgeText.setAttribute('x',end);
                            //var xLoc = svgSpace.bp2Native(xlength);
                            rightEdgeText.setAttribute('x',5);
                            rightEdgeText.setAttribute('y',42);
                            rightEdgeText.setAttribute('font-size','x-small');
                            //rightEdgeText.setAttribute('transform','rotate(90 0 20)');
                            //rightEdgeText.setAttribute('fill','white');
                            rightEdgeText.setAttribute('stroke','blue');
                            rightEdgeText.setAttribute('display','block');
                            rightEdgeText.innerHTML =  formattedLabel;
                            return rightEdgeText;
                        });
                    }
                },


                renderRegionRight: function(context, fRect){
                    var thisB = this;
                    // create svg element new
                    var feature = fRect.f;

                    // draw line
                    var svgSpace = this.svgSpace;

                    // compute the x coord given the bpCoord
                    var start = feature.get("start");
                    var end = feature.get("end");
                    var label = feature.get("label");
                    var color = feature.get("color");
                    var cx = svgSpace.bp2Native(start);
                    var len = (end - start ) * .18 ;
                    len = svgSpace.getHeight() - len;
                    console.log("bpCoord="+start+" cx="+cx+" len="+len+" scale="+this.svgScale);
                    console.log("rendering region "+label + " from "+ start + " to "+end) ;

                    var id3 = "RR-"+this.fixId(fRect.f.id());

                    console.log("cx="+cx+" color="+color);
                    this.addSVGObject(id3,start,100,100,function () {
                        var rightLabelRegion = document.createElementNS('http://www.w3.org/2000/svg','text');
                        //var formattedLabel = '('+numberWithCommas(end)+') '+ label;
                        var formattedLabel =  label;
                        var xlength = -((formattedLabel.length-1) * 12) ;
                        rightLabelRegion.setAttribute('x',xlength);
                        //var xLoc = svgSpace.bp2Native(xlength);
                        //rightLabelRegion.setAttribute('x',-30);
                        rightLabelRegion.setAttribute('y',13);
                        //rightLabelRegion.setAttribute('fill','white');
                        rightLabelRegion.setAttribute('stroke','black');
                        rightLabelRegion.setAttribute('display','block');
                        rightLabelRegion.innerHTML =  formattedLabel ;
                        return rightLabelRegion;
                    });

                    var id4 = "RRR-"+this.fixId(fRect.f.id());
                    console.log("cx="+cx+" color="+color);
                    this.addSVGObject(id4,start,100,100,function () {
                        var rightEdgeText = document.createElementNS('http://www.w3.org/2000/svg','text');
                        var formattedLabel = numberWithCommas(end) ;
                        var xlength = -((formattedLabel.length-1) * 8) ;
                        //rightEdgeText.setAttribute('x',end);
                        //var xLoc = svgSpace.bp2Native(xlength);
                        rightEdgeText.setAttribute('x',xlength);
                        rightEdgeText.setAttribute('y',42);
                        rightEdgeText.setAttribute('font-size','x-small');
                        //rightEdgeText.setAttribute('transform','rotate(90 0 20)');
                        //rightEdgeText.setAttribute('fill','white');
                        rightEdgeText.setAttribute('stroke','blue');
                        rightEdgeText.setAttribute('display','block');
                        rightEdgeText.innerHTML =  formattedLabel;
                        return rightEdgeText;
                    });
                },

                // draw each feature
                renderFeature: function( context, fRect ) {

                    this.inherited(arguments);      // call the superclass

                    var feature = fRect.f;
                    var type = feature.get("type");
                    //console.log('feautre: '+JSON.stringify(feature));
                    //if(type=='grid'){
                    //    this.renderGrid(context,fRect,5);
                    //}
                    //else
                    if(type=='region'){
                        console.log('render region');
                        this.renderRegion(context,fRect);
                    }
                    else
                    if(type=='region-right'){
                        console.log('render region right');
                        this.renderRegionRight(context,fRect,3);
                    }
                    //else
                    //if(type=='grid-right'){
                    //    console.log('render region right');
                    //    this.renderGrid(context,fRect,7);
                    //}
                }

            });
    });
