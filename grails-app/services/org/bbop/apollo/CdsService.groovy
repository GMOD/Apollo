package org.bbop.apollo

import grails.transaction.Transactional
import grails.compiler.GrailsCompileStatic

@GrailsCompileStatic
@Transactional
class CdsService {

    public static final String MANUALLY_SET_TRANSLATION_START = "Manually set translation start";
    public static final String MANUALLY_SET_TRANSLATION_END = "Manually set translation end";

    FeatureService featureService
//    CvTermService cvTermService
    FeatureRelationshipService featureRelationshipService
    FeaturePropertyService featurePropertyService

    public void setManuallySetTranslationStart(CDS cds, boolean manuallySetTranslationStart) {
        if (manuallySetTranslationStart && isManuallySetTranslationStart(cds)) {
            return;
        }
        if (!manuallySetTranslationStart && !isManuallySetTranslationStart(cds)) {
            return;
        }
        if (manuallySetTranslationStart) {
            featurePropertyService.addComment(cds, MANUALLY_SET_TRANSLATION_START)
//            cds.addComment(FeatureService.MANUALLY_SET_TRANSLATION_START);
        }
        if (!manuallySetTranslationStart) {
            featurePropertyService.deleteComment(cds, MANUALLY_SET_TRANSLATION_START)
//            cds.deleteComment(FeatureService.MANUALLY_SET_TRANSLATION_START);
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


//    public StopCodonReadThrough getStopCodonReadThrough(CDS cds) {
//        List<Feature> featureList = featureRelationshipService.getChildrenForFeatureAndTypes(cds, StopCodonReadThrough.ontologyId )
//        return featureList.size() == 1 ? (StopCodonReadThrough) featureList.get(0) : null
//    }

    /**
     * TODO: is this right?  I think it should be CDS , not transcript?
     * TODO: is this just remove parents and children?
     * @param cds
     * @param stopCodonReadThrough
     * @return
     */
    def deleteStopCodonReadThrough(CDS cds, StopCodonReadThrough stopCodonReadThrough) {
//        CVTerm partOfCvTerm = cvTermService.partOf
//        CVTerm childCvTerm = cvTermService.getTerm(FeatureStringEnum.STOP_CODON_READTHROUGH)
//        CVTerm parentCvTerm = cvTermService.getTerm(FeatureStringEnum.TRANSCRIPT)
        featureRelationshipService.deleteChildrenForTypes(cds, StopCodonReadThrough.ontologyId)
        featureRelationshipService.deleteParentForTypes(stopCodonReadThrough, Transcript.ontologyId)
    }

    def deleteStopCodonReadThrough(CDS cds) {
        StopCodonReadThrough stopCodonReadThrough = (StopCodonReadThrough) featureRelationshipService.getChildForFeature(cds,StopCodonReadThrough.ontologyId)
        if (stopCodonReadThrough != null) {
            deleteStopCodonReadThrough(cds, stopCodonReadThrough);
        }

    }


//    Transcript getTranscript(CDS cds) {
//        Criteria criteria = FeatureRelationship.createCriteria()
//        List<FeatureRelationship> featureRelationshipList = criteria {
//            eq("childFeature", cds)
//            eq("parentFeature.ontologyId", Transcript.ontologyId)
//        }
//
//        if (featureRelationshipList.size() == 0) {
//            return null
//        }
//
//        if (featureRelationshipList.size() > 1) {
//            log.error "More than one feature relationships found for CDS ${cds} and ID ${Transcript.ontologyId}"
//        }
//
//        return (Transcript) featureRelationshipList.get(0).parentFeature
//
//        List<Feature> featureList = featureRelationshipService.getParentForFeature(cds,Transcript.ontologyId)
//
////
////        List<Feature> featureList = featureRelationshipService.getParentForFeature(cds, FeatureStringEnum.TRANSCRIPT)
////        featureList.size() == 1 ? (Transcript) featureList.get(0) : null
//    }

    public StopCodonReadThrough createStopCodonReadThrough(CDS cds) {
//        Date date = new Date();

        String uniqueName = cds.getUniqueName() + FeatureStringEnum.STOP_CODON_READHTHROUGH_SUFFIX.value;
        StopCodonReadThrough stopCodonReadThrough = new StopCodonReadThrough(
                uniqueName: uniqueName
                , isAnalysis: cds.isIsAnalysis()
                , isObsolete: cds.isIsObsolete()
        )
//        StopCodonReadThrough stopCodonReadThrough = new StopCodonReadThrough(cds.getOrganism(), uniqueName, cds.isAnalysis(),
//                cds.isObsolete(), null, cds.getConfiguration());
        FeatureLocation featureLocation = new FeatureLocation(
                sequence: cds.featureLocation.sequence
                , feature: stopCodonReadThrough
        ).save()

        stopCodonReadThrough.addToFeatureLocations(featureLocation)
        stopCodonReadThrough.featureLocation.setStrand(cds.getStrand());

        stopCodonReadThrough.save(flush: true)

//        stopCodonReadThrough.setFeatureLocation(new FeatureLocation());
//        stopCodonReadThrough.getFeatureLocation().setSourceFeature(cds.getFeatureLocation().getSourceFeature());
//        stopCodonReadThrough.setTimeAccessioned(date);
//        stopCodonReadThrough.setTimeLastModified(date);
        return stopCodonReadThrough;
    }

    def setStopCodonReadThrough(CDS cds, StopCodonReadThrough stopCodonReadThrough, boolean replace = true) {
//        CVTerm partOfCvTerm = cvTermService.partOf
//        CVTerm stopCodonReadThroughCvTerm = cvTermService.getTerm(FeatureStringEnum.STOP_CODON_READTHROUGH)


        if (replace) {
            featureRelationshipService.setChildForType(cds,stopCodonReadThrough)
        }

        FeatureRelationship fr = new FeatureRelationship(
//                type: partOfCvTerm
                parentFeature: cds
                , childFeature: stopCodonReadThrough
                , rank: 0 // TODO: Do we need to rank the order of any other transcripts?
        ).save(insert: true)
        cds.getChildFeatureRelationships().add(fr);
        stopCodonReadThrough.getParentFeatureRelationships().add(fr);

        stopCodonReadThrough.save()
        cds.save(flush: true)

    }
}
