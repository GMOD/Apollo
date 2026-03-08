package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.security.ApolloSecurityUtils
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.grails.web.json.JSONException
import org.grails.web.json.JSONObject
import org.springframework.security.core.AuthenticationException

import org.springframework.messaging.simp.SimpMessagingTemplate

import jakarta.servlet.http.HttpServletResponse

class LoginController extends AbstractApolloController {

    UserService userService
    SimpMessagingTemplate brokerMessagingTemplate

    def index() {}


    def handleOperation(String operation) {
        JSONObject postObject = findPost()
        if(postObject?.containsKey(REST_OPERATION)){
            operation = postObject.get(REST_OPERATION)
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
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(new JSONObject().put("error", e.getMessage()).toString());
        }
        catch (JSONException e2) {
            log.error("Error sending error: ${e2}")
        }
    }

    def registerAdmin(){
        if(User.count > 0 && !permissionService.isAdmin()){
            log.error "Can only register admins if no users ${User.count} or user is admin"
            throw new AnnotationException("Can only register admins if no users ${User.count} or user is admin")
        }
        def jsonObj = request.JSON
        if(!jsonObj){
            jsonObj = JSON.parse(params.data)
            log.debug "jsonObj ${jsonObj}"
        }

        log.debug "register -> the jsonObj ${jsonObj}"
        userService.registerAdmin(jsonObj)

        // Authenticate the newly registered admin directly
        String username = jsonObj.username
        String password = jsonObj.password
        if(!permissionService.authenticateWithToken(username, password, request)){
            throw new AnnotationException("Bad credentials for user ${username}")
        }
        def session = ApolloSecurityUtils.getSession(true)
        session.setAttribute("username", username)
        session.setAttribute("permissions", new HashMap<String, Integer>())
        User user = User.findByUsername(username)
        Map<String, Integer> permissions = permissionService.getPermissionsForUser(user)
        if(permissions){
            session.setAttribute("permissions", permissions)
        }
        render new JSONObject() as JSON
    }

    def login(){
        def jsonObj
        try {
            jsonObj = request.JSON
            if(!jsonObj){
                jsonObj = JSON.parse(params.data)
                log.debug "jsonObj ${jsonObj}"
            }
            log.debug "login -> the jsonObj ${jsonObj}"
            String username = jsonObj.username
            String password = jsonObj.password

            def targetUri = params.targetUri ?: "/"

            if(!permissionService.authenticateWithToken(username, password, request)){
                throw new AnnotationException("Bad credentials for user ${username}")
            }

            log.debug "IS AUTHENTICATED: " + ApolloSecurityUtils.isAuthenticated()
            def session = ApolloSecurityUtils.getSession(true)
            log.debug "SESSION ${session}"
            log.debug "LOGIN SESSION ${session?.id}"

            session.setAttribute("username", username);
            session.setAttribute("permissions", new HashMap<String, Integer>());

            User user = User.findByUsername(username)

            Map<String, Integer> permissions = permissionService.getPermissionsForUser(user)
            if(permissions){
                session.setAttribute("permissions", permissions);
            }

            if(targetUri.length()>2){
                redirect(uri: targetUri)
            }
            else{
                render new JSONObject() as JSON
            }
        } catch(AuthenticationException ex) {
            def m = [ username: jsonObj.username ]
            if (jsonObj.rememberMe) {
                m["rememberMe"] = true
            }
            if (jsonObj.targetUri) {
                m["targetUri"] = jsonObj.targetUri
            }
            m.error="Incorrect login"
            render m as JSON
        } catch ( Exception e ) {
            def error=[error: e.message]
            render error as JSON
        }
    }


    def logout(){
        log.debug "logging out with params: ${params}"
        String username = ApolloSecurityUtils.currentUsername ?: params.username
        log.debug "sending logout for username ${username}"
        sendLogout(username, params.get(FeatureStringEnum.CLIENT_TOKEN.value).toString())
        log.debug "sent logout"
        sleep(1000)
        log.debug "doing local logout"
        ApolloSecurityUtils.logout()
        sleep(1000)
        log.debug "logged out"
        if(params.targetUri){
            redirect(uri:"/auth/login?targetUri=${params.targetUri}")
        }
        else{
            render new JSONObject() as JSON
        }
    }

    def sendLogout(String username,String clientToken) {
        User user = User.findByUsername(username)
        log.debug "sending logout for ${user} via ${username} with ${clientToken}"
        JSONObject jsonObject = new JSONObject()
        if(!user){
            log.error("Already logged out or user not found: ${username}")
            return jsonObject.toString()
        }
        jsonObject.put(FeatureStringEnum.USERNAME.value,username)
        jsonObject.put(FeatureStringEnum.CLIENT_TOKEN.value,clientToken)
        jsonObject.put(REST_OPERATION,"logout")
        log.debug "sending to: '/topic/AnnotationNotification/user/' + ${user.username}"
        try {
            brokerMessagingTemplate.convertAndSend "/topic/AnnotationNotification/user/" + username, jsonObject.toString()
        } catch (e) {
            log.error("working?: "+e)
        }
        return jsonObject.toString()
    }
}
