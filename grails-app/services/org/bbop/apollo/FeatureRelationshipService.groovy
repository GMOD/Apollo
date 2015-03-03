package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class FeatureRelationshipService {

//    def cvTermService

//    List<Feature> getChildrenForFeature(Feature feature, CVTerm cvFeatureTerm, CVTerm cvRelationshipTerm) {
//        Collection<Feature> features = new ArrayList<Feature>();
//        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
//            if (cvRelationshipTerm == fr.type && cvFeatureTerm == fr.childFeature.type) {
//                features.add(fr.childFeature)
//            }
//        }
//        return features;
//    }

//    List<Feature> getChildrenForFeatureAndTypes(Feature feature, FeatureStringEnum featureStringEnum) {
//        CVTerm featureCvTerm = cvTermService.getTerm(featureStringEnum)
//        return getChildrenForFeatureAndTypes(feature, featureCvTerm, cvTermService.partOf)
//    }

    List<Feature> getChildrenForFeatureAndTypes(Feature feature, String... ontologyIds) {
        List<Feature> childFeatures = FeatureRelationship.findAllByParentFeature(feature)*.childFeature
        List<Feature> returnFeatures = new ArrayList<>()
        if(childFeatures) {
            returnFeatures.addAll(
                    childFeatures.findAll() {
                        it?.ontologyId in ontologyIds
                    }
            )
        }

        return returnFeatures
    }

//    List<Feature> getParentForFeature(Feature feature, CVTerm cvFeatureTerm, CVTerm cvRelationshipTerm) {
//        Collection<Feature> features = new ArrayList<Feature>();
//        for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
//            if (cvRelationshipTerm == fr.type && cvFeatureTerm == fr.parentFeature.type) {
//                features.add(fr.parentFeature)
//            }
//        }
//        return features;
//    }

//    List<Feature> getParentForFeature(Feature feature, FeatureStringEnum featureStringEnum) {
//        CVTerm featureCvTerm = cvTermService.getTerm(featureStringEnum)
//        return getParentForFeature(feature, featureCvTerm, cvTermService.partOf)
//    }


    Feature getChildForFeature(Feature feature, String ontologyId) {
        List<Feature> featureList = getChildrenForFeatureAndTypes(feature, ontologyId)

        if (featureList.size() == 0) {
            return null
        }

        if (featureList.size() > 1) {
            log.error "More than one child feature relationships found for ${feature} and ID ${ontologyId}"
            return null
        }

        return featureList.get(0)
    }


    Feature getParentForFeature(Feature feature, String... ontologyId) {
        List<Feature> featureList = getParentsForFeature(feature, ontologyId)

        if (featureList.size() == 0) {
            return null
        }

        if (featureList.size() > 1) {
            log.error "More than one feature relationships parent found for ${feature} and ID ${ontologyId}"
            return null
        }

        return featureList.get(0)
    }

    List<Feature> getParentsForFeature(Feature feature, String... ontologyIds) {
        List<String> ontologyIdList = new ArrayList<>()
        ontologyIdList.addAll(ontologyIds)
        return FeatureRelationship.findAllByChildFeature(feature)*.parentFeature.findAll() {
            ontologyIdList.empty || (it && ontologyIdList.contains(it.ontologyId))
        }
    }

    def deleteRelationships(Feature feature, String parentOntologyId, String childOntologyId) {
        deleteChildrenForTypes(feature, childOntologyId)
        deleteParentForTypes(feature, parentOntologyId)
    }

    def setChildForType(Feature parentFeature, Feature childFeature) {
        // delete transcript -> non canonical 3' splice site child relationship
//        def criteria = FeatureRelationship.createCriteria()
//        def results = criteria{
//            eq("parentFeature", parentFeature)
//            eq("childFeature.ontologyId", childFeature.ontologyId)
//        }  find the same type . . .
        List<FeatureRelationship> results = FeatureRelationship.findAllByParentFeature(parentFeature).findAll() {
            println "evaluating: ${it.childFeature.ontologyId} vs ${childFeature.ontologyId}"
            it.childFeature.ontologyId == childFeature.ontologyId
        }


        if (results.size() == 1) {
            results.get(0).childFeature = childFeature
            return true
        } else {
            if (results.size() == 0) {
                log.info "No feature relationships exist for parent ${parentFeature} and child ${childFeature}"
            }
            if (results.size() > 1) {
                log.warn "${results.size()} feature relationships exist for parent ${parentFeature} and child ${childFeature}"
            }
            return false
        }

//        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
//            if ( ontologyId == fr.childFeature.ontologyId
//                    && fr.getSubjectFeature().equals(feature)) {
//                boolean ok = feature.getChildFeatureRelationships().remove(fr);
//            }
//        }
    }

    def deleteChildrenForTypes(Feature feature, String... ontologyIds) {
        // delete transcript -> non canonical 3' splice site child relationship
        def criteria = FeatureRelationship.createCriteria()

        criteria {
            eq("parentFeature", feature)
        }.findAll() {
            it.childFeature.ontologyId in ontologyIds
        }.each {
            feature.removeFromChildFeatureRelationships(it)
        }

//        for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
//            if ( ontologyId == fr.childFeature.ontologyId
//                    && fr.getSubjectFeature().equals(feature)) {
//                boolean ok = feature.getChildFeatureRelationships().remove(fr);
//            }
//        }
    }

    def deleteParentForTypes(Feature feature, String... ontologyIds) {
        // delete transcript -> non canonical 3' splice site child relationship
        def criteria = FeatureRelationship.createCriteria()

        criteria {
            eq("childFeature", feature)
        }.findAll() {
            it.parentFeature.ontologyId in ontologyIds
        }.each {
            feature.removeFromParentFeatureRelationships(it)
        }

    }

    // based on Transcript.setCDS
    def addChildFeature(Feature parent, Feature child, boolean replace = true) {
//        CVTerm partOfCvTerm = cvTermService.partOf
//        CVTerm childType = child.type
//        CVTerm parentType = parent.type

        // replace if of the same type

        if (replace) {
            def criteria = FeatureRelationship.createCriteria()
            criteria {
                eq("parentFeature", parent)
//                    eq("childFeature.ontologyId", child.ontologyId)
            }
            .findAll() {
                it.childFeature.ontologyId == child.ontologyId
            }
            .each {
                it.childFeature = child
//                feature.removeFromParentFeatureRelationships(it)
            }

//            for (FeatureRelationship fr : parent.getChildFeatureRelationships()) {
//                if (partOfCvTerm == fr.getType() &&
//                        childType == fr.parentFeature.type) {
//                    fr.setSubjectFeature(child);
//                    return
//                }
//            }
        }

        FeatureRelationship fr = new FeatureRelationship(
                parentFeature: parent
                , childFeature: child
                , rank: 0 // TODO: Do we need to rank the order of any other transcripts?
        );
//        parent.getChildFeatureRelationships().add(fr);
        parent.addToChildFeatureRelationships(fr)
//        child.getParentFeatureRelationships().add(fr);
        child.addToParentFeatureRelationships(fr)
    }

    public void removeFeatureRelationship(Feature parentFeature, Feature childFeature) {

        FeatureRelationship featureRelationship = FeatureRelationship.findByParentFeatureAndChildFeature(parentFeature, childFeature)
        if (featureRelationship) {
            FeatureRelationship.executeUpdate(" delete from FeatureRelationship fr where fr.id = :frid",[frid:featureRelationship.id])
        }

//        CVTerm partOfCvterm = cvTermService.partOf
////        CVTerm exonCvterm = cvTermService.getTerm(type);
//        CVTerm transcriptCvterms = cvTermService.getTerm(FeatureStringEnum.TRANSCRIPT);
//
//        // delete transcript -> exon child relationship
//        for (FeatureRelationship fr : transcript.getChildFeatureRelationships()) {
//            if (partOfCvterm == fr.type
//                    && exonCvterm == fr.childFeature.type
//                    && fr.getSubjectFeature().equals(feature)
//            ) {
//                boolean ok = transcript.getChildFeatureRelationships().remove(fr);
//                break;
//
//            }
//        }

    }

//    public void removeParentFeature(Transcript transcript, Feature feature) {
//
//
//
//        CVTerm partOfCvterm = cvTermService.partOf
////        CVTerm exonCvterm = cvTermService.getTerm(type);
//        CVTerm transcriptCvterms = cvTermService.getTerm(FeatureStringEnum.TRANSCRIPT);
//
//        // delete transcript -> exon child relationship
//        for (FeatureRelationship fr : transcript.getParentFeatureRelationships()) {
//            if (partOfCvterm == fr.type
//                    && transcriptCvterms == fr.parentFeature.type
//                    && fr.getSubjectFeature().equals(feature)
//            ) {
//                boolean ok = feature.getParentFeatureRelationships().remove(fr);
//                break;
//
//            }
//        }
//
//    }
    List<Frameshift> getFeaturePropertyForTypes(Transcript transcript, List<String> strings) {
        return (List<Frameshift>) FeatureProperty.findAllByFeaturesInListAndOntologyIdsInList([transcript], strings)
//        for (FeatureProperty featureProperty : transcript.getFeatureProperties()) {
//            if (frameshiftCvterms.contains(featureProperty.getType())) {
//                frameshiftList.add((Frameshift) featureProperty);
//            }
//        }
    }

    List<Feature> getChildren(Feature feature) {
        return FeatureRelationship.findAllByParentFeature(feature)*.childFeature
    }
}
