import org.bbop.apollo.websocket.WebSocketConfig

// BACKWARDS INCOMPATIBILITY: CorsFilter bean removed.
// CORS is now handled via Spring Boot's WebMvcConfigurer in Application.groovy.
// The old com.brandseye.cors.CorsFilter is no longer needed.

// BACKWARDS INCOMPATIBILITY: GrailsSimpAnnotationMethodMessageHandler bean removed.
// WebSocket message handling is now configured via WebSocketConfig using
// Spring's standard @EnableWebSocketMessageBroker annotation.

beans = {
    webSocketConfig(WebSocketConfig)
}
