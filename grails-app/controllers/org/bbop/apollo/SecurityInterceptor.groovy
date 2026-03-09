package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.security.ApolloSecurityUtils
import org.grails.web.json.JSONObject
import org.springframework.http.HttpStatus
import java.net.URLEncoder

class SecurityInterceptor {

    PermissionService permissionService
    static final List<String> WEB_ACTION_LIST = ['index', 'show', 'create', 'edit', 'update', 'delete']

    static final List<String> SECURED_CONTROLLERS = [
        'home', 'cannedKey', 'cannedValue', 'suggestedName',
        'geneProduct', 'cannedComment', 'availableStatus',
        'proxy', 'featureType'
    ]

    static final List<String> SECURED_ACTIONS = ['report', 'loadLink']

    SecurityInterceptor() {
        matchAll()
    }

    boolean before() {
        if (!(controllerName in SECURED_CONTROLLERS) && !(actionName in SECURED_ACTIONS)) {
            return true
        }

        try {
            log.debug "apollo interceptor ${controllerName}::${actionName}"
            if (ApolloSecurityUtils.isAuthenticated()) {
                return true
            }

            if (permissionService.authenticateWithToken(null, null, request)) {
                if (params.targetUri) {
                    redirect(uri: params.targetUri)
                }
                return true
            }

            log.warn "Authentication failed"

            if (request.JSON?.size() > 0) {
                response.status = HttpStatus.UNAUTHORIZED.value()
                render new JSONObject("error": "Failed to authenticate")
                return false
            }

            String queryString = request.queryString
            String targetUri = "/${controllerName}/${actionName}"
            if (queryString) {
                targetUri += "?" + queryString
            }
            String encodedUri = URLEncoder.encode(targetUri, "UTF-8")
            redirect(uri: "${request.contextPath}/auth/login?targetUri=${encodedUri}")
            return false
        } catch (Exception e) {
            render([error: e.message] as JSON)
            return false
        }
    }

    boolean after() { true }

    void afterView() {}
}
