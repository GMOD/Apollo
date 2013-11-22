package org.gmod.gbol.bioObject;

import java.sql.Timestamp;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.Organism;

/** Wrapper class representing a substitution. 
 * 
 * @author elee
 *
 */

public class Substitution extends SequenceAlteration {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public Substitution(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}
	
	/** Alternate constructor to create a Substitution object without having to pre-create the underlying
	 *  Feature object.  The constructor will take care of creating the underlying Feature object.
	 * 
	 * @param organism - Organism that this Gene belongs to
	 * @param uniqueName - String representing the unique name for this Substitution
	 * @param analysis - boolean flag for whether this feature is a result of an analysis
	 * @param obsolete - boolean flag for whether this feature is obsolete
	 * @param dateAccessioned - Timestamp for when this feature was first accessioned
	 * @param conf - Configuration containing mapping information
	 */
	public Substitution(Organism organism, String uniqueName, boolean analysis,
			boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
		super(new Feature(
				conf.getDefaultCVTermForClass("Substitution"),
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
}
