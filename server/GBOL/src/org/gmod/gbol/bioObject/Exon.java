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

/** Wrapper class representing an exon.
 * 
 * @author elee
 *
 */

public class Exon extends AbstractSingleLocationBioFeature {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public Exon(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}
	
	/** Copy constructor.
	 * 
	 * @param exon - Exon to create the copy from
	 * @param uniqueName - String representing the unique name for this Exon
	 */
	public Exon(Exon exon, String uniqueName) {
		this(exon.getFeature().getOrganism(), uniqueName, exon.getFeature().isIsAnalysis(), exon.getFeature().isIsObsolete(),
				new Timestamp(exon.getFeature().getTimeAccessioned().getTime()), exon.conf);
		feature.addFeatureLocation(new FeatureLocation(exon.getFeatureLocation()));
	}

	/** Alternate constructor to create an Exon object without having to pre-create the underlying
	 *  Feature object.  The constructor will take care of creating the underlying Feature object.
	 * 
	 * @param organism - Organism that this Gene belongs to
	 * @param uniqueName - String representing the unique name for this Exon
	 * @param analysis - boolean flag for whether this feature is a result of an analysis
	 * @param obsolete - boolean flag for whether this feature is obsolete
	 * @param dateAccessioned - Timestamp for when this feature was first accessioned
	 * @param conf - Configuration containing mapping information
	 */
	public Exon(Organism organism, String uniqueName, boolean analysis,
			boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
		super(new Feature(
				conf.getDefaultCVTermForClass("Exon"),
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
	
	/** Retrieve the transcript that this exon is associated with.  Uses the configuration to
	 * determine which parent is a transcript.  The transcript object is generated on the fly.  Returns
	 * <code>null</code> if this exon is not associated with any transcript.
	 * 
	 * @return Transcript that this Exon is associated with
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
	
	/** Set the transcript that this exon is associated with.  Uses the configuration to
	 * determine which parent is a transcript.  If the exon is already associated with a transcript,
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
	
	@Override
	public void setFmin(Integer fmin) {
		super.setFmin(fmin);
		if (getTranscript() != null && fmin < getTranscript().getFmin()) {
			getTranscript().setFmin(fmin);
		}
	}

	@Override
	public void setFmax(Integer fmax) {
		super.setFmax(fmax);
		if (getTranscript() != null && fmax > getTranscript().getFmax()) {
			getTranscript().setFmax(fmax);
		}
	}

}
