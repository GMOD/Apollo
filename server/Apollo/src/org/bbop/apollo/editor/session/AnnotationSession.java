package org.bbop.apollo.editor.session;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bbop.apollo.editor.AbstractAnnotationSession;
import org.gmod.gbol.bioObject.AbstractBioFeature;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.CDS;
import org.gmod.gbol.bioObject.Deletion;
import org.gmod.gbol.bioObject.Frameshift;
import org.gmod.gbol.bioObject.Insertion;
import org.gmod.gbol.bioObject.SequenceAlteration;
import org.gmod.gbol.bioObject.Substitution;
import org.gmod.gbol.bioObject.Transcript;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.util.SequenceUtil;

/** Session that holds annotations.
 * 
 * @author elee
 *
 */

public class AnnotationSession implements AbstractAnnotationSession {

	private DataStore dataStore;
	private Feature sourceFeature;
	private Organism organism;

	public AnnotationSession() {
		this(new MemoryDataStore());
	}
	
	/** Constructor.
	 * 
	 */
	public AnnotationSession(DataStore dataStore) {
		this.dataStore = dataStore;
	}
	
	/** Get the source feature associated with this session.
	 * 
	 * @return Source feature associated with this session
	 */
	public Feature getSourceFeature() {
		return sourceFeature;
	}
	
	/** Set the source feature associated with this session.
	 * 
	 * @param sourceFeature - Source feature to be associated with this session
	 */
	public void setSourceFeature(Feature sourceFeature) {
		if (sourceFeature.getOrganism() == null) {
			sourceFeature.setOrganism(getOrganism());
		}
		this.sourceFeature = sourceFeature;
	}
	
	/** Get the organism associated with this session.
	 * 
	 * @return Organism associated with this session
	 */
	public Organism getOrganism() {
		return organism;
	}
	
	/** Set the organism to be associated with this session.
	 * 
	 * @param organism - Organism to be associated with this session
	 */
	public void setOrganism(Organism organism) {
		this.organism = organism;
	}
	
	/** Add a feature to the session.  Features are always stored in sorted order by position.
	 * 
	 * @param feature - AbstractSingleLocationBioFeature to be added to the session
	 */
	public void addFeature(AbstractSingleLocationBioFeature feature) {
		dataStore.addFeature(feature);
	}
	
	/** Delete a feature from the session.
	 * 
	 * @param feature - AbstractSingleLocationBioFeature to be deleted from the session
	 */
	public void deleteFeature(AbstractSingleLocationBioFeature feature) {
		dataStore.deleteFeature(feature);
	}
	
	/** Get features.
	 * 
	 * @return Collection of AbstractSingleLocationBioFeature objects
	 */
	public Collection<AbstractSingleLocationBioFeature> getFeatures() {
		return dataStore.getFeatures();
	}
	
	/** Get a feature by unique name.  Returns null if there are no features with the unique name.
	 * 
	 * @param uniqueName - Unique name to look up the feature by
	 * @return AbstractSingleLocationBioFeature with the unique name
	 */
	public AbstractSingleLocationBioFeature getFeatureByUniqueName(String uniqueName) {
		return dataStore.getFeatureByUniqueName(uniqueName);
	}
	
	/** Get features that overlap a given location.  Compares strand as well as coordinates.
	 * 
	 * @param location - FeatureLocation that the features overlap
	 * @return Collection of AbstractSingleLocationBioFeature objects that overlap the FeatureLocation
	 */
	public Collection<AbstractSingleLocationBioFeature> getOverlappingFeatures(FeatureLocation location) {
		return getOverlappingFeatures(location, true);
	}
	
	/** Get features that overlap a given location.
	 * 
	 * @param location - FeatureLocation that the features overlap
	 * @param compareStrands - Whether to compare strands in overlap
	 * @return Collection of AbstractSingleLocationBioFeature objects that overlap the FeatureLocation
	 */
	public Collection<AbstractSingleLocationBioFeature> getOverlappingFeatures(FeatureLocation location,
			boolean compareStrands) {
		return dataStore.getOverlappingFeatures(location, compareStrands);
	}
	
