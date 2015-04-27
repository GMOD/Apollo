package org.bbop.apollo

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class AnnotatorService {

    def permissionService
    def requestHandlingService

    def getAppState() {
        JSONObject appStateObject = new JSONObject()
        def organismList = permissionService.getOrganismsForCurrentUser()
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(permissionService.currentUser, true)
        Long defaultOrganismId = userOrganismPreference ? userOrganismPreference.organism.id : null


//        request.session.getAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value) == organism.directory

        log.debug "organism list: ${organismList}"
        log.debug "finding all organisms: ${Organism.count}"

        JSONArray organismArray = new JSONArray()
        for (def organism in organismList) {
            Integer annotationCount = Feature.executeQuery("select count(distinct f) from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o = :organism and f.class in (:viewableTypes)",[organism:organism,viewableTypes:requestHandlingService.viewableAnnotationList])[0] as Integer
            JSONObject jsonObject = [
                    id             : organism.id as Long,
                    commonName     : organism.commonName,
                    blatdb         : organism.blatdb,
                    directory      : organism.directory,
                    annotationCount: annotationCount,
                    sequences      : organism.sequences?.size(),
                    genus          : organism.genus,
                    species        : organism.species,
                    valid          : organism.valid,
                    currentOrganism: defaultOrganismId != null ? organism.id == defaultOrganismId : false
            ] as JSONObject
            organismArray.add(jsonObject)
        }
        appStateObject.put("organismList",organismArray)
        UserOrganismPreference currentUserOrganismPreference =  permissionService.currentOrganismPreference
//        println "current org ${currrentUserOrganismPreference.organism as JSON}"
//        appStateObject.put("currentOrganism", currrentUserOrganismPreference.organism as JSON)
//        appStateObject.put("currentOrganism", convertToJSONobject(currrentUserOrganismPreference.organism) as JSON)
        appStateObject.put("currentOrganism", currentUserOrganismPreference.organism)
////        Sequence sequence = Sequence.findByOrganismAndName(currrentUserOrganismPreference.organism,currrentUserOrganismPreference.defaultSequence)


        log.info "the current sequence ${currentUserOrganismPreference.sequence}"
        if(!currentUserOrganismPreference.sequence){
            currentUserOrganismPreference.sequence = currentUserOrganismPreference.organism.sequences.iterator().next()
            currentUserOrganismPreference.save()
        }
        appStateObject.put("currentSequence",currentUserOrganismPreference.sequence)
//
//
//        JSONArray sequenceArray = new JSONArray()
//        for (Sequence sequence in currentUserOrganismPreference.organism.sequences) {
////            println "seq i . . ${sequence as JSON}"
//            JSONObject jsonObject = new JSONObject()
//            jsonObject.put("id", sequence.id)
//            jsonObject.put("name", sequence.name)
//            jsonObject.put("length", sequence.length)
//            jsonObject.put("start", sequence.start)
//            jsonObject.put("end", sequence.end)
//
//
//
////            jsonObject.put("sequence", sequence as JSON)
////            jsonObject.put("default", defaultName && defaultName == sequence.name)
////            if (defaultName == sequence.name) {
////                log.info "setting the default sequence: ${jsonObject.get("default")}"
////            }
//            sequenceArray.put(jsonObject)
//        }
//
////
//        appStateObject.put("currentSequenceList",sequenceArray)


        if(currentUserOrganismPreference.startbp && currentUserOrganismPreference.endbp){
            appStateObject.put("currentStartBp",currentUserOrganismPreference.startbp)
            appStateObject.put("currentEndBp",currentUserOrganismPreference.endbp)
        }

//        appStateObject.put("currentSequenceList",currrentUserOrganismPreference.organism.sequences)

//        println "appState obj ${appStateObject as JSON}"

//        render appStateObject as JSON

        return appStateObject
    }
}
