package org.bbop.apollo.web.sockets;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.bbop.apollo.web.datastore.DataStoreChangeEvent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AnnotationEditorSessionManager {

	private static AnnotationEditorSessionManager instance;
	
	private Map<String, List<Session>> sessionsById = new ConcurrentHashMap<String, List<Session>>();
	private Map<String, List<Session>> sessionsByRefSeq = new ConcurrentHashMap<String, List<Session>>();
	
	public static AnnotationEditorSessionManager getInstance() {
		if (instance == null) {
			instance = new AnnotationEditorSessionManager();
		}
		return instance;
	}
	
	private AnnotationEditorSessionManager() {
	}
	
	public void addSession(Session session) {
		String sessionId = ((HttpSession)session.getUserProperties().get("http_session")).getId();
		String refSeq = (String)session.getUserProperties().get("refSeq");
		addSessionById(sessionId, session);
		addSessionByRefSeq(refSeq, session);
	}
	
	private void addSessionById(String sessionId, Session session) {
		List<Session> sessions = sessionsById.get(sessionId);
		if (sessions == null) {
			sessions = new CopyOnWriteArrayList<Session>();
			sessionsById.put(sessionId, sessions);
		}
		sessions.add(session);
	}

	private void addSessionByRefSeq(String refSeq, Session session) {
		List<Session> sessions = sessionsByRefSeq.get(refSeq);
		if (sessions == null) {
			sessions = new CopyOnWriteArrayList<Session>();
			sessionsByRefSeq.put(refSeq, sessions);
		}
		sessions.add(session);
	}

	public void removeSession(Session session) {
		String sessionId = ((HttpSession)session.getUserProperties().get("http_session")).getId();
		String refSeq = (String)session.getUserProperties().get("refSeq");
		removeSessionById(sessionId, session);
		removeSessionByRefSeq(refSeq, session);
	}
	
	private void removeSessionById(String sessionId, Session session) {
		if (sessionId != null) {
			List<Session> sessions = sessionsById.get(sessionId);
			if (sessions != null) {
				sessions.remove(session);
				if (sessions.isEmpty()) {
					sessionsById.remove(sessionId);
				}
			}
		}
	}
	
	private void removeSessionByRefSeq(String refSeq, Session session) {
		if (refSeq != null) {
			List<Session> sessions = sessionsByRefSeq.get(refSeq);
			if (sessions != null) {
				sessions.remove(session);
				if (sessions.isEmpty()) {
					sessionsByRefSeq.remove(refSeq);
				}
			}
		}
	}

	public void closeSession(String sessionId, String reason) throws IOException {
		List<Session> sessions = sessionsById.get(sessionId);
		if (sessions != null) {
			for (Session session : sessions) {
				session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Logged out"));
			}
			sessionsById.remove(sessionId);
		}
	}
	
	public void fireDataStoreChange(DataStoreChangeEvent ... events) {
		if (events.length == 0) {
			return;
		}
		List<Session> sessions = sessionsByRefSeq.get(events[0].getTrack());
		if (sessions == null) {
			return;
		}
		JSONArray operations = new JSONArray();
		for (DataStoreChangeEvent event : events) {
			JSONObject features = event.getFeatures();
			try {
				features.put("operation", event.getOperation().name());
				features.put("sequenceAlterationEvent", event.isSequenceAlterationEvent());
				operations.put(features);
			}
			catch (JSONException e) {
			}
		}
		for (Session session : sessions) {
			if (session.isOpen()) {
				try {
					session.getBasicRemote().sendText(operations.toString());
				}
				catch (IOException e) {
				}
			}
		}
	}

	public void sessionCreated(HttpSessionEvent event) {
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		try {
			closeSession(event.getSession().getId(), "Logged out");
		}
		catch (IOException e) {
		}
	}

	
}
