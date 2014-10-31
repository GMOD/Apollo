package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class FeatureRelationshipService {

    def cvTermService

    List<Feature> getChildrenForFeature(Feature feature, CVTerm cvFeatureTerm, CVTerm cvRelationshipTerm) {
        Collection<Feature> features = new ArrayList<Feature>();
        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
            if (cvRelationshipTerm == fr.type && cvFeatureTerm == fr.subjectFeature.type) {
                features.add(fr.subjectFeature)
            }
        }
        return features;
    }

    List<Feature> getChildrenForFeature(Feature feature, FeatureStringEnum featureStringEnum) {
        CVTerm featureCvTerm = cvTermService.getTerm(featureStringEnum)
        return getChildrenForFeature(feature, featureCvTerm, cvTermService.partOf)
    }

    List<Feature> getParentForFeature(Feature feature, CVTerm cvFeatureTerm, CVTerm cvRelationshipTerm) {
        Collection<Feature> features = new ArrayList<Feature>();
        for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
            if (cvRelationshipTerm == fr.type && cvFeatureTerm == fr.objectFeature.type) {
                features.add(fr.objectFeature)
            }
        }
        return features;
    }

    List<Feature> getParentForFeature(Feature feature, FeatureStringEnum featureStringEnum) {
        CVTerm featureCvTerm = cvTermService.getTerm(featureStringEnum)
        return getParentForFeature(feature, featureCvTerm, cvTermService.partOf)
    }

    def deleteRelationships(Feature feature, FeatureStringEnum parentFeatureEnum, FeatureStringEnum childFeatureEnum) {
        CVTerm parentFeatureCvTerm = cvTermService.getTerm(parentFeatureEnum)
        CVTerm childFeatureCvTerm = cvTermService.getTerm(childFeatureEnum)
        CVTerm partOfCvTerm = cvTermService.partOf

        deleteChildrenForType(feature, childFeatureCvTerm, partOfCvTerm)
        deleteParentForType(feature, parentFeatureCvTerm, partOfCvTerm)

    }

    def deleteChildrenForType(Feature feature, CVTerm cvFeatureTerm, CVTerm cvRelationshipTerm) {
        // delete transcript -> non canonical 3' splice site child relationship
        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
            if (cvRelationshipTerm == fr.type
                    && cvFeatureTerm == fr.subjectFeature.type
                    && fr.getSubjectFeature().equals(feature)) {
                boolean ok = feature.getChildFeatureRelationships().remove(fr);
            }
        }
    }

    def deleteParentForType(Feature feature, CVTerm cvFeatureTerm, CVTerm cvRelationshipTerm) {
        // delete transcript -> non canonical 3' splice site child relationship
        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
            if (cvRelationshipTerm == fr.type
                    && cvFeatureTerm == fr.objectFeature.type
                    && fr.getSubjectFeature().equals(feature)) {
                boolean ok = feature.getParentFeatureRelationships().remove(fr);
            }
        }
    }

    // based on Transcript.setCDS
    def addChildFeature(Feature parent, Feature child,boolean replace=true) {
        CVTerm partOfCvTerm = cvTermService.partOf
        CVTerm childType = child.type
//        CVTerm parentType = parent.type

        // replace if of the same type
        if(replace){
            for (FeatureRelationship fr : parent.getChildFeatureRelationships()) {
                if (partOfCvTerm == fr.getType() &&
                        childType == fr.objectFeature.type) {
                    fr.setSubjectFeature(child);
                    return
                }
            }
        }

        FeatureRelationship fr = new FeatureRelationship(
                type: partOfCvTerm
                ,objectFeature: parent
                ,subjectFeature: child
                ,rank: 0 // TODO: Do we need to rank the order of any other transcripts?
        );
        parent.getChildFeatureRelationships().add(fr);
        child.getParentFeatureRelationships().add(fr);
    }
}
