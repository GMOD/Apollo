package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum

import grails.converters.JSON
import org.apache.shiro.crypto.hash.Sha256Hash
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb
import org.springframework.http.HttpStatus

@RestApi(name = "User Services", description = "Methods for managing users")
@Transactional(readOnly = true)
class UserController {

    def permissionService
    def userService



    @RestApiMethod(description="Load all users",path="/user/loadUsers",verb = RestApiVerb.POST)
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="userId", type="long", paramType = RestApiParamType.QUERY,description="Optionally only user a specific userId")
    ])
    def loadUsers() {
        try {
            JSONObject dataObject = (request.JSON ?: (JSON.parse(params.data?:"{}"))) as JSONObject
            JSONArray returnArray = new JSONArray()
            if(!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)){
                render status:  HttpStatus.UNAUTHORIZED
                return
            }

            def allowableOrganisms = permissionService.getOrganisms(permissionService.currentUser)

            List<String> allUserGroups = UserGroup.all.name
            Map<String, List<UserOrganismPermission>> userOrganismPermissionMap = new HashMap<>()
            List<UserOrganismPermission> userOrganismPermissionList = UserOrganismPermission.findAllByOrganismInList(allowableOrganisms as List)
            for (UserOrganismPermission userOrganismPermission in userOrganismPermissionList) {
                List<UserOrganismPermission> userOrganismPermissionListTemp = userOrganismPermissionMap.get(userOrganismPermission.user.username)
                if (userOrganismPermissionListTemp == null) {
                    userOrganismPermissionListTemp = new ArrayList<>()
                }
                userOrganismPermissionListTemp.add(userOrganismPermission)
                userOrganismPermissionMap.put(userOrganismPermission.user.username, userOrganismPermissionListTemp)
            }
            for (v in userOrganismPermissionMap) {
                log.debug "${v.key} ${v.value}"
            }

            def c=User.createCriteria()
            def users = c.list() {
                if(dataObject.userId && dataObject.userId in Integer) {
                    eq('id', (Long)dataObject.userId)
                }
                if(dataObject.userId && dataObject.userId in String) {
                    eq('username', dataObject.userId)
                }
            }
            users.each {
                def userObject = new JSONObject()

                userObject.userId = it.id
                userObject.username = it.username
                userObject.firstName = it.firstName
                userObject.lastName = it.lastName
                Role role = userService.getHighestRole(it)
                userObject.role = role?.name


                JSONArray groupsArray = new JSONArray()
                List<String> groupsForUser = new ArrayList<>()
                for (group in it.userGroups) {
                    JSONObject groupJson = new JSONObject()
                    groupsForUser.add(group.name)
                    groupJson.put("name", group.name)
                    groupsArray.add(groupJson)
                }
                userObject.groups = groupsArray


                JSONArray availableGroupsArray = new JSONArray()
                List<String> availableGroups = allUserGroups - groupsForUser
                for (group in availableGroups) {
                    JSONObject groupJson = new JSONObject()
                    groupJson.put("name", group)
                    availableGroupsArray.add(groupJson)
                }
                userObject.availableGroups = availableGroupsArray

                // organism permissions
                JSONArray organismPermissionsArray = new JSONArray()
                def userOrganismPermissionList3 = userOrganismPermissionMap.get(it.username)
                List<Long> organismsWithPermissions = new ArrayList<>()
                log.debug "number of groups for user: ${userOrganismPermissionList3?.size()} for ${it.username}"
                for (UserOrganismPermission userOrganismPermission in userOrganismPermissionList3) {
                    if (userOrganismPermission.organism in allowableOrganisms) {
                        JSONObject organismJSON = new JSONObject()
                        organismJSON.put("organism", userOrganismPermission.organism.commonName)
                        organismJSON.put("permissions", userOrganismPermission.permissions)
                        organismJSON.put("userId", userOrganismPermission.userId)
                        organismJSON.put("id", userOrganismPermission.id)
                        organismPermissionsArray.add(organismJSON)
                        organismsWithPermissions.add(userOrganismPermission.organism.id)
                    }
                }

                // if an organism has permissions
                Set<Organism> organismList = allowableOrganisms.findAll() {
                    !organismsWithPermissions.contains(it.id)
                }

                for (Organism organism in organismList) {
                    JSONObject organismJSON = new JSONObject()
                    organismJSON.put("organism", organism.commonName)
                    organismJSON.put("permissions", "[]")
                    organismJSON.put("userId", it.id)
                    organismPermissionsArray.add(organismJSON)
                }


                userObject.organismPermissions = organismPermissionsArray

                returnArray.put(userObject)
            }

            render returnArray as JSON
        }
        catch(Exception e) {
            response.status=HttpStatus.INTERNAL_SERVER_ERROR.value()
            def error=[error: e.message]
            log.error error
            render error as JSON
        }
    }

    def checkLogin() {
        def currentUser = permissionService.currentUser
        if (currentUser) {

            UserOrganismPreference userOrganismPreference
            try {
                // sets it by default
                userOrganismPreference = permissionService.getCurrentOrganismPreference()
            } catch (e) {
                log.error(e)
            }


            def userObject = userService.convertUserToJson(currentUser)

            if ((!userOrganismPreference || !permissionService.hasAnyPermissions(currentUser)) && !permissionService.isUserAdmin(currentUser)) {
                userObject.put(FeatureStringEnum.ERROR.value, "You do not have access to any organism on this server.  Please contact your administrator.")
            }
            else if(userOrganismPreference) {
                userObject.put("tracklist", userOrganismPreference.nativeTrackList)
            }

            render userObject as JSON
        } else {
            def userObject = new JSONObject()
            userObject.put(FeatureStringEnum.HAS_USERS.value, User.count > 0)
            render userObject as JSON
        }
    }


    @Transactional
    def updateTrackListPreference() {
        try {
            JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
            if (!permissionService.hasPermissions(dataObject, PermissionEnum.READ)) {
                render status: HttpStatus.UNAUTHORIZED
                return
            }
            log.info "updateTrackListPreference"

            UserOrganismPreference uop=permissionService.getCurrentOrganismPreference()

            uop.nativeTrackList = dataObject.get("tracklist")
            uop.save(flush: true)
            log.info "Added userOrganismPreference ${uop.nativeTrackList}"
            render new JSONObject() as JSON
        }
        catch(Exception e) {
            log.error "${e.message}"
            render ([error: e.message]) as JSON
        }
    }


    @RestApiMethod(description = "Add user to group", path = "/user/addUserToGroup", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "group", type = "string", paramType = RestApiParamType.QUERY, description = "Group name")
            , @RestApiParam(name = "userId", type = "long", paramType = RestApiParamType.QUERY, description = "User id")
            , @RestApiParam(name = "user", type = "email", paramType = RestApiParamType.QUERY, description = "User email/username (supplied if user id unknown)")
    ])
    @Transactional
    def addUserToGroup() {
        JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        log.info "Adding user to group"
        UserGroup userGroup = UserGroup.findByName(dataObject.group)
        User user = dataObject.userId ? User.findById(dataObject.userId) : User.findByUsername(dataObject.user)
        user.addToUserGroups(userGroup)
        user.save(flush: true)
        log.info "Added user ${user.username} to group ${userGroup.name}"
        render new JSONObject() as JSON
    }

    @RestApiMethod(description = "Remove user from group", path = "/user/removeUserFromGroup", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "group", type = "string", paramType = RestApiParamType.QUERY, description = "Group name")
            , @RestApiParam(name = "userId", type = "long", paramType = RestApiParamType.QUERY, description = "User id")
            , @RestApiParam(name = "user", type = "email", paramType = RestApiParamType.QUERY, description = "User email/username (supplied if user id unknown)")
    ])
    @Transactional
    def removeUserFromGroup() {
        JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        log.info "Removing user from group"
        UserGroup userGroup = UserGroup.findByName(dataObject.group)
        User user = dataObject.userId ? User.findById(dataObject.userId) : User.findByUsername(dataObject.user)
        user.removeFromUserGroups(userGroup)
        user.save(flush: true)
        log.info "Removed user ${user.username} from group ${userGroup.name}"
        render new JSONObject() as JSON
    }

    @RestApiMethod(description = "Create user", path = "/user/createUser", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "email", type = "email", paramType = RestApiParamType.QUERY, description = "Email of the user to add")
            , @RestApiParam(name = "firstName", type = "string", paramType = RestApiParamType.QUERY, description = "First name of user to add")
            , @RestApiParam(name = "lastName", type = "string", paramType = RestApiParamType.QUERY, description = "Last name of user to add")
            , @RestApiParam(name = "newPassword", type = "string", paramType = RestApiParamType.QUERY, description = "Password of user to add")
    ])
    @Transactional
    def createUser() {
        try {
            log.info "Creating user"
            JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
            if (!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)) {
                render status: HttpStatus.UNAUTHORIZED
                return
            }
            if (User.findByUsername(dataObject.email) != null) {
                JSONObject error = new JSONObject()
                error.put(FeatureStringEnum.ERROR.value, "User already exists. Please enter a new username")
                render error.toString()
                return
            }

            User user = new User(
                    firstName: dataObject.firstName
                    , lastName: dataObject.lastName
                    , username: dataObject.email
                    , passwordHash: new Sha256Hash(dataObject.newPassword ?: dataObject.password).toHex()
            )
            user.save(insert: true)

            String roleString = dataObject.role
            Role role = Role.findByName(roleString.toUpperCase())
            log.debug "adding role: ${role}"
            user.addToRoles(role)
            role.addToUsers(user)
            role.save()
            user.save(flush: true)

            log.info "Added user ${user.username} with role ${role.name}"

            render new JSONObject() as JSON
        } catch (e) {
            log.error(e.fillInStackTrace())
            JSONObject jsonObject = new JSONObject()
            jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to add the user " + e.message)
            render jsonObject as JSON
        }

    }

    @RestApiMethod(description = "Delete user", path = "/user/deleteUser", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "userId", type = "long", paramType = RestApiParamType.QUERY, description = "User ID to delete")
            , @RestApiParam(name = "userToDelete", type = "email", paramType = RestApiParamType.QUERY, description = "Username (email) to delete")
    ])
    @Transactional
    def deleteUser() {
        try {
            log.info "Removing user"
            JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
            if (!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)) {
                render status: HttpStatus.UNAUTHORIZED
                return
            }
            User user = null
            if (dataObject.has('userId')) {
                user = User.findById(dataObject.userId)
            }
            // to support the webservice
            if (!user && dataObject.has("userToDelete")) {
                user = User.findByUsername(dataObject.userToDelete)
            }
            user.userGroups.each { it ->
                it.removeFromUsers(user)
            }
            UserTrackPermission.deleteAll(UserTrackPermission.findAllByUser(user))
            UserOrganismPermission.deleteAll(UserOrganismPermission.findAllByUser(user))
            UserOrganismPreference.deleteAll(UserOrganismPreference.findAllByUser(user))
            user.delete(flush: true)

            log.info "Removed user ${user.username}"

            render new JSONObject() as JSON
        } catch (e) {
            log.error(e.fillInStackTrace())
            JSONObject jsonObject = new JSONObject()
            jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to delete the user " + e.message)
            render jsonObject as JSON
        }
    }

    @RestApiMethod(description = "Update user", path = "/user/updateUser", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "userId", type = "long", paramType = RestApiParamType.QUERY, description = "User ID to update")
            , @RestApiParam(name = "email", type = "email", paramType = RestApiParamType.QUERY, description = "Email of the user to update")
            , @RestApiParam(name = "firstName", type = "string", paramType = RestApiParamType.QUERY, description = "First name of user to update")
            , @RestApiParam(name = "lastName", type = "string", paramType = RestApiParamType.QUERY, description = "Last name of user to update")
            , @RestApiParam(name = "newPassword", type = "string", paramType = RestApiParamType.QUERY, description = "Password of user to update")
    ]
    )
    @Transactional
    def updateUser() {
        try {
            log.info "Updating user"
            JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
            if (!permissionService.sameUser(dataObject) && !permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)) {
                render status: HttpStatus.UNAUTHORIZED
                return
            }
            User user = User.findById(dataObject.userId)
            user.firstName = dataObject.firstName
            user.lastName = dataObject.lastName
            user.username = dataObject.email

            if (dataObject.newPassword) {
                user.passwordHash = new Sha256Hash(dataObject.newPassword).toHex()
            }

            String roleString = dataObject.role
            Role currentRole = userService.getHighestRole(user)

            if (!currentRole || !roleString.equalsIgnoreCase(currentRole.name)) {
                if (currentRole) {
                    user.removeFromRoles(currentRole)
                }
                Role role = Role.findByName(roleString.toUpperCase())
                user.addToRoles(role)
            }
            log.info "Updated user"
            user.save(flush: true)
            render new JSONObject() as JSON
        } catch (e) {
            log.error(e.fillInStackTrace())
            JSONObject jsonObject = new JSONObject()
            jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to update the user " + e.message)
            render jsonObject as JSON
        }

    }

    @RestApiMethod(description = "Get organism permissions for user, returns an array of permission objects", path = "/user/getOrganismPermissionsForUser", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "userId", type = "long", paramType = RestApiParamType.QUERY, description = "User ID to fetch")
    ])
    def getOrganismPermissionsForUser() {
        JSONObject dataObject = JSON.parse(params.data)
        User user = User.findById(dataObject.userId)

        List<UserOrganismPermission> userOrganismPermissionList = UserOrganismPermission.findAllByUser(user)

        render userOrganismPermissionList as JSON
    }

    /**
     * Only changing one of the boolean permissions
     * @return
     */
    @RestApiMethod(description = "Update organism permissions", path = "/user/updateOrganismPermission", verb = RestApiVerb.POST)
    @RestApiParams(params = [
            @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
            , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)

            , @RestApiParam(name = "userId", type = "long", paramType = RestApiParamType.QUERY, description = "User ID to modify permissions for")
            , @RestApiParam(name = "user", type = "email", paramType = RestApiParamType.QUERY, description = "(Optional) user email of the user to modify permissions for if User ID is not provided")
            , @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "Name of organism to update")
            , @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.QUERY, description = "Permission ID to update (can get from userId/organism instead)")

            , @RestApiParam(name = "administrate", type = "boolean", paramType = RestApiParamType.QUERY, description = "Indicate if user has administrative privileges for the organism")
            , @RestApiParam(name = "write", type = "boolean", paramType = RestApiParamType.QUERY, description = "Indicate if user has write privileges for the organism")
            , @RestApiParam(name = "export", type = "boolean", paramType = RestApiParamType.QUERY, description = "Indicate if user has export privileges for the organism")
            , @RestApiParam(name = "read", type = "boolean", paramType = RestApiParamType.QUERY, description = "Indicate if user has read privileges for the organism")
    ])
    @Transactional
    def updateOrganismPermission() {
        log.info "Updating organism permissions"
        JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        UserOrganismPermission userOrganismPermission = UserOrganismPermission.findById(dataObject.id)


        User user = dataObject.userId ? User.findById(dataObject.userId) : User.findByUsername(dataObject.user)

        Organism organism = Organism.findByCommonName(dataObject.organism)
        log.debug "found ${userOrganismPermission}"
        if (!userOrganismPermission) {
            userOrganismPermission = UserOrganismPermission.findByUserAndOrganism(user, organism)
        }

        if (!userOrganismPermission) {
            log.debug "creating new permissions! "
            userOrganismPermission = new UserOrganismPermission(
                    user: user
                    , organism: Organism.findByCommonName(dataObject.organism)
                    , permissions: "[]"
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


        userOrganismPermission.permissions = permissionsArray.toString()
        userOrganismPermission.save(flush: true)

        log.info "Updated organism permissions for user ${user.username} and organism ${organism.commonName} and permissions ${permissionsArray.toString()}"

        render userOrganismPermission as JSON
    }

}
