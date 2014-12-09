package org.bbop.apollo


import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class FeatureController {

    static navigationScope = 'feature'

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Feature.list(params), model: [featureInstanceCount: Feature.count()]
    }

//    def show(Feature featureInstance) {
    def show(Feature featureInstance) {
        println "featureInstance: ${featureInstance}"

        if(featureInstance?.ontologyId==MRNA.ontologyId){
            println "mathed MRNA"
            redirect( action: "show", controller: "MRNA", id:featureInstance.id)
            return
        }

        respond featureInstance

    }

    def create() {
        respond new Feature(params)
    }

    @Transactional
    def save(Feature featureInstance) {
        if (featureInstance == null) {
            notFound()
            return
        }

        if (featureInstance.hasErrors()) {
            respond featureInstance.errors, view: 'create'
            return
        }

        featureInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'feature.label', default: 'Feature'), featureInstance.id])
                redirect featureInstance
            }
            '*' { respond featureInstance, [status: CREATED] }
        }
    }

    def edit(Feature featureInstance) {
        respond featureInstance
    }

    @Transactional
    def update(Feature featureInstance) {
        if (featureInstance == null) {
            notFound()
            return
        }

        if (featureInstance.hasErrors()) {
            respond featureInstance.errors, view: 'edit'
            return
        }

        featureInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Feature.label', default: 'Feature'), featureInstance.id])
                redirect featureInstance
            }
            '*' { respond featureInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Feature featureInstance) {

        if (featureInstance == null) {
            notFound()
            return
        }

        featureInstance.delete flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Feature.label', default: 'Feature'), featureInstance.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'feature.label', default: 'Feature'), params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND }
        }
    }
}
