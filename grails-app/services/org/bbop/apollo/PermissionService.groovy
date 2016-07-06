package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import grails.util.Environment
import org.apache.commons.lang.RandomStringUtils
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.session.Session
import org.apache.shiro.subject.Subject
import org.bbop.apollo.authenticator.AuthenticatorService
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import javax.servlet.http.HttpServletRequest

@Transactional
class PermissionService {

    def preferenceService
    def configWrapperService
    def grailsApplication


    def remoteUserAuthenticatorService
    def usernamePasswordAuthenticatorService

    boolean isUserAdmin(User user) {
        if (user != null) {
            for (Role role in user.roles) {
                if (role.name == UserService.ADMIN) {
                    return true
                }
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
        if (isUserAdmin(user)) {
            return Organism.listOrderByCommonName()
        }
        Set<Organism> organismList = new HashSet<>()
        for (UserOrganismPermission userPermission in UserOrganismPermission.findAllByUser(user)) {
            if (userPermission.permissions) {
                organismList.add(userPermission.organism)
            }
        }
        for (UserGroup userGroup in user?.userGroups) {
            organismList.addAll(getOrganismsForGroup(userGroup))
        }
        return organismList
    }

    List<Organism> getOrganismsForGroup(UserGroup group) {
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


    static Collection<PermissionEnum> mergeOrganismPermissions(Collection<PermissionEnum> permissionsA, Collection<PermissionEnum> permissionsB) {
        Set<PermissionEnum> permissionEnums = new HashSet<>()
        permissionEnums.addAll(permissionsA)

        for (PermissionEnum permissionEnum in permissionsB) {
            permissionEnums.add(permissionEnum)
        }

        return permissionEnums
    }


    List<PermissionEnum> getOrganismPermissionsForUser(Organism organism, User user) {
        Set<PermissionEnum> permissions = new HashSet<>()
        if (isUserAdmin(user)) {
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
        if (user != null) {
            for (UserGroup group in user.userGroups) {
                permissions = mergeOrganismPermissions(permissions, getOrganismPermissionsForUserGroup(organism, group))
            }
        } else {
            permissions.add(PermissionEnum.NONE)
        }


        return permissions as List

    }

    List<PermissionEnum> getOrganismPermissionsForUserGroup(Organism organism, UserGroup userGroup) {
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


    void setOrganismPermissionsForUser(List<PermissionEnum> permissions, Organism organism, User user, String token) {

        UserOrganismPermission userOrganismPermission = UserOrganismPermission.findByOrganismAndUser(organism, user)
        if (!userOrganismPermission) {
            userOrganismPermission = new UserOrganismPermission(
                    organism: organism
                    , permissions: generatePermissionString(permissions)
                    , user: user
                    , token: token
            ).save(insert: true)
        } else {
            userOrganismPermission.permissions = generatePermissionString(permissions)
            userOrganismPermission.save()
        }

    }

    void setOrganismPermissionsForUserGroup(List<PermissionEnum> permissions, Organism organism, UserGroup group, String token) {

        GroupOrganismPermission groupOrganismPermission = GroupOrganismPermission.findByOrganismAndGroup(organism, group)
        if (!groupOrganismPermission) {
            groupOrganismPermission = new GroupOrganismPermission(
                    organism: organism
                    , permissions: generatePermissionString(permissions)
                    , group: group
                    , token: token
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

    /**
     * Get all of the highest organism permissions for a user
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

    JSONObject copyValue(FeatureStringEnum featureStringEnum, JSONObject fromJSON, JSONObject toJSON) {
        if (fromJSON.containsKey(featureStringEnum.value)) {
            toJSON.put(featureStringEnum.value, fromJSON.getString(featureStringEnum.value))
        } else {
            log.info "No ${featureStringEnum.value} to copy from ${fromJSON}"
        }
        return toJSON
    }

    /**
     * Copies values relevant to request
     * @param fromJSON
     * @param toJSON
     * @return
     */
    JSONObject copyRequestValues(JSONObject fromJSON, JSONObject toJSON) {
        copyValue(FeatureStringEnum.USERNAME, fromJSON, toJSON)
        copyValue(FeatureStringEnum.CLIENT_TOKEN, fromJSON, toJSON)
        copyValue(FeatureStringEnum.ORGANISM, fromJSON, toJSON)
        return toJSON
    }

    def getOrganismsForCurrentUser(JSONObject jsonObject) {
        User thisUser = getCurrentUser(jsonObject)
        if (thisUser) {
            return getOrganisms(thisUser) as List<Organism>
        }
        return []
    }

    public static String getSequenceNameFromInput(JSONObject inputObject) {
        String trackName = null
        if (inputObject.has(FeatureStringEnum.SEQUENCE.value)) {
            trackName = inputObject.sequence
        }
        if (inputObject.has(FeatureStringEnum.TRACK.value)) {
            trackName = inputObject.track
        }
        return trackName
    }

    // get current user from session or input object
    User getCurrentUser(JSONObject inputObject = new JSONObject()) {
        if (Environment.current == Environment.TEST && !inputObject.containsKey(FeatureStringEnum.USERNAME.value)) {
            return null
        }

        String username
        if (inputObject?.has(FeatureStringEnum.USERNAME.value)) {
            username = inputObject.getString(FeatureStringEnum.USERNAME.value)
        }
        if (!username) {
            username = SecurityUtils.subject.principal
        }
        if (!username) {
            return null;
        }

        User user = User.findByUsername(username)
        return user

    }


    Organism getOrganismFromInput(JSONObject inputObject) {

        if (inputObject.has(FeatureStringEnum.ORGANISM.value)) {
            String organismString = inputObject.getString(FeatureStringEnum.ORGANISM.value)
            Organism organism = Organism.findByCommonNameIlike(organismString)
            if (organism) {
                log.debug "return organism ${organism} by name ${organismString}"
                return organism
            }
            if (!organism) {
                organism = Organism.findById(organismString as Long);
            }
            if (organism) {
                log.debug "return organism ${organism} by ID ${organismString}"
                return organism
            } else {
                log.info "organism not found ${organismString}"
            }
        }
        return null
    }

    /**
     * This method finds the proper username with their proper organism for the current organism when including the track name.
     *
     * @param inputObject
     * @param requiredPermissionEnum
     * @return
     */
    Sequence checkPermissions(JSONObject inputObject, PermissionEnum requiredPermissionEnum) {
        Organism organism
        String trackName = getSequenceNameFromInput(inputObject)

        // this is for testing only
        if (Environment.current == Environment.TEST && !inputObject.containsKey(FeatureStringEnum.USERNAME.value)) {
            Sequence sequence = trackName ? Sequence.findByName(trackName) : null
            return sequence
        }

        User user = getCurrentUser(inputObject)
        organism = getOrganismFromInput(inputObject)

        if (!organism) {
            organism = preferenceService.getOrganismFromPreferences(user, trackName, inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
        }

        Sequence sequence
        if (!trackName) {
            sequence = UserOrganismPreference.findByClientTokenAndOrganism(trackName, organism, [max: 1, sort: "lastUpdated", order: "desc"])?.sequence
        } else {
            sequence = Sequence.findByNameAndOrganism(trackName, organism)
        }

        if (!sequence && organism) {
            sequence = Sequence.findByOrganism(organism, [max: 1, sort: "end", order: "desc"])
        }

        List<PermissionEnum> permissionEnums = getOrganismPermissionsForUser(organism, user)
        PermissionEnum highestValue = isUserAdmin(user) ? PermissionEnum.ADMINISTRATE : findHighestEnum(permissionEnums)

        if (highestValue.rank < requiredPermissionEnum.rank) {
            log.debug "highest value ${highestValue}"
            log.debug "required permission ${requiredPermissionEnum}"
            log.debug "highest value display ${highestValue.display}"
            log.debug "permission display ${requiredPermissionEnum.display}"
            throw new AnnotationException("You have insufficient permissions [${highestValue.display} < ${requiredPermissionEnum.display}] to perform this operation")
        }
        return sequence
    }

    Boolean checkPermissions(PermissionEnum requiredPermissionEnum) {
        try {
            Session session = SecurityUtils.subject.getSession(false)
            if (session) {
                Map<String, Integer> permissions = session.getAttribute(FeatureStringEnum.PERMISSIONS.getValue());
                if (permissions) {
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
                } else {
                    log.debug "No permissions found on session"
                }
            } else {
                log.debug "No session found"
                return false
            }

        } catch (e) {
            log.error "Error checking permissions from session ${e}"
            e.printStackTrace()
            return false
        }

    }

    PermissionEnum checkPermissions(JSONObject jsonObject, Organism organism, PermissionEnum requiredPermissionEnum) {

        //def session = RequestContextHolder.currentRequestAttributes().getSession()
        String username = jsonObject.getString(FeatureStringEnum.USERNAME.value)


        User user = User.findByUsername(username)

        List<PermissionEnum> permissionEnums = getOrganismPermissionsForUser(organism, user)
        PermissionEnum highestValue = isUserAdmin(user) ? PermissionEnum.ADMINISTRATE : findHighestEnum(permissionEnums)

        if (highestValue.rank < requiredPermissionEnum.rank) {
            //return false
            throw new AnnotationException("You have insufficient permissions [${highestValue.display} < ${requiredPermissionEnum.display}] to perform this operation")
        }

        return highestValue
    }

    /**
     * This method validates after logged in, so it *should* not need a special authenticator.
     * @param jsonObject
     * @return
     */
    JSONObject validateSessionForJsonObject(JSONObject jsonObject) {
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
        } else if (!jsonObject.username && SecurityUtils?.subject?.principal) {
            jsonObject.username = SecurityUtils?.subject?.principal
        } else if (!jsonObject.username && session.attributeKeys.contains(FeatureStringEnum.USERNAME.value)) {
            jsonObject.username = session.getAttribute(FeatureStringEnum.USERNAME.value)
        }
        return jsonObject
    }

    /**
     * If a user exists and is a admin (not just for organism), then check, otherwise a regular user is still a valid user.
     * @param jsonObject
     * @param permissionEnum
     * @return
     */
    Boolean hasGlobalPermissions(JSONObject jsonObject, PermissionEnum permissionEnum) {
        jsonObject = validateSessionForJsonObject(jsonObject)
        User user = User.findByUsername(jsonObject.username)
        if (!user) {
            log.error("User ${jsonObject.username} for ${jsonObject as JSON} does not exist in the database.")
            return false
        }
        if (permissionEnum.rank > PermissionEnum.ADMINISTRATE.rank) {
            return isUserAdmin(user)
        }
        return true
    }

    Boolean hasPermissions(JSONObject jsonObject, PermissionEnum permissionEnum) {
        if (!hasGlobalPermissions(jsonObject, permissionEnum)) {
            log.info("User for ${jsonObject} lacks permissions ${permissionEnum.display}")
            return false
        }
        String clientToken = jsonObject.getString(FeatureStringEnum.CLIENT_TOKEN.value)

        Organism organism = preferenceService.getCurrentOrganismPreference(clientToken)?.organism
        log.debug "passing in an organism ${jsonObject.organism}"
        if (jsonObject.organism) {
            Organism thisOrganism = null
            try {
                thisOrganism = Organism.findById(jsonObject.organism as Long)
            } catch (npe) {
                // obviously not a long type
            }
            if (!thisOrganism) {
                thisOrganism = Organism.findByCommonNameIlike(jsonObject.organism)
            }
            if (!thisOrganism) {
                thisOrganism = Organism.findByAbbreviation(jsonObject.organism)
            }
            if (organism.id != thisOrganism.id) {
                log.debug "switching organism from ${organism.commonName} -> ${thisOrganism.commonName}"
                organism = thisOrganism
            }
            log.debug "final organism ${organism.commonName}"
            preferenceService.setCurrentOrganism(getCurrentUser(), organism, clientToken)
        }

        return checkPermissions(jsonObject, organism, permissionEnum)

    }


    Boolean hasAnyPermissions(User user) {

        Map<String, Integer> permissions = getPermissionsForUser(user)
        if (!permissions) {
            return false
        }

        for (Integer value : permissions.values()) {
            if (value > PermissionEnum.NONE.value) {
                return true
            }
        }

        return false
    }

    PermissionEnum findHighestOrganismPermissionForCurrentUser(Organism organism) {
        findHighestOrganismPermissionForUser(organism, currentUser)
    }

    PermissionEnum findHighestOrganismPermissionForUser(Organism organism, User user) {
        List<PermissionEnum> permissionEnums = getOrganismPermissionsForUser(organism, user)

        PermissionEnum highestEnum = PermissionEnum.NONE
        for (PermissionEnum permissionEnum : permissionEnums) {
            if (permissionEnum.rank > highestEnum.rank) {
                highestEnum = permissionEnum
            }
        }
        return highestEnum
    }

    Boolean userHasOrganismPermission(Organism organism, PermissionEnum permissionEnum) {
        return findHighestOrganismPermissionForCurrentUser(organism).rank >= permissionEnum.rank
    }

    def authenticateWithToken(UsernamePasswordToken usernamePasswordToken, HttpServletRequest request) {

        def authentications = configWrapperService.authentications
        for (auth in authentications) {
            if (auth.active) {
                println "class name ${auth.className}"
                def authenticationService
                if(remoteUserAuthenticatorService == auth.className ){
                    authenticationService = remoteUserAuthenticatorService
                }
                else
                if(usernamePasswordAuthenticatorService == auth.className ){
                    authenticationService = usernamePasswordAuthenticatorService
                }
                else{
                    log.error("No authentication service for ${auth.className}")
                    // better to return false if mis-configured
                    return false
                }

                if(usernamePasswordToken){
                    if (authenticationService.authenticate(usernamePasswordToken, request)) {
                        log.info "Authenticated user ${usernamePasswordToken.username} using ${auth.name}"
                        return true
                    }
                }
                else{
                    if (authenticationService.authenticate(request)) {
                        log.info "Authenticated user ${auth.name}"
                        return true
                    }
                }
            }
        }
        log.warn "Failed to authenticate user"
        return false
    }

    /**
     * Verifies that "userId" matches userName for the secured session user
     * @param jsonObject
     * @return
     */
    Boolean sameUser(JSONObject jsonObject, HttpServletRequest request) {
        // not sure if permissions with translate through or not
        Session session = SecurityUtils.subject.getSession(false)
        if (!session) {
            // login with jsonObject tokens
            log.debug "creating session with found json object ${jsonObject.username}, ${jsonObject.password as String}"
            UsernamePasswordToken authToken = new UsernamePasswordToken(jsonObject.username, jsonObject.password as String)
            authenticateWithToken(authToken, request)
        } else if (!jsonObject.username && SecurityUtils?.subject?.principal) {
            jsonObject.username = SecurityUtils?.subject?.principal
        } else if (!jsonObject.username && session.attributeKeys.contains(FeatureStringEnum.USERNAME.value)) {
            jsonObject.username = session.getAttribute(FeatureStringEnum.USERNAME.value)
        }
        if (jsonObject.username) {
            User user = User.findByUsername(jsonObject.username)
            return user?.id == jsonObject.userId
        }
        return false
    }

    @NotTransactional
    def getInsufficientPermissionMessage(PermissionEnum permissionEnum) {
        if (permissionEnum == PermissionEnum.ADMINISTRATE) {
            return "Must have permissions ${PermissionEnum.ADMINISTRATE.display}."
        } else {
            return "Must have permissions ${permissionEnum.display} or better."
        }
    }

    @NotTransactional
    String handleToken(GrailsParameterMap params, JSONObject dataObject) {
        if (params.containsKey(FeatureStringEnum.CLIENT_TOKEN.value)) {
            dataObject.put(FeatureStringEnum.CLIENT_TOKEN.value, params.get(FeatureStringEnum.CLIENT_TOKEN.value))
        } else {
            dataObject.put(FeatureStringEnum.CLIENT_TOKEN.value, RandomStringUtils.random(20))
        }
        return dataObject.get(FeatureStringEnum.CLIENT_TOKEN.value)
    }

    @NotTransactional
    JSONObject handleInput(HttpServletRequest request, GrailsParameterMap params) {
        JSONObject payloadJson = new JSONObject()
        if (request.JSON) {
            payloadJson = request.JSON as JSONObject
        }
        if (!payloadJson || payloadJson.size() == 0) {
            if (params.data) {
                payloadJson = JSON.parse(params.data.toString()) as JSONObject
            } else {
                payloadJson = params as JSONObject
            }
        }
        handleToken(params, payloadJson)
        return payloadJson
    }
}
