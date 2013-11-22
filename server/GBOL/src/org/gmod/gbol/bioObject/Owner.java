package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

public class Owner extends AbstractBioFeatureProperty {

	private static final long serialVersionUID = 1L;

	public Owner(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(featureProperty, conf);
	}
	
	public Owner(AbstractBioFeature feature, String owner, BioObjectConfiguration conf) {
		super(new FeatureProperty(
				conf.getDefaultCVTermForClass("Owner"),
				feature.getFeature(),
				owner),
				conf);
	}

	public String getOwner() {
		return featureProperty.getValue();
	}

	public void setOwner(String owner) {
		featureProperty.setValue(owner);
	}
	
}
