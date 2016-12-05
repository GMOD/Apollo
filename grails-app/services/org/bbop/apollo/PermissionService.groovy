package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.session.Session
import org.apache.shiro.subject.Subject
import org.bbop.apollo.gwt.shared.ClientTokenGenerator
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
    def assemblageService

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

    /**
     * Have to handle the cases when it is a simple "sequence' or "track" or contains multiple sequences.
     *{"projection":"None", "padding":50, "referenceTrack":"Official Gene Set v3.2", "sequences":[{"name":"Group5.7"},{"name":"Group9.2"}]}:1..53600 (53.6 Kb)
     *
     * @param inputObject
     * @return
     */
    @NotTransactional
    public Map<String, Integer> getSequenceNameFromInput(JSONObject inputObject) {
        Map<String, Integer> sequenceMap = new HashMap<>()
        int counter = 0
        if (inputObject.has(FeatureStringEnum.SEQUENCE_LIST.value)) {
            inputObject.sequenceList.each { it ->
                if (!sequenceMap.containsKey(it.name)) {
                    sequenceMap.put(it.name, counter)
                    ++counter
                }
            }
        } else if (inputObject.has(FeatureStringEnum.TRACK.value)) {
            if (AssemblageService.isProjectionString(inputObject.track.toString())) {
//                JSONObject sequenceObject = inputObject.track
                def track = inputObject.track
                if (track instanceof String && track.startsWith("{")) {
                    track = JSON.parse(inputObject.track) as JSONObject
                } else if (track instanceof String && track.startsWith("[")) {
                    track = new JSONObject()
                    track.sequenceList = JSON.parse(inputObject.track as String) as JSONArray
                } else if (track.sequenceList instanceof String) {
                    track = (JSONObject) track
                    track.sequenceList = JSON.parse(track.sequenceList) as JSONArray
                }
                track.sequenceList.each { it ->
                    sequenceMap.put(it.name, counter)
                    ++counter
                }
            } else if (inputObject.track.contains(FeatureStringEnum.SEQUENCE_LIST.value)) {
                JSONObject sequenceObject = JSON.parse(inputObject.track) as JSONObject
                sequenceObject.sequenceList.each { it ->
                    sequenceMap.put(it.name, counter)
                    ++counter
                }
            } else {
                sequenceMap.put(inputObject.track, counter)
            }
        }
        return sequenceMap
    }

    // get current user from session or input object
    User getCurrentUser(JSONObject inputObject = new JSONObject()) {
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
    Assemblage checkPermissions(JSONObject inputObject, PermissionEnum requiredPermissionEnum) {
        Organism organism

        Map<String, Integer> sequenceStrings = getSequenceNameFromInput(inputObject)
        String trackName = null
        if (sequenceStrings) {
            trackName = sequenceStrings.keySet().first()
        }


        User user = getCurrentUser(inputObject)
        organism = preferenceService.getOrganismFromInput(inputObject)

        if (!organism) {
            organism = preferenceService.getOrganismFromPreferences(user, trackName, inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
        }

//        Sequence sequence = null
        Assemblage assemblage = null
        if (!trackName) {
//            sequence = UserOrganismPreference.findByClientTokenAndOrganism(trackName, organism, [max: 1, sort: "lastUpdated", order: "desc"])?.sequence
            assemblage = UserOrganismPreference.findByClientTokenAndOrganism(trackName, organism, [max: 1, sort: "lastUpdated", order: "desc"])?.assemblage
        }
//        else {
//            sequence = Sequence.findByNameAndOrganism(trackName, organism)
//        }
//        if (!sequence && organism) {
//            sequence = Sequence.findByOrganism(organism, [max: 1, sort: "end", order: "desc"])
//        }

        List<PermissionEnum> permissionEnums = getOrganismPermissionsForUser(organism, user)
        PermissionEnum highestValue = isUserAdmin(user) ? PermissionEnum.ADMINISTRATE : findHighestEnum(permissionEnums)

        if (highestValue.rank < requiredPermissionEnum.rank) {
            log.debug "highest value ${highestValue}"
            log.debug "required permission ${requiredPermissionEnum}"
            log.debug "highest value display ${highestValue.display}"
            log.debug "permission display ${requiredPermissionEnum.display}"
            throw new AnnotationException("You have insufficient permissions [${highestValue.display} < ${requiredPermissionEnum.display}] to perform this operation")
        }

//        if (orderedSequences) {
        if (inputObject.track instanceof String) {
            if (inputObject.track.startsWith("{") || inputObject.track.startsWith("[")) {
                JSONArray sequenceListArray = inputObject.track.startsWith("{") ? (JSON.parse(inputObject.track) as JSONObject).sequenceList : (JSON.parse(inputObject.track) as JSONArray)
                List<String> sequenceList = []
                for (int i = 0; i < sequenceListArray.size(); i++) {
                    sequenceList << sequenceListArray.getJSONObject(i).name
                }
                if (sequenceList) {
                    def sequenceObjects = Sequence.findAllByNameInList(sequenceList)
                    assemblage = assemblageService.generateAssemblageForSequence(sequenceObjects.toArray(new Sequence[sequenceObjects.size()]))
                }
                println "assemblage sequence list ${assemblage} vs ${sequenceList} and ${inputObject as JSON}"
            }
            if (inputObject.track.startsWith("[")) {
                JSONArray sequenceListArray = (JSON.parse(inputObject.track) as JSONArray)
                List<String> sequenceList = []
                for (int i = 0; i < sequenceListArray.size(); i++) {
                    sequenceList << sequenceListArray.getJSONObject(i).name
                }
                if (sequenceList) {
                    def sequenceObjects = Sequence.findAllByNameInList(sequenceList)
                    assemblage = assemblageService.generateAssemblageForSequence(sequenceObjects.toArray(new Sequence[sequenceObjects.size()]))
                }
                println "assemblage sequence list ${assemblage} vs ${sequenceList} and ${inputObject as JSON}"
            } else {
                Sequence sequence = Sequence.findByName(inputObject.track)
                if (sequence) {
                    assemblage = assemblageService.generateAssemblageForSequence(sequence)
                }
                println "has a sequence: ${sequence} for ${inputObject.track}"
            }
            if (!assemblage) {
                log.error("Invalid sequence name: " + inputObject.track)
            }
        } else if (inputObject.track instanceof JSONObject) {
            println "NO Track assemblage ${assemblage} and ${inputObject.track as JSON}"
            if (!inputObject.containsValue(FeatureStringEnum.ORGANISM.value)) {
                inputObject.put(FeatureStringEnum.ORGANISM.value, organism.id)
            }
            copyRequestValues(inputObject, inputObject.track)
            assemblage = assemblageService.convertJsonToAssemblage(inputObject.getJSONObject(FeatureStringEnum.TRACK.value))
        }
        String clientToken = inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value)
        if (assemblage) {
            preferenceService.setCurrentAssemblage(user, assemblage, clientToken)
            if ((inputObject.track instanceof JSONObject) && inputObject?.track?.projection) {
                assemblage.projection = inputObject.track.projection
                assemblage.padding = inputObject.track?.padding
//                assemblage.referenceTrack = inputObject.track?.referenceTrack
                println "save here?"
                assemblage.save(flush: true)
            }
            if (user) {
                user.addToAssemblages(assemblage)
            }
        }
        return assemblage
//        }
//        return null
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
            }

        } catch (e) {
            log.error "Error checking permissions from session ${e}"
            e.printStackTrace()
            return false
        }
        return false
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
            if(!jsonObject.username){
                log.error "Username not supplied so can not authenticate."
                jsonObject.error_message = "Username not supplied so can not authenticate."
                return jsonObject
            }
            def authToken = new UsernamePasswordToken(jsonObject.username, jsonObject.password as String)
            try {
                Subject subject = SecurityUtils.getSubject();
                session = subject.getSession(true);

                subject.login(authToken)
                if (!subject.authenticated) {
                    log.warn "Failed to authenticate user ${jsonObject.username}"
                    jsonObject.error_message = "Failed to authenticate user ${jsonObject.username}"
                    return jsonObject
                }
            } catch (Exception ae) {
                log.error("Problem authenticating: " + ae.fillInStackTrace())
                jsonObject.error_message = "Problem authenticating: " + ae.fillInStackTrace()
                return jsonObject
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
            log.error("User ${jsonObject.username} does not exist in the database.")
            return false
        }

        // if the rank required is less than administrator than ask if they are an administrator
        if (PermissionEnum.ADMINISTRATE.rank < permissionEnum.rank ) {
            return isUserAdmin(user)
        }
        return true
    }

    Boolean hasPermissions(JSONObject jsonObject, PermissionEnum permissionEnum) {
        if (!hasGlobalPermissions(jsonObject, permissionEnum)) {
            log.info("User lacks permissions ${permissionEnum.display}")
            return false
        }
        String clientToken = jsonObject.getString(FeatureStringEnum.CLIENT_TOKEN.value)

        Organism organism = getOrganismFromInput(jsonObject)
        if(clientToken==FeatureStringEnum.IGNORE.value){
            organism = getOrganismFromInput(jsonObject)
        }

        organism = organism ?: preferenceService.getCurrentOrganismPreference(clientToken)?.organism
        // don't set the preferences if it is coming off a script
        if(clientToken!=FeatureStringEnum.IGNORE.value){
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
                log.info "Authenticating with ${auth.className}"
                def authenticationService
                if ("remoteUserAuthenticatorService" == auth.className) {
                    authenticationService = remoteUserAuthenticatorService
                } else if ("usernamePasswordAuthenticatorService" == auth.className) {
                    authenticationService = usernamePasswordAuthenticatorService
                } else {
                    log.error("No authentication service for ${auth.className}")
                    // better to return false if mis-configured
                    return false
                }

                if (usernamePasswordToken) {
                    if (authenticationService.authenticate(usernamePasswordToken, request)) {
                        log.info "Authenticated user ${usernamePasswordToken.username} using ${auth.name}"
                        return true
                    }
                } else {
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

    /**
     * we prefer the param over the dataObject one I guess
     * @param params
     * @param dataObject
     * @return
     */
    @NotTransactional
    String handleToken(GrailsParameterMap params, JSONObject dataObject) {
        // replace the dataObject either way
        if (params.containsKey(FeatureStringEnum.CLIENT_TOKEN.value)) {
            dataObject.put(FeatureStringEnum.CLIENT_TOKEN.value, params.get(FeatureStringEnum.CLIENT_TOKEN.value))
        }
        // if the dataObject doesn't contain nor does the param, then we create it
        if(!dataObject.containsKey(FeatureStringEnum.CLIENT_TOKEN.value) ){
            // client should generate token, not server
//            dataObject.put(FeatureStringEnum.CLIENT_TOKEN.value,ClientTokenGenerator.generateRandomString())
            dataObject.put(FeatureStringEnum.CLIENT_TOKEN.value,FeatureStringEnum.IGNORE.value)
        }
        String clientToken = dataObject.get(FeatureStringEnum.CLIENT_TOKEN.value)
        if(clientToken == FeatureStringEnum.IGNORE.value && !dataObject.containsKey(FeatureStringEnum.ORGANISM.value)){
            throw new RuntimeException("Must contain 'organism' value if we ignore the clientToken")
        }
        return clientToken
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
