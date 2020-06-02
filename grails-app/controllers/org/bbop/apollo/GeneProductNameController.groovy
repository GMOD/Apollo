package org.bbop.apollo



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class GeneProductNameController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
//        params.max = Math.min(max ?: 10, 100)
//        respond GeneProductName.list(params), model:[geneProductNameInstanceCount: GeneProductName.count()]
        params.max = Math.min(max ?: 10, 100)
        def geneProductNames = GeneProductName.list(params)
        def organismFilterMap = [:]
        GeneProductNameOrganismFilter.findAllByGeneProductNameInList(geneProductNames).each() {
            List filterList = organismFilterMap.containsKey(it.geneProductName) ? organismFilterMap.get(it.geneProductName) : []
            filterList.add(it)
            organismFilterMap[it.geneProductName] = filterList
        }
        respond geneProductNames, model: [geneProductNameInstanceCount: GeneProductName.count(), organismFilters: organismFilterMap]
    }

    def show(GeneProductName geneProductNameInstance) {
        respond geneProductNameInstance, model: [organismFilters: GeneProductNameOrganismFilter.findAllByGeneProductName(geneProductNameInstance)]
    }

    def create() {
        respond new GeneProductName(params)
    }

    @Transactional
    def save(GeneProductName geneProductNameInstance) {
        if (geneProductNameInstance == null) {
            notFound()
            return
        }

        if (geneProductNameInstance.hasErrors()) {
            respond geneProductNameInstance.errors, view:'create'
            return
        }

        geneProductNameInstance.save()

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new GeneProductNameOrganismFilter(
                organism: organism,
                geneProductName: geneProductNameInstance
            ).save()
        }

        geneProductNameInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'geneProductName.label', default: 'GeneProductName'), geneProductNameInstance.id])
                redirect geneProductNameInstance
            }
            '*' { respond geneProductNameInstance, [status: CREATED] }
        }
    }

    def edit(GeneProductName geneProductNameInstance) {
        respond geneProductNameInstance, model: [organismFilters: GeneProductNameOrganismFilter.findAllByGeneProductName(geneProductNameInstance)]
    }

    @Transactional
    def update(GeneProductName geneProductNameInstance) {
        if (geneProductNameInstance == null) {
            notFound()
            return
        }

        if (geneProductNameInstance.hasErrors()) {
            respond geneProductNameInstance.errors, view:'edit'
            return
        }

        geneProductNameInstance.save()

        GeneProductNameOrganismFilter.deleteAll(GeneProductNameOrganismFilter.findAllByGeneProductName(geneProductNameInstance))

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new GeneProductNameOrganismFilter(
                organism: organism,
                geneProductName: geneProductNameInstance
            ).save()
        }

        geneProductNameInstance.save(flush: true)


        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'GeneProductName.label', default: 'GeneProductName'), geneProductNameInstance.id])
                redirect geneProductNameInstance
            }
            '*'{ respond geneProductNameInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(GeneProductName geneProductNameInstance) {

        if (geneProductNameInstance == null) {
            notFound()
            return
        }

        geneProductNameInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'GeneProductName.label', default: 'GeneProductName'), geneProductNameInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'geneProductName.label', default: 'GeneProductName'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
