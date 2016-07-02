package org.bbop.apollo.authenticator

import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.session.Session
import org.apache.shiro.subject.Subject

import javax.servlet.http.HttpServletRequest

@Transactional
class UsernamePasswordAuthenticatorService implements AuthenticatorService{

    def authenticate(UsernamePasswordToken authToken, HttpServletRequest request) {
        try {
            Subject subject = SecurityUtils.getSubject();
//            Session session = subject.getSession(true);
            subject.login(authToken)
            if (!subject.authenticated) {
                log.error "Failed to authenticate user ${authToken.username}"
                return false
            }
        } catch (Exception ae) {
            log.error("Problem authenticating: " + ae.fillInStackTrace())
            return false
        }

    }
}
