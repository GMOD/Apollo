package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class PermissionService {

    public static String TRACK_NAME_SPLITTER = "::"

    String getTracks(User user, Organism organism) {
        String trackList = ""
        for (UserPermission userPermission in UserPermission.findAllByUserAndOrganism(user, organism)) {
            trackList += userPermission.trackNames // TODO: add properly
        }
        for (UserGroup userGroup in user.userGroups) {
            trackList += getTracks(userGroup, organism)
        }
        return trackList
    }

    String getTracks(UserGroup group, Organism organism) {
        String trackList = ""
        JSONArray jsonArray = new JSONArray()
        for (GroupPermission groupPermission in GroupPermission.findAllByGroupAndOrganism(group, organism)) {
            trackList += "||" + groupPermission.trackNames // TODO: add properly
        }
        return trackList.trim()
    }

    Set<Organism> getOrganisms(User user) {
        Set<Organism> organismList = new HashSet<>()
        for (UserPermission userPermission in UserPermission.findAllByUser(user)) {
            if (userPermission.permissions) {
                organismList.add(userPermission.organism)
            }
        }
        for (UserGroup userGroup in user.userGroups) {
            organismList.addAll(getOrganisms(userGroup))
        }
        return organismList
    }

    List<Organism> getOrganisms(UserGroup group) {
        List<Organism> organismList = new ArrayList<>()
        for (GroupOrganismPermission groupPermission in GroupOrganismPermission.findAllByGroup(group)) {
            // minimally, you should have at least one permission
            if(groupPermission.permissions){
                organismList.add(groupPermission.organism)
            }
        }
        return organismList
    }

    String getTrackPermissions(UserGroup userGroup, Organism organism) {
        JSONArray jsonArray = new JSONArray()
        for (GroupPermission groupPermission in GroupPermission.findAllByGroupAndOrganism(userGroup, organism)) {
            jsonArray.add(groupPermission as JSON)
        }
        return jsonArray.toString()
    }

    String getTrackPermissions(User user, Organism organism) {
        JSONArray jsonArray = new JSONArray()
        for (UserPermission userPermission in UserPermission.findAllByUserAndOrganism(user, organism)) {
            jsonArray.add(userPermission as JSON)
        }
        String returnString = jsonArray.toString()
        for (UserGroup userGroup in user.userGroups) {
            returnString += getTrackPermissions(userGroup, organism)
        }
        return returnString
    }

    static Map<String, Boolean> mergeTrackVisibilityMaps(Map<String, Boolean> mapA, Map<String, Boolean> mapB) {
        Map<String, Boolean> returnMap = new HashMap<>()
        mapA.keySet().each{ it ->
            returnMap.put(it, mapA.get(it))
        }

        mapB.keySet().each { it ->
            if (returnMap.containsKey(it)) {
                returnMap.put(it, returnMap.get(it) || mapB.get(it))
            } else {
                returnMap.put(it, mapB.get(it))
            }
        }
        return returnMap
    }


    static  Collection<PermissionEnum> mergeOrganismPermissions(Collection<PermissionEnum> permissionsA, Collection<PermissionEnum> permissionsB) {
        Set<PermissionEnum> permissionEnums = new HashSet<>()
        permissionEnums.addAll(permissionsA)

        for (PermissionEnum permissionEnum in permissionsB) {
            permissionEnums.add(permissionEnum)
        }

        return permissionEnums
    }

    public Map<String, Boolean> getTracksVisibleForOrganismAndUser(Organism organism, User user) {
        Map<String, Boolean> trackVisibilityMap = new HashMap<>()

        List<UserTrackPermission> userPermissionList = UserTrackPermission.findAllByOrganismAndUser(organism, user)
        for (UserTrackPermission userPermission in userPermissionList) {
            JSONObject jsonObject = JSON.parse(userPermission.trackVisibilities) as JSONObject

            jsonObject.keySet().each{
                Boolean visible = trackVisibilityMap.get(it)
                // if null or false, can over-ride to true
                if(!visible){
                    trackVisibilityMap.put(it,jsonObject.get(it))
                }
            }
        }

        for (UserGroup group in user.userGroups) {
            Map<String, Boolean> specificMap = getTracksVisibleForOrganismAndGroup(organism, group)
            trackVisibilityMap = mergeTrackVisibilityMaps(specificMap, trackVisibilityMap)
        }

        return trackVisibilityMap
    }


    public Map<String, Boolean> getTracksVisibleForOrganismAndGroup(Organism organism, UserGroup userGroup) {
        Map<String, Boolean> trackVisibilityMap = new HashMap<>()

        List<GroupTrackPermission> groupPermissions = GroupTrackPermission.findAllByOrganismAndGroup(organism, userGroup)
        for (GroupTrackPermission groupPermission in groupPermissions) {
            JSONObject jsonObject = JSON.parse(groupPermission.trackVisibilities) as JSONObject
            
            // this should make it default to true if a true is ever given
            jsonObject.keySet().each{
                Boolean visible = trackVisibilityMap.get(it)
                // if null or false, can over-ride to true
                if(!visible){
                    trackVisibilityMap.put(it,jsonObject.get(it))
                }
            }
        }

        return trackVisibilityMap
    }


    public List<PermissionEnum> getOrganismPermissionsForUser(Organism organism, User user) {
        Set<PermissionEnum> permissions = new HashSet<>()

        List<UserOrganismPermission> userPermissionList = UserOrganismPermission.findAllByOrganismAndUser(organism, user)
        for (UserOrganismPermission userPermission in userPermissionList) {
            JSONArray jsonArray = JSON.parse(userPermission.permissions) as JSONArray
            for (int i = 0; i < jsonArray.size(); i++) {
                String permission = jsonArray.getString(i)
                PermissionEnum permissionEnum = PermissionEnum.getValueForString(permission)
                permissions.add(permissionEnum)
            }
        }

        for (UserGroup group in user.userGroups) {
            permissions = mergeOrganismPermissions(permissions, getOrganismPermissionsForUserGroup(organism, group))
        }

        return permissions as List

    }

    public List<PermissionEnum> getOrganismPermissionsForUserGroup(Organism organism, UserGroup userGroup) {
        Set<PermissionEnum> permissions = new HashSet<>()

        List<GroupOrganismPermission> groupPermissionList = GroupOrganismPermission.findAllByOrganismAndGroup(organism, userGroup)
        for (GroupOrganismPermission groupPermission in groupPermissionList) {
            JSONArray jsonArray = JSON.parse(groupPermission.permissions) as JSONArray
            for (int i = 0; i < jsonArray.size(); i++) {
//                String permission = jsonArray.getJSONObject(i).toString()
                String permission = jsonArray.getString(i)
                PermissionEnum permissionEnum = PermissionEnum.getValueForString(permission)
                permissions.add(permissionEnum)
            }
        }
        return permissions as List
    }


    public void setOrganismPermissionsForUser(List<PermissionEnum> permissions, Organism organism, User user) {
        
        UserOrganismPermission userOrganismPermission = UserOrganismPermission.findByOrganismAndUser(organism,user)
        if(!userOrganismPermission){
            userOrganismPermission = new UserOrganismPermission(
                    organism: organism
                    ,permissions: generatePermissionString(permissions)
                    ,user: user
            ).save(insert: true)
        }
        else{
            userOrganismPermission.permissions = generatePermissionString(permissions)
            userOrganismPermission.save()
        }

    }

    public void setOrganismPermissionsForUserGroup(List<PermissionEnum> permissions, Organism organism, UserGroup group) {
        
        GroupOrganismPermission groupOrganismPermission = GroupOrganismPermission.findByOrganismAndGroup(organism,group)
        if(!groupOrganismPermission){
            groupOrganismPermission = new GroupOrganismPermission(
                    organism: organism
                    ,permissions : generatePermissionString(permissions)
                    ,group: group
            ).save(insert: true)
        }
        else{
            groupOrganismPermission.permissions = generatePermissionString(permissions)
            groupOrganismPermission.save()
        }
    }

    private String generatePermissionString(List<PermissionEnum> permissionEnums) {
        JSONArray jsonArray = new JSONArray()
        for(PermissionEnum permissionEnum in permissionEnums){
            jsonArray.add(permissionEnum.name())
        }
        return jsonArray.toString()
    }

    private String convertHashMapToJsonString(Map map){
        JSONObject jsonObject = new JSONObject()
        map.keySet().each {
            jsonObject.put(it,map.get(it))
        }
        return jsonObject.toString()
    }

    /**
     *
     * * @param trackVisibilityMap  Map of track names and visibility.
     * @param user
     * @param organism
     */
    public void setTracksVisibleForOrganismAndUser(Map<String, Boolean> trackVisibilityMap, Organism organism, User user) {
        UserTrackPermission userTrackPermission = UserTrackPermission.findByOrganismAndUser(organism,user)
        String jsonString = convertHashMapToJsonString(trackVisibilityMap)
        if(!userTrackPermission){
            userTrackPermission = new UserTrackPermission(
                    user: user
                    ,organism: organism
                    ,trackVisibilities: jsonString
            ).save(insert: true)
        }
        else{
            userTrackPermission.trackVisibilities = jsonString
            userTrackPermission.save()
        }
    }

    /**
     * * 
     * * @param trackVisibilityMap  Map of track names and visibility.
     * @param group
     * @param organism
     */
    public void setTracksVisibleForOrganismAndGroup(Map<String, Boolean> trackVisibilityMap, Organism organism, UserGroup group) {
        
        GroupTrackPermission groupTrackPermission = GroupTrackPermission.findByOrganismAndGroup(organism,group)
        String jsonString = convertHashMapToJsonString(trackVisibilityMap)
        if(!groupTrackPermission){
            groupTrackPermission = new GroupTrackPermission(
                    group: group
                    ,organism: organism
                    ,trackVisibilities: jsonString
            ).save(insert: true)
        }
        else{
            groupTrackPermission.trackVisibilities = jsonString
            groupTrackPermission.save()
        }
        

    }

    /**
     * This maps between the two permission schemas.
     * Here we get all of the highest permissions
     * @param user
     * @return
     */
    Map<String, Integer> getPermissionsForUser(User user) {
        Map<String,Integer> returnMap = new HashMap<>()
        returnMap.put(user.username,0)
        Organism.all.each{ organism -> 
             List<PermissionEnum> permissionEnums = getOrganismPermissionsForUser(organism,user)
             int highestValue = findHighestEnumValue(permissionEnums)
             if(highestValue>returnMap.get(user.username)){
                 returnMap.put(user.username,highestValue)
             }
        }
        
        return returnMap
    }

    int findHighestEnumValue(List<PermissionEnum> permissionEnums) {
        int highestValue = -1 
        permissionEnums.each{ it ->
            highestValue = it.value > highestValue ? it.value : highestValue
        }
        
        return highestValue 
    }
}
