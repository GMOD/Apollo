package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray

@Transactional
class PermissionService {

    String getTracks(User user,Organism organism) {
        String trackList = ""
        for(UserPermission userPermission in UserPermission.findAllByUserAndOrganism(user,organism)){
            trackList += userPermission.trackNames // TODO: add properly
        }
        for(UserGroup userGroup in user.userGroups){
            trackList += getTracks(userGroup,organism)
        }
        return trackList
    }

    String getTracks(UserGroup group,Organism organism) {
        String trackList = ""
        JSONArray jsonArray = new JSONArray()
        for(GroupPermission groupPermission in GroupPermission.findAllByGroupAndOrganism(group,organism)){
            trackList += "||" + groupPermission.trackNames // TODO: add properly
        }
        return trackList.trim()
    }

    Set<Organism> getOrganisms(User user) {
        Set<Organism> organismList = new HashSet<>()
        for(UserPermission userPermission in UserPermission.findAllByUser(user)){
            if(userPermission.permissions){
                organismList.add(userPermission.organism)
            }
        }
        for(UserGroup userGroup in user.userGroups){
            organismList.addAll(getOrganisms(userGroup))
        }
        return organismList
    }

    List<Organism> getOrganisms(UserGroup group) {
        List<Organism> organismList = new ArrayList<>()
        for(GroupPermission groupPermission in GroupPermission.findAllByGroup(group)){
            if(groupPermission.permissions){
                organismList.add(groupPermission.organism)
            }
        }
        return organismList
    }
    
    String getTrackPermissions(UserGroup userGroup,Organism organism){
        JSONArray jsonArray = new JSONArray()
        for(GroupPermission groupPermission in GroupPermission.findAllByGroupAndOrganism(userGroup,organism)){
            jsonArray.add(groupPermission as JSON)
        }
        return jsonArray.toString()
    }

    String getTrackPermissions(User user,Organism organism){
        JSONArray jsonArray = new JSONArray()
        for(UserPermission userPermission in UserPermission.findAllByUserAndOrganism(user,organism)){
            jsonArray.add(userPermission as JSON)
        }
        String returnString = jsonArray.toString()
        for(UserGroup userGroup in user.userGroups){
            returnString += getTrackPermissions(userGroup,organism)
        }
        return returnString 
    }

}
