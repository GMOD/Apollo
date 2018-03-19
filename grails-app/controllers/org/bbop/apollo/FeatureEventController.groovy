package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import static org.springframework.http.HttpStatus.*

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
     * @param date
     * @param beforeDate
     * @return
     */
    @RestApiMethod(description = "Returns a JSON representation of all current Annotations before or after a given date.", path = "/featureEvent/findChanges", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "date", type = "Date", paramType = RestApiParamType.QUERY, description = "Date to query yyyy-MM-dd:HH:mm:ss or yyyy-MM-dd")
            , @RestApiParam(name = "afterDate", type = "Boolean", paramType = RestApiParamType.QUERY, description = "Search after or on the given date.")
            , @RestApiParam(name = "beforeDate", type = "Boolean", paramType = RestApiParamType.QUERY, description = "Search before or on the given date.")
            , @RestApiParam(name = "max", type = "Integer", paramType = RestApiParamType.QUERY, description = "Max to return")
            , @RestApiParam(name = "sort", type = "String", paramType = RestApiParamType.QUERY, description = "Sort parameter (lastUpdated).  See FeatureEvent object/table.")
            , @RestApiParam(name = "order", type = "String", paramType = RestApiParamType.QUERY, description = "desc/asc sort order by sort param")
    ])
    def findChanges() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        if (!permissionService.hasGlobalPermissions(inputObject, org.bbop.apollo.gwt.shared.GlobalPermissionEnum.ADMIN)) {
            render status: org.springframework.http.HttpStatus.UNAUTHORIZED
            return
        }
        String date = inputObject.date
        Boolean afterDate = inputObject.afterDate
        Date compareDate = Date.parse(date.contains(":") ? FULL_DATE_FORMAT : DAY_DATE_FORMAT, date)
        params.max = params.max ?: 200

        def c = FeatureEvent.createCriteria()

        def list = c.list(max: params.max, offset: params.offset) {
            eq('current', true)
            if (afterDate) {
                ge('lastUpdated', compareDate)
            } else {
                le('lastUpdated', compareDate)
            }
            order(params.sort ?: "lastUpdated", params.order ?: "desc")
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

        params.max = Math.min(max ?: 15, 100)
        def organisms = permissionService.getOrganismsWithMinimumPermission(permissionService.currentUser,PermissionEnum.ADMINISTRATE)

        def c = Feature.createCriteria()

        def list = c.list(max: params.max, offset: params.offset) {
            if (params.sort == "owners") {
                owners {
                    order('username', params.order)
                }
            }
            if (params.sort == "sequencename") {
                featureLocations {
                    sequence {
                        order('name', params.order)
                    }
                }
            } else if (params.sort == "name") {
                order('name', params.order)
            } else if (params.sort == "cvTerm") {
                order('class', params.order)
            } else if (params.sort == "organism") {
                featureLocations {
                    sequence {
                        organism {
                            order('commonName', params.order)
                        }
                    }
                }
            }
            else if (params.sort == "lastUpdated") {
                order('lastUpdated', params.order)
            }
            else if (params.sort == "dateCreated") {
                order('dateCreated', params.order)
            }

            if (params.ownerName && params.ownerName != "null") {
                owners {
                    ilike('username', '%' + params.ownerName + '%')
                }
            }
            if (params.featureType && params.featureType != "null") {
                ilike('class', '%' + params.featureType)
            }
            if (params.organismName && params.organismName != "null") {
                featureLocations {
                    sequence {
                        organism {
                            eq('commonName', params.organismName)
                        }
                    }
                }
            }
            else{
                featureLocations {
                    sequence {
                        organism {
                            inList('commonName', organisms.commonName as List)
                        }
                    }
                }
            }
            if (params.sequenceName && params.sequenceName != "null") {
                featureLocations {
                    sequence {
                        ilike('name', '%' + params.sequenceName + '%')
                    }
                }
            }

            if (params.afterDate) {
                Calendar calendar = GregorianCalendar.getInstance()
                calendar.setTime(params.afterDate)
                calendar.set(Calendar.HOUR,0)
                calendar.set(Calendar.MINUTE,0)
                calendar.set(Calendar.SECOND,0)
                gte('lastUpdated', calendar.getTime())
            }
            if (params.beforeDate) {
                Date beforeDate = params.beforeDate
                // set the before date to the very end of day
                Calendar calendar = GregorianCalendar.getInstance()
                calendar.setTime(params.beforeDate)
                calendar.set(Calendar.HOUR,23)
                calendar.set(Calendar.MINUTE,59)
                calendar.set(Calendar.SECOND,59)
                lte('lastUpdated', calendar.getTime())
            }
            log.debug "afterDateDate ${params.afterDateDate}"
            log.debug "beforeDate ${params.beforeDate}"


            if (params.dateCreatedAfterDate) {
                Calendar calendar = GregorianCalendar.getInstance()
                calendar.setTime(params.dateCreatedAfterDate)
                calendar.set(Calendar.HOUR,0)
                calendar.set(Calendar.MINUTE,0)
                calendar.set(Calendar.SECOND,0)
                gte('dateCreated', calendar.getTime())
            }
            if (params.dateCreatedBeforeDate) {
                Date dateCreatedBeforeDate = params.dateCreatedBeforeDate
                // set the before date to the very end of day
                Calendar calendar = GregorianCalendar.getInstance()
                calendar.setTime(params.dateCreatedBeforeDate)
                calendar.set(Calendar.HOUR,23)
                calendar.set(Calendar.MINUTE,59)
                calendar.set(Calendar.SECOND,59)
                lte('dateCreated', calendar.getTime())
            }
            log.debug "dateCreatedAfterDateDate ${params.dateCreatedAfterDateDate}"
            log.debug "dateCreatedBeforeDate ${params.dateCreatedBeforeDate}"


            'in'('class', requestHandlingService.viewableAnnotationList)
        }

        def filters = [organismName: params.organismName, featureType: params.featureType, ownerName: params.ownerName]

        def featureTypes = []
        RequestHandlingService.viewableAnnotationTypesList.each() {
            featureTypes << it.substring(it.lastIndexOf(".") + 1)
        }.sort()

        Date today = new Date()
        Date veryOldDate = today.minus(20 * 365)  // 20 years back
        Date beforeDate = params.beforeDate ?: today
        Date afterDate = params.afterDate ?: veryOldDate
        Date dateCreatedBeforeDate = params.dateCreatedBeforeDate ?: today
        Date dateCreatedAfterDate = params.dateCreatedAfterDate ?: veryOldDate


        render view: "report", model: [organisms: organisms,dateCreatedAfterDate: dateCreatedAfterDate, dateCreatedBeforeDate: dateCreatedBeforeDate,afterDate: afterDate, beforeDate: beforeDate, sequenceName: params.sequenceName, features: list, featureCount: list.totalCount, organismName: params.organismName, featureTypes: featureTypes, featureType: params.featureType, ownerName: params.ownerName, filters: filters, sort: params.sort]
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
