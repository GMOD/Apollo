package org.bbop.apollo.editor;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.bbop.apollo.config.Configuration;
import org.bbop.apollo.editor.session.AnnotationSession;
import org.gmod.gbol.bioObject.AbstractBioFeature;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.CDS;
import org.gmod.gbol.bioObject.Comment;
import org.gmod.gbol.bioObject.Exon;
import org.gmod.gbol.bioObject.FlankingRegion;
import org.gmod.gbol.bioObject.Frameshift;
import org.gmod.gbol.bioObject.Gene;
import org.gmod.gbol.bioObject.GenericFeatureProperty;
import org.gmod.gbol.bioObject.NonCanonicalFivePrimeSpliceSite;
import org.gmod.gbol.bioObject.NonCanonicalThreePrimeSpliceSite;
import org.gmod.gbol.bioObject.SequenceAlteration;
import org.gmod.gbol.bioObject.StopCodonReadThrough;
import org.gmod.gbol.bioObject.Transcript;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.DBXref;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.util.SequenceUtil;

public class AnnotationEditor {
	
	private static final String MANUALLY_SET_TRANSLATION_START = "Manually set translation start";
	private static final String MANUALLY_SET_TRANSLATION_END = "Manually set translation end";
	
	private AnnotationSession session;
	private Configuration configuration;
	private List<AnnotationChangeListener> listeners;
	
	/** Constructor.
	 * 
	 * @param session - AnnotationSession associated with the editor
	 * @param configuration - Configuration for the different editing options
	 */
	public AnnotationEditor(AnnotationSession session, Configuration configuration) {
		this.session = session;
		this.configuration = configuration;
		listeners = new ArrayList<AnnotationChangeListener>();
	}
	
	/** Get the session associated with the editor.
	 * 
	 * @return AnnotationSession associated with the editor.
	 */
	public AnnotationSession getSession() {
		return session;
	}
	
	/** Get the configuration used for the different editing functions.
	 * 
	 * @return Configuration object containing different editing options
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/** Set the name of a feature.
	 * 
	 * @param feature - AbstractSingleLocationBioFeature to be modified
	 * @param name - Name to be set
	 */
	public void setName(AbstractSingleLocationBioFeature feature, String name) {
		feature.setName(name);
		feature.setTimeLastModified(new Date());
	}
	
	/** Add a feature to the underlying session.
	 * 
	 * @param feature - AbstractSingleLocationBioFeature to be added
	 */
	public void addFeature(AbstractSingleLocationBioFeature feature) {
		AbstractSingleLocationBioFeature topLevelFeature = getTopLevelFeature(feature);
		
		if (feature instanceof Gene) {
			for (Transcript transcript : ((Gene)feature).getTranscripts()) {
				removeExonOverlapsAndAdjacencies(transcript);
			}
		}
		else if (feature instanceof Transcript) {
			removeExonOverlapsAndAdjacencies((Transcript)feature);
		}

		// event fire
		fireAnnotationChangeEvent(feature, topLevelFeature, AnnotationChangeEvent.Operation.ADD);
		
		getSession().addFeature(feature);

	}
	
	/** Delete a feature to the underlying session.
	 * 
	 * @param feature - AbstractSingleLocationBioFeature to be deleted
	 */
	public void deleteFeature(AbstractSingleLocationBioFeature feature) {
		AbstractSingleLocationBioFeature topLevelFeature = getTopLevelFeature(feature);
		getSession().deleteFeature(feature);
		
		// event fire
		fireAnnotationChangeEvent(feature, topLevelFeature, AnnotationChangeEvent.Operation.DELETE);
		
	}
	
	/** Set the frameshift position for a transcript.  Calculates the longest ORF for the transcript.
	 * 
	 * @param transcript - Transcript to have frameshift added to
	 * @param frameshift - Frameshift to add to the transcript
	 */
	public void setFrameShift(Transcript transcript, Frameshift frameshift) {
		transcript.addFrameshift(frameshift);
		transcript.setTimeLastModified(new Date());
//		setLongestORF(transcript);

		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	
	/** Add a sequence alteration to the genomic region.
	 * 
	 * @param sequenceAlteration - Sequence alteration to be added
	 */
	public void addSequenceAlteration(SequenceAlteration sequenceAlteration) {
		getSession().addSequenceAlteration(sequenceAlteration);
//		for (AbstractSingleLocationBioFeature feature :
//			getSession().getOverlappingFeatures(sequenceAlteration.getFeatureLocation(), false)) {
//			if (feature instanceof Gene) {
//				for (Transcript transcript : ((Gene)feature).getTranscripts()) {
//					setLongestORF(transcript);
//				}
//			}
//		}

		// event fire
		fireAnnotationChangeEvent(sequenceAlteration, sequenceAlteration, AnnotationChangeEvent.Operation.ADD);
		
	}

	/** Delete a sequence alteration to the genomic region.
	 * 
	 * @param sequenceAlteration - Sequence alteration to be deleted
	 */
	public void deleteSequenceAlteration(SequenceAlteration sequenceAlteration) {
		getSession().deleteSequenceAlteration(sequenceAlteration);

		// event fire
		fireAnnotationChangeEvent(sequenceAlteration, sequenceAlteration, AnnotationChangeEvent.Operation.DELETE);
		
	}
	/** Add a transcript to a gene.
	 * 
	 * @param gene - Gene to have transcript added to
	 * @param transcript - Transcript to add to the gene
	 */
	public void addTranscript(Gene gene, Transcript transcript) {
		removeExonOverlapsAndAdjacencies(transcript);
		gene.addTranscript(transcript);

		updateGeneBoundaries(gene);
		
		getSession().indexFeature(transcript);

		// event fire
		fireAnnotationChangeEvent(transcript, gene, AnnotationChangeEvent.Operation.ADD);
		
	}
	
	/** Delete a transcript from a gene.
	 * 
	 * @param gene - Gene to have the transcript deleted from
	 * @param transcript - Transcript to delete from the gene
	 */
	public void deleteTranscript(Gene gene, Transcript transcript) {
		gene.deleteTranscript(transcript);
		getSession().unindexFeature(transcript);
//		if (gene.getTranscripts().size() == 0) {
//			getSession().deleteFeature(gene);
//		}

		if (gene.getTranscripts().size() > 0) {
			updateGeneBoundaries(gene);
		}
		
		// event fire
		fireAnnotationChangeEvent(transcript, gene, AnnotationChangeEvent.Operation.DELETE);
		
	}

	/** Duplicate a transcript.  Adds it to the parent gene if it is set.
	 * 
	 * @param transcript - Transcript to be duplicated
	 */
	public void duplicateTranscript(Transcript transcript) {
		Date date = new Date();
		Transcript duplicate = (Transcript)transcript.cloneFeature(transcript.getUniqueName() + "-copy");
		if (transcript.getGene() != null) {
			transcript.getGene().addTranscript(duplicate);
			transcript.getGene().setTimeLastModified(date);
		}
		// copy exons
		for (Exon exon : transcript.getExons()) {
			addExon(duplicate, new Exon(exon, exon.getUniqueName() + "-copy"));
		}
		// copy CDS
		if (transcript.getCDS() != null) {
			duplicate.setCDS(new CDS(transcript.getCDS(), transcript.getCDS().getUniqueName() + "-copy"));
		}

		duplicate.setTimeAccessioned(date);
		duplicate.setTimeLastModified(date);
		
		/*
		// event fire
		fireAnnotationChangeEvent(duplicate, transcript.getGene(), AnnotationChangeEvent.Operation.ADD);
		*/

	}
	
	/** Merge two transcripts together.
	 * 
	 * @param transcript1 - Transcript to be merged to
	 * @param transcript2 - Transcript to be merged from
	 */
	public void mergeTranscripts(Transcript transcript1, Transcript transcript2) {
		Date date = new Date();
		getSession().unindexFeature(transcript1);
		getSession().unindexFeature(transcript2);
		// Merging transcripts basically boils down to moving all exons from one transcript to the other
		for (Exon exon : transcript2.getExons()) {
			transcript2.deleteExon(exon);
			transcript1.addExon(exon);
		}
		transcript1.setTimeLastModified(date);
		if (transcript1.getGene() != null) {
			transcript1.getGene().setTimeLastModified(date);
		}
		// if the parent genes aren't the same, this leads to a merge of the genes
		if (transcript1.getGene() != null && transcript2.getGene() != null) {
			if (!transcript1.getGene().equals(transcript2.getGene())) {
				for (Transcript transcript : transcript2.getGene().getTranscripts()) {
					if (!transcript.equals(transcript2)) {
						transcript2.getGene().deleteTranscript(transcript);
						transcript1.getGene().addTranscript(transcript);
					}
				}
				deleteFeature(transcript2.getGene());
				getSession().unindexFeature(transcript2.getGene());
			}
		}
		// Delete the empty transcript from the gene
		if (transcript2.getGene() != null) {
			deleteTranscript(transcript2.getGene(), transcript2);
		}
		else {
			deleteFeature(transcript2);
		}
		removeExonOverlapsAndAdjacencies(transcript1);
		getSession().indexFeature(transcript1);
//		setLongestORF(transcript1);
	}

	/** Split a transcript between the two exons.  One transcript will contain all exons from the leftmost
	 *  exon up to leftExon and the other will contain all exons from rightExon to the rightmost exon.
	 *  
	 * @param transcript - Transcript to split
	 * @param leftExon - Left exon of the split
	 * @param rightExon - Right exon of the split
	 * @return Newly created right transcript
	 */
	public Transcript splitTranscript(Transcript transcript, Exon leftExon, Exon rightExon, String splitTranscriptUniqueName) {
		List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons());
		Transcript splitTranscript = (Transcript)transcript.cloneFeature(splitTranscriptUniqueName);
		if (transcript.getGene() != null) {
			addTranscript(transcript.getGene(), splitTranscript);
		}
		else {
			addFeature(splitTranscript);
		}
		transcript.setFmax(leftExon.getFmax());
		splitTranscript.setFmin(rightExon.getFmin());
		for (Exon exon : exons) {
			if (exon.getFmin() > leftExon.getFmin()) {
				deleteExon(transcript, exon);
				if (exon.equals(rightExon)) {
					addExon(splitTranscript, rightExon);
				}
				else {
					addExon(splitTranscript, exon);
				}
			}
		}
		Date date = new Date();
		transcript.setTimeLastModified(date);
		splitTranscript.setTimeAccessioned(date);
		return splitTranscript;
	}

