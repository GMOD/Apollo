package org.gmod.gbol.bioObject;

import java.sql.Timestamp;
import java.util.Collection;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.Organism;

/** Wrapper class representing an intron.
 * 
 * @author elee
 *
 */

public class Intron extends AbstractSingleLocationBioFeature {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public Intron(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}
	
	/** Copy constructor.
	 * 
	 * @param intron - Intron to create the copy from
	 * @param uniqueName - String representing the unique name for this Intron
	 */
	public Intron(Intron intron, String uniqueName) {
		this(intron.getFeature().getOrganism(), uniqueName, intron.getFeature().isIsAnalysis(), intron.getFeature().isIsObsolete(),
				new Timestamp(intron.getFeature().getTimeAccessioned().getTime()), intron.conf);
		feature.addFeatureLocation(new FeatureLocation(intron.getFeatureLocation()));
	}

	/** Alternate constructor to create an Intron object without having to pre-create the underlying
	 *  Feature object.  The constructor will take care of creating the underlying Feature object.
	 * 
	 * @param organism - Organism that this Gene belongs to
	 * @param uniqueName - String representing the unique name for this Intron
	 * @param analysis - boolean flag for whether this feature is a result of an analysis
	 * @param obsolete - boolean flag for whether this feature is obsolete
	 * @param dateAccessioned - Timestamp for when this feature was first accessioned
	 * @param conf - Configuration containing mapping information
	 */
	public Intron(Organism organism, String uniqueName, boolean analysis,
			boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
		super(new Feature(
				conf.getDefaultCVTermForClass("Intron"),
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
	
	/** Retrieve the transcript that this intron is associated with.  Uses the configuration to
	 * determine which parent is a transcript.  The transcript object is generated on the fly.  Returns
	 * <code>null</code> if this intron is not associated with any transcript.
	 * 
	 * @return Transcript that this Intron is associated with
	 */
	public Transcript getTranscript() {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> geneCvterms = conf.getDescendantCVTermsForClass("Transcript");
		for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!geneCvterms.contains(fr.getObjectFeature().getType())) {
				continue;
			}
			return ((Transcript)BioObjectUtil.createBioObject(fr.getObjectFeature(), conf));
		}
		return null;
	}
	
	/** Set the transcript that this intron is associated with.  Uses the configuration to
	 * determine which parent is a transcript.  If the intron is already associated with a transcript,
	 * updates that association.  Otherwise, it creates a new association.
	 * 
	 * @param transcript - Transcript that this transcript will be associated with
	 */
	public void setTranscript(Transcript transcript) {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> geneCvterms = conf.getDescendantCVTermsForClass("Transcript");
		for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!geneCvterms.contains(fr.getObjectFeature().getType())) {
				continue;
			}
			fr.setObjectFeature(transcript.getFeature());
			return;
		}

		FeatureRelationship fr = new FeatureRelationship(
				conf.getDefaultCVTermForClass("PartOf"),
				transcript.getFeature(),
				this.feature,
				0 // TODO: Do we need to rank the order of any other transcripts?
		);
		feature.getParentFeatureRelationships().add(fr);
		transcript.getFeature().getChildFeatureRelationships().add(fr);
	}

}
