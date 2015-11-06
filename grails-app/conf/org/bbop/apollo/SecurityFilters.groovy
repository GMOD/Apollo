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
                Subject subject = SecurityUtils.getSubject();
                if(subject.isAuthenticated()) {
                    log.debug "authenticated"
            }
                else {
                    log.debug "not auth"
                    log.debug "${request.JSON}"
                    log.debug "${params}"
                    def req=request.JSON
                    if(req.username&&req.password) {
                        def authToken = new UsernamePasswordToken(req.username, req.password)
                        authenticate(authToken)
                    }
                    else {
                        log.warn "username/password not submitted"
                        render text: ([error: "Not authorized"] as JSON), status: HttpStatus.UNAUTHORIZED
                        return false
                    }
                }
            }
        }
    }

}
