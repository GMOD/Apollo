package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject


class LoginController extends AbstractApolloController {



    def index() {}

    def handleOperation(String track, String operation) {
        // TODO: this is a hack, but it should come through the UrlMapper

        println "request stuff ${request.parameterMap.keySet()}"
        println "upstream params ${params}"
        JSONObject postObject = findPost()
        println "postObject ${postObject as JSON}"
        if(postObject.containsKey(REST_OPERATION)){
            operation = postObject.get(REST_OPERATION)
        }
        if(postObject.containsKey(REST_TRACK)){
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

    def login(){
        println "doing the login ${params}"
        def jsonObj = request.JSON
        println "login -> the jsonObj ${jsonObj}"
    }



    def logout(){
        println "doing the login"

    }
    
    protected  def findGet(){
        println "params for login ${params}"
        
    }
}
