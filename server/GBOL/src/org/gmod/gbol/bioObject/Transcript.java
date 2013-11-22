package org.gmod.gbol.bioObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureProperty;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.Organism;

/** Wrapper class representing a transcript.
 * 
 * @author elee
 *
 */

public class Transcript extends AbstractSingleLocationBioFeature {

//	private static final long serialVersionUID = 1186452030717911835L;
	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public Transcript(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}
	
	/** Copy constructor.
	 * 
	 * @param transcript - Transcript to create the copy from
	 * @param uniqueName - String representing the unique name for this Transcript
	 */
	public Transcript(Transcript transcript, String uniqueName) {
		this(transcript.getFeature().getOrganism(), uniqueName, transcript.getFeature().isIsAnalysis(),
				transcript.getFeature().isIsObsolete(),
				new Timestamp(transcript.getFeature().getTimeAccessioned().getTime()), transcript.getConfiguration());
		feature.addFeatureLocation(new FeatureLocation(transcript.getFeatureLocation()));
	}

	/** Alternate constructor to create a Transcript object without having to pre-create the underlying
	 *  Feature object.  The constructor will take care of creating the underlying Feature object.
	 * 
	 * @param organism - Organism that this Gene belongs to
	 * @param uniqueName - String representing the unique name for this Transcript
	 * @param analysis - boolean flag for whether this feature is a result of an analysis
	 * @param obsolete - boolean flag for whether this feature is obsolete
	 * @param dateAccessioned - Timestamp for when this feature was first accessioned
	 * @param conf - Configuration containing mapping information
	 */
    public Transcript(Organism organism, String uniqueName, boolean analysis,
            boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
    	super(new Feature(
    			conf.getDefaultCVTermForClass("Transcript"),
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
	
	/** Retrieve all the exons associated with this transcript.  Uses the configuration to determine
	 *  which children are exons.  Exon objects are generated on the fly.  The collection
	 *  will be empty if there are no exons associated with the transcript.
	 *  
	 * @return Collection of exons associated with this transcript
	 */
	public Collection<Exon> getExons() {
		Collection<Exon> exons = new ArrayList<Exon>();
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> exonCvterms = conf.getCVTermsForClass("Exon");
		
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!exonCvterms.contains(fr.getSubjectFeature().getType())) {
				continue;
			}
			exons.add(new Exon(fr.getSubjectFeature(), conf));
		}
		return exons;
	}
	
	/** Retrieve the CDS associated with this transcript.  Uses the configuration to determine
	 *  which child is a CDS.  The CDS object is generated on the fly.  Returns <code>null</code>
	 *  if no CDS is associated.
	 *  
	 * @return CDS associated with this transcript
	 */
	public CDS getCDS() {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> cdsCvterms = conf.getCVTermsForClass("CDS");
		
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!cdsCvterms.contains(fr.getSubjectFeature().getType())) {
				continue;
			}
			return new CDS(fr.getSubjectFeature(), conf);
		}
		return null;
	}
	
	public void deleteCDS() {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> cdsCvterms = conf.getCVTermsForClass("CDS");
		
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!cdsCvterms.contains(fr.getSubjectFeature().getType())) {
				continue;
			}
			feature.getChildFeatureRelationships().remove(fr);
			break;
		}
	}
	
	/** Set the CDS associated with this transcript.  Uses the configuration to determine
	 *  the default term to use for CDS features.
	 *  
	 * @param cds - CDS to be set to this transcript
	 */
	public void setCDS(CDS cds) {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> geneCvterms = conf.getDescendantCVTermsForClass("CDS");
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!geneCvterms.contains(fr.getObjectFeature().getType())) {
				continue;
			}
			fr.setSubjectFeature(cds.getFeature());
			return;
		}

		FeatureRelationship fr = new FeatureRelationship(
				conf.getDefaultCVTermForClass("PartOf"),
				this.feature,
				cds.getFeature(),
				0 // TODO: Do we need to rank the order of any other transcripts?
		);
		feature.getChildFeatureRelationships().add(fr);
		cds.getFeature().getParentFeatureRelationships().add(fr);
	}
	
	/** Retrieve the gene that this transcript is associated with.  Uses the configuration to
	 * determine which parent is a gene.  The gene object is generated on the fly.  Returns
	 * <code>null</code> if this transcript is not associated with any gene.
	 * 
	 * @return Gene that this Transcript is associated with
	 */
	public Gene getGene() {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> geneCvterms = conf.getDescendantCVTermsForClass("Gene");
		for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!geneCvterms.contains(fr.getObjectFeature().getType())) {
				continue;
			}
			return ((Gene)BioObjectUtil.createBioObject(fr.getObjectFeature(), conf));
		}
		return null;
	}
	
	/** Set the gene that this transcript is associated with.  Uses the configuration to
	 * determine which parent is a gene.  If the transcript is already associated with a gene,
	 * updates that association.  Otherwise, it creates a new association.
	 * 
	 * @param gene - Gene that this transcript will be associated with
	 */
	public void setGene(Gene gene) {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> geneCvterms = conf.getDescendantCVTermsForClass("Gene");
		for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!geneCvterms.contains(fr.getObjectFeature().getType())) {
				continue;
			}
			fr.setObjectFeature(gene.getFeature());
			return;
		}

		FeatureRelationship fr = new FeatureRelationship(
				conf.getDefaultCVTermForClass("PartOf"),
				gene.getFeature(),
				this.feature,
				0 // TODO: Do we need to rank the order of any other transcripts?
		);
		feature.getParentFeatureRelationships().add(fr);
		gene.getFeature().getChildFeatureRelationships().add(fr);
	}
	
	/** Delete an exon.  Deletes both the transcript -> exon and exon -> transcript
	 *  relationships.
	 *  
	 * @param exon - Exon to be deleted
	 */
	public void deleteExon(Exon exon) {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> exonCvterms = conf.getCVTermsForClass("Exon");
		Collection<CVTerm> transcriptCvterms = conf.getCVTermsForClass("Transcript");

		// delete transcript -> exon child relationship
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!exonCvterms.contains(fr.getSubjectFeature().getType())) {
				continue;
			}
			if (fr.getSubjectFeature().equals(exon.getFeature())) {
				boolean ok = feature.getChildFeatureRelationships().remove(fr);
				break;
			}
		}
		
		// delete transcript -> exon parent relationship
		for (FeatureRelationship fr : exon.getFeature().getParentFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!transcriptCvterms.contains(fr.getObjectFeature().getType())) {
				continue;
			}
			if (fr.getSubjectFeature().equals(exon.getFeature())) {
				boolean ok = exon.getFeature().getParentFeatureRelationships().remove(fr);
				break;
			}
		}

	}
	
	/** Get the number of exons.
	 * 
	 * @return Number of exons
	 */
	public int getNumberOfExons() {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> exonCvterms = conf.getCVTermsForClass("Exon");
		int numExons = 0;

		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!exonCvterms.contains(fr.getSubjectFeature().getType())) {
				continue;
			}
			++numExons;
		}
		return numExons;
	}

	/** Add an exon.  If the exon's bounds are beyond the transcript's bounds, the transcript's bounds
	 *  are adjusted accordingly.  Sets the exon's transcript to this transcript object.
	 * 
	 * @param exon - Exon to be added
	 */
	public void addExon(Exon exon) {
		CVTerm partOfCvterm = conf.getDefaultCVTermForClass("PartOf");

		// if the exon's bounds are beyond the transcript's bounds, need to adjust the transcript's bounds
		if (exon.getFeatureLocation().getFmin() < getFeatureLocation().getFmin()) {
			getFeatureLocation().setFmin(exon.getFeatureLocation().getFmin());
		}
		if (exon.getFeatureLocation().getFmax() > getFeatureLocation().getFmax()) {
			getFeatureLocation().setFmax(exon.getFeatureLocation().getFmax());
		}
		
		// if the transcript's bounds are beyond the gene's bounds, need to adjust the gene's bounds
		if (getGene() != null) {
			if (getFmin() < getGene().getFmin()) {
				getGene().setFmin(getFmin());
			}
			if (getFmax() > getGene().getFmax()) {
				getGene().setFmax(getFmax());
			}
		}

		// add exon
		int rank = 0;
		//TODO: do we need to figure out the rank?
		feature.getChildFeatureRelationships().add(
				new FeatureRelationship(partOfCvterm, feature, exon.getFeature(), rank));
		exon.setTranscript(this);
	}
	
	/** Add a frameshift.
	 * 
	 * @param frameshift - Frameshift to be added
	 */
	public void addFrameshift(Frameshift frameshift) {
		addProperty(frameshift);
	}
	
	/** Retrieve all the frameshifts associated with this transcript.  Uses the configuration to determine
	 *  which children are frameshifts.  Frameshift objects are generated on the fly.  The collection
	 *  will be empty if there are no frameshifts associated with the transcript.
	 * 
	 * @return Collection of exons associated with this transcript
	 */
	public Collection<Frameshift> getFrameshifts() {
		Collection<Frameshift> frameshifts = new ArrayList<Frameshift>();
		Collection<CVTerm> frameshiftCvterms = conf.getDescendantCVTermsForClass("Frameshift");

		for (FeatureProperty featureProperty : feature.getFeatureProperties()) {
			if (frameshiftCvterms.contains(featureProperty.getType())) {
				frameshifts.add((Frameshift)BioObjectUtil.createBioObject(featureProperty, conf));
			}
		}
		return frameshifts;
	}
	
	/** Delete a frameshift.
	 *  
	 * @param frameshift - Frameshift to be deleted
	 */
	public void deleteFrameshift(Frameshift frameshift) {
		Collection<CVTerm> frameshiftCvterms = conf.getDescendantCVTermsForClass("Frameshift");

		for (FeatureProperty featureProperty : feature.getFeatureProperties()) {
			if (frameshiftCvterms.contains(featureProperty.getType())) {
				if (featureProperty.equals(frameshift.getFeatureProperty())) {
					feature.getFeatureProperties().remove(frameshift.getFeatureProperty());
					break;
				}
			}
		}
	}
	
	@Override
	public String getResidues() {
		if (feature.getResidues() != null) {
			return feature.getResidues();
		}
		if (getExons() == null || getExons().size() == 0) {
			return super.getResidues();
		}
		StringBuilder residues = new StringBuilder();
		List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(getExons());
		for (Exon exon : exons) {
			if (exon.getResidues() != null) {
				residues.append(exon.getResidues());
			}
		}
		return residues.length() > 0 ? residues.toString() : null;
	}
	
	@Override
	public int convertLocalCoordinateToSourceCoordinate(int localCoordinate) {
		List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(getExons());
		int sourceCoordinate = -1;
		if (exons.size() == 0) {
			return super.convertLocalCoordinateToSourceCoordinate(localCoordinate);
		}
		int currentLength = 0;
		int currentCoordinate = localCoordinate;
		for (Exon exon : exons) {
			int exonLength = exon.getLength();
			if (currentLength + exonLength >= localCoordinate) {
				if (getFeatureLocation().getStrand() == -1) {
					sourceCoordinate = exon.getFeatureLocation().getFmax() - currentCoordinate - 1;
				}
				else {
					sourceCoordinate = exon.getFeatureLocation().getFmin() + currentCoordinate;
				}
				break;
			}
			currentLength += exonLength;
			currentCoordinate -= exonLength;
		}
		return sourceCoordinate;
	}
	
	@Override
	public int convertSourceCoordinateToLocalCoordinate(int sourceCoordinate) {
		List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(getExons());
		int localCoordinate = -1;
		if (exons.size() == 0) {
			return super.convertSourceCoordinateToLocalCoordinate(sourceCoordinate);
		}
		int currentCoordinate = 0;
		for (Exon exon : exons) {
			if (exon.getFeatureLocation().getFmin() <= sourceCoordinate &&
					exon.getFeatureLocation().getFmax() >= sourceCoordinate) {
				if (getFeatureLocation().getStrand() == -1) {
					localCoordinate = currentCoordinate + (exon.getFeatureLocation().getFmax() - sourceCoordinate) - 1;
				}
				else {
					localCoordinate = currentCoordinate + (sourceCoordinate - exon.getFeatureLocation().getFmin());
				}
			}
			currentCoordinate += exon.getLength();
		}
		return localCoordinate;
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
		for (Exon exon : getExons()) {
			if (exon.overlaps(location, compareStrands)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void setFmin(Integer fmin) {
		super.setFmin(fmin);
		if (getGene() != null && fmin < getGene().getFmin()) {
			getGene().setFmin(fmin);
		}
	}

	@Override
	public void setFmax(Integer fmax) {
		super.setFmax(fmax);
		if (getGene() != null && fmax > getGene().getFmax()) {
			getGene().setFmax(fmax);
		}
	}
	
	/** Retrieve all the non canonical 5' splice sites associated with this transcript.  Uses the configuration to determine
	 *  which children are non canonical 5' splice sites.  Non canonical 5' splice site objects are generated on the fly.
	 *  The collection will be empty if there are no non canonical 5' splice sites associated with the transcript.
	 *  
	 * @return Collection of non canonical 5' splice sites associated with this transcript
	 */
	public Collection<NonCanonicalFivePrimeSpliceSite> getNonCanonicalFivePrimeSpliceSites() {
		Collection<NonCanonicalFivePrimeSpliceSite> nonCanonicalFivePrimeSpliceSites = new ArrayList<NonCanonicalFivePrimeSpliceSite>();
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> nonCanonicalFivePrimeSpliceSiteCvterms = conf.getCVTermsForClass("NonCanonicalFivePrimeSpliceSite");
		
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!nonCanonicalFivePrimeSpliceSiteCvterms.contains(fr.getSubjectFeature().getType())) {
				continue;
			}
			nonCanonicalFivePrimeSpliceSites.add(new NonCanonicalFivePrimeSpliceSite(fr.getSubjectFeature(), conf));
		}
		return nonCanonicalFivePrimeSpliceSites;
	}
	
	/** Add a non canonical 5' splice site.  Sets the splice site's transcript to this transcript object.
	 * 
	 * @param nonCanonicalFivePrimeSpliceSite - Non canonical 5' splice site to be added
	 */
	public void addNonCanonicalFivePrimeSpliceSite(NonCanonicalFivePrimeSpliceSite nonCanonicalFivePrimeSpliceSite) {
		CVTerm partOfCvterm = conf.getDefaultCVTermForClass("PartOf");

		// add non canonical 5' splice site
		FeatureRelationship fr = new FeatureRelationship(
				conf.getDefaultCVTermForClass("PartOf"),
				this.feature,
				nonCanonicalFivePrimeSpliceSite.getFeature(),
				0 // TODO: Do we need to rank the order of any other transcripts?
		);
		feature.getChildFeatureRelationships().add(fr);
		nonCanonicalFivePrimeSpliceSite.getFeature().getParentFeatureRelationships().add(fr);
	}

	/** Delete an non canonical 5' splice site.  Deletes both the transcript -> non canonical 5' splice site and
	 *  non canonical 5' splice site -> transcript relationships.
	 *  
	 * @param nonCanonicalFivePrimeSpliceSite - NonCanonicalFivePrimeSpliceSite to be deleted
	 */
	public void deleteNonCanonicalFivePrimeSpliceSite(NonCanonicalFivePrimeSpliceSite nonCanonicalFivePrimeSpliceSite) {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> nonCanonicalFivePrimeSpliceSiteCvterms = conf.getCVTermsForClass("NonCanonicalFivePrimeSpliceSite");
		Collection<CVTerm> transcriptCvterms = conf.getCVTermsForClass("Transcript");

		// delete transcript -> non canonical 5' splice site child relationship
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!nonCanonicalFivePrimeSpliceSiteCvterms.contains(fr.getSubjectFeature().getType())) {
				continue;
			}
			if (fr.getSubjectFeature().equals(nonCanonicalFivePrimeSpliceSite.getFeature())) {
				boolean ok = feature.getChildFeatureRelationships().remove(fr);
				break;
			}
		}
		
		// delete transcript -> non canonical 5' splice site parent relationship
		for (FeatureRelationship fr : nonCanonicalFivePrimeSpliceSite.getFeature().getParentFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!transcriptCvterms.contains(fr.getObjectFeature().getType())) {
				continue;
			}
			if (fr.getSubjectFeature().equals(nonCanonicalFivePrimeSpliceSite.getFeature())) {
				boolean ok = nonCanonicalFivePrimeSpliceSite.getFeature().getParentFeatureRelationships().remove(fr);
				break;
			}
		}

	}
	
	/** Delete all non canonical 5' splice site.  Deletes all transcript -> non canonical 5' splice sites and
	 *  non canonical 5' splice sites -> transcript relationships.
	 *  
	 */
	public void deleteAllNonCanonicalFivePrimeSpliceSites() {
		for (NonCanonicalFivePrimeSpliceSite spliceSite : getNonCanonicalFivePrimeSpliceSites()) {
			deleteNonCanonicalFivePrimeSpliceSite(spliceSite);
		}
	}
	
	/** Retrieve all the non canonical 3' splice sites associated with this transcript.  Uses the configuration to determine
	 *  which children are non canonical 3' splice sites.  Non canonical 3' splice site objects are generated on the fly.
	 *  The collection will be empty if there are no non canonical 3' splice sites associated with the transcript.
	 *  
	 * @return Collection of non canonical 3' splice sites associated with this transcript
	 */
	public Collection<NonCanonicalThreePrimeSpliceSite> getNonCanonicalThreePrimeSpliceSites() {
		Collection<NonCanonicalThreePrimeSpliceSite> nonCanonicalThreePrimeSpliceSites = new ArrayList<NonCanonicalThreePrimeSpliceSite>();
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> nonCanonicalThreePrimeSpliceSiteCvterms = conf.getCVTermsForClass("NonCanonicalThreePrimeSpliceSite");
		
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!nonCanonicalThreePrimeSpliceSiteCvterms.contains(fr.getSubjectFeature().getType())) {
				continue;
			}
			nonCanonicalThreePrimeSpliceSites.add(new NonCanonicalThreePrimeSpliceSite(fr.getSubjectFeature(), conf));
		}
		return nonCanonicalThreePrimeSpliceSites;
	}
	
	/** Add a non canonical 3' splice site.  Sets the splice site's transcript to this transcript object.
	 * 
	 * @param nonCanonicalThreePrimeSpliceSite - Non canonical 3' splice site to be added
	 */
	public void addNonCanonicalThreePrimeSpliceSite(NonCanonicalThreePrimeSpliceSite nonCanonicalThreePrimeSpliceSite) {
		CVTerm partOfCvterm = conf.getDefaultCVTermForClass("PartOf");

		// add non canonical 3' splice site
		FeatureRelationship fr = new FeatureRelationship(
				conf.getDefaultCVTermForClass("PartOf"),
				this.feature,
				nonCanonicalThreePrimeSpliceSite.getFeature(),
				0 // TODO: Do we need to rank the order of any other transcripts?
		);
		feature.getChildFeatureRelationships().add(fr);
		nonCanonicalThreePrimeSpliceSite.getFeature().getParentFeatureRelationships().add(fr);
	}
	
	/** Delete an non canonical 3' splice site.  Deletes both the transcript -> non canonical 3' splice site and
	 *  non canonical 3' splice site -> transcript relationships.
	 *  
	 * @param nonCanonicalThreePrimeSpliceSite - NonCanonicalThreePrimeSpliceSite to be deleted
	 */
	public void deleteNonCanonicalThreePrimeSpliceSite(NonCanonicalThreePrimeSpliceSite nonCanonicalThreePrimeSpliceSite) {
		Collection<CVTerm> partOfCvterms = conf.getCVTermsForClass("PartOf");
		Collection<CVTerm> nonCanonicalThreePrimeSpliceSiteCvterms = conf.getCVTermsForClass("NonCanonicalThreePrimeSpliceSite");
		Collection<CVTerm> transcriptCvterms = conf.getCVTermsForClass("Transcript");

		// delete transcript -> non canonical 3' splice site child relationship
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!nonCanonicalThreePrimeSpliceSiteCvterms.contains(fr.getSubjectFeature().getType())) {
				continue;
			}
			if (fr.getSubjectFeature().equals(nonCanonicalThreePrimeSpliceSite.getFeature())) {
				boolean ok = feature.getChildFeatureRelationships().remove(fr);
				break;
			}
		}
		
		// delete transcript -> non canonical 3' splice site parent relationship
		for (FeatureRelationship fr : nonCanonicalThreePrimeSpliceSite.getFeature().getParentFeatureRelationships()) {
			if (!partOfCvterms.contains(fr.getType())) {
				continue;
			}
			if (!transcriptCvterms.contains(fr.getObjectFeature().getType())) {
				continue;
			}
			if (fr.getSubjectFeature().equals(nonCanonicalThreePrimeSpliceSite.getFeature())) {
				boolean ok = nonCanonicalThreePrimeSpliceSite.getFeature().getParentFeatureRelationships().remove(fr);
				break;
			}
		}

	}
	
	/** Delete all non canonical 3' splice site.  Deletes all transcript -> non canonical 3' splice sites and
	 *  non canonical 3' splice sites -> transcript relationships.
	 *  
	 */
	public void deleteAllNonCanonicalThreePrimeSpliceSites() {
		for (NonCanonicalThreePrimeSpliceSite spliceSite : getNonCanonicalThreePrimeSpliceSites()) {
			deleteNonCanonicalThreePrimeSpliceSite(spliceSite);
		}
	}
	
	public boolean isProteinCoding() {
		if (getGene() != null && getGene().isPseudogene()) {
			return false;
		}
		return true;
	}

}
