package org.bbop.apollo.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

    private final def apolloHandshakeHandler = new ApolloHandshakeHandler()

    @Override
    void registerStompEndpoints(StompEndpointRegistry ser) {
        ser.addEndpoint("/stomp")
            .setAllowedOrigins("*")
            .setHandshakeHandler(apolloHandshakeHandler)
            .withSockJS()
    }

}
