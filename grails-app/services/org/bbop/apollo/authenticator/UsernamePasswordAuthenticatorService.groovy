package org.bbop.apollo.authenticator

import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.session.Session
import org.apache.shiro.subject.Subject

import javax.servlet.http.HttpServletRequest

@Transactional
class UsernamePasswordAuthenticatorService implements AuthenticatorService{

    @Override
    def authenticate(HttpServletRequest request) {
        log.error("Not implemented without a token")
        return false
    }

    def authenticate(UsernamePasswordToken authToken, HttpServletRequest request) {
        try {
            Subject subject = SecurityUtils.getSubject();
            subject.login(authToken)
            if (!subject.authenticated) {
                log.error "Failed to authenticate user ${authToken.username}"
                return false
            }
            return true
        } catch (Exception ae) {
            log.error("Problem authenticating: " + ae.fillInStackTrace())
            return false
        }

    }


    @Override
    Boolean requiresToken() {
        return true
    }
}
