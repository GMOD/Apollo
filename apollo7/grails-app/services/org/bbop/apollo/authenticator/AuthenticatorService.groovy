package org.bbop.apollo.authenticator

import jakarta.servlet.http.HttpServletRequest

interface AuthenticatorService {

    def authenticate(HttpServletRequest request)
    def authenticate(String username, String password, HttpServletRequest request)
    Boolean requiresToken()
}
