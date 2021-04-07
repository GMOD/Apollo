import com.brandseye.cors.CorsFilter
import grails.plugin.springwebsocket.GrailsSimpAnnotationMethodMessageHandler

// Place your Spring DSL code here
beans = {
    corsFilter(CorsFilter)

    webSocketConfig org.bbop.apollo.websocket.WebSocketConfig

    grailsSimpAnnotationMethodMessageHandler(
        GrailsSimpAnnotationMethodMessageHandler,
        ref("clientInboundChannel"),
        ref("clientOutboundChannel"),
        ref("brokerMessagingTemplate")
    ) {
        destinationPrefixes = ["/app"]
    }
}
