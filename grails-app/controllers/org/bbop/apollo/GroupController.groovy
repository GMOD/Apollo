package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class GroupController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond GroupAnnotation.list(params), model:[groupInstanceCount: GroupAnnotation.count()]
    }

    def show(GroupAnnotation groupInstance) {
        respond groupInstance
    }

    def create() {
        respond new GroupAnnotation(params)
    }

    @Transactional
    def save(GroupAnnotation groupInstance) {
        if (groupInstance == null) {
            notFound()
            return
        }

        if (groupInstance.hasErrors()) {
            respond groupInstance.errors, view:'create'
            return
        }

        groupInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'group.label', default: 'Group'), groupInstance.id])
                redirect groupInstance
            }
            '*' { respond groupInstance, [status: CREATED] }
        }
    }

    def edit(GroupAnnotation groupInstance) {
        respond groupInstance
    }

    @Transactional
    def update(GroupAnnotation groupInstance) {
        if (groupInstance == null) {
            notFound()
            return
        }

        if (groupInstance.hasErrors()) {
            respond groupInstance.errors, view:'edit'
            return
        }

        groupInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Group.label', default: 'Group'), groupInstance.id])
                redirect groupInstance
            }
            '*'{ respond groupInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(GroupAnnotation groupInstance) {

        if (groupInstance == null) {
            notFound()
            return
        }

        groupInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Group.label', default: 'Group'), groupInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'group.label', default: 'Group'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
