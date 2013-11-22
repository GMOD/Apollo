package org.bbop.apollo.web.overlap;

import java.util.List;

import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.CDS;
import org.gmod.gbol.bioObject.Exon;
import org.gmod.gbol.bioObject.Gene;
import org.gmod.gbol.bioObject.Transcript;
import org.gmod.gbol.bioObject.util.BioObjectUtil;

public class OrfOverlapper implements Overlapper {

	@Override
	public boolean overlaps(Transcript transcript, Gene gene) {
		for (Transcript geneTranscript : gene.getTranscripts()) {
			if (overlaps(transcript, geneTranscript)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean overlaps(Transcript transcript1, Transcript transcript2) {
		if ((transcript1.isProteinCoding() && transcript2.isProteinCoding()) && ((transcript1.getGene() == null || transcript2.getGene() == null) || (!transcript1.getGene().isPseudogene() && !transcript2.getGene().isPseudogene()))) {
			CDS cds = transcript1.getCDS();
			if (cds.overlaps(transcript2.getCDS()) && transcript2.getCDS().overlaps(cds)) {
				List<? extends AbstractSingleLocationBioFeature> exons1 = BioObjectUtil.createSortedFeatureListByLocation(transcript1.getExons(), false);
				List<? extends AbstractSingleLocationBioFeature> exons2 = BioObjectUtil.createSortedFeatureListByLocation(transcript2.getExons(), false);
				return overlaps(exons1, exons2, true);
			}
		}
		/*
		else {s
			List<? extends AbstractSingleLocationBioFeature> exons1 = BioObjectUtil.createSortedFeatureListByLocation(transcript1.getExons(), false);
			List<? extends AbstractSingleLocationBioFeature> exons2 = BioObjectUtil.createSortedFeatureListByLocation(transcript2.getExons(), false);
			return overlaps(exons1, exons2, false);
		}
		*/
		return false;
	}

	private boolean overlaps(List<? extends AbstractSingleLocationBioFeature> exons1, List<? extends AbstractSingleLocationBioFeature> exons2, boolean checkFrame) {
		int i = 0;
		int j = 0;
		while (i < exons1.size() && j < exons2.size()) {
			Exon exon1 = (Exon)exons1.get(i);
			Exon exon2 = (Exon)exons2.get(j);
			if (exon1.overlaps(exon2)) {
				if (checkFrame) {
					if (exon1.getStrand().equals(1) && exon1.getFmin() % 3 == exon2.getFmin() % 3) {
						return true;
					}
					else if (exon1.getStrand().equals(-1) && exon1.getFmax() % 3 == exon2.getFmax() % 3) {
						return true;
					}
				}
				else {
					return true;
				}
			}
			if (exon1.getFmin() < exon2.getFmin()) {
				++i;
			}
			else if (exon2.getFmin() < exon1.getFmin()) {
				++j;
			}
			else if (exon1.getFmax() < exon2.getFmax()) {
				++i;
			}
			else if (exon2.getFmax() < exon1.getFmax()) {
				++j;
			}
			else {
				++i;
				++j;
			}
		}
		return false;
	}
	
}
