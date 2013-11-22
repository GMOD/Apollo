package org.gmod.gbol.bioObject;

import java.sql.Timestamp;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;

/** Wrapper class representing a RepeatRegion. 
 * 
 * @author elee
 *
 */

public class RepeatRegion extends AbstractSingleLocationBioFeature {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public RepeatRegion(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}
	
	/** Copy constructor.
	 * 
	 * @param repeatRegion - RepeatRegion to create the copy from
	 * @param uniqueName - String representing the unique name for this repeatRegion
	 */
	public RepeatRegion(RepeatRegion repeatRegion, String uniqueName) {
		this(repeatRegion.getFeature().getOrganism(), uniqueName, repeatRegion.getFeature().isIsAnalysis(),
				repeatRegion.getFeature().isIsObsolete(),
				new Timestamp(repeatRegion.getFeature().getTimeAccessioned().getTime()), repeatRegion.getConfiguration());
		feature.addFeatureLocation(new FeatureLocation(repeatRegion.getFeatureLocation()));
	}

	
	/** Alternate constructor to create a RepeatRegion object without having to pre-create the underlying
	 *  Feature object.  The constructor will take care of creating the underlying Feature object.
	 * 
	 * @param organism - Organism that this Gene belongs to
	 * @param uniqueName - String representing the unique name for this RepeatRegion
	 * @param analysis - boolean flag for whether this feature is a result of an analysis
	 * @param obsolete - boolean flag for whether this feature is obsolete
	 * @param dateAccessioned - Timestamp for when this feature was first accessioned
	 * @param conf - Configuration containing mapping information
	 */
	public RepeatRegion(Organism organism, String uniqueName, boolean analysis,
			boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
		super(new Feature(
				conf.getDefaultCVTermForClass("RepeatRegion"),
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
