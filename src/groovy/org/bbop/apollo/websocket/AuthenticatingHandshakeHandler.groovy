package org.bbop.apollo.websocket


import grails.util.Holders
import org.apache.shiro.SecurityUtils

import javax.security.auth.Subject
import java.security.Principal

import org.apache.shiro.authc.UsernamePasswordToken
import org.bbop.apollo.websocket.StompPrincipal
import org.springframework.http.server.ServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler

class AuthenticatingHandshakeHandler extends DefaultHandshakeHandler {
    def usernamePasswordAuthenticatorService = Holders.grailsApplication.mainContext.getBean('usernamePasswordAuthenticatorService')

    @Override
    Principal determineUser(
        ServerHttpRequest request,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        // https://stackoverflow.com/a/41584987/10750707
        def query = request.getURI().getQuery()
        def queryParams = query.split('&')
        def mapParams = queryParams.collectEntries { param -> param.split('=').collect { URLDecoder.decode(it) }}
        String username = mapParams["username"]
        String password = mapParams["password"]
        if (!(username && password)) {
            return null
        }
        UsernamePasswordToken authToken = new UsernamePasswordToken(username, password)
        if(usernamePasswordAuthenticatorService.authenticate(authToken,null)){
            return request.getPrincipal()
        }
        return null
    }

}
