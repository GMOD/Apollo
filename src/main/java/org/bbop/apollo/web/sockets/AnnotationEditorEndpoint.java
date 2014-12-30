package org.bbop.apollo.web.sockets;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.bbop.apollo.web.AnnotationEditorServiceManager;
import org.json.JSONObject;

@ServerEndpoint(
		value = "/AnnotationEditor/{refSeq}",
		configurator = AnnotationEditorConfigurator.class
)
public class AnnotationEditorEndpoint {

	private HttpSession httpSession;
	
	@OnOpen
	public void onOpen(Session session, EndpointConfig config, @PathParam("refSeq") String refSeq) throws IOException {
		httpSession = (HttpSession)session.getUserProperties().get("http_session");
		if (httpSession == null || httpSession.getAttribute("username") == null) {
			String reason = "You must first login before editing";
			throw new AnnotationEditorEndpointException(reason);
		}
		session.getUserProperties().put("refSeq", refSeq);
		AnnotationEditorSessionManager.getInstance().addSession(session);
	}
	
	@OnClose
	public void onClose(Session session, CloseReason reason, @PathParam("refSeq") String refSeq) throws IOException {
		if (httpSession != null) {
			AnnotationEditorSessionManager.getInstance().removeSession(session);
		}
	}
	
	@OnError
	public void onError(Session session, Throwable t) throws IOException {
		if (session.isOpen() && t instanceof AnnotationEditorEndpointException) {
			session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, t.getMessage()));
		}
	}
	
	@OnMessage
	public void onMessage(Session session, String msg) throws Exception {
		try {
			StringWriter out = new StringWriter();
			JSONObject inJson = new JSONObject(msg);
			AnnotationEditorServiceManager.getInstance().processRequest(inJson, new BufferedWriter(out), httpSession);
		}
		catch (Exception e) {
			e.printStackTrace();
			session.getBasicRemote().sendText(new JSONObject().put("error", e.getMessage()).toString());
		}
		/*
		JSONObject outJson = new JSONObject(out.toString());
		if (outJson.has("confirm")) {
			outJson.put("track", inJson.get("track"));
			outJson.put("features", inJson.get("features"));
			outJson.put("operation", inJson.get("operation"));
		}
		session.getBasicRemote().sendText(outJson.toString());
		*/
	}

	public static class AnnotationEditorEndpointException extends RuntimeException {
		
		private static final long serialVersionUID = 1L;

		public AnnotationEditorEndpointException(String message) {
			super(message);
		}
		
	}
}
