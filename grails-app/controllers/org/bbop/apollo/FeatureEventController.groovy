package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class FeatureEventController {

    static final String DAY_DATE_FORMAT = 'yyyy-MM-dd'
    static final String FULL_DATE_FORMAT = DAY_DATE_FORMAT + ' HH:mm:ss'

    def requestHandlingService
    def permissionService



    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    /**
     * Returns a JSON representation of all "current" Genome Annotations before or after a given date.
     *
     * @param compareDateString
     * @param beforeDate
     * @return
     */
    @RestApiMethod(description="Returns a JSON representation of all current Annotations before or after a given date." ,path="/featureEvent/findChanges",verb = RestApiVerb.POST )
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="compareDateString", type="Date", paramType = RestApiParamType.QUERY,description = "Date to query yyyy-MM-dd:HH:mm:ss or yyyy-MM-dd")
            ,@RestApiParam(name="afterDate", type="Boolean", paramType = RestApiParamType.QUERY,description = "Search after the given date.")
    ] )
    def findChanges(String compareDateString,Boolean afterDate){

        Date date = Date.parse( compareDateString.contains(":")?FULL_DATE_FORMAT:DAY_DATE_FORMAT,compareDateString)
        params.max = params.max ?: 50

        def c = FeatureEvent.createCriteria()

        def list = c.list(max: params.max, offset:params.offset) {
            eq('current',true)
            if(afterDate){
                lte('lastUpdated',date)
            }
            else{
                gte('lastUpdated',date)
            }
            order('lastUpdated',params.sort ?: "lastUpdated",params.order ?: "desc")
        }

        JSONArray returnList = new JSONArray()

        list.each { FeatureEvent featureEvent ->
            JSONArray entry = JSON.parse(featureEvent.newFeaturesJsonArray) as JSONArray
            returnList.add(entry)
        }

        render returnList as JSON
    }

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
