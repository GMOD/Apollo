package org.bbop.apollo.web;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.bbop.apollo.web.datastore.AbstractDataStoreManager;
import org.bbop.apollo.web.datastore.DataStoreChangeEvent;
import org.bbop.apollo.web.datastore.DataStoreChangeListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class AnnotationChangeNotificationService
 */
@WebServlet(name="/AnnotationChangeNotificationService", urlPatterns = {"/AnnotationChangeNotificationService"}, asyncSupported=true)
public class AnnotationChangeNotificationService extends HttpServlet implements DataStoreChangeListener, HttpSessionListener, ServletContextListener {
	private static final long serialVersionUID = 1L;
 
	private static Map<String, Queue<AsyncContext>> queue = new ConcurrentHashMap<String, Queue<AsyncContext>>();
	private static Map<String, Queue<AsyncContext>> sessionToAsyncContext = new ConcurrentHashMap<String, Queue<AsyncContext>>();
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AnnotationChangeNotificationService() {
        super();
    }
    
    @Override
    public void init(ServletConfig config) throws ServletException {
    	AbstractDataStoreManager.getInstance().addDataStoreChangeListener(this);
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected synchronized void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json");
		final String track = request.getParameter("track");
		final HttpSession session = request.getSession(false);
		if (session == null) {
			return;
		}
		final AsyncContext asyncContext = request.startAsync();
		if (sessionToAsyncContext.get(session.getId()) == null) {
			sessionToAsyncContext.put(session.getId(), new ConcurrentLinkedQueue<AsyncContext>());
		}
		sessionToAsyncContext.get(session.getId()).add(asyncContext);
		asyncContext.setTimeout(5 * 60 * 1000);
		asyncContext.addListener(new AsyncListener() {
			
			@Override
			public void onTimeout(AsyncEvent event) throws IOException {
				sendError((HttpServletResponse)event.getAsyncContext().getResponse(), "Connection timed out", HttpServletResponse.SC_GATEWAY_TIMEOUT);
				event.getAsyncContext().complete();
			}
			
			@Override
			public void onStartAsync(AsyncEvent event) throws IOException {
			}
			
			@Override
			public void onError(AsyncEvent event) throws IOException {
				sendError((HttpServletResponse)event.getAsyncContext().getResponse(), "Connection error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				event.getAsyncContext().complete();
			}
			
			@Override
			public void onComplete(AsyncEvent event) throws IOException {
				removeFromQueue();
			}
			
			private void removeFromQueue() {
				Queue<AsyncContext> contexts = queue.get(track);
				if (contexts != null) {
					contexts.remove(asyncContext);
				}
				if (session.getId() != null) {
					if (sessionToAsyncContext.get(session.getId()) != null) {
						sessionToAsyncContext.get(session.getId()).remove(asyncContext);
						if (sessionToAsyncContext.get(session.getId()).isEmpty()) {
							sessionToAsyncContext.remove(session.getId());
						}
					}
				}
			}
		});
		Queue<AsyncContext> contexts = queue.get(track);
		if (contexts == null) {
			contexts = new ConcurrentLinkedQueue<AsyncContext>();
			queue.put(track, contexts);
		}
		contexts.add(asyncContext);
	}
    
	@Override
	public synchronized void handleDataStoreChangeEvent(DataStoreChangeEvent ... events) {
		if (events.length == 0) {
			return;
		}
		Queue<AsyncContext> contexts = queue.get(events[0].getTrack());
		if (contexts == null) {
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
		for (AsyncContext asyncContext : contexts) {
			ServletResponse response = asyncContext.getResponse();
			try {
				response.getWriter().write(operations.toString());
			}
			catch (IOException e) {
			}
			asyncContext.complete();
		}
	}

	@Override
	public void sessionCreated(HttpSessionEvent event) {
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		if (sessionToAsyncContext.get(event.getSession().getId()) == null) {
			return;
		}
		for (AsyncContext asyncContext : sessionToAsyncContext.get(event.getSession().getId())) {
			HttpServletResponse response = null;
			try {
				response = (HttpServletResponse)asyncContext.getResponse();
			}
			catch (IllegalStateException e) {
				return;
			}
			sendError(response, "Session timed out - you must relogin", HttpServletResponse.SC_FORBIDDEN);
			asyncContext.complete();
		}
	}
	
	private void sendError(HttpServletResponse response, String message, int errorCode) {
		try {
			response.sendError(errorCode, new JSONObject().put("error", message).toString());
		}
		catch (Exception e) {
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		for (Queue<AsyncContext> contexts : queue.values()) {
			for (AsyncContext context : contexts) {
				sendError((HttpServletResponse)context.getResponse(), "Server shutdown", HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				context.complete();
			}
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
