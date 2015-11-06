package org.bbop.apollo

import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject
import org.apache.shiro.SecurityUtils
import org.springframework.http.HttpStatus
import grails.converters.JSON


class SecurityFilters {
    def filters = {

        all(uri: "/**") {
            before = {
                log.debug "apollo filter ${controllerName}::${actionName}"
                if (!controllerName) return true
                if(controllerName=="jbrowse") return true
                if(controllerName=="assets") return true
                if(controllerName=="organism"&&actionName=="chooseOrganismForJbrowse") return true
                if(controllerName=="annotator" && actionName=="index") return true
                if(controllerName=="user" && actionName=="checkLogin") return true
                if(controllerName!="login") {
                    log.debug "here"
                    Subject subject = SecurityUtils.getSubject();
                    if (!subject.isAuthenticated()) {
                        def req = request.JSON
                        if (req.username && req.password) {
                            def authToken = new UsernamePasswordToken(req.username, req.password)
                            authenticate(authToken)
                        } else {
                            log.warn "username/password not submitted"
                            render text: ([error: "Not authorized"] as JSON), status: HttpStatus.UNAUTHORIZED
                            return false
                        }
                    }
                }
            }
        }
    }

}
