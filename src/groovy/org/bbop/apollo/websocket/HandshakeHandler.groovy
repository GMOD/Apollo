package org.bbop.apollo.websocket


import java.security.Principal

import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeFailureException
import org.springframework.web.socket.server.HandshakeHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler

public class HandshakeHandler implements HandshakeHandler {

    public final boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response,
        WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {
        DefaultHandshakeHandler defaultHandshakeHandler = new DefaultHandshakeHandler()
        Principal user = defaultHandshakeHandler.determineUser(request, wsHandler, attributes)
        if (user == null) {
            response.setStatusCode(HttpStatus.FORBIDDEN)
            return false
        }
        return defaultHandshakeHandler.doHandshake(request, response, wsHandler, attributes)
    }

}
