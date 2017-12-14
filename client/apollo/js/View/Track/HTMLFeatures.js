define( [
        'dojo/_base/declare',
        'dojo/_base/lang',
        'dojo/_base/array',
        'dojo/dom-construct',
        'dojo/dom-geometry',
        'dojo/on',
        'dojo/query',
        'dojo/dom-construct',
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
              domConstruct,
              Util,
              ProjectionUtils,
              HTMLFeatures
    ) {

        var HTMLFeatures = declare( [ HTMLFeatures], {


            _renderAdditionalTagsDetail: function( track, f, featDiv, container ) {
                var additionalTags = array.filter( f.tags(), function(t) {
                    return ! this._isReservedTag( t ) && !t.startsWith('_');
                },this);

                if( additionalTags.length ) {
                    var atElement = domConstruct.create(
                        'div',
                        { className: 'additional',
                            innerHTML: '<h2 class="sectiontitle">Attributes</h2>'
                        },
                        container );
                    array.forEach( additionalTags.sort(), function(t) {
                        this.renderDetailField( container, t, f.get(t), f );
                    }, this );
                }
            },


            renderFeature: function( feature, uniqueId, block, scale, labelScale, descriptionScale, containerStart, containerEnd ) {
                //featureStart and featureEnd indicate how far left or right
                //the feature extends in bp space, including labels
                //and arrowheads if applicable
                console.log('AAAAAAA');

                var refSeqName = this.refSeq.name ;
                feature = ProjectionUtils.projectJSONFeature(feature,refSeqName);

                var featureEnd = feature.get('end');
                var featureStart = feature.get('start');
                if( typeof featureEnd == 'string' )
                    featureEnd = parseInt(featureEnd);
                if( typeof featureStart == 'string' )
                    featureStart = parseInt(featureStart);

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
