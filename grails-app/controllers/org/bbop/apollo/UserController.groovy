package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class UserController {

    def index() {}
    
    def loadUsers(){
        render User.all as JSON
    }
}
