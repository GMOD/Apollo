package org.bbop.apollo

import grails.converters.JSON
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.util.StringUtil

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
        JSONObject dataObject = JSON.parse(params.data)
        User user = new User(
                firstName: dataObject.firstName
                ,lastName: dataObject.lastName
                ,username: dataObject.email
                ,passwordHash: RandomStringUtils.random(20)
        )
        user.save(flush: true,insert:true)
    }

    def deleteUser(){
        println "deleting user ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        User user = User.findById(dataObject.userId)
        user.userGroups.clear()
        UserTrackPermission.deleteAll(UserTrackPermission.findAllByUser(user))
        UserOrganismPermission.deleteAll(UserOrganismPermission.findAllByUser(user))
        user.delete(flush: true)
    }

    def updateUser(){
        JSONObject dataObject = JSON.parse(params.data)
        User user = User.findById(dataObject.userId)
        user.firstName = dataObject.firstName
        user.lastName = dataObject.lastName
        user.username = dataObject.email
        user.save(flush: true)
    }
}
