package org.bbop.apollo

import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.apache.shiro.subject.Subject
import org.springframework.http.HttpStatus
import grails.converters.JSON


class SecurityFilters {
    def filters = {

        all(controller: 'organism', action: '*') {
            before = {
                log.debug "apollo filter ${controllerName}::${actionName}"
                if (!controllerName) return true
                Subject subject = SecurityUtils.getSubject();
                if (!subject.isAuthenticated()) {
                    def req = request.JSON
                    if (req.username && req.password) {
                        def authToken = new UsernamePasswordToken(req.username, req.password)
                        subject.login(authToken)
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
