package org.bbop.apollo.session

import org.apache.shiro.session.Session
import org.apache.shiro.session.SessionListenerAdapter

/**
 * Created by nathandunn on 8/3/15.
 */
class SessionExpiredListener  extends SessionListenerAdapter{

//    def permissionService

//    @Override
//    void logout(Subject subject) {
//    }

    @Override
    void onStart(Session session) {
        println "session listener started"
        super.onStart(session)
    }

    @Override
    void onStop(Session session) {
        println "session listener STOPPED"
        super.onStop(session)
    }

    @Override
    void onExpiration(Session session) {
        println "session EXPIRED "
        super.onExpiration(session)
//        println "trying to logout? " + session.id
//        SecurityUtils.securityManager.getSession(session.id)
//        String principal = subject.principal
//        super.logout(subject)
//        permissionService.sendLogout(subject.principal)
    }
////    public void onExpiration(Session session) {
////        sendLogout()
////    }
//
//    @Override
//    void sessionCreated(HttpSessionEvent httpSessionEvent) {
//
//    }
//
//    @Override
//    void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
//
//    }
}
