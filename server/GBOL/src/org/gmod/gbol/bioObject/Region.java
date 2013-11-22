package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;

/** Wrapper class representing a generic region.
 * 
 * @author elee
 *
 */

public class Region extends SequenceFeature {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public Region(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}

}
