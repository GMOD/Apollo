package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

public class Symbol extends AbstractBioFeatureProperty {

	private static final long serialVersionUID = 1L;

	public Symbol(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(featureProperty, conf);
	}
	
	public Symbol(AbstractBioFeature feature, String comment, BioObjectConfiguration conf) {
		super(new FeatureProperty(
				conf.getDefaultCVTermForClass("Symbol"),
				feature.getFeature(),
				comment),
				conf);
	}

	public String getSymbol() {
		return featureProperty.getValue();
	}

	public void setSymbol(String symbol) {
		featureProperty.setValue(symbol);
	}
	
}
