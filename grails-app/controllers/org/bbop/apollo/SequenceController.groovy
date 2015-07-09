package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.DownloadFile
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


    // see #464
    private Map<String,DownloadFile> fileMap = new HashMap<>()

    def permissions() {  }


    @Transactional
    def setCurrentSequenceLocation() {

        try {
            Integer start, end
            start = params.startbp as Integer
            end = params.endbp as Integer

            UserOrganismPreference userOrganismPreference = preferenceService.setCurrentSequenceLocation(params.name, start, end)
            if(params.suppressOutput){
                render new JSONObject() as JSON
            }
            else{
                render userOrganismPreference.sequence as JSON
            }
        } catch (NumberFormatException e) {
            //  we can ignore this specific exception as null is an acceptable value for start / end
        }
        catch (Exception e) {
            def error=[error: e.message]
            log.error e.message
            render error as JSON
        }
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
        println "Loading sequences ${organism.commonName}"
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
                    , sequence: Sequence.findByOrganism(organism)
            ).save(insert: true, flush: true)
        }
        UserOrganismPreference.executeUpdate("update UserOrganismPreference  pref set pref.currentOrganism = false where pref.id != :prefId ", [prefId: userOrganismPreference.id])

        JSONArray sequenceArray = new JSONArray()
        for (Sequence sequence in organism.sequences) {
            JSONObject jsonObject = new JSONObject()
            jsonObject.put("id", sequence.id)
            jsonObject.put("name", sequence.name)
            jsonObject.put("length", sequence.length)
            jsonObject.put("start", sequence.start)
            jsonObject.put("end", sequence.end)
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
        String exportGff3Fasta = dataObject.exportGff3Fasta
        Organism organism = preferenceService.getCurrentOrganismForCurrentUser()

        def sequences = dataObject.sequences.name
        def sequenceList
        if (exportAllSequences == "true") {
            // HQL for all sequences
            sequenceList = Sequence.executeQuery("select distinct s from Sequence s join s.featureLocations fl where s.organism = :organism order by s.name asc ",[organism: organism])
        } else {
            // HQL for a single sequence or selected sequences
            sequenceList = Sequence.executeQuery("select distinct s from Sequence s join s.featureLocations fl where s.organism = :organism and s.name in (:sequenceNames) order by s.name asc ", [sequenceNames: sequences,organism: organism])
        }
        log.debug "# of sequences to export ${sequenceList.size()}"

        List<String> ontologyIdList = [Gene.class.name]
        List<String> alterationTypes = [Insertion.class.canonicalName, Deletion.class.canonicalName, Substitution.class.canonicalName]
        List<Feature> listOfFeatures = new ArrayList<>()
        List<Feature> listOfSequenceAlterations = new ArrayList<>()
        
        if(sequenceList){
            listOfFeatures.addAll(Feature.executeQuery("select distinct f from FeatureLocation fl join fl.sequence s join fl.feature f where s in (:sequenceList) and fl.feature.class in (:ontologyIdList) order by f.name asc", [sequenceList: sequenceList, ontologyIdList: ontologyIdList]))
        }
        else{
            log.warn "There are no annotations to be exported in this list of sequences ${sequences}"
        }
        File outputFile = File.createTempFile("Annotations", "." + typeOfExport.toLowerCase())

        if (typeOfExport == "GFF3") {
            // adding sequence alterations to list of features to export
            listOfSequenceAlterations = Feature.executeQuery("select f from Feature f join f.featureLocations fl join fl.sequence s where s in :sequenceList and f.class in :alterationTypes", [sequenceList: sequenceList, alterationTypes: alterationTypes])
            listOfFeatures.addAll(listOfSequenceAlterations)
            // call gff3HandlerService
            if (exportGff3Fasta == "true") {
                gff3HandlerService.writeFeaturesToText(outputFile.path, listOfFeatures, grailsApplication.config.apollo.gff3.source as String, true, sequenceList)
            } else {
                gff3HandlerService.writeFeaturesToText(outputFile.path, listOfFeatures, grailsApplication.config.apollo.gff3.source as String)
            }
        } else if (typeOfExport == "FASTA") {
            // call fastaHandlerService
            fastaHandlerService.writeFeatures(listOfFeatures, sequenceType, ["name"] as Set, outputFile.path, FastaHandlerService.Mode.WRITE, FastaHandlerService.Format.TEXT)
        }
        String uuidString = UUID.randomUUID().toString()
        DownloadFile downloadFile = new DownloadFile(
                uuid: uuidString
                ,path: outputFile.path
        )
        fileMap.put(uuidString,downloadFile)
        JSONObject jsonObject = new JSONObject()
        jsonObject.put("uuid", uuidString)
        jsonObject.put("exportType", typeOfExport)
        jsonObject.put("sequenceType", sequenceType)
        render jsonObject as JSON
    }

    def exportHandler() {
        log.debug "params to exportHandler: ${params}"
        String uuid = params.uuid
        DownloadFile downloadFile = fileMap.remove(uuid)
        def file = new File(downloadFile.path)
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
    def lookupSequenceByNameAndOrganism() {
        JSONObject j;
        for(k in params) {
            j=JSON.parse(k.key)
            break;
        }
        def organism
        if(!j.name||!j.organism) {
            organism = preferenceService.getCurrentOrganismForCurrentUser()
        }
        else {
            organism=Organism.findById(j.organism)
        }
        def seqid=j.name
        def sequences = Sequence.findAllByNameAndOrganism(seqid,organism)
        render sequences as JSON
    }

    @Transactional
    def getSequences(String name, Integer start, Integer length, String sort, Boolean asc, Integer minFeatureLength, Integer maxFeatureLength) {
        try {
            Organism organism = preferenceService.getCurrentOrganismForCurrentUser()
            minFeatureLength = minFeatureLength ?: 0
            maxFeatureLength = maxFeatureLength ?: Integer.MAX_VALUE
            List<Sequence> sequences
            def sequenceCount = Sequence.countByOrganismAndNameIlikeAndLengthGreaterThanEqualsAndLengthLessThanEquals(organism, "%${name}%", minFeatureLength, maxFeatureLength )
            sequences = Sequence.findAllByOrganismAndNameIlikeAndLengthGreaterThanEqualsAndLengthLessThanEquals(organism, "%${name}%", minFeatureLength, maxFeatureLength, [offset: start, max: length, sort: sort, order: asc ? "asc" : "desc"])
            JSONArray returnSequences = JSON.parse( (sequences as JSON).toString()) as JSONArray

            for(int i = 0 ; i < returnSequences.size() ; i++){
                returnSequences.getJSONObject(i).put("sequenceCount",sequenceCount)
            }

            render returnSequences as JSON
        }
        catch(PermissionException e) {
            def error=[error: "Error: "+e]
            render error as JSON
        }
    }



}
