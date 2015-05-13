package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class AvailableStatusController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def permissionService

    def beforeInterceptor = {
        if(!permissionService.isAdmin()){
            forward action: "notAuthorized" ,controller: "annotator"
            return
        }
    }

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond AvailableStatus.list(params), model:[availableStatusInstanceCount: AvailableStatus.count()]
    }

    def show(AvailableStatus availableStatusInstance) {
        respond availableStatusInstance
    }

    def create() {
        respond new AvailableStatus(params)
    }

    @Transactional
    def save(AvailableStatus availableStatusInstance) {
        if (availableStatusInstance == null) {
            notFound()
            return
        }

        if (availableStatusInstance.hasErrors()) {
            respond availableStatusInstance.errors, view:'create'
            return
        }

        availableStatusInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'availableStatus.label', default: 'AvailableStatus'), availableStatusInstance.id])
                redirect availableStatusInstance
            }
            '*' { respond availableStatusInstance, [status: CREATED] }
        }
    }

    def edit(AvailableStatus availableStatusInstance) {
        respond availableStatusInstance
    }

    @Transactional
    def update(AvailableStatus availableStatusInstance) {
        if (availableStatusInstance == null) {
            notFound()
            return
        }

        if (availableStatusInstance.hasErrors()) {
            respond availableStatusInstance.errors, view:'edit'
            return
        }

        availableStatusInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'AvailableStatus.label', default: 'AvailableStatus'), availableStatusInstance.id])
                redirect availableStatusInstance
            }
            '*'{ respond availableStatusInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(AvailableStatus availableStatusInstance) {

        if (availableStatusInstance == null) {
            notFound()
            return
        }

        availableStatusInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'AvailableStatus.label', default: 'AvailableStatus'), availableStatusInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'availableStatus.label', default: 'AvailableStatus'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
