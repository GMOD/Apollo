package org.bbop.apollo.session

import org.apache.shiro.session.Session
import org.apache.shiro.session.SessionListener
import org.apache.shiro.session.mgt.DefaultSessionKey
import org.apache.shiro.session.mgt.DelegatingSession
import org.apache.shiro.session.mgt.ImmutableProxiedSession
import org.apache.shiro.session.mgt.SessionContext
import org.apache.shiro.session.mgt.SessionKey
import org.apache.shiro.web.session.mgt.ServletContainerSessionManager
import org.apache.shiro.web.session.mgt.WebSessionKey
import org.apache.shiro.web.util.WebUtils

import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

/**
 * Created by nathandunn on 8/4/15.
 */
class SessionListeningServletSessionManager extends ServletContainerSessionManager {

    private Collection<SessionListener> listeners;

    public SessionListeningServletSessionManager() {
//        this.listeners = new ArrayList<SessionListener>();
    }

    public void setSessionListeners(Collection<SessionListener> listeners) {
        this.listeners = listeners != null ? listeners : new ArrayList<SessionListener>();
    }

//    @SuppressWarnings({"UnusedDeclaration"})
    public Collection<SessionListener> getSessionListeners() {
        return this.listeners;
    }

    protected void notifyStart(Session session) {
        for (SessionListener listener : this.listeners) {
            listener.onStart(session);
        }
    }

    protected void notifyStop(Session session) {
        Session forNotification = beforeInvalidNotification(session);
        for (SessionListener listener : this.listeners) {
            listener.onStop(forNotification);
        }
    }

    protected void notifyExpiration(Session session) {
        Session forNotification = beforeInvalidNotification(session);
        for (SessionListener listener : this.listeners) {
            listener.onExpiration(forNotification);
        }
    }
    protected Session beforeInvalidNotification(Session session) {
        return new ImmutableProxiedSession(session);
    }

//    protected Session createExposedSession(Session session, SessionContext context) {
//        if (!WebUtils.isWeb(context)) {
//            return new DelegatingSession(this, new DefaultSessionKey(session.getId()));
////            return super.createExposedSession(session, context);
//        }
//        ServletRequest request = WebUtils.getRequest(context);
//        ServletResponse response = WebUtils.getResponse(context);
//        SessionKey key = new WebSessionKey(session.getId(), request, response);
//        return new HttpServletSession(httpSession, host);
//        return new DelegatingSession(this, key);
//    }

//    protected Session createExposedSession(Session session, SessionContext context) {
//        return new DelegatingSession(this, new DefaultSessionKey(session.getId()));
//    }
//
//    protected Session createExposedSession(Session session, SessionKey key) {
//        return new DelegatingSession(this, new DefaultSessionKey(session.getId()));
//    }

    @Override
    public Session start(SessionContext context) {
        Session session = createSession(context);
        onStart(session, context);
        notifyStart(session);
        //Don't expose the EIS-tier Session object to the client-tier:
//        return createExposedSession(session, context);
//        return createExposedSession(session, context);
        return createSession(context);
    }
    /**
     * Template method that allows subclasses to react to a new session being created.
     * <p/>
     * This method is invoked <em>before</em> any session listeners are notified.
     *
     * @param session the session that was just {@link #createSession created}.
     * @param context the {@link SessionContext SessionContext} that was used to start the session.
     */
    protected void onStart(Session session, SessionContext context) {
    }
}
