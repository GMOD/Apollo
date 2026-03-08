package org.bbop.apollo.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

import jakarta.servlet.http.HttpSession

class ApolloSecurityUtils {

    static String getCurrentUsername() {
        def auth = SecurityContextHolder.context.authentication
        if (auth != null && auth.principal != "anonymousUser") {
            return auth.name
        }
        return null
    }

    static boolean isAuthenticated() {
        def auth = SecurityContextHolder.context.authentication
        return auth != null && auth.isAuthenticated() && auth.principal != "anonymousUser"
    }

    static HttpSession getSession(boolean create = false) {
        def attrs = RequestContextHolder.getRequestAttributes()
        if (attrs instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attrs).request.getSession(create)
        }
        return null
    }

    static void loginUser(String username, Collection<String> roleNames) {
        def authorities = roleNames.collect { new SimpleGrantedAuthority("ROLE_${it}") }
        def auth = new UsernamePasswordAuthenticationToken(username, null, authorities)
        def context = SecurityContextHolder.context
        context.authentication = auth
        // Spring Security 6 no longer auto-persists SecurityContext to session.
        // Explicitly save it so subsequent requests remain authenticated.
        def session = getSession(true)
        if (session) {
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
            )
        }
    }

    static void logout() {
        SecurityContextHolder.clearContext()
        try {
            getSession(false)?.invalidate()
        } catch (e) {
            // session may already be invalidated
        }
    }
}
