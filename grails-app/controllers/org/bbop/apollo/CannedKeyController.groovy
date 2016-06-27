package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class CannedKeyController {

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
        respond CannedKey.list(params), model:[cannedKeyInstanceCount: CannedKey.count()]
    }

    def show(CannedKey cannedKeyInstance) {
        respond cannedKeyInstance
    }

    def create() {
        CannedKey cannedKey = new CannedKey(params)
        println "validated ${cannedKey.validate()} and errors: ${cannedKey.errors}"

        respond new CannedKey(params)
    }

    @Transactional
    def save(CannedKey cannedKeyInstance) {
        if (cannedKeyInstance == null) {
            notFound()
            return
        }

        if (cannedKeyInstance.hasErrors()) {
            println "has errors: ${cannedKeyInstance.errors}"
            respond cannedKeyInstance.errors, view:'create'
            return
        }

        cannedKeyInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'cannedKey.label', default: 'CannedKey'), cannedKeyInstance.id])
                redirect cannedKeyInstance
            }
            '*' { respond cannedKeyInstance, [status: CREATED] }
        }
    }

    def edit(CannedKey cannedKeyInstance) {
        respond cannedKeyInstance
    }

    @Transactional
    def update(CannedKey cannedKeyInstance) {
        if (cannedKeyInstance == null) {
            notFound()
            return
        }

        if (cannedKeyInstance.hasErrors()) {
            respond cannedKeyInstance.errors, view:'edit'
            return
        }

        cannedKeyInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'CannedKey.label', default: 'CannedKey'), cannedKeyInstance.id])
                redirect cannedKeyInstance
            }
            '*'{ respond cannedKeyInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(CannedKey cannedKeyInstance) {

        if (cannedKeyInstance == null) {
            notFound()
            return
        }

        cannedKeyInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'CannedKey.label', default: 'CannedKey'), cannedKeyInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'cannedKey.label', default: 'CannedKey'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
