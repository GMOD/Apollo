package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

public class ReadthroughStopCodon extends AbstractBioFeatureProperty {

	private static final long serialVersionUID = 1L;

	public ReadthroughStopCodon(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(featureProperty, conf);
	}
	
	public ReadthroughStopCodon(AbstractBioFeature feature, BioObjectConfiguration conf) {
		super(new FeatureProperty(
				conf.getDefaultCVTermForClass("ReadthroughStopCodon"),
				feature.getFeature(),
				"true"),
				conf);
	}

}
