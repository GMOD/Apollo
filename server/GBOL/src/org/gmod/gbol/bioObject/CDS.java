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

/** Wrapper class representing a coding region.
 * 
 * @author elee
 *
 */

public class CDS extends AbstractSingleLocationBioFeature {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public CDS(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}
	
	/** Alternate constructor to create a CDS object without having to pre-create the underlying
	 *  Feature object.  The constructor will take care of creating the underlying Feature object.
	 * 
	 * @param organism - Organism that this Gene belongs to
	 * @param uniqueName - String representing the unique name for this Transcript
	 * @param analysis - boolean flag for whether this feature is a result of an analysis
	 * @param obsolete - boolean flag for whether this feature is obsolete
	 * @param dateAccessioned - Timestamp for when this feature was first accessioned
	 * @param conf - Configuration containing mapping information
	 */
    public CDS(Organism organism, String uniqueName, boolean analysis,
            boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
    	super(new Feature(
    			conf.getDefaultCVTermForClass("CDS"),
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
    
	/** Copy constructor.
	 * 
	 * @param cds - CDS to create the copy from
	 * @param uniqueName - String representing the unique name for this CDS
	 */
    public CDS(CDS cds, String uniqueName) {
    	this(cds.getOrganism(), uniqueName, cds.isAnalysis(), cds.isObsolete(),
    			new Timestamp(cds.getFeature().getTimeAccessioned().getTime()), cds.getConfiguration());
    	setFeatureLocation(new FeatureLocation(cds.getFeatureLocation()));
    }
    
	/** Retrieve the transcript that this CDS is associated with.  Uses the configuration to
	 * determine which parent is a transcript.  The transcript object is generated on the fly.  Returns
	 * <code>null</code> if this CDS is not associated with any transcript.
	 * 
	 * @return Transcript that this CDS is associated with
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

	@Override
	public int convertLocalCoordinateToSourceCoordinate(int localCoordinate) {
		if (getTranscript() == null) {
			return super.convertLocalCoordinateToSourceCoordinate(localCoordinate);
		}
		Transcript transcript = getTranscript();
		int currentOffset = 0;
		for (Exon exon : BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons())) {
			if (!overlaps(exon)) {
				currentOffset += exon.getLength();
				continue;
			}
			if (getFeatureLocation().getStrand() == -1) {
				currentOffset += exon.getFeatureLocation().getFmax() - getFeatureLocation().getFmax();
			}
			else {
				currentOffset += getFeatureLocation().getFmin() - exon.getFeatureLocation().getFmin();
			}
			break;
		}
		return transcript.convertLocalCoordinateToSourceCoordinate(localCoordinate + currentOffset);
	}
	
	@Override
	public int convertSourceCoordinateToLocalCoordinate(int sourceCoordinate) {
		if (getTranscript() == null) {
			return super.convertSourceCoordinateToLocalCoordinate(sourceCoordinate);
		}
		Transcript transcript = getTranscript();
		int currentOffset = 0;
		for (Exon exon : BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons())) {
			if (!overlaps(exon)) {
				currentOffset += exon.getLength();
				continue;
			}
			if (getFeatureLocation().getStrand() == -1) {
				currentOffset += exon.getFeatureLocation().getFmax() - getFeatureLocation().getFmax();
			}
			else {
				currentOffset += getFeatureLocation().getFmin() - exon.getFeatureLocation().getFmin();
			}
			break;
		}
		return transcript.convertSourceCoordinateToLocalCoordinate(sourceCoordinate) - currentOffset;
	}
    
	@Override
    public String getResidues() {
    	if (getTranscript() == null) {
    		return super.getResidues();
    	}
    	Transcript transcript = getTranscript();
    	String residues = transcript.getResidues();
    	int begin;
    	int end;
    	transcript.convertSourceCoordinateToLocalCoordinate(getFeatureLocation().getFmax());
    	if (getFeatureLocation().getStrand() == -1) {
    		end = transcript.convertSourceCoordinateToLocalCoordinate(getFeatureLocation().getFmin()) + 1;
    		begin = transcript.convertSourceCoordinateToLocalCoordinate(getFeatureLocation().getFmax()) + 1;
    	}
    	else {
    		begin = transcript.convertSourceCoordinateToLocalCoordinate(getFeatureLocation().getFmin());
    		end = transcript.convertSourceCoordinateToLocalCoordinate(getFeatureLocation().getFmax());

    	}
    	return residues.substring(begin, end);
    	/*
		StringBuilder residues = new StringBuilder();
		for (Exon exon : BioObjectUtil.createSortedFeatureListByLocation(getTranscript().getExons())) {
			if (!overlaps(exon)) {
				continue;
			}
			if (getFeatureLocation().getFmin() <= exon.getFeatureLocation().getFmin() &&
					getFeatureLocation().getFmax() >= exon.getFeatureLocation().getFmax()) {
				residues.append(exon.getResidues());
			}
			else if (getFeatureLocation().getFmin() <= exon.getFeatureLocation().getFmin()) {
				
				String exonResidues = exon.getResidues();
				
				residues.append(exon.getResidues().substring(0, exon.getLength() - (exon.getFeatureLocation().getFmax() -
						getFeatureLocation().getFmax())));
			}
			else {
				residues.append(exon.getResidues().substring(getFeatureLocation().getFmin() -
						exon.getFeatureLocation().getFmin()));
			}
		}
		return residues.toString();
		*/
    }
	
	@Override
	public boolean overlaps(AbstractSingleLocationBioFeature other) {
		return overlaps(other.getFeatureLocation(), true);
	}

	@Override
	public boolean overlaps(AbstractSingleLocationBioFeature other, boolean compareStrands) {
		return overlaps(other.getFeatureLocation(), compareStrands);
	}
	
	@Override
	public boolean overlaps(FeatureLocation location) {
		return overlaps(location, true);
	}
	
	@Override
	public boolean overlaps(FeatureLocation location, boolean compareStrands) {
		boolean overlaps = super.overlaps(location, compareStrands);
		if (overlaps && getTranscript() != null) {
			overlaps = getTranscript().overlaps(location, compareStrands);
		}
		return overlaps;
	}
	
	public StopCodonReadThrough getStopCodonReadThrough() {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> cdsCvterms = conf.getCVTermsForClass("StopCodonReadThrough");
		
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!cdsCvterms.contains(fr.getSubjectFeature().getType())) {
				continue;
			}
			return new StopCodonReadThrough(fr.getSubjectFeature(), conf);
		}
		return null;
	}

