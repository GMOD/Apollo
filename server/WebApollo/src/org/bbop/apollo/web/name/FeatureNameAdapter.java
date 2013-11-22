package org.bbop.apollo.web.name;

import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;

public class FeatureNameAdapter extends AbstractNameAdapter {

	@Override
	public String generateUniqueName() {
		return null;
	}
	
	@Override
	public String generateName(Feature feature) {
		return generateName(feature.getFeatureLocations().iterator().next());
	}

	@Override
	public String generateName(AbstractSingleLocationBioFeature feature) {
		return generateName(feature.getFeatureLocation());
	}
	
	private String generateName(FeatureLocation loc) {
		return String.format("%s:%d-%d", loc.getSourceFeature().getUniqueName(), loc.getFmin(), loc.getFmax());
	}
}
