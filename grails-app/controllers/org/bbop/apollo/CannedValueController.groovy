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

@RestApi(name = "Canned Values Services", description = "Methods for managing canned values")
@Transactional(readOnly = true)
class CannedValueController {

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
        def cannedValues = CannedValue.list(params)
        def organismFilterMap = [:]
        CannedValueOrganismFilter.findAllByCannedValueInList(cannedValues).each() {
            List filterList = organismFilterMap.containsValue(it.cannedValue) ? organismFilterMap.get(it.cannedValue) : []
            filterList.add(it)
            organismFilterMap[it.cannedValue] = filterList
        }
        respond cannedValues, model: [cannedValueInstanceCount: CannedValue.count(), organismFilters: organismFilterMap]
    }

    def show(CannedValue cannedValueInstance) {
        respond cannedValueInstance, model: [organismFilters: CannedValueOrganismFilter.findAllByCannedValue(cannedValueInstance)]
    }

    def create() {
        respond new CannedValue(params)
    }

    @Transactional
    def save(CannedValue cannedValueInstance) {
        if (cannedValueInstance == null) {
            notFound()
            return
        }

        if (cannedValueInstance.hasErrors()) {
            respond cannedValueInstance.errors, view:'create'
            return
        }

        cannedValueInstance.save()

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new CannedValueOrganismFilter(
                    organism: organism,
                    cannedValue: cannedValueInstance
            ).save()
        }

        cannedValueInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'cannedValue.label', default: 'CannedValue'), cannedValueInstance.id])
                redirect cannedValueInstance
            }
            '*' { respond cannedValueInstance, [status: CREATED] }
        }
    }

    def edit(CannedValue cannedValueInstance) {
        respond cannedValueInstance
    }

    @Transactional
    def update(CannedValue cannedValueInstance) {
        if (cannedValueInstance == null) {
            notFound()
            return
        }

        if (cannedValueInstance.hasErrors()) {
            respond cannedValueInstance.errors, view:'edit'
            return
        }

        cannedValueInstance.save()

        CannedValueOrganismFilter.deleteAll(CannedValueOrganismFilter.findAllByCannedValue(cannedValueInstance))

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new CannedValueOrganismFilter(
                    organism: organism,
                    cannedValue: cannedValueInstance
            ).save()
        }

        cannedValueInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'CannedValue.label', default: 'CannedValue'), cannedValueInstance.id])
                redirect cannedValueInstance
            }
            '*'{ respond cannedValueInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(CannedValue cannedValueInstance) {

        if (cannedValueInstance == null) {
            notFound()
            return
        }

        cannedValueInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'CannedValue.label', default: 'CannedValue'), cannedValueInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'cannedValue.label', default: 'CannedValue'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }

    @RestApiMethod(description = "Create canned value", path = "/cannedValue/createValue", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "value", type = "string", paramType = RestApiParamType.QUERY, description = "Canned value to add")
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "Optional additional information")
    ]
    )
    @Transactional
    def createValue() {
        JSONObject valueJson = permissionService.handleInput(request, params)
        try {
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(valueJson))) {
                if (!valueJson.value) {
                    throw new Exception('empty fields detected')
                }

                if (!valueJson.metadata) {
                    valueJson.metadata = ""
                }

                log.debug "Adding canned value ${valueJson.value}"
                CannedValue value = new CannedValue(
                        label: valueJson.value,
                        metadata: valueJson.metadata
                ).save(flush: true)

                render value as JSON
            } else {
                def error = [error: 'not authorized to add CannedValue']
                render error as JSON
                log.error(error.error)
            }
        } catch (e) {
            def error = [error: 'problem saving CannedValue: ' + e]
            render error as JSON
            e.printStackTrace()
            log.error(error.error)
        }
    }

    @RestApiMethod(description = "Update canned value", path = "/cannedValue/updateValue", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Canned value ID to update (or specify the old_value)")
            , @RestApiParam(name = "old_value", type = "string", paramType = RestApiParamType.QUERY, description = "Canned value to update")
            , @RestApiParam(name = "new_value", type = "string", paramType = RestApiParamType.QUERY, description = "Canned value to change to (the only editable option)")
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "Optional additional information")
    ]
    )
    @Transactional
    def updateValue() {
        try {
            JSONObject valueJson = permissionService.handleInput(request, params)
            log.debug "Updating canned value ${valueJson}"
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(valueJson))) {

                log.debug "Canned value ID: ${valueJson.id}"
                CannedValue value = CannedValue.findById(valueJson.id) ?: CannedValue.findByLabel(valueJson.old_value)

                if (!value) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to update the canned value")
                    render jsonObject as JSON
                    return
                }

                value.label = valueJson.new_value

                if (valueJson.metadata) {
                    value.metadata = valueJson.metadata
                }

                value.save(flush: true)

                log.info "Success updating canned value: ${value.id}"
                render new JSONObject() as JSON
            } else {
                def error = [error: 'not authorized to edit canned value']
                log.error(error.error)
                render error as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem editing canned value: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Remove a canned value", path = "/cannedValue/deleteValue", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Canned value ID to remove (or specify the name)")
            , @RestApiParam(name = "value", type = "string", paramType = RestApiParamType.QUERY, description = "Canned value to delete")
    ])
    @Transactional
    def deleteValue() {
        try {
            JSONObject valueJson = permissionService.handleInput(request, params)
            log.debug "Deleting canned value ${valueJson}"
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(valueJson))) {

                CannedValue value = CannedValue.findById(valueJson.id) ?: CannedValue.findByLabel(valueJson.value)

                if (!value) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to delete the canned value")
                    render jsonObject as JSON
                    return
                }

                value.delete()

                log.info "Success deleting canned value: ${valueJson}"
                render new JSONObject() as JSON
            } else {
                def error = [error: 'not authorized to delete canned value']
                log.error(error.error)
                render error as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem deleting canned value: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Returns a JSON array of all canned values, or optionally, gets information about a specific canned value", path = "/cannedValue/showValue", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Value ID to show (or specify a value)")
            , @RestApiParam(name = "value", type = "string", paramType = RestApiParamType.QUERY, description = "Value to show")
    ])
    @Transactional
    def showValue() {
        try {
            JSONObject valueJson = permissionService.handleInput(request, params)
            log.debug "Showing canned value ${valueJson}"
            if (!permissionService.hasGlobalPermissions(valueJson, GlobalPermissionEnum.ADMIN)) {
                render status: UNAUTHORIZED
                return
            }

            if (valueJson.id || valueJson.value) {
                CannedValue value = CannedValue.findById(valueJson.id) ?: CannedValue.findByLabel(valueJson.old_value)

                if (!value) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to delete the canned values")
                    render jsonObject as JSON
                    return
                }

                log.info "Success showing value: ${valueJson}"
                render value as JSON
            }
            else {
                def values = CannedValue.all

                log.info "Success showing all canned values"
                render values as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem showing canned values: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }
}
