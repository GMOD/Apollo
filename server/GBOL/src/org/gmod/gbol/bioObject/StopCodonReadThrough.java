package org.gmod.gbol.bioObject;

import java.sql.Timestamp;
import java.util.Collection;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.Organism;

/** Wrapper class representing a stop codon read through. 
 * 
 * @author elee
 *
 */

public class StopCodonReadThrough extends AbstractSingleLocationBioFeature {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public StopCodonReadThrough(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}
	
	/** Alternate constructor to create a StopCodonReadThrough object without having to pre-create the underlying
	 *  Feature object.  The constructor will take care of creating the underlying Feature object.
	 * 
	 * @param organism - Organism that this Gene belongs to
	 * @param uniqueName - String representing the unique name for this StopCodonReadThrough
	 * @param analysis - boolean flag for whether this feature is a result of an analysis
	 * @param obsolete - boolean flag for whether this feature is obsolete
	 * @param dateAccessioned - Timestamp for when this feature was first accessioned
	 * @param conf - Configuration containing mapping information
	 */
	public StopCodonReadThrough(Organism organism, String uniqueName, boolean analysis,
			boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
		super(new Feature(
				conf.getDefaultCVTermForClass("StopCodonReadThrough"),
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

	/** Retrieve the cds that this stop codon read through is associated with.  Uses the configuration to
	 * determine which parent is a cds.  The cds object is generated on the fly.  Returns
	 * <code>null</code> if this splice site is not associated with any cds.
	 * 
	 * @return CDS that this stop codon read through site is associated with
	 */
	public CDS getCDS() {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> geneCvterms = conf.getDescendantCVTermsForClass("CDS");
		for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!geneCvterms.contains(fr.getObjectFeature().getType())) {
				continue;
			}
			return ((CDS)BioObjectUtil.createBioObject(fr.getObjectFeature(), conf));
		}
		return null;
	}

}
