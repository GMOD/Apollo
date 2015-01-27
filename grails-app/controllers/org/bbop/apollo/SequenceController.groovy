package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class SequenceController {


    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def sequenceService

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

    def setDefaultSequence(String sequenceName){
        println "setting default sequences: ${params}"
        request.session.setAttribute("defaultSequenceName",sequenceName)
    }

    @Transactional
    def loadSequences(Organism organism) {
        println "loading sequences for organism ${organism}"
        if(!organism.sequences){
            sequenceService.loadRefSeqs(organism)
        }

        String defaultName = request.session.getAttribute("defaultSequenceName")
        println "defaultName retrieved? ${defaultName}"
        JSONArray sequenceArray = new JSONArray()
        for(Sequence sequence in organism.sequences){
            JSONObject jsonObject = new JSONObject()
            jsonObject.put("name",sequence.name)
            jsonObject.put("length",sequence.length)
            jsonObject.put("start",sequence.start)
            jsonObject.put("end",sequence.end)
            jsonObject.put("default",defaultName && defaultName==sequence.name)
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
}
