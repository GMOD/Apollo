package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class AnnotatorService {

    def permissionService
    def preferenceService
    def requestHandlingService

    def getAppState(String token) {
        JSONObject appStateObject = new JSONObject()
        try {
            List<Organism> organismList = permissionService.getOrganismsForCurrentUser()
            UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(permissionService.currentUser, true,token,[max: 1, sort: "lastUpdated", order: "desc"])
            log.debug "found organism preference: ${userOrganismPreference} for token ${token}"
            Long defaultOrganismId = userOrganismPreference ? userOrganismPreference.organism.id : null

            Map<Organism,Boolean> organismBooleanMap  = permissionService.userHasOrganismPermissions(PermissionEnum.ADMINISTRATE)
            Map<Sequence,Integer> sequenceIntegerMap  = [:]
            Sequence.executeQuery("select o,count(s) from Organism o join o.sequences s where o in (:organismList) group by o ",[organismList:organismList]).each(){
                sequenceIntegerMap.put(it[0],it[1])
            }
            Map<Organism,Integer> annotationCountMap = [:]

            Feature.executeQuery("select o,count(distinct f) from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o in (:organismList) and f.class in (:viewableTypes) group by o", [organismList: organismList, viewableTypes: requestHandlingService.viewableAnnotationList]).each {
                annotationCountMap.put(it[0],it[1])
            }


            JSONArray organismArray = new JSONArray()
            for (Organism organism in organismList) {
                Integer sequenceCount = sequenceIntegerMap.get(organism) ?: 0
                JSONObject jsonObject = [
                        id             : organism.id as Long,
                        commonName     : organism.commonName,
                        blatdb         : organism.blatdb,
                        directory      : organism.directory,
                        annotationCount: annotationCountMap.get(organism) ?: 0,
                        sequences      : sequenceCount,
                        genus          : organism.genus,
                        species        : organism.species,
                        valid          : organism.valid,
                        publicMode     : organism.publicMode,
                        currentOrganism: defaultOrganismId != null ? organism.id == defaultOrganismId : false,
                        editable       : organismBooleanMap.get(organism) ?: false

                ] as JSONObject
                organismArray.add(jsonObject)
            }
            appStateObject.put("organismList", organismArray)
            UserOrganismPreference currentUserOrganismPreference = preferenceService.getCurrentOrganismPreference(token)
            if(currentUserOrganismPreference){
                Organism currentOrganism = currentUserOrganismPreference?.organism
                appStateObject.put("currentOrganism", currentOrganism )


                if (!currentUserOrganismPreference.sequence) {
                    Sequence sequence = Sequence.findByOrganism(currentUserOrganismPreference.organism)
                    currentUserOrganismPreference.sequence = sequence
                    currentUserOrganismPreference.save()
                }
                appStateObject.put("currentSequence", currentUserOrganismPreference.sequence)


                if (currentUserOrganismPreference.startbp && currentUserOrganismPreference.endbp) {
                    appStateObject.put("currentStartBp", currentUserOrganismPreference.startbp)
                    appStateObject.put("currentEndBp", currentUserOrganismPreference.endbp)
                }
            }
        }
        catch(PermissionException e) {
            def error=[error: "Error: "+e]
            log.error(error.error)
            return error
        }



        return appStateObject
    }
}
