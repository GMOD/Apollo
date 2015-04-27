package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.*

@Transactional(readOnly = true)
class SequenceController {


    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def sequenceService
    def featureService
    def transcriptService
    def fastaHandlerService
    def gff3HandlerService
    def permissionService
    def preferenceService

    def permissions() {

    }


    @Transactional
    def setCurrentSequenceLocation() {

//        render userOrganismPreference.sequence as JSON
        UserOrganismPreference userOrganismPreference = preferenceService.setCurrentSequenceLocation(params.name, params.startbp as Integer, params.endbp as Integer)

        render userOrganismPreference.sequence as JSON
    }

    /**
     * ID is the organism ID
     * Sequence is the default sequence name
     *
     * If no sequence name is set, pull the preferences, otherwise just choose a random one.
     * @param id
     * @param sequenceName
     * @return
     */
    @Transactional
    def setCurrentSequence(Sequence sequenceInstance) {
        log.debug "setting default sequences: ${params}"
        Organism organism = sequenceInstance.organism

        User currentUser = permissionService.currentUser
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(currentUser, organism)

        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: currentUser
                    , organism: organism
                    , sequence: sequenceInstance
                    , currentOrganism: true
            ).save(insert: true, flush: true, failOnError: true)
        } else {
            userOrganismPreference.sequence = sequenceInstance
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true, failOnError: true)
        }
        preferenceService.setOtherCurrentOrganismsFalse(userOrganismPreference, currentUser)

        Session session = SecurityUtils.subject.getSession(false)
        session.setAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value, sequenceInstance.name)
        session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value, sequenceInstance.name)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organism.directory)
        session.setAttribute(FeatureStringEnum.ORGANISM_ID.value, sequenceInstance.organismId)


        render userOrganismPreference.sequence.name as String
    }


    @Transactional
    def loadSequences(Organism organism) {
        println "LOADING SEQUENCES ${organism.commonName}"
        log.info "loading sequences for organism ${organism}"
        if (!organism.sequences) {
            sequenceService.loadRefSeqs(organism)
        }

        User currentUser = permissionService.currentUser
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(currentUser, organism)
        if (userOrganismPreference?.sequence?.name) {
            userOrganismPreference.currentOrganism = true
            request.session.setAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value, userOrganismPreference.sequence.name)
            userOrganismPreference.save(flush: true)
        } else {
            userOrganismPreference = new UserOrganismPreference(
                    user: currentUser
                    , organism: organism
                    , currentOrganism: true
                    , sequence: organism.sequences.iterator().next()
                    ,
            ).save(insert: true, flush: true)
        }
        UserOrganismPreference.executeUpdate("update UserOrganismPreference  pref set pref.currentOrganism = false where pref.id != :prefId ", [prefId: userOrganismPreference.id])

//        log.info "loading default sequence from session: ${defaultName}"
        JSONArray sequenceArray = new JSONArray()
        for (Sequence sequence in organism.sequences) {
            JSONObject jsonObject = new JSONObject()
            jsonObject.put("id", sequence.id)
            jsonObject.put("name", sequence.name)
            jsonObject.put("length", sequence.length)
            jsonObject.put("start", sequence.start)
            jsonObject.put("end", sequence.end)
//            jsonObject.put("default", defaultName && defaultName == sequence.name)
//            if (defaultName == sequence.name) {
//                log.info "setting the default sequence: ${jsonObject.get("default")}"
//            }
            sequenceArray.put(jsonObject)
        }

        render sequenceArray as JSON
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'sequence.label', default: 'Sequence'), params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND }
        }
    }

    @Transactional
    def exportSequences() {
        log.debug "export sequences ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        String typeOfExport = dataObject.type
        String sequenceType = dataObject.sequenceType
        String exportAllSequences = dataObject.exportAllSequences

        def sequences = dataObject.sequences.name
        def sequenceList
        if (exportAllSequences == "true") {
            // HQL for all sequences
            sequenceList = Sequence.executeQuery("select distinct s from Sequence s join s.featureLocations fl order by s.name asc ")
        } else {
            // HQL for a single sequence or selected sequences
            sequenceList = Sequence.executeQuery("select distinct s from Sequence s join s.featureLocations fl where s.name in (:sequenceNames) order by s.name asc ", [sequenceNames: sequences])
        }
        println "::: sequenceList: ${sequenceList}"
        log.debug "# of sequences to export ${sequenceList.size()}"

        List<String> ontologyIdList = [Gene.class.name]
        def listOfFeatures = FeatureLocation.executeQuery("select distinct f from FeatureLocation fl join fl.sequence s join fl.feature f where s in (:sequenceList) and fl.feature.class in (:ontologyIdList) order by f.name asc", [sequenceList: sequenceList, ontologyIdList: ontologyIdList])
        File outputFile = File.createTempFile("Annotations", "." + typeOfExport.toLowerCase())

        if (typeOfExport == "GFF3") {
            // call gff3HandlerService
            gff3HandlerService.writeFeaturesToText(outputFile.path, listOfFeatures, grailsApplication.config.apollo.gff3.source as String)
        } else if (typeOfExport == "FASTA") {
            // call fastaHandlerService
            fastaHandlerService.writeFeatures(listOfFeatures, sequenceType, ["name"] as Set, outputFile.path, FastaHandlerService.Mode.WRITE, FastaHandlerService.Format.TEXT)
        }
        JSONObject jsonObject = new JSONObject()
        jsonObject.put("filePath", outputFile.path)
        jsonObject.put("exportType", typeOfExport)
        jsonObject.put("sequenceType", sequenceType)
        render jsonObject as JSON
    }

    def exportHandler() {
        log.debug "params to exportHandler: ${params}"
        String pathToFile = params.filePath
        def file = new File(pathToFile)
        response.contentType = "txt"
        if (params.exportType == "GFF3") {
            response.setHeader("Content-disposition", "attachment; filename=Annotations.gff3")
        } else if (params.exportType == "FASTA") {
            response.setHeader("Content-disposition", "attachment; filename=Annotations.fasta")
        }
        def outputStream = response.outputStream
        outputStream << file.text
        outputStream.flush()
        outputStream.close()
        file.delete()

    }

    def lookupSequenceByName(String q) {
        Organism organism = preferenceService.getCurrentOrganismForCurrentUser()
        def sequences = Sequence.findAllByNameIlikeAndOrganism(q + "%", organism, ["sort": "name", "order": "asc", "max": 20]).collect() {
            it.name
        }
        render sequences as JSON
    }

    def getSequences(String name,Integer start,Integer length,String sort,Boolean asc){
//    def getSequences() {
//        println "params ${params}"
        println "getting sequences ${name} ${start},${length},${sort},${asc}"

        Organism organism = preferenceService.getCurrentOrganismForCurrentUser()
        List<Sequence> sequences
        if(name){
            sequences = Sequence.findAllByOrganismAndNameIlike(organism,"%${name}%",[offset:start,max: length,sort:sort,order:asc ? "asc":"desc"])
        }
        else{
            sequences = Sequence.findAllByOrganism(organism,[offset:start,max: length,sort:sort,order:asc ? "asc":"desc"])
        }
        println "rurning size: ${sequences.size()}"
        render sequences as JSON
    }

}
