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

        if (request.JSON?.username && request.JSON?.password) {
            permissionService.authenticateWithToken(
                request.JSON.username as String,
                request.JSON.password as String,
                request
            )
        }

        return true
    }

    boolean after() { true }

    void afterView() {}
}