	public int convertModifiedLocalCoordinateToSourceCoordinate(AbstractSingleLocationBioFeature feature,
			int localCoordinate) {
		List<SequenceAlteration> alterations = feature instanceof CDS ?
				getFrameshiftsAsAlterations(((CDS)feature).getTranscript()) : new ArrayList<SequenceAlteration>();
		alterations.addAll(dataStore.getSequenceAlterations());
		if (alterations.size() == 0) {
			return feature.convertLocalCoordinateToSourceCoordinate(localCoordinate);
		}
		Collections.sort(alterations, new BioObjectUtil.FeaturePositionComparator<SequenceAlteration>());
		if (feature.getFeatureLocation().getStrand() == -1) {
			Collections.reverse(alterations);
		}
		for (SequenceAlteration alteration : alterations) {
			if (!feature.overlaps(alteration)) {
				continue;
			}
			if (feature.getFeatureLocation().getStrand() == -1) {
				if (feature.convertSourceCoordinateToLocalCoordinate(alteration.getFeatureLocation().getFmin()) > localCoordinate) {
					localCoordinate -= alteration.getOffset();
				}
			}
			else {
				if (feature.convertSourceCoordinateToLocalCoordinate(alteration.getFeatureLocation().getFmin()) < localCoordinate) {
					localCoordinate -= alteration.getOffset();
				}
			}
		}
		return feature.convertLocalCoordinateToSourceCoordinate(localCoordinate);
	}
	
	/** Add a sequence alteration.  It uses the source feature of the alteration, which should be the source
	 *  feature of the annotations in this session, for retrieving the modified residues for an annotation.
	 *  
	 * @param sequenceAlteration - SequenceAlteration to add to the session
	 */
	public void addSequenceAlteration(SequenceAlteration sequenceAlteration) {
		dataStore.addSequenceAlteration(sequenceAlteration);
	}
	
	/** Delete a sequence alteration.
	 *  
	 * @param sequenceAlteration - SequenceAlteration to delete from the session
	 */
	public void deleteSequenceAlteration(SequenceAlteration sequenceAlteration) {
		dataStore.deleteSequenceAlteration(sequenceAlteration);
	}
	
	/** Get the sequence alterations stored in the session.
	 * 
	 * @return Collection of sequence alterations in this session
	 */
	public Collection<SequenceAlteration> getSequenceAlterations() {
		return dataStore.getSequenceAlterations();
	}
	
	/** Get the residues for a feature with any frameshifts.  If the feature is not an instance of
	 *  CDS, it will just return the raw feature.
	 *  
	 * @param feature - AbstractSingleLocationBioFeature to retrieve the annotation with any frameshifts
	 * @return Residues of the feature with frameshifts
	 */
	public String getResiduesWithFrameshifts(AbstractSingleLocationBioFeature feature) {
		if (!(feature instanceof CDS)) {
			return feature.getResidues();
		}
		CDS cds = (CDS)feature;
		Transcript transcript = cds.getTranscript();
		if (cds.getTranscript() == null) {
			return feature.getResidues();
		}
		return getResiduesWithAlterations(feature, getFrameshiftsAsAlterations(transcript));
	}
	
	/** Get the residues for a feature with any alterations and frameshifts.
	 * 
	 * @param feature - AbstractSingleLocationBioFeature to retrieve the residues for
	 * @return Residues for the feature with any alterations and frameshifts
	 */
	public String getResiduesWithAlterationsAndFrameshifts(AbstractSingleLocationBioFeature feature) {
		if (!(feature instanceof CDS)) {
			return getResiduesWithAlterations(feature);
		}
		Collection<SequenceAlteration> alterations = getFrameshiftsAsAlterations(((CDS)feature).getTranscript());
		alterations.addAll(dataStore.getSequenceAlterations());
		return getResiduesWithAlterations(feature, alterations);
	}

	/** Get the residues for a feature with any alterations.
	 * 
	 * @param feature - AbstractSingleLocationBioFeature to retrieve the residues for
	 * @return Residues for the feature with any alterations
	 */
	public String getResiduesWithAlterations(AbstractSingleLocationBioFeature feature) {
		return getResiduesWithAlterations(feature, dataStore.getSequenceAlterations());
	}
	
	public void beginTransactionForFeature(AbstractSingleLocationBioFeature feature) {
		dataStore.beginTransactionForFeature(feature);
	}
	
	public void endTransactionForFeature(AbstractSingleLocationBioFeature feature) {
		dataStore.endTransactionForFeature(feature);
	}
	
	public void endTransactionForAllFeatures() {
		dataStore.endTransactionForAllFeatures();
	}
	
