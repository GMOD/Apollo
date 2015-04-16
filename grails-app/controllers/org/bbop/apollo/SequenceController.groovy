package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import java.nio.file.Paths
import java.nio.file.Files

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

    def permissions() {

    }

    /**
     * ID is the organism ID
     * Sequence is teh default sequence name
     * @param id
     * @param sequenceName
     * @return
     */
    @Transactional
    def setDefaultSequence(Long id, String sequenceName) {
        log.debug "setting default sequences: ${params}"
        Session session = SecurityUtils.subject.getSession(false)
        Organism organism = Organism.findById(id)
        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName,organism)
        if (!sequence) {
            if (organism) {
                sequence = organism.sequences.iterator().next()
            } else {
                log.error "default sequence not found ${sequenceName}"
                return
            }
        }

        session.setAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value, sequence.name)
        session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value, sequence.name)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organism.directory)
        session.setAttribute(FeatureStringEnum.ORGANISM_ID.value, sequence.organismId)

        User currentUser = permissionService.currentUser

        UserOrganismPreference.executeUpdate("update UserOrganismPreference  pref set pref.currentOrganism = false ")
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(currentUser,organism)
        if(!userOrganismPreference){
            userOrganismPreference = new UserOrganismPreference(
                    user:currentUser
                    ,organism: organism
                    ,defaultSequence: sequence.name
                    ,currentOrganism: true
            ).save(insert:true)
        }
        else{
            userOrganismPreference.defaultSequence = sequence.name
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save()
        }



        render sequenceName as String
    }

    @Transactional
    def loadSequences(Organism organism) {
        log.info "loading sequences for organism ${organism}"
        if (!organism.sequences) {
            sequenceService.loadRefSeqs(organism)
        }

        User currentUser = permissionService.currentUser
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(currentUser,organism)
        String defaultName
        if(userOrganismPreference){
            defaultName = userOrganismPreference.defaultSequence
            request.session.setAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value,defaultName)
        }
        else{
            defaultName = request.session.getAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value)
            userOrganismPreference = new UserOrganismPreference(
                    user:currentUser
                    ,organism: organism
                    ,defaultSequence: defaultName
            ).save(insert:true)
        }

        log.info "loading default sequence from session: ${defaultName}"
        JSONArray sequenceArray = new JSONArray()
        for (Sequence sequence in organism.sequences) {
            JSONObject jsonObject = new JSONObject()
            jsonObject.put("id", sequence.id)
            jsonObject.put("name", sequence.name)
            jsonObject.put("length", sequence.length)
            jsonObject.put("start", sequence.start)
            jsonObject.put("end", sequence.end)
            jsonObject.put("default", defaultName && defaultName == sequence.name)
            if (defaultName == sequence.name) {
                log.info "setting the default sequence: ${jsonObject.get("default")}"
            }
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
        }
        else {
            // HQL for a single sequence or selected sequences
            sequenceList = Sequence.executeQuery("select distinct s from Sequence s join s.featureLocations fl where s.name in (:sequenceNames) order by s.name asc ", [sequenceNames: sequences])
        }
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
}
