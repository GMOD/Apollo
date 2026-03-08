package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONException
import org.grails.web.json.JSONObject
import org.springframework.http.HttpStatus

abstract class AbstractApolloController {

    PermissionService permissionService
    static final String REST_OPERATION = "operation"
    static final String REST_TRACK = "track"
    static final String REST_FEATURES = "features"
    static final String REST_USERNAME = "username"
    static final String REST_PERMISSION = "permission"
    static final String REST_DATA_ADAPTER = "data_adapter"
    static final String REST_DATA_ADAPTERS = "data_adapters"
    static final String REST_KEY = "key"
    static final String REST_OPTIONS = "options"
    static final String REST_TRANSLATION_TABLE = "translation_table"
    static final String REST_START_PROTEINS = "start_proteins"
    static final String REST_STOP_PROTEINS = "stop_proteins"

    protected String underscoreToCamelCase(String underscore) {
        if (!underscore || underscore.isAllWhitespace()) {
            return ''
        }
        return underscore.replaceAll(/_\w/) { it[1].toUpperCase() }
    }


    protected def withPermission(PermissionEnum permission, Closure action) {
        try {
            JSONObject inputObject = permissionService.handleInput(request, params)
            permissionService.hasPermissions(inputObject, PermissionEnum.READ)
            if (permissionService.hasPermissions(inputObject, permission)) {
                render action(inputObject) as JSON
            } else {
                render status: HttpStatus.UNAUTHORIZED
            }
        } catch (Exception e) {
            def error = [error: e.message]
            render error as JSON
        }
    }

    protected def findPost() {
        for (p in params) {
            String key = p.key
            if (key.contains("operation")) {
                return (JSONObject) JSON.parse(key)
            }
        }
    }
}
