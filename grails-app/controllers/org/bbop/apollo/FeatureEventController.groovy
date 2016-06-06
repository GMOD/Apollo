package org.bbop.apollo

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class FeatureEventController {

    def requestHandlingService
    def permissionService



    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    /**
     * Permissions handled upstream
     * @param max
     * @return
     */
    def report(Integer max) {

        log.debug "${params}"

        params.max = Math.min(max ?: 15, 100)

        def c = Feature.createCriteria()

        def list = c.list(max: params.max, offset:params.offset) {
            if(params.sort=="owners") {
                owners {
                    order('username', params.order)
                }
            }
            if(params.sort=="sequencename") {
                featureLocations {
                    sequence {
                        order('name', params.order)
                    }
                }
            }
            else if(params.sort=="name") {
                order('name', params.order)
            }
            else if(params.sort=="cvTerm") {
                order('class', params.order)
            }
            else if(params.sort=="organism") {
                featureLocations {
                    sequence {
                        organism {
                            order('commonName',params.order)
                        }
                    }
                }
            }
            else if(params.sort=="lastUpdated") {
                order('lastUpdated',params.order)
            }

            if(params.ownerName!=null&&params.ownerName!="") {
                owners {
                    ilike('username', '%'+params.ownerName+'%')
                }
            }
            if(params.featureType!= null&&params.featureType!= "") {
                ilike('class', '%'+params.featureType)
            }
            if(params.organismName!= null&&params.organismName != "") {
                featureLocations {
                    sequence {
                        organism {
                            ilike('commonName','%'+params.organismName+'%')
                        }
                    }
                }
            }


            'in'('class',requestHandlingService.viewableAnnotationList)
        }

        def filters = [organismName: params.organismName, featureType: params.featureType, ownerName: params.ownerName]

        render view: "report", model: [features: list, featureCount: list.totalCount, organismName: params.organismName, featureType: params.featureType, ownerName: params.ownerName, filters: filters, sort: params.sort]
    }

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond FeatureEvent.list(params), model: [featureEventInstanceCount: FeatureEvent.count()]
    }

    def show(FeatureEvent featureEventInstance) {
        respond featureEventInstance
    }

    def create() {
        respond new FeatureEvent(params)
    }

    @Transactional
    def save(FeatureEvent featureEventInstance) {
        if (featureEventInstance == null) {
            notFound()
            return
        }

        if (featureEventInstance.hasErrors()) {
            respond featureEventInstance.errors, view: 'create'
            return
        }

        featureEventInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'featureEvent.label', default: 'FeatureEvent'), featureEventInstance.id])
                redirect featureEventInstance
            }
            '*' { respond featureEventInstance, [status: CREATED] }
        }
    }

    def edit(FeatureEvent featureEventInstance) {
        respond featureEventInstance
    }

    @Transactional
    def update(FeatureEvent featureEventInstance) {
        if (featureEventInstance == null) {
            notFound()
            return
        }

        if (featureEventInstance.hasErrors()) {
            respond featureEventInstance.errors, view: 'edit'
            return
        }

        featureEventInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'FeatureEvent.label', default: 'FeatureEvent'), featureEventInstance.id])
                redirect featureEventInstance
            }
            '*' { respond featureEventInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(FeatureEvent featureEventInstance) {

        if (featureEventInstance == null) {
            notFound()
            return
        }

        featureEventInstance.delete flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'FeatureEvent.label', default: 'FeatureEvent'), featureEventInstance.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'featureEvent.label', default: 'FeatureEvent'), params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND }
        }
    }
}
