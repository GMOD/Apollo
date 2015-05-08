package org.bbop.apollo

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.hibernate.Hibernate

@Transactional
class AnnotatorService {

    def permissionService
    def requestHandlingService
    def sessionFactory

    def getAppState() {
        JSONObject appStateObject = new JSONObject()
        def organismList = permissionService.getOrganismsForCurrentUser()
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(permissionService.currentUser, true)
        Long defaultOrganismId = userOrganismPreference ? userOrganismPreference.organism.id : null


//        request.session.getAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value) == organism.directory

        log.debug "organism list: ${organismList}"
        log.debug "finding all organisms: ${Organism.count}"

        JSONArray organismArray = new JSONArray()
        for (Organism organism in organismList) {
            Integer annotationCount =  Feature.executeQuery("select count(distinct f) from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o = :organism and f.class in (:viewableTypes)",[organism:organism,viewableTypes:requestHandlingService.viewableAnnotationList])[0] as Integer
            Integer sequenceCount = Sequence.countByOrganism(organism)
            JSONObject jsonObject = [
                    id             : organism.id as Long,
                    commonName     : organism.commonName,
                    blatdb         : organism.blatdb,
                    directory      : organism.directory,
                    annotationCount: annotationCount,
                    sequences      : sequenceCount ,
                    genus          : organism.genus,
                    species        : organism.species,
                    valid          : organism.valid,
                    currentOrganism: defaultOrganismId != null ? organism.id == defaultOrganismId : false
            ] as JSONObject
            organismArray.add(jsonObject)
        }
        appStateObject.put("organismList",organismArray)
        UserOrganismPreference currentUserOrganismPreference =  permissionService.currentOrganismPreference
        appStateObject.put("currentOrganism", currentUserOrganismPreference.organism)


        log.info "the current sequence ${currentUserOrganismPreference.sequence}"
        if(!currentUserOrganismPreference.sequence){
            Sequence sequence = Sequence.findByOrganism(currentUserOrganismPreference.organism)
            currentUserOrganismPreference.sequence = sequence
            currentUserOrganismPreference.save()
        }
        appStateObject.put("currentSequence",currentUserOrganismPreference.sequence)


        if(currentUserOrganismPreference.startbp && currentUserOrganismPreference.endbp){
            appStateObject.put("currentStartBp",currentUserOrganismPreference.startbp)
            appStateObject.put("currentEndBp",currentUserOrganismPreference.endbp)
        }


        return appStateObject
    }
}
