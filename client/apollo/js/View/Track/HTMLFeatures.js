define( [
        'dojo/_base/declare',
        'dojo/_base/lang',
        'dojo/_base/array',
        'dojo/dom-construct',
        'dojo/dom-geometry',
        'dojo/on',
        'dojo/query',
        'JBrowse/Util',
        'WebApollo/ProjectionUtils',
        'JBrowse/View/Track/HTMLFeatures'
    ],
    function( declare,
              lang,
              array,
              dom,
              domGeom,
              on,
              query,
              Util,
              ProjectionUtils,
              HTMLFeatures
    ) {

        var HTMLFeatures = declare( [ HTMLFeatures], {

            // updateFeatureArrowPositions: function( coords ) {
            //     if( ! ('x' in coords) )
            //         return;
            //
            //     var viewmin = this.browser.view.minVisible();
            //     var viewmax = this.browser.view.maxVisible();
            //
            //     var blocks = this.blocks;
            //
            //     for( var blockIndex = 0; blockIndex < blocks.length; blockIndex++ ) {
            //         var block = blocks[blockIndex];
            //         if( ! block )
            //             continue;
            //         var childNodes = block.domNode.childNodes;
            //         for( var i = 0; i<childNodes.length; i++ ) {
            //             var featDiv = childNodes[i];
            //             if( ! featDiv.feature )
            //                 continue;
            //             var feature = featDiv.feature;
            //
            //             // Retrieve containerStart/End to resolve div truncation from renderFeature
            //             var containerStart = featDiv._containerStart;
            //             var containerEnd = featDiv._containerEnd;
            //
            //             var strand  = feature.get('strand');
            //             if( ! strand )
            //                 continue;
            //
            //             var fmin    = feature.get('start');
            //             var fmax    = feature.get('end');
            //             var arrowhead;
            //             var featDivChildren;
            //             //borrow displayStart,displayEnd for arrowhead calculations because of truncations in renderFeat
            //             var displayStart = Math.max( fmin, containerStart );
            //             var displayEnd = Math.min( fmax, containerEnd );
            //
            //             // minus strand
            //             if( strand < 0 && fmax > viewmin ) {
            //                 var minusArrowClass = 'minus-'+this.config.style.arrowheadClass;
            //                 featDivChildren = featDiv.childNodes;
            //                 for( var j = 0; j<featDivChildren.length; j++ ) {
            //                     arrowhead = featDivChildren[j];
            //                     if (typeof arrowhead.className === 'string') {
            //                         if( arrowhead && arrowhead.className && arrowhead.className.indexOf( minusArrowClass ) >= 0 ) {
            //                             arrowhead.style.left =
            //                                 ( fmin < viewmin ? block.bpToX( viewmin ) - block.bpToX( displayStart )
            //                                         : -this.minusArrowWidth
            //                                 ) + 'px';
            //                         };
            //                     }
            //                 }
            //             }
            //             // plus strand
            //             else if( strand > 0 && fmin < viewmax ) {
            //                 var plusArrowClass = 'plus-'+this.config.style.arrowheadClass;
            //                 featDivChildren = featDiv.childNodes;
            //                 for( var j = 0; j<featDivChildren.length; j++ ) {
            //                     arrowhead = featDivChildren[j];
            //                     if (typeof arrowhead.className === 'string') {
            //                         if( arrowhead && arrowhead.className && arrowhead.className.indexOf( plusArrowClass ) >= 0 ) {
            //                             arrowhead.style.right =
            //                                 ( fmax > viewmax ? block.bpToX( displayEnd ) - block.bpToX( viewmax-2 )
            //                                         : -this.plusArrowWidth
            //                                 ) + 'px';
            //                         }
            //                     }
            //                 }
            //             }
            //         }
            //     }
            // },


            // /**
            //  * arguments:
            //  * @param args.block div to be filled with info
            //  * @param args.leftBlock div to the left of the block to be filled
            //  * @param args.rightBlock div to the right of the block to be filled
            //  * @param args.leftBase starting base of the block
            //  * @param args.rightBase ending base of the block
            //  * @param args.scale pixels per base at the current zoom level
            //  * @param args.containerStart don't make HTML elements extend further left than this
            //  * @param args.containerEnd don't make HTML elements extend further right than this. 0-based.
            //  */
            // fillFeatures: function(args) {
            //     var blockIndex = args.blockIndex;
            //     var block = args.block;
            //     var leftBase = args.leftBase;
            //     var rightBase = args.rightBase;
            //     var scale = args.scale;
            //     var stats = args.stats;
            //     var containerStart = args.containerStart;
            //     var containerEnd = args.containerEnd;
            //     var finishCallback = args.finishCallback;
            //     var browser = this.browser;
            //
            //
            //     this.scale = scale;
            //
            //     block.featureNodes = {};
            //
            //     //determine the glyph height, arrowhead width, label text dimensions, etc.
            //     if( !this.haveMeasurements ) {
            //         this.measureStyles();
            //         this.haveMeasurements = true;
            //     }
            //
            //     var labelScale       = this.config.style.labelScale       || stats.featureDensity * this.config.style._defaultLabelScale;
            //     var descriptionScale = this.config.style.descriptionScale || stats.featureDensity * this.config.style._defaultDescriptionScale;
            //
            //     var curTrack = this;
            //
            //     var featCallback = dojo.hitch(this,function( feature ) {
            //         var uniqueId = feature.id();
            //         if( ! this._featureIsRendered( uniqueId ) ) {
            //             /* feature render, adding to block, centering refactored into addFeatureToBlock() */
            //             // var filter = this.browser.view.featureFilter;
            //             if( this.filterFeature( feature ) )  {
            //
            //                 //todo: adapt filterFeature instead of renderFeature
            //
            //                 // hook point
            //                 var render = 1;
            //                 if (typeof this.renderFilter === 'function')
            //                     render = this.renderFilter(feature);
            //
            //                 if (render === 1) {
            //                     this.addFeatureToBlock( feature, uniqueId, block, scale, labelScale, descriptionScale, containerStart, containerEnd );
            //                 }
            //             }
            //         }
            //     });
            //
            //     this.store.getFeatures( { ref: this.refSeq.name,
            //             start: leftBase,
            //             end: rightBase
            //         },
            //         featCallback,
            //         function ( args ) {
            //             curTrack.heightUpdate(curTrack._getLayout(scale).getTotalHeight(),
            //                 blockIndex);
            //             if ( args && args.maskingSpans ) {
            //                 //note: spans have to be inverted
            //                 var invSpan = [];
            //                 invSpan[0] = { start: leftBase };
            //                 var i = 0;
            //                 for ( var span in args.maskingSpans) {
            //                     if (args.maskingSpans.hasOwnProperty(span)) {
            //                         span = args.maskingSpans[span];
            //                         invSpan[i].end = span.start;
            //                         i++;
            //                         invSpan[i] = { start: span.end };
            //                     }
            //                 }
            //                 invSpan[i].end = rightBase;
            //                 if (invSpan[i].end <= invSpan[i].start) {
            //                     invSpan.splice(i,1); }
            //                 if (invSpan[0].end <= invSpan[0].start) {
            //                     invSpan.splice(0,1); }
            //                 curTrack.maskBySpans( invSpan, args.maskingSpans );
            //             }
            //             finishCallback();
            //         },
            //         function( error ) {
            //             console.error( error, error.stack );
            //             curTrack.fillBlockError( blockIndex, block, error );
            //             finishCallback();
            //         }
            //     );
            // },

            renderFeature: function( feature, uniqueId, block, scale, labelScale, descriptionScale, containerStart, containerEnd ) {
                //featureStart and featureEnd indicate how far left or right
                //the feature extends in bp space, including labels
                //and arrowheads if applicable

                var refSeqName = this.refSeq.name ;
                var featureEnd = feature.get('end');
                var featureStart = feature.get('start');
                if( typeof featureEnd == 'string' )
                    featureEnd = parseInt(featureEnd);
                if( typeof featureStart == 'string' )
                    featureStart = parseInt(featureStart);


                var projectedFeatures = ProjectionUtils.projectCoordinates(refSeqName,featureStart,featureEnd);
                featureStart = projectedFeatures[0];
                featureEnd = projectedFeatures[1];


                // layoutStart: start genome coord (at current scale) of horizontal space need to render feature,
                //       including decorations (arrowhead, label, etc) and padding
                var layoutStart = featureStart;
                // layoutEnd: end genome coord (at current scale) of horizontal space need to render feature,
                //       including decorations (arrowhead, label, etc) and padding
                var layoutEnd = featureEnd;

                //     JBrowse now draws arrowheads within feature genome coord bounds
                //     For WebApollo we're keeping arrow outside of feature genome coord bounds,
                //           because otherwise arrow can obscure edge-matching, CDS/UTR transitions, small inton/exons, etc.
                //     Would like to implement arrowhead change in WebApollo plugin, but would need to refactor HTMLFeature more to allow for that
                if (this.config.style.arrowheadClass) {
                    switch (feature.get('strand')) {
                        case 1:
                        case '+':
                            layoutEnd   += (this.plusArrowWidth / scale); break;
                        case -1:
                        case '-':
                            layoutStart -= (this.minusArrowWidth / scale); break;
                    }
                }

                var levelHeight = this.glyphHeight + this.glyphHeightPad;

                // if the label extends beyond the feature, use the
                // label end position as the end position for layout
                var name = this.getFeatureLabel( feature );
                var description = scale > descriptionScale && this.getFeatureDescription(feature);
                if( description && description.length > this.config.style.maxDescriptionLength )
                    description = description.substr(0, this.config.style.maxDescriptionLength+1 ).replace(/(\s+\S+|\s*)$/,'')+String.fromCharCode(8230);

                // add the label div (which includes the description) to the
                // calculated height of the feature if it will be displayed
                if( this.showLabels && scale >= labelScale && name ) {
                    layoutEnd = Math.max(layoutEnd, layoutStart + (''+name).length * this.labelWidth / scale );
                    levelHeight += this.labelHeight + this.labelPad;
                }
                if( this.showLabels && description ) {
                    layoutEnd = Math.max( layoutEnd, layoutStart + (''+description).length * this.labelWidth / scale );
                    levelHeight += this.labelHeight + this.labelPad;
                }

                layoutEnd += Math.max(1, this.padding / scale);

                var top = this._getLayout( scale )
                    .addRect( uniqueId,
                        layoutStart,
                        layoutEnd,
                        levelHeight);

                if( top === null ) {
                    // could not lay out, would exceed our configured maxHeight
                    // mark the block as exceeding the max height
                    this.markBlockHeightOverflow( block );
                    return null;
                }

                var featDiv = this.config.hooks.create(this, feature );
                this._connectFeatDivHandlers( featDiv );
                // NOTE ANY DATA SET ON THE FEATDIV DOM NODE NEEDS TO BE
                // MANUALLY DELETED IN THE cleanupBlock METHOD BELOW
                featDiv.track = this;
                featDiv.feature = feature;
                featDiv.layoutEnd = layoutEnd;

                // border values used in positioning boolean subfeatures, if any.
                featDiv.featureEdges = { s : Math.max( featDiv.feature.get('start'), containerStart ),
                    e : Math.min( featDiv.feature.get('end')  , containerEnd   ) };

                // (callbackArgs are the args that will be passed to callbacks
                // in this feature's context menu or left-click handlers)
                featDiv.callbackArgs = [ this, featDiv.feature, featDiv ];

                // save the label scale and description scale in the featDiv
                // so that we can use them later
                featDiv._labelScale = labelScale;
                featDiv._descriptionScale = descriptionScale;


                block.featureNodes[uniqueId] = featDiv;

                // hook point
                if (typeof this.featureHook1 === 'function')
                    this.featureHook1(feature,featDiv);

                // record whether this feature protrudes beyond the left and/or right side of the block
                if( layoutStart < block.startBase ) {
                    if( ! block.leftOverlaps ) block.leftOverlaps = [];
                    block.leftOverlaps.push( uniqueId );
                }
                if( layoutEnd > block.endBase ) {
                    if( ! block.rightOverlaps ) block.rightOverlaps = [];
                    block.rightOverlaps.push( uniqueId );
                }

                dojo.addClass(featDiv, "feature");
                var className = this.config.style.className;
                if (className == "{type}") { className = feature.get('type'); }
                var strand = feature.get('strand');
                switch (strand) {
                    case 1:
                    case '+':
                        dojo.addClass(featDiv, "plus-" + className); break;
                    case -1:
                    case '-':
                        dojo.addClass(featDiv, "minus-" + className); break;
                    default:
                        dojo.addClass(featDiv, className);
                }
                var phase = feature.get('phase');
                if ((phase !== null) && (phase !== undefined))
//            featDiv.className = featDiv.className + " " + featDiv.className + "_phase" + phase;
                    dojo.addClass(featDiv, className + "_phase" + phase);

                // check if this feature is highlighted
                var highlighted = this.isFeatureHighlighted( feature, name );

                // add 'highlighted' to the feature's class if its name
                // matches the objectName of the global highlight and it's
                // within the highlighted region
                if( highlighted )
                    dojo.addClass( featDiv, 'highlighted' );

                // Since some browsers don't deal well with the situation where
                // the feature goes way, way offscreen, we truncate the feature
                // to exist betwen containerStart and containerEnd.
                // To make sure the truncated end of the feature never gets shown,
                // we'll destroy and re-create the feature (with updated truncated
                // boundaries) in the transfer method.
                var displayStart = Math.max( featureStart, containerStart );
                var displayEnd = Math.min( featureEnd, containerEnd );
                var blockWidth = block.endBase - block.startBase;
                var featwidth = Math.max( this.minFeatWidth, (100 * ((displayEnd - displayStart) / blockWidth)));
                featDiv.style.cssText =
                    "left:" + (100 * (displayStart - block.startBase) / blockWidth) + "%;"
                    + "top:" + top + "px;"
                    + " width:" + featwidth + "%;"
                    + (this.config.style.featureCss ? this.config.style.featureCss : "");

                // Store the containerStart/End so we can resolve the truncation
                // when we are updating static elements
                featDiv._containerStart=containerStart;
                featDiv._containerEnd=containerEnd;

                if ( this.config.style.arrowheadClass ) {
                    var ah = document.createElement("div");
                    var featwidth_px = featwidth/100*blockWidth*scale;

                    switch (strand) {
                        case 1:
                        case '+':
                            ah.className = "plus-" + this.config.style.arrowheadClass;
                            ah.style.cssText =  "right: "+(-this.plusArrowWidth) + "px";
                            featDiv.appendChild(ah);
                            break;
                        case -1:
                        case '-':
                            ah.className = "minus-" + this.config.style.arrowheadClass;
                            ah.style.cssText = "left: " + (-this.minusArrowWidth) + "px";
                            featDiv.appendChild(ah);
                            break;
                    }
                }

                // fill in the template parameters in the featDiv and also for the labelDiv (see below)
                var context = lang.mixin( { track: this, feature: feature, callbackArgs: [ this, feature ] } );
                if(featDiv.title) {
                    featDiv.title=this.template( feature, this._evalConf( context, featDiv.title, "label" ));
                }

                if ( ( name || description ) && this.showLabels && scale >= labelScale ) {
                    var labelDiv = dojo.create( 'div', {
                        className: "feature-label" + ( highlighted ? ' highlighted' : '' ),
                        innerHTML:  ( name ? '<div class="feature-name">'+name+'</div>' : '' )
                        +( description ? ' <div class="feature-description">'+description+'</div>' : '' ),
                        style: {
                            top: (top + this.glyphHeight + 2) + "px",
                            left: (100 * (layoutStart - block.startBase) / blockWidth)+'%'
                        }
                    }, block.domNode );

                    this._connectFeatDivHandlers( labelDiv );

                    if(featDiv.title) labelDiv.title=featDiv.title;
                    featDiv.label = labelDiv;


                    // NOTE: ANY DATA ADDED TO THE labelDiv MUST HAVE A
                    // CORRESPONDING DELETE STATMENT IN cleanupBlock BELOW
                    labelDiv.feature = feature;
                    labelDiv.track = this;
                    // (callbackArgs are the args that will be passed to callbacks
                    // in this feature's context menu or left-click handlers)
                    labelDiv.callbackArgs = [ this, featDiv.feature, featDiv ];
                }

                if( featwidth > this.config.style.minSubfeatureWidth ) {
                    this.handleSubFeatures(feature, featDiv, displayStart, displayEnd, block);
                }

                // render the popup menu if configured
                if( this.config.menuTemplate ) {
                    window.setTimeout( dojo.hitch( this, '_connectMenus', featDiv ), 50+Math.random()*150 );
                }

                if ( typeof this.config.hooks.modify == 'function' ) {
                    this.config.hooks.modify(this, feature, featDiv);
                }

                return featDiv;
            },


            renderSubfeature: function( feature, featDiv, subfeature, displayStart, displayEnd, block ) {
                var subStart = subfeature.get('start');
                var subEnd = subfeature.get('end');

                var projectedFeatures = ProjectionUtils.projectCoordinates(refSeqName,subStart,subEnd);
                subStart = projectedFeatures[0];
                subEnd = projectedFeatures[1];

                var featLength = displayEnd - displayStart;
                var type = subfeature.get('type');
                var className;
                if( this.config.style.subfeatureClasses ) {
                    className = this.config.style.subfeatureClasses[type];
                    // if no class mapping specified for type, default to subfeature.get('type')
                    if (className === undefined) { className = type; }
                    // if subfeatureClasses specifies that subfeature type explicitly maps to null className
                    //     then don't render the feature
                    else if (className === null)  {
                        return null;
                    }
                }
                else {
                    // if no config.style.subfeatureClasses to specify subfeature class mapping, default to subfeature.get('type')
                    className = type;
                }

                // a className of 'hidden' causes things to not even be rendered
                if( className == 'hidden' )
                    return null;

                var subDiv = document.createElement("div");
                // used by boolean tracks to do positiocning
                subDiv.subfeatureEdges = { s: subStart, e: subEnd };

                dojo.addClass(subDiv, "subfeature");
                // check for className to avoid adding "null", "plus-null", "minus-null"
                if (className) {
                    switch ( subfeature.get('strand') ) {
                        case 1:
                        case '+':
                            dojo.addClass(subDiv, "plus-" + className); break;
                        case -1:
                        case '-':
                            dojo.addClass(subDiv, "minus-" + className); break;
                        default:
                            dojo.addClass(subDiv, className);
                    }
                }

                // if the feature has been truncated to where it doesn't cover
                // this subfeature anymore, just skip this subfeature

                var truncate = false;
                if (typeof this.config.truncateFeatures !== 'undefined' && this.config.truncateFeatures===true )
                    truncate = true;

                if ( truncate && (subEnd <= displayStart || subStart >= displayEnd) )
                    return null;

                subDiv.style.cssText = "left: " + (100 * ((subStart - displayStart) / featLength)) + "%;"
                    + "width: " + (100 * ((subEnd - subStart) / featLength)) + "%;";
                featDiv.appendChild(subDiv);

                block.featureNodes[ subfeature.id() ] = subDiv;

                return subDiv;
            }

        });

        return HTMLFeatures;
    });

/*

 Copyright (c) 2007-2010 The Evolutionary Software Foundation

 Created by Mitchell Skinner <mitch_skinner@berkeley.edu>

 This package and its accompanying libraries are free software; you can
 redistribute it and/or modify it under the terms of the LGPL (either
 version 2.1, or at your option, any later version) or the Artistic
 License 2.0.  Refer to LICENSE for the full license text.

 */
