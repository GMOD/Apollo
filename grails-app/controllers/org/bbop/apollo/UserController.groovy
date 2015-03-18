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
    
    def createUser(){
        println "creating user ${request.JSON} -> ${params}"
    }

    def deleteUser(){
        println "deleting user ${request.JSON} -> ${params}"
    }

    def updateUser(){
        println "updating user ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        println "firstName -> ${params.firstName}"
        User user = User.findById(params.userId)
        user.firstName = dataObject.firstName
        user.lastName = dataObject.lastName
        user.username = dataObject.email
        user.save(flush: true)
    }
}
