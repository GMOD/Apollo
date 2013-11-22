package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

/**Wrapper class representing a frameshift.
 * 
 * @author elee
 *
 */

public abstract class Frameshift extends TranscriptAttribute {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param featureProperty - FeatureProperty object that this object will wrap
	 * @param conf - Configuration containing mapping information
	 */
	public Frameshift(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(featureProperty, conf);
	}
	
	/** Get the coordinate for the frameshift.
	 * 
	 * @return Coordinate for the frameshift
	 */
	public int getCoordinate() {
		return Integer.parseInt(featureProperty.getValue());
	}

	/** Returns whether this frameshift is in the plus translational direction.
	 * 
	 * @return true if the frameshift is in the plus translational direction
	 */
	public abstract boolean isPlusFrameshift();

	/** Get the frameshift value.
	 * 
	 * @return Frameshift value
	 */
	public abstract int getFrameshiftValue();
}
