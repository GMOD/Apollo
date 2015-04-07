package org.bbop.apollo

import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum

import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.parser.JSONParser

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class OrganismController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def sequenceService
    def permissionService

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Organism.list(params), model: [organismInstanceCount: Organism.count()]
    }

    def list(Integer max) {
        forward action: "index"
    }

    def featureCountForOrganism(Organism organism) {

    }

    def show(Organism organismInstance) {
        respond organismInstance
    }

    def create() {
        respond new Organism(params)
    }

    @Transactional
    def deleteOrganism() {
        println "saving params: ${params.data}"
        def organismJson = JSON.parse(params.data.toString()) as JSONObject
        println "organismJSON ${organismJson}"
        println "id: ${organismJson.id}"
        Organism organism = Organism.findById(organismJson.id as Long)
        if (organism) {
            organism.delete()
        }
        render findAllOrganisms()
    }

    @Transactional
    def saveOrganism() {
        println "saving params: ${params.data}"
        def organismJson = JSON.parse(params.data.toString()) as JSONObject
        println "organismJSON ${organismJson}"
        println "id: ${organismJson.id}"
        Organism organism = new Organism(
                commonName: organismJson.commonName
                , directory: organismJson.directory
                , species: organismJson.species
                , genus: organismJson.genus
        )
        println "organism ${organism as JSON}"

        checkOrganism(organism)
        try {
            organism.save(failOnError: true, flush: true, insert: true)
            sequenceService.loadRefSeqs(organism)
        } catch (e) {
            log.error("problem saving organism: " + e)
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
        }
        else if(!trackListFile.text.contains("WebApollo")){
            log.error("Track is not WebApollo enabled: " + trackListFile.absolutePath)
            organism.valid = false
        }
        else {
            organism.valid = true
        }
        return organism.valid
    }

    @Transactional
    def save(Organism organismInstance) {
        if (organismInstance == null) {
            notFound()
            return
        }

        if (organismInstance.hasErrors()) {
            respond organismInstance.errors, view: 'create'
            return
        }

        organismInstance.save flush: true, failOnError: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'organism.label', default: 'Organism'), organismInstance.id])
                redirect organismInstance
            }
            '*' { respond organismInstance, [status: CREATED] }
        }
    }

    def edit(Organism organismInstance) {
        respond organismInstance
    }

    @Transactional
    def updateOrganismInfo() {
        println "updating organism info ${params}"
        println "updating feature ${params.data}"
//                        {"data":{"id":"14", "name":"Honey Bee7", "directory":"/opt/another.apollo/jbrowse/data"}}
        def organismJson = JSON.parse(params.data.toString()) as JSONObject
        println "jsonObject ${organismJson}"
        Organism organism = Organism.findById(organismJson.id)
        println "found an organism ${organism}"
        if (organism) {
            organism.commonName = organismJson.name

            boolean directoryChanged = organism.directory != organismJson.directory || organismJson.forceReload
            println "directoryChanged ${directoryChanged}"
            try {
                if (directoryChanged) {
                    organism.directory = organismJson.directory
                }
                organism.save(flush: true, insert: false, failOnError: true)

                if (directoryChanged && checkOrganism(organism)) {
                    sequenceService.loadRefSeqs(organism)
                }
            } catch (e) {
                log.error("Problem updating organism info: ${e}")
            }
            render findAllOrganisms()
        } else {
            println "organism not found ${organismJson}"
            render { text: 'NOT updated' } as JSON
        }
    }

    @Transactional
    def changeOrganism(String id) {
        println "changing organism ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        String organismId = dataObject.organismId
        Session session = SecurityUtils.subject.getSession(false)
        println "organismId ${organismId}"
        Organism organism = Organism.findById(organismId as Long)
        if (organism) {
            println "found the organism ${organism}"
//            request.session.setAttribute("organismJBrowseDirectory", organism.directory)
            session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value,organism.directory)
//            session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value,sequence.name)
            session.setAttribute(FeatureStringEnum.ORGANISM_ID.value,organism.id)
        } else {
            println "no organism found"
        }

        println "updating organism"
        println params.data

        render organism as JSON
    }

    @Transactional
    def update(Organism organismInstance) {
        if (organismInstance == null) {
            notFound()
            return
        }

        if (organismInstance.hasErrors()) {
            respond organismInstance.errors, view: 'edit'
            return
        }

        organismInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Organism.label', default: 'Organism'), organismInstance.id])
                redirect organismInstance
            }
            '*' { respond organismInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Organism organismInstance) {

        if (organismInstance == null) {
            notFound()
            return
        }

        organismInstance.delete flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Organism.label', default: 'Organism'), organismInstance.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NO_CONTENT }
        }
    }

    def findAllOrganisms() {
       
        def organismList = permissionService.getOrganismsForCurrentUser()
        println "organism list: ${organismList}"

        println "finding all organisms: ${Organism.count}"

        JSONArray jsonArray = new JSONArray()
        for (def organism in organismList) {
            println organism
            Integer geneCount = Gene.executeQuery("select count(distinct g) from Gene g join g.featureLocations fl join fl.sequence s join s.organism o where o.id=:organismId", ["organismId": organism.id])[0]
            JSONObject jsonObject = [
                id: organism.id,
                commonName: organism.commonName,
                directory: organism.directory,
                annotationCount: geneCount,
                sequences: organism.sequences?.size(),
                genus: organism.genus,
                species: organism.species,
                valid: organism.valid,
                currentOrganism: request.session.getAttribute("organismJBrowseDirectory")==organism.directory
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
