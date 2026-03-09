package org.bbop.apollo

import grails.gorm.transactions.Transactional
import org.hibernate.Hibernate

@Transactional(readOnly = true)
class FeatureRelationshipService {

    List<Feature> getChildrenForFeatureAndTypes(Feature feature, String... ontologyIds) {
        def list = new ArrayList<Feature>()
        if (feature?.parentFeatureRelationships != null) {
            for (FeatureRelationship fr in feature.parentFeatureRelationships) {
                if (ontologyIds.size() == 0 || (fr && ontologyIds.contains(fr.childFeature.ontologyId))) {
                    list.add((Feature) Hibernate.unproxy(fr.childFeature))
                }
            }
        }

        return list
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
        def list = new ArrayList<Feature>()
        if (feature?.childFeatureRelationships != null) {
            for (FeatureRelationship fr in feature.childFeatureRelationships) {
                if (ontologyIds.size() == 0 || (fr && ontologyIds.contains(fr.parentFeature.ontologyId))) {
                    list.add((Feature) Hibernate.unproxy(fr.parentFeature))
                }
            }
        }

        return list
    }

    @Transactional
    def deleteRelationships(Feature feature, String parentOntologyId, String childOntologyId) {
        deleteChildrenForTypes(feature, childOntologyId)
        deleteParentForTypes(feature, parentOntologyId)
    }

    @Transactional
    def setChildForType(Feature parentFeature, Feature childFeature) {
        List<FeatureRelationship> allResults = FeatureRelationship.findAllByParentFeature(parentFeature)
        List<FeatureRelationship> results = []
        for (FeatureRelationship fr in allResults) {
            if (fr.childFeature.ontologyId == childFeature.ontologyId) {
                results.add(fr)
            }
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

    @Transactional
    def deleteChildrenForTypes(Feature feature, String... ontologyIds) {
        def criteria = FeatureRelationship.createCriteria()

        def allRelationships = criteria {
            eq("parentFeature", feature)
        }
        List<FeatureRelationship> featureRelationships = []
        for (FeatureRelationship fr in allRelationships) {
            if (ontologyIds.length == 0 || fr.childFeature.ontologyId in ontologyIds) {
                featureRelationships.add(fr)
            }
        }

        int numRelationships = featureRelationships.size()
        for (int i = 0; i < numRelationships; i++) {
            FeatureRelationship featureRelationship = featureRelationships.get(i)
            removeFeatureRelationship(featureRelationship.parentFeature, featureRelationship.childFeature)
        }
    }

    @Transactional
    def deleteParentForTypes(Feature feature, String... ontologyIds) {
        // delete transcript -> non canonical 3' splice site child relationship
        def criteria = FeatureRelationship.createCriteria()

        def allRelationships = criteria {
            eq("childFeature", feature)
        }
        for (FeatureRelationship fr in allRelationships) {
            if (fr.parentFeature.ontologyId in ontologyIds) {
                feature.removeFromChildFeatureRelationships(fr)
            }
        }

    }

    // based on Transcript.setCDS
    @Transactional
    def addChildFeature(Feature parent, Feature child, boolean replace = true) {

        // replace if of the same type
        if (replace) {
            boolean found = false
            def criteria = FeatureRelationship.createCriteria()
            def allRelationships = criteria {
                eq("parentFeature", parent)
            }
            for (FeatureRelationship fr in allRelationships) {
                if (fr.childFeature.ontologyId == child.ontologyId) {
                    found = true
                    fr.childFeature = child
                    fr.save()
                    break
                }
            }

            if (found) {
                return
            }

        }



        FeatureRelationship fr = new FeatureRelationship(
                parentFeature: parent
                , childFeature: child
        ).save(flush: true);
        parent.addToParentFeatureRelationships(fr)
        child.addToChildFeatureRelationships(fr)
        child.save(flush: true)
        parent.save(flush: true)
    }

    @Transactional
    void removeFeatureRelationship(Feature parentFeature, Feature childFeature) {

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
        List<Feature> children = new ArrayList<>()
        for (FeatureRelationship fr in feature.parentFeatureRelationships) {
            children.add((Feature) Hibernate.unproxy(fr.childFeature))
        }
        return children
    }

    /**
     * Iterate to all of the children, and delete the child and thereby the feature relationship automatically.
     *
     *
     * @param feature
     * @return
     */
    @Transactional
    def deleteFeatureAndChildren(Feature feature) {

        // if grandchildren then delete those
        for (FeatureRelationship featureRelationship in feature.parentFeatureRelationships) {
            if (featureRelationship.childFeature?.parentFeatureRelationships) {
                deleteFeatureAndChildren(featureRelationship.childFeature)
            }
        }

        // create a list of relationships to remove (assume we have no grandchildren here)
        List<FeatureRelationship> relationshipsToRemove = []
        for (FeatureRelationship featureRelationship in feature.parentFeatureRelationships) {
            relationshipsToRemove << featureRelationship
        }

        // actually delete those
        for (FeatureRelationship fr in relationshipsToRemove) {
            fr.childFeature.delete()
            feature.removeFromParentFeatureRelationships(fr)
            fr.delete()
        }

        // last, delete self or save updated relationships
        if (!feature.parentFeatureRelationships && !feature.childFeatureRelationships) {
            feature.delete(flush: true)
        } else {
            feature.save(flush: true)
        }

    }
}
