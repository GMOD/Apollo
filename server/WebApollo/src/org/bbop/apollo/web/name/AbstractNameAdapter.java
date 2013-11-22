package org.bbop.apollo.web.name;

import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.simpleObject.Feature;

public abstract class AbstractNameAdapter {

	public abstract String generateUniqueName();
	
	public String generateName(Feature feature) {
		return generateUniqueName();
	}
	
	public String generateName(AbstractSingleLocationBioFeature feature) {
		return generateUniqueName();
	}
	
}
