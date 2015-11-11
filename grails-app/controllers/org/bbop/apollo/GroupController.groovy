package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum

import grails.converters.JSON
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

    @RestApiMethod(description="Get organism permissions for group",path="/group/getOrganismPermissionsForGroup",verb = RestApiVerb.POST)
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="id", type="long", paramType = RestApiParamType.QUERY,description = "Group ID (or specify the name)")
            ,@RestApiParam(name="name", type="string", paramType = RestApiParamType.QUERY,description = "Group name")
    ]
    )
    def getOrganismPermissionsForGroup(){
        JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        UserGroup group = UserGroup.findById(dataObject.groupId)
        if(!group){
            group = UserGroup.findByName(dataObject.name)
        }
        if(!group){
            JSONObject jsonObject = new JSONObject()
            jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to get organism permissions" )
            render jsonObject as JSON
            return
        }

        List<GroupOrganismPermission> groupOrganismPermissions = GroupOrganismPermission.findAllByGroup(group)
        render groupOrganismPermissions as JSON
    }

    @RestApiMethod(description="Load all groups",path="/group/loadGroups",verb = RestApiVerb.POST)
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="groupId", type="long", paramType = RestApiParamType.QUERY,description="Optional only load a specific groupId")
    ])
    def loadGroups() {
        try {
            log.debug "loadGroups"
            JSONArray returnArray = new JSONArray()
            JSONObject dataObject = (request.JSON ?: (JSON.parse(params.data?:"{}"))) as JSONObject
            if(!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)){
                render status: HttpStatus.UNAUTHORIZED
                return
            }
            def allowableOrganisms = permissionService.getOrganisms((User) permissionService.currentUser)

            Map<String,List<GroupOrganismPermission>> groupOrganismPermissionMap = new HashMap<>()

            List<GroupOrganismPermission> groupOrganismPermissionList = GroupOrganismPermission.findAllByOrganismInList(allowableOrganisms as List)
            for(GroupOrganismPermission groupOrganismPermission in groupOrganismPermissionList){
                List<GroupOrganismPermission> groupOrganismPermissionListTemp =  groupOrganismPermissionMap.get(groupOrganismPermission.group.name)
                if(groupOrganismPermissionListTemp==null){
                    groupOrganismPermissionListTemp = new ArrayList<>()
                }
                groupOrganismPermissionListTemp.add(groupOrganismPermission)
                groupOrganismPermissionMap.put(groupOrganismPermission.group.name,groupOrganismPermissionListTemp)
            }


            def groups = dataObject.groupId?[UserGroup.findById(dataObject.groupId)]:UserGroup.all
            groups.each {
                def groupObject = new JSONObject()
                groupObject.id = it.id
                groupObject.name = it.name
                groupObject.public = it.isPublicGroup()
                groupObject.numberOfUsers = it.users?.size()

                JSONArray userArray = new JSONArray()
                it.users.each{ user ->
                    JSONObject userObject = new JSONObject()
                    userObject.id=user.id
                    userObject.email=user.username
                    userObject.firstName=user.firstName
                    userObject.lastName=user.lastName

                    userArray.add(userObject)
                }
                groupObject.users = userArray


                // add organism permissions
                JSONArray organismPermissionsArray = new JSONArray()
                def  groupOrganismPermissionList3 = groupOrganismPermissionMap.get(it.name)
                List<Long> organismsWithPermissions = new ArrayList<>()
                for(GroupOrganismPermission groupOrganismPermission in groupOrganismPermissionList3){
                    if(allowableOrganisms.contains(groupOrganismPermission.organism )){
                        JSONObject organismJSON = new JSONObject()
                        organismJSON.organism=groupOrganismPermission.organism.commonName
                        organismJSON.permissions=groupOrganismPermission.permissions
                        organismJSON.groupId=groupOrganismPermission.groupId
                        organismJSON.id=groupOrganismPermission.id
                        organismPermissionsArray.add(organismJSON)
                        organismsWithPermissions.add(groupOrganismPermission.organism.id)
                    }
                }

                Set<Organism> organismList = allowableOrganisms.findAll(){
                    !organismsWithPermissions.contains(it.id)
                }

                for(Organism organism in organismList){
                    JSONObject organismJSON = new JSONObject()
                    organismJSON.organism= organism.commonName
                    organismJSON.permissions="[]"
                    organismJSON.groupId=it.id
                    organismPermissionsArray.add(organismJSON)
                }


                groupObject.organismPermissions = organismPermissionsArray
                returnArray.put(groupObject)
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

    @RestApiMethod(description="Create group",path="/group/createGroup",verb = RestApiVerb.POST)
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="name", type="string", paramType = RestApiParamType.QUERY,description = "Group name to add")
    ]
    )
    @Transactional
    def createGroup(){
        JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if(!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)){
            render status: HttpStatus.UNAUTHORIZED
            return
        }
        log.info "Creating group"

        UserGroup group = new UserGroup(
                name: dataObject.name
        ).save(flush: true)

        log.info "Added group ${group.name}"

        render group as JSON

    }

    @RestApiMethod(description="Delete a group",path="/group/deleteGroup",verb = RestApiVerb.POST)
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="id", type="long", paramType = RestApiParamType.QUERY,description = "Group ID to remove (or specify the name)")
            ,@RestApiParam(name="name", type="string", paramType = RestApiParamType.QUERY,description = "Group name to remove")
    ]
    )
    @Transactional
    def deleteGroup(){
        JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if(!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)){
            render status: HttpStatus.UNAUTHORIZED.value()
            return
        }
        log.info "Removing group"
        UserGroup group = UserGroup.findById(dataObject.id)
        if(!group){
            group = UserGroup.findByName(dataObject.name)
        }
        if(!group){
            JSONObject jsonObject = new JSONObject()
            jsonObject.put(FeatureStringEnum.ERROR.value, "Failed to delete the group" )
            render jsonObject as JSON
            return
        }
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

    @RestApiMethod(description="Update group",path="/group/updateGroup",verb = RestApiVerb.POST)
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="id", type="long", paramType = RestApiParamType.QUERY,description = "Group ID to update")
            ,@RestApiParam(name="name", type="string", paramType = RestApiParamType.QUERY,description = "Group name to change to (the only editable optoin)")
    ]
    )
    @Transactional
    def updateGroup(){
        log.info "Updating group"
        JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if(!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)){
            render status: HttpStatus.UNAUTHORIZED.value()
            return
        }
        UserGroup group = UserGroup.findById(dataObject.id)
        // the only thing that can really change
        log.info "Updated group ${group.name} to use name ${dataObject.name}"
        group.name = dataObject.name

        group.save(flush: true)
    }

    /**
     * Only changing one of the boolean permissions
     * @return
     */
    @RestApiMethod(description="Update organism permission",path="/group/updateOrganismPermission",verb = RestApiVerb.POST)
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="groupId", type="long", paramType = RestApiParamType.QUERY,description = "Group ID to modify permissions for")
            ,@RestApiParam(name="name", type="string", paramType = RestApiParamType.QUERY,description = "Group name to modify permissions for")
            ,@RestApiParam(name="organism", type="string", paramType = RestApiParamType.QUERY,description = "Organism common name")



            ,@RestApiParam(name="administrate", type="boolean", paramType = RestApiParamType.QUERY,description = "Indicate if user has administrative privileges for the organism")
            ,@RestApiParam(name="write", type="boolean", paramType = RestApiParamType.QUERY,description = "Indicate if user has write privileges for the organism")
            ,@RestApiParam(name="export", type="boolean", paramType = RestApiParamType.QUERY,description = "Indicate if user has export privileges for the organism")
            ,@RestApiParam(name="read", type="boolean", paramType = RestApiParamType.QUERY,description = "Indicate if user has read privileges for the organism")
    ]
    )
    @Transactional
    def updateOrganismPermission(){
        JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if(!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)){
            render status: HttpStatus.UNAUTHORIZED.value()
            return
        }
        log.info "Trying to update group organism permissions"
        GroupOrganismPermission groupOrganismPermission = GroupOrganismPermission.findById(dataObject.id)


        UserGroup group
        if(dataObject.groupId){
            group = UserGroup.findById(dataObject.groupId)
        }
        if(!group){
            group = UserGroup.findByName(dataObject.name)
        }
        if(!group){
            render ([(FeatureStringEnum.ERROR.value):"Failed to find group"] as JSON)
            return
        }

        Organism organism =  Organism.findByCommonName(dataObject.organism)
        if(!organism)  Organism.findById(dataObject.organism)
        if(!organism) {
            render ([(FeatureStringEnum.ERROR.value):"Failed to find organism"] as JSON)
            return
        }


        log.debug "found ${groupOrganismPermission}"
        if(!groupOrganismPermission){
            groupOrganismPermission = GroupOrganismPermission.findByGroupAndOrganism(group,organism)
        }

        if(!groupOrganismPermission){
            log.debug "creating new permissions! "
            groupOrganismPermission = new GroupOrganismPermission(
                    group: group
                    ,organism: organism
                    ,permissions: "[]"
            ).save(insert: true)
            log.debug "created new permissions! "
        }



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


        groupOrganismPermission.permissions = permissionsArray.toString()
        groupOrganismPermission.save(flush: true)

        log.info "Updated permissions for group ${group.name} and organism ${organism?.commonName} and permissions ${permissionsArray?.toString()}"

        render groupOrganismPermission as JSON

    }

    @RestApiMethod(description="Update group membership",path="/group/updateMembership",verb = RestApiVerb.POST)
    @RestApiParams(params=[
            @RestApiParam(name="username", type="email", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="password", type="password", paramType = RestApiParamType.QUERY)
            ,@RestApiParam(name="groupId", type="long", paramType = RestApiParamType.QUERY,description = "Group ID to alter membership of")
            ,@RestApiParam(name="user", type="JSONArray", paramType = RestApiParamType.QUERY,description = "A JSON array of strings of emails of users the now belong to the group")
    ]
    )
    @Transactional
    def updateMembership() {
        JSONObject dataObject = (request.JSON ?: JSON.parse(params.data)) as JSONObject
        if (!permissionService.hasPermissions(dataObject, PermissionEnum.ADMINISTRATE)) {
            render status: HttpStatus.UNAUTHORIZED.value()
            return
        }
        log.info "Trying to update user group membership"
        UserGroup groupInstance = UserGroup.findById(dataObject.groupId)
        List<User> oldUsers = groupInstance.users as List
        List<String> usernames = dataObject.users
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

        render loadGroups() as JSON
    }

}
