package org.bbop.apollo.session

import org.apache.shiro.subject.Subject
import org.apache.shiro.web.mgt.DefaultWebSecurityManager

/**
 * Created by nathandunn on 8/3/15.
 */
class SessionExpiredListener  extends  DefaultWebSecurityManager{

    def permissionService

    @Override
    void logout(Subject subject) {
        println "trying to logout? "
        String principal = subject.principal
        super.logout(subject)
        permissionService.sendLogout(subject.principal)
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
