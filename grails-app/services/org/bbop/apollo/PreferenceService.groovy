package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.SequenceLocationDTO
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class PreferenceService {

    def permissionService
    def assemblageService

    final Integer PREFERENCE_SAVE_DELAY_SECONDS = 30  // saves every 30 seconds
    // enqueue to store save
    private Map<SequenceLocationDTO, Date> saveSequenceLocationMap = [:]
    // set of client locations
    private Set<String> currentlySavingLocation = new HashSet<>()



    Organism getSessionOrganism(String clientToken) {
        JSONObject preferenceObject = getSessionPreferenceObject(clientToken)
        if (preferenceObject) {
            def organismId = preferenceObject.organism.id as Long
            return Organism.get(organismId) ?: Organism.findById(organismId)
        }
        return null
    }


    UserOrganismPreference getSessionPreference(String clientToken) {
        JSONObject preferenceObject = getSessionPreferenceObject(clientToken)
        if (preferenceObject) {
            def preferenceId = preferenceObject.id as Long
            return UserOrganismPreference.get(preferenceId) ?: UserOrganismPreference.findById(preferenceId)
        }
        return null
    }

    JSONObject getSessionPreferenceObject(String clientToken) {
        try {
            Session session = SecurityUtils.subject.getSession(false)
            if (session) {
                // should be client_token , JSONObject
                String preferenceString = session.getAttribute(FeatureStringEnum.PREFERENCE.getValue() + "::" + clientToken)?.toString()
                if (!preferenceString) return null
                return JSON.parse(preferenceString) as JSONObject
            } else {
                log.debug "No session found"
            }
        } catch (e) {
            log.debug "faild to get the gession preference objec5 ${e}"
        }
        return null
    }

    UserOrganismPreference setSessionPreference(String clientToken, UserOrganismPreference userOrganismPreference) {
        Session session = SecurityUtils.subject.getSession(false)
        if (session) {
            // should be client_token , JSONObject
            String preferenceString = (userOrganismPreference as JSON).toString()
            session.setAttribute(FeatureStringEnum.PREFERENCE.getValue() + "::" + clientToken, preferenceString)
        } else {
            log.warn "No session found"
        }
        return userOrganismPreference
    }


    Organism getCurrentOrganismForCurrentUser(String clientToken) {
        log.debug "PS: getCurrentOrganismForCurrentUser ${clientToken}"
        Organism organism = getSessionOrganism(clientToken)
        log.debug "found organism in session ${organism} so returning"
        if (organism) return organism
        if (permissionService.currentUser == null) {
            println "no user, so just using client token"
            organism = getOrganismForTokenInDB(clientToken)
            return organism
        } else {
            UserOrganismPreference userOrganismPreference = getCurrentOrganismPreference(permissionService.currentUser, null, clientToken)
            return setSessionPreference(clientToken, userOrganismPreference)?.organism
        }
    }

    Organism getOrganismFromInput(JSONObject inputObject) {

        if (inputObject.has(FeatureStringEnum.ORGANISM.value)) {
            String organismString = inputObject.getString(FeatureStringEnum.ORGANISM.value)
            Organism organism = getOrganismForToken(organismString)
            if (organism) {
                log.debug "return organism ${organism} by ID ${organismString}"
                return organism
            } else {
                log.info "organism not found ${organismString}"
            }
        }
        return null
    }

    Organism getOrganismForTokenInDB(String token) {
        log.debug "token for org ${token}"
        if (token.isLong()) {
            log.debug "is long "
            return Organism.findById(Long.parseLong(token))
        } else {
            log.debug "is NOT long "
            return Organism.findByCommonNameIlike(token)
        }
    }

    Organism getOrganismForToken(String token) {
        Organism organism = getSessionOrganism(token)
        if (organism) {
            return organism
        } else {
            return getOrganismForTokenInDB(token)
        }
    }

    /**
     * Get the current user preference.
     * If no preference, then set one
     * @param user
     * @return
     */
    String getCurrentSequence(User user) {
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByCurrentOrganismAndUser(true, user)
        if (!userOrganismPreference) {
            userOrganismPreference = UserOrganismPreference.findByUser(user)

            if (!userOrganismPreference) {
                Iterator i = permissionService.getOrganisms(user).iterator();
                if (i.hasNext()) {
                    Organism organism = i.next()
                    userOrganismPreference = new UserOrganismPreference(
                            user: user
                            , organism: organism
                            , assemblage: Assemblage.findByOrganism(organism)
                            , currentOrganism: true
                    ).save()
                } else {
                    throw new PermissionException("User has no access to any organisms!")
                }
            }

            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true)
        }
        Assemblage assemblage = userOrganismPreference.assemblage
        String sequenceList = assemblage.sequenceList

        return sequenceList
    }

    def setCurrentOrganism(User user, Organism organism, String clientToken) {
        UserOrganismPreference userOrganismPreference = getCurrentOrganismPreference(user, null, clientToken)
        userOrganismPreference.organism = organism
        setSessionPreference(clientToken, userOrganismPreference)
//        storePreferenceInDB(userOrganismPreference)
        setCurrentOrganismInDB(user, organism, clientToken)
    }

    def setCurrentOrganismInDB(User user, Organism organism, String clientToken) {
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientToken(user, organism, clientToken, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), user, clientToken)
        }

        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (!userOrganismPreference) {
            def sequences = Sequence.findAllByOrganism(organism)
            if (!sequences) {
                log.warn "Sequences not loaded for organism ${organism.commonName}, so not setting preference for user ${user.username}."
                return
            }
            Assemblage assemblage = Assemblage.findByOrganism(organism) ?: assemblageService.generateAssemblageForSequence(sequences.first())
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , assemblage: assemblage
                    , clientToken: clientToken
            ).save(flush: true, insert: true)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.save(flush: true, insert: false)
        }
        setOtherCurrentOrganismsFalseInDB(userOrganismPreference, user, clientToken)
    }

    protected static
    def setOtherCurrentOrganismsFalseInDB(UserOrganismPreference userOrganismPreference, User user, String clientToken) {
        UserOrganismPreference.executeUpdate(
                "update UserOrganismPreference  pref set pref.currentOrganism = false " +
                        "where pref.id != :prefId and pref.user = :user and pref.clientToken = :clientToken",
                [prefId: userOrganismPreference.id, user: user, clientToken: clientToken])
    }

    def setCurrentAssemblage(User user, Assemblage assemblage, String clientToken) {
        Organism organism = assemblage.organism
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism)
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , assemblage: assemblage
                    , clientToken: clientToken
            ).save(flush: true)
            setOtherCurrentOrganismsFalseInDB(userOrganismPreference, user, clientToken)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.assemblage = assemblage
            userOrganismPreference.save()
            setOtherCurrentOrganismsFalseInDB(userOrganismPreference, user, clientToken)
        } else {
            userOrganismPreference.assemblage = assemblage
            userOrganismPreference.save()
        }
    }

    def setCurrentSequence(User user, Sequence sequence, String clientToken) {
        UserOrganismPreference userOrganismPreference = getCurrentOrganismPreference(user, null, clientToken)
        Assemblage assemblage = assemblageService.generateAssemblageForSequence(sequence)
        userOrganismPreference.assemblage = assemblage
        setSessionPreference(clientToken, userOrganismPreference)
        setCurrentSequenceInDB(user, sequence, clientToken)
    }

    def setCurrentSequenceInDB(User user, Sequence sequence, String clientToken) {
        Organism organism = sequence.organism
        Assemblage assemblage = assemblageService.generateAssemblageForSequence(sequence)
        if (user && assemblage) {
            user.addToAssemblages(assemblage)
        }
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientTokenAndAssemblage(user, organism, clientToken, assemblage, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences for sequence and organism: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), user, clientToken)
        }

        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null

        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , assemblage: assemblage
