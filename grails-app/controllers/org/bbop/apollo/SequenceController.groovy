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
    
    def permissions(){

    }

    def index3(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Sequence.list(params), model: [sequenceInstanceCount: Sequence.count(),username:'demo@demo.gov',isAdmin:'true']
    }

    def index2(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Sequence.list(params), model: [sequenceInstanceCount: Sequence.count(),username:'demo@demo.gov',isAdmin:'true']
    }
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Sequence.list(params), model: [sequenceInstanceCount: Sequence.count(),username:'demo@demo.gov',isAdmin:'true']
    }

    def websocketTest(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Sequence.list(params), model: [sequenceInstanceCount: Sequence.count()]
    }
//
    def show(Sequence sequenceInstance) {
//        respond sequenceInstance
        if (sequenceInstance == null) {
            notFound()
            return
        }
        respond sequenceInstance, model: [featureLocations: FeatureLocation.findAllBySequence(sequenceInstance)]
    }

    def create() {
        respond new Sequence(params)
    }
//
//    def retrieveSequences(Organism organism){
//    }

    def setDefaultSequence(Long id,String sequenceName){
        println "setting default sequences: ${params}"
        Session session = SecurityUtils.subject.getSession(false)
        Sequence sequence = Sequence.findByName(sequenceName)
        Organism organism = Organism.findById(id)
        if(!sequence){
            if(organism){
                sequence = organism.sequences.iterator().next()
            }
            else{
                log.error "default sequence not found ${sequenceName}"
                return
            }
        }
//        Organism organism = sequence.organism
//        HttpSession session = request.session
        session.setAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value,sequence.name)
        session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value,sequence.name)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value,organism.directory)
        session.setAttribute(FeatureStringEnum.ORGANISM_ID.value,sequence.organismId)
        render sequenceName as String
    }

    @Transactional
    def loadSequences(Organism organism) {
        log.info "loading sequences for organism ${organism}"
        if(!organism.sequences){
            sequenceService.loadRefSeqs(organism)
        }

        String defaultName = request.session.getAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value)
        log.info "loading default sequence from session: ${defaultName}"
        JSONArray sequenceArray = new JSONArray()
        for(Sequence sequence in organism.sequences){
            JSONObject jsonObject = new JSONObject()
            jsonObject.put("id",sequence.id)
            jsonObject.put("name",sequence.name)
            jsonObject.put("length",sequence.length)
            jsonObject.put("start",sequence.start)
            jsonObject.put("end",sequence.end)
            jsonObject.put("default",defaultName && defaultName==sequence.name)
            if(defaultName==sequence.name){
                log.info "setting the default sequence: ${jsonObject.get("default")}"
            }
            sequenceArray.put(jsonObject)
        }

        render sequenceArray as JSON
    }

    @Transactional
    def save(Sequence sequenceInstance) {
        if (sequenceInstance == null) {
            notFound()
            return
        }

        if (sequenceInstance.hasErrors()) {
            respond sequenceInstance.errors, view: 'create'
            return
        }

        sequenceInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'sequence.label', default: 'Sequence'), sequenceInstance.id])
                redirect sequenceInstance
            }
            '*' { respond sequenceInstance, [status: CREATED] }
        }
    }

    def edit(Sequence sequenceInstance) {
        respond sequenceInstance
    }

    @Transactional
    def update(Sequence sequenceInstance) {
        if (sequenceInstance == null) {
            notFound()
            return
        }

        if (sequenceInstance.hasErrors()) {
            respond sequenceInstance.errors, view: 'edit'
            return
        }

        sequenceInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Track.label', default: 'Sequence'), sequenceInstance.id])
                redirect sequenceInstance
            }
            '*' { respond sequenceInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Sequence sequenceInstance) {

        if (sequenceInstance == null) {
            notFound()
            return
        }

        sequenceInstance.delete flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Track.label', default: 'Sequence'), sequenceInstance.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NO_CONTENT }
        }
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
        println "export sequences ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        String typeOfExport = dataObject.type
        Collection<Feature> listOfFeatures = new ArrayList<Feature>();
        
        println "==> TypeofExport: ${typeOfExport}"
        println "===> dataobjectSequence: ${dataObject.sequences.name}"
        for(String sequence : dataObject.sequences.name) {
            println "${Sequence.findByName(sequence)}"
            for(Sequence eachSeq in Sequence.findByName(sequence)) {
                // for each sequence in the params
                println "===> eachSeQ: ${eachSeq.name}"
                List<FeatureLocation> testList = sequenceService.getFeatureLocations(eachSeq)
                println "===> SIZE OF FEATURE LIST: ${testList.size()}"
                if(testList.size() == 0) {
                    println "No features on sequence ${sequence}"
                    continue
                }
                JSONObject requestObject = new JSONObject()
                // create a request object

                requestObject.put("track", "Annotations-" + eachSeq.name)

                JSONArray featuresObjectArray = new JSONArray()
                for (FeatureLocation entity in testList) {
                    if(entity.feature.class.cvTerm == MRNA.cvTerm) {
                        // getting only MRNA features might want to extend to other features in future
                        Feature featureToWrite = Feature.findByUniqueName(entity.feature.uniqueName)
                        listOfFeatures.add(featureToWrite)
                    }
                }
                // outputFile = File.createTempFile("Annotations-" + eachSeq.name, ".gff3")
//                if (typeOfExport == "GFF3") {
//                    
//                    requestObject.put("operation", "get_gff3")
//                    sequenceService.getGff3ForFeature(requestObject, outputFile) // fetching GFF3 for each chromosome
//                    pathToOutputFile = Paths.get(outputFile.getPath())
//                    println "The output is located at ${pathToOutputFile}"
//                }
//                else if(typeOfExport == "FASTA") {
////                    outputFile = File.createTempFile("Annotations", ".fa")
//                    requestObject.put("operation", "get_sequence")
//                    requestObject.put(FeatureStringEnum.TYPE.value, FeatureStringEnum.TYPE_GENOMIC.value)
////                    sequenceService.getSequenceForFeature(requestObject, outputFile) // fetching FASTA for each chromosome
//
//                    pathToOutputFile = Paths.get(outputFile.getPath())
//                    println "The output is located at ${pathToOutputFile}"
//                }
            }
    
        }

        File outputFile = File.createTempFile("Annotations", "." + typeOfExport.toLowerCase())
        if(typeOfExport == 'GFF3') {
            // call gff3HandlerService
            println "CHECKFILE : ${outputFile.path}"
            gff3HandlerService.writeFeaturesToText(outputFile.path, listOfFeatures, grailsApplication.config.apollo.gff3.source as String)
        }
        else if(typeOfExport == 'FASTA') {
            // call fastaHandlerService
            // currently handles genomic. Must handle all types depending on user's input
            fastaHandlerService.writeFeatures( listOfFeatures, 'genomic', ['name','date_creation'] as Set, outputFile.path, FastaHandlerService.Mode.WRITE, FastaHandlerService.Format.TEXT)
            
        }
        JSONObject jsonObject = new JSONObject()
        jsonObject.put("filePath",outputFile.path)
        jsonObject.put("exportType",typeOfExport)
        render jsonObject as JSON
    }
    
    def exportHandler() {
        println "PARAMS: ${params}"
        String pathToFile = params.filePath
        def file = new File(pathToFile)
        response.contentType = 'txt'
        if(params.exportType == 'GFF3') {
            response.setHeader("Content-disposition", "attachment; filename=Annotations.gff3")
        }
        else if(params.exportType == 'FASTA') {
            response.setHeader("Content-disposition", "attachment; filename=Annotations.fasta")
        }
        def outputStream = response.outputStream
        outputStream << file.text
        outputStream.flush()
        outputStream.close()
        file.delete()
        
    }
}
