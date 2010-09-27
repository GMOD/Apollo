/* 
   DasFeatureTrack ==> FeatureTrack ==> Track
   extending / "subclassing" FeatureTrack to modify a few methods to handle differences 
   between standard JBrowse features and DAS features (and their corresponding NCLists)
   Main differences:
        DAS features have full subfeature info embedded in same JSON, 
	     whereas JBrowse features have pointers into other JSON files 
	     for subfeature data
	DAS features use the DAS feature id for determining uniqueness, 
	     whereas JBrowse features use path in their NCList 
	     due to way DAS is queried, the same DAS feature could be in two different
	     NCLists and have different paths in those different NCLists, so 
	     can't use same mechanism.
	     And JBrowse features aren't guaranteed to have a uniques id/name, 
	     so can't use same method as DAS either

   For object-oriented correctness, 
        may want to split these same methods off for standard JBrowse features into 
	a subclass too, instead of being in the base class and being overriden here:
	StandardFeatureTrack ==> FeatureTrack ==> Track
 */

function DasFeatureTrack(trackMeta, url, refSeq, browserParams) {
    FeatureTrack.call(this, trackMeta, url, refSeq, browserParams);
}

DasFeatureTrack.prototype = new FeatureTrack("");

DasFeatureTrack.prototype.fillBlock = function(blockIndex, block,
                                            leftBlock, rightBlock,
                                            leftBase, rightBase,
                                             scale, stripeWidth, 
					     containerStart, containerEnd) {
  // Histogram not yet re-implemented
  this.fillFeatures(blockIndex, block, leftBlock, rightBlock,
                    leftBase, rightBase, scale, 
		    containerStart, containerEnd);
};

DasFeatureTrack.prototype.getId = function(feature, path)  {
    var fid = this.fields["id"];
    return feature[fid];
};

DasFeatureTrack.prototype.handleSubFeatures = function(feature, featDiv, 
						     displayStart, displayEnd)  {
    var subfeatures = this.fields["subfeatures"];
    for (var i = 0; i < feature[subfeatures].length; i++) {
	var subfeature = feature[subfeatures][i];
	this.renderSubfeature(feature, featDiv, subfeature, displayStart, displayEnd);
    }
}


/*
Copyright (c) 2007-2010 The Evolutionary Software Foundation & BerkeleyBOP
Created by Mitchell Skinner <mitch_skinner@berkeley.edu>
    and Gregg Helt <gregghelt@gmail.com

This package and its accompanying libraries are free software; you can
redistribute it and/or modify it under the terms of the LGPL (either
version 2.1, or at your option, any later version) or the Artistic
License 2.0.  Refer to LICENSE for the full license text.
*/