	private String getResiduesWithAlterations(AbstractSingleLocationBioFeature feature,
			Collection<SequenceAlteration> sequenceAlterations) {
		if (sequenceAlterations.size() == 0) {
			return feature.getResidues();
		}
		StringBuilder residues = new StringBuilder(feature.getResidues());
		FeatureLocation featureLoc = feature.getFeatureLocation();
		List<SequenceAlteration> orderedSequenceAlterationList = BioObjectUtil.createSortedFeatureListByLocation(sequenceAlterations);
		if (!feature.getFeatureLocation().getStrand().equals(orderedSequenceAlterationList.get(0).getFeatureLocation().getStrand())) {
			Collections.reverse(orderedSequenceAlterationList);
		}
		int currentOffset = 0;
		for (SequenceAlteration sequenceAlteration : orderedSequenceAlterationList) {
			if (!feature.overlaps(sequenceAlteration, false)) {
				continue;
			}
			FeatureLocation sequenceAlterationLoc = sequenceAlteration.getFeatureLocation();
			if (sequenceAlterationLoc.getSourceFeature().equals(featureLoc.getSourceFeature())) {
				int localCoordinate = feature.convertSourceCoordinateToLocalCoordinate(sequenceAlterationLoc.getFmin());
				String sequenceAlterationResidues = sequenceAlteration.getResidues();
				if (feature.getFeatureLocation().getStrand() == -1) {
					sequenceAlterationResidues = SequenceUtil.reverseComplementSequence(sequenceAlterationResidues);
				}
				// Insertions
				if (sequenceAlteration instanceof Insertion) {
					if (feature.getFeatureLocation().getStrand() == -1) {
						++localCoordinate;
					}
					residues.insert(localCoordinate + currentOffset, sequenceAlterationResidues);
					currentOffset += sequenceAlterationResidues.length();
				}
				// Deletions
				else if (sequenceAlteration instanceof Deletion) {
					if (feature.getFeatureLocation().getStrand() == -1) {
						residues.delete(localCoordinate + currentOffset - sequenceAlteration.getLength() + 1,
								localCoordinate + currentOffset + 1);
					}
					else {
						residues.delete(localCoordinate + currentOffset,
								localCoordinate + currentOffset + sequenceAlteration.getLength());
					}
					currentOffset -= sequenceAlterationResidues.length();
				}
				// Substitions
				else if (sequenceAlteration instanceof Substitution) {
					int start = feature.getStrand() == -1 ? localCoordinate - (sequenceAlteration.getLength() - 1) : localCoordinate;
					residues.replace(start + currentOffset,
							start + currentOffset + sequenceAlteration.getLength(),
							sequenceAlterationResidues);
				}
			}
		}
		return residues.toString();
	}
	
	private List<SequenceAlteration> getFrameshiftsAsAlterations(Transcript transcript) {
		List<SequenceAlteration> frameshifts = new ArrayList<SequenceAlteration>();
		CDS cds = transcript.getCDS();
		if (cds == null) {
			return frameshifts;
		}
		AbstractBioFeature sourceFeature =
			(AbstractBioFeature)BioObjectUtil.createBioObject(cds.getFeatureLocation().getSourceFeature(),
					cds.getConfiguration());
		for (Frameshift frameshift : transcript.getFrameshifts()) {
			if (frameshift.isPlusFrameshift()) {
				// a plus frameshift skips bases during translation, which can be mapped to a deletion for the
				// the skipped bases
				Deletion deletion = new Deletion(cds.getOrganism(), "Deletion-" + frameshift.getCoordinate(), false,
						false, new Timestamp(new Date().getTime()), cds.getConfiguration());
				deletion.setFeatureLocation(frameshift.getCoordinate(),
						frameshift.getCoordinate() + frameshift.getFrameshiftValue(),
						cds.getFeatureLocation().getStrand(), sourceFeature);
				frameshifts.add(deletion);
			}
			else {
				// a minus frameshift goes back bases during translation, which can be mapped to an insertion for the
				// the repeated bases
				Insertion insertion = new Insertion(cds.getOrganism(), "Insertion-" + frameshift.getCoordinate(), false,
						false, new Timestamp(new Date().getTime()), cds.getConfiguration());
				insertion.setFeatureLocation(frameshift.getCoordinate() + frameshift.getFrameshiftValue(),
						frameshift.getCoordinate() + frameshift.getFrameshiftValue(),
						cds.getFeatureLocation().getStrand(), sourceFeature);
				insertion.setResidues(sourceFeature.getResidues().substring(
						frameshift.getCoordinate() + frameshift.getFrameshiftValue(), frameshift.getCoordinate()));
				frameshifts.add(insertion);
			}
		}
		return frameshifts;
	}
	
	public void indexFeature(AbstractSingleLocationBioFeature feature) {
		dataStore.indexFeature(feature);
	}

	public void unindexFeature(AbstractSingleLocationBioFeature feature) {
		dataStore.unindexFeature(feature);
	}
	
	public DataStore getDataStore() {
		return dataStore;
	}
	
}