//                    , sequence: sequence
                    , clientToken: clientToken
            ).save(flush: true, insert: true)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.assemblage = assemblage
            userOrganismPreference.save(flush: true, insert: false)
        }
        setOtherCurrentOrganismsFalseInDB(userOrganismPreference, user, clientToken)
    }

    UserOrganismPreference setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp, String clientToken) {
        UserOrganismPreference userOrganismPreference = getCurrentOrganismPreference(permissionService.currentUser, sequenceName, clientToken)
        if (userOrganismPreference.assemblage.name != sequenceName ) {
//            Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, userOrganismPreference.organism)
            Assemblage assemblage = Assemblage.findByNameAndOrganism(sequenceName,userOrganismPreference.organism)
            userOrganismPreference.assemblage = assemblage
        }
        userOrganismPreference.startbp = startBp
        userOrganismPreference.endbp = endBp
        setSessionPreference(clientToken, userOrganismPreference)

        SequenceLocationDTO sequenceLocationDTO = new SequenceLocationDTO(
                sequenceName: sequenceName,
                startBp: startBp,
                endBp: endBp,
                clientToken: clientToken
        )
        scheduleSaveSequenceLocationInDB(sequenceLocationDTO)

        return userOrganismPreference
    }


    def evaluateSave(Date date,SequenceLocationDTO sequenceLocationDTO){
        if(currentlySavingLocation.contains(sequenceLocationDTO.clientToken)){
            log.debug "is currently saving these client token, so not trying to save"
          return
        }

        try {
            currentlySavingLocation.add(sequenceLocationDTO.clientToken)
            Date now = new Date()
            log.debug "trying to save it ${sequenceLocationDTO.clientToken}"
            def timeDiff = (now.getTime() - date.getTime()) / 1000
            if(timeDiff > PREFERENCE_SAVE_DELAY_SECONDS){
                log.debug "saving ${sequenceLocationDTO.clientToken} location to the database time: ${timeDiff}"
                setCurrentSequenceLocationInDB(sequenceLocationDTO)
            }
            else{
                log.debug "not saving ${sequenceLocationDTO.clientToken} location to the database time: ${timeDiff}"
            }
        } catch (e) {
            log.error "Problem saving ${e}"
        } finally {
            currentlySavingLocation.remove(sequenceLocationDTO.clientToken)
        }

    }

    def scheduleSaveSequenceLocationInDB(SequenceLocationDTO sequenceLocationDTO) {
        Date date = saveSequenceLocationMap.get(sequenceLocationDTO)
        if (!date) {
            log.debug "no last save found so saving ${sequenceLocationDTO.clientToken} location to the database"
            setCurrentSequenceLocationInDB(sequenceLocationDTO)
        }
        else{
            evaluateSave(date,sequenceLocationDTO)
        }
        saveSequenceLocationMap.put(sequenceLocationDTO,new Date())
        saveOutstandingLocationPreferences(sequenceLocationDTO.clientToken)
    }

    def saveOutstandingLocationPreferences(String ignoreToken=""){
        log.debug "trying to save outstanding ${saveSequenceLocationMap.size()}"
        saveSequenceLocationMap.each {
            if(it.key.clientToken!=ignoreToken){
                evaluateSave(it.value,it.key)
            }
        }
    }


    UserOrganismPreference setCurrentSequenceLocationInDB(SequenceLocationDTO sequenceLocationDTO) {
        saveSequenceLocationMap.remove(saveSequenceLocationMap)
        User currentUser = permissionService.currentUser
        String sequenceName = sequenceLocationDTO.sequenceName
        String clientToken = sequenceLocationDTO.clientToken
        Integer startBp = sequenceLocationDTO.startBp
        Integer endBp = sequenceLocationDTO.endBp

        Organism currentOrganism = getCurrentOrganismPreference(currentUser, sequenceName, clientToken)?.organism
        if (!currentOrganism) {
            throw new AnnotationException("Organism preference is not set for user")
        }

        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientTokenAndOrganism(currentUser, true, clientToken, currentOrganism, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), currentUser, clientToken)
        }
        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null

        // TODO: this is not quite correct yet
        if (!userOrganismPreference) {
            Assemblage thisAssemblage = Assemblage.findByName(sequenceName)
            userOrganismPreference = new UserOrganismPreference(
                    user: currentUser
                    , currentOrganism: true
                    , sequence: thisAssemblage
                    , organism: currentOrganism
                    , clientToken: clientToken
            ).save(insert: true)
        }

        Assemblage assemblage;
        if (AssemblageService.isProjectionString(sequenceName)) {
            JSONObject jsonObject = JSON.parse(sequenceName) as JSONObject
            jsonObject.put(FeatureStringEnum.CLIENT_TOKEN.value, clientToken)
            assemblage = assemblageService.convertJsonToAssemblage(jsonObject)
        } else {
            Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, userOrganismPreference.organism)
            assemblage = assemblageService.generateAssemblageForSequence(sequence)
        }

        userOrganismPreference.refresh()

        userOrganismPreference.clientToken = clientToken
        userOrganismPreference.currentOrganism = true
        userOrganismPreference.assemblage = assemblage

        // use the current value if we aren't setting it
        if (userOrganismPreference.startbp) {
            userOrganismPreference.startbp = startBp ?: userOrganismPreference.startbp
        } else {
            userOrganismPreference.startbp = startBp ?: 0
        }

        if (userOrganismPreference.endbp) {
            userOrganismPreference.endbp = endBp ?: userOrganismPreference.endbp
        } else {
            userOrganismPreference.endbp = endBp ?: assemblage.end
        }

        userOrganismPreference.save(flush: true, insert: false)
    }

    UserOrganismPreference getCurrentOrganismPreference(User user, String sequenceName, String clientToken) {
        UserOrganismPreference preference = getSessionPreference(clientToken)
        return preference ?: getCurrentOrganismPreferenceInDB(user, sequenceName, clientToken)
    }

    UserOrganismPreference getCurrentOrganismPreferenceInDB(User user, String sequenceName, String clientToken) {
        if (!user && !clientToken) {
            log.warn("No organism preference if no user ${user} or client token ${clientToken}")
            return null
        }

        // 1 - if a user exists, look up their client token and if they have a current organism.
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(user, true, clientToken, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), user, clientToken)
        }
        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (userOrganismPreference) {
            return userOrganismPreference
        }

        // 2 - if there is not a current organism for that token, then grab the first non-current one (unlikely) and make it current
        userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(user, false, clientToken, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), user, clientToken)
        }
        userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (userOrganismPreference) {
            setOtherCurrentOrganismsFalseInDB(userOrganismPreference, user, clientToken)
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true, insert: false)
            return userOrganismPreference
        }

        //3 - if none at all exist, we should ignore the client token and look it up by the user (missing), saving it for the current client token
        // we create a new one off of that, but for this client token
        userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganism(user, true, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), user, clientToken)
        }
        userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null

        // just grab an adjacent one for that user, that is not current
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndCurrentOrganism(user, false, [max: 1, sort: "lastUpdated", order: "desc"])
        if (userOrganismPreference) {
            Organism organism = userOrganismPreference.organism
            Assemblage assemblage = sequenceName ? Assemblage.findByNameAndOrganism(sequenceName, organism) : userOrganismPreference.assemblage
            UserOrganismPreference newPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , assemblage: assemblage
                    , startbp: userOrganismPreference.startbp
                    , endbp: userOrganismPreference.endbp
                    , clientToken: clientToken
            ).save(insert: true, flush: true)
            return newPreference
        }

        // 4 - if none at all exist, then we create one
        if (!userOrganismPreference) {
            // find a random organism based on sequence
            Assemblage assemblage = sequenceName ? Assemblage.findByName(sequenceName) : null
            Set<Organism> organisms = permissionService.getOrganisms(user)
//            Organism organism = sequence ? sequence.organism : organisms?.first()
            Organism organism = null
            if (assemblage) {
                organism = assemblage.organism
            }
            if (!organism && organisms) {
                organism = organisms.first()
            }
            if (!organism && permissionService.isAdmin()) {
                organism = Organism.first()
            }
            if (!organism) {
                throw new PermissionException("User does not have permission for any organisms.")
            }

            if (!assemblage) {
                Sequence sequence = sequenceName ? Sequence.findByNameAndOrganism(sequenceName, organism) : null
                sequence = sequence ?: organism.sequences.first()
                assemblage = assemblageService.generateAssemblageForSequence(sequence)
            }

            if (user) {
                UserOrganismPreference newUserOrganismPreference = new UserOrganismPreference(
                        user: user
                        , organism: organism
                        , currentOrganism: true
                        , assemblage: assemblage
                        , clientToken: clientToken
                        , startbp: assemblage.start
                        , endbp: assemblage.end
                ).save(insert: true, flush: true)
                return newUserOrganismPreference
            } else {
                return null
            }
        }

        return userOrganismPreference
    }

    UserOrganismPreference getCurrentOrganismPreferenceInDB(String token) {
        getCurrentOrganismPreferenceInDB(permissionService.currentUser, null, token)
    }

    /**
     * Looks at sequences to infer organism from
     * @param jsonString
     * @return
     */
    Organism inferOrganismFromReference(String jsonString) {
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        String firstSequenceName = jsonObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value).get(0).name
        Sequence sequence = Sequence.findByName(firstSequenceName)
        return sequence?.organism
    }

    /**
     * 1. Find all preferences a month old or more
     * 2. For each client token shared by a user and more than one organism
     * 3. Delete the older set of client tokens for each organisms / user combination
     */
    def removeStalePreferences() {

        try {
            log.info "Removing stale preferences"
            Date lastMonth = new Date().minus(0)
            int removalCount = 0

            // get user client tokens
            Map<User, Map<Organism, UserOrganismPreference>> userClientTokens = [:]
            UserOrganismPreference.findAllByLastUpdatedLessThan(lastMonth, [sort: "lastUpdated", order: "desc"]).each {
                Map<Organism, UserOrganismPreference> organismPreferenceMap = userClientTokens.containsKey(it.user) ? userClientTokens.get((it.user)) : [:]
                if (organismPreferenceMap.containsKey(it.organism)) {
                    UserOrganismPreference preference = organismPreferenceMap.get(it.organism)
                    // since we are sorting from the newest to the oldest, so just delete the older one
                    preference.delete()
                    ++removalCount
                    organismPreferenceMap.remove(it.organism)
                } else {
                    organismPreferenceMap.put(it.organism, it)
                }
                userClientTokens.put(it.user, organismPreferenceMap)
            }

            log.info "Removed ${removalCount} stale preferences"
        } catch (e) {
            log.error("Error removing preferences ${e}")
        }

    }
}
