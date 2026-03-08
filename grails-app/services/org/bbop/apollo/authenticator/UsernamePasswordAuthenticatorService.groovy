package org.bbop.apollo.authenticator

import grails.gorm.transactions.Transactional
import org.bbop.apollo.User
import org.bbop.apollo.security.ApolloSecurityUtils
import org.bbop.apollo.security.Sha256PasswordEncoder

import jakarta.servlet.http.HttpServletRequest

@Transactional
class UsernamePasswordAuthenticatorService implements AuthenticatorService {

    private static final Sha256PasswordEncoder passwordEncoder = new Sha256PasswordEncoder()

    @Override
    def authenticate(HttpServletRequest request) {
        log.error("Not implemented without a token")
        return false
    }

    def authenticate(String username, String password, HttpServletRequest request) {
        if (!username || !password) {
            return false
        }
        try {
            User user = User.findByUsername(username)
            if (!user) {
                log.error "User not found: ${username}"
                return false
            }
            if (!passwordEncoder.matches(password, user.passwordHash)) {
                log.error "Failed to authenticate user ${username}"
                return false
            }
            ApolloSecurityUtils.loginUser(username, user.roles*.name)
            return true
        } catch (Exception ae) {
            log.error(ae.message, ae)
            return false
        }
    }

    @Override
    Boolean requiresToken() {
        return true
    }
}