	/** Set the translation start in the transcript.  Sets the translation start in the underlying CDS feature.
	 *  Instantiates the CDS object for the transcript if it doesn't already exist.
	 * 
	 * @param transcript - Transcript to set the translation start in
	 * @param translationStart - Coordinate of the start of translation
	 */
	public void setTranslationStart(Transcript transcript, int translationStart) {
		setTranslationStart(transcript, translationStart, false);
	}
	
	/** Set the translation start in the transcript.  Sets the translation start in the underlying CDS feature.
	 *  Instantiates the CDS object for the transcript if it doesn't already exist.
	 * 
	 * @param transcript - Transcript to set the translation start in
	 * @param translationStart - Coordinate of the start of translation
	 * @param setTranslationEnd - if set to true, will search for the nearest in frame stop codon
	 */
	public void setTranslationStart(Transcript transcript, int translationStart, boolean setTranslationEnd) {
		setTranslationStart(transcript, translationStart, setTranslationEnd, false);
	}
	
	/** Set the translation start in the transcript.  Sets the translation start in the underlying CDS feature.
	 *  Instantiates the CDS object for the transcript if it doesn't already exist.
	 * 
	 * @param transcript - Transcript to set the translation start in
	 * @param translationStart - Coordinate of the start of translation
	 * @param setTranslationEnd - if set to true, will search for the nearest in frame stop codon
	 * @param readThroughStopCodon - if set to true, will read through the first stop codon to the next
	 */
	public void setTranslationStart(Transcript transcript, int translationStart, boolean setTranslationEnd, boolean readThroughStopCodon) {
		setTranslationStart(transcript, translationStart, setTranslationEnd, setTranslationEnd ? configuration.getTranslationTable() : null, readThroughStopCodon);
	}

