package org.bbop.apollo.web.dataadapter.fasta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bbop.apollo.editor.AbstractAnnotationSession;
import org.bbop.apollo.web.data.FeatureLazyResidues;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.CDS;
import org.gmod.gbol.bioObject.Exon;
import org.gmod.gbol.bioObject.FlankingRegion;
import org.gmod.gbol.bioObject.Transcript;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.io.FastaHandler;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;
import org.gmod.gbol.util.SequenceUtil;
import org.gmod.gbol.util.SequenceUtil.TranslationTable;

public class FastaIO {

	private FastaHandler handler;
	protected AbstractAnnotationSession session;
	protected TranslationTable translationTable;
	protected BioObjectConfiguration conf;

	public FastaIO(String path, String source, BioObjectConfiguration conf, AbstractAnnotationSession session, TranslationTable translationTable) throws IOException {
		this(path, source, FastaHandler.Format.TEXT, conf, session, translationTable);
	}
	
	public FastaIO(String path, String source, FastaHandler.Format format, BioObjectConfiguration conf, AbstractAnnotationSession session, TranslationTable translationTable) throws IOException {
		handler = new FastaHandler(path, FastaHandler.Mode.WRITE, format);
		this.conf = conf;
		this.session = session;
		this.translationTable = translationTable;
	}
	
	public void writeFeatures(Collection<? extends AbstractSingleLocationBioFeature> features, String seqType, Set<String> metaDataToExport) throws IOException {
		handler.writeFeatures(features, seqType, metaDataToExport);
	}
	
	public void writeFeatures(Iterator<? extends AbstractSingleLocationBioFeature> iterator, String seqType, Set<String> featureTypes, Set<String> metaDataToExport) throws IOException {
		while (iterator.hasNext()) {
			List<AbstractSingleLocationBioFeature> matchingFeatures = new ArrayList<AbstractSingleLocationBioFeature>();
			getMatchingFeature(iterator.next(), featureTypes, matchingFeatures);
			for (AbstractSingleLocationBioFeature feature : matchingFeatures) {
				feature.setResidues(getUpdatedSequence(feature, seqType, 0));
				handler.writeFeature(feature, seqType, metaDataToExport);
			}
		}
	}
	
	public void close() {
		handler.close();
	}
	
	private String getUpdatedSequence(AbstractSingleLocationBioFeature gbolFeature, String type, int flank) {
		String sequence = null;
		if (type.equals("peptide")) {
			if (gbolFeature instanceof Transcript && ((Transcript)gbolFeature).isProteinCoding()) {
				String rawSequence = session.getResiduesWithAlterationsAndFrameshifts(((Transcript)gbolFeature).getCDS());
				sequence = SequenceUtil.translateSequence(rawSequence, translationTable, true, ((Transcript)gbolFeature).getCDS().getStopCodonReadThrough() != null);
				if (sequence.charAt(sequence.length() - 1) == TranslationTable.STOP.charAt(0)) {
					sequence = sequence.substring(0, sequence.length() - 1);
				}
				int idx;
				if ((idx = sequence.indexOf(TranslationTable.STOP)) != -1) {
					String codon = rawSequence.substring(idx * 3, idx * 3 + 3);
					String aa = translationTable.getAlternateTranslationTable().get(codon);
					if (aa != null) {
						sequence = sequence.replace(TranslationTable.STOP, aa);
					}
				}
			}
			else if (gbolFeature instanceof Exon && ((Exon)gbolFeature).getTranscript().isProteinCoding()) {
				String rawSequence = getCodingSequenceInPhase(session, (Exon)gbolFeature, true);
				sequence = SequenceUtil.translateSequence(rawSequence, translationTable, true, ((Exon)gbolFeature).getTranscript().getCDS().getStopCodonReadThrough() != null);
				if (sequence.charAt(sequence.length() - 1) == TranslationTable.STOP.charAt(0)) {
					sequence = sequence.substring(0, sequence.length() - 1);
				}
				int idx;
				if ((idx = sequence.indexOf(TranslationTable.STOP)) != -1) {
					String codon = rawSequence.substring(idx * 3, idx * 3 + 3);
					String aa = translationTable.getAlternateTranslationTable().get(codon);
					if (aa != null) {
						sequence = sequence.replace(TranslationTable.STOP, aa);
					}
				}
			}
			else {
//				sequence = SequenceUtil.translateSequence(editor.getSession().getResiduesWithAlterationsAndFrameshifts(gbolFeature), editor.getConfiguration().getTranslationTable());
				sequence = "";
			}
			
		}
		else if (type.equals("cdna")) {
			if (gbolFeature instanceof Transcript || gbolFeature instanceof Exon) {
				sequence = session.getResiduesWithAlterationsAndFrameshifts(gbolFeature);
			}
			else {
				sequence = "";
			}
		}
		else if (type.equals("cds")) {
			if (gbolFeature instanceof Transcript && ((Transcript)gbolFeature).isProteinCoding()) {
				sequence = session.getResiduesWithAlterationsAndFrameshifts(((Transcript)gbolFeature).getCDS());
			}
			else if (gbolFeature instanceof Exon && ((Exon)gbolFeature).getTranscript().isProteinCoding()) {
				sequence = getCodingSequenceInPhase(session, (Exon)gbolFeature, false);
			}
			else {
//				sequence = editor.getSession().getResiduesWithAlterationsAndFrameshifts(gbolFeature);
				sequence = "";
			}
		}
		else if (type.equals("genomic")) {
			AbstractSingleLocationBioFeature genomicFeature = new AbstractSingleLocationBioFeature((Feature)((SimpleObjectIteratorInterface)gbolFeature.getWriteableSimpleObjects(conf)).next(), conf) { };
			FeatureLazyResidues sourceFeature = (FeatureLazyResidues)gbolFeature.getFeatureLocation().getSourceFeature();
			genomicFeature.getFeatureLocation().setSourceFeature(sourceFeature);
			if (flank > 0) {
				int fmin = genomicFeature.getFmin() - flank;
				if (fmin < sourceFeature.getFmin()) {
					fmin = sourceFeature.getFmin();
				}
				int fmax = genomicFeature.getFmax() + flank;
				if (fmax > sourceFeature.getFmax()) {
					fmax = sourceFeature.getFmax();
				}
				genomicFeature.setFmin(fmin);
				genomicFeature.setFmax(fmax);
			}
			gbolFeature = genomicFeature;
			sequence = session.getResiduesWithAlterationsAndFrameshifts(gbolFeature);
		}
		return sequence;
	}
	
