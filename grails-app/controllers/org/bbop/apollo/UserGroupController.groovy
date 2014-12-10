package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class UserGroupController {

    static navigationScope = 'admin'

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]


    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond UserGroup.list(params), model:[userGroupInstanceCount: UserGroup.count()]
    }

    def show(UserGroup userGroupInstance) {
        respond userGroupInstance
    }

    def create() {
        respond new UserGroup(params)
    }

    @Transactional
    def save(UserGroup userGroupInstance) {
        if (userGroupInstance == null) {
            notFound()
            return
        }

        if (userGroupInstance.hasErrors()) {
            respond userGroupInstance.errors, view:'create'
            return
        }

        userGroupInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'userGroup.label', default: 'UserGroup'), userGroupInstance.id])
                redirect userGroupInstance
            }
            '*' { respond userGroupInstance, [status: CREATED] }
        }
    }

    def edit(UserGroup userGroupInstance) {
        respond userGroupInstance
    }

    @Transactional
    def update(UserGroup userGroupInstance) {
        if (userGroupInstance == null) {
            notFound()
            return
        }

        if (userGroupInstance.hasErrors()) {
            respond userGroupInstance.errors, view:'edit'
            return
        }

        userGroupInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'UserGroup.label', default: 'UserGroup'), userGroupInstance.id])
                redirect userGroupInstance
            }
            '*'{ respond userGroupInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(UserGroup userGroupInstance) {

        if (userGroupInstance == null) {
            notFound()
            return
        }

        userGroupInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'UserGroup.label', default: 'UserGroup'), userGroupInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'userGroup.label', default: 'UserGroup'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
