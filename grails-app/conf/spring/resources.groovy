import org.apache.shiro.web.session.mgt.DefaultWebSessionManager
import org.apache.shiro.web.session.mgt.ServletContainerSessionManager
import org.bbop.apollo.session.SessionExpiredListener
import org.bbop.apollo.session.SessionListeningServletSessionManager

// Place your Spring DSL code here
beans = {

//    from: https://github.com/apa64/try-shiro/blob/master/grails-app/conf/Config.groovy
//    for https://github.com/GMOD/Apollo/issues/493
    apolloSessionListener(SessionExpiredListener)
//    // a session manager, assigned to shiroSecurityManager in Bootstrap
//    apolloSessionManager(DefaultWebSessionManager){
    apolloSessionManager(SessionListeningServletSessionManager){
        sessionListeners =  [ apolloSessionListener ]
    }
//    apolloSessionManager(ServletContainerSessionManager) {
////        sessionListeners = [ ref ("apolloSessionListener") ]
////        sessionListeners = [ ref ("apolloSessionListener") ]
//    }

//    myAuthcListener(MyAuthcListener)
//    // override shiroAuthenticator bean from plugin
//    shiroAuthenticator(ModularRealmAuthenticator) {
//        // default auth strategy from plugin
//        authenticationStrategy = ref("shiroAuthenticationStrategy")
//        // my authentication listener
//        authenticationListeners = [ ref("myAuthcListener") ]
//    }
}
