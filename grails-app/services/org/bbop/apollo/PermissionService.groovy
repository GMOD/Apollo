package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import grails.util.Environment
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.session.Session
import org.apache.shiro.subject.Subject
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class PermissionService {

    def preferenceService

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

    boolean isUserAdmin(User user) {
        for (Role role in user.roles) {
            if (role.name == UserService.ADMIN) {
                return true
            }
        }
        return false
    }

    boolean isAdmin() {
        String currentUserName = SecurityUtils.subject.principal
        if (currentUserName) {
            User researcher = User.findByUsername(currentUserName)
            if (isUserAdmin(researcher)) {
                return true
            }
        }
        return false
    }

    Set<Organism> getOrganisms(User user) {
        if (isAdmin()) {
            return Organism.listOrderByCommonName()
        }
        Set<Organism> organismList = new HashSet<>()
        for (UserOrganismPermission userPermission in UserOrganismPermission.findAllByUser(user)) {
            if (userPermission.permissions) {
                organismList.add(userPermission.organism)
            }
        }
        for (UserGroup userGroup in user?.userGroups) {
            organismList.addAll(getOrganisms(userGroup))
        }
        return organismList
    }

    List<Organism> getOrganisms(UserGroup group) {
        if (isAdmin()) {
            return Organism.listOrderByCommonName()
        }
        List<Organism> organismList = new ArrayList<>()
        for (GroupOrganismPermission groupPermission in GroupOrganismPermission.findAllByGroup(group)) {
            // minimally, you should have at least one permission
            if (groupPermission.permissions) {
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
        mapA.keySet().each { it ->
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


    static Collection<PermissionEnum> mergeOrganismPermissions(Collection<PermissionEnum> permissionsA, Collection<PermissionEnum> permissionsB) {
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

            jsonObject.keySet().each {
                Boolean visible = trackVisibilityMap.get(it)
                // if null or false, can over-ride to true
                if (!visible) {
                    trackVisibilityMap.put(it, jsonObject.get(it))
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
            jsonObject.keySet().each {
                Boolean visible = trackVisibilityMap.get(it)
                // if null or false, can over-ride to true
                if (!visible) {
                    trackVisibilityMap.put(it, jsonObject.get(it))
                }
            }
        }

        return trackVisibilityMap
    }


    public List<PermissionEnum> getOrganismPermissionsForUser(Organism organism, User user) {
        Set<PermissionEnum> permissions = new HashSet<>()
        if(isUserAdmin(user)){
            permissions.addAll(PermissionEnum.ADMINISTRATE as List)
        }

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
                String permission = jsonArray.getString(i)
                PermissionEnum permissionEnum = PermissionEnum.getValueForString(permission)
                permissions.add(permissionEnum)
            }
        }
        return permissions as List
    }


    public void setOrganismPermissionsForUser(List<PermissionEnum> permissions, Organism organism, User user) {

        UserOrganismPermission userOrganismPermission = UserOrganismPermission.findByOrganismAndUser(organism, user)
        if (!userOrganismPermission) {
            userOrganismPermission = new UserOrganismPermission(
                    organism: organism
                    , permissions: generatePermissionString(permissions)
                    , user: user
            ).save(insert: true)
        } else {
            userOrganismPermission.permissions = generatePermissionString(permissions)
            userOrganismPermission.save()
        }

    }

    public void setOrganismPermissionsForUserGroup(List<PermissionEnum> permissions, Organism organism, UserGroup group) {

        GroupOrganismPermission groupOrganismPermission = GroupOrganismPermission.findByOrganismAndGroup(organism, group)
        if (!groupOrganismPermission) {
            groupOrganismPermission = new GroupOrganismPermission(
                    organism: organism
                    , permissions: generatePermissionString(permissions)
                    , group: group
            ).save(insert: true)
        } else {
            groupOrganismPermission.permissions = generatePermissionString(permissions)
            groupOrganismPermission.save()
        }
    }

    private String generatePermissionString(List<PermissionEnum> permissionEnums) {
        JSONArray jsonArray = new JSONArray()
        for (PermissionEnum permissionEnum in permissionEnums) {
            jsonArray.add(permissionEnum.name())
        }
        return jsonArray.toString()
    }

    private String convertHashMapToJsonString(Map map) {
        JSONObject jsonObject = new JSONObject()
        map.keySet().each {
            jsonObject.put(it, map.get(it))
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
        UserTrackPermission userTrackPermission = UserTrackPermission.findByOrganismAndUser(organism, user)
        String jsonString = convertHashMapToJsonString(trackVisibilityMap)
        if (!userTrackPermission) {
            userTrackPermission = new UserTrackPermission(
                    user: user
                    , organism: organism
                    , trackVisibilities: jsonString
            ).save(insert: true)
        } else {
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

        GroupTrackPermission groupTrackPermission = GroupTrackPermission.findByOrganismAndGroup(organism, group)
        String jsonString = convertHashMapToJsonString(trackVisibilityMap)
        if (!groupTrackPermission) {
            groupTrackPermission = new GroupTrackPermission(
                    group: group
                    , organism: organism
                    , trackVisibilities: jsonString
            ).save(insert: true)
        } else {
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
        Map<String, Integer> returnMap = new HashMap<>()
        returnMap.put(user.username, 0)
        Organism.all.each { organism ->
            List<PermissionEnum> permissionEnums = getOrganismPermissionsForUser(organism, user)
            int highestValue = findHighestEnumValue(permissionEnums)
            if (highestValue > returnMap.get(user.username)) {
                returnMap.put(user.username, highestValue)
            }
        }

        return returnMap
    }

    User getCurrentUser() {
        String currentUserName = SecurityUtils.subject.principal
//        def subject = SecurityUtils.subject
        if (currentUserName) {
            User user = User.findByUsername(currentUserName)
            if (user) {
                return user
            }

            Session session = SecurityUtils.subject.session(false)
        }
        return null
    }

    PermissionEnum findHighestEnum(List<PermissionEnum> permissionEnums) {
        PermissionEnum highestValue = PermissionEnum.NONE
        permissionEnums.each { it ->
            highestValue = it.rank > highestValue.rank ? it : highestValue
        }

        return highestValue
    }

    int findHighestEnumValue(List<PermissionEnum> permissionEnums) {
        int highestValue = -1
        permissionEnums.each { it ->
            highestValue = it.value > highestValue ? it.value : highestValue
        }

        return highestValue
    }

    int findHighestEnumRank(List<PermissionEnum> permissionEnums) {
        int highestValue = -1
        permissionEnums.each { it ->
            highestValue = it.rank > highestValue ? it.rank : highestValue
        }

        return highestValue
    }

    /**
     * If it comes through a WebSocket, the USERNAME will be set explcitly
     * @param jsonTranscript
     * @return
     */
    User findUser(JSONObject jsonTranscript) {
        String userName = findUserName(jsonTranscript)
        return userName ? User.findByUsername(userName) : null
    }

    /**
     * If it comes through a WebSocket, the USERNAME will be set explcitly
     * @param jsonTranscript
     * @return
     */
    String findUserName(JSONObject jsonTranscript) {
        if (jsonTranscript.containsKey(FeatureStringEnum.USERNAME.value)) {
            return jsonTranscript.getString(FeatureStringEnum.USERNAME.value)
        } else {
            try {
                return SecurityUtils.subject.principal?.toString()
            } catch (e) {
                log.info "unable to find user for session ${e}"
                return null
            }
        }
    }

    JSONObject copyUserName(JSONObject fromJSON, JSONObject toJSON) {
        if (fromJSON.containsKey(FeatureStringEnum.USERNAME.value)) {
            toJSON.put(FeatureStringEnum.USERNAME.value, fromJSON.getString(FeatureStringEnum.USERNAME.value))
        } else {
            log.info "No username to copy from ${fromJSON}"
        }
        return toJSON
    }

    def getOrganismsForCurrentUser() {
        User thisUser = currentUser
        if (thisUser) {
            return getOrganisms(thisUser)
        }
        return []
    }

    public static String getSequenceNameFromInput(JSONObject inputObject) {
        String trackName = null
        if (inputObject.has("sequence")) {
            trackName = fixTrackHeader(inputObject.sequence)
        }
        if (inputObject.has("track")) {
            trackName = fixTrackHeader(inputObject.track)
        }
        return trackName
    }

    private static String fixTrackHeader(String trackInput) {
        return !trackInput.startsWith("Annotations-") ? trackInput : trackInput.substring("Annotations-".size())
    }

    /**
     * This method finds the proper username with their proper organism for the current organism.
     *
     * If there are no preferences, that is okay, we'll create one from the permissions.
     *
     * If there are no permissions then the result is the same . . we throw an exception.
     *
     * @param inputObject
     * @param requiredPermissionEnum
     * @return
     */
    Organism checkPermissionsForOrganism(JSONObject inputObject, PermissionEnum requiredPermissionEnum) {
        Organism organism

        // this is for testing only
        if (Environment.current == Environment.TEST && !inputObject.containsKey(FeatureStringEnum.USERNAME.value)) {
            return null
        }

        //def session = RequestContextHolder.currentRequestAttributes().getSession()
        String username
        if (inputObject.has(FeatureStringEnum.USERNAME.value)) {
            username = inputObject.getString(FeatureStringEnum.USERNAME.value)
        }
        if (!username) {
            username = SecurityUtils.subject.principal
        }
        if (!username) {
            throw new PermissionException("Unable to find a username to check")
        }


        User user = User.findByUsername(username)
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(user, true)

        if (!userOrganismPreference) {
            userOrganismPreference = UserOrganismPreference.findByUser(user)
        }

        if (!userOrganismPreference) {
            // see if this user has any permissions or an organism . . just grab the first one
            UserOrganismPermission userOrganismPermission = UserOrganismPermission.findByUser(user)
            if(userOrganismPermission){
                organism = userOrganismPermission.organism
            }
            else
            // if not, but we are admin, then just grab the first organism
            if(!userOrganismPermission && isAdmin() && Organism.count>0){
                organism = Organism.list().iterator().next()
            }


            if(organism){
                userOrganismPreference = new UserOrganismPreference(
                        user: user
                        , organism: organism
                        , currentOrganism: true
                        , sequence: Sequence.findByOrganism(organism)
                ).save(insert: true)
            }
            else{
                if(Organism.count>0){
                    throw new PermissionException("User has no access to an organism and/or is not admin")
                }
                else{
                    return null
                }
            }

        }

        organism = userOrganismPreference.organism

        List<PermissionEnum> permissionEnums = getOrganismPermissionsForUser(organism, user)
        PermissionEnum highestValue = isUserAdmin(user) ? PermissionEnum.ADMINISTRATE : findHighestEnum(permissionEnums)

        if (highestValue.rank < requiredPermissionEnum.rank) {
            //return false
            throw new AnnotationException("You have insufficent permissions [${highestValue.display} < ${requiredPermissionEnum.display}] to perform this operation")
        }

        return organism
    }


    User getActiveUser(JSONObject inputObject){

        if (Environment.current == Environment.TEST && !inputObject.containsKey(FeatureStringEnum.USERNAME.value)) {
            return null
        }

        //def session = RequestContextHolder.currentRequestAttributes().getSession()
        String username
        if (inputObject.has(FeatureStringEnum.USERNAME.value)) {
            username = inputObject.getString(FeatureStringEnum.USERNAME.value)
        }
        if (!username) {
            username = SecurityUtils.subject.principal
        }
        if (!username) {
            throw new PermissionException("Unable to find a username to check")
        }


        User user = User.findByUsername(username)
        return user

    }
    /**
     * This method finds the proper username with their proper organism for the current organism.
     *
     * If there are no preferences, that is okay, we'll create one from the permissions.
     *
     * If there are no permissions then the result is the same . . we throw an exception.
     *
     * @param inputObject
     * @param requiredPermissionEnum
     * @return
     */
    Sequence checkPermissions(JSONObject inputObject, PermissionEnum requiredPermissionEnum) {
        Organism organism
        String trackName = null
        if (inputObject.has("track")) {
            trackName = fixTrackHeader(inputObject.track)
        }

        // this is for testing only
        if (Environment.current == Environment.TEST && !inputObject.containsKey(FeatureStringEnum.USERNAME.value)) {
            Sequence sequence = trackName ? Sequence.findByName(trackName) : null
            return sequence
        }

        User user = getActiveUser(inputObject)

        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(user, true)

        if (!userOrganismPreference) {
            userOrganismPreference = UserOrganismPreference.findByUser(user)
        }

        if (!userOrganismPreference) {
            // find a random organism based on sequence
            Sequence sequence = Sequence.findByName(trackName)
            organism  = sequence.organism

            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
            ).save(insert: true)
        }

        organism = userOrganismPreference.organism

        List<PermissionEnum> permissionEnums = getOrganismPermissionsForUser(organism, user)
        PermissionEnum highestValue = isUserAdmin(user) ? PermissionEnum.ADMINISTRATE : findHighestEnum(permissionEnums)

        if (highestValue.rank < requiredPermissionEnum.rank) {
            println "highest value ${highestValue}"
            println "required permission ${requiredPermissionEnum}"
            println "highest value display ${highestValue.display}"
            println "perm dispaly ${requiredPermissionEnum.display}"
            throw new AnnotationException("You have insufficent permissions [${highestValue.display} < ${requiredPermissionEnum.display}] to perform this operation")
        }
        return userOrganismPreference.sequence ?: null
    }

    Boolean checkPermissions(PermissionEnum requiredPermissionEnum) {
        try {
            Session session = SecurityUtils.subject.getSession(false)
            Map<String, Integer> permissions = session.getAttribute(FeatureStringEnum.PERMISSIONS.getValue());
            Integer permission = permissions.get(SecurityUtils.subject.principal)
            PermissionEnum sessionPermissionsEnum = isAdmin() ? PermissionEnum.ADMINISTRATE : PermissionEnum.getValueForOldInteger(permission)

            if (sessionPermissionsEnum == null) {
                log.warn "No permissions found in session"
                return false
            }

            if (sessionPermissionsEnum.rank < requiredPermissionEnum.rank) {
                log.warn "Permission required ${requiredPermissionEnum.display} vs found ${sessionPermissionsEnum.display}"
                return false
            }
            return true
        } catch (e) {
            log.error "Error checking permissions from session ${e}"
            return false
        }

    }

    def checkPermissions(JSONObject jsonObject, Organism organism, PermissionEnum requiredPermissionEnum) {

        if (Environment.current == Environment.TEST && !jsonObject.containsKey(FeatureStringEnum.USERNAME.value)) {
            return true
        }

        //def session = RequestContextHolder.currentRequestAttributes().getSession()
        String username = jsonObject.getString(FeatureStringEnum.USERNAME.value)


        User user = User.findByUsername(username)

        List<PermissionEnum> permissionEnums = getOrganismPermissionsForUser(organism, user)
        PermissionEnum highestValue = isUserAdmin(user) ? PermissionEnum.ADMINISTRATE : findHighestEnum(permissionEnums)

        if (highestValue.rank < requiredPermissionEnum.rank) {
            //return false
            throw new AnnotationException("You have insufficent permissions [${highestValue.display} < ${requiredPermissionEnum.display}] to perform this operation")
        }

        return highestValue
    }

    Boolean hasPermissions(JSONObject jsonObject, PermissionEnum permissionEnum) {
        // not sure if permissions with translate through or not
        Session session = SecurityUtils.subject.getSession(false)
        if (!session) {
            // login with jsonObject tokens
            log.debug "creating session with found json object ${jsonObject.username}, ${jsonObject.password as String}"
            def authToken = new UsernamePasswordToken(jsonObject.username, jsonObject.password as String)
            try {
                Subject subject = SecurityUtils.getSubject();
                session = subject.getSession(true);
                subject.login(authToken)
                if (!subject.authenticated) {
                    log.error "Failed to authenticate user ${jsonObject.username}"
                    return false
                }
            } catch (Exception ae) {
                log.error("Problem authenticating: " + ae.fillInStackTrace())
                return false
            }
        }
        else if (!jsonObject.username) {
            jsonObject.username = session.getAttribute(FeatureStringEnum.USERNAME.value)
        }


        Organism organism = getCurrentOrganismPreference()?.organism
        log.debug "passing in an organism ${jsonObject.organism}"
        if (jsonObject.organism) {
            Organism thisOrganism = null
            try {
                thisOrganism = Organism.findById(jsonObject.organism as Long)
            } catch (npe) {
                // obviously not a long type
            }
            if (!thisOrganism) {
                thisOrganism = Organism.findByCommonName(jsonObject.organism)
            }
            if (!thisOrganism) {
                thisOrganism = Organism.findByAbbreviation(jsonObject.organism)
            }
            if(organism.id!=thisOrganism.id){
                log.debug "switching organism from ${organism.commonName} -> ${thisOrganism.commonName}"
                organism = thisOrganism
            }
            log.debug "final organism ${organism.commonName}"
            preferenceService.setCurrentOrganism(getCurrentUser(), organism)
        }

        return checkPermissions(jsonObject,organism,permissionEnum)

    }

    UserOrganismPreference getCurrentOrganismPreference(){
        User currentUser = getCurrentUser()
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(currentUser, true)
        if (userOrganismPreference) {
            return userOrganismPreference
        }

        // find another one
        userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(currentUser, false)
        if (userOrganismPreference) {
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true)
            return userOrganismPreference
        }

        def organisms = getOrganisms(currentUser)
        if(!organisms){
            if(isAdmin()){
                return null
            }
            else{
                throw new PermissionException("User does not have permission for any organisms.")
            }
        }
        Organism organism = organisms?.iterator()?.next()
        userOrganismPreference = new UserOrganismPreference(
                user: currentUser
                , currentOrganism: true
                , organism: organism
        ).save(insert: true, flush: true)
        return userOrganismPreference
    }

    Boolean hasAnyPermissions(User user) {

        Map<String,Integer> permissions = getPermissionsForUser(user)
        if(!permissions){
            return false
        }

        for(Integer value : permissions.values()){
            if(value > PermissionEnum.NONE.value){
                return true
            }
        }

        return false
    }
}
