package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.preference.AssemblageDTO
import org.bbop.apollo.preference.OrganismDTO
import org.bbop.apollo.preference.SequenceDTO
import org.bbop.apollo.preference.SequenceListDTO
import org.bbop.apollo.preference.UserDTO
import org.bbop.apollo.preference.UserOrganismPreferenceDTO
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class PreferenceService {

    def permissionService
    def assemblageService

    final Integer PREFERENCE_SAVE_DELAY_SECONDS = 5  // saves every 30 seconds
    // enqueue to store save
    private Map<UserOrganismPreferenceDTO, Date> saveSequenceLocationMap = new HashMap<>()
    // set of client locations
    private Set<String> currentlySavingLocation = new HashSet<>()
    private Date lastSaveEvaluation = new Date()


    Organism getSessionOrganism(String clientToken) {
        JSONObject preferenceObject = getSessionPreferenceObject(clientToken)
        if (preferenceObject) {
            def organismId = preferenceObject.organism.id as Long
            return Organism.get(organismId) ?: Organism.findById(organismId)
        }
        return null
    }


    UserOrganismPreferenceDTO getSessionPreference(String clientToken) {
        JSONObject preferenceObject = getSessionPreferenceObject(clientToken)
        return preferenceObject ? getDTOPreferenceFromObject(preferenceObject) : null
    }

    UserOrganismPreferenceDTO getDTOPreferenceFromObject(JSONObject userOrganismPreferenceObject) {
//        JSONObject sequenceObject = userOrganismPreferenceObject.containsKey(FeatureStringEnum.SEQUENCE.value) && !userOrganismPreferenceObject.isNull(FeatureStringEnum.SEQUENCE.value) ? userOrganismPreferenceObject.getJSONObject(FeatureStringEnum.SEQUENCE.value) : null
//        SequenceDTO sequenceDTO = sequenceObject ? getDTOSequenceFromObject(sequenceObject) : null
        OrganismDTO organismDTO = getDTOFromOrganismFromObject(userOrganismPreferenceObject.getJSONObject(FeatureStringEnum.ORGANISM.value))
//        SequenceDTO sequenceDTO = getDTOSequenceFromObject(userOrganismPreferenceObject.getJSONObject(FeatureStringEnum.SEQUENCE.value))
        AssemblageDTO assemblageDTO = getDTOAssemblageFromObject(userOrganismPreferenceObject.getJSONObject(FeatureStringEnum.ASSEMBLAGE.value))
        UserDTO userDTO = getDTOUserFromObject(userOrganismPreferenceObject.getJSONObject("user"))
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = new UserOrganismPreferenceDTO(
                organism: organismDTO
                , assemblage: assemblageDTO
                , id: userOrganismPreferenceObject.id
                , user: userDTO
                , currentOrganism: userOrganismPreferenceObject.currentOrganism
                , nativeTrackList: userOrganismPreferenceObject.nativeTrackList
                , startbp: userOrganismPreferenceObject.startbp
                , endbp: userOrganismPreferenceObject.endbp
                , clientToken: userOrganismPreferenceObject.clientToken
        )
        return userOrganismPreferenceDTO
    }

    UserDTO getDTOUserFromObject(JSONObject user) {
        UserDTO userDTO = new UserDTO(
                id: user.id
                , username: user.username
        )
        return userDTO
    }

    AssemblageDTO getDTOAssemblageFromObject(JSONObject jSONObject) {
        if(!jSONObject) return null
        OrganismDTO organismDTO = getDTOFromOrganismFromObject(jSONObject.getJSONObject(FeatureStringEnum.ORGANISM.value))
        AssemblageDTO assemblageDTO = new AssemblageDTO(
                id: jSONObject.id
                , organism: organismDTO
                , name: jSONObject.name
                , start: jSONObject.start
                , end: jSONObject.end
                , sequenceList: jSONObject.sequenceList
        )
        return assemblageDTO
    }

    SequenceDTO getDTOSequenceFromObject(JSONObject sequence) {
        if(!sequence) return null
        OrganismDTO organismDTO = getDTOFromOrganismFromObject(sequence.getJSONObject(FeatureStringEnum.ORGANISM.value))
        SequenceDTO sequenceDTO = new SequenceDTO(
                id: sequence.id
                , organism: organismDTO
                , name: sequence.name
                , start: sequence.start
                , end: sequence.end
                , length: sequence.length
        )
        return sequenceDTO
    }

    OrganismDTO getDTOFromOrganismFromObject(JSONObject organism) {
        OrganismDTO organismDTO = new OrganismDTO(
                id: organism.id
                , commonName: organism.commonName
                , directory: organism.directory
        )
        return organismDTO
    }

    def printKeys(Session thisSession) {
        println "==== START KEYS ===="
        def keys = thisSession.getAttributeKeys()
        keys.each {
            println "key[${it}] value[${thisSession.getAttribute(it)}]"
        }
        println "==== END KEYS ===="
    }

    JSONObject getSessionPreferenceObject(String clientToken) {
        try {
            Session session = SecurityUtils.subject.getSession(false)
            if (session) {
//                printKeys(session)
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

    UserOrganismPreferenceDTO setSessionPreference(String clientToken, UserOrganismPreferenceDTO userOrganismPreferenceDTO) {
        Session session = SecurityUtils.subject.getSession(false)
        if (session) {
            // should be client_token , JSONObject
            String preferenceString = (userOrganismPreferenceDTO as JSON).toString()
            session.setAttribute(FeatureStringEnum.PREFERENCE.getValue() + "::" + clientToken, preferenceString)
        } else {
            log.warn "No session found"
        }
        return userOrganismPreferenceDTO
    }


    Organism getCurrentOrganismForCurrentUser(String clientToken) {
        log.debug "PS: getCurrentOrganismForCurrentUser ${clientToken}"
        Organism organism = getSessionOrganism(clientToken)
        log.debug "found organism in session ${organism} so returning"
        if (organism) return organism
        if (permissionService.currentUser == null) {
            log.warn "No user present, so using the client token"
            organism = getOrganismForTokenInDB(clientToken)
            return organism
        } else {
            UserOrganismPreferenceDTO userOrganismPreference = getCurrentOrganismPreferenceWithSequence(permissionService.currentUser,  clientToken)
            OrganismDTO organismDTO = setSessionPreference(clientToken, userOrganismPreference)?.organism
            return Organism.findById(organismDTO.id)
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

    UserOrganismPreferenceDTO setCurrentOrganism(User user, Organism organism, String clientToken) {
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = getCurrentOrganismPreferenceWithSequence(user,  clientToken)
        if (userOrganismPreferenceDTO.organism.id == organism.id) {
            log.info "Same organism so not changing preference"
            return userOrganismPreferenceDTO
        }
        evaluateSaves(true, clientToken)
        // we have to go to the database to see if there was a prior sequence
        // we look for the organism association with the current active token, first
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganismAndCurrentOrganismAndClientToken(user, organism, true, clientToken, [sort: "lastUpdated", order: "desc", max: 1])
        // we look for the organism association with the current inactive token, first
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndOrganismAndClientToken(user, organism, clientToken, [sort: "lastUpdated", order: "desc", max: 1])
        if (!userOrganismPreference) {

            // We need to create a new preference
            // We look for the organism association with any current organism
            UserOrganismPreference existingPreference  = UserOrganismPreference.findByUserAndOrganismAndCurrentOrganism(user, organism, true, [sort: "lastUpdated", order: "desc", max: 1])
            // We look for the organism association with any non-current organism
            existingPreference = existingPreference ?: UserOrganismPreference.findByUserAndOrganism(user, organism, [sort: "lastUpdated", order: "desc", max: 1])

            // if the existing preference exists, then use those values
            if(existingPreference){
                userOrganismPreference = new UserOrganismPreference(
                        user: user
                        , organism: organism
                        , clientToken: clientToken
                        , sequence: existingPreference.sequence
                        , currentOrganism: true
                        , startbp: existingPreference.startbp
                        , endbp: existingPreference.endbp
                )
            }


            // else use random ones
            else{
                // then create one
                Sequence sequence = Sequence.findAllByOrganism(organism, [max: 1, sort: "end", order: "desc"]).first()
                userOrganismPreference = new UserOrganismPreference(
                        user: user
                        , organism: organism
                        , clientToken: clientToken
                        , sequence: sequence
                        , currentOrganism: true
                        , startbp: 0
                        , endbp: sequence.end
                )
            }

        } else {
            userOrganismPreference.currentOrganism = true
        }
        userOrganismPreference.save(flush: true)

        userOrganismPreferenceDTO = getDTOFromPreference(userOrganismPreference)

        setSessionPreference(clientToken, userOrganismPreferenceDTO)
        setCurrentOrganismInDB(userOrganismPreferenceDTO)
        return userOrganismPreferenceDTO
    }

    /**
     * We can save the preference locally.
     *
     * In the database we set this organism and sequence to true.
     *
     * 1. If there is none for this organism + sequence + client_token
     *    Then we generate it
     * 2. If there is one for this organism + sequence + client_token
     *    We retrieve it and update the start / stop
     *
     * Else . . .
     *
     * @param userOrganismPreferenceDTO
     * @return
     */
    UserOrganismPreference setCurrentOrganismInDB(UserOrganismPreferenceDTO userOrganismPreferenceDTO) {
//        UserOrganismPreference setCurrentOrganismInDB(User user, Organism organism, String clientToken) {
        UserOrganismPreference organismPreference = userOrganismPreferenceDTO.id ? UserOrganismPreference.findById(userOrganismPreferenceDTO.id) : null

        // 2. update it
        if (organismPreference
                && userOrganismPreferenceDTO.clientToken == organismPreference.clientToken
                && userOrganismPreferenceDTO.assemblage.sequenceList == organismPreference.assemblage.sequenceList
                && userOrganismPreferenceDTO.organism.id == organismPreference.organism.id
        ) {
            organismPreference.startbp = userOrganismPreferenceDTO.startbp
            organismPreference.endbp = userOrganismPreferenceDTO.endbp
            organismPreference.save()
        }
        // 1. create a new one
        else {
            organismPreference = generateNewPreferenceFromDTO(userOrganismPreferenceDTO)
        }

        // if the preference already exists
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientToken(organismPreference.user, organismPreference.organism, organismPreference.clientToken, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(getMostRecentPreference(userOrganismPreferences))
        }
        int affected = setOtherCurrentOrganismsFalseInDB(organismPreference, organismPreference.user, organismPreference.clientToken)
        log.debug "others set to false: ${affected}"
        return organismPreference
    }

//    def setCurrentOrganismInDB(User user, Organism organism, String clientToken) {
//        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientToken(user, organism, clientToken, [sort: "lastUpdated", order: "desc"])
//        if (userOrganismPreferences.size() > 1) {
//            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
//            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), user, clientToken)
//        }
//
//        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
//        if (!userOrganismPreference) {
//            def sequences = Sequence.findAllByOrganism(organism)
//            if (!sequences) {
//                log.warn "Sequences not loaded for organism ${organism.commonName}, so not setting preference for user ${user.username}."
//                return
//            }
//            Assemblage assemblage = Assemblage.findByOrganism(organism) ?: assemblageService.generateAssemblageForSequence(sequences.first())
//            userOrganismPreference = new UserOrganismPreference(
//                    user: user
//                    , organism: organism
//                    , currentOrganism: true
//                    , assemblage: assemblage
//                    , clientToken: clientToken
//            ).save(flush: true, insert: true)
//        } else if (!userOrganismPreference.currentOrganism) {
//            userOrganismPreference.currentOrganism = true;
//            userOrganismPreference.save(flush: true, insert: false)
//        }
//        setOtherCurrentOrganismsFalseInDB(userOrganismPreference, user, clientToken)
//    }

    protected static
    def setOtherCurrentOrganismsFalseInDB(UserOrganismPreference userOrganismPreference) {
        return setOtherCurrentOrganismsFalseInDB(userOrganismPreference, userOrganismPreference.user, userOrganismPreference.clientToken)
    }

    protected static
    def setOtherCurrentOrganismsFalseInDB(UserOrganismPreference userOrganismPreference, User user, String clientToken) {
        int affected = UserOrganismPreference.executeUpdate(
                "update UserOrganismPreference  pref set pref.currentOrganism = false " +
                        "where pref.id != :prefId and pref.user = :user and pref.clientToken = :clientToken",
                [prefId: userOrganismPreference.id, user: user, clientToken: clientToken])

        Date now = new Date()
        Date newDate = new Date(now.getTime() - 8 * 1000 * 60 * 60)
        // no current organisms if they are 8 hours hold
        affected += UserOrganismPreference.executeUpdate(
                "update UserOrganismPreference  pref set pref.currentOrganism = false " +
                        "where pref.id != :prefId and pref.user = :user and pref.organism = :organism and pref.assemblage = :assemblage and pref.lastUpdated < :lastUpdated ",
                [prefId: userOrganismPreference.id, user: user, organism: userOrganismPreference.organism, assemblage: userOrganismPreference.assemblage, lastUpdated: newDate])

        // if the preference is not set correctly, we have to make sure we add it correctly
        affected += UserOrganismPreference.executeUpdate(
                "update UserOrganismPreference  pref set pref.currentOrganism = true " +
                        "where pref.id = :prefId and pref.user = :user and pref.clientToken = :clientToken and pref.currentOrganism = false ",
                [prefId: userOrganismPreference.id, user: user, clientToken: clientToken])

        return affected
    }

    /**
     * @deprecated  Should use setCurrentAssemblage or transfer to an assemblage method
     * @param user
     * @param sequence
     * @param clientToken
     * @return
     */
    UserOrganismPreferenceDTO setCurrentSequence(User user, Sequence sequence, String clientToken) {
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = getCurrentOrganismPreferenceWithSequence(user, sequence.name, clientToken) ?: null
        Assemblage assemblage = assemblageService.generateAssemblageForSequence(sequence)
        if (userOrganismPreferenceDTO.assemblage.id == assemblage.id) {
            log.info "Same sequence so not changing preference"
            return userOrganismPreferenceDTO
        }

        evaluateSaves(true, clientToken)
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndAssemblageAndCurrentOrganismAndClientToken(user, assemblage, true, clientToken, [sort: "lastUpdated", order: "desc", max: 1])
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndAssemblageAndClientToken(user, assemblage, clientToken, [sort: "lastUpdated", order: "desc", max: 1])
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndAssemblageAndCurrentOrganism(user, assemblage, true, [sort: "lastUpdated", order: "desc", max: 1])
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndAssemblage(user, assemblage, [sort: "lastUpdated", order: "desc", max: 1])

        if (!userOrganismPreference) {
            // then create one
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: sequence.organism
                    , clientToken: clientToken
                    , sequence: sequence
                    , currentOrganism: true
                    , startbp: 0
                    , endbp: sequence.end
            )
        } else {
            userOrganismPreference.currentOrganism = true
        }
        userOrganismPreference.save(flush: true)

        userOrganismPreferenceDTO = getDTOFromPreference(userOrganismPreference)

        setSessionPreference(clientToken, userOrganismPreferenceDTO)
        setCurrentSequenceInDB(user, sequence, clientToken)
        return userOrganismPreferenceDTO
    }
//    def setCurrentSequence(User user, Sequence sequence, String clientToken) {
//        UserOrganismPreference userOrganismPreference = getCurrentOrganismPreferenceWithSequence(user, null, clientToken)
//        Assemblage assemblage = assemblageService.generateAssemblageForSequence(sequence)
//        userOrganismPreference.assemblage = assemblage
//        setSessionPreference(clientToken, userOrganismPreference)
//        setCurrentSequenceInDB(user, sequence, clientToken)
//    }


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

    private
    static UserOrganismPreference getMostRecentPreference(List<UserOrganismPreference> userOrganismPreferences) {
        if (userOrganismPreferences) {
            return userOrganismPreferences.sort() { a, b ->
                a.lastUpdated <=> b.lastUpdated
            }.first()
        }
        return null
    }

    UserOrganismPreference setCurrentSequenceInDB(User user, Sequence sequence, String clientToken) {
        Organism organism = sequence.organism
        Assemblage assemblage = assemblageService.generateAssemblageForSequence(sequence)
        if (user && assemblage) {
            user.addToAssemblages(assemblage)
        }
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientTokenAndAssemblage(user, organism, clientToken, assemblage, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences for sequence and organism: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(getMostRecentPreference(userOrganismPreferences), user, clientToken)
        }
        UserOrganismPreference userOrganismPreference = getMostRecentPreference(userOrganismPreferences)

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
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.assemblage = assemblage
            userOrganismPreference.save(flush: true, insert: false)
        }
        setOtherCurrentOrganismsFalseInDB(userOrganismPreference, user, clientToken)
        return userOrganismPreference
    }

//    UserOrganismPreference setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp, String clientToken) {
//        UserOrganismPreference userOrganismPreference = getCurrentOrganismPreferenceWithSequence(permissionService.currentUser, sequenceName, clientToken)
//        println "assemblage ${userOrganismPreference.assemblage}"
//        if (userOrganismPreference?.assemblage?.name != sequenceName ) {
////            Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, userOrganismPreference.organism)
//            Assemblage assemblage = Assemblage.findByNameAndOrganism(sequenceName,userOrganismPreference.organism)
//            userOrganismPreference.assemblage = assemblage
//        }
//        userOrganismPreference.startbp = startBp
//        userOrganismPreference.endbp = endBp
//        setSessionPreference(clientToken, userOrganismPreference)
//
//        SequenceLocationDTO sequenceLocationDTO = new SequenceLocationDTO(
//                sequenceName: sequenceName,
//                startBp: startBp,
//                endBp: endBp,
//                clientToken: clientToken
//        )
//        scheduleSaveSequenceLocationInDB(sequenceLocationDTO)
//
//        return userOrganismPreference
//    }


//    UserOrganismPreference setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp, String clientToken) {
//        UserOrganismPreference userOrganismPreference = getCurrentOrganismPreferenceWithSequence(permissionService.currentUser, sequenceName, clientToken)
//        if (userOrganismPreference.sequence.name != sequenceName || userOrganismPreference.sequence.organismId != userOrganismPreference.organismId) {
//            Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, userOrganismPreference.organism)
//            userOrganismPreference.sequence = sequence
//        }
//        userOrganismPreference.startbp = startBp
//        userOrganismPreference.endbp = endBp
//        setSessionPreference(clientToken, userOrganismPreference)
//
//        SequenceLocationDTO sequenceLocationDTO = new SequenceLocationDTO(
//                sequenceName: sequenceName,
//                startBp: startBp,
//                endBp: endBp,
//                clientToken: clientToken
//        )
//        scheduleSaveSequenceLocationInDB(sequenceLocationDTO)
//
//        return userOrganismPreference
//    }

    /**
     * @deprecated
     * Should set for sequenceList or convert to another sequence name
     * @param sequenceName
     * @param startBp
     * @param endBp
     * @param clientToken
     * @return
     */
    UserOrganismPreferenceDTO setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp, String clientToken) {
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = getCurrentOrganismPreferenceWithSequence(permissionService.currentUser, sequenceName, clientToken)
        if (userOrganismPreferenceDTO.assemblage.name != sequenceName || userOrganismPreferenceDTO.assemblage?.organism?.id != userOrganismPreferenceDTO.organism.id) {
            Organism organism = Organism.findById(userOrganismPreferenceDTO.organism.id)
            Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)
            Assemblage assemblage = assemblageService.generateAssemblageForSequence(sequence)
            userOrganismPreferenceDTO.assemblage = getDTOFromAssemblage(assemblage)
        }
        userOrganismPreferenceDTO.startbp = startBp
        userOrganismPreferenceDTO.endbp = endBp
        userOrganismPreferenceDTO.currentOrganism = true
        setSessionPreference(clientToken, userOrganismPreferenceDTO)

        scheduleSaveSequenceLocationInDB(userOrganismPreferenceDTO)

        return userOrganismPreferenceDTO
    }

    def evaluateSaves(boolean forceSaves = false, String onlySaveToken = null) {
        log.debug "Evaluating saves: ${forceSaves}"
        long timeDiff = (new Date()).getTime() - lastSaveEvaluation.getTime()
        if (!forceSaves && timeDiff / 1000.0 < PREFERENCE_SAVE_DELAY_SECONDS) {
            log.debug "Not saving yet timeDif: ${timeDiff}"
            return
        }
        log.debug "Saving with time diff: ${timeDiff}"
        lastSaveEvaluation = new Date()
        def sequenceMapSet = saveSequenceLocationMap.entrySet()
        def sequenceMapSetIterator = sequenceMapSet.iterator()

        try {
            while (sequenceMapSetIterator.hasNext()) {
                Map.Entry<UserOrganismPreferenceDTO, Date> userOrganismPreferenceDTOEntry = sequenceMapSetIterator.next()
                println "DTO: ${userOrganismPreferenceDTOEntry.key as JSON}"
                println "value date : ${saveSequenceLocationMap.get(userOrganismPreferenceDTOEntry.key)}"
                println "value date 2 : ${userOrganismPreferenceDTOEntry.value}"
                if (onlySaveToken && onlySaveToken == userOrganismPreferenceDTOEntry.key.clientToken) {
                    evaluateSave(userOrganismPreferenceDTOEntry.value, userOrganismPreferenceDTOEntry.key, forceSaves)
                } else {
                    evaluateSave(userOrganismPreferenceDTOEntry.value, userOrganismPreferenceDTOEntry.key, forceSaves)
                }
            }
        } catch (e) {
            log.warn("Problem saving preference: "+e)
        }

    }

    def evaluateSave(Date date, UserOrganismPreferenceDTO preferenceDTO, Boolean forceSave) {
        try {
            currentlySavingLocation.add(preferenceDTO.clientToken)
            Date now = new Date()
            log.debug "trying to save it ${preferenceDTO.clientToken}"
            def timeDiff = (now.getTime() - date.getTime()) / 1000
            if (forceSave || timeDiff > PREFERENCE_SAVE_DELAY_SECONDS) {
                log.debug "saving ${preferenceDTO.clientToken} location to the database time: ${timeDiff}"
                setCurrentSequenceLocationInDB(preferenceDTO)
            } else {
                log.debug "not saving ${preferenceDTO.clientToken} location to the database time: ${timeDiff}"
            }
        } catch (e) {
            log.error "Problem saving ${e} for ${preferenceDTO as JSON}"
        } finally {
            currentlySavingLocation.remove(preferenceDTO.clientToken)
        }
    }

    private def printSaves() {
        println "# of entries ${saveSequenceLocationMap.size()}"
        saveSequenceLocationMap.each {
            println "JSON: [" + (it.key as JSON) + "] date[" + it.value + "]"
        }
    }

    def scheduleDbSave(Date date, UserOrganismPreferenceDTO preferenceDTO) {
        // these should replace, though
        if (!currentlySavingLocation.contains(preferenceDTO.clientToken)) {
            // saves with the latest one now
            currentlySavingLocation.add(preferenceDTO.clientToken)
        }
        saveSequenceLocationMap.put(preferenceDTO, date)
//        printSaves()
    }

    def scheduleSaveSequenceLocationInDB(UserOrganismPreferenceDTO userOrganismPreferenceDTO) {
        Date date = saveSequenceLocationMap.get(userOrganismPreferenceDTO)
        log.debug "Scheduling save date: ${date}"
        if (!date) {
            log.debug "No last save found so saving ${userOrganismPreferenceDTO.clientToken} location to the database."
            setCurrentSequenceLocationInDB(userOrganismPreferenceDTO)
        } else {
            log.debug "Evaluate save for date: ${date}"
            scheduleDbSave(date, userOrganismPreferenceDTO)
        }
        saveSequenceLocationMap.put(userOrganismPreferenceDTO, new Date())
        saveOutstandingLocationPreferences(userOrganismPreferenceDTO.clientToken)
    }

    def saveOutstandingLocationPreferences(String ignoreToken = "") {
        saveSequenceLocationMap.each {
            if (it.key.clientToken != ignoreToken) {
                scheduleDbSave(it.value, it.key)
            }
        }
    }

    UserOrganismPreference generateNewPreferenceFromDTO(UserOrganismPreferenceDTO userOrganismPreferenceDTO) {
        Organism organism = Organism.findById(userOrganismPreferenceDTO.organism.id)
//        Sequence sequence = Sequence.findByNameAndOrganism(userOrganismPreferenceDTO.sequence.name, organism)
        Assemblage assemblage = Assemblage.findById(userOrganismPreferenceDTO.assemblage.id)
        User user = User.findByUsername(userOrganismPreferenceDTO.user.username)
        UserOrganismPreference userOrganismPreference = new UserOrganismPreference(
                organism: organism
                , user: user
                , assemblage: assemblage
                , currentOrganism: userOrganismPreferenceDTO.currentOrganism
                , nativeTrackList: userOrganismPreferenceDTO.nativeTrackList
                , startbp: userOrganismPreferenceDTO.startbp
                , endbp: userOrganismPreferenceDTO.endbp
                , clientToken: userOrganismPreferenceDTO.clientToken
        ).save(insert: true)
        return userOrganismPreference
    }

    SequenceDTO getDTOFromSequence(Sequence sequence) {
//        if(!sequence) return null
        OrganismDTO organismDTO = getDTOFromOrganism(sequence.organism)
        SequenceDTO sequenceDTO = new SequenceDTO(
                id: sequence.id
                , organism: organismDTO
                , name: sequence.name
                , start: sequence.start
                , end: sequence.end
                , length: sequence.length
        )
        return sequenceDTO
    }

    AssemblageDTO getDTOFromAssemblage(Assemblage assemblage) {
//        if(!sequence) return null
        OrganismDTO organismDTO = getDTOFromOrganism(assemblage.organism)
        AssemblageDTO assemblageDTO = new AssemblageDTO(
                id: assemblage.id
                , organism: organismDTO
                , name: assemblage.name
                , start: assemblage.start
                , end: assemblage.end
                , sequenceList: new SequenceListDTO(assemblage.sequenceList)
        )
        return assemblageDTO
    }

    OrganismDTO getDTOFromOrganism(Organism organism) {
        OrganismDTO organismDTO = new OrganismDTO(
                id: organism.id
                , commonName: organism.commonName
                , directory: organism.directory
        )
        return organismDTO
    }

    UserDTO getDTOFromUser(User user) {
        UserDTO userDTO = new UserDTO(
                id: user.id
                , username: user.username
        )
        return userDTO
    }

    UserOrganismPreferenceDTO getDTOFromPreference(UserOrganismPreference userOrganismPreference) {
        Assemblage assemblage = userOrganismPreference.assemblage
        AssemblageDTO assemblageDTO = getDTOFromAssemblage(assemblage)
        OrganismDTO organismDTO = getDTOFromOrganism(userOrganismPreference.organism)
        UserDTO userDTO = getDTOFromUser(userOrganismPreference.user)
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = new UserOrganismPreferenceDTO(
                organism: organismDTO
                , assemblage: assemblageDTO
                , id: userOrganismPreference.id
                , user: userDTO
                , currentOrganism: userOrganismPreference.currentOrganism
                , nativeTrackList: userOrganismPreference.nativeTrackList
                , startbp: userOrganismPreference.startbp
                , endbp: userOrganismPreference.endbp
                , clientToken: userOrganismPreference.clientToken
        )
        return userOrganismPreferenceDTO
    }


    UserOrganismPreference setCurrentSequenceLocationInDB(UserOrganismPreferenceDTO preferenceDTO) {
        log.debug "Saving location in DB: ${preferenceDTO as JSON}"
        saveSequenceLocationMap.remove(preferenceDTO)

        preferenceDTO = getSessionPreference(preferenceDTO.clientToken) ?: preferenceDTO


        User user = User.findByUsername(preferenceDTO.user.username)
//        String sequenceName = preferenceDTO.sequence.name
        Assemblage assemblage = Assemblage.findById(preferenceDTO.assemblage.id)
        String clientToken = preferenceDTO.clientToken
        Integer startBp = preferenceDTO.startbp
        Integer endBp = preferenceDTO.endbp

//        OrganismDTO currentOrganism = getCurrentOrganismPreferenceWithSequence(user, sequenceName, clientToken)?.organism
        OrganismDTO currentOrganism = getCurrentOrganismPreferenceByAssemblage(user, assemblage, clientToken)?.organism
        if (!currentOrganism) {
            throw new AnnotationException("Organism preference is not set for user")
        }
        Organism organism = Organism.findById(currentOrganism.id)
//        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)
        if (!assemblage) {
            throw new AnnotationException("Assemblage is invalid ${assemblage} and organism ${organism as JSON}")
        }

//        String sequenceList = assemblageService.generateAssemblageForSequence(sequence)?.sequenceList

        def userOrganismPreferences = UserOrganismPreference.createCriteria().list {
            createAlias('sequence', 'sequence', org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN)
            and {
                eq("user", user)
                eq("clientToken", clientToken)
                eq("assemblage", assemblage)
                eq("organism", organism)
            }
        }
//        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientTokenAndOrganism(currentUser, true, clientToken, currentOrganism, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(getMostRecentPreference(userOrganismPreferences), user, clientToken)
        }
        UserOrganismPreference userOrganismPreference = getMostRecentPreference(userOrganismPreferences)
        if (!userOrganismPreference) {
            log.debug "Creating a new PReference"
//            Assemblage thisAssemblage = Assemblage.findByName(sequenceName)
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , currentOrganism: true
                    , assemblage: assemblage
                    , organism: organism
                    , clientToken: clientToken
            ).save(insert: true)
        }
        log.debug "init save ${userOrganismPreference as JSON}"

//        Assemblage assemblage;
//        if (AssemblageService.isProjectionString(sequenceName)) {
//            JSONObject jsonObject = JSON.parse(sequenceName) as JSONObject
//            jsonObject.put(FeatureStringEnum.CLIENT_TOKEN.value, clientToken)
//            assemblage = assemblageService.convertJsonToAssemblage(jsonObject)
//        } else {
//            Sequence innerSequence = Sequence.findByNameAndOrganism(sequenceName, userOrganismPreference.organism)
//            assemblage = assemblageService.generateAssemblageForSequence(innerSequence)
//        }

        userOrganismPreference.refresh()

        log.debug "refresh save ${userOrganismPreference as JSON}"

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
        log.debug "final save ${userOrganismPreference as JSON}"
    }

    UserOrganismPreferenceDTO getCurrentOrganismPreferenceByAssemblage(User user, Assemblage assemblage, String clientToken) {

        UserOrganismPreferenceDTO preference = getSessionPreference(clientToken)
//        preference = preference ?: getSavingPreferences(user, sequenceName, clientToken)
        preference = preference ?: getSavingPreferencesForAssemblage(user, assemblage, clientToken)
        println "found in-memory preference: ${preference ? preference as JSON : null}"
//        return preference ?: getDTOFromPreference(getCurrentOrganismPreferenceInDB(user, sequenceName, clientToken))
        // TODO: this is not correct as assemblage does not have a method
        return preference ?: getDTOFromPreference(getCurrentOrganismPreferenceInDB(user, assemblage, clientToken))
    }

    /**
     * @deprecated
     * @param user
     * @param clientToken
     * @return
     */
    UserOrganismPreferenceDTO getCurrentOrganismPreferenceWithSequence(User user, String clientToken) {
        return getCurrentOrganismPreferenceWithSequence(user, null,clientToken)
    }

    /**
     * @deprecated
     * @param user
     * @param sequenceName
     * @param clientToken
     * @return
     */
    UserOrganismPreferenceDTO getCurrentOrganismPreferenceWithSequence(User user, String sequenceName, String clientToken) {

        UserOrganismPreferenceDTO preference = getSessionPreference(clientToken)
        preference = preference ?: getSavingPreferences(user, sequenceName, clientToken)
        println "found in-memory preference: ${preference ? preference as JSON : null}"
        return preference ?: getDTOFromPreference(getCurrentOrganismPreferenceInDB(user, sequenceName, clientToken))
    }

    def getSavingPreferencesForAssemblage(User user, Assemblage assemblage, String clientToken) {
        return saveSequenceLocationMap.keySet().find() {
            it.clientToken == clientToken && it.user.username == user.username && it.assemblage.id == assemblage.id
        }
    }

    /**
     * @deprecated
     * @param user
     * @param sequenceName
     * @param clientToken
     * @return
     */
    def getSavingPreferences(User user, String sequenceList, String clientToken) {
        return saveSequenceLocationMap.keySet().find() {
            it.clientToken == clientToken && it.user.username == user.username && it.assemblage.sequenceList.toString() == sequenceList
        }
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
            setOtherCurrentOrganismsFalseInDB(getMostRecentPreference(userOrganismPreferences), user, clientToken)
        }
        UserOrganismPreference userOrganismPreference = getMostRecentPreference(userOrganismPreferences)
        if (userOrganismPreference) {
            return userOrganismPreference
        }

        // 2 - if there is not a current organism for that token, then grab the first non-current one (unlikely) and make it current
        userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(user, false, clientToken, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(getMostRecentPreference(userOrganismPreferences), user, clientToken)
        }
        userOrganismPreference = getMostRecentPreference(userOrganismPreferences)
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
            setOtherCurrentOrganismsFalseInDB(getMostRecentPreference(userOrganismPreferences), user, clientToken)
        }
        userOrganismPreference = getMostRecentPreference(userOrganismPreferences)

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

//            sequence = sequence ?: Sequence.findByOrganism(organism, [sort: "end", order: "desc", max: 1])
            if(!assemblage){
//                sequence = sequence ?: Sequence.findAllByOrganism(organism, [sort: "end", order: "desc", max: 1]).first()
                Sequence sequence = sequenceName ? Sequence.findByNameAndOrganism(sequenceName, organism) : Sequence.findAllByOrganism(organism, [sort: "end", order: "desc", max: 1]).first()
                sequence = sequence ?: organism.sequences?.first()
                if(sequence){
                    assemblage = assemblageService.generateAssemblageForSequence(sequence)
                }
            }
            if (user) {
                UserOrganismPreference newUserOrganismPreference = new UserOrganismPreference(
                        user: user
                        , organism: organism
                        , currentOrganism: true
                        , assemblage: assemblage  // can be null
                        , clientToken: clientToken
                )
                if (assemblage) {
                    newUserOrganismPreference.startbp = assemblage.start
                    newUserOrganismPreference.endbp = assemblage.end
                }
                newUserOrganismPreference.save(insert: true, flush: true)
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
