package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureRelationship;

/** Wrapper class representing a 'part_of' feature relationship.
 * 
 * @author elee
 *
 */
public class PartOf extends AbstractBioFeatureRelationship {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param fr - FeatureRelationship object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public PartOf(FeatureRelationship fr, BioObjectConfiguration conf) {
		super(fr, conf);
	}
	
}
