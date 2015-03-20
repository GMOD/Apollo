package org.bbop.apollo

import grails.converters.JSON
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import org.apache.shiro.crypto.hash.Sha256Hash
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.util.StringUtil

class UserController {

    def permissionService
    def userService

    def index() {}

    def loadUsers() {
        JSONArray returnArray = new JSONArray()
        
        User.all.each{
            def userObject = new JSONObject()
//            userObject.putAll(it.properties)
            userObject.userId = it.id
            userObject.username = it.username
            userObject.firstName = it.firstName
            userObject.lastName = it.lastName
            Role role = userService.getHighestRole(it)
            userObject.role = role?.name

            returnArray.put(userObject)
        }
        
        render returnArray as JSON
    }

    def checkLogin() {
        if (permissionService.currentUser) {
            def it = permissionService.currentUser
            def userObject = new JSONObject()
//            userObject.putAll(it.properties)
            userObject.userId = it.id
            userObject.username = it.username
            userObject.firstName = it.firstName
            userObject.lastName = it.lastName
            Role role = userService.getHighestRole(it)
            userObject.role = role?.name
            render userObject as JSON
        } else {
            render new JSONObject() as JSON
        }
    }

    def createUser() {
        println "creating user ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        User user = new User(
                firstName: dataObject.firstName
                , lastName: dataObject.lastName
                , username: dataObject.email
                 ,passwordHash : new Sha256Hash(dataObject.password).toHex()
        )
        user.save(flush: true, insert: true)
        render new JSONObject() as JSON
    }

    def deleteUser() {
        println "deleting user ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        User user = User.findById(dataObject.userId)
        user.userGroups.each { it ->
            it.removeFromUsers(user)
        }
        UserTrackPermission.deleteAll(UserTrackPermission.findAllByUser(user))
        UserOrganismPermission.deleteAll(UserOrganismPermission.findAllByUser(user))
        user.delete(flush: true)
    }

    def updateUser() {
        JSONObject dataObject = JSON.parse(params.data)
        User user = User.findById(dataObject.userId)
        user.firstName = dataObject.firstName
        user.lastName = dataObject.lastName
        user.username = dataObject.email

        if(dataObject.password){
            user.passwordHash = new Sha256Hash(dataObject.password).toHex()
        }

        String roleString = dataObject.role
        Role currentRole = userService.getHighestRole(user)

        if(!currentRole || !roleString.equalsIgnoreCase(currentRole.name)){
            if(currentRole){
                user.removeFromRoles(currentRole)
            }
            println "trying to add role ${roleString}"
            Role role = Role.findByName(roleString.toUpperCase())
            user.addToRoles(role)
//            user.save()
        }

        user.save(flush: true)

    }
}
