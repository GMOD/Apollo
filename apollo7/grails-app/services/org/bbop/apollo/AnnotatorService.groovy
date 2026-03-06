package org.bbop.apollo


import grails.gorm.transactions.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.preference.OrganismDTO
import org.bbop.apollo.preference.UserOrganismPreferenceDTO
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

@Transactional
class AnnotatorService {

    def permissionService
    def preferenceService
    def requestHandlingService
    def configWrapperService
    def trackService
    def variantService

    Integer getAnnotationCount(Organism organism) {
        if(configWrapperService.getCountAnnotations()){
            def viewableTypes = requestHandlingService.viewableAnnotationList + requestHandlingService.viewableSequenceAlterationList
            def results = Feature.executeQuery(" select count(f) from Feature f join f.featureLocations fl join fl.sequence s join s.organism o where o = :organism and f.class in :viewableTypes ",[organism:organism,viewableTypes: viewableTypes])
            return results[0] as Integer
        }
        return 0
    }

    def getAppState(String token) {
        JSONObject appStateObject = new JSONObject()
        try {
            List<Organism> organismList = permissionService.getOrganismsForCurrentUser()
            UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(permissionService.currentUser, true, token, [max: 1, sort: "lastUpdated", order: "desc"])
            log.debug "found organism preference: ${userOrganismPreference} for token ${token}"
            Long defaultOrganismId = userOrganismPreference ? userOrganismPreference.organism.id : null

            Map<Organism, Boolean> organismBooleanMap = permissionService.userHasOrganismPermissions(PermissionEnum.ADMINISTRATE)
            Map<Sequence, Integer> sequenceIntegerMap = [:]
            Map<Organism, Integer> annotationCountMap = [:]
            if (organismList) {
                Sequence.executeQuery("select o,count(s) from Organism o join o.sequences s where o in (:organismList) group by o ", [organismList: organismList]).each() {
                    sequenceIntegerMap.put(it[0], it[1])
                }
                if (configWrapperService.getCountAnnotations()) {
                    Feature.executeQuery("select o,count(distinct f) from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o in (:organismList) and f.class in (:viewableTypes) group by o", [organismList: organismList, viewableTypes: requestHandlingService.viewableAnnotationList]).each {
                        annotationCountMap.put(it[0], it[1])
                    }
                } else {
                    for (o in organismList) {
                        annotationCountMap.put(o, 0)
                    }
                }
            }


        JSONArray organismArray = new JSONArray()
        for (Organism organism in organismList.findAll()) {
            Integer sequenceCount = sequenceIntegerMap.get(organism) ?: 0
            JSONObject jsonObject = [
                id                        : organism.id as Long,
                commonName                : organism.commonName,
                blatdb                    : organism.blatdb,
                directory                 : organism.directory,
                annotationCount           : annotationCountMap.get(organism) ?: 0,
                sequences                 : sequenceCount,
                genus                     : organism.genus,
                species                   : organism.species,
                valid                     : organism.valid,
                publicMode                : organism.publicMode,
                obsolete                  : organism.obsolete,
                nonDefaultTranslationTable: organism.nonDefaultTranslationTable,
                currentOrganism           : defaultOrganismId != null ? organism.id == defaultOrganismId : false,
                editable                  : organismBooleanMap.get(organism) ?: false,
                officialGeneSetTrack      : organism.officialGeneSetTrack

            ] as JSONObject
            organismArray.add(jsonObject)
        }
        appStateObject.put("organismList", organismArray)
        UserOrganismPreferenceDTO currentUserOrganismPreferenceDTO = preferenceService.getCurrentOrganismPreference(permissionService.currentUser, null, token)
        if (currentUserOrganismPreferenceDTO) {
            OrganismDTO currentOrganism = currentUserOrganismPreferenceDTO?.organism

            if (userOrganismPreference?.organism) {
                currentOrganism.annotationCount = getAnnotationCount(userOrganismPreference.organism)
                currentOrganism.variantEffectCount = variantService.getSequenceAlterationEffectsCountForOrgansim(userOrganismPreference.organism)
            }
            Organism organism = Organism.findById(currentOrganism.id)
            currentOrganism.officialGeneSetTrack = organism?.officialGeneSetTrack
            appStateObject.put("currentOrganism", currentOrganism)


            if (!currentUserOrganismPreferenceDTO.sequence) {
                Sequence sequence = Sequence.findByOrganism(organism, [sort: "name", order: "asc", max: 1])
                // often the case when creating it
                currentUserOrganismPreferenceDTO.sequence = preferenceService.getDTOFromSequence(sequence)
            }
            appStateObject.put("currentSequence", currentUserOrganismPreferenceDTO.sequence)


            if (currentUserOrganismPreferenceDTO.startbp && currentUserOrganismPreferenceDTO.endbp) {
                appStateObject.put("currentStartBp", currentUserOrganismPreferenceDTO.startbp)
                appStateObject.put("currentEndBp", currentUserOrganismPreferenceDTO.endbp)
            }
        }
        appStateObject.put(FeatureStringEnum.COMMON_DATA_DIRECTORY.value, trackService.commonDataDirectory)
    }

    catch (
    PermissionException e
    ) {
        def error = [error: "Error: " + e]
        log.error(error.error)
        return error
    }



    return appStateObject
}


}
