package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb
import org.springframework.http.HttpStatus

@RestApi(name = "Group Services", description = "Methods for managing groups")
class GroupController {

    def permissionService
    def preferenceService

    @RestApiMethod(description = "Get organism permissions for group", path = "/group/getOrganismPermissionsForGroup", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Group ID (or specify the name)")
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Group name")
    ]
    )
    def getOrganismPermissionsForGroup() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        UserGroup group = UserGroup.findById(dataObject.groupId)
        if (!group) {
            group = UserGroup.findByName(dataObject.name)
        }
        if (!group) {
            JSONObject jsonObject = new JSONObject()
            jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to get organism permissions")
            render jsonObject as JSON
            return
        }

        List<GroupOrganismPermission> groupOrganismPermissions = GroupOrganismPermission.findAllByGroup(group)
        render groupOrganismPermissions as JSON
    }

    @RestApiMethod(description = "Load all groups", path = "/group/loadGroups", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "groupId", type = "long", paramType = RestApiParamType.QUERY, description = "Optional only load a specific groupId")
    ])
    def loadGroups() {
        try {
            log.debug "loadGroups"
            JSONObject dataObject = permissionService.handleInput(request, params)
            // allow instructor to view groups
            if (!permissionService.hasGlobalPermissions(dataObject, GlobalPermissionEnum.INSTRUCTOR)) {
                render status: HttpStatus.UNAUTHORIZED
                return
            }
            // to support webservice, get current user from session or input object
            def currentUser = permissionService.getCurrentUser(dataObject)
            JSONArray returnArray = new JSONArray()
            def allowableOrganisms = permissionService.getOrganisms((User) currentUser)

            Map<String, List<GroupOrganismPermission>> groupOrganismPermissionMap = new HashMap<>()

            List<GroupOrganismPermission> groupOrganismPermissionList = GroupOrganismPermission.findAllByOrganismInList(allowableOrganisms as List)
            for (GroupOrganismPermission groupOrganismPermission in groupOrganismPermissionList) {
                List<GroupOrganismPermission> groupOrganismPermissionListTemp = groupOrganismPermissionMap.get(groupOrganismPermission.group.name)
                if (groupOrganismPermissionListTemp == null) {
                    groupOrganismPermissionListTemp = new ArrayList<>()
                }
                groupOrganismPermissionListTemp.add(groupOrganismPermission)
                groupOrganismPermissionMap.put(groupOrganismPermission.group.name, groupOrganismPermissionListTemp)
            }

            // restricted groups
            def groups = dataObject.groupId ? [UserGroup.findById(dataObject.groupId)] : UserGroup.all
            def filteredGroups =  groups

            // if user is admin, then include all
            // if group has metadata with the creator or no metadata then include
            // instead of using !permissionService.isAdmin() because it only works for login user but doesn't work for webservice
            if (!permissionService.isUserGlobalAdmin(currentUser)) {
                log.debug "filtering groups"

                filteredGroups = groups.findAll(){
                    // permissionService.currentUser is None when accessing by webservice
                    it.metadata == null || it.getMetaData(FeatureStringEnum.CREATOR.value) == (currentUser.id as String) || permissionService.isGroupAdmin(it, currentUser)
                }
            }

            filteredGroups.each {
                def groupObject = new JSONObject()
                groupObject.id = it.id
                groupObject.name = it.name
                groupObject.public = it.isPublicGroup()
                groupObject.numberOfUsers = it.users?.size()

                JSONArray userArray = new JSONArray()
                it.users.each { user ->
                    JSONObject userObject = new JSONObject()
                    userObject.id = user.id
                    userObject.email = user.username
                    userObject.firstName = user.firstName
                    userObject.lastName = user.lastName
                    userArray.add(userObject)
                }
                groupObject.users = userArray

                JSONArray adminArray = new JSONArray()
                it.admin.each { user ->
                    JSONObject userObject = new JSONObject()
                    userObject.id = user.id
                    userObject.email = user.username
                    userObject.firstName = user.firstName
                    userObject.lastName = user.lastName
                    adminArray.add(userObject)
                }
                groupObject.admin = adminArray

                // add organism permissions
                JSONArray organismPermissionsArray = new JSONArray()
                def groupOrganismPermissionList3 = groupOrganismPermissionMap.get(it.name)
                List<Long> organismsWithPermissions = new ArrayList<>()
                for (GroupOrganismPermission groupOrganismPermission in groupOrganismPermissionList3) {
                    if (allowableOrganisms.contains(groupOrganismPermission.organism)) {
                        JSONObject organismJSON = new JSONObject()
                        organismJSON.organism = groupOrganismPermission.organism.commonName
                        organismJSON.permissions = groupOrganismPermission.permissions
                        organismJSON.permissionArray = groupOrganismPermission.permissionValues
                        organismJSON.groupId = groupOrganismPermission.groupId
                        organismJSON.id = groupOrganismPermission.id
                        organismPermissionsArray.add(organismJSON)
                        organismsWithPermissions.add(groupOrganismPermission.organism.id)
                    }
                }

                Set<Organism> organismList = allowableOrganisms.findAll() {
                    !organismsWithPermissions.contains(it.id)
                }

                for (Organism organism in organismList) {
                    JSONObject organismJSON = new JSONObject()
                    organismJSON.organism = organism.commonName
                    organismJSON.permissions = "[]"
                    organismJSON.permissionArray = new JSONArray()
                    organismJSON.groupId = it.id
                    organismPermissionsArray.add(organismJSON)
                }


                groupObject.organismPermissions = organismPermissionsArray
                returnArray.put(groupObject)
            }
            render returnArray as JSON
        }
        catch (Exception e) {
            response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
            def error = [error: e.message]
            log.error error
            render error as JSON
        }
    }

    @RestApiMethod(description = "Create group", path = "/group/createGroup", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Group name to add")
    ]
    )
    @Transactional
    def createGroup() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        // allow instructor to create Group
        if (!permissionService.hasGlobalPermissions(dataObject, GlobalPermissionEnum.INSTRUCTOR)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        log.info "Creating group"
        // permissionService.currentUser is None when accessing by webservice
        // to support webservice, get current user from session or input object
        def currentUser = permissionService.getCurrentUser(dataObject)
        UserGroup group = new UserGroup(
                name: dataObject.name,
                // add metadata from webservice
                metadata: dataObject.metadata ? dataObject.metadata.toString() : null
        )
        group.save(flush: true)
        // allow specify the metadata creator through webservice, if not specified, take current user as the creator
        if (!group.getMetaData(FeatureStringEnum.CREATOR.value)) {
            log.debug "creator does not exist, set current user as the creator"
            group.addMetaData(FeatureStringEnum.CREATOR.value, currentUser.id.toString())
        }
        // assign group creator as group admin
        def creatorId = group.getMetaData(FeatureStringEnum.CREATOR.value)
        User creator = User.findById(creatorId)
        group.addToAdmin(creator)
        log.debug "Add metadata creator: ${group.getMetaData(FeatureStringEnum.CREATOR.value)}"

        log.info "Added group ${group.name}"

        render group as JSON

    }

    @RestApiMethod(description = "Delete a group", path = "/group/deleteGroup", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Group ID to remove (or specify the name)")
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Group name to remove")
    ]
    )
    @Transactional
    def deleteGroup() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        UserGroup group = UserGroup.findById(dataObject.id)
        if (!group) {
            group = UserGroup.findByName(dataObject.name)
        }
        if (!group) {
            def error = [error: "Group ${dataObject.name} not found"]
            log.error(error.error)
            render error as JSON
            return
        }
        String creatorMetaData = group.getMetaData(FeatureStringEnum.CREATOR.value)
        // to support webservice, get current user from session or input object
        def currentUser = permissionService.getCurrentUser(dataObject)
        // only allow global admin or group creator, or group admin to delete the group
        if (!permissionService.hasGlobalPermissions(dataObject, GlobalPermissionEnum.ADMIN) && !(creatorMetaData && currentUser.id.toString() == creatorMetaData) && !permissionService.isGroupAdmin(group, currentUser)) {
            //render status: HttpStatus.UNAUTHORIZED.value()
            def error = [error: 'not authorized to delete the group']
            log.error(error.error)
            render error as JSON
            return
        }
        log.info "Removing group"

        List<User> users = group.users as List
        users.each { it ->
            it.removeFromUserGroups(group)
            it.save()
        }

        def groupOrganismPermissions = GroupOrganismPermission.findAllByGroup(group)
        GroupOrganismPermission.deleteAll(groupOrganismPermissions)

        log.info "Removing group ${group.name}"

        group.save(flush: true)
        group.delete(flush: true)


        render new JSONObject() as JSON
    }

    @RestApiMethod(description = "Update group", path = "/group/updateGroup", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Group ID to update")
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Group name to change to (the only editable optoin)")
    ]
    )
    @Transactional
    def updateGroup() {
        log.info "Updating group"
        JSONObject dataObject = permissionService.handleInput(request, params)
        UserGroup group = UserGroup.findById(dataObject.id)
        if (!group) {
            group = UserGroup.findByName(dataObject.name)
        }
        if (!group) {
            JSONObject jsonObject = new JSONObject()
            jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to delete the group")
            render jsonObject as JSON
            return
        }
        // to support webservice, get current user from session or input object
        def currentUser = permissionService.getCurrentUser(dataObject)
        String creatorMetaData = group.getMetaData(FeatureStringEnum.CREATOR.value)
        // allow global admin, group creator, and group admin to update the group
        if (!permissionService.hasGlobalPermissions(dataObject, GlobalPermissionEnum.ADMIN) && !(creatorMetaData && currentUser.id.toString() == creatorMetaData) && !permissionService.isGroupAdmin(group, currentUser)) {
            render status: HttpStatus.UNAUTHORIZED.value()
            return
        }

        // the only thing that can really change
        log.info "Updated group ${group.name} to use name ${dataObject.name}"
        group.name = dataObject.name
        // also allow update metadata
        group.metadata = dataObject.metadata?dataObject.metadata.toString():group.metadata
        group.save(flush: true)
    }

    /**
     * Only changing one of the boolean permissions
     * @return
     */
    @RestApiMethod(description = "Update organism permission", path = "/group/updateOrganismPermission", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "groupId", type = "long", paramType = RestApiParamType.QUERY, description = "Group ID to modify permissions for (must provide this or 'name')")
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Group name to modify permissions for (must provide this or 'groupId')")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name")

            , @RestApiParam(name = "ADMINISTRATE", type = "boolean", paramType = RestApiParamType.QUERY, description = "Indicate if user has administrative and all lesser (including user/group) privileges for the organism")
            , @RestApiParam(name = "WRITE", type = "boolean", paramType = RestApiParamType.QUERY, description = "Indicate if user has write and all lesser privileges for the organism")
            , @RestApiParam(name = "EXPORT", type = "boolean", paramType = RestApiParamType.QUERY, description = "Indicate if user has export and all lesser privileges for the organism")
            , @RestApiParam(name = "READ", type = "boolean", paramType = RestApiParamType.QUERY, description = "Indicate if user has read and all lesser privileges for the organism")
    ]
    )
    @Transactional
    def updateOrganismPermission() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        if (!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)) {
            render status: HttpStatus.UNAUTHORIZED.value()
            return
        }
        log.info "Trying to update group organism permissions"
        GroupOrganismPermission groupOrganismPermission = GroupOrganismPermission.findById(dataObject.id)


        UserGroup group
        if (dataObject.groupId) {
            group = UserGroup.findById(dataObject.groupId as Long)
        }
        if (!group) {
            group = UserGroup.findByName(dataObject.name)
        }
        if (!group) {
            render([(FeatureStringEnum.ERROR.value): "Failed to find group for ${dataObject.name} and ${dataObject.groupId}"] as JSON)
            return
        }

        log.debug "Finding organism by ${dataObject.organism}"
        Organism organism = preferenceService.getOrganismForTokenInDB(dataObject.organism)
        if (!organism) {
            render([(FeatureStringEnum.ERROR.value): "Failed to find organism for ${dataObject.organism}"] as JSON)
            return
        }


        log.debug "found ${groupOrganismPermission}"
        if (!groupOrganismPermission) {
            groupOrganismPermission = GroupOrganismPermission.findByGroupAndOrganism(group, organism)
        }

        if (!groupOrganismPermission) {
            log.debug "creating new permissions! "
            groupOrganismPermission = new GroupOrganismPermission(
                    group: group
                    , organism: organism
                    , permissions: "[]"
                    , permissionArray: new JSONArray()
            ).save(insert: true)
            log.debug "created new permissions! "
        }



        JSONArray permissionsArray = new JSONArray()
        if (dataObject.getBoolean(PermissionEnum.ADMINISTRATE.name())) {
            permissionsArray.add(PermissionEnum.ADMINISTRATE.name())
        }
        if (dataObject.getBoolean(PermissionEnum.WRITE.name())) {
            permissionsArray.add(PermissionEnum.WRITE.name())
        }
        if (dataObject.getBoolean(PermissionEnum.EXPORT.name())) {
            permissionsArray.add(PermissionEnum.EXPORT.name())
        }
        if (dataObject.getBoolean(PermissionEnum.READ.name())) {
            permissionsArray.add(PermissionEnum.READ.name())
        }

        if(permissionsArray.size()==0){
            groupOrganismPermission.delete(flush: true)
            render groupOrganismPermission as JSON
            return
        }


        groupOrganismPermission.permissions = permissionsArray.toString()
        groupOrganismPermission.save(flush: true)

        log.info "Updated permissions for group ${group.name} and organism ${organism?.commonName} and permissions ${permissionsArray?.toString()}"

        render groupOrganismPermission as JSON

    }

    @RestApiMethod(description = "Update group membership", path = "/group/updateMembership", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "groupId", type = "long", paramType = RestApiParamType.QUERY, description = "Group ID to alter membership of")
            , @RestApiParam(name = "users", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "A JSON array of strings of emails of users the now belong to the group")
    ]
    )
    @Transactional
    def updateMembership() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        UserGroup groupInstance = UserGroup.findById(dataObject.groupId)
        // to support webservice, get current user from session or input object
        def currentUser = permissionService.getCurrentUser(dataObject)
        String creatorMetaData = groupInstance.getMetaData(FeatureStringEnum.CREATOR.value)
        // allow global admin, group creator, and group admin to update the group membership
        if (!permissionService.hasGlobalPermissions(dataObject, GlobalPermissionEnum.ADMIN) && !(creatorMetaData && currentUser.id.toString() == creatorMetaData) && !permissionService.isGroupAdmin(groupInstance, currentUser)) {

            render status: HttpStatus.UNAUTHORIZED.value()
            return
        }
        log.info "Trying to update user group membership"


        List<User> oldUsers = groupInstance.users as List
        //List<String> usernames = dataObject.users
        //Fixed bug on passing array through web services: cannot cast String to List
        JSONArray arr = new JSONArray(dataObject.users)
        List<String> usernames = new ArrayList<String>()
        for (int i = 0; i < arr.length(); i++){
            usernames.add(arr.getString(i))
        }
        List<User> newUsers = User.findAllByUsernameInList(usernames)

        List<User> usersToAdd = newUsers - oldUsers
        List<User> usersToRemove = oldUsers - newUsers
        usersToAdd.each {
            groupInstance.addToUsers(it)
            it.addToUserGroups(groupInstance)
            it.save()
        }

        usersToRemove.each {
            groupInstance.removeFromUsers(it)
            it.removeFromUserGroups(groupInstance)
            it.save()
        }

        groupInstance.save(flush: true)

        log.info "Updated group ${groupInstance.name} membership setting users ${newUsers.join(' ')}"
        loadGroups()
    }

    @RestApiMethod(description = "Update group admin", path = "/group/updateGroupAdmin", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "groupId", type = "long", paramType = RestApiParamType.QUERY, description = "Group ID to alter membership of")
            , @RestApiParam(name = "users", type = "JSONArray", paramType = RestApiParamType.QUERY, description = "A JSON array of strings of emails of users the now belong to the group")
    ]
    )
    @Transactional
    def updateGroupAdmin() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        UserGroup groupInstance = UserGroup.findById(dataObject.groupId)
        // to support webservice, get current user from session or input object
        def currentUser = permissionService.getCurrentUser(dataObject)
        String creatorMetaData = groupInstance.getMetaData(FeatureStringEnum.CREATOR.value)
        // allow global admin, group creator, and group admin to update the group membership
        if (!permissionService.hasGlobalPermissions(dataObject, GlobalPermissionEnum.ADMIN) && !(creatorMetaData && currentUser.id.toString() == creatorMetaData) && !permissionService.isGroupAdmin(groupInstance, currentUser)) {

            render status: HttpStatus.UNAUTHORIZED.value()
            return
        }
        log.info "Trying to update group admin"
        
        List<User> oldUsers = groupInstance.admin as List
        //Fixed bug on passing array through web services: cannot cast String to List
        JSONArray arr = new JSONArray(dataObject.users)
        List<String> usernames = new ArrayList<String>()
        for (int i = 0; i < arr.length(); i++){
            usernames.add(arr.getString(i))
        }
        List<User> newUsers = User.findAllByUsernameInList(usernames)
        List<User> usersToAdd = newUsers - oldUsers
        List<User> usersToRemove = oldUsers - newUsers
        usersToAdd.each {
            groupInstance.addToAdmin(it)
            it.addToGroupAdmins(groupInstance)
            it.save()
        }
        usersToRemove.each {
            groupInstance.removeFromAdmin(it)
            it.removeFromGroupAdmins(groupInstance)
            it.save()
        }

        groupInstance.save(flush: true)
        log.info "Updated group ${groupInstance.name} admin ${newUsers.join(' ')}"
        loadGroups()
    }

    @RestApiMethod(description = "Get group admins, returns group admins as JSONArray", path = "/group/getGroupAdmin", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Group name")
    ])
    def getGroupAdmin() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        println "data: ${dataObject}"
        if (!permissionService.hasGlobalPermissions(dataObject, GlobalPermissionEnum.ADMIN)) {
            def error = [error: 'not authorized to view the metadata']
            log.error(error.error)
            render error as JSON
            return
        }
        UserGroup groupInstance = UserGroup.findByName(dataObject.name)
        if (!groupInstance) {
            def error = [error: 'The group does not exist']
            log.error(error.error)
            render error as JSON
            return
        }
        JSONArray returnArray = new JSONArray()
        def adminList = groupInstance.admin
        println "admin = ${adminList}"
        adminList.each {
            JSONObject user = new JSONObject()
            user.id = it.id
            user.firstName = it.firstName
            user.lastName = it.lastName
            user.username = it.username
            returnArray.put(user)
        }

        render returnArray as JSON

    }

    @RestApiMethod(description = "Get creator metadata for group, returns userId as JSONObject", path = "/group/getGroupCreator", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "Group name")
    ])
    def getGroupCreator() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        println "data: ${dataObject}"
        if (!permissionService.hasGlobalPermissions(dataObject, GlobalPermissionEnum.ADMIN)) {
            def error = [error: 'not authorized to view the metadata']
            log.error(error.error)
            render error as JSON
            return
        }
        UserGroup groupInstance = UserGroup.findByName(dataObject.name)
        if (!groupInstance) {
            def error = [error: 'The group does not exist']
            log.error(error.error)
            render error as JSON
            return
        }
        JSONObject metaData = new JSONObject()
        metaData.creator = groupInstance.getMetaData(FeatureStringEnum.CREATOR.value)
        render metaData as JSON

    }


}
