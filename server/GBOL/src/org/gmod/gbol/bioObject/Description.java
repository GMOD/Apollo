package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

public class Description extends AbstractBioFeatureProperty {

	private static final long serialVersionUID = 1L;

	public Description(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(featureProperty, conf);
	}
	
	public Description(AbstractBioFeature feature, String comment, BioObjectConfiguration conf) {
		super(new FeatureProperty(
				conf.getDefaultCVTermForClass("Description"),
				feature.getFeature(),
				comment),
				conf);
	}

	public String getDescription() {
		return featureProperty.getValue();
	}

	public void setDescription(String description) {
		featureProperty.setValue(description);
	}
	
}
