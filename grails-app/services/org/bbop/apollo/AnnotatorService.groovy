package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class AnnotatorService {

    def permissionService
    def requestHandlingService

    def getAppState() {
        JSONObject appStateObject = new JSONObject()
        try {
            def organismList = permissionService.getOrganismsForCurrentUser()
            UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(permissionService.currentUser, true)
            Long defaultOrganismId = userOrganismPreference ? userOrganismPreference.organism.id : null


            JSONArray organismArray = new JSONArray()
            for (Organism organism in organismList) {
                Integer annotationCount = Feature.executeQuery("select count(distinct f) from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o = :organism and f.class in (:viewableTypes)", [organism: organism, viewableTypes: requestHandlingService.viewableAnnotationList])[0] as Integer
                Integer sequenceCount = Sequence.countByOrganism(organism)
                JSONObject jsonObject = [
                        id             : organism.id as Long,
                        commonName     : organism.commonName,
                        blatdb         : organism.blatdb,
                        directory      : organism.directory,
                        annotationCount: annotationCount,
                        sequences      : sequenceCount,
                        genus          : organism.genus,
                        species        : organism.species,
                        valid          : organism.valid,
                        publicMode     : organism.publicMode,
                        currentOrganism: defaultOrganismId != null ? organism.id == defaultOrganismId : false,
                        editable       : permissionService.userHasOrganismPermission(organism,PermissionEnum.ADMINISTRATE)

                ] as JSONObject
                organismArray.add(jsonObject)
            }
            appStateObject.put("organismList", organismArray)
            UserOrganismPreference currentUserOrganismPreference = permissionService.currentOrganismPreference
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