	public void setStopCodonReadThrough(StopCodonReadThrough stopCodonReadThrough) {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> stopCodonReadThroughCvterms = conf.getDescendantCVTermsForClass("StopCodonReadThrough");
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!stopCodonReadThroughCvterms.contains(fr.getObjectFeature().getType())) {
				continue;
			}
			fr.setSubjectFeature(stopCodonReadThrough.getFeature());
			return;
		}

		FeatureRelationship fr = new FeatureRelationship(
				conf.getDefaultCVTermForClass("PartOf"),
				this.feature,
				stopCodonReadThrough.getFeature(),
				0 // TODO: Do we need to rank the order of any other transcripts?
		);
		feature.getChildFeatureRelationships().add(fr);
		stopCodonReadThrough.getFeature().getParentFeatureRelationships().add(fr);
	}
	
	public void deleteStopCodonReadThrough(StopCodonReadThrough stopCodonReadThrough) {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> nonCanonicalFivePrimeSpliceSiteCvterms = conf.getCVTermsForClass("StopCodonReadThrough");
		Collection<CVTerm> transcriptCvterms = conf.getCVTermsForClass("Transcript");

		// delete transcript -> non canonical 5' splice site child relationship
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!nonCanonicalFivePrimeSpliceSiteCvterms.contains(fr.getSubjectFeature().getType())) {
				continue;
			}
			if (fr.getSubjectFeature().equals(stopCodonReadThrough.getFeature())) {
				boolean ok = feature.getChildFeatureRelationships().remove(fr);
				break;
			}
		}
		
		// delete transcript -> non canonical 5' splice site parent relationship
		for (FeatureRelationship fr : stopCodonReadThrough.getFeature().getParentFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!transcriptCvterms.contains(fr.getObjectFeature().getType())) {
				continue;
			}
			if (fr.getSubjectFeature().equals(stopCodonReadThrough.getFeature())) {
				boolean ok = stopCodonReadThrough.getFeature().getParentFeatureRelationships().remove(fr);
				break;
			}
		}

	}
	
	public void deleteStopCodonReadThrough() {
		StopCodonReadThrough stopCodonReadThrough = getStopCodonReadThrough();
		if (stopCodonReadThrough != null) {
			deleteStopCodonReadThrough(stopCodonReadThrough);
		}
	}


}
