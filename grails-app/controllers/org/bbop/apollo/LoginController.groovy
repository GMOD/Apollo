package org.bbop.apollo

import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.session.Session
import org.apache.shiro.subject.Subject
import org.apache.shiro.web.util.SavedRequest
import org.apache.shiro.web.util.WebUtils
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

import javax.servlet.http.HttpServletResponse

class LoginController extends AbstractApolloController {

    def permissionService

    def index() {}

    def handleOperation(String track, String operation) {
        // TODO: this is a hack, but it should come through the UrlMapper

        println "request stuff ${request.parameterMap.keySet()}"
        println "upstream params ${params}"
        JSONObject postObject = findPost()
        println "postObject ${postObject as JSON}"
        if(postObject?.containsKey(REST_OPERATION)){
            operation = postObject.get(REST_OPERATION)
        }
        if(postObject?.containsKey(REST_TRACK)){
            track = postObject.get(REST_TRACK)
        }
        log.info "updated operation: ${operation}"
        if(!operation){
            forward action: "doLogin"
            return
        }
        def mappedAction = underscoreToCamelCase(operation)
        log.debug "${operation} -> ${mappedAction}"
        forward action: "${mappedAction}",  params: [data: postObject]
    }


    def doLogin(){
        log.debug "creating login popup"
    }

    private void sendError(HttpServletResponse response, Exception e) throws IOException {
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("error", e.getMessage()).toString());
        }
        catch (JSONException e2) {
            log.error("Error sending error: ${e2}")
        }
    }
    

    /**
     * Merging Login.java (old) and  AuthController.groovy
     * @return
     */
    def login(){
        println "doing the login ${params}"
        def jsonObj = request.JSON
        if(!jsonObj){
            jsonObj = JSON.parse(params.data)
            println "jsonObj ${jsonObj}"
        }
        println "login -> the jsonObj ${jsonObj}"
        String username = jsonObj.username
        String password = jsonObj.password
        Boolean rememberMe = jsonObj.rememberMe
        
        def authToken = new UsernamePasswordToken(username, password as String)

        // Support for "remember me"
        if (rememberMe) {
            authToken.rememberMe = true
        }
        println "remembmerMe: ${rememberMe}"
        println "authToken : ${authToken.rememberMe}"

        // If a controller redirected to this page, redirect back
        // to it. Otherwise redirect to the root URI.
        def targetUri = params.targetUri ?: "/"

        // Handle requests saved by Shiro filters.
        SavedRequest savedRequest = WebUtils.getSavedRequest(request)
        if (savedRequest) {
            targetUri = savedRequest.requestURI - request.contextPath
            if (savedRequest.queryString) targetUri = targetUri + '?' + savedRequest.queryString
        }

        try{
            // Perform the actual login. An AuthenticationException
            // will be thrown if the username is unrecognised or the
            // password is incorrect.
            Subject subject = SecurityUtils.getSubject();
            Session session = subject.getSession(true);
            subject.login(authToken)
            println "IS AUTHENTICATED: " + subject.isAuthenticated()
            println "has a session ${session}"
            println "LOGIN SESSIN ${SecurityUtils.subject.getSession(false).id}"

//            HttpSession session = request.getSession();
            session.setAttribute("username", username);
            session.setAttribute("permissions", new HashMap<String, Integer>());

            // should this be <context>/annotator/index?
//            log.info "Redirecting to '${targetUri}'."
//            redirect(uri: targetUri)
//            redirect(uri: "/annotator/index")
//            response.sendRedirect("/apollo/annotator/index")
            
            User user = User.findByUsername(username)


//            int permission = 0
//            // TODO: should be per organism
            Map<String, Integer> permissions = permissionService.getPermissionsForUser(user)
            if(permissions){
                session.setAttribute("permissions", permissions);
            }
//            if (permissions.values().size() > 0) {
//                permission = permissions.values().iterator().next();
//            }
            JSONObject responseJSON = new JSONObject();
//            responseJSON.put("url", "/apollo/annotator/index").put("sessionId", session.getId());
            render responseJSON as JSON
//            response.getWriter().write(responseJSON.toString());
        }
        catch (AuthenticationException ex){
            // Authentication failed, so display the appropriate message
            // on the login page.
//            log.info "Authentication failure for user '${params.username}'."
//            flash.message = message(code: "login.failed")
            

            // Keep the username and "remember me" setting so that the
            // user doesn't have to enter them again.
            def m = [ username: jsonObj.username ]
            if (jsonObj.rememberMe) {
                m["rememberMe"] = true
            }

            // Remember the target URI too.
            if (jsonObj.targetUri) {
                m["targetUri"] = jsonObj.targetUri
            }

            // Now redirect back to the login page.
//            redirect(action: "login", params: m)
            sendError(response,ex)
        }
    }



    def logout(){
        println "doing the logOUT"
//        if (request.getSession(false)) {
//            try {
//                request.getSession(false).invalidate();
//            } catch (e) {
//                log.error "error invalidating session ${e}"
//            }
//        }
        SecurityUtils.subject.logout()
        println "LOGOUT SESSIN ${SecurityUtils.subject.getSession(false).id}"

//        webRequest.getCurrentRequest().session = null
        response.status = HttpServletResponse.SC_OK
        render {result:"OK"} as JSON
    }
    
}
