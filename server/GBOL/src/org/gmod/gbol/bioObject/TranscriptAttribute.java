package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

/**Wrapper class representing a transcript attribute.
 * 
 * @author elee
 *
 */

public class TranscriptAttribute extends FeatureAttribute {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param featureProperty - FeatureProperty object that this object will wrap
	 * @param conf - Configuration containing mapping information
	 */
	public TranscriptAttribute(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(featureProperty, conf);
	}
	
}
