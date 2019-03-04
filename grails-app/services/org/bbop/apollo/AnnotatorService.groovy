package org.bbop.apollo

import grails.converters.JSON
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
    def configWrapperService

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
                Feature.executeQuery("select o,count(distinct f) from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o in (:organismList) and f.class in (:viewableTypes) group by o", [organismList: organismList, viewableTypes: requestHandlingService.viewableAnnotationList]).each {
                    annotationCountMap.put(it[0], it[1])
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
                        editable                  : organismBooleanMap.get(organism) ?: false

                ] as JSONObject
                organismArray.add(jsonObject)
            }
            appStateObject.put("organismList", organismArray)
            UserOrganismPreferenceDTO currentUserOrganismPreferenceDTO = preferenceService.getCurrentOrganismPreference(permissionService.currentUser, null, token)
            if (currentUserOrganismPreferenceDTO) {
                OrganismDTO currentOrganism = currentUserOrganismPreferenceDTO?.organism
                appStateObject.put("currentOrganism", currentOrganism)


                if (!currentUserOrganismPreferenceDTO.sequence) {
                    Organism organism = Organism.findById(currentOrganism.id)
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
        }
        catch (PermissionException e) {
            def error = [error: "Error: " + e]
            log.error(error.error)
            return error
        }



        return appStateObject
    }


    String getCommonDataDirectory() {
        ApplicationPreference commonDataPreference = ApplicationPreference.findByName("common_data_directory")
        return commonDataPreference.directory
    }

    /**
     *
     * 1. Determine the preferred version
     *    1a. The database one has the source of user, config, or ??  If it is user, then the database is always the default.
     *    1b.
     * 2. See if that version is valid (exists and can write)
     *    2a. Use the next backup (config)
     *    2b. Use the next backup (find a home writeable directory)
     *    2c. Ask the user for help
     * 3. Notify the admin user the first time of where that directory is.
     *
     * The reason for using the database is to remove the configuration detail if startup is easier.
     *
     * If both exist and they match and they are both writeable, then return
     *
     *
     * @return
     */
    def checkCommonDataDirectory() {
        ApplicationPreference commonDataPreference = ApplicationPreference.findByName("common_data_directory")
        String directory

        if (commonDataPreference) {
            directory = commonDataPreference.value
            println "Can write to ${directory}"

            File testDirectory = new File(directory)
            if (!testDirectory.exists()) {
                assert testDirectory.createNewFile()
            }
            if (testDirectory.exists() && testDirectory.canWrite()) {
                return null
            }
        }

        // if all of the tests fail, then do the next thing
        log.warn "Unable to write to the database directory, so checking the config"
        directory = configWrapperService.commonDataDirectory
        File testDirectory = new File(directory)
        if (!testDirectory.exists()) {
            assert testDirectory.createNewFile()
        }
        if (testDirectory.exists() && testDirectory.canWrite()) {
            ApplicationPreference applicationPreference = new ApplicationPreference(
                    name: "common_data_directory",
                    value: directory

            ).save(failOnError: true, flush: true)
            println("Saving new preference for common data directory ${directory}")
            return null
        }

        return "Unable to write to directory ${directory}."
    }


    def updateCommonDataDirectory(String newDirectory) {
        File testDirectory = new File(newDirectory)
        if (!testDirectory.exists()) {
            assert testDirectory.createNewFile()
        }
        if (!testDirectory.exists() || !testDirectory.canWrite()) {
            return "Unable to write to directory ${newDirectory}"
        }
        ApplicationPreference commonDataPreference = ApplicationPreference.findOrSaveByName("common_data_directory")
        commonDataPreference.value = newDirectory
        commonDataPreference.save()
        return null
    }


}
