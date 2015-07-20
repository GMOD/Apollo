package org.bbop.apollo

import org.bbop.apollo.report.FeatureSummary

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class FeatureController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def reportService

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Feature.list(params), model:[featureInstanceCount: Feature.count()]
    }

    def show(Feature featureInstance) {
        respond featureInstance
    }

    /**
     * TODO: perOrganism summary
     * @param featureInstance
     * @return
     */
    def summary() {
        Map<Organism,FeatureSummary> featureSummaryListInstance = new TreeMap<>(new Comparator<Organism>() {
            @Override
            int compare(Organism o1, Organism o2) {
                return o1.commonName <=> o2.commonName
            }
        })

        // global version
        FeatureSummary featureSummaryInstance = reportService.generateAllFeatureSummary()


        Organism.listOrderByCommonName().each { organism ->
            FeatureSummary thisFeatureSummaryInstance = reportService.generateFeatureSummary(organism)
            featureSummaryListInstance.put(organism,thisFeatureSummaryInstance)
        }


        respond featureSummaryInstance, model: [featureSummaries:featureSummaryListInstance]
//        respond featureInstance
    }

    def organismSummary() {
//        respond []
    }

    def annotatorSummary() {
//        respond []
    }

    def systemInfo() {
//        respond []
    }

    def changes() {
//        respond []
    }

    def create() {
        respond new Feature(params)
    }

    @Transactional
    def save(Feature featureInstance) {
        if (featureInstance == null) {
            notFound()
            return
        }

        if (featureInstance.hasErrors()) {
            respond featureInstance.errors, view:'create'
            return
        }

        featureInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'feature.label', default: 'Feature'), featureInstance.id])
                redirect featureInstance
            }
            '*' { respond featureInstance, [status: CREATED] }
        }
    }

    def edit(Feature featureInstance) {
        respond featureInstance
    }

    @Transactional
    def update(Feature featureInstance) {
        if (featureInstance == null) {
            notFound()
            return
        }

        if (featureInstance.hasErrors()) {
            respond featureInstance.errors, view:'edit'
            return
        }

        featureInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Feature.label', default: 'Feature'), featureInstance.id])
                redirect featureInstance
            }
            '*'{ respond featureInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Feature featureInstance) {

        if (featureInstance == null) {
            notFound()
            return
        }

        featureInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Feature.label', default: 'Feature'), featureInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'feature.label', default: 'Feature'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
