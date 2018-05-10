package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.preference.OrganismDTO
import org.bbop.apollo.preference.SequenceDTO
import org.bbop.apollo.preference.UserDTO
import org.bbop.apollo.preference.UserOrganismPreferenceDTO
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class PreferenceService {

    def permissionService
    def configWrapperService

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
        OrganismDTO organismDTO = getDTOFromOrganismFromObject(userOrganismPreferenceObject.getJSONObject(FeatureStringEnum.ORGANISM.value))
        SequenceDTO sequenceDTO = getDTOSequenceFromObject(userOrganismPreferenceObject.getJSONObject(FeatureStringEnum.SEQUENCE.value))
        UserDTO userDTO = getDTOUserFromObject(userOrganismPreferenceObject.getJSONObject("user"))
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = new UserOrganismPreferenceDTO(
                organism: organismDTO
                , sequence: sequenceDTO
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

    SequenceDTO getDTOSequenceFromObject(JSONObject sequence) {
        if (!sequence) return null
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
            UserOrganismPreferenceDTO userOrganismPreference = getCurrentOrganismPreference(permissionService.currentUser, null, clientToken)
            OrganismDTO organismDTO = setSessionPreference(clientToken, userOrganismPreference)?.organism
            return Organism.findById(organismDTO.id)
        }
    }

    Organism getOrganismForTokenInDB(String token) {
        log.debug "token for org ${token}"
        if (token.isLong()) {
            log.debug "is long "
            return Organism.findById(Long.parseLong(token))
        } else {
            log.debug "is NOT long "
            // Cannot use findByCommonNameIlike, because it will fail to update the permission of an organism named orgam
            // if an organism named Orgam exist. findByCommonNameIlike ignores the case.
            return Organism.findByCommonName(token)
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


    UserOrganismPreferenceDTO setCurrentOrganism(User user, Organism organism, String clientToken) {
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = getCurrentOrganismPreference(user, null, clientToken)
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
            UserOrganismPreference existingPreference = UserOrganismPreference.findByUserAndOrganismAndCurrentOrganism(user, organism, true, [sort: "lastUpdated", order: "desc", max: 1])
            // We look for the organism association with any non-current organism
            existingPreference = existingPreference ?: UserOrganismPreference.findByUserAndOrganism(user, organism, [sort: "lastUpdated", order: "desc", max: 1])

            // if the existing preference exists, then use those values
            if (existingPreference) {
                userOrganismPreference = new UserOrganismPreference(
                        user: user
                        , organism: organism
                        , clientToken: clientToken
                        , sequence: existingPreference.sequence
                        , currentOrganism: true
                        , nativeTrackList: configWrapperService.getNativeTrackSelectorDefaultOn()
                        , startbp: existingPreference.startbp
                        , endbp: existingPreference.endbp
                )
            }

            // else use random ones
            else {
                // then create one
                Sequence sequence = Sequence.findAllByOrganism(organism, [max: 1, sort: "end", order: "desc"]).first()
                userOrganismPreference = new UserOrganismPreference(
                        user: user
                        , organism: organism
                        , clientToken: clientToken
                        , sequence: sequence
                        , currentOrganism: true
                        , nativeTrackList: configWrapperService.getNativeTrackSelectorDefaultOn()
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
                && userOrganismPreferenceDTO.sequence.name == organismPreference.sequence.name
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
        if (userOrganismPreference.sequence) {
            affected += UserOrganismPreference.executeUpdate(
                    "update UserOrganismPreference  pref set pref.currentOrganism = false " +
                            "where pref.id != :prefId and pref.user = :user and pref.organism = :organism and pref.sequence = :sequence and pref.lastUpdated < :lastUpdated ",
                    [prefId: userOrganismPreference.id, user: user, organism: userOrganismPreference.organism, sequence: userOrganismPreference.sequence, lastUpdated: newDate])
        }

        // if the preference is not set correctly, we have to make sure we add it correctly
        affected += UserOrganismPreference.executeUpdate(
                "update UserOrganismPreference  pref set pref.currentOrganism = true " +
                        "where pref.id = :prefId and pref.user = :user and pref.clientToken = :clientToken and pref.currentOrganism = false ",
                [prefId: userOrganismPreference.id, user: user, clientToken: clientToken])

        return affected
    }

    UserOrganismPreferenceDTO setCurrentSequence(User user, Sequence sequence, String clientToken) {
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = getCurrentOrganismPreference(user, sequence.name, clientToken) ?: null
        if (userOrganismPreferenceDTO.sequence.id == sequence.id) {
            log.info "Same sequence so not changing preference"
            return userOrganismPreferenceDTO
        }

        evaluateSaves(true, clientToken)
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndSequenceAndCurrentOrganismAndClientToken(user, sequence, true, clientToken, [sort: "lastUpdated", order: "desc", max: 1])
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndSequenceAndClientToken(user, sequence, clientToken, [sort: "lastUpdated", order: "desc", max: 1])
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndSequenceAndCurrentOrganism(user, sequence, true, [sort: "lastUpdated", order: "desc", max: 1])
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndSequence(user, sequence, [sort: "lastUpdated", order: "desc", max: 1])

        if (!userOrganismPreference) {
            // then create one
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: sequence.organism
                    , clientToken: clientToken
                    , sequence: sequence
                    , nativeTrackList: configWrapperService.getNativeTrackSelectorDefaultOn()
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

    private
    static UserOrganismPreference getMostRecentPreference(List<UserOrganismPreference> userOrganismPreferences) {
        if (userOrganismPreferences) {
            return userOrganismPreferences.sort() { a, b ->
                b.lastUpdated <=> a.lastUpdated
            }.first()
        }
        return null
    }

    UserOrganismPreference setCurrentSequenceInDB(User user, Sequence sequence, String clientToken) {
        Organism organism = sequence.organism
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientTokenAndSequence(user, organism, clientToken, sequence, [sort: "lastUpdated", order: "desc"])
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
                    , sequence: sequence
                    , nativeTrackList: configWrapperService.getNativeTrackSelectorDefaultOn()
                    , clientToken: clientToken
            ).save(flush: true, insert: true)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.sequence = sequence
            userOrganismPreference.save(flush: true, insert: false)
        }
        setOtherCurrentOrganismsFalseInDB(userOrganismPreference, user, clientToken)
        return userOrganismPreference
    }

    UserOrganismPreferenceDTO setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp, String clientToken) {
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = getCurrentOrganismPreference(permissionService.currentUser, sequenceName, clientToken)
        if (userOrganismPreferenceDTO.sequence.name != sequenceName || userOrganismPreferenceDTO.sequence?.organism?.id != userOrganismPreferenceDTO.organism.id) {
            Organism organism = Organism.findById(userOrganismPreferenceDTO.organism.id)
            Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)
            userOrganismPreferenceDTO.sequence = getDTOFromSequence(sequence)
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
                log.debug "DTO: ${userOrganismPreferenceDTOEntry.key as JSON}"
                log.debug "value date : ${saveSequenceLocationMap.get(userOrganismPreferenceDTOEntry.key)}"
                log.debug "value date 2 : ${userOrganismPreferenceDTOEntry.value}"
                if (onlySaveToken && onlySaveToken == userOrganismPreferenceDTOEntry.key.clientToken) {
                    evaluateSave(userOrganismPreferenceDTOEntry.value, userOrganismPreferenceDTOEntry.key, forceSaves)
                } else {
                    evaluateSave(userOrganismPreferenceDTOEntry.value, userOrganismPreferenceDTOEntry.key, forceSaves)
                }
            }
        } catch (e) {
            log.warn("Problem saving preference: " + e)
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

    def scheduleDbSave(Date date, UserOrganismPreferenceDTO preferenceDTO) {
        // these should replace, though
        if (!currentlySavingLocation.contains(preferenceDTO.clientToken)) {
            // saves with the latest one now
            currentlySavingLocation.add(preferenceDTO.clientToken)
        }
        saveSequenceLocationMap.put(preferenceDTO, date)
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
        Sequence sequence = Sequence.findByNameAndOrganism(userOrganismPreferenceDTO.sequence.name, organism)
        User user = User.findByUsername(userOrganismPreferenceDTO.user.username)
        UserOrganismPreference userOrganismPreference = new UserOrganismPreference(
                organism: organism
                , user: user
                , sequence: sequence
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
        Sequence sequence = userOrganismPreference.sequence
        SequenceDTO sequenceDTO = getDTOFromSequence(sequence)
        OrganismDTO organismDTO = getDTOFromOrganism(userOrganismPreference.organism)
        UserDTO userDTO = getDTOFromUser(userOrganismPreference.user)
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = new UserOrganismPreferenceDTO(
                organism: organismDTO
                , sequence: sequenceDTO
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
        String sequenceName = preferenceDTO.sequence.name
        String clientToken = preferenceDTO.clientToken
        Integer startBp = preferenceDTO.startbp
        Integer endBp = preferenceDTO.endbp

        OrganismDTO currentOrganism = getCurrentOrganismPreference(user, sequenceName, clientToken)?.organism
        if (!currentOrganism) {
            throw new AnnotationException("Organism preference is not set for user")
        }
        Organism organism = Organism.findById(currentOrganism.id)
        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)
        if (!sequence) {
            throw new AnnotationException("Sequence name is invalid ${sequenceName} and organism ${organism as JSON}")
        }

        def userOrganismPreferences = UserOrganismPreference.createCriteria().list {
            createAlias('sequence', 'sequence', org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN)
            and {
                eq("user", user)
                eq("clientToken", clientToken)
                eq("sequence.name", sequenceName)
                eq("organism", organism)
            }
        }
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(getMostRecentPreference(userOrganismPreferences), user, clientToken)
        }
        UserOrganismPreference userOrganismPreference = getMostRecentPreference(userOrganismPreferences)
        if (!userOrganismPreference) {
            log.debug "Creating a new PReference"
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , currentOrganism: true
                    , sequence: sequence
                    , organism: organism
                    , clientToken: clientToken
            ).save(insert: true)
        }
        log.debug "init save ${userOrganismPreference as JSON}"

        log.debug "version ${userOrganismPreference.version} for ${userOrganismPreference.organism.commonName} ${userOrganismPreference.currentOrganism}"

        userOrganismPreference.refresh()

        log.debug "refresh save ${userOrganismPreference as JSON}"

        userOrganismPreference.clientToken = clientToken
        userOrganismPreference.currentOrganism = true
        userOrganismPreference.sequence = sequence

        // use the current value if we aren't setting it
        if (userOrganismPreference.startbp) {
            userOrganismPreference.startbp = startBp ?: userOrganismPreference.startbp
        } else {
            userOrganismPreference.startbp = startBp ?: 0
        }

        if (userOrganismPreference.endbp) {
            userOrganismPreference.endbp = endBp ?: userOrganismPreference.endbp
        } else {
            userOrganismPreference.endbp = endBp ?: sequence.end
        }

        userOrganismPreference.save(flush: true, insert: false)
        log.debug "final save ${userOrganismPreference as JSON}"
    }

    UserOrganismPreferenceDTO getCurrentOrganismPreference(User user, String sequenceName, String clientToken) {

        UserOrganismPreferenceDTO preference = getSessionPreference(clientToken)
        preference = preference ?: getSavingPreferences(user, sequenceName, clientToken)
        log.debug "found in-memory preference: ${preference ? preference as JSON : null}"
        return preference ?: getDTOFromPreference(getCurrentOrganismPreferenceInDB(user, sequenceName, clientToken))
    }

    def getSavingPreferences(User user, String sequenceName, String clientToken) {
        return saveSequenceLocationMap.keySet().find() {
            it.clientToken == clientToken && it.user.username == user.username && it.sequence.name == sequenceName
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
            Sequence sequence = sequenceName ? Sequence.findByNameAndOrganism(sequenceName, organism) : userOrganismPreference.sequence
            UserOrganismPreference newPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
                    , startbp: userOrganismPreference.startbp
                    , endbp: userOrganismPreference.endbp
                    , nativeTrackList: configWrapperService.getNativeTrackSelectorDefaultOn()
                    , clientToken: clientToken
            ).save(insert: true, flush: true)
            return newPreference
        }

        // 4 - if none at all exist, then we create one
        if (!userOrganismPreference) {
            // find a random organism based on sequence
            Sequence sequence = sequenceName ? Sequence.findByName(sequenceName) : null
            Set<Organism> organisms = permissionService.getOrganisms(user)
            Organism organism = null
            if (sequence) {
                organism = sequence.organism
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
            sequence = sequence ?: Sequence.findAllByOrganism(organism, [sort: "end", order: "desc", max: 1]).first()
            UserOrganismPreference newUserOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
                    , nativeTrackList: configWrapperService.getNativeTrackSelectorDefaultOn()
                    , clientToken: clientToken
            )
            if (sequence) {
                newUserOrganismPreference.startbp = sequence.start
                newUserOrganismPreference.endbp = sequence.end
            }
            newUserOrganismPreference.save(insert: true, flush: true)
            return newUserOrganismPreference
        }

        return userOrganismPreference
    }

    UserOrganismPreference getCurrentOrganismPreferenceInDB(String token) {
        getCurrentOrganismPreferenceInDB(permissionService.currentUser, null, token)
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
