package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONException
import org.grails.web.json.JSONObject
import org.springframework.http.HttpStatus

abstract class AbstractApolloController {

    def permissionService

    public static String REST_OPERATION = "operation"
    public static String REST_TRACK = "track"
    public static String REST_FEATURES = "features"
    public static REST_USERNAME = "username"
    public static REST_PERMISSION = "permission"
    public static REST_DATA_ADAPTER = "data_adapter"
    public static REST_DATA_ADAPTERS = "data_adapters"
    public static REST_KEY = "key"
    public static REST_OPTIONS = "options"
    public static REST_TRANSLATION_TABLE = "translation_table"
    public static REST_START_PROTEINS = "start_proteins"
    public static REST_STOP_PROTEINS = "stop_proteins"

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
        } catch (e) {
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
