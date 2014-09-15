package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class GenomeController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        if(Genome.count==1){
            redirect(action:"show",id:Genome.first().id)
        }
        respond Genome.list(params), model:[genomeInstanceCount: Genome.count()]
    }

    def show(Genome genomeInstance) {
        respond genomeInstance
    }

    def create() {
        respond new Genome(params)
    }

    @Transactional
    def save(Genome genomeInstance) {
        if (genomeInstance == null) {
            notFound()
            return
        }

        if (genomeInstance.hasErrors()) {
            respond genomeInstance.errors, view:'create'
            return
        }

        genomeInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'genome.label', default: 'Genome'), genomeInstance.id])
                redirect genomeInstance
            }
            '*' { respond genomeInstance, [status: CREATED] }
        }
    }

    def edit(Genome genomeInstance) {
        respond genomeInstance
    }

    @Transactional
    def update(Genome genomeInstance) {
        if (genomeInstance == null) {
            notFound()
            return
        }

        if (genomeInstance.hasErrors()) {
            respond genomeInstance.errors, view:'edit'
            return
        }

        genomeInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Genome.label', default: 'Genome'), genomeInstance.id])
                redirect genomeInstance
            }
            '*'{ respond genomeInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Genome genomeInstance) {

        if (genomeInstance == null) {
            notFound()
            return
        }

        genomeInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Genome.label', default: 'Genome'), genomeInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'genome.label', default: 'Genome'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
