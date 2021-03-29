package org.bbop.apollo.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

    @Override
    void configureMessageBroker(MessageBrokerRegistry mbr) {
        mbr.enableSimpleBroker "/queue", "/topic"
        mbr.setApplicationDestinationPrefixes "/app"
        mbr.setUserDestinationPrefix "/user/"
    }

    @Override
    void registerStompEndpoints(StompEndpointRegistry ser) {
        ser.addEndpoint("/stomp")
            .setAllowedOrigins("*")
            .setHandshakeHandler(handshakeHandler())
            .withSockJS()
    }

    ApolloHandshakeHandler handshakeHandler() {
        return new ApolloHandshakeHandler();
    }

}
