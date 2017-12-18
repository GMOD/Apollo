define([
           'dojo/_base/declare',
           'dojo/_base/array',
            'JBrowse/View/FeatureGlyph'
       ],
       function(
           declare,
           array,
           FeatureGlyph
       ) {

return declare( [FeatureGlyph], {
    //stub
    // renderFeature: function( context, fRect ) {
    // },

    /* If it's a boolean track, mask accordingly */
    maskBySpans: function( context, fRect ) {
        var canvasHeight = context.canvas.height;

        var thisB = this;

        // make a temporary canvas to store image data
        var tempCan = dojo.create( 'canvas', {height: canvasHeight, width: context.canvas.width} );
        var ctx2 = tempCan.getContext('2d');
        var l = Math.floor(fRect.l);
        var w = Math.ceil(fRect.w + fRect.l) - l;

        /* note on the above: the rightmost pixel is determined
           by l+w. If either of these is a float, then canvas
           methods will not behave as desired (i.e. clear and
           draw will not treat borders in the same way).*/
        array.forEach( fRect.m, function(m) { try {
            if ( m.l < l ) {
                m.w += m.l-l;
                m.l = l;
            }
            if ( m.w > w )
                m.w = w;
            if ( m.l < 0 ) {
                m.w += m.l;
                m.l = 0;
            }
            if ( m.l + m.w > l + w )
                m.w = w + l - m.l;
            if ( m.l + m.w > context.canvas.width )
                m.w = context.canvas.width-m.l;
            ctx2.drawImage(context.canvas, m.l, fRect.t, m.w, fRect.h, m.l, fRect.t, m.w, fRect.h);
            context.globalAlpha = thisB.booleanAlpha;
            // clear masked region and redraw at lower opacity.
            context.clearRect(m.l, fRect.t, m.w, fRect.h);
            context.drawImage(tempCan, m.l, fRect.t, m.w, fRect.h, m.l, fRect.t, m.w, fRect.h);
            context.globalAlpha = 1;
        } catch(e) {};
        });
    },

});
});
