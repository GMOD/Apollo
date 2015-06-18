package org.bbop.apollo

import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.IncorrectCredentialsException
import org.apache.shiro.authc.UnknownAccountException
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.crypto.hash.Sha256Hash
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
        log.debug "request stuff ${request.parameterMap.keySet()}"
        log.debug "upstream params ${params}"
        JSONObject postObject = findPost()
        log.debug "postObject ${postObject as JSON}"
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
        log.debug "doing the register ${params}"
        def jsonObj = request.JSON
        if(!jsonObj){
            jsonObj = JSON.parse(params.data)
            log.debug "jsonObj ${jsonObj}"
        }
        log.debug "register -> the jsonObj ${jsonObj}"
        String username = jsonObj.username
        String password = jsonObj.password
        Boolean rememberMe = jsonObj.rememberMe

        def adminRole = Role.findByName(UserService.ADMIN)

        User user = new User(
                username: username
                ,passwordHash: new Sha256Hash(password).toHex()
                ,firstName: jsonObj.firstName
                ,lastName: jsonObj.lastName
        ).save()
        user.addToRoles(adminRole)
        return login()
    }

    /**
     * Merging Login.java (old) and  AuthController.groovy
     * @return
     */
    def login(){
        log.debug "doing the login ${params}"
        def jsonObj = request.JSON
        if(!jsonObj){
            jsonObj = JSON.parse(params.data)
            log.debug "jsonObj ${jsonObj}"
        }
        log.debug "login -> the jsonObj ${jsonObj}"
        String username = jsonObj.username
        String password = jsonObj.password
        Boolean rememberMe = jsonObj.rememberMe

        def authToken = new UsernamePasswordToken(username, password as String)

        // Support for "remember me"
        if (rememberMe) {
            authToken.rememberMe = true
        }
        log.debug "rememberMe: ${rememberMe}"
        log.debug "authToken : ${authToken.rememberMe}"

        // If a controller redirected to this page, redirect back
        // to it. Otherwise redirect to the root URI.
        def targetUri = params.targetUri ?: "/"

        // Handle requests saved by Shiro filters.
        SavedRequest savedRequest = WebUtils.getSavedRequest(request)
        if (savedRequest) {
            targetUri = savedRequest.requestURI - request.contextPath
            if (savedRequest.queryString) targetUri = targetUri + '?' + savedRequest.queryString
        }

        try {

            // Perform the actual login. An AuthenticationException
            // will be thrown if the username is unrecognised or the
            // password is incorrect.
            Subject subject = SecurityUtils.getSubject();
            Session session = subject.getSession(true);
            subject.login(authToken)
            log.debug "IS AUTHENTICATED: " + subject.isAuthenticated()
            log.debug "SESSION ${session}"
            log.debug "LOGIN SESSION ${SecurityUtils.subject.getSession(false).id}"

            session.setAttribute("username", username);
            session.setAttribute("permissions", new HashMap<String, Integer>());

            User user = User.findByUsername(username)


            Map<String, Integer> permissions = permissionService.getPermissionsForUser(user)
            if(permissions){
                session.setAttribute("permissions", permissions);
            }
            JSONObject responseJSON = new JSONObject();
            render responseJSON as JSON
        } catch(IncorrectCredentialsException ex) {
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
            m.error="Incorrect login"
            // Now redirect back to the login page.
            //redirect(action: "login", params: m)
            render m as JSON
        } catch(UnknownAccountException ex) {

            def m = [ username: jsonObj.username ]
            if (jsonObj.rememberMe) {
                m["rememberMe"] = true
            }

            // Remember the target URI too.
            if (jsonObj.targetUri) {
                m["targetUri"] = jsonObj.targetUri
            }
            m.error="Unknown account"
            render m as JSON

        } catch ( AuthenticationException ae ) {

            def m = [ username: jsonObj.username ]
            if (jsonObj.rememberMe) {
                m["rememberMe"] = true
            }

            // Remember the target URI too.
            if (jsonObj.targetUri) {
                m["targetUri"] = jsonObj.targetUri
            }
            m.error="Unknown authentication erro"
            render m as JSON
            //unexpected condition - error?
        }
    }



    def logout(){
        log.debug "LOGOUT SESSION ${SecurityUtils?.subject?.getSession(false)?.id}"
        SecurityUtils.subject.logout()
        render new JSONObject() as JSON
    }
    
}
