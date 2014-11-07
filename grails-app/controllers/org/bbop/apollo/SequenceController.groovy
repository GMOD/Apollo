package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class SequenceController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def sequenceService
    def configWrapperService

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Sequence.list(params), model:[sequenceInstanceCount: Sequence.count()]
    }

    def show(Sequence sequenceInstance) {
        respond sequenceInstance
    }

    def create() {
        respond new Sequence(params)
    }

    def loadSequence() {
        sequenceService.loadRefSeqs(Organism.first(),configWrapperService.refSeqDirectory)
        respond new Sequence(params)
    }

    @Transactional
    def save(Sequence sequenceInstance) {
        if (sequenceInstance == null) {
            notFound()
            return
        }

        if (sequenceInstance.hasErrors()) {
            respond sequenceInstance.errors, view:'create'
            return
        }

        sequenceInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'sequence.label', default: 'Sequence'), sequenceInstance.id])
                redirect sequenceInstance
            }
            '*' { respond sequenceInstance, [status: CREATED] }
        }
    }

    def edit(Sequence sequenceInstance) {
        respond sequenceInstance
    }

    @Transactional
    def update(Sequence sequenceInstance) {
        if (sequenceInstance == null) {
            notFound()
            return
        }

        if (sequenceInstance.hasErrors()) {
            respond sequenceInstance.errors, view:'edit'
            return
        }

        sequenceInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Track.label', default: 'Sequence'), sequenceInstance.id])
                redirect sequenceInstance
            }
            '*'{ respond sequenceInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Sequence sequenceInstance) {

        if (sequenceInstance == null) {
            notFound()
            return
        }

        sequenceInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Track.label', default: 'Sequence'), sequenceInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'sequence.label', default: 'Sequence'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
