package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.sequence.Overlapper
import org.bbop.apollo.sequence.Strand

@Transactional(readOnly = true)
class OverlapperService implements Overlapper{


    def transcriptService
    def exonService 
    def configWrapperService 

    @Override
    boolean overlaps(Transcript transcript, Gene gene) {
        //log.debug("overlaps(Transcript transcript, Gene gene) ")
        String overlapperName = configWrapperService.overlapper.class.name
        if(overlapperName.contains("Orf")){
            return overlapsOrf(transcript,gene)
        }
        throw new AnnotationException("Only ORF overlapper supported right now")
    }

    @Override
    boolean overlaps(Transcript transcript1, Transcript transcript2) {
        //log.debug("overlaps(Transcript transcript1, Transcript transcript2) ")
        String overlapperName = configWrapperService.overlapper.class.name
        if(overlapperName.contains("Orf")){
            return overlapsOrf(transcript1,transcript2)
        }
        throw new AnnotationException("Only ORF overlapper supported right now")
    }


    boolean overlapsOrf(Transcript transcript, Gene gene) {
        //log.debug("overlapsOrf(Transcript transcript, Gene gene) ")

        for (Transcript geneTranscript : transcriptService.getTranscripts(gene)) {
            if (overlapsOrf(transcript, geneTranscript)) {
                return true;
            }
        }
        return false;
    }

    boolean overlapsOrf(Transcript transcript1, Transcript transcript2) {
        //log.debug("overlapsOrf(Transcript transcript1, Transcript transcript2) ")
        if ((transcriptService.isProteinCoding(transcript1) && transcriptService.isProteinCoding(transcript2))
                && ((transcriptService.getGene(transcript1) == null || transcriptService.getGene(transcript2) == null) || (!(transcriptService.getGene(transcript1) instanceof Pseudogene) && !(transcriptService.getGene(transcript2) instanceof Pseudogene)))) {

            CDS cds = transcriptService.getCDS(transcript1);

            if (overlaps(cds,transcriptService.getCDS(transcript2)) &&  (overlaps(transcriptService.getCDS(transcript2),cds)))  {
                List<Exon> exons1 = exonService.getSortedExons(transcript1);
                List<Exon> exons2 = exonService.getSortedExons(transcript2);
                return exonsOverlap(exons1, exons2, true);
            }
        }
        return false
    }

    private boolean exonsOverlap(List<Exon> exons1, List<Exon> exons2, boolean checkStrand) {
        //log.debug("boolean exonsOverlap(List<Exon> exons1, List<Exon> exons2, boolean checkStrand) ")
        CDS cds1 = transcriptService.getCDS(exonService.getTranscript(exons1.get(0)))
        CDS cds2 = transcriptService.getCDS(exonService.getTranscript(exons2.get(0)))
        int cds1TranslStart = cds1.strand.equals(Strand.POSITIVE.value) ? cds1.fmin : cds1.fmax
        int cds2TranslStart = cds2.strand.equals(Strand.POSITIVE.value) ? cds2.fmin : cds2.fmax
        for (int i = 0; i < exons1.size(); i++) {
            for (int j = 0; j < exons2.size(); j++) {
                Exon exon1 = (Exon) exons1.get(i);
                Exon exon2 = (Exon) exons2.get(j);
                if (overlaps(exon1, exon2)) {
                    if (checkStrand) {
                        if (overlaps(cds1,cds2) && cds1TranslStart % 3 == cds2TranslStart % 3) {
                            return true
                        }
                    }
                    else {
                        return true
                    }
                }
            }
        }
        return false;
    }

    boolean overlaps(Feature leftFeature, Feature rightFeature, boolean compareStrands = true) {
        //log.debug("overlaps(Feature leftFeature, Feature rightFeature, boolean compareStrands)")
        return overlaps(leftFeature.featureLocation, rightFeature.featureLocation, compareStrands)
    }

    boolean overlaps(FeatureLocation leftFeatureLocation, FeatureLocation rightFeatureLocation, boolean compareStrands = true) {
        //log.debug("overlaps(FeatureLocation leftFeatureLocation, FeatureLocation rightFeatureLocation, boolean compareStrands)")
        if (leftFeatureLocation.sequence != rightFeatureLocation.sequence) {
            return false;
        }
        int thisFmin = leftFeatureLocation.getFmin();
        int thisFmax = leftFeatureLocation.getFmax();
        int thisStrand = leftFeatureLocation.getStrand();
        int otherFmin = rightFeatureLocation.getFmin();
        int otherFmax = rightFeatureLocation.getFmax();
        int otherStrand = rightFeatureLocation.getStrand();
        boolean strandsOverlap = compareStrands ? thisStrand == otherStrand : true;
        if (strandsOverlap &&
                overlaps(thisFmin,thisFmax,otherFmin,otherFmax)
                ) {
            return true;
        }
        return false;
    }

    boolean overlaps(int leftFmin, int leftFmax,int rightFmin,int rightFmax) {
        return (leftFmin <= rightFmin && leftFmax > rightFmin ||
                        leftFmin >= rightFmin && leftFmin < rightFmax)
    }


}
