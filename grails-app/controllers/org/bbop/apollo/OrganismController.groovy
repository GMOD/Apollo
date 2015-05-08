package org.bbop.apollo

import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum

import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

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
        log.debug "organismJSON ${organismJson}"
        log.debug "id: ${organismJson.id}"
        Organism organism = Organism.findById(organismJson.id as Long)
        if (organism) {
            UserOrganismPreference.deleteAll(UserOrganismPreference.findAllByOrganism(organism))
            organism.delete()
        }
        render findAllOrganisms()
    }

    @Transactional
    def saveOrganism() {
        log.debug "saving params: ${params.data}"
        def organismJson = JSON.parse(params.data.toString()) as JSONObject
        log.debug "organismJSON ${organismJson}"
        log.debug "id: ${organismJson.id}"
        try {
            if(organismJson.get("commonName")==""||organismJson.get("directory")=="") {
                def error= [error: 'problem saving organism: empty fields detected']
                render error as JSON
                log.error(error.error)
                return
            }


            Organism organism = new Organism(
                    commonName: organismJson.commonName
                    , directory: organismJson.directory
                    , blatdb: organismJson.blatdb
                    , species: organismJson.species
                    , genus: organismJson.genus
            )
            log.debug "organism ${organism as JSON}"

            if(!checkOrganism(organism)) {
                def error= [error: 'problem saving organism: invalid data directory specified']
                render error as JSON
                log.error(error.error)
                return
            }
            organism.save(failOnError: true, flush: true, insert: true)
            sequenceService.loadRefSeqs(organism)
        } catch (e) {
            def error= [error: 'problem saving organism: invalid data directory specified']
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
            log.error("Is an invalid directory: " + directory.absolutePath)
            organism.valid = false
        } else if (!trackListFile.exists()) {
            log.error("Track file does not exists: " + trackListFile.absolutePath)
            organism.valid = false
        } else if (!refSeqFile.exists()) {
            log.error("Reference sequence file does not exists: " + refSeqFile.absolutePath)
            organism.valid = false
        } else if (!trackListFile.text.contains("WebApollo")) {
            log.error("Track is not WebApollo enabled: " + trackListFile.absolutePath)
            organism.valid = false
        } else {
            organism.valid = true
        }
        return organism.valid
    }


    @Transactional
    def updateOrganismInfo() {
        log.debug "updating organism info ${params}"
        def organismJson = JSON.parse(params.data.toString()) as JSONObject
        Organism organism = Organism.findById(organismJson.id)
        if (organism) {
            organism.commonName = organismJson.name
            organism.blatdb = organismJson.blatdb
            organism.species = organismJson.species
            organism.genus = organismJson.genus

            boolean directoryChanged = organism.directory != organismJson.directory || organismJson.forceReload
            log.debug "directoryChanged ${directoryChanged}"
            try {
                if (directoryChanged) {
                    organism.directory = organismJson.directory
                }

                if (directoryChanged && checkOrganism(organism)) {
                    organism.save(flush: true, insert: false, failOnError: true)
                    sequenceService.loadRefSeqs(organism)
                }
                else if(directoryChanged) {
                    def error=[error: 'problem saving organism: data directory not found. data directory not updated']
                    render error as JSON
                    log.error(error.error)
                }
            } catch (e) {
                def error= [error: 'problem saving organism: '+e]
                render error as JSON
                log.error(error.error)
            }
            render findAllOrganisms()
        } else {
            def error= [error: 'problem saving organism: organism not found']
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