	/** Set the translation start in the transcript.  Sets the translation start in the underlying CDS feature.
	 *  Instantiates the CDS object for the transcript if it doesn't already exist.
	 * 
	 * @param transcript - Transcript to set the translation start in
	 * @param translationStart - Coordinate of the start of translation
	 * @param setTranslationEnd - if set to true, will search for the nearest in frame stop codon
	 * @param translationTable - Translation table that defines the codon translation
	 * @param readThroughStopCodon - if set to true, will read through the first stop codon to the next
	 */
	public void setTranslationStart(Transcript transcript, int translationStart, boolean setTranslationEnd, SequenceUtil.TranslationTable translationTable, boolean readThroughStopCodon) {
		CDS cds = transcript.getCDS();
		if (cds == null) {
			cds = createCDS(transcript);
			transcript.setCDS(cds);
		}
		if (transcript.getStrand() == -1) {
			cds.setFmax(translationStart + 1);
		}
		else {
			cds.setFmin(translationStart);
		}
		setManuallySetTranslationStart(cds, true);
		cds.deleteStopCodonReadThrough();
		if (setTranslationEnd && translationTable != null) {
			String mrna = getSession().getResiduesWithAlterationsAndFrameshifts(transcript);
			if (mrna == null || mrna.equals("null")) {
				return;
			}
			int stopCodonCount = 0;
			for (int i = transcript.convertSourceCoordinateToLocalCoordinate(translationStart); i < transcript.getLength(); i += 3) {
				if (i + 3 > mrna.length()) {
					break;
				}
				String codon = mrna.substring(i, i + 3);
				if (translationTable.getStopCodons().contains(codon)) {
					if (readThroughStopCodon && ++stopCodonCount < 2) {
						StopCodonReadThrough stopCodonReadThrough = cds.getStopCodonReadThrough();
						if (stopCodonReadThrough == null) {
							stopCodonReadThrough = createStopCodonReadThrough(cds);
							cds.setStopCodonReadThrough(stopCodonReadThrough);
							if (cds.getStrand() == -1) {
								stopCodonReadThrough.setFmin(getSession().convertModifiedLocalCoordinateToSourceCoordinate(transcript, i + 3));
								stopCodonReadThrough.setFmax(getSession().convertModifiedLocalCoordinateToSourceCoordinate(transcript, i) + 1);
							}
							else {
								stopCodonReadThrough.setFmin(getSession().convertModifiedLocalCoordinateToSourceCoordinate(transcript, i));
								stopCodonReadThrough.setFmax(getSession().convertModifiedLocalCoordinateToSourceCoordinate(transcript, i + 3) + 1);
							}
						}
						continue;
					}
					if (transcript.getStrand() == -1) {
						cds.setFmin(transcript.convertLocalCoordinateToSourceCoordinate(i + 2));
					}
					else {
						cds.setFmax(transcript.convertLocalCoordinateToSourceCoordinate(i + 3));
					}
					return;
				}
			}
			if (transcript.getStrand() == -1) {
				cds.setFmin(transcript.getFmin());
				cds.setFminPartial(true);
			}
			else {
				cds.setFmax(transcript.getFmax());
				cds.setFmaxPartial(true);
			}
		}

		Date date = new Date();
		cds.setTimeLastModified(date);
		transcript.setTimeLastModified(date);
		
		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	
	/** Set the translation end in the transcript.  Sets the translation end in the underlying CDS feature.
	 *  Instantiates the CDS object for the transcript if it doesn't already exist.
	 * 
	 * @param transcript - Transcript to set the translation start in
	 * @param translationEnd - Coordinate of the end of translation
	 */
	/*
	public void setTranslationEnd(Transcript transcript, int translationEnd) {
		CDS cds = transcript.getCDS();
		if (cds == null) {
			cds = createCDS(transcript);
			transcript.setCDS(cds);
		}
		if (transcript.getStrand() == -1) {
			cds.setFmin(translationEnd + 1);
		}
		else {
			cds.setFmax(translationEnd);
		}
		setManuallySetTranslationEnd(cds, true);
		cds.deleteStopCodonReadThrough();

		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	*/
	
	/** Set the translation end in the transcript.  Sets the translation end in the underlying CDS feature.
	 *  Instantiates the CDS object for the transcript if it doesn't already exist.
	 * 
	 * @param transcript - Transcript to set the translation end in
	 * @param translationEnd - Coordinate of the end of translation
	 */
	public void setTranslationEnd(Transcript transcript, int translationEnd) {
		setTranslationEnd(transcript, translationEnd, false);
	}
	
	/** Set the translation end in the transcript.  Sets the translation end in the underlying CDS feature.
	 *  Instantiates the CDS object for the transcript if it doesn't already exist.
	 * 
	 * @param transcript - Transcript to set the translation end in
	 * @param translationEnd - Coordinate of the end of translation
	 * @param setTranslationStart - if set to true, will search for the nearest in frame start
	 */
	public void setTranslationEnd(Transcript transcript, int translationEnd, boolean setTranslationStart) {
		setTranslationEnd(transcript, translationEnd, setTranslationStart, setTranslationStart ? configuration.getTranslationTable() : null);
	}

	/** Set the translation end in the transcript.  Sets the translation end in the underlying CDS feature.
	 *  Instantiates the CDS object for the transcript if it doesn't already exist.
	 * 
	 * @param transcript - Transcript to set the translation end in
	 * @param translationEnd - Coordinate of the end of translation
	 * @param setTranslationStart - if set to true, will search for the nearest in frame start codon
	 * @param translationTable - Translation table that defines the codon translation
	 */
	public void setTranslationEnd(Transcript transcript, int translationEnd, boolean setTranslationStart, SequenceUtil.TranslationTable translationTable) {
		CDS cds = transcript.getCDS();
		if (cds == null) {
			cds = createCDS(transcript);
			transcript.setCDS(cds);
		}
		if (transcript.getStrand() == -1) {
			cds.setFmin(translationEnd);
		}
		else {
			cds.setFmax(translationEnd + 1);
		}
		setManuallySetTranslationEnd(cds, true);
		cds.deleteStopCodonReadThrough();
		if (setTranslationStart && translationTable != null) {
			String mrna = getSession().getResiduesWithAlterationsAndFrameshifts(transcript);
			if (mrna == null || mrna.equals("null")) {
				return;
			}
			for (int i = transcript.convertSourceCoordinateToLocalCoordinate(translationEnd) - 3; i >= 0; i -= 3) {
				if (i - 3 < 0) {
					break;
				}
				String codon = mrna.substring(i, i + 3);
				if (translationTable.getStartCodons().contains(codon)) {
					if (transcript.getStrand() == -1) {
						cds.setFmax(transcript.convertLocalCoordinateToSourceCoordinate(i + 3));
					}
					else {
						cds.setFmin(transcript.convertLocalCoordinateToSourceCoordinate(i + 2));
					}
					return;
				}
			}
			if (transcript.getStrand() == -1) {
				cds.setFmin(transcript.getFmin());
				cds.setFminPartial(true);
			}
			else {
				cds.setFmax(transcript.getFmax());
				cds.setFmaxPartial(true);
			}
		}

		Date date = new Date();
		cds.setTimeLastModified(date);
		transcript.setTimeLastModified(date);

		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	

	/** Set the translation start and end in the transcript.  Sets the translation start and end in the underlying CDS
	 *  feature.  Instantiates the CDS object for the transcript if it doesn't already exist.
	 * 
	 * @param transcript - Transcript to set the translation start in
	 * @param translationStart - Coordinate of the start of translation
	 * @param translationEnd - Coordinate of the end of translation
	 * @param manuallySetStart - whether the start was manually set
	 * @param manuallySetEnd - whether the end was manually set
	 */
	public void setTranslationEnds(Transcript transcript, int translationStart, int translationEnd, boolean manuallySetStart, boolean manuallySetEnd) {
		setTranslationFmin(transcript, translationStart);
		setTranslationFmax(transcript, translationEnd);
		setManuallySetTranslationStart(transcript.getCDS(), manuallySetStart);
		setManuallySetTranslationEnd(transcript.getCDS(), manuallySetEnd);

		Date date = new Date();
		transcript.getCDS().setTimeLastModified(date);
		transcript.setTimeLastModified(date);
		
		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	
	private void setTranslationFmin(Transcript transcript, int translationFmin) {
		CDS cds = transcript.getCDS();
		if (cds == null) {
			cds = createCDS(transcript);
			transcript.setCDS(cds);
		}
		cds.setFmin(translationFmin);

		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}

	private void setTranslationFmax(Transcript transcript, int translationFmax) {
		CDS cds = transcript.getCDS();
		if (cds == null) {
			cds = createCDS(transcript);
			transcript.setCDS(cds);
		}
		cds.setFmax(translationFmax);

		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	
	public void calculateCDS(Transcript transcript) {
		calculateCDS(transcript, false);
	}
	
	public void calculateCDS(Transcript transcript, boolean readThroughStopCodon) {
		CDS cds = transcript.getCDS();
		if (cds == null) {
			setLongestORF(transcript, readThroughStopCodon);
			return;
		}
		boolean manuallySetStart = isManuallySetTranslationStart(cds);
		boolean manuallySetEnd = isManuallySetTranslationEnd(cds);
		if (manuallySetStart && manuallySetEnd) {
			return;
		}
		if (!manuallySetStart && !manuallySetEnd) {
			setLongestORF(transcript, readThroughStopCodon);
		}
		else if (manuallySetStart) {
			setTranslationStart(transcript, cds.getStrand().equals(-1) ? cds.getFmax() - 1 : cds.getFmin(), true, readThroughStopCodon);
		}
		else {
			setTranslationEnd(transcript, cds.getStrand().equals(-1) ? cds.getFmin() : cds.getFmax() - 1, true);
		}
	}

	public void setLongestORF(Transcript transcript) {
		setLongestORF(transcript, false);
	}
	
	/** Calculate the longest ORF for a transcript.  If a valid start codon is not found, allow for partial CDS start/end.
	 *  Calls setLongestORF(Transcript, TranslationTable, boolean) with the translation table and whether partial
	 *  ORF calculation extensions are allowed from the configuration associated with this editor.
	 *  
	 * @param transcript - Transcript to set the longest ORF to
	 */
	public void setLongestORF(Transcript transcript, boolean readThroughStopCodon) {
		setLongestORF(transcript, configuration.getTranslationTable(), configuration.isPartialTranslationExtensionAllowed(), readThroughStopCodon);
	}
	
	public void setLongestORF(Transcript transcript, SequenceUtil.TranslationTable translationTable, boolean allowPartialExtension) {
		setLongestORF(transcript, translationTable, allowPartialExtension, false);
	}
	
	/** Calculate the longest ORF for a transcript.  If a valid start codon is not found, allow for partial CDS start/end.
	 * 
	 * @param transcript - Transcript to set the longest ORF to
	 * @param translationTable - Translation table that defines the codon translation
	 * @param allowPartialExtension - Where partial ORFs should be used for possible extension
	 */
	public void setLongestORF(Transcript transcript, SequenceUtil.TranslationTable translationTable, boolean allowPartialExtension, boolean readThroughStopCodon) {
		String mrna = getSession().getResiduesWithAlterationsAndFrameshifts(transcript);
		if (mrna == null || mrna.equals("null")) {
			return;
		}
		String longestPeptide = "";
		int bestStartIndex = -1;
		int bestStopIndex = -1;
		boolean partialStop = false;
		
		if (mrna.length() > 3) {
			for (String startCodon : translationTable.getStartCodons()) {
				int startIndex = mrna.indexOf(startCodon);
				while (startIndex >= 0) {
					String mrnaSubstring = mrna.substring(startIndex);
					String aa = SequenceUtil.translateSequence(mrnaSubstring, translationTable, true, readThroughStopCodon);
					if (aa.length() > longestPeptide.length()) {
						longestPeptide = aa;
						bestStartIndex = startIndex;
						bestStopIndex = startIndex + (aa.length() * 3);
						if (!longestPeptide.substring(longestPeptide.length() - 1).equals(SequenceUtil.TranslationTable.STOP)) {
							partialStop = true;
							bestStopIndex += mrnaSubstring.length() % 3;
						}
					}
					startIndex = mrna.indexOf(startCodon, startIndex + 1);
				}
			}
		}
		
		boolean needCdsIndex = transcript.getCDS() == null;
		CDS cds = transcript.getCDS();
		if (cds == null) {
			cds = createCDS(transcript);
			transcript.setCDS(cds);
		}
		if (bestStartIndex >= 0) {
			int fmin = getSession().convertModifiedLocalCoordinateToSourceCoordinate(transcript, bestStartIndex);
			int fmax = getSession().convertModifiedLocalCoordinateToSourceCoordinate(transcript, bestStopIndex);
			if (cds.getStrand().equals(-1)) {
				int tmp = fmin;
				fmin = fmax + 1;
				fmax = tmp + 1;
			}
			cds.setFmin(fmin);
			cds.setFminPartial(false);
			cds.setFmax(fmax);
			cds.setFmaxPartial(partialStop);
		}
		else {
			cds.setFmin(transcript.getFmin());
			cds.setFminPartial(true);
			String aa = SequenceUtil.translateSequence(mrna, translationTable, true, readThroughStopCodon);
			if (aa.substring(aa.length() - 1).equals(SequenceUtil.TranslationTable.STOP)) {
				cds.setFmax(getSession().convertModifiedLocalCoordinateToSourceCoordinate(transcript, aa.length() * 3));
				cds.setFmaxPartial(false);
			}
			else {
				cds.setFmax(transcript.getFmax());
				cds.setFmaxPartial(true);
			}
		}
		if (readThroughStopCodon) {
			String aa = SequenceUtil.translateSequence(getSession().getResiduesWithAlterationsAndFrameshifts(cds), translationTable, true, true);
			int firstStopIndex = aa.indexOf(SequenceUtil.TranslationTable.STOP);
			if (firstStopIndex < aa.length() - 1) {
				StopCodonReadThrough stopCodonReadThrough = createStopCodonReadThrough(cds);
				cds.setStopCodonReadThrough(stopCodonReadThrough);
				int offset = transcript.getStrand() == -1 ? -2 : 0;
				stopCodonReadThrough.setFmin(getSession().convertModifiedLocalCoordinateToSourceCoordinate(cds, firstStopIndex * 3) + offset);
				stopCodonReadThrough.setFmax(getSession().convertModifiedLocalCoordinateToSourceCoordinate(cds, firstStopIndex * 3) + 3 + offset);
			}
		}
		else {
			cds.deleteStopCodonReadThrough();
		}
		setManuallySetTranslationStart(cds, false);
		setManuallySetTranslationEnd(cds, false);

		if (needCdsIndex) {
			getSession().indexFeature(cds);
		}

		Date date = new Date();
		cds.setTimeLastModified(date);
		transcript.setTimeLastModified(date);
		
		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}

	/** Add an exon to a transcript.
	 * 
	 * @param transcript - Transcript to have the exon added to
	 * @param exon - Exon to be added to the transcript
	 */
	public void addExon(Transcript transcript, Exon exon) {
		transcript.addExon(exon);
		removeExonOverlapsAndAdjacencies(transcript);

		updateGeneBoundaries(exon.getTranscript().getGene());
		
		getSession().indexFeature(exon);
		getSession().indexFeature(exon);

		transcript.setTimeLastModified(new Date());
		
		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	
	/** Delete an exon from a transcript.  If there are no exons left on the transcript, the transcript
	 *  is deleted from the parent gene.
	 * 
	 * @param transcript - Transcript to have the exon deleted from
	 * @param exon - Exon to be deleted from the transcript
	 */
	public void deleteExon(Transcript transcript, Exon exon) {
		transcript.deleteExon(exon);
		// an empty transcript should be removed from gene
//		if (transcript.getNumberOfExons() == 0) {
//			if (transcript.getGene() != null) {
//				deleteTranscript(transcript.getGene(), transcript);
//			}
//			else {
//				deleteFeature(transcript);
//			}
//		}
//		else {
//			setLongestORF(transcript);
//		}
		// update transcript boundaries if necessary
		if (exon.getFmin().equals(transcript.getFmin())) {
			int fmin = Integer.MAX_VALUE;
			for (Exon e : transcript.getExons()) {
				if (e.getFmin() < fmin) {
					fmin = e.getFmin();
				}
			}
			transcript.setFmin(fmin);
		}
		if (exon.getFmax().equals(transcript.getFmax())) {
			int fmax = Integer.MIN_VALUE;
			for (Exon e : transcript.getExons()) {
				if (e.getFmax() > fmax) {
					fmax = e.getFmax();
				}
			}
			transcript.setFmax(fmax);
		}
		// update gene boundaries if necessary
		updateGeneBoundaries(transcript.getGene());
		
		getSession().unindexFeature(exon);
		getSession().indexFeature(transcript);
		
		transcript.setTimeLastModified(new Date());
		
		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}

	/** Merge exon1 and exon2.  The "newly" created exon retains exon1's ID.
	 * 
	 * @param exon1 - Exon to be merged to
	 * @param exon2 - Exon to be merged with
	 * @throws AnnotationEditorException - If exons don't belong to the same transcript or are in separate strands
	 */
	public void mergeExons(Exon exon1, Exon exon2) throws AnnotationEditorException {
//		// both exons must be part of the same transcript
//		if (!exon1.getTranscript().equals(exon2.getTranscript())) {
//			throw new AnnotationEditorException("mergeExons(): Exons must have same parent transcript", exon1, exon2);
//		}
		// both exons must be in the same strand
		Transcript transcript = exon1.getTranscript();
		if (!exon1.getStrand().equals(exon2.getStrand())) {
			throw new AnnotationEditorException("mergeExons(): Exons must be in the same strand", exon1, exon2);
		}
		if (exon1.getFmin() > exon2.getFmin()) {
			exon1.setFmin(exon2.getFmin());
		}
		if (exon1.getFmax() < exon2.getFmax()) {
			exon1.setFmax(exon2.getFmax());
		}
		// need to delete exon2 from transcript
		if (exon2.getTranscript() != null) {
			deleteExon(exon2.getTranscript(), exon2);
		}
//		setLongestORF(exon1.getTranscript());
		removeExonOverlapsAndAdjacencies(transcript);
		
		Date date = new Date();
		exon1.setTimeLastModified(date);
		transcript.setTimeLastModified(date);

		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	
	/**Splits the exon, creating two exons, the left one which starts at exon.getFmin() and ends at
	 * newLeftMax and the right one which starts at newRightMin and ends at exon.getFeatureLocation.getFmax().
	 * 
	 * @param exon - Exon to be split
	 * @param newLeftMax - Left split exon max
	 * @param newRightMin - Right split exon min
	 */
	public Exon splitExon(Exon exon, int newLeftMax, int newRightMin, String splitExonUniqueName) {
		session.unindexFeature(exon);
		Exon leftExon = exon;
		Exon rightExon = new Exon(exon, splitExonUniqueName);

//		leftExon.setUniqueName(exon.getUniqueName() + "-left");
		leftExon.setFmax(newLeftMax);
		rightExon.setFmin(newRightMin);

		addExon(exon.getTranscript(), rightExon);
		session.indexFeature(leftExon);
		session.indexFeature(rightExon);
		
		// event fire
		fireAnnotationChangeEvent(exon.getTranscript(), exon.getTranscript().getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
		Date date = new Date();
		exon.setTimeLastModified(date);
		rightExon.setTimeAccessioned(date);
		rightExon.setTimeLastModified(date);
		exon.getTranscript().setTimeLastModified(date);
		
		return rightExon;
	}
	
	public Exon makeIntron(Exon exon, int genomicPosition, int minimumIntronSize, String splitExonUniqueName) {
		String sequence = exon.getResidues();
		int exonPosition = exon.convertSourceCoordinateToLocalCoordinate(genomicPosition);
		// find donor coordinate
		String donorSite = null;
		int donorCoordinate = -1;
		for (String donor : SequenceUtil.getSpliceDonorSites()) {
			int coordinate = sequence.substring(0, exonPosition - minimumIntronSize).lastIndexOf(donor);
			if (coordinate > donorCoordinate) {
				donorCoordinate = coordinate;
				donorSite = donor;
			}
		}
		// find acceptor coordinate
		String acceptorSite = null;
		int acceptorCoordinate = -1;
		for (String acceptor : SequenceUtil.getSpliceAcceptorSites()) {
			int coordinate = sequence.substring(exonPosition + minimumIntronSize, sequence.length()).indexOf(acceptor);
			if (acceptorCoordinate == -1 || coordinate < acceptorCoordinate) {
				acceptorCoordinate = coordinate;
				acceptorSite = acceptor;
			}
		}
		// no donor/acceptor found
		if (donorCoordinate == -1 || acceptorCoordinate == -1 || (acceptorCoordinate - donorCoordinate) == 1) {
			//return splitExon(exon, genomicPosition - 1, genomicPosition + 1, splitExonUniqueName);
			return null;
		}
		acceptorCoordinate += exonPosition + minimumIntronSize;
		if (exon.getStrand().equals(-1)) {
			int tmp = acceptorCoordinate;
			acceptorCoordinate = donorCoordinate + 1 - donorSite.length();
			donorCoordinate = tmp + 1;
		}
		else {
			acceptorCoordinate += acceptorSite.length();
		}
		Exon splitExon = splitExon(exon, exon.convertLocalCoordinateToSourceCoordinate(donorCoordinate), exon.convertLocalCoordinateToSourceCoordinate(acceptorCoordinate), splitExonUniqueName);
		/*
		if (exon.getLength() == 0) {
			deleteExon(exon.getTranscript(), exon);
		}
		if (splitExon.getLength() == 0) {
			deleteExon(splitExon.getTranscript(), splitExon);
		}
		*/

		// event fire
		fireAnnotationChangeEvent(exon.getTranscript(), exon.getTranscript().getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
		Date date = new Date();
		exon.setTimeLastModified(date);
		splitExon.setTimeAccessioned(date);
		splitExon.setTimeLastModified(date);
		exon.getTranscript().setTimeLastModified(date);
		
		return splitExon;
	}

	/** Add a frameshift to a transcript.
	 * 
	 * @param transcript - Transcript to add the frameshift to
	 * @param frameshift - Frameshift to add to the transcript
	 */
	public void addFrameshift(Transcript transcript, Frameshift frameshift) {
		transcript.addFrameshift(frameshift);

		transcript.setTimeLastModified(new Date());
		
		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	
	/** Delete a frameshift from a transcript.
	 * 
	 * @param transcript - Transcript to delete the frameshift from
	 * @param frameshift - Frameshift to delete from the transcript
	 */
	public void deleteFrameshift(Transcript transcript, Frameshift frameshift) {
		transcript.deleteFrameshift(frameshift);

		transcript.setTimeLastModified(new Date());

		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	
	/** Set exon boundaries.
	 * 
	 * @param exon - Exon to be modified
	 * @param fmin - New fmin to be set
	 * @param fmax - New fmax to be set
	 */
	public void setExonBoundaries(Exon exon, int fmin, int fmax) {
		Transcript transcript = exon.getTranscript();
		exon.setFmin(fmin);
		exon.setFmax(fmax);
		removeExonOverlapsAndAdjacencies(transcript);

		updateGeneBoundaries(exon.getTranscript().getGene());

		session.unindexFeature(transcript);
		session.indexFeature(transcript);
		
		Date date = new Date();
		exon.setTimeLastModified(date);
		exon.getTranscript().setTimeLastModified(date);
		
		// event fire
		fireAnnotationChangeEvent(exon.getTranscript(), exon.getTranscript().getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	
	public void setToDownstreamDonor(Exon exon) throws AnnotationEditorException {
		Transcript transcript = exon.getTranscript();
		Gene gene = transcript.getGene();
		List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons(), true);
		Integer nextExonFmin = null;
		Integer nextExonFmax = null;
		for (ListIterator<Exon> iter = exons.listIterator(); iter.hasNext(); ) {
			Exon e = iter.next();
			if (e.getUniqueName().equals(exon.getUniqueName())) {
				if (iter.hasNext()) {
					Exon e2 = iter.next();
					nextExonFmin = e2.getFmin();
					nextExonFmax = e2.getFmax();
					break;
				}
			}
		}
		int coordinate = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin()) + 2 : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax()) + 1;
		String residues = gene.getResidues();
		while (coordinate < residues.length()) {
			int c = gene.convertLocalCoordinateToSourceCoordinate(coordinate);
			if (nextExonFmin != null && (c >= nextExonFmin && c <= nextExonFmax + 1)) {
				throw new AnnotationEditorException("Cannot set to downstream donor - will overlap next exon");
			}
			String seq = residues.substring(coordinate, coordinate + 2);
			if (SequenceUtil.getSpliceDonorSites().contains(seq)) {
				if (exon.getStrand() == -1) {
					setExonBoundaries(exon, gene.convertLocalCoordinateToSourceCoordinate(coordinate) + 1, exon.getFmax());
				}
				else {
					setExonBoundaries(exon, exon.getFmin(), gene.convertLocalCoordinateToSourceCoordinate(coordinate));
				}
				return;
			}
			++coordinate;
		}
	}

	public void setToUpstreamDonor(Exon exon) throws AnnotationEditorException {
		Transcript transcript = exon.getTranscript();
		Gene gene = transcript.getGene();
		int coordinate = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin()) : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax()) - 1;
		int exonStart = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax()) - 1 : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin());
		String residues = gene.getResidues();
		while (coordinate > 0) {
			if (coordinate <= exonStart) {
				throw new AnnotationEditorException("Cannot set to upstream donor - will remove exon");
			}
			String seq = residues.substring(coordinate, coordinate + 2);
			if (SequenceUtil.getSpliceDonorSites().contains(seq)) {
				if (exon.getStrand() == -1) {
					setExonBoundaries(exon, gene.convertLocalCoordinateToSourceCoordinate(coordinate) + 1, exon.getFmax());
				}
				else {
					setExonBoundaries(exon, exon.getFmin(), gene.convertLocalCoordinateToSourceCoordinate(coordinate));
				}
				return;
			}
			--coordinate;
		}
	}

	public void setToDownstreamAcceptor(Exon exon) throws AnnotationEditorException {
		Transcript transcript = exon.getTranscript();
		Gene gene = transcript.getGene();
		int coordinate = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax()) : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin());
		int exonEnd = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin()) : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax()) - 1;
		String residues = gene.getResidues();
		while (coordinate < residues.length()) {
			if (coordinate >= exonEnd) {
				throw new AnnotationEditorException("Cannot set to downstream acceptor - will remove exon");
			}
			String seq = residues.substring(coordinate, coordinate + 2);
			if (SequenceUtil.getSpliceAcceptorSites().contains(seq)) {
				if (exon.getStrand() == -1) {
					setExonBoundaries(exon, exon.getFmin(), gene.convertLocalCoordinateToSourceCoordinate(coordinate) - 1);
				}
				else {
					setExonBoundaries(exon, gene.convertLocalCoordinateToSourceCoordinate(coordinate) + 2, exon.getFmax());
				}
				return;
			}
			++coordinate;
		}
	}

	public void setToUpstreamAcceptor(Exon exon) throws AnnotationEditorException {
		Transcript transcript = exon.getTranscript();
		Gene gene = transcript.getGene();
		List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons(), true);
		Integer prevExonFmin = null;
		Integer prevExonFmax = null;
		for (ListIterator<Exon> iter = exons.listIterator(); iter.hasNext(); ) {
			Exon e = iter.next();
			if (e.getUniqueName().equals(exon.getUniqueName())) {
				if (iter.hasPrevious()) {
					iter.previous();
					if (iter.hasPrevious()) {
						Exon e2 = iter.previous();
						prevExonFmin = e2.getFmin();
						prevExonFmax = e2.getFmax();
					}
				}
				break;
			}
		}
		int coordinate = exon.getStrand() == -1 ? gene.convertSourceCoordinateToLocalCoordinate(exon.getFmax() + 2) : gene.convertSourceCoordinateToLocalCoordinate(exon.getFmin() - 3);
		String residues = gene.getResidues();
		while (coordinate >= 0) {
			int c = gene.convertLocalCoordinateToSourceCoordinate(coordinate);
			if (prevExonFmin != null && (c >= prevExonFmin && c <= prevExonFmax - 2)) {
				throw new AnnotationEditorException("Cannot set to upstream acceptor - will overlap previous exon");
			}
			String seq = residues.substring(coordinate, coordinate + 2);
			if (SequenceUtil.getSpliceAcceptorSites().contains(seq)) {
				if (exon.getStrand() == -1) {
					setExonBoundaries(exon, exon.getFmin(), gene.convertLocalCoordinateToSourceCoordinate(coordinate) - 1);
				}
				else {
					setExonBoundaries(exon, gene.convertLocalCoordinateToSourceCoordinate(coordinate) + 2, exon.getFmax());
				}
				return;
			}
			--coordinate;
		}
	}
	
