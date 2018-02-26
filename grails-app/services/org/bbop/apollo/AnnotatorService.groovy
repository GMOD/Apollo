package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.preference.OrganismDTO
import org.bbop.apollo.preference.UserOrganismPreferenceDTO
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
            Map<Organism,Integer> annotationCountMap = [:]
            if(organismList){
                Sequence.executeQuery("select o,count(s) from Organism o join o.sequences s where o in (:organismList) group by o ",[organismList:organismList]).each(){
                    sequenceIntegerMap.put(it[0],it[1])
                }
                Feature.executeQuery("select o,count(distinct f) from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o in (:organismList) and f.class in (:viewableTypes) group by o", [organismList: organismList, viewableTypes: requestHandlingService.viewableAnnotationList]).each {
                    annotationCountMap.put(it[0],it[1])
                }
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
                        nonDefaultTranslationTable : organism.nonDefaultTranslationTable,
                        currentOrganism: defaultOrganismId != null ? organism.id == defaultOrganismId : false,
                        editable       : organismBooleanMap.get(organism) ?: false

                ] as JSONObject
                organismArray.add(jsonObject)
            }
            appStateObject.put("organismList", organismArray)
            UserOrganismPreferenceDTO currentUserOrganismPreferenceDTO = preferenceService.getCurrentOrganismPreference(permissionService.currentUser,null,token)
            if(currentUserOrganismPreferenceDTO){
                OrganismDTO currentOrganism = currentUserOrganismPreferenceDTO?.organism
                appStateObject.put("currentOrganism", currentOrganism )


                if (!currentUserOrganismPreferenceDTO.sequence) {
                    Organism organism = Organism.findById(currentOrganism.id)
                    Sequence sequence = Sequence.findByOrganism(organism,[sort:"name",order:"asc",max: 1])
                    // often the case when creating it
                    currentUserOrganismPreferenceDTO.sequence = preferenceService.getDTOFromSequence(sequence)
                }
                appStateObject.put("currentSequence", currentUserOrganismPreferenceDTO.sequence)


                if (currentUserOrganismPreferenceDTO.startbp && currentUserOrganismPreferenceDTO.endbp) {
                    appStateObject.put("currentStartBp", currentUserOrganismPreferenceDTO.startbp)
                    appStateObject.put("currentEndBp", currentUserOrganismPreferenceDTO.endbp)
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
