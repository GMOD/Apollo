package org.bbop.apollo

import org.bbop.apollo.gwt.shared.FeatureStringEnum

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class GroupController {

    def permissionService

    def getOrganismPermissionsForGroup(){
        JSONObject dataObject = JSON.parse(params.data)
        UserGroup group = UserGroup.findById(dataObject.userId)

        List<GroupOrganismPermission> groupOrganismPermissions = GroupOrganismPermission.findAllByGroup(group)
        render groupOrganismPermissions as JSON
    }

    def loadGroups() {
        JSONArray returnArray = new JSONArray()
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
        for(v in groupOrganismPermissionMap){
            log.debug "${v.key} ${v.value}"
        }


        UserGroup.all.each {
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
                if(groupOrganismPermission.organism in allowableOrganisms){
                    JSONObject organismJSON = new JSONObject()
                    organismJSON.put("organism", groupOrganismPermission.organism.commonName)
                    organismJSON.put("permissions",groupOrganismPermission.permissions)
                    organismJSON.put("groupId",groupOrganismPermission.groupId)
                    organismJSON.put("id",groupOrganismPermission.id)
                    organismPermissionsArray.add(organismJSON)
                    organismsWithPermissions.add(groupOrganismPermission.organism.id)
                }
            }

            Set<Organism> organismList = allowableOrganisms.findAll(){
                !organismsWithPermissions.contains(it.id)
            }

            for(Organism organism in organismList){
                JSONObject organismJSON = new JSONObject()
                organismJSON.put("organism", organism.commonName)
                organismJSON.put("permissions","[]")
                organismJSON.put("groupId",it.id)
                organismPermissionsArray.add(organismJSON)
            }


            groupObject.organismPermissions = organismPermissionsArray
            returnArray.put(groupObject)
        }

        render returnArray as JSON
    }

    def createGroup(){
        log.debug "creating user ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        log.debug "dataObject ${dataObject}"

        UserGroup group = new UserGroup(
                name: dataObject.name
        ).save(flush: true)


        render group as JSON

    }

    def deleteGroup(){
        log.debug "deleting user ${request.JSON} -> ${params}"
        JSONObject dataObject = JSON.parse(params.data)
        UserGroup group = UserGroup.findById(dataObject.id)
        group.users.each { it ->
            it.removeFromUserGroups(group)
        }
//        UserTrackPermission.deleteAll(UserTrackPermission.findAllByUser(user))
//        UserOrganismPermission.deleteAll(UserOrganismPermission.findAllByUser(user))
        group.delete(flush: true)

        render new JSONObject() as JSON
    }

    def updateGroup(){

        log.debug "json: ${request.JSON}"
        log.debug "params: ${params}"
        log.debug "params.data: ${params.data}"
        JSONObject dataObject = JSON.parse(params.data)
        UserGroup group = UserGroup.findById(dataObject.id)
        // the only thing that can really change
        group.name = dataObject.name

        group.save(flush: true)
    }

    /**
     * Only changing one of the boolean permissions
     * @return
     */
    def updateOrganismPermission(){
        JSONObject dataObject = JSON.parse(params.data)
        log.debug "json data ${dataObject}"
        GroupOrganismPermission groupOrganismPermission = GroupOrganismPermission.findById(dataObject.id)


        UserGroup group = UserGroup.findById(dataObject.groupId)
        Organism organism =  Organism.findByCommonName(dataObject.organism)
        log.debug "found ${groupOrganismPermission}"
        if(!groupOrganismPermission){
            groupOrganismPermission = GroupOrganismPermission.findByGroupAndOrganism(group,organism)
        }

        if(!groupOrganismPermission){
            log.debug "creating new permissions! "
            groupOrganismPermission = new GroupOrganismPermission(
                    group: UserGroup.findById(dataObject.groupId)
                    ,organism: Organism.findByCommonName(dataObject.organism)
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

        render groupOrganismPermission as JSON

    }
}
