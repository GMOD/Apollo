package org.bbop.apollo.web.sockets;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class AnnotationNotificationConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig conf,
                                HandshakeRequest req,
                                HandshakeResponse resp) {
    	Object session = req.getHttpSession();
    	if (session != null) {
    		conf.getUserProperties().put("http_session", req.getHttpSession());
    	}
    }

}