	private String getCodingSequenceInPhase(AbstractAnnotationSession session, Exon exon, boolean removePartialCodons) {
		Transcript transcript = exon.getTranscript();
		CDS cds = transcript.getCDS();
		if (cds == null || !exon.overlaps(cds)) {
			return "";
		}
		int length = 0;
		FlankingRegion flankingRegion = new FlankingRegion(null, null, false, false, null, exon.getConfiguration());
		flankingRegion.setFeatureLocation(new FeatureLocation());
		flankingRegion.getFeatureLocation().setSourceFeature(exon.getFeatureLocation().getSourceFeature());
		flankingRegion.setStrand(exon.getStrand());
		List<Exon> exons = BioObjectUtil.createSortedFeatureListByLocation(transcript.getExons(), true);
		for (Exon e : exons) {
			if (e.equals(exon)) {
				break;
			}
			if (!e.overlaps(cds)) {
				continue;
			}
			int fmin = e.getFmin() < cds.getFmin() ? cds.getFmin() : e.getFmin();
			int fmax = e.getFmax() > cds.getFmax() ? cds.getFmax() : e.getFmax();
			flankingRegion.setFmin(fmin);
			flankingRegion.setFmax(fmax);
			length += session.getResiduesWithAlterationsAndFrameshifts(flankingRegion).length();
		}
		flankingRegion.setFmin(exon.getFmin() < cds.getFmin() ? cds.getFmin() : exon.getFmin());
		flankingRegion.setFmax(exon.getFmax() > cds.getFmax() ? cds.getFmax() : exon.getFmax());
		String residues = session.getResiduesWithAlterationsAndFrameshifts(flankingRegion);
		if (removePartialCodons) {
			int phase = length % 3 == 0 ? 0 : 3 - (length % 3);
			residues = residues.substring(phase);
			residues = residues.substring(0, residues.length() - (residues.length() % 3));
		}
		return residues;
	}
	
	private void getMatchingFeature(AbstractSingleLocationBioFeature feature, Set<String> featureTypes, List<AbstractSingleLocationBioFeature> matchingFeatures) {
		if (featureTypes.contains(feature.getType())) {
			matchingFeatures.add(feature);
		}
		for (AbstractSingleLocationBioFeature child : feature.getChildren()) {
			getMatchingFeature(child, featureTypes, matchingFeatures);
		}
	}
	
}