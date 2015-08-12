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
        int i = 0;
        int j = 0;
        while (i < exons1.size() && j < exons2.size()) {
            Exon exon1 = (Exon)exons1.get(i);
            Exon exon2 = (Exon)exons2.get(j);
            if (overlaps(exon1,exon2)) {
                if (checkStrand) {
                    FeatureLocation exon1FeatureLocation= exon1.featureLocation
                    FeatureLocation exon2FeatureLocation= exon2.featureLocation
                    if (exon1FeatureLocation.strand.equals(Strand.POSITIVE.value) && exon1FeatureLocation.getFmin() % 3 == exon2FeatureLocation.getFmin() % 3) {
                        return true;
                    }
                    else if (exon1.strand.equals(Strand.NEGATIVE.value) && exon1FeatureLocation.getFmax() % 3 == exon2FeatureLocation.getFmax() % 3) {
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
