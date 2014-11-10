package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class GeneController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Gene.list(params), model:[geneInstanceCount: Gene.count()]
    }

    def show(Gene geneInstance) {
        respond geneInstance
    }

    def create() {
        respond new Gene(params)
    }

    @Transactional
    def save(Gene geneInstance) {
        if (geneInstance == null) {
            notFound()
            return
        }

        if (geneInstance.hasErrors()) {
            respond geneInstance.errors, view:'create'
            return
        }

        geneInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'gene.label', default: 'Gene'), geneInstance.id])
                redirect geneInstance
            }
            '*' { respond geneInstance, [status: CREATED] }
        }
    }

    def edit(Gene geneInstance) {
        respond geneInstance
    }

    @Transactional
    def update(Gene geneInstance) {
        if (geneInstance == null) {
            notFound()
            return
        }

        if (geneInstance.hasErrors()) {
            respond geneInstance.errors, view:'edit'
            return
        }

        geneInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Gene.label', default: 'Gene'), geneInstance.id])
                redirect geneInstance
            }
            '*'{ respond geneInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Gene geneInstance) {

        if (geneInstance == null) {
            notFound()
            return
        }

        geneInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Gene.label', default: 'Gene'), geneInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'gene.label', default: 'Gene'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
