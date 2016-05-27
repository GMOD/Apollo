package org.bbop.apollo

import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject

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

        all(controller: 'organism', action: '*') {
            before = {
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
                            redirect(uri: "/auth/login")
//                            render text: ([error: "Not authorized"] as JSON), status: HttpStatus.UNAUTHORIZED
                            return false
                        }
                    }
                }
                catch(Exception e) {
                    render ([error: e.message] as JSON)
                    render false
                }
            }
        }
    }

}
