package org.bbop.apollo.authenticator

import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.session.Session
import org.apache.shiro.subject.Subject

@Transactional
class UsernamePasswordAuthenticatorService implements AuthenticatorService{

    def authenticate(UsernamePasswordToken authToken, Session session ) {
        try {
            Subject subject = SecurityUtils.getSubject();
            session = subject.getSession(true);
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
