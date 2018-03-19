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

@RestApi(name = "Canned Keys Services", description = "Methods for managing canned keys")
@Transactional(readOnly = true)
class CannedKeyController {

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

        def cannedKeys = CannedKey.list(params)
        def organismFilterMap = [:]
        CannedKeyOrganismFilter.findAllByCannedKeyInList(cannedKeys).each() {
            List filterList = organismFilterMap.containsKey(it.cannedKey) ? organismFilterMap.get(it.cannedKey) : []
            filterList.add(it)
            organismFilterMap[it.cannedKey] = filterList
        }
        respond cannedKeys, model: [cannedKeyInstanceCount: CannedKey.count(), organismFilters: organismFilterMap]
    }

    def show(CannedKey cannedKeyInstance) {
        respond cannedKeyInstance, model: [organismFilters: CannedKeyOrganismFilter.findAllByCannedKey(cannedKeyInstance)]
    }

    def create() {
        respond new CannedKey(params)
    }

    @Transactional
    def save(CannedKey cannedKeyInstance) {
        if (cannedKeyInstance == null) {
            notFound()
            return
        }

        if (cannedKeyInstance.hasErrors()) {
            respond cannedKeyInstance.errors, view:'create'
            return
        }

        cannedKeyInstance.save()

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new CannedKeyOrganismFilter(
                    organism: organism,
                    cannedKey: cannedKeyInstance
            ).save()
        }

        cannedKeyInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'cannedKey.label', default: 'CannedKey'), cannedKeyInstance.id])
                redirect cannedKeyInstance
            }
            '*' { respond cannedKeyInstance, [status: CREATED] }
        }
    }

    def edit(CannedKey cannedKeyInstance) {
        respond cannedKeyInstance
    }

    @Transactional
    def update(CannedKey cannedKeyInstance) {
        if (cannedKeyInstance == null) {
            notFound()
            return
        }

        if (cannedKeyInstance.hasErrors()) {
            respond cannedKeyInstance.errors, view:'edit'
            return
        }

        cannedKeyInstance.save()

        CannedKeyOrganismFilter.deleteAll(CannedKeyOrganismFilter.findAllByCannedKey(cannedKeyInstance))

        if (params.organisms instanceof String) {
            params.organisms = [params.organisms]
        }

        params?.organisms.each {
            Organism organism = Organism.findById(it)
            new CannedKeyOrganismFilter(
                    organism: organism,
                    cannedKey: cannedKeyInstance
            ).save()
        }

        cannedKeyInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'CannedKey.label', default: 'CannedKey'), cannedKeyInstance.id])
                redirect cannedKeyInstance
            }
            '*'{ respond cannedKeyInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(CannedKey cannedKeyInstance) {

        if (cannedKeyInstance == null) {
            notFound()
            return
        }

        cannedKeyInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'CannedKey.label', default: 'CannedKey'), cannedKeyInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'cannedKey.label', default: 'CannedKey'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }

    @RestApiMethod(description = "Create canned key", path = "/cannedKey/createKey", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "key", type = "string", paramType = RestApiParamType.QUERY, description = "Canned key to add")
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "Optional additional information")
    ]
    )
    @Transactional
    def createKey() {
        JSONObject keyJson = permissionService.handleInput(request, params)
        try {
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(keyJson))) {
                if (!keyJson.key) {
                    throw new Exception('empty fields detected')
                }

                if (!keyJson.metadata) {
                    keyJson.metadata = ""
                }

                log.debug "Adding canned key ${keyJson.key}"
                CannedKey key = new CannedKey(
                        label: keyJson.key,
                        metadata: keyJson.metadata
                ).save(flush: true)

                render key as JSON
            } else {
                def error = [error: 'not authorized to add CannedKey']
                render error as JSON
                log.error(error.error)
            }
        } catch (e) {
            def error = [error: 'problem saving CannedKey: ' + e]
            render error as JSON
            e.printStackTrace()
            log.error(error.error)
        }
    }

    @RestApiMethod(description = "Update canned key", path = "/cannedKey/updateKey", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Canned key ID to update (or specify the old_key)")
            , @RestApiParam(name = "old_key", type = "string", paramType = RestApiParamType.QUERY, description = "Canned key to update")
            , @RestApiParam(name = "new_key", type = "string", paramType = RestApiParamType.QUERY, description = "Canned key to change to (the only editable option)")
            , @RestApiParam(name = "metadata", type = "string", paramType = RestApiParamType.QUERY, description = "Optional additional information")
    ]
    )
    @Transactional
    def updateKey() {
        try {
            JSONObject keyJson = permissionService.handleInput(request, params)
            log.debug "Updating canned key ${keyJson}"
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(keyJson))) {

                log.debug "Canned key ID: ${keyJson.id}"
                CannedKey key = CannedKey.findById(keyJson.id) ?: CannedKey.findByLabel(keyJson.old_key)

                if (!key) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to update the canned key")
                    render jsonObject as JSON
                    return
                }

                key.label = keyJson.new_key

                if (keyJson.metadata) {
                    key.metadata = keyJson.metadata
                }

                key.save(flush: true)

                log.info "Success updating canned key: ${key.id}"
                render new JSONObject() as JSON
            } else {
                def error = [error: 'not authorized to edit canned key']
                log.error(error.error)
                render error as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem editing canned key: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Remove a canned key", path = "/cannedKey/deleteKey", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Canned key ID to remove (or specify the name)")
            , @RestApiParam(name = "key", type = "string", paramType = RestApiParamType.QUERY, description = "Canned key to delete")
    ])
    @Transactional
    def deleteKey() {
        try {
            JSONObject keyJson = permissionService.handleInput(request, params)
            log.debug "Deleting canned key ${keyJson}"
            if (permissionService.isUserGlobalAdmin(permissionService.getCurrentUser(keyJson))) {

                CannedKey key = CannedKey.findById(keyJson.id) ?: CannedKey.findByLabel(keyJson.key)

                if (!key) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to delete the canned key")
                    render jsonObject as JSON
                    return
                }

                key.delete()

                log.info "Success deleting canned key: ${keyJson}"
                render new JSONObject() as JSON
            } else {
                def error = [error: 'not authorized to delete canned key']
                log.error(error.error)
                render error as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem deleting canned key: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Returns a JSON array of all canned keys, or optionally, gets information about a specific canned key", path = "/cannedKey/showKey", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Key ID to show (or specify a key)")
            , @RestApiParam(name = "key", type = "string", paramType = RestApiParamType.QUERY, description = "Key to show")
    ])
    @Transactional
    def showKey() {
        try {
            JSONObject keyJson = permissionService.handleInput(request, params)
            log.debug "Showing canned key ${keyJson}"
            if (!permissionService.hasGlobalPermissions(keyJson, GlobalPermissionEnum.ADMIN)) {
                render status: UNAUTHORIZED
                return
            }

            if (keyJson.id || keyJson.key) {
                CannedKey key = CannedKey.findById(keyJson.id) ?: CannedKey.findByLabel(keyJson.old_key)

                if (!key) {
                    JSONObject jsonObject = new JSONObject()
                    jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to delete the canned keys")
                    render jsonObject as JSON
                    return
                }

                log.info "Success showing key: ${keyJson}"
                render key as JSON
            }
            else {
                def keys = CannedKey.all

                log.info "Success showing all canned keys"
                render keys as JSON
            }
        }
        catch (Exception e) {
            def error = [error: 'problem showing canned keys: ' + e]
            log.error(error.error)
            render error as JSON
        }
    }
}
