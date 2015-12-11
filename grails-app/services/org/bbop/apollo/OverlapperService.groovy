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
        return overlapsOrf(transcript,gene)
    }

    @Override
    boolean overlaps(Transcript transcript1, Transcript transcript2) {
        return overlapsOrf(transcript1,transcript2)
    }


    boolean overlapsOrf(Transcript transcript, Gene gene) {
        long start = System.currentTimeMillis();
        for (Transcript geneTranscript : transcriptService.getTranscripts(gene)) {
            if (transcript.uniqueName == geneTranscript.uniqueName) {
                // if transcript and geneTranscript are the same then don't test for overlap
                // to avoid false positive
                continue
            }
            if (overlapsOrf(transcript, geneTranscript)) {
                log.debug "@Duration for cdsOverlap: ${System.currentTimeMillis() - start}"
                return true;
            }
        }
        log.debug "@Duration for cdsOverlap: ${System.currentTimeMillis() - start}"
        return false;
    }

    boolean overlapsOrf(Transcript transcript1, Transcript transcript2) {
//        log.debug("overlapsOrf(Transcript transcript1, Transcript transcript2) ")
        if ((transcriptService.isProteinCoding(transcript1) && transcriptService.isProteinCoding(transcript2))
                && ((transcriptService.getGene(transcript1) == null || transcriptService.getGene(transcript2) == null) || (!(transcriptService.getGene(transcript1) instanceof Pseudogene) && !(transcriptService.getGene(transcript2) instanceof Pseudogene)))) {

            CDS cds = transcriptService.getCDS(transcript1);

            if (overlaps(cds,transcriptService.getCDS(transcript2)) &&  (overlaps(transcriptService.getCDS(transcript2),cds)))  {
                List<Exon> exons1 = exonService.getSortedExons(transcript1);
                List<Exon> exons2 = exonService.getSortedExons(transcript2);
                return cdsOverlap(exons1, exons2, true);
            }
        }
        return false
    }

    private class CDSEntity {
        // POGO for handling CDS of individual exons
        int fmin;
        int fmax;
        int length;
        int phase;
        String name;
        String uniqueName;
        Sequence sequence;
        int strand;
    }
    
    boolean overlaps(CDSEntity cds1, CDSEntity cds2) {
        //overlaps() method for POGO CDSEntity
        return overlaps(cds1.fmin, cds1.fmax, cds2.fmin, cds2.fmax)
    }
    
    private ArrayList<CDSEntity> getCDSEntities(CDS cds, List<Exon> exons) {
        ArrayList<CDSEntity> cdsEntities = new ArrayList<CDSEntity>();
        HashMap<String,String> exonFrame = new HashMap<String,String>();
        for (Exon exon : exons) {
            if (!overlaps(exon,cds)) {
                continue
            }
            int fmin = exon.fmin < cds.fmin ? cds.fmin : exon.fmin
            int fmax = exon.fmax > cds.fmax ? cds.fmax : exon.fmax
            int cdsEntityLength = fmax - fmin
            
            CDSEntity cdsEntity = new CDSEntity();
            cdsEntity.fmin = fmin;
            cdsEntity.fmax = fmax;
            cdsEntity.length = cdsEntityLength;
            cdsEntity.name = cds.name;
            cdsEntity.uniqueName = cds.uniqueName + "-cds-entity"
            cdsEntity.sequence = cds.featureLocation.sequence
            cdsEntity.strand = cds.strand
            cdsEntities.add(cdsEntity);
        }
        return cdsEntities;
    }
    
    private boolean cdsOverlap(List<Exon> exons1, List<Exon> exons2, boolean checkStrand) {
        // implementation for determining isoforms based on CDS overlaps
        if(exons1.size()==0 || exons2.size()==0){
            return false
        }
        CDS cds1 = transcriptService.getCDS( exonService.getTranscript(exons1.get(0)) )
        CDS cds2 = transcriptService.getCDS( exonService.getTranscript(exons2.get(0)) )
        ArrayList<CDSEntity> cdsEntitiesForTranscript1 = getCDSEntities(cds1, exons1)
        ArrayList<CDSEntity> cdsEntitiesForTranscript2 = getCDSEntities(cds2, exons2)
        int cds1Length = 0
        int cds1Overhang = 0
        int cds1AbsFrame = 0
        int cds1RelFrame = 0
        int cds1Frame = 0
        int cds2Length = 0
        int cds2Overhang = 0
        int cds2AbsFrame = 0
        int cds2RelFrame = 0
        int cds2Frame = 0

        for (int i = 0; i < cdsEntitiesForTranscript1.size(); i++) {
            CDSEntity c1 = cdsEntitiesForTranscript1.get(i)
            cds1Overhang = cds1Length % 3
            cds1RelFrame = (3 - cds1Overhang) % 3
            cds1AbsFrame = c1.strand == Strand.POSITIVE.value ? c1.fmin % 3 : (c1.fmax - 1) % 3
            cds1Frame = c1.strand == Strand.POSITIVE.value ? (cds1AbsFrame + cds1RelFrame) % 3 : (3 + cds1AbsFrame - cds1RelFrame) % 3

            for (int j = 0; j < cdsEntitiesForTranscript2.size(); j++) {
                CDSEntity c2 = cdsEntitiesForTranscript2.get(j)
                cds2Overhang = cds2Length % 3
                cds2RelFrame = (3 - cds2Overhang) % 3
                cds2AbsFrame = c2.strand == Strand.POSITIVE.value ? c2.fmin % 3 : (c2.fmax - 1) % 3
                cds2Frame = c2.strand == Strand.POSITIVE.value ? (cds2AbsFrame + cds2RelFrame) % 3 : (3 + cds2AbsFrame - cds2RelFrame) % 3

                log.debug "Comparing CDSEntity ${c1.fmin}-${c1.fmax} to ${c2.fmin}-${c2.fmax}"
                log.debug "CDS1 vs. CDS2 frame: ${cds1Frame} vs. ${cds2Frame}"
                if (overlaps(c1,c2)) {
                    if (checkStrand) {
                        if ((c1.strand == c2.strand) && (cds1Frame == cds2Frame)) {
                            log.debug "Conditions met"
                            return true
                        }
                    }
                    else {
                        return true
                    }
                }
                cds2Length += c2.length
            }
            cds1Length += c1.length
        }
        return false
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
