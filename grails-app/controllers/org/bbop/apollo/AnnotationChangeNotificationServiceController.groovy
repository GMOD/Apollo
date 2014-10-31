package org.bbop.apollo

import org.codehaus.groovy.grails.web.json.JSONObject
import grails.compiler.GrailsCompileStatic

import javax.servlet.AsyncContext
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

@GrailsCompileStatic
class AnnotationChangeNotificationServiceController {

    private static Map<String, Queue<AsyncContext>> queue = new ConcurrentHashMap<String, Queue<AsyncContext>>();
    private static Map<String, Queue<AsyncContext>> sessionToAsyncContext = new ConcurrentHashMap<String, Queue<AsyncContext>>();


    def index(String track) {
        println "AnnotationChangeNotificationService track: ${track}"
        response.setContentType("application/json");

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
                println "staring async"
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
        println "AnnotationChangeNotificationService output : ${track}"
        JSONObject jsonObject = new JSONObject()
        println "what what? "
        render jsonObject
    }

    private void sendError(HttpServletResponse response, String message, int errorCode) {
        try {
            response.sendError(errorCode, new JSONObject().put("error", message).toString());
        }
        catch (Exception e) {
            System.err.println("error handled: "+e)
        }
    }
}
