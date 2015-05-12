package org.bbop.apollo

import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.http.HttpStatus

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class OrganismController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def sequenceService
    def permissionService
    def requestHandlingService


    @Transactional
    def deleteOrganism() {
        log.debug "DELETING ORGANISM params: ${params.data}"
        def organismJson = JSON.parse(params.data.toString()) as JSONObject
        try {
            permissionService.checkPermissions(organismJson, PermissionEnum.ADMINISTRATE)

            log.debug "organismJSON ${organismJson}"
            log.debug "id: ${organismJson.id}"
            Organism organism = Organism.findById(organismJson.id as Long)
            if (organism) {
                UserOrganismPreference.deleteAll(UserOrganismPreference.findAllByOrganism(organism))
                organism.delete()
            }
        }
        catch(Exception e) {
            def error= [error: 'problem deleting organism: '+e]
            render error as JSON
            log.error(error.error)
        }
        render findAllOrganisms()
    }

    /**
     * web services exposed method
     * TODO: merge with saveOrganism into a service method
     * @return
     */
    @Transactional
    def addOrganism(){
        JSONObject inputObject = request.JSON
//        println "response.JSON ${request.JSON}"
        if (permissionService.hasPermissions(inputObject, PermissionEnum.ADMINISTRATE)) {
            Organism organism = new Organism(
                    commonName: inputObject.name
                    ,directory: inputObject.directory
                    ,blatdb: inputObject.blatdb
                    ,genus: inputObject.genus ?: ""
                    ,species: inputObject.species ?: ""
            ).save()
            if(checkOrganism(organism)) {
                organism.save(failOnError: true, flush: true, insert: true)
            }
            sequenceService.loadRefSeqs(organism)

        } else {
            render status: HttpStatus.UNAUTHORIZED
        }
        render new JSONObject() as JSON
    }

    @Transactional
    def saveOrganism() {
        log.debug "saving params: ${params.data}"
        def organismJson = JSON.parse(params.data.toString()) as JSONObject
        log.debug "organismJSON ${organismJson}"
        log.debug "id: ${organismJson.id}"
        try {
            permissionService.checkPermissions(PermissionEnum.ADMINISTRATE)
            if(organismJson.get("commonName")==""||organismJson.get("directory")=="") {
                throw new Exception('empty fields detected')
            }

            Organism organism = new Organism(
                    commonName: organismJson.commonName
                    , directory: organismJson.directory
                    , blatdb: organismJson.blatdb
                    , species: organismJson.species
                    , genus: organismJson.genus
            )
            log.debug "organism ${organism as JSON}"

            if(checkOrganism(organism)) {
                organism.save(failOnError: true, flush: true, insert: true)
            }
            sequenceService.loadRefSeqs(organism)
        } catch (e) {
            def error= [error: 'problem saving organism: '+e]
            render error as JSON
            log.error(error.error)
            return
        }
        render findAllOrganisms()
    }

    private boolean checkOrganism(Organism organism) {
        File directory = new File(organism.directory)
        File trackListFile = new File(organism.getTrackList())
        File refSeqFile = new File(organism.getRefseqFile())

        if (!directory.exists() || !directory.isDirectory()) {
            organism.valid = false
            throw new Exception("Invalid directory specified: " + directory.absolutePath)
        } else if (!trackListFile.exists()) {
            organism.valid = false
            throw new Exception("Track file does not exists: " + trackListFile.absolutePath)
        } else if (!refSeqFile.exists()) {
            organism.valid = false
            throw new Exception("Reference sequence file does not exists: " + refSeqFile.absolutePath)
        } else if (!trackListFile.text.contains("WebApollo")) {
            organism.valid = false
            throw new Exception("The WA plugin is not enabled: " + trackListFile.absolutePath)
        } else {
            organism.valid = true
        }
        return organism.valid
    }


    @Transactional
    def updateOrganismInfo() {
        log.debug "updating organism info ${params}"
        def organismJson = JSON.parse(params.data.toString()) as JSONObject
        try {
            permissionService.checkPermissions(organismJson, PermissionEnum.ADMINISTRATE)
            Organism organism = Organism.findById(organismJson.id)
            if (organism) {
                organism.commonName = organismJson.name
                organism.blatdb = organismJson.blatdb
                organism.species = organismJson.species
                organism.genus = organismJson.genus

                boolean directoryChanged = organism.directory != organismJson.directory || organismJson.forceReload
                log.debug "directoryChanged ${directoryChanged}"
                if (directoryChanged && checkOrganism(organism)) {
                    organism.directory = organismJson.directory
                    organism.save(flush: true, insert: false, failOnError: true)
                    sequenceService.loadRefSeqs(organism)
                }
            } else {
                throw new Exception('organism not found')
            }
            render findAllOrganisms()
        }
        catch (e) {
            def error= [error: 'problem saving organism: '+e]
            render error as JSON
            log.error(error.error)
        }
    }

    def findAllOrganisms() {

        def organismList = permissionService.getOrganismsForCurrentUser()
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(permissionService.currentUser, true)
        Long defaultOrganismId = userOrganismPreference ? userOrganismPreference.organism.id : null

        JSONArray jsonArray = new JSONArray()
        for (def organism in organismList) {
            Integer annotationCount = Feature.executeQuery("select count(distinct f) from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o = :organism and f.class in (:viewableTypes)", [organism: organism, viewableTypes: requestHandlingService.viewableAnnotationList])[0] as Integer
            Integer sequenceCount = Sequence.countByOrganism(organism)
            JSONObject jsonObject = [
                    id             : organism.id,
                    commonName     : organism.commonName,
                    blatdb         : organism.blatdb,
                    directory      : organism.directory,
                    annotationCount: annotationCount,
                    sequences      : sequenceCount,
                    genus          : organism.genus,
                    species        : organism.species,
                    valid          : organism.valid,
                    currentOrganism: defaultOrganismId != null ? organism.id == defaultOrganismId : false
            ] as JSONObject
            jsonArray.add(jsonObject)
        }
        render jsonArray as JSON
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'organism.label', default: 'Organism'), params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND }
        }
    }

}
