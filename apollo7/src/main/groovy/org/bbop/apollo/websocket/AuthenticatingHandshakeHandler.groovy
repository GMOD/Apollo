package org.bbop.apollo.websocket

import grails.util.Holders
import org.apache.shiro.authc.UsernamePasswordToken
import org.springframework.http.server.ServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal

class AuthenticatingHandshakeHandler extends DefaultHandshakeHandler {
    def usernamePasswordAuthenticatorService = Holders.grailsApplication.mainContext.getBean('usernamePasswordAuthenticatorService')

    @Override
    Principal determineUser(
        ServerHttpRequest request,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        def query = request.getURI().getQuery()
        if (!query) {
            return null
        }
        def queryParams = query.split('&')
        def mapParams = queryParams.collectEntries { param ->
            param.split('=').collect { URLDecoder.decode(it, 'UTF-8') }
        }
        String username = mapParams["username"]
        String password = mapParams["password"]
        if (!(username && password)) {
            return null
        }
        UsernamePasswordToken authToken = new UsernamePasswordToken(username, password)
        if (usernamePasswordAuthenticatorService.authenticate(authToken, null)) {
            return request.getPrincipal()
        }
        return null
    }
}
