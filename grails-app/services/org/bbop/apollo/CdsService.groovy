package org.bbop.apollo

import org.bbop.apollo.gwt.shared.FeatureStringEnum

import grails.transaction.Transactional
import org.bbop.apollo.sequence.Strand

@Transactional
class CdsService {

    public static final String MANUALLY_SET_TRANSLATION_START = "Manually set translation start";
    public static final String MANUALLY_SET_TRANSLATION_END = "Manually set translation end";

    def featureRelationshipService
    def featurePropertyService
    def transcriptService
    def featureService
    def exonService
    def sequenceService
    def overlapperService
    
    public void setManuallySetTranslationStart(CDS cds, boolean manuallySetTranslationStart) {
        if (manuallySetTranslationStart && isManuallySetTranslationStart(cds)) {
            return;
        }
        if (!manuallySetTranslationStart && !isManuallySetTranslationStart(cds)) {
            return;
        }
        if (manuallySetTranslationStart) {
            featurePropertyService.addComment(cds, MANUALLY_SET_TRANSLATION_START)
        }
        if (!manuallySetTranslationStart) {
            featurePropertyService.deleteComment(cds, MANUALLY_SET_TRANSLATION_START)
        }
    }

    public boolean isManuallySetTranslationStart(CDS cds) {
        for (Comment comment : featurePropertyService.getComments(cds)) {
            if (comment.value.equals(MANUALLY_SET_TRANSLATION_START)) {
                return true;
            }
        }
        return false;
    }


    public boolean isManuallySetTranslationEnd(CDS cds) {

        for (Comment comment : featurePropertyService.getComments(cds)) {
            if (comment.value.equals(MANUALLY_SET_TRANSLATION_END)) {
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
            featurePropertyService.addComment(cds, MANUALLY_SET_TRANSLATION_END)
        }
        if (!manuallySetTranslationEnd) {
            featurePropertyService.deleteComment(cds, MANUALLY_SET_TRANSLATION_END)
        }
    }


    /**
     * TODO: is this right?  I think it should be CDS , not transcript?
     * TODO: is this just remove parents and children?
     * @param cds
     * @param stopCodonReadThrough
     * @return
     */
    def deleteStopCodonReadThrough(CDS cds, StopCodonReadThrough stopCodonReadThrough) {
        featureRelationshipService.deleteChildrenForTypes(cds, StopCodonReadThrough.ontologyId)
        featureRelationshipService.deleteParentForTypes(stopCodonReadThrough, Transcript.ontologyId)
    }

    def deleteStopCodonReadThrough(CDS cds) {
        StopCodonReadThrough stopCodonReadThrough = (StopCodonReadThrough) featureRelationshipService.getChildForFeature(cds,StopCodonReadThrough.ontologyId)
        if (stopCodonReadThrough != null) {
            deleteStopCodonReadThrough(cds, stopCodonReadThrough);
        }

    }

    def getStopCodonReadThrough(CDS cds){
        return featureRelationshipService.getChildrenForFeatureAndTypes(cds,StopCodonReadThrough.ontologyId)
    }

    public StopCodonReadThrough createStopCodonReadThrough(CDS cds) {
        String uniqueName = cds.getUniqueName() + FeatureStringEnum.STOP_CODON_READHTHROUGH_SUFFIX.value;
        StopCodonReadThrough stopCodonReadThrough = new StopCodonReadThrough(
                uniqueName: uniqueName
                ,name: uniqueName
                , isAnalysis: cds.isIsAnalysis()
                , isObsolete: cds.isIsObsolete()
        ).save(failOnError: true)

        cds.featureLocations.each {
            FeatureLocation featureLocation = new FeatureLocation(
                    sequence: it.sequence
                    ,feature: stopCodonReadThrough
                    ,fmin: it.fmin
                    ,fmax: it.fmax
                    ,strand: cds.strand
            ).save(failOnError: true)

            stopCodonReadThrough.addToFeatureLocations(featureLocation)
        }

        stopCodonReadThrough.save(flush: true)

        return stopCodonReadThrough;
    }

    def setStopCodonReadThrough(CDS cds, StopCodonReadThrough stopCodonReadThrough, boolean replace = true) {
        if (replace) {
            featureRelationshipService.setChildForType(cds,stopCodonReadThrough)
        }

        FeatureRelationship fr = new FeatureRelationship(
                parentFeature: cds
                , childFeature: stopCodonReadThrough
                , rank: 0 // TODO: Do we need to rank the order of any other transcripts?
        ).save(insert: true,failOnError: true)
        cds.addToParentFeatureRelationships(fr);
        stopCodonReadThrough.addToChildFeatureRelationships(fr)

        stopCodonReadThrough.save(failOnError: true)
        cds.save(flush: true,failOnError: true)

    }
    
    def getResiduesFromCDS(CDS cds) {
        // New implementation that infers CDS based on overlapping exons
        Transcript transcript = transcriptService.getTranscript(cds)
        List <Exon> exons = transcriptService.getSortedExons(transcript,true)
        String residues = ""
        for(Exon exon : exons) {
            if (!overlapperService.overlaps(exon,cds)) {
                continue
            }
            int fmin = exon.fmin < cds.fmin ? cds.fmin : exon.fmin
            int fmax = exon.fmax > cds.fmax ? cds.fmax : exon.fmax
            int localStart
            int localEnd
            if (cds.isNegativeStrand()) {
                localEnd = featureService.convertSourceCoordinateToLocalCoordinate((Feature) exon, fmin) + 1
                localStart = featureService.convertSourceCoordinateToLocalCoordinate((Feature) exon, fmax) + 1
            } 
            else {
                localStart = featureService.convertSourceCoordinateToLocalCoordinate((Feature) exon, fmin)
                localEnd = featureService.convertSourceCoordinateToLocalCoordinate((Feature) exon, fmax)
            }
            residues += sequenceService.getResiduesFromFeature((Feature) exon).substring(localStart, localEnd)
        }
        return residues
    }

}
