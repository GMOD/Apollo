define([
           'dojo/_base/declare',
           'dojo/_base/array',
           'dojo/_base/lang',
           'JBrowse/Util/FastPromise',
           'WebApollo/View/SVGFeatureGlyph',
           './_FeatureLabelMixin'
       ],
       function(
           declare,
           array,
           lang,
           FastPromise,
           FeatureGlyph,
           FeatureLabelMixin
       ) {


return declare([ FeatureGlyph, FeatureLabelMixin], {

    renderFeature: function( context, fRect ) {
        if( this.track.displayMode != 'collapsed' )
            context.clearRect( Math.floor(fRect.l), fRect.t, Math.ceil(fRect.w-Math.floor(fRect.l)+fRect.l), fRect.h );

        this.renderBox( context, fRect.viewInfo, fRect.f, fRect.t, fRect.rect.h, fRect.f );
        this.renderLabel( context, fRect );
        this.renderDescription( context, fRect );
        this.renderArrowhead( context, fRect );
    },

    // top and height are in px
    renderBox: function( context, viewInfo, feature, top, overallHeight, parentFeature, style ) {
        var left  = viewInfo.block.bpToX( feature.get('start') );
        var width = viewInfo.block.bpToX( feature.get('end') ) - left;
        //left = Math.round( left );
        //width = Math.round( width );

        style = style || lang.hitch( this, 'getStyle' );

        var height = this._getFeatureHeight( viewInfo, feature );
        if( ! height )
            return;
        if( height != overallHeight )
            top += Math.round( (overallHeight - height)/2 );

        // background
        var bgcolor = style( feature, 'color' );
        if( bgcolor ) {
            context.fillStyle = bgcolor;
            context.fillRect( left, top, Math.max(1,width), height );
        }
        else {
            context.clearRect( left, top, Math.max(1,width), height );
        }

        // foreground border
        var borderColor, lineWidth;
        if( (borderColor = style( feature, 'borderColor' )) && ( lineWidth = style( feature, 'borderWidth')) ) {
            if( width > 3 ) {
                context.lineWidth = lineWidth;
                context.strokeStyle = borderColor;

                // need to stroke a smaller rectangle to remain within
                // the bounds of the feature's overall height and
                // width, because of the way stroking is done in
                // canvas.  thus the +0.5 and -1 business.
                context.strokeRect( left+lineWidth/2, top+lineWidth/2, width-lineWidth, height-lineWidth );
            }
            else {
                context.globalAlpha = lineWidth*2/width;
                context.fillStyle = borderColor;
                context.fillRect( left, top, Math.max(1,width), height );
                context.globalAlpha = 1;
            }
        }
    },

    // feature label
    renderLabel: function( context, fRect ) {
        if( fRect.label ) {
            context.font = fRect.label.font;
            context.fillStyle = fRect.label.fill;
            context.textBaseline = fRect.label.baseline;
            context.fillText( fRect.label.text,
                              fRect.l+(fRect.label.xOffset||0),
                              fRect.t+(fRect.label.yOffset||0)
                            );
        }
    },

    // feature description
    renderDescription: function( context, fRect ) {
        if( fRect.description ) {
            context.font = fRect.description.font;
            context.fillStyle = fRect.description.fill;
            context.textBaseline = fRect.description.baseline;
            context.fillText(
                fRect.description.text,
                fRect.l+(fRect.description.xOffset||0),
                fRect.t + (fRect.description.yOffset||0)
            );
        }
    },

    // strand arrowhead
    renderArrowhead: function( context, fRect ) {
        if( fRect.strandArrow ) {
            if( fRect.strandArrow == 1 && fRect.rect.l+fRect.rect.w <= context.canvas.width ) {
                this.getEmbeddedImage( 'plusArrow' )
                    .then( function( img ) {
                               context.imageSmoothingEnabled = false;
                               context.drawImage( img, fRect.rect.l + fRect.rect.w, fRect.t + (fRect.rect.h-img.height)/2 );
                           });
            }
            else if( fRect.strandArrow == -1 && fRect.rect.l >= 0 ) {
                this.getEmbeddedImage( 'minusArrow' )
                    .then( function( img ) {
                               context.imageSmoothingEnabled = false;
                               context.drawImage( img, fRect.rect.l-9, fRect.t + (fRect.rect.h-img.height)/2 );
                           });
            }
        }
    },

    updateStaticElements: function( context, fRect, viewArgs ) {
        var vMin = viewArgs.minVisible;
        var vMax = viewArgs.maxVisible;
        var block = fRect.viewInfo.block;

        if( !( block.containsBp( vMin ) || block.containsBp( vMax ) ) )
            return;

        var scale = block.scale;
        var bpToPx = viewArgs.bpToPx;
        var lWidth = viewArgs.lWidth;
        var labelBp = lWidth / scale;
        var feature = fRect.f;
        var fMin = feature.get('start');
        var fMax = feature.get('end');

        if( fRect.strandArrow ) {
            if( fRect.strandArrow == 1 && fMax >= vMax && fMin <= vMax ) {
                this.getEmbeddedImage( 'plusArrow' )
                    .then( function( img ) {
                               context.imageSmoothingEnabled = false;
                               context.drawImage( img, bpToPx(vMax) - bpToPx(vMin) - 9, fRect.t + (fRect.rect.h-img.height)/2 );
                           });
            }
            else if( fRect.strandArrow == -1 && fMin <= vMin && fMax >= vMin ) {
                this.getEmbeddedImage( 'minusArrow' )
                    .then( function( img ) {
                               context.imageSmoothingEnabled = false;
                               context.drawImage( img, 0, fRect.t + (fRect.rect.h-img.height)/2 );
                           });
            }
        }

        var fLabelWidth = fRect.label ? fRect.label.w : 0;
        var fDescriptionWidth = fRect.description ? fRect.description.w : 0;
        var maxLeft = bpToPx( fMax ) - Math.max(fLabelWidth, fDescriptionWidth) - bpToPx( vMin );
        var minLeft = bpToPx( fMin ) - bpToPx( vMin );

    }

});
});