package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.crypto.hash.Sha256Hash
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class UserService {

    // return admin role or user role
    Role getHighestRole(User user){
        for(Role role in user.roles.sort(){ a,b -> b.name<=>a.name }){
            return role
        }
    }

    JSONObject convertUserToJson(User currentUser){

        def userObject = new JSONObject()
        userObject.userId = currentUser.id
        userObject.username = currentUser.username
        userObject.firstName = currentUser.firstName
        userObject.lastName = currentUser.lastName
        Role role = getHighestRole(currentUser)
        userObject.role = role?.name


        Map<String, JSONObject> organismMap = new HashMap<>()
        for (UserOrganismPermission userOrganismPermission in UserOrganismPermission.findAllByUser(currentUser)) {
            JSONObject organismJSON = new JSONObject()
            organismJSON.put("organism", userOrganismPermission.organism.commonName)
            organismJSON.put("permissions", userOrganismPermission.permissions)
            organismJSON.put("permissionArray", userOrganismPermission.permissionValues)
            organismJSON.put("userId", userOrganismPermission.userId)
            organismJSON.put("id", userOrganismPermission.id)

            organismMap.put(userOrganismPermission.organism.commonName, organismJSON)
        }
        for (GroupOrganismPermission groupOrganismPermission in GroupOrganismPermission.findAllByGroupInList(currentUser.userGroups as List)) {

            JSONObject organismJSON = organismMap.get(groupOrganismPermission.organism.commonName)
            if (!organismJSON) {
                organismJSON = new JSONObject()
                organismJSON.put("organism", groupOrganismPermission.organism.commonName)
                organismJSON.put("permissions", groupOrganismPermission.permissions)
                organismJSON.put("id", groupOrganismPermission.id)
            } else {
                String permissions = mergePermissions(organismJSON.getString("permissions"), groupOrganismPermission.permissions)
                organismJSON.put("permissions", permissions)
            }
            organismJSON.put("groupId", groupOrganismPermission.groupId)

            organismMap.put(groupOrganismPermission.organism.commonName, organismJSON)
        }

        JSONArray organismPermissionsArray = new JSONArray()
        organismPermissionsArray.addAll(organismMap.values())

        userObject.organismPermissions = organismPermissionsArray
        userObject.put(FeatureStringEnum.HAS_USERS.value,true)
    }

    String mergePermissions(String permissions1, String permissions2) {
        log.debug "permissions1: ${permissions1}"
        log.debug "permissions2: ${permissions2}"
        JSONArray permissions1Array = JSON.parse(permissions1) as JSONArray
        JSONArray permissions2Array = JSON.parse(permissions2) as JSONArray

        Set<String> finalPermissions = new HashSet<>()
        for(int i =0 ; i < permissions1Array.size() ; i++){
            finalPermissions.add(permissions1Array.getString(i))
        }
        for(int i =0 ; i < permissions2Array.size() ; i++){
            finalPermissions.add(permissions2Array.getString(i))
        }

        JSONArray returnArray = new JSONArray()
        for(String permission in finalPermissions){
            returnArray.add(permission)
        }

        String finalPermissionsString = returnArray.toString()

        log.debug "final permission string ${finalPermissionsString}"

        return finalPermissionsString
    }

    def registerAdmin(JSONObject jsonObj) {
        registerAdmin(jsonObj.username,jsonObj.password,jsonObj.firstName,jsonObj.lastName)
    }
    
    def registerAdmin(String username,String password,String firstName,String lastName) {
        if(User.countByUsername(username)>0){
            log.warn("User exists ${username} and can not be added again.")
            return ;
        }

        def adminRole = Role.findByName(GlobalPermissionEnum.ADMIN.name())

        User user = new User(
                username: username
                ,passwordHash: new Sha256Hash(password).toHex()
                ,firstName: firstName
                ,lastName: lastName
        ).save(failOnError: true,flush:true)
        user.addToRoles(adminRole)
    }
}
