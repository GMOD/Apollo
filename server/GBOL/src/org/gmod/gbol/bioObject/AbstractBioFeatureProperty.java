package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

/** Abstract class for all Bio objects that wrap a FeatureProperty object.
 * 
 * @author elee
 *
 */

public abstract class AbstractBioFeatureProperty extends AbstractBioObject {

	private static final long serialVersionUID = 1L;
	protected FeatureProperty featureProperty;

	/** Constructor.
	 * 
	 * @param featureProperty - FeatureProperty object that this object will wrap
	 * @param conf - Configuration containing mapping information
	 */
	public AbstractBioFeatureProperty(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(conf);
		this.featureProperty = featureProperty;
	}

	/** Friendly method for getting the GSOL feature property.
	 * 
	 * @return Wrapped feature object
	 */
	FeatureProperty getFeatureProperty() {
		return featureProperty;
	}
	
	@Override
	public AbstractSimpleObjectIterator getWriteableSimpleObjects(
			BioObjectConfiguration conf) {
		// TODO Auto-generated method stub
		return null;
	}

}
