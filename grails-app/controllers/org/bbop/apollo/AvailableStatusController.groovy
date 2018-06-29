package org.bbop.apollo


import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

@RestApi(name = "Available Status Services", description = "Methods for managing available statuses")
@Transactional(readOnly = true)
class AvailableStatusController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def permissionService

    def beforeInterceptor = {
        if(!permissionService.checkPermissions(PermissionEnum.ADMINISTRATE)){
            forward action: "notAuthorized", controller: "annotator"
            return
        }
    }

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        def availableStatuss = AvailableStatus.list(params)
        def organismFilterMap = [:]
        AvailableStatusOrganismFilter.findAllByAvailableStatusInList(availableStatuss).each() {
            List filterList = organismFilterMap.containsKey(it.availableStatus) ? organismFilterMap.get(it.availableStatus) : []
            filterList.add(it)
            organismFilterMap[it.availableStatus] = filterList
        }
        respond availableStatuss, model: [availableStatusInstanceCount: AvailableStatus.count(), organismFilters: organismFilterMap]
    }

    def show(AvailableStatus availableStatusInstance) {
        respond availableStatusInstance, model: [organismFilters: AvailableStatusOrganismFilter.findAllByAvailableStatus(availableStatusInstance)]
    }

    def create() {
        respond new AvailableStatus(params)
    }

    @Transactional
    def save(AvailableStatus availableStatusInstance) {
        if (availableStatusInstance == null) {
            notFound()
            return
        }

        if (availableStatusInstance.hasErrors()) {
            respond availableStatusInstance.errors, view:'create'
            return
        }


        availableStatusInstance.save()

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new AvailableStatusOrganismFilter(
                    organism: organism,
                    availableStatus: availableStatusInstance
            ).save()
        }

        availableStatusInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'availableStatus.label', default: 'AvailableStatus'), availableStatusInstance.id])
                redirect availableStatusInstance
            }
            '*' { respond availableStatusInstance, [status: CREATED] }
        }
    }

    def edit(AvailableStatus availableStatusInstance) {
        respond availableStatusInstance, model: [organismFilters: AvailableStatusOrganismFilter.findAllByAvailableStatus(availableStatusInstance)]
    }

    @Transactional
    def update(AvailableStatus availableStatusInstance) {
        if (availableStatusInstance == null) {
            notFound()
            return
        }

        if (availableStatusInstance.hasErrors()) {
            respond availableStatusInstance.errors, view:'edit'
            return
        }

        availableStatusInstance.save()

        AvailableStatusOrganismFilter.deleteAll(AvailableStatusOrganismFilter.findAllByAvailableStatus(availableStatusInstance))

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new AvailableStatusOrganismFilter(
                    organism: organism,
                    availableStatus: availableStatusInstance
            ).save()
        }

        availableStatusInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'AvailableStatus.label', default: 'AvailableStatus'), availableStatusInstance.id])
                redirect availableStatusInstance
            }
            '*'{ respond availableStatusInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(AvailableStatus availableStatusInstance) {

        if (availableStatusInstance == null) {
            notFound()
            return
        }

        def filters = AvailableStatusOrganismFilter.findAllByAvailableStatus(availableStatusInstance)
        AvailableStatusOrganismFilter.deleteAll(filters)
        availableStatusInstance.featureTypes.each {
            availableStatusInstance.removeFromFeatureTypes(it)
        }

        availableStatusInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'AvailableStatus.label', default: 'AvailableStatus'), availableStatusInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'availableStatus.label', default: 'AvailableStatus'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }

    @RestApiMethod(description = "Create status", path = "/availableStatus/createStatus", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "value", type = "string", paramType = RestApiParamType.QUERY, description = "Status name to add")
    ]
    )
    @Transactional
    def createStatus() {
        JSONObject statusJson = permissionService.handleInput(request, params)
        try {
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(statusJson))) {
                if (!statusJson.value) {
                    throw new Exception('empty fields detected')
                }

                log.debug "Adding ${statusJson.value}"
                AvailableStatus status = new AvailableStatus(
                        value: statusJson.value
                ).save(flush: true)

                render status as JSON
            } else {
                def error = [error: 'not authorized to add AvailableStatus']
                render error as JSON
                log.error(error.error)
            }
        } catch (e) {
            def error = [error: 'problem saving AvailableStatus: ' + e]
            render error as JSON
            e.printStackTrace()
            log.error(error.error)
        }
    }

    @RestApiMethod(description = "Update status", path = "/availableStatus/updateStatus", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Status ID to update (or specify the old_value)")
            , @RestApiParam(name = "old_value", type = "string", paramType = RestApiParamType.QUERY, description = "Status name to update")
            , @RestApiParam(name = "new_value", type = "string", paramType = RestApiParamType.QUERY, description = "Status name to change to (the only editable option)")
    ]
    )
    @Transactional
    def updateStatus() {
        try {
            JSONObject statusJson = permissionService.handleInput(request, params)
            log.debug "Updating status ${statusJson}"
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(statusJson))) {
                if (!statusJson.new_value) {
                    throw new Exception('empty fields detected')
                }

                log.debug "status ID: ${statusJson.id}"
                AvailableStatus status = AvailableStatus.findById(statusJson.id) ?: AvailableStatus.findByValue(statusJson.old_value)

                if (!status) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to update the status")
                    render jsonObject as JSON
                    return
                }

                status.value = statusJson.new_value
                status.save(flush: true)

                log.info "Success updating status: ${status.id}"
                render new JSONObject() as JSON
            } else {
                def error = [error: 'not authorized to edit status']
                log.error(error.error)
                render error as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem editing status: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Remove a status", path = "/availableStatus/deleteStatus", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Status ID to remove (or specify the name)")
            , @RestApiParam(name = "value", type = "string", paramType = RestApiParamType.QUERY, description = "Status name to delete")
    ])
    @Transactional
    def deleteStatus() {
        try {
            JSONObject statusJson = permissionService.handleInput(request, params)
            log.debug "Deleting status ${statusJson}"
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(statusJson))) {

                AvailableStatus status = AvailableStatus.findById(statusJson.id) ?: AvailableStatus.findByValue(statusJson.value)

                if (!status) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to delete the status")
                    render jsonObject as JSON
                    return
                }

                status.delete()

                log.info "Success deleting status: ${statusJson}"
                render new JSONObject() as JSON
            } else {
                def error = [error: 'not authorized to delete status']
                log.error(error.error)
                render error as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem deleting status: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Returns a JSON array of all statuses, or optionally, gets information about a specific status", path = "/availableStatus/showStatus", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Status ID to show (or specify a name)")
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Status name to show")
    ])
    @Transactional
    def showStatus() {
        try {
            JSONObject statusJson = permissionService.handleInput(request, params)
            log.debug "Showing status ${statusJson}"
            if (!permissionService.hasGlobalPermissions(statusJson, GlobalPermissionEnum.ADMIN)) {
                render status: UNAUTHORIZED
                return
            }

            if (statusJson.id || statusJson.name) {
                AvailableStatus status = AvailableStatus.findById(statusJson.id) ?: AvailableStatus.findByValue(statusJson.name)

                if (!status) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to delete the status")
                    render jsonObject as JSON
                    return
                }

                log.info "Success showing status: ${statusJson}"
                render status as JSON
            }
            else {
                def statuses = AvailableStatus.all

                log.info "Success showing all statuses"
                render statuses as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem showing statuses: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

}
