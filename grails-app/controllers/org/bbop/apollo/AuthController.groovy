package org.bbop.apollo

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.web.util.SavedRequest
import org.apache.shiro.web.util.WebUtils

class AuthController {


    def permissionService
    def preferenceService

    def index = { redirect(action: "login", params: params) }

    def login = {
        WebUtils.saveRequest(request)
        return [ username: params.username, rememberMe: (params.rememberMe != null), targetUri: params.targetUri ]
    }

    def signIn = {
        def authToken = new UsernamePasswordToken(params.username, params.password as String)

        // Support for "remember me"
        if (params.rememberMe) {
            authToken.rememberMe = true
        }
        
        // If a controller redirected to this page, redirect back
        // to it. Otherwise redirect to the root URI.
        def targetUri = params.targetUri ?: "/"
        println "targetUri: ${targetUri}"
        
        // Handle requests saved by Shiro filters.
        SavedRequest savedRequest = WebUtils.getSavedRequest(request)
        println "saved request: ${savedRequest}"
        if (savedRequest) {
            println "query: ${savedRequest.queryString} vs ${targetUri}"
            if(savedRequest.queryString && savedRequest.queryString.startsWith("targetUri=")){
                targetUri = savedRequest.queryString.substring("targetUri=".size())
                println "target URI / saved request: ${targetUri}"
            }
            else{
                println "ELSE target URI / saved request: ${targetUri}"
                targetUri = savedRequest.requestURI - request.contextPath
                if (savedRequest.queryString) {
                    println "B"
                    targetUri = targetUri + '?' + savedRequest.queryString
                }
                println "ELSE final target URi target URI / saved request: ${targetUri}"
            }
        }
        
        try{
            // Perform the actual login. An AuthenticationException
            // will be thrown if the username is unrecognised or the
            // password is incorrect.
            permissionService.authenticateWithToken(authToken,request)
//            SecurityUtils.subject.login(authToken)
            println "authenticated has a target URI: ${targetUri}"
            if(targetUri) {
                println "calling Target uri: ${targetUri}"
//                if (targetUri.contains("http://") || targetUri.contains("https://") || targetUri.contains("ftp://")) {
//                    redirect(uri: "${request.contextPath}${targetUri}")
//                }
//                else {
                    redirect(url: targetUri)
//                }
//                response.setStatus(301);
//                response.setHeader("Location", "http://location:8080/apollo" + request.forwardURI)
//                response.flushBuffer()
                return true; // return false, otherwise request is handled from controller
            }
        }
        catch (AuthenticationException ex){
            // Authentication failed, so display the appropriate message
            // on the login page.
            log.info "Authentication failure for user '${params.username}'."
            flash.message = message(code: "login.failed")

            // Keep the username and "remember me" setting so that the
            // user doesn't have to enter them again.
            def m = [ username: params.username ]
            if (params.rememberMe) {
                m["rememberMe"] = true
            }

            // Remember the target URI too.
            if (params.targetUri) {
                m["targetUri"] = params.targetUri
            }

            // Now redirect back to the login page.
            redirect(action: "login", params: m)
        }
    }

    def signOut = {
        preferenceService.evaluateSaves(true)
        // Log the user out of the application.
        SecurityUtils.subject?.logout()
        webRequest.getCurrentRequest().session = null

        // For now, redirect back to the home page.
        redirect(uri: "/")
    }

    def unauthorized = {
//        render "You do not have permission to access this page."
    }
}
