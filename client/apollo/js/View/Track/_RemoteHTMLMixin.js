define([
        'dojo/_base/declare',
        'dojo/dom-construct',
        'dojo/_base/array',
        'JBrowse/Util',
        'WebApollo/ProjectionUtils'
    ],
    function(
        declare,
        domConstruct,
        array,
        Util,
        ProjectionUtils
    ) {

        return declare( null, {

            fillFeatures: function(args) {
                var blockIndex = args.blockIndex;
                var block = args.block;
                var leftBase = args.leftBase;
                var rightBase = args.rightBase;
                var scale = args.scale;
                var stats = args.stats;
                var containerStart = args.containerStart;
                var containerEnd = args.containerEnd;
                var finishCallback = args.finishCallback;
                var browser = this.browser;

                this.scale = scale;

                block.featureNodes = {};

                //determine the glyph height, arrowhead width, label text dimensions, etc.
                if( !this.haveMeasurements ) {
                    this.measureStyles();
                    this.haveMeasurements = true;
                }

                var labelScale       = this.config.style.labelScale       || stats.featureDensity * this.config.style._defaultLabelScale;
                var descriptionScale = this.config.style.descriptionScale || stats.featureDensity * this.config.style._defaultDescriptionScale;

                var curTrack = this;

                var featCallback = dojo.hitch(this,function( feature ) {
                    var uniqueId = feature.id();
                    if( ! this._featureIsRendered( uniqueId ) ) {
                        if( this.filterFeature( feature ) )  {
                            // hook point
                            var render = 1;
                            if (typeof this.renderFilter === 'function')
                                render = this.renderFilter(feature);

                            if (render === 1) {
                                this.addFeatureToBlock( feature, uniqueId, block, scale, labelScale, descriptionScale, containerStart, containerEnd );
                            }
                        }
                    }
                });

                var errorCallback = dojo.hitch(this, function(e) {
                    this._handleError(e, args);
                    finishCallback(e);
                });

                var sequenceList = ProjectionUtils.parseSequenceList(this.refSeq.name);
                if (sequenceList[0].reverse) {
                    errorCallback(ProjectionUtils.NOT_YET_SUPPORTED_MESSAGE)
                }
                else {
                    var refSeqName = sequenceList[0].name;
                    this.store.getFeatures( {
                            ref: refSeqName,
                            start: leftBase,
                            end: rightBase
                        },
                        featCallback,
                        function ( args ) {
                            curTrack.heightUpdate(curTrack._getLayout(scale).getTotalHeight(), blockIndex);
                            if ( args && args.maskingSpans ) {
                                //note: spans have to be inverted
                                var invSpan = [];
                                invSpan[0] = { start: leftBase };
                                var i = 0;
                                for ( var span in args.maskingSpans) {
                                    if (args.maskingSpans.hasOwnProperty(span)) {
                                        span = args.maskingSpans[span];
                                        invSpan[i].end = span.start;
                                        i++;
                                        invSpan[i] = { start: span.end };
                                    }
                                }
                                invSpan[i].end = rightBase;
                                if (invSpan[i].end <= invSpan[i].start) {
                                    invSpan.splice(i,1); }
                                if (invSpan[0].end <= invSpan[0].start) {
                                    invSpan.splice(0,1); }
                                curTrack.maskBySpans( invSpan, args.maskingSpans );
                            }
                            finishCallback();
                        },
                        errorCallback
                    );
                }
            }
        });
});