	public void setBoundaries(AbstractSingleLocationBioFeature feature, int fmin, int fmax) {
		feature.setFmin(fmin);
		feature.setFmax(fmax);
		session.unindexFeature(feature);
		session.indexFeature(feature);

		Date date = new Date();
		AbstractSingleLocationBioFeature f = feature;
		while (f != null) {
			f.setTimeLastModified(date);
			Collection<? extends AbstractSingleLocationBioFeature> parents = f.getParents();
			f = parents.size() > 0 ? parents.iterator().next() : null;
		}
		
		// event fire
		fireAnnotationChangeEvent(feature, getTopLevelFeature(feature), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	
	/** Flips the strand of a feature and all of its children (recursively).  If the feature has no strand (0), it will flip it to
	 *  the minus strand (-1).
	 * 
	 * @param feature - Feature to have strand flipped
	 */
	public void flipStrand(AbstractSingleLocationBioFeature feature) {
		if (feature.getStrand() == -1) {
			feature.setStrand(1);
		}
		else {
			feature.setStrand(-1);
		}
		for (AbstractSingleLocationBioFeature childFeature : feature.getChildren()) {
			flipStrand(childFeature);
		}
		
		feature.setTimeLastModified(new Date());

		// event fire
		fireAnnotationChangeEvent(feature, getTopLevelFeature(feature), AnnotationChangeEvent.Operation.UPDATE);
		
	}
	
	public void findNonCanonicalAcceptorDonorSpliceSites(Transcript transcript) {
		transcript.deleteAllNonCanonicalFivePrimeSpliceSites();
		transcript.deleteAllNonCanonicalThreePrimeSpliceSites();
		List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons());
		int exonNum = 0;
		int sourceFeatureLength = transcript.getFeatureLocation().getSourceFeature().getSequenceLength();
		for (Exon exon : exons) {
			++exonNum;
			int fivePrimeSpliceSitePosition = -1;
			int threePrimeSpliceSitePosition = -1;
			boolean validFivePrimeSplice = false;
			boolean validThreePrimeSplice = false;
			for (String donor : SequenceUtil.getSpliceDonorSites()) {
				for (String acceptor : SequenceUtil.getSpliceAcceptorSites()) {
					FlankingRegion spliceAcceptorSiteFlankingRegion = createFlankingRegion(exon, exon.getFmin() - donor.length(), exon.getFmin());
					FlankingRegion spliceDonorSiteFlankingRegion = createFlankingRegion(exon, exon.getFmax(), exon.getFmax() + donor.length());
					if (exon.getStrand() == -1) {
						FlankingRegion tmp = spliceAcceptorSiteFlankingRegion;
						spliceAcceptorSiteFlankingRegion = spliceDonorSiteFlankingRegion;
						spliceDonorSiteFlankingRegion = tmp;
					}
					/*
					String donorSpliceSiteSequence = session.getResiduesWithAlterations(spliceDonorSiteFlankingRegion);
					String acceptorSpliceSiteSequence = session.getResiduesWithAlterations(spliceAcceptorSiteFlankingRegion);
					*/
					String donorSpliceSiteSequence = spliceDonorSiteFlankingRegion.getFmin() >= 0 && spliceDonorSiteFlankingRegion.getFmax() <= sourceFeatureLength ?
							session.getResiduesWithAlterations(spliceDonorSiteFlankingRegion) : null;
					String acceptorSpliceSiteSequence = spliceAcceptorSiteFlankingRegion.getFmin() >= 0 && spliceAcceptorSiteFlankingRegion.getFmax() <= sourceFeatureLength ?
							session.getResiduesWithAlterations(spliceAcceptorSiteFlankingRegion) : null;
					if (exonNum < exons.size()) {
						if (!validFivePrimeSplice) {
							if (!donorSpliceSiteSequence.equals(donor)) {
								fivePrimeSpliceSitePosition = exon.getStrand() == -1 ? spliceDonorSiteFlankingRegion.getFmax() : spliceDonorSiteFlankingRegion.getFmin();
							}
							else {
								validFivePrimeSplice = true;
							}
						}
					}
					if (exonNum > 1) {
						if (!validThreePrimeSplice) {
							if (!acceptorSpliceSiteSequence.equals(acceptor)) {
								threePrimeSpliceSitePosition = exon.getStrand() == -1 ? spliceAcceptorSiteFlankingRegion.getFmin() : spliceAcceptorSiteFlankingRegion.getFmax();
							}
							else {
								validThreePrimeSplice = true;
							}
						}
					}
				}
			}
			if (!validFivePrimeSplice && fivePrimeSpliceSitePosition != -1) {
				transcript.addNonCanonicalFivePrimeSpliceSite(createNonCanonicalFivePrimeSpliceSite(transcript, fivePrimeSpliceSitePosition));
			}
			if (!validThreePrimeSplice && threePrimeSpliceSitePosition != -1) {
				transcript.addNonCanonicalThreePrimeSpliceSite(createNonCanonicalThreePrimeSpliceSite(transcript, threePrimeSpliceSitePosition));
			}
		}

		transcript.setTimeLastModified(new Date());
		
		// event fire
		fireAnnotationChangeEvent(transcript, transcript.getGene(), AnnotationChangeEvent.Operation.UPDATE);
		
	}

	private NonCanonicalFivePrimeSpliceSite createNonCanonicalFivePrimeSpliceSite(Transcript transcript, int position) {
		String uniqueName = transcript.getUniqueName() + "-non_canonical_five_prive_splice_site-" + position;
		NonCanonicalFivePrimeSpliceSite spliceSite = new NonCanonicalFivePrimeSpliceSite(transcript.getOrganism(),
				uniqueName, transcript.isAnalysis(), transcript.isObsolete(), new Timestamp(new Date().getTime()),
				transcript.getConfiguration());
		spliceSite.setFeatureLocation(new FeatureLocation());
		spliceSite.setStrand(transcript.getStrand());
		spliceSite.getFeatureLocation().setSourceFeature(transcript.getFeatureLocation().getSourceFeature());
		spliceSite.setFmin(position);
		spliceSite.setFmax(position);
		spliceSite.setTimeLastModified(new Date());
		return spliceSite;
	}

	private NonCanonicalThreePrimeSpliceSite createNonCanonicalThreePrimeSpliceSite(Transcript transcript, int position) {
		String uniqueName = transcript.getUniqueName() + "-non_canonical_three_prive_splice_site-" + position;
		NonCanonicalThreePrimeSpliceSite spliceSite =  new NonCanonicalThreePrimeSpliceSite(transcript.getOrganism(),
				uniqueName, transcript.isAnalysis(), transcript.isObsolete(), new Timestamp(new Date().getTime()),
				transcript.getConfiguration());
		spliceSite.setFeatureLocation(new FeatureLocation());
		spliceSite.setStrand(transcript.getStrand());
		spliceSite.getFeatureLocation().setSourceFeature(transcript.getFeatureLocation().getSourceFeature());
		spliceSite.setFmin(position);
		spliceSite.setFmax(position);
		spliceSite.setTimeLastModified(new Date());
		return spliceSite;
	}
	
	private FlankingRegion createFlankingRegion(AbstractSingleLocationBioFeature feature, int fmin, int fmax) {
		FlankingRegion flankingRegion = new FlankingRegion(null, null, false, false, null, feature.getConfiguration());
		flankingRegion.setFeatureLocation(new FeatureLocation());
		flankingRegion.getFeatureLocation().setSourceFeature(feature.getFeatureLocation().getSourceFeature());
		flankingRegion.setStrand(feature.getStrand());
		flankingRegion.setFmin(fmin);
		flankingRegion.setFmax(fmax);
		return flankingRegion;
	}

	public class AnnotationEditorException extends Exception {

		private static final long serialVersionUID = 1L;
		private AbstractBioFeature[] sources;
		
		public AnnotationEditorException(String message, AbstractBioFeature ... sources) {
			super(message);
			this.sources = sources;
		}
		
		public AnnotationEditorException(String message, Collection<? extends AbstractBioFeature> sources) {
			super(message);
			this.sources = new AbstractBioFeature[sources.size()];
			int i = 0;
			for (AbstractBioFeature feature : sources) {
				this.sources[i++] = feature;
			}
		}
		
		public AbstractBioFeature[] getSources() {
			return sources;
		}
		
	}
	
	private void removeExonOverlapsAndAdjacencies(Transcript transcript) {
		if (transcript.getExons().size() <= 1) {
			return;
		}
		List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons(), false);
		int inc = 1;
		for (int i = 0; i < exons.size() - 1; i += inc) {
			inc = 1;
			Exon leftExon = exons.get(i);
			for (int j = i + 1; j < exons.size(); ++j) {
				Exon rightExon = exons.get(j);
				if (leftExon.overlaps(rightExon) || leftExon.isAdjacentTo(rightExon)) {
					try {
						mergeExons(leftExon, rightExon);
					}
					catch (AnnotationEditorException e) {
					}
					++inc;
				}
			}
		}
	}
	
	private CDS createCDS(Transcript transcript) {
		Date date = new Date();
		String uniqueName = transcript.getUniqueName() + "-CDS";
		CDS cds = new CDS(transcript.getOrganism(), uniqueName, transcript.isAnalysis(),
				transcript.isObsolete(), null, transcript.getConfiguration());
		cds.setFeatureLocation(new FeatureLocation());
		cds.setStrand(transcript.getStrand());
		cds.getFeatureLocation().setSourceFeature(transcript.getFeatureLocation().getSourceFeature());
		cds.setTimeAccessioned(date);
		cds.setTimeLastModified(date);
		return cds;
	}
	
	public boolean isManuallySetTranslationStart(CDS cds) {
		for (Comment comment : cds.getComments()) {
			if (comment.getComment().equals(MANUALLY_SET_TRANSLATION_START)) {
				return true;
			}
		}
		return false;
	}
	
	public void setManuallySetTranslationStart(CDS cds, boolean manuallySetTranslationStart) {
		if (manuallySetTranslationStart && isManuallySetTranslationStart(cds)) {
			return;
		}
		if (!manuallySetTranslationStart && !isManuallySetTranslationStart(cds)) {
			return;
		}
		if (manuallySetTranslationStart) {
			cds.addComment(MANUALLY_SET_TRANSLATION_START);
		}
		if (!manuallySetTranslationStart) {
			cds.deleteComment(MANUALLY_SET_TRANSLATION_START);
		}
	}

	public boolean isManuallySetTranslationEnd(CDS cds) {
		for (Comment comment : cds.getComments()) {
			if (comment.getComment().equals(MANUALLY_SET_TRANSLATION_END)) {
				return true;
			}
		}
		return false;
	}

	public void setManuallySetTranslationEnd(CDS cds, boolean manuallySetTranslationEnd) {
		if (manuallySetTranslationEnd && isManuallySetTranslationEnd(cds)) {
			return;
		}
		if (!manuallySetTranslationEnd && !isManuallySetTranslationEnd(cds)) {
			return;
		}
		if (manuallySetTranslationEnd) {
			cds.addComment(MANUALLY_SET_TRANSLATION_END);
		}
		if (!manuallySetTranslationEnd) {
			cds.deleteComment(MANUALLY_SET_TRANSLATION_END);
		}
	}

	public void addComment(AbstractSingleLocationBioFeature feature, String comment) {
		feature.addComment(comment);
		feature.setTimeLastModified(new Date());
	}

	public void deleteComment(AbstractSingleLocationBioFeature feature, String comment) {
		feature.deleteComment(comment);
		feature.setTimeLastModified(new Date());
	}

	public void updateComment(AbstractSingleLocationBioFeature feature, String oldComment, String newComment) {
		for (Comment comment : feature.getComments()) {
			if (comment.getComment().equals(oldComment)) {
				comment.setComment(newComment);
				feature.setTimeLastModified(new Date());
				break;
			}
		}
	}
	
	public void setDescription(AbstractSingleLocationBioFeature feature, String description) {
		feature.setDescription(description);
		feature.setTimeLastModified(new Date());
	}

	public void setSymbol(AbstractSingleLocationBioFeature feature, String symbol) {
		feature.setSymbol(symbol);
		feature.setTimeLastModified(new Date());
	}

	public void setStatus(AbstractSingleLocationBioFeature feature, String status) {
		feature.setStatus(status);
		feature.setTimeLastModified(new Date());
	}
	
	public void deleteStatus(AbstractSingleLocationBioFeature feature) {
		feature.deleteStatus();
		feature.setTimeLastModified(new Date());
	}
	
	public void addNonPrimaryDBXref(AbstractSingleLocationBioFeature feature, String db, String accession) {
		feature.addNonPrimaryDBXref(db, accession);
		feature.setTimeLastModified(new Date());
	}
	
	public void deleteNonPrimaryDBXref(AbstractSingleLocationBioFeature feature, String db, String accession) {
		feature.deleteNonPrimaryDBXref(db, accession);
		feature.setTimeLastModified(new Date());
	}

	public void updateNonPrimaryDBXref(AbstractSingleLocationBioFeature feature, String oldDb, String oldAccession, String newDb, String newAccession) {
		for (DBXref dbxref : feature.getNonPrimaryDBXrefs()) {
			if (dbxref.getDb().getName().equals(oldDb) && dbxref.getAccession().equals(oldAccession)) {
				dbxref.getDb().setName(newDb);
				dbxref.setAccession(newAccession);
				feature.setTimeLastModified(new Date());
				break;
			}
		}
	}
	
	public void addNonReservedProperty(AbstractSingleLocationBioFeature feature, String tag, String value) {
		feature.addNonReservedProperty(tag, value);
		feature.setTimeLastModified(new Date());
	}
	
	public void deleteNonReservedProperty(AbstractSingleLocationBioFeature feature, String tag, String value) {
		feature.deleteNonReservedProperty(tag, value);
		feature.setTimeLastModified(new Date());
	}

	public void updateNonReservedProperty(AbstractSingleLocationBioFeature feature, String oldTag, String oldValue, String newTag, String newValue) {
		for (GenericFeatureProperty property : feature.getNonReservedProperties()) {
			if (property.getTag().equals(oldTag) && property.getValue().equals(oldValue)) {
				property.setTag(newTag);
				property.setValue(newValue);
				feature.setTimeLastModified(new Date());
				break;
			}
		}
	}
	
	private StopCodonReadThrough createStopCodonReadThrough(CDS cds) {
		Date date = new Date();
		String uniqueName = cds.getUniqueName() + "-stop_codon_read_through";
		StopCodonReadThrough stopCodonReadThrough = new StopCodonReadThrough(cds.getOrganism(), uniqueName, cds.isAnalysis(),
				cds.isObsolete(), null, cds.getConfiguration());
		stopCodonReadThrough.setFeatureLocation(new FeatureLocation());
		stopCodonReadThrough.setStrand(cds.getStrand());
		stopCodonReadThrough.getFeatureLocation().setSourceFeature(cds.getFeatureLocation().getSourceFeature());
		stopCodonReadThrough.setTimeAccessioned(date);
		stopCodonReadThrough.setTimeLastModified(date);
		return stopCodonReadThrough;
	}
	
	private void fireAnnotationChangeEvent(AbstractSingleLocationBioFeature feature, AbstractSingleLocationBioFeature topLevelFeature, AnnotationChangeEvent.Operation operation) {
		/*
		for (AnnotationChangeListener listener : listeners) {
			listener.handleChangeEvent(new AnnotationChangeEvent(this, feature, topLevelFeature, operation));
		}
		*/
		/*
		if (operation.equals(AnnotationChangeEvent.Operation.DELETE)) {
			if (feature.equals(topLevelFeature)) {
				session.getDataStore().removeFromStore(topLevelFeature);
			}
			else {
				session.getDataStore().addToStore(topLevelFeature);
			}
		}
		else {
			session.getDataStore().addToStore(topLevelFeature);
		}
		*/
	}
	
	private void updateGeneBoundaries(Gene gene) {
		if (gene == null) {
			return;
		}
		int geneFmax = Integer.MIN_VALUE;
		int geneFmin = Integer.MAX_VALUE;
		for (Transcript t : gene.getTranscripts()) {
			if (t.getFmin() < geneFmin) {
				geneFmin = t.getFmin();
			}
			if (t.getFmax() > geneFmax) {
				geneFmax = t.getFmax();
			}
		}
		gene.setFmin(geneFmin);
		gene.setFmax(geneFmax);
		gene.setTimeLastModified(new Date());
	}
	
	public void addAnnotationChangeListener(AnnotationChangeListener listener) {
		listeners.add(listener);
	}
	
	public void removeAnnotationChangeListener(AnnotationChangeListener listener) {
		listeners.remove(listener);
	}
	
	private AbstractSingleLocationBioFeature getTopLevelFeature(AbstractSingleLocationBioFeature feature) {
		Collection<? extends AbstractSingleLocationBioFeature> parents = feature.getParents();
		if (parents.size() > 0) {
			return getTopLevelFeature(parents.iterator().next());
		}
		else {
			return feature;
		}
	}
	
	public interface AnnotationChangeListener extends EventListener {
		
		public void handleChangeEvent(AnnotationChangeEvent event);
		
	}
	
	public static class AnnotationChangeEvent extends EventObject {

		private Operation operation;
		private AbstractSingleLocationBioFeature feature;
		private AbstractSingleLocationBioFeature topLevelFeature;
		
		public enum Operation {
			ADD,
			DELETE,
			UPDATE
		}
		
		public AnnotationChangeEvent(Object source, AbstractSingleLocationBioFeature feature, AbstractSingleLocationBioFeature topLevelFeature, Operation operation) {
			super(source);
			this.feature = feature;
			this.topLevelFeature = topLevelFeature;
			this.operation = operation;
		}
		
		public Operation getOperation() {
			return operation;
		}
		
		public AbstractSingleLocationBioFeature getFeature() {
			return feature;
		}
		
		public AbstractSingleLocationBioFeature getTopLevelFeature() {
			return topLevelFeature;
		}
		
	}

}
