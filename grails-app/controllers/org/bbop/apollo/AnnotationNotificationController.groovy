package org.bbop.apollo

import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.event.AnnotationListener
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

import javax.servlet.AsyncContext
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/*nd*
 * Based on AnnotationChangeNotificationService
 */
//@GrailsCompileStatic
class AnnotationNotificationController implements AnnotationListener {

    DataListenerHandler dataListenerHandler = DataListenerHandler.getInstance()
    Map<String, Queue<AsyncContext>> queue = new ConcurrentHashMap<String, Queue<AsyncContext>>();
//    Map<String, Queue<AsyncContext>> sessionToAsyncContext = new ConcurrentHashMap<String, Queue<AsyncContext>>();


    @MessageMapping("/hello")
    @SendTo("/topic/hello")
    protected String hello(String world) {
        println "got here! . . . "
        return "hello from controller . . . whadup?, ${world}!"
    }

    @MessageMapping("/AnnotationNotification")
    @SendTo("/topic/AnnotationNotification")
    protected String annotationEditor(String inputString) {
        println " annotation editor service ${inputString}"
        return "returning annotationEditor ${inputString}!"
    }


//    def index(String track) {
//
//        println "AnnotationChangeNotificationService track: ${track}"
//        dataListenerHandler.addDataStoreChangeListener(this)
//        println "num of listeners: ${dataListenerHandler.getListeners().size()}"
//        response.setContentType("application/json");
//
//        final HttpSession session = request.getSession(false);
//        if (session == null) {
//            return;
//        }
//        final AsyncContext asyncContext = request.startAsync();
//        if (sessionToAsyncContext.get(session.getId()) == null) {
//            sessionToAsyncContext.put(session.getId(), new ConcurrentLinkedQueue<AsyncContext>());
//        }
//        sessionToAsyncContext.get(session.getId()).add(asyncContext);
//        asyncContext.setTimeout(5 * 60 * 1000);
//        asyncContext.addListener(new AsyncListener() {
//
//            @Override
//            public void onTimeout(AsyncEvent event) throws IOException {
//                sendError((HttpServletResponse) event.getAsyncContext().getResponse(), "Connection timed out", HttpServletResponse.SC_GATEWAY_TIMEOUT);
//                event.getAsyncContext().complete();
//            }
//
//            @Override
//            public void onStartAsync(AsyncEvent event) throws IOException {
//                println "staring async"
//            }
//
//            @Override
//            public void onError(AsyncEvent event) throws IOException {
//                sendError((HttpServletResponse) event.getAsyncContext().getResponse(), "Connection error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                event.getAsyncContext().complete();
//            }
//
//            @Override
//            public void onComplete(AsyncEvent event) throws IOException {
//                removeFromQueue();
//            }
//
//            private void removeFromQueue() {
//                Queue<AsyncContext> contexts = queue.get(track);
//                if (contexts != null) {
//                    contexts.remove(asyncContext);
//                }
//                if (session.getId() != null) {
//                    if (sessionToAsyncContext.get(session.getId()) != null) {
//                        sessionToAsyncContext.get(session.getId()).remove(asyncContext);
//                        if (sessionToAsyncContext.get(session.getId()).isEmpty()) {
//                            sessionToAsyncContext.remove(session.getId());
//                        }
//                    }
//                }
//            }
//        });
//        Queue<AsyncContext> contexts = queue.get(track);
//        if (contexts == null) {
//            contexts = new ConcurrentLinkedQueue<AsyncContext>();
//            queue.put(track, contexts);
//        }
//        contexts.add(asyncContext);
//        println "AnnotationChangeNotificationService output : ${track}"
//        JSONObject jsonObject = new JSONObject()
//        println "what what? "
//        render jsonObject
//    }

    private void sendError(HttpServletResponse response, String message, int errorCode) {
        try {
            response.sendError(errorCode, new JSONObject().put("error", message).toString());
        }
        catch (Exception e) {
            System.err.println("error handled: " + e)
        }
    }

    synchronized void handleChangeEvent(AnnotationEvent... events) {
        if (events.length == 0) {
            return;
        }
        // TODO: this is more than a bit of a hack
        String sequenceName = "Annotations-${events[0].sequence.name}"
        Queue<AsyncContext> contexts = queue.get(sequenceName);
//        Queue<AsyncContext> contexts = queue.get(events[0].getSequence());
        if (contexts == null) {
            return;
        }
        JSONArray operations = new JSONArray();
        for (AnnotationEvent event : events) {
            JSONObject features = event.getFeatures();
            try {
                features.put("operation", event.getOperation().name());
                features.put("sequenceAlterationEvent", event.isSequenceAlterationEvent());
                operations.put(features);
            }
            catch (JSONException e) {
                log.error("error handling change event ${event}: ${e}")
            }
        }
//        ??
        for (AsyncContext asyncContext : contexts) {
            ServletResponse response = asyncContext.getResponse();
            try {
                response.getWriter().write(operations.toString());
                response.flushBuffer();
            }
            catch (IOException e) {
                log.error(e)
            }
//            asyncContext.complete();
        }

    }
}
