package org.bbop.apollo.go



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class GoAnnotationController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def goAnnotationService

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond GoAnnotation.list(params), [status: OK]
    }

    @Transactional
    def save(GoAnnotation goAnnotationInstance) {
        if (goAnnotationInstance == null) {
            render status: NOT_FOUND
            return
        }

        goAnnotationInstance.validate()
        if (goAnnotationInstance.hasErrors()) {
            render status: NOT_ACCEPTABLE
            return
        }

        goAnnotationInstance.save flush:true
        respond goAnnotationInstance, [status: CREATED]
    }

    @Transactional
    def update(GoAnnotation goAnnotationInstance) {
        if (goAnnotationInstance == null) {
            render status: NOT_FOUND
            return
        }

        goAnnotationInstance.validate()
        if (goAnnotationInstance.hasErrors()) {
            render status: NOT_ACCEPTABLE
            return
        }

        goAnnotationInstance.save flush:true
        respond goAnnotationInstance, [status: OK]
    }

    @Transactional
    def delete(GoAnnotation goAnnotationInstance) {

        if (goAnnotationInstance == null) {
            render status: NOT_FOUND
            return
        }

        goAnnotationInstance.delete flush:true
        render status: NO_CONTENT
    }
}
