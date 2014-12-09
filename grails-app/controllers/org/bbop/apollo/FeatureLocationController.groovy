package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class FeatureLocationController {

    static navigationScope = 'gene'

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond FeatureLocation.list(params), model:[featureLocationInstanceCount: FeatureLocation.count()]
    }

    def show(FeatureLocation featureLocationInstance) {
        respond featureLocationInstance
    }

    def create() {
        respond new FeatureLocation(params)
    }

    @Transactional
    def save(FeatureLocation featureLocationInstance) {
        if (featureLocationInstance == null) {
            notFound()
            return
        }

        if (featureLocationInstance.hasErrors()) {
            respond featureLocationInstance.errors, view:'create'
            return
        }

        featureLocationInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'featureLocation.label', default: 'FeatureLocation'), featureLocationInstance.id])
                redirect featureLocationInstance
            }
            '*' { respond featureLocationInstance, [status: CREATED] }
        }
    }

    def edit(FeatureLocation featureLocationInstance) {
        respond featureLocationInstance
    }

    @Transactional
    def update(FeatureLocation featureLocationInstance) {
        if (featureLocationInstance == null) {
            notFound()
            return
        }

        if (featureLocationInstance.hasErrors()) {
            respond featureLocationInstance.errors, view:'edit'
            return
        }

        featureLocationInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'FeatureLocation.label', default: 'FeatureLocation'), featureLocationInstance.id])
                redirect featureLocationInstance
            }
            '*'{ respond featureLocationInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(FeatureLocation featureLocationInstance) {

        if (featureLocationInstance == null) {
            notFound()
            return
        }

        featureLocationInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'FeatureLocation.label', default: 'FeatureLocation'), featureLocationInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'featureLocation.label', default: 'FeatureLocation'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
