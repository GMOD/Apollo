package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import java.nio.file.Paths

import static org.springframework.http.HttpStatus.*

@Transactional(readOnly = true)
class SequenceController {


    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def sequenceService
    def featureService
    def transcriptService
    
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
    

    def exportSequences() {
        println "export sequences ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        String pathToOutputFile = ""
        
        for(String sequence : dataObject.sequences.name) {
            println "===>${sequence}"
            println "${Sequence.findByName(sequence)}"
            for(Sequence eachSeq in Sequence.findByName(sequence)) {
                // for each sequence in the params
                println "===> eachSeQ: ${eachSeq.name}"
                List<FeatureLocation> testList = sequenceService.getFeatureLocations(eachSeq)
                JSONObject requestObject = new JSONObject()
                // create a request object
                println "CREATED JSON : ${requestObject}"
                requestObject.put("operation", "get_gff3")
                requestObject.put("track", "Annotations-" + eachSeq.name)

                JSONArray featuresObjectArray = new JSONArray()
                for (FeatureLocation entity in testList) {
                    if(entity.feature.class.cvTerm == Gene.cvTerm) {
                        println "${entity.feature}"
                        JSONObject featureUniqueName = new JSONObject()
                        featureUniqueName.put('uniquename', entity.feature.uniqueName)
                        featuresObjectArray.add(featureUniqueName)
                        requestObject.put("features", featuresObjectArray)
                    }
                }
                File outputFile = File.createTempFile("Annotations-" + eachSeq.name, ".gff3")
                sequenceService.getGff3ForFeature(requestObject, outputFile) // fetching Gff3 for each chromosome
                pathToOutputFile = Paths.get(outputFile.getPath())
                println "The output is located at ${pathToOutputFile}"

            }

        }
        // TODO: call methods here and generate url
        // use sequenceServices to export the object and then generate a URL
        JSONObject jsonObject = new JSONObject()
        jsonObject.put("url",pathToOutputFile)
        render jsonObject as JSON
    }
}
