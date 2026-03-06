package org.bbop.apollo.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

// BACKWARDS INCOMPATIBILITY: AbstractWebSocketMessageBrokerConfigurer was removed
// in Spring 6. Now implements WebSocketMessageBrokerConfigurer directly.
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ApolloHandshakeHandler apolloHandshakeHandler = new ApolloHandshakeHandler()

    @Override
    void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    @Override
    void registerStompEndpoints(StompEndpointRegistry registry) {
        // BACKWARDS INCOMPATIBILITY: setAllowedOrigins("*") no longer works
        // with credentials in Spring 6. Use setAllowedOriginPatterns instead.
        registry.addEndpoint("/stomp")
            .setAllowedOriginPatterns("*")
            .setHandshakeHandler(apolloHandshakeHandler)
            .withSockJS()
    }
}
