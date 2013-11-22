package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.FeatureProperty;

/**Wrapper class representing a minus 2 frameshift.
 * 
 * @author elee
 *
 */

public class Minus2Frameshift extends Frameshift {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param featureProperty - FeatureProperty object that this object will wrap
	 * @param conf - Configuration containing mapping information
	 */
	public Minus2Frameshift(FeatureProperty featureProperty, BioObjectConfiguration conf) {
		super(featureProperty, conf);
	}
	
	/** Alternate constructor to create a Plus1Frameshift object without having to pre-create the underlying
	 *  FeatureProperty object.  The constructor will take care of creating the underlying FeatureProperty
	 *  object.
	 * 
	 * @param transcript - the AbstractBioFeature where the frameshift is located
	 * @param coordinate - coordinate of the frameshift
	 * @param conf - Configuration containing mapping information
	 */
	public Minus2Frameshift(Transcript transcript, int coordinate, BioObjectConfiguration conf) {
		super(new FeatureProperty(conf.getDefaultCVTermForClass("Minus2Frameshift"),
				transcript.getFeature(), Integer.toString(coordinate)), conf);
	}

	@Override
	public int getFrameshiftValue() {
		return -2;
	}

	@Override
	public boolean isPlusFrameshift() {
		return false;
	}

}
