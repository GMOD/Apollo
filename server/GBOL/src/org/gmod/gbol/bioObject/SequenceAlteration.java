package org.gmod.gbol.bioObject;

import java.sql.Timestamp;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.Organism;

/** Wrapper class representing a generic sequence alteration. 
 * 
 * @author elee
 *
 */
public class SequenceAlteration extends AbstractSingleLocationBioFeature {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public SequenceAlteration(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}

	/** Alternate constructor to create a SequenceAlteration object without having to pre-create the
	 *  underlying Feature object.  The constructor will take care of creating the underlying Feature
	 *  object.
	 * 
	 * @param organism - Organism that this Gene belongs to
	 * @param uniqueName - String representing the unique name for this SequenceAlteration
	 * @param analysis - boolean flag for whether this feature is a result of an analysis
	 * @param obsolete - boolean flag for whether this feature is obsolete
	 * @param dateAccessioned - Timestamp for when this feature was first accessioned
	 * @param conf - Configuration containing mapping information
	 */
	public SequenceAlteration(Organism organism, String uniqueName, boolean analysis,
			boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
		super(new Feature(
				conf.getDefaultCVTermForClass("SequenceAlteration"),
				null,
				organism,
				null,
				uniqueName,
				null,
				null,
				null,
				analysis,
				obsolete,
				dateAccessioned,
				null),
				conf);
	}
	
	/** Get the offset added by the sequence alteration.
	 * 
	 * @return Offset added by the sequence alteration
	 */
	public int getOffset() {
		return 0;
	}

}
