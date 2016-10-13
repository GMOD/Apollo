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
                    this.posHeight = 20;
                    this.height = 20;
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
                    this.inherited(arguments);
                    if (this.svgSpace) {
                        this.svgSpace.height = 20;
                    }
                    else {
                        console.log('has no space ');
                        //this.svgCanvas.height = 200 ;
                    }
                    this.height = 20;
                },

                setViewInfo: function (genomeView, heightUpdate, numBlocks, trackDiv, widthPct, widthPx, scale) {
                    this.inherited(arguments);

                    this.svgSpace = new SVGLayerPxSpace(this);      // px-space svg layer
                    //this.svgSpace = new SVGLayerBpSpace(this);    // bp-space svg layer
                    this.svgSpace.setViewInfo(genomeView, heightUpdate, numBlocks, trackDiv, widthPct, widthPx, scale);

                },

                showRange: function (first, last, startBase, bpPerBlock, scale, containerStart, containerEnd) {

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

                    this.svgSpace.showRange(first, last, startBase, bpPerBlock, scale, containerStart, containerEnd);

                },
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
                    var feature = fRect.f;
                    var sequence = feature.data.sequence;

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
                        svgItem.setAttribute('d', 'M0 0 L0 20 L10 20 L10 0');
                        svgItem.setAttribute('fill', color);
                        svgItem.setAttribute('stroke', 'black');
                        svgItem.setAttribute('fill-opacity', 1);
                        return svgItem;
                    });

                    var id4 = "RLL-" + this.fixId(fRect.f.id());
                    //console.log("cx=" + nativeStart + " color=" + color);
                    this.addSVGObject(id4, start, 100, 100, function () {
                        var rightEdgeText = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                        var formattedLabel = numberWithCommas(startLabel);
                        rightEdgeText.setAttribute('x', 15);
                        rightEdgeText.setAttribute('y', 12);
                        rightEdgeText.setAttribute('font-size', 'small');
                        rightEdgeText.setAttribute('fill',color);
                        rightEdgeText.setAttribute('stroke-width', 0);
                        rightEdgeText.setAttribute('stroke', 'black');
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

                    var endLabel = sequence.end;

                    // compute the x coord given the bpCoord
                    var start = feature.get("start");
                    var end = feature.get("end");
                    var label = feature.get("label");
                    var color = feature.get("color");

                    var id5 = "RRD-" + this.fixId(fRect.f.id());

                    this.addSVGObject(id5, start, 100, 100, function () {
                        var svgItem = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                        // svgItem.setAttribute('d', 'M0 0 L0 50 L-160 50 L-160 26 L-5 26 L-5 0 Z');
                        svgItem.setAttribute('d', 'M0 0 L0 20 L-10 20 L-10 0 Z');
                        svgItem.setAttribute('fill', color);
                        svgItem.setAttribute('stroke', 'black');
                        return svgItem;
                    });

                    var id4 = "RRR-" + this.fixId(fRect.f.id());
                    this.addSVGObject(id4, start, 100, 100, function () {
                        var rightEdgeText = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                        var formattedLabel = numberWithCommas(endLabel);
                        var xlength = -((formattedLabel.length - 1) * 10);
                        rightEdgeText.setAttribute('x', xlength);
                        rightEdgeText.setAttribute('y', 12);
                        rightEdgeText.setAttribute('font-size', 'small');
                        rightEdgeText.setAttribute('fill', color);
                        rightEdgeText.setAttribute('stroke-width', 0);
                        rightEdgeText.setAttribute('stroke', color);
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

