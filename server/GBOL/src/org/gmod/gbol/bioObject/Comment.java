package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

public class Comment extends AbstractBioFeatureProperty {

	private static final long serialVersionUID = 1L;

	public Comment(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(featureProperty, conf);
	}
	
	public Comment(AbstractBioFeature feature, String comment, BioObjectConfiguration conf) {
		super(new FeatureProperty(
				conf.getDefaultCVTermForClass("Comment"),
				feature.getFeature(),
				comment),
				conf);
	}

	public String getComment() {
		return featureProperty.getValue();
	}

	public void setComment(String comment) {
		featureProperty.setValue(comment);
	}
	
}
