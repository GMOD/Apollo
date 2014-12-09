package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class MRNAController {

    static navigationScope = 'feature'

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond MRNA.list(params), model:[MRNAInstanceCount: MRNA.count()]
    }

    def show(MRNA MRNAInstance) {
        respond MRNAInstance
    }

    def create() {
        respond new MRNA(params)
    }

    @Transactional
    def save(MRNA MRNAInstance) {
        if (MRNAInstance == null) {
            notFound()
            return
        }

        if (MRNAInstance.hasErrors()) {
            respond MRNAInstance.errors, view:'create'
            return
        }

        MRNAInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'MRNA.label', default: 'MRNA'), MRNAInstance.id])
                redirect MRNAInstance
            }
            '*' { respond MRNAInstance, [status: CREATED] }
        }
    }

    def edit(MRNA MRNAInstance) {
        respond MRNAInstance
    }

    @Transactional
    def update(MRNA MRNAInstance) {
        if (MRNAInstance == null) {
            notFound()
            return
        }

        if (MRNAInstance.hasErrors()) {
            respond MRNAInstance.errors, view:'edit'
            return
        }

        MRNAInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'MRNA.label', default: 'MRNA'), MRNAInstance.id])
                redirect MRNAInstance
            }
            '*'{ respond MRNAInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(MRNA MRNAInstance) {

        if (MRNAInstance == null) {
            notFound()
            return
        }

        MRNAInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'MRNA.label', default: 'MRNA'), MRNAInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'MRNA.label', default: 'MRNA'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
