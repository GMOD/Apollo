package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class GroupAnnotationController {

    static navigationScope = 'feature'

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond GroupAnnotation.list(params), model:[groupAnnotationInstanceCount: GroupAnnotation.count()]
    }

    def show(GroupAnnotation groupAnnotationInstance) {
        respond groupAnnotationInstance
    }

    def create() {
        respond new GroupAnnotation(params)
    }

    @Transactional
    def save(GroupAnnotation groupAnnotationInstance) {
        if (groupAnnotationInstance == null) {
            notFound()
            return
        }

        if (groupAnnotationInstance.hasErrors()) {
            respond groupAnnotationInstance.errors, view:'create'
            return
        }

        groupAnnotationInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'group.label', default: 'Group'), groupAnnotationInstance.id])
                redirect groupAnnotationInstance
            }
            '*' { respond groupAnnotationInstance, [status: CREATED] }
        }
    }

    def edit(GroupAnnotation groupAnnotationInstance) {
        respond groupAnnotationInstance
    }

    @Transactional
    def update(GroupAnnotation groupAnnotationInstance) {
        if (groupAnnotationInstance == null) {
            notFound()
            return
        }

        if (groupAnnotationInstance.hasErrors()) {
            respond groupAnnotationInstance.errors, view:'edit'
            return
        }

        groupAnnotationInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Group.label', default: 'Group'), groupAnnotationInstance.id])
                redirect groupAnnotationInstance
            }
            '*'{ respond groupAnnotationInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(GroupAnnotation groupAnnotationInstance) {

        if (groupAnnotationInstance == null) {
            notFound()
            return
        }

        groupAnnotationInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Group.label', default: 'Group'), groupAnnotationInstance.id])
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
