package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class AssemblageController {

//    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def permissionService
    def preferenceService
    def projectionService
    def assemblageService

    @Transactional
    def list() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        User user = permissionService.getCurrentUser(inputObject)
        if (Organism.count > 0) {
            Organism currentOrganism = preferenceService.getOrganismFromPreferences(user, null, inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            render assemblageService.getAssemblagesForUserAndOrganism(user, currentOrganism).sort() { a, b -> a.sequenceList <=> b.sequenceList } as JSON
        } else {
            render new JSONObject() as JSON
        }

    }

    def getAssemblage() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        User user = permissionService.currentUser
        Organism organism = preferenceService.getOrganismFromPreferences(user, inputObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value).toString(), inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))

        // creates a projection based on the Assemblages and caches them
        inputObject.organism = organism.commonName
        // this generates the projection
        projectionService.getProjection(inputObject)

        render inputObject as JSON
    }

    @Transactional
    def addAssemblage() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        Assemblage assemblage = assemblageService.convertJsonToAssemblage(inputObject) // this will save a new assemblage
        User user = permissionService.currentUser
        user.addToAssemblages(assemblage)
        user.save(flush: true)
        render list() as JSON
    }

    @Transactional
    def addAssemblageAndReturn() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        Assemblage assemblage = assemblageService.convertJsonToAssemblage(inputObject)
        render assemblageService.convertAssemblageToJson(assemblage) as JSON
    }

    @Transactional
    def deleteAssemblage() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        User user = permissionService.getCurrentUser(inputObject)

        inputObject.id.each{
            assemblageService.removeAssemblageById(it,user)
        }

        render list() as JSON
    }

    def searchAssemblage(String searchQuery) {
        JSONObject inputObject = permissionService.handleInput(request, params)
        User user = permissionService.getCurrentUser(inputObject);

        ArrayList<Assemblage> assemblages = new ArrayList<Assemblage>();
        for (Assemblage assemblage : user.assemblages) {
            if (assemblage.sequenceList.toLowerCase().contains(searchQuery)) {
                assemblages.add(assemblage);
            }
        }

        if (assemblages.size() > 0) {
            render assemblages.sort() { a, b -> a.sequenceList <=> b.sequenceList } as JSON
        } else {
            render new JSONObject() as JSON
        }
    }

}
