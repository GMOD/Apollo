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

        List<String> allUserGroups = UserGroup.all.name
        Map<String,List<UserOrganismPermission>> userOrganismPermissionMap = new HashMap<>()
        List<UserOrganismPermission> userOrganismPermissionList = UserOrganismPermission.all
        println "total permission list ${userOrganismPermissionList.size()}"
        for(UserOrganismPermission userOrganismPermission in userOrganismPermissionList){
            List<UserOrganismPermission> userOrganismPermissionListTemp =  userOrganismPermissionMap.get(userOrganismPermission.user)
            if(userOrganismPermissionListTemp==null){
                userOrganismPermissionListTemp = new ArrayList<>()
            }
            userOrganismPermissionListTemp.add(userOrganismPermission)
            userOrganismPermissionMap.put(userOrganismPermission.user.username,userOrganismPermissionListTemp)
        }
        println "org permission map ${userOrganismPermissionMap.size()}"
        for(v in userOrganismPermissionMap){
            println "${v.key} ${v.value}"
        }

        User.all.each {
            def userObject = new JSONObject()
//            userObject.putAll(it.properties)
            userObject.userId = it.id
            userObject.username = it.username
            userObject.firstName = it.firstName
            userObject.lastName = it.lastName
            Role role = userService.getHighestRole(it)
            userObject.role = role?.name

            println "groups ${it.userGroups} for ${it.username}"


            JSONArray groupsArray = new JSONArray()
            List<String> groupsForUser = new ArrayList<>()
            for(group in it.userGroups){
                JSONObject groupJson = new JSONObject()
                groupsForUser.add(group.name)
                groupJson.put("name",group.name)
                groupsArray.add(groupJson)
            }
            userObject.groups = groupsArray


            JSONArray availableGroupsArray = new JSONArray()
            List<String> availableGroups  = allUserGroups-groupsForUser
            for(group in availableGroups){
                JSONObject groupJson = new JSONObject()
                groupJson.put("name",group)
                availableGroupsArray.add(groupJson)
            }
            userObject.availableGroups = availableGroupsArray


            // organism permissions
            JSONArray organismPermissionsArray = new JSONArray()
            def  userOrganismPermissionList3 = userOrganismPermissionMap.get(it.username)
            println "lsit retrieved? : ${userOrganismPermissionList3?.size()} for ${it.username}"
            for(UserOrganismPermission userOrganismPermission in userOrganismPermissionList3){
                JSONObject organismJSON = new JSONObject()
//                organismJSON.put("organism", (userOrganismPermission.organism as JSON).toString())
                organismJSON.put("organism", userOrganismPermission.organism.commonName)
                organismJSON.put("permissions",userOrganismPermission.permissions)
                organismJSON.put("userId",userOrganismPermission.userId)
                organismJSON.put("id",userOrganismPermission.id)
                organismPermissionsArray.add(organismJSON)
            }
            userObject.organismPermissions = organismPermissionsArray

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

    def addUserToGroup(){
        println "adding user to group ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        UserGroup userGroup = UserGroup.findByName(dataObject.group)
        User user = User.findById(dataObject.userId)
        user.addToUserGroups(userGroup)
        user.save(flush: true)
        render new JSONObject() as JSON
    }

    def removeUserFromGroup(){
        println "removing user from group ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        UserGroup userGroup = UserGroup.findByName(dataObject.group)
        User user = User.findById(dataObject.userId)
        user.removeFromUserGroups(userGroup)
        user.save(flush: true)
        render new JSONObject() as JSON
    }

    def createUser() {
        println "creating user ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        User user = new User(
                firstName: dataObject.firstName
                , lastName: dataObject.lastName
                , username: dataObject.email
                , passwordHash: new Sha256Hash(dataObject.password).toHex()
        )
        user.save(insert: true)

        String roleString = dataObject.role
        Role role = Role.findByName(roleString.toUpperCase())
        println "adding role: ${role}"
        user.addToRoles(role)
        role.addToUsers(user)
        role.save()
        user.save(flush:true)

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

        if (dataObject.password) {
            user.passwordHash = new Sha256Hash(dataObject.password).toHex()
        }

        String roleString = dataObject.role
        Role currentRole = userService.getHighestRole(user)

        if (!currentRole || !roleString.equalsIgnoreCase(currentRole.name)) {
            if (currentRole) {
                user.removeFromRoles(currentRole)
            }
            Role role = Role.findByName(roleString.toUpperCase())
            user.addToRoles(role)
//            user.save()
        }

        user.save(flush: true)

    }

    def getOrganismPermissionsForUser(){
        JSONObject dataObject = JSON.parse(params.data)
        User user = User.findById(dataObject.userId)

        List<UserOrganismPermission> userOrganismPermissionList = UserOrganismPermission.findAllByUser(user)

        render userOrganismPermissionList as JSON
    }

    /**
     * Only changing one of the boolean permissions
     * @return
     */
    def updateOrganismPermission(){
        JSONObject dataObject = JSON.parse(params.data)
        println "finding by id ${dataObject.id}"
        UserOrganismPermission userOrganismPermission = UserOrganismPermission.findById(dataObject.id)
        println "found ${userOrganismPermission}"



        JSONArray permissionsArray = new JSONArray()
        if(dataObject.getBoolean(PermissionEnum.ADMINISTRATE.name())){
            permissionsArray.add(PermissionEnum.ADMINISTRATE.name())
        }
        if(dataObject.getBoolean(PermissionEnum.WRITE.name())){
            permissionsArray.add(PermissionEnum.WRITE.name())
        }
        if(dataObject.getBoolean(PermissionEnum.EXPORT.name())){
            permissionsArray.add(PermissionEnum.EXPORT.name())
        }
        if(dataObject.getBoolean(PermissionEnum.READ.name())){
            permissionsArray.add(PermissionEnum.READ.name())
        }


        userOrganismPermission.permissions = permissionsArray.toString()
        userOrganismPermission.save(flush: true)

        render userOrganismPermission as JSON

    }

}
