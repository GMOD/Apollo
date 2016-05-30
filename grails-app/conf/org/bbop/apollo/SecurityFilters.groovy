package org.bbop.apollo

import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject
import org.apache.shiro.web.util.SavedRequest
import org.apache.shiro.web.util.WebUtils

class SecurityFilters {
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

        all(controller: '*', action: '*') {
            before = {
                if (controllerName == "organism"
                        || controllerName == "home"
                        || actionName == "report"
                        || actionName == "loadLink"
                ) {
                    try {
                        log.debug "apollo filter ${controllerName}::${actionName}"
                        Subject subject = SecurityUtils.getSubject();
                        if (!subject.isAuthenticated()) {
                            def req = request.JSON
                            if (req.username && req.password) {
                                def authToken = new UsernamePasswordToken(req.username, req.password)
                                subject.login(authToken)
                                redirect(uri: params.targetUri)
                                return true
                            } else {
                                log.warn "username/password not submitted"
                                // TODO: works for most
                                def targetUri = "/${controllerName}/${actionName}"
                                int paramCount = 0
                                def paramString = ""
                                for(p in params){
                                    if(p.key!="controller" || p.key!="action"){
                                        paramString += paramCount==0 ? "?" : "&"
                                        paramString += p.key +"="+ p.value
                                        ++paramCount
                                    }
                                }
                                targetUri = targetUri + (paramString ? URLEncoder.encode(paramString,"UTF-8") :"")
                                redirect(uri: "/auth/login?targetUri=${targetUri}")
                                return false
                            }
                        }
                    }
                    catch (Exception e) {
                        render([error: e.message] as JSON)
                        render false
                    }
                }
            }
        }
    }

}
