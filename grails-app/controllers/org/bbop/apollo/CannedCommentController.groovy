package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class CannedCommentController {

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
        respond CannedComment.list(params), model:[cannedCommentInstanceCount: CannedComment.count()]
    }

    def show(CannedComment cannedCommentInstance) {
        respond cannedCommentInstance
    }

    def create() {
        respond new CannedComment(params)
    }

    @Transactional
    def save(CannedComment cannedCommentInstance) {
        if (cannedCommentInstance == null) {
            notFound()
            return
        }

        if (cannedCommentInstance.hasErrors()) {
            respond cannedCommentInstance.errors, view:'create'
            return
        }

        cannedCommentInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'cannedComment.label', default: 'CannedComment'), cannedCommentInstance.id])
                redirect cannedCommentInstance
            }
            '*' { respond cannedCommentInstance, [status: CREATED] }
        }
    }

    def edit(CannedComment cannedCommentInstance) {
        respond cannedCommentInstance
    }

    @Transactional
    def update(CannedComment cannedCommentInstance) {
        if (cannedCommentInstance == null) {
            notFound()
            return
        }

        if (cannedCommentInstance.hasErrors()) {
            respond cannedCommentInstance.errors, view:'edit'
            return
        }

        cannedCommentInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'CannedComment.label', default: 'CannedComment'), cannedCommentInstance.id])
                redirect cannedCommentInstance
            }
            '*'{ respond cannedCommentInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(CannedComment cannedCommentInstance) {

        if (cannedCommentInstance == null) {
            notFound()
            return
        }

        cannedCommentInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'CannedComment.label', default: 'CannedComment'), cannedCommentInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'cannedComment.label', default: 'CannedComment'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
