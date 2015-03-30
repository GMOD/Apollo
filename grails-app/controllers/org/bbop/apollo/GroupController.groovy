package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class GroupController {

    def getOrganismPermissionsForGroup(){
        JSONObject dataObject = JSON.parse(params.data)
        UserGroup group = UserGroup.findById(dataObject.userId)

        List<GroupOrganismPermission> groupOrganismPermissions = GroupOrganismPermission.findAllByGroup(group)
        render groupOrganismPermissions as JSON
    }

    def loadGroups() {
        JSONArray returnArray = new JSONArray()

        UserGroup.all.each {
            def groupObject = new JSONObject()
            groupObject.groupId = it.id
            groupObject.name = it.name
            groupObject.public = it.isPublicGroup()
            groupObject.numberOfUsers = it.users?.size()


            returnArray.put(groupObject)
        }

        render returnArray as JSON
    }

    def createGroup(){
        println "creating user ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
//        User user = new User(
//                firstName: dataObject.firstName
//                , lastName: dataObject.lastName
//                , username: dataObject.email
//                , passwordHash: new Sha256Hash(dataObject.password).toHex()
//        )
//        user.save(insert: true)
//
//        String roleString = dataObject.role
//        Role role = Role.findByName(roleString.toUpperCase())
//        println "adding role: ${role}"
//        user.addToRoles(role)
//        role.addToUsers(user)
//        role.save()
//        user.save(flush:true)

        render new JSONObject() as JSON

    }

    def deleteGroup(){
        println "deleting user ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        UserGroup group = UserGroup.findById(dataObject.groupId)
        group.users.each { it ->
            it.removeFromUserGroups(group)
        }
//        UserTrackPermission.deleteAll(UserTrackPermission.findAllByUser(user))
//        UserOrganismPermission.deleteAll(UserOrganismPermission.findAllByUser(user))
        group.delete(flush: true)
    }

    def updateGroup(){

        JSONObject dataObject = JSON.parse(params.data)
        UserGroup group = UserGroup.findById(dataObject.groupId)
//        User user = User.findById(dataObject.userId)
//        user.firstName = dataObject.firstName
//        user.lastName = dataObject.lastName
//        user.username = dataObject.email
//
//        if (dataObject.password) {
//            user.passwordHash = new Sha256Hash(dataObject.password).toHex()
//        }
//
//        String roleString = dataObject.role
//        Role currentRole = userService.getHighestRole(user)
//
//        if (!currentRole || !roleString.equalsIgnoreCase(currentRole.name)) {
//            if (currentRole) {
//                user.removeFromRoles(currentRole)
//            }
//            Role role = Role.findByName(roleString.toUpperCase())
//            user.addToRoles(role)
////            user.save()
//        }

        group.save(flush: true)
    }
}
