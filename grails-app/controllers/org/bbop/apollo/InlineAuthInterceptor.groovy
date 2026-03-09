package org.bbop.apollo

import org.bbop.apollo.security.ApolloSecurityUtils

class InlineAuthInterceptor {

    PermissionService permissionService

    int order = -100

    InlineAuthInterceptor() {
        matchAll()
    }

    boolean before() {
        if (ApolloSecurityUtils.isAuthenticated()) {
            return true
        }

        String username = null
        String password = null

        if (request.JSON?.username && request.JSON?.password) {
            username = request.JSON.username as String
            password = request.JSON.password as String
        } else if (params.username && params.password) {
            username = params.username as String
            password = params.password as String
        }

        if (username && password) {
            permissionService.authenticateWithToken(username, password, request)
        }

        return true
    }

    boolean after() { true }

    void afterView() {}
}
