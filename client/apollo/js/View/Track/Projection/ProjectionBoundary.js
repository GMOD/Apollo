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
        'JBrowse/View/Track/SVG/SVGLayerPxSpace'
    ],
    function (declare,
              array,
              lang,
              domEvent,
              domConstruct,
              domStyle,
              SVGTrackBase,
              SVGLayerCoords,
              SVGLayerBpSpace,
              SVGLayerPxSpace
    ) {

        // TODO: move to util funciton
        function numberWithCommas(x) {
            return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        }

        return declare(
            [SVGTrackBase]
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
                    console.log('legend3 args: ');
                    console.log(args);
                    this.loaded = true;
                    this.labelClass = args.labelClass;
                    this.pinned = true;
                    this.posHeight = 30;
                    this.height = 30;
                },

                // this track has no track label or track menu, stub them out
                makeTrackLabel: function () {
                },
                makeTrackMenu: function () {
                },

                _trackMenuOptions: function () {
                },

                _defaultConfig: function () {
                    var thisConfig = this.inherited(arguments);
                    thisConfig.menuTemplate = null;
                    thisConfig.noExport = true;  // turn off default "Save track data" "
                    //thisConfig.style.centerChildrenVertically = false;
                    thisConfig.pinned = true;
                    return thisConfig;
                },
                heightUpdate: function (height, blockIndex) {
                    //console.log("SVGFeatures::heightUpdate("+height+")");
                    //console.dir(arguments);
                    //var err = new Error();
                    //console.log(err.stack);

                    this.inherited(arguments);
                    if (this.svgSpace) {
                        //this.svgCanvas.height = this.svgCanvas.offsetHeight;
                        //console.log('has height ');
                        this.svgSpace.height = 30;
                    }
                    else {
                        console.log('has no space ');
                        //this.svgCanvas.height = 200 ;
                    }
                    this.height = 30;
                },

                setViewInfo: function (genomeView, heightUpdate, numBlocks, trackDiv, widthPct, widthPx, scale) {
                    console.log("SVGLollipop::setViewInfo");
                    console.log(numBlocks + " " + widthPct + " " + widthPx + " " + scale);

                    this.inherited(arguments);

                    // this.svgCoords = new ProjectionCoordinates(this);
                    // this.svgCoords.setViewInfo(genomeView, heightUpdate, numBlocks, trackDiv, widthPct, widthPx, scale);

                    this.svgSpace = new SVGLayerPxSpace(this);      // px-space svg layer
                    //this.svgSpace = new SVGLayerBpSpace(this);    // bp-space svg layer
                    this.svgSpace.setViewInfo(genomeView, heightUpdate, numBlocks, trackDiv, widthPct, widthPx, scale);

                },

                showRange: function (first, last, startBase, bpPerBlock, scale, containerStart, containerEnd) {
                    console.log("SVGLollipop::showRange");
                    console.log(first + " " + last + " " + startBase + " " + bpPerBlock + " " + scale + " " + containerStart + " " + containerEnd);

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

                    // this.svgCoords.showRange(first, last, startBase, bpPerBlock, scale, containerStart, containerEnd);
                    this.svgSpace.showRange(first, last, startBase, bpPerBlock, scale, containerStart, containerEnd);

                },
                /*
                 id = unique string of object
                 bpCoord = basepair coordinate of object
                 width = width of object
                 height = height of object
                 callback = function that returns object

                 */
                addSVGObject: function (id, bpCoord, width, height, callback) {
                    this.svgSpace.addSVGObject(id, bpCoord, width, height, callback);
                },
                fixId: function (val) {
                    return val.replace(",", "-");
                },
                computeHeight: function () {
                    //return this.svgSpace.getHeight();
                    return this.height;
                },
                fillFeatures: function (args) {
                    this.inherited(arguments);      // call the superclass's
                },


                renderRegion: function (context, fRect) {
                    var thisB = this;
                    // create svg element new
                    var feature = fRect.f;
                    //var data = feature.sequence;
                    var sequence = feature.data.sequence;
                    console.log(feature);


                    // draw line
                    //var svgSpace = this.svgSpace;

                    var padding = sequence.padding ? sequence.padding : 0;
                    var startLabel = sequence.start-padding;

                    // compute the x coord given the bpCoord
                    var start = feature.get("start");
                    var label = feature.get("label");
                    var color = feature.get("color");
                    var end = feature.get("end");

                    // this is the left boundary
                    var id = "R-" + this.fixId(fRect.f.id());

                    this.addSVGObject(id, start, 100, 100, function () {
                        var svgItem = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                        svgItem.setAttribute('d', 'M0 0 L0 50 L160 50 L160 25 L5 25 L5 0');
                        svgItem.setAttribute('fill', color);
                        svgItem.setAttribute('stroke', 'black');
                        svgItem.setAttribute('fill-opacity', 1);
                        return svgItem;
                    });

                    // var id2 = "RL-" + this.fixId(fRect.f.id());
                    // //console.log("cx=" + nativeStart + " color=" + color);
                    // this.addSVGObject(id2, start, 100, 100, function () {
                    //     var leftLabelSvg = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                    //     leftLabelSvg.setAttribute('x', 30);
                    //     leftLabelSvg.setAttribute('y', 42);
                    //     leftLabelSvg.setAttribute('fill','white');
                    //     leftLabelSvg.setAttribute('stroke', 'white');
                    //     leftLabelSvg.setAttribute('stroke-width', 0);
                    //     leftLabelSvg.setAttribute('display', 'block');
                    //     leftLabelSvg.innerHTML = label;
                    //     return leftLabelSvg;
                    // });

                    var id4 = "RLL-" + this.fixId(fRect.f.id());
                    //console.log("cx=" + nativeStart + " color=" + color);
                    this.addSVGObject(id4, start, 100, 100, function () {
                        var rightEdgeText = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                        var formattedLabel = numberWithCommas(startLabel);
                        rightEdgeText.setAttribute('x', 5);
                        rightEdgeText.setAttribute('y', 42);
                        rightEdgeText.setAttribute('font-size', 'x-small');
                        rightEdgeText.setAttribute('fill','white');
                        rightEdgeText.setAttribute('stroke-width', 0);
                        rightEdgeText.setAttribute('stroke', 'white');
                        rightEdgeText.setAttribute('display', 'block');
                        rightEdgeText.innerHTML = formattedLabel;
                        return rightEdgeText;
                    });
                },


                renderRegionRight: function (context, fRect) {
                    var thisB = this;
                    // create svg element new
                    var feature = fRect.f;
                    var sequence = feature.data.sequence;

                    // draw line
                    //var svgSpace = this.svgSpace;

                    var endLabel = sequence.end;

                    // compute the x coord given the bpCoord
                    var start = feature.get("start");
                    var end = feature.get("end");
                    var label = feature.get("label");
                    var color = feature.get("color");

                    var id5 = "RRD-" + this.fixId(fRect.f.id());

                    this.addSVGObject(id5, start, 100, 100, function () {
                        var svgItem = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                        svgItem.setAttribute('d', 'M0 0 L0 50 L-160 50 L-160 26 L-5 26 L-5 0 Z');
                        svgItem.setAttribute('fill', color);
                        svgItem.setAttribute('stroke', 'black');
                        return svgItem;
                    });

                    var id3 = "RR-" + this.fixId(fRect.f.id());

                    //console.log("cx=" + cx + " color=" + color);
                    // this.addSVGObject(id3, start, 100, 100, function () {
                    //     var rightLabelRegion = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                    //     var formattedLabel = label;
                    //     var xlength = -((formattedLabel.length - 1) * 14 + 30);
                    //     rightLabelRegion.setAttribute('x', xlength);
                    //     rightLabelRegion.setAttribute('y', 42);
                    //     rightLabelRegion.setAttribute('fill', 'white');
                    //     rightLabelRegion.setAttribute('stroke', 'white');
                    //     rightLabelRegion.setAttribute('stroke-width', 0);
                    //     rightLabelRegion.setAttribute('display', 'block');
                    //     rightLabelRegion.innerHTML = formattedLabel;
                    //     return rightLabelRegion;
                    // });

                    var id4 = "RRR-" + this.fixId(fRect.f.id());
                    //console.log("cx=" + cx + " color=" + color);
                    this.addSVGObject(id4, start, 100, 100, function () {
                        var rightEdgeText = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                        var formattedLabel = numberWithCommas(endLabel);
                        var xlength = -((formattedLabel.length - 1) * 8);
                        rightEdgeText.setAttribute('x', xlength);
                        rightEdgeText.setAttribute('y', 42);
                        rightEdgeText.setAttribute('font-size', 'x-small');
                        rightEdgeText.setAttribute('fill', 'white');
                        rightEdgeText.setAttribute('stroke-width', 0);
                        rightEdgeText.setAttribute('stroke', 'white');
                        rightEdgeText.setAttribute('display', 'block');
                        rightEdgeText.innerHTML = formattedLabel;
                        return rightEdgeText;
                    });

                },

                // draw each feature
                renderFeature: function (context, fRect) {

                    this.inherited(arguments);      // call the superclass

                    var feature = fRect.f;
                    var type = feature.get("type");
                    if (type == 'region') {
                        console.log('render region');
                        this.renderRegion(context, fRect);
                    }
                    else if (type == 'region-right') {
                        console.log('render region right');
                        this.renderRegionRight(context, fRect, 3);
                    }
                }

            });
    });

