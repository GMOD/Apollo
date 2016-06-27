package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class CannedValueController {

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
        respond CannedValue.list(params), model:[cannedValueInstanceCount: CannedValue.count()]
    }

    def show(CannedValue cannedValueInstance) {
        respond cannedValueInstance
    }

    def create() {
        respond new CannedValue(params)
    }

    @Transactional
    def save(CannedValue cannedValueInstance) {
        if (cannedValueInstance == null) {
            notFound()
            return
        }

        if (cannedValueInstance.hasErrors()) {
            respond cannedValueInstance.errors, view:'create'
            return
        }

        cannedValueInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'cannedValue.label', default: 'CannedValue'), cannedValueInstance.id])
                redirect cannedValueInstance
            }
            '*' { respond cannedValueInstance, [status: CREATED] }
        }
    }

    def edit(CannedValue cannedValueInstance) {
        respond cannedValueInstance
    }

    @Transactional
    def update(CannedValue cannedValueInstance) {
        if (cannedValueInstance == null) {
            notFound()
            return
        }

        if (cannedValueInstance.hasErrors()) {
            respond cannedValueInstance.errors, view:'edit'
            return
        }

        cannedValueInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'CannedValue.label', default: 'CannedValue'), cannedValueInstance.id])
                redirect cannedValueInstance
            }
            '*'{ respond cannedValueInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(CannedValue cannedValueInstance) {

        if (cannedValueInstance == null) {
            notFound()
            return
        }

        cannedValueInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'CannedValue.label', default: 'CannedValue'), cannedValueInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'cannedValue.label', default: 'CannedValue'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
