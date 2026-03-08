package org.bbop.apollo

import org.bbop.apollo.security.ApolloSecurityUtils
import org.springframework.security.core.AuthenticationException

class AuthController {

    PermissionService permissionService
    PreferenceService preferenceService
    def index = { redirect(action: "login", params: params) }

    def login = {
        return [ username: params.username, rememberMe: (params.rememberMe != null), targetUri: params.targetUri ]
    }

    def signIn = {
        // If a controller redirected to this page, redirect back
        // to it. Otherwise redirect to the root URI.
        def targetUri = params.targetUri ?: "/"

        // Handle saved request from session
        def session = ApolloSecurityUtils.getSession(false)
        if (session) {
            String savedTargetUri = session.getAttribute("SPRING_SECURITY_SAVED_REQUEST_TARGET")
            if (savedTargetUri) {
                targetUri = savedTargetUri
                session.removeAttribute("SPRING_SECURITY_SAVED_REQUEST_TARGET")
            }
        }

        try{
            permissionService.authenticateWithToken(params.username as String, params.password as String, request)
            if(targetUri) {
                if (targetUri.contains("http://") || targetUri.contains("https://") || targetUri.contains("ftp://")) {
                    redirect(uri: "${request.contextPath}${targetUri}")
                }
                else {
                    redirect(uri: targetUri)
                }
                return
            }
        }
        catch (AuthenticationException ex){
            log.info "Authentication failure for user '${params.username}'."
            flash.message = message(code: "login.failed")

            def m = [ username: params.username ]
            if (params.rememberMe) {
                m["rememberMe"] = true
            }

            if (params.targetUri) {
                m["targetUri"] = params.targetUri
            }

            redirect(action: "login", params: m)
        }
    }

    def signOut = {
        preferenceService.evaluateSaves(true)
        ApolloSecurityUtils.logout()

        redirect(uri: "/")
    }

    def unauthorized = {
    }
}
