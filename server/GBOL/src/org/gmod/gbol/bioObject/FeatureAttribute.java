package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

/**Wrapper class representing a generic feature attribute.
 * 
 * @author elee
 *
 */

public class FeatureAttribute extends SequenceAttribute {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param featureProperty - FeatureProperty object that this object will wrap
	 * @param conf - Configuration containing mapping information
	 */
	public FeatureAttribute(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(featureProperty, conf);
	}

}
