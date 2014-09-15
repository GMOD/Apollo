package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class TrackController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Track.list(params), model:[trackInstanceCount: Track.count()]
    }

    def show(Track trackInstance) {
        respond trackInstance
    }

    def create() {
        respond new Track(params)
    }

    @Transactional
    def save(Track trackInstance) {
        if (trackInstance == null) {
            notFound()
            return
        }

        if (trackInstance.hasErrors()) {
            respond trackInstance.errors, view:'create'
            return
        }

        trackInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'track.label', default: 'Track'), trackInstance.id])
                redirect trackInstance
            }
            '*' { respond trackInstance, [status: CREATED] }
        }
    }

    def edit(Track trackInstance) {
        respond trackInstance
    }

    @Transactional
    def update(Track trackInstance) {
        if (trackInstance == null) {
            notFound()
            return
        }

        if (trackInstance.hasErrors()) {
            respond trackInstance.errors, view:'edit'
            return
        }

        trackInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Track.label', default: 'Track'), trackInstance.id])
                redirect trackInstance
            }
            '*'{ respond trackInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Track trackInstance) {

        if (trackInstance == null) {
            notFound()
            return
        }

        trackInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Track.label', default: 'Track'), trackInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'track.label', default: 'Track'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
