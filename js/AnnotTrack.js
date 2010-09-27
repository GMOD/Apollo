function AnnotTrack(trackMeta, url, refSeq, browserParams) {
    //trackMeta: object with:
    //            key:   display text track name
    //            label: internal track name (no spaces, odd characters)
    //url: URL of the track's JSON file
    //refSeq: object with:
    //         start: refseq start
    //         end:   refseq end
    //browserParams: object with:
    //                changeCallback: function to call once JSON is loaded
    //                trackPadding: distance in px between tracks
    //                baseUrl: base URL for the URL in trackMeta

  FeatureTrack.call(this, trackMeta, url, refSeq, browserParams);

    var thisObj = this;
    this.subfeatureCallback = function(i, val, param) {
        thisObj.renderSubfeature(param.feature, param.featDiv, val);
    };

}

// Inherit from FeatureTrack
AnnotTrack.prototype = new FeatureTrack();
console.log("AnnotTrack created");  // DEL

AnnotTrack.prototype.setViewInfo = function(genomeView, numBlocks,
                                            trackDiv, labelDiv,
                                            widthPct, widthPx, scale) {
  Track.prototype.setViewInfo.apply(this, [genomeView, numBlocks,
                                           trackDiv, labelDiv,
                                           widthPct, widthPx, scale]);
  this.setLabel(this.key);
};

/*

Copyright (c) 2007-2010 The Evolutionary Software Foundation

Created by Mitchell Skinner <mitch_skinner@berkeley.edu>

This package and its accompanying libraries are free software; you can
redistribute it and/or modify it under the terms of the LGPL (either
version 2.1, or at your option, any later version) or the Artistic
License 2.0.  Refer to LICENSE for the full license text.

*/
