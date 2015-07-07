package org.bbop.apollo

import grails.transaction.Transactional
import grails.transaction.NotTransactional

@Transactional
class FeatureRelationshipService {

    @NotTransactional
    List<Feature> getChildrenForFeatureAndTypes(Feature feature, String... ontologyIds) {

        def childRelations=feature.parentFeatureRelationships.findAll() {
            it.childFeature.ontologyId in ontologyIds
        }
        return childRelations.collect { it ->
            it.childFeature
        }

        //incurs overhead query
        //List<Feature> childFeatures = feature.parentFeatureRelationships*.childFeature
        //List<Feature> childFeatures = FeatureRelationship.findAllByParentFeature(feature)*.childFeature
    }



    @NotTransactional
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

    @NotTransactional
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
    @NotTransactional
    List<Feature> getParentsForFeature(Feature feature, String... ontologyIds) {
        def parentRelations=feature.childFeatureRelationships.findAll() {
            it.parentFeature.ontologyId in ontologyIds
        }
        return parentRelations.collect { it ->
            it.parentFeature
        }
    }

    def deleteRelationships(Feature feature, String parentOntologyId, String childOntologyId) {
        deleteChildrenForTypes(feature, childOntologyId)
        deleteParentForTypes(feature, parentOntologyId)
    }

    def setChildForType(Feature parentFeature, Feature childFeature) {
        List<FeatureRelationship> results = FeatureRelationship.findAllByParentFeature(parentFeature).findAll() {
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
//            featureRelationship.childFeature.delete()
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

    @NotTransactional
    List<Frameshift> getFeaturePropertyForTypes(Transcript transcript, List<String> strings) {
        return (List<Frameshift>) FeatureProperty.findAllByFeaturesInListAndOntologyIdsInList([transcript], strings)
    }

    @NotTransactional
    List<Feature> getChildren(Feature feature) {
//        List<Feature> childFeatures = (List<Feature>) Feature.executeQuery("select fr.childFeature from FeatureRelationship fr where fr.parentFeature = :parentFeature",["parentFeature":feature])
//        return childFeatures
        // HQL commented out due to issue with exporting GFF3 (inability of calculating CDS segments)
        def exonRelations=feature.parentFeatureRelationships.findAll()
        return exonRelations.collect { it ->
            it.childFeature
        }
        //slow query
        //return FeatureRelationship.findAllByParentFeature(feature)*.childFeature
    }

    /**
     * Iterate to all of the children, and delete the child and thereby the feature relationship automatically.
     * @param feature
     * @return
     */
    def deleteFeatureAndChildren(Feature feature) {

//        Feature.withNewTransaction {
////            featureEventService.deleteHistory(featureId)
////            FeatureEvent.executeUpdate("delete  from FeatureEvent fe where fe.featureId = :featureId",[featureId:feature.id])
//            FeatureEvent.executeUpdate("delete  from FeatureEvent fe where fe.uniqueName = :uniqueName",[uniqueName:feature.uniqueName])
//        }

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
