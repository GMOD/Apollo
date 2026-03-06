package org.bbop.apollo

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

@Transactional
class GroupService {

    def permissionService

    List<UserGroup> createGroups(String metadata,User currentUser,String[] names) {
        List<UserGroup> groups = []
        for(name in names){
            UserGroup group = new UserGroup(
                    name: name,
                    // add metadata from webservice
                    metadata: metadata ? metadata.toString() : null
            )
            group.save()
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
            groups.add(group)
        }
        return groups
    }

    List<UserGroup> deleteGroups(JSONObject dataObject, User currentUser, List<UserGroup> groupList) {

        log.info "Removing groups ${groupList as JSON}"
        for(UserGroup group in groupList){
            log.info "Removing group ${group.name}"
            String creatorMetaData = group.getMetaData(FeatureStringEnum.CREATOR.value)
            // to support webservice, get current user from session or input object
            // only allow global admin or group creator, or group admin to delete the group
            if (!permissionService.hasGlobalPermissions(dataObject, GlobalPermissionEnum.ADMIN) && !(creatorMetaData && currentUser.id.toString() == creatorMetaData) && !permissionService.isGroupAdmin(group, currentUser)) {
                //render status: HttpStatus.UNAUTHORIZED.value()
                def error = [error: 'not authorized to delete the group']
                log.error(error.error)
                throw new RuntimeException(error)
//                render error as JSON
            }

            List<User> users = group.users as List
            users.each { it ->
                it.removeFromUserGroups(group)
                it.save()
            }

            def groupOrganismPermissions = GroupOrganismPermission.findAllByGroup(group)
            GroupOrganismPermission.deleteAll(groupOrganismPermissions)


            group.save()
            group.delete()
            log.info "Removed group ${group.name}"
        }

    }

    def updateMembership(JSONObject dataObject,User currentUser,Long groupId,JSONArray users){
        UserGroup groupInstance = UserGroup.findById(groupId)
        // to support webservice, get current user from session or input object
        String creatorMetaData = groupInstance.getMetaData(FeatureStringEnum.CREATOR.value)
        // allow global admin, group creator, and group admin to update the group membership
        if (!permissionService.hasGlobalPermissions(dataObject, GlobalPermissionEnum.ADMIN) && !(creatorMetaData && currentUser.id.toString() == creatorMetaData) && !permissionService.isGroupAdmin(groupInstance, currentUser)) {
            throw new AnnotationException("Unauthorized access of group ${groupInstance.name}")
//
//            render status: HttpStatus.UNAUTHORIZED.value()
//            return
        }
        log.info "Trying to update user group membership"

        List<User> oldUsers = groupInstance.users as List
        JSONArray arr = new JSONArray(users)
        List<String> usernames = new ArrayList<String>()
        for (int i = 0; i < arr.length(); i++) {
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
        groupInstance.save()
        log.info "Updated group ${groupInstance.name} membership setting users ${newUsers.join(' ')}"

    }
}
