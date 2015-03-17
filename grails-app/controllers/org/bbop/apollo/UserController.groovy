package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class UserController {
    
    def permissionService

    def index() {}
    
    def loadUsers(){
        render User.all as JSON
    }

    def checkLogin(){
        if(permissionService.currentUser){
            render permissionService.currentUser as JSON
        }
        else{
            render new JSONObject() as JSON
        }
    }
}
