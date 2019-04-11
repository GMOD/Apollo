package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.codehaus.groovy.grails.web.json.JSONObject

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
//        groups[0].save(flush:true,failOnError: true)
    }

    List<UserGroup> deleteGroups(JSONObject dataObject, User currentUser, List<UserGroup> groupList) {

        println "rmoeving groups ${groupList as JSON}"
        for(UserGroup group in groupList){
            println "Removing group ${group.name}"
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
            println "Removed group ${group.name}"
        }

    }
}
