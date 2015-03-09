package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class FeatureRelationshipService {


    List<Feature> getChildrenForFeatureAndTypes(Feature feature, String... ontologyIds) {
        List<Feature> childFeatures = FeatureRelationship.findAllByParentFeature(feature)*.childFeature
        List<Feature> returnFeatures = new ArrayList<>()
        if (childFeatures) {
            returnFeatures.addAll(
                    childFeatures.findAll() {
                        it?.ontologyId in ontologyIds
                    }
            )
        }

        return returnFeatures
    }



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
        }.unique()
    }

    def deleteRelationships(Feature feature, String parentOntologyId, String childOntologyId) {
        deleteChildrenForTypes(feature, childOntologyId)
        deleteParentForTypes(feature, parentOntologyId)
    }

    def setChildForType(Feature parentFeature, Feature childFeature) {
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

    }

    def deleteChildrenForTypes(Feature feature, String... ontologyIds) {
        def criteria = FeatureRelationship.createCriteria()

        def featureRelationships = criteria {
            eq("parentFeature", feature)
        }.findAll() {
            ontologyIds.length==0 || it.childFeature.ontologyId in ontologyIds
        }
       
        int numRelationships = featureRelationships.size()
        for(int i = 0 ; i < numRelationships ; i++){
            FeatureRelationship featureRelationship = featureRelationships.get(i)
            removeFeatureRelationship(featureRelationship.parentFeature,featureRelationship.childFeature)
        }
    }

    def deleteParentForTypes(Feature feature, String... ontologyIds) {
        // delete transcript -> non canonical 3' splice site child relationship
        def criteria = FeatureRelationship.createCriteria()

        criteria {
            eq("childFeature", feature)
        }.findAll() {
            it.parentFeature.ontologyId in ontologyIds
        }.each {
            feature.removeFromChildFeatureRelationships(it)
        }

    }

    // based on Transcript.setCDS
    def addChildFeature(Feature parent, Feature child, boolean replace = true) {
        
        // replace if of the same type
        if (replace) {
            boolean found = false
            def criteria = FeatureRelationship.createCriteria()
            criteria {
                eq("parentFeature", parent)
            }
            .findAll() {
                it.childFeature.ontologyId == child.ontologyId
            }
            .each {
                found = true 
                it.childFeature = child
                it.save()
                return 
            }
           
            if(found){
                return
            }

        }
        
        

        FeatureRelationship fr = new FeatureRelationship(
                parentFeature: parent
                , childFeature: child
                , rank: 0 // TODO: Do we need to rank the order of any other transcripts?
        ).save(flush: true);
        parent.addToParentFeatureRelationships(fr)
        child.addToChildFeatureRelationships(fr)
        child.save(flush: true)
        parent.save(flush: true )
    }

    public void removeFeatureRelationship(Feature parentFeature, Feature childFeature) {

        FeatureRelationship featureRelationship = FeatureRelationship.findByParentFeatureAndChildFeature(parentFeature, childFeature)
        if (featureRelationship) {
            parentFeature.parentFeatureRelationships?.remove(featureRelationship)
            childFeature.childFeatureRelationships?.remove(featureRelationship)
            parentFeature.save(flush: true)
            childFeature.save(flush: true)
        }
    }

    List<Frameshift> getFeaturePropertyForTypes(Transcript transcript, List<String> strings) {
        return (List<Frameshift>) FeatureProperty.findAllByFeaturesInListAndOntologyIdsInList([transcript], strings)
    }

    List<Feature> getChildren(Feature feature) {
        return FeatureRelationship.findAllByParentFeature(feature)*.childFeature
    }

    /**
     * Iterate to all of the children, and delete the child and thereby the feature relationship automatically.
     * @param feature
     * @return
     */
    def deleteFeatureAndChildren(Feature feature) {
        
        if(feature.parentFeatureRelationships){
            def parentFeatureRelationships = feature.parentFeatureRelationships
            Iterator<FeatureRelationship> featureRelationshipIterator = parentFeatureRelationships.iterator()
            while(featureRelationshipIterator.hasNext()){
                FeatureRelationship featureRelationship = featureRelationshipIterator.next()
                deleteFeatureAndChildren(featureRelationship.childFeature)
            }
        }
        feature.delete()



    }
}
