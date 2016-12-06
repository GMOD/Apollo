package org.bbop.apollo

import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject
import org.apache.shiro.web.util.SavedRequest
import org.apache.shiro.web.util.WebUtils

class SecurityFilters {

    def permissionService

    def filters = {

        // TODO: this is the right way to do this as it uses proper forwarding, but
        // the user object lacks permissions
//        all(uri: "/**") {
//            before = {
//                // Ignore direct views (e.g. the default main index page).
//                if (!controllerName) return true
//                // Access control by convention.
//                accessControl(auth:false)
//            }
//        }

        // TODO: route more controllers through here
        all(controller: '*', action: '*') {
            before = {
                if (controllerName == "organism"
                        || controllerName == "home"
                        || controllerName == "cannedKey"
                        || controllerName == "cannedValue"
                        || controllerName == "cannedComment"
                        || actionName == "report"
                        || actionName == "loadLink"
                ) {
                    try {
                        log.debug "apollo filter ${controllerName}::${actionName}"
                        Subject subject = SecurityUtils.getSubject();
                        if (!subject.isAuthenticated()) {
                            def req = request.JSON
                            def authToken = req.username ? new UsernamePasswordToken(req.username, req.password) : null  // we don't try to add this here
                            if(authToken && permissionService.authenticateWithToken(authToken,request)){
                                if(params.targetUri){
                                    redirect(uri: params.targetUri)
                                }
                                return true
                            } else {
                                log.warn "Authentication failed"
                                def targetUri = "/${controllerName}/${actionName}"
                                int paramCount = 0
                                def paramString = ""
                                for(p in params){
                                    if(p.key!="controller" && p.key!="action"){
                                        paramString += paramCount==0 ? "?" : "&"
                                        if(p.key.startsWith("addStores")){
                                            paramString += p.key +"="+ URLEncoder.encode(p.value,"UTF-8")
                                        }
                                        else{
                                            paramString += p.key +"="+ p.value
                                        }
                                        ++paramCount
                                    }
                                }
                                targetUri = targetUri + paramString
                                redirect(uri: "/auth/login?targetUri=${targetUri}")
                                return false
                            }
                        }
                    }
                    catch (Exception e) {
                        render([error: e.message] as JSON)
                        return false
                    }
                }
            }
        }
    }

}
