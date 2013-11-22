package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

public class Status extends AbstractBioFeatureProperty {

	private static final long serialVersionUID = 1L;

	public Status(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(featureProperty, conf);
	}
	
	public Status(AbstractBioFeature feature, String comment, BioObjectConfiguration conf) {
		super(new FeatureProperty(
				conf.getDefaultCVTermForClass("Status"),
				feature.getFeature(),
				comment),
				conf);
	}

	public String getStatus() {
		return featureProperty.getValue();
	}

	public void setStatus(String status) {
		featureProperty.setValue(status);
	}
	
}
