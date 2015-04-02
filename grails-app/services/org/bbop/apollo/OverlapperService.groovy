package org.bbop.apollo

import org.bbop.apollo.gwt.shared.FeatureStringEnum

import grails.transaction.Transactional
import org.bbop.apollo.sequence.Overlapper

@Transactional(readOnly = true)
class OverlapperService implements Overlapper{


    def featureService 
    def transcriptService 
    def exonService 
    def configWrapperService 

    @Override
    boolean overlaps(Transcript transcript, Gene gene) {
        String overlapperName = configWrapperService.overlapper.class.name
        if(overlapperName.contains("Orf")){
            return overlapsOrf(transcript,gene)
        }
        throw new AnnotationException("Only ORF overlapper supported right now")
    }

    @Override
    boolean overlaps(Transcript transcript1, Transcript transcript2) {
        String overlapperName = configWrapperService.overlapper.class.name
        if(overlapperName.contains("Orf")){
            return overlapsOrf(transcript1,transcript2)
        }
        throw new AnnotationException("Only ORF overlapper supported right now")
    }


    boolean overlapsOrf(Transcript transcript, Gene gene) {

        for (Transcript geneTranscript : transcriptService.getTranscripts(gene)) {
            if (overlapsOrf(transcript, geneTranscript)) {
                return true;
            }
        }
        return false;
    }

    boolean overlapsOrf(Transcript transcript1, Transcript transcript2) {
        if ((transcriptService.isProteinCoding(transcript1) && transcriptService.isProteinCoding(transcript2))
                && (transcriptService.getGene(transcript1) == null || transcriptService.getGene(transcript2) == null) || (!(transcriptService.getGene(transcript1) instanceof Pseudogene) && !(transcriptService.getGene(transcript2) instanceof Pseudogene))) {

            CDS cds = transcriptService.getCDS(transcript1);

            if (featureService.overlaps(cds,transcriptService.getCDS(transcript2)) &&  (featureService.overlaps(transcriptService.getCDS(transcript2),cds)))  {
                List<Exon> exons1 = exonService.getSortedExons(transcript1);
                List<Exon> exons2 = exonService.getSortedExons(transcript2);
                return exonsOverlap(exons1, exons2, true);
            }
        }

    }

    private boolean exonsOverlap(List<Exon> exons1, List<Exon> exons2, boolean checkFrame) {
        int i = 0;
        int j = 0;
        while (i < exons1.size() && j < exons2.size()) {
            Exon exon1 = (Exon)exons1.get(i);
            Exon exon2 = (Exon)exons2.get(j);
            if (featureService.overlaps(exon1,exon2)) {
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
