package org.gmod.gbol.bioObject;

import java.sql.Timestamp;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;

/** Wrapper class representing a non cononical 5' splice site. 
 * 
 * @author elee
 *
 */

public class NonCanonicalFivePrimeSpliceSite extends SpliceSite {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public NonCanonicalFivePrimeSpliceSite(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}
	
	/** Copy constructor.
	 * 
	 * @param spliceSite - NonCanonicalFivePrimeSpliceSite to create the copy from
	 * @param uniqueName - String representing the unique name for this non canonical 5' splice site
	 */
	public NonCanonicalFivePrimeSpliceSite(NonCanonicalFivePrimeSpliceSite spliceSite, String uniqueName) {
		this(spliceSite.getFeature().getOrganism(), uniqueName, spliceSite.getFeature().isIsAnalysis(), spliceSite.getFeature().isIsObsolete(),
				new Timestamp(spliceSite.getFeature().getTimeAccessioned().getTime()), spliceSite.conf);
		feature.addFeatureLocation(new FeatureLocation(spliceSite.getFeatureLocation()));
	}

	
	/** Alternate constructor to create a Chromosome object without having to pre-create the underlying
	 *  Feature object.  The constructor will take care of creating the underlying Feature object.
	 * 
	 * @param organism - Organism that this Gene belongs to
	 * @param uniqueName - String representing the unique name for this Chromosome
	 * @param analysis - boolean flag for whether this feature is a result of an analysis
	 * @param obsolete - boolean flag for whether this feature is obsolete
	 * @param dateAccessioned - Timestamp for when this feature was first accessioned
	 * @param conf - Configuration containing mapping information
	 */
	public NonCanonicalFivePrimeSpliceSite(Organism organism, String uniqueName, boolean analysis,
			boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
		super(new Feature(
				conf.getDefaultCVTermForClass("NonCanonicalFivePrimeSpliceSite"),
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
