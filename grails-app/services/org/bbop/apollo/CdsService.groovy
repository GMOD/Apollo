package org.bbop.apollo

import grails.transaction.Transactional
import groovy.transform.CompileStatic

@CompileStatic
@Transactional
class CdsService {

    def featureService
    def cvTermService
    def featureRelationshipService

    public void setManuallySetTranslationStart(CDS cds, boolean manuallySetTranslationStart) {
        if (manuallySetTranslationStart && isManuallySetTranslationStart(cds)) {
            return;
        }
        if (!manuallySetTranslationStart && !isManuallySetTranslationStart(cds)) {
            return;
        }
        if (manuallySetTranslationStart) {
            featureService.addComment(cds,FeatureService.MANUALLY_SET_TRANSLATION_START)
//            cds.addComment(FeatureService.MANUALLY_SET_TRANSLATION_START);
        }
        if (!manuallySetTranslationStart) {
            featureService.deleteComment(cds,FeatureService.MANUALLY_SET_TRANSLATION_START)
//            cds.deleteComment(FeatureService.MANUALLY_SET_TRANSLATION_START);
        }
    }

    public boolean isManuallySetTranslationStart(CDS cds) {
        for (Comment comment : featureService.getComments(cds)) {
            if (comment.value.equals(FeatureService.MANUALLY_SET_TRANSLATION_START)) {
                return true;
            }
        }
        return false;
    }

    public StopCodonReadThrough getStopCodonReadThrough(CDS cds) {
        List<Feature> featureList =  featureRelationshipService.getChildrenForFeature(cds, FeatureStringEnum.STOP_CODON_READTHROUGH)
        return featureList.size()==1 ? (StopCodonReadThrough) featureList.get(0) : null
    }

    /**
     * TODO: is this right?  I think it should be CDS , not transcript?
     * @param cds
     * @param stopCodonReadThrough
     * @return
     */
    def deleteStopCodonReadThrough(CDS cds,StopCodonReadThrough stopCodonReadThrough) {
        CVTerm partOfCvTerm = cvTermService.partOf
        CVTerm childCvTerm = cvTermService.getTerm(FeatureStringEnum.STOP_CODON_READTHROUGH)
        CVTerm parentCvTerm = cvTermService.getTerm(FeatureStringEnum.TRANSCRIPT)
        featureRelationshipService.deleteChildrenForType(cds,childCvTerm,partOfCvTerm)
        featureRelationshipService.deleteParentForType(stopCodonReadThrough.feature,parentCvTerm,partOfCvTerm)
    }

    def deleteStopCodonReadThrough(CDS cds) {
        StopCodonReadThrough stopCodonReadThrough = getStopCodonReadThrough(cds);
        if (stopCodonReadThrough != null) {
            deleteStopCodonReadThrough(cds,stopCodonReadThrough);
        }

    }

    Transcript getTranscript(CDS cds) {
        List<Feature> featureList = featureRelationshipService.getParentForFeature(cds,FeatureStringEnum.TRANSCRIPT)
        featureList.size()==1 ? (Transcript) featureList.get(0)  : null
    }
}
