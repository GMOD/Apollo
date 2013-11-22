package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;

public class SequenceFeature extends AbstractBioFeature {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public SequenceFeature(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}

}
