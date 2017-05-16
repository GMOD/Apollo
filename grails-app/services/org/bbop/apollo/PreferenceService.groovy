package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class PreferenceService {

    def permissionService

    def getSessionPreference(String clientToken) {
        Session session = SecurityUtils.subject.getSession(false)
        if (session) {
            // should be client_token , JSONObject
//            Map<String, String> preferences = JSON.parse(session.getAttribute(FeatureStringEnum.PREFERENCE.getValue()))
            String preferenceString = session.getAttribute(FeatureStringEnum.PREFERENCE.getValue() + "::" + clientToken)?.toString()
            if (!preferenceString) return null
            JSONObject preferenceObject = JSON.parse(preferenceString) as JSONObject
            if (preferenceObject) {
                try {
                    println "preference object ${preferenceObject as JSON}"
//                        JSONObject preferenceJSON = JSON.parse(preferenceObject) as JSONObject
                    def organismId = preferenceObject.organism.id as Long
                    return Organism.get(organismId) ?: Organism.findById(organismId)
                } catch (e) {
                    log.error("failed to parse ${preferenceObject}:\n" + e)
                    return null
                }
            } else {
                log.debug "No preference found on session"
            }
        } else {
            log.debug "No session found"
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
        Organism organism = getSessionPreference(clientToken)
        println "found organism in session ${organism} so returning"
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
        Organism organism = getSessionPreference(token)
        if(organism){
          return organism
        }
        else{
            return getOrganismForTokenInDB(token)
        }
    }


    def setCurrentOrganism(User user, Organism organism, String clientToken) {
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientToken(user, organism, clientToken, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(), user, clientToken)
        }

        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: Sequence.findByOrganism(organism)
                    , clientToken: clientToken
            ).save(flush: true, insert: true)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.save(flush: true, insert: false)
        }
        setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
    }

    protected static
    def setOtherCurrentOrganismsFalse(UserOrganismPreference userOrganismPreference, User user, String clientToken) {
        UserOrganismPreference.executeUpdate(
                "update UserOrganismPreference  pref set pref.currentOrganism = false " +
                        "where pref.id != :prefId and pref.user = :user and pref.clientToken = :clientToken",
                [prefId: userOrganismPreference.id, user: user, clientToken: clientToken])
    }

    def setCurrentSequence(User user, Sequence sequence, String clientToken) {
        Organism organism = sequence.organism
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientTokenAndSequence(user, organism, clientToken, sequence, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences for sequence and organism: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(), user, clientToken)
        }

        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null

        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
                    , clientToken: clientToken
            ).save(flush: true, insert: true)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.sequence = sequence
            userOrganismPreference.save(flush: true, insert: false)
        }
        setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
    }

    UserOrganismPreference setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp, String clientToken) {
        User currentUser = permissionService.currentUser

        Organism currentOrganism = getCurrentOrganismPreference(currentUser, sequenceName, clientToken)?.organism
        if (!currentOrganism) {
            throw new AnnotationException("Organism preference is not set for user")
        }
        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, currentOrganism)
        if (!sequence) {
            throw new AnnotationException("Sequence name is invalid ${sequenceName}")
        }

//        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(currentUser, true, clientToken,[sort: "lastUpdated", order: "desc"])
        def userOrganismPreferences = UserOrganismPreference.createCriteria().list {
            createAlias('sequence', 'sequence', org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN)
            and {
                eq("user", currentUser)
                eq("clientToken", clientToken)
                eq("sequence.name", sequenceName)
                eq("organism", currentOrganism)
            }
        }
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(), currentUser, clientToken)
        }
        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: currentUser
                    , currentOrganism: true
                    , sequence: sequence
                    , organism: currentOrganism
                    , clientToken: clientToken
            ).save(insert: true)
        }

        log.debug "version ${userOrganismPreference.version} for ${userOrganismPreference.organism.commonName} ${userOrganismPreference.currentOrganism}"

        userOrganismPreference.refresh()

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
    }


    UserOrganismPreference getCurrentOrganismPreference(User user, String sequenceName, String clientToken) {
        if (!user && !clientToken) {
            log.warn("No organism preference if no user ${user} or client token ${clientToken}")
            return null
        }

        // 1 - if a user exists, look up their client token and if they have a current organism.
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(user, true, clientToken, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(), user, clientToken)
        }
        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (userOrganismPreference) {
            return userOrganismPreference
        }

        // 2 - if there is not a current organism for that token, then grab the first non-current one (unlikely) and make it current
        userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(user, false, clientToken, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(), user, clientToken)
        }
        userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (userOrganismPreference) {
            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true, insert: false)
            return userOrganismPreference
        }

        //3 - if none at all exist, we should ignore the client token and look it up by the user (missing), saving it for the current client token
        // we create a new one off of that, but for this client token
        userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganism(user, true, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(), user, clientToken)
        }
        userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null

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
                    , clientToken: clientToken
            ).save(insert: true, flush: true)
            return newPreference
        }

        // 4 - if none at all exist, then we create one
        if (!userOrganismPreference) {
            // find a random organism based on sequence
            Sequence sequence = sequenceName ? Sequence.findByName(sequenceName) : null
            Set<Organism> organisms = permissionService.getOrganisms(user)
//            Organism organism = sequence ? sequence.organism : organisms?.first()
            Organism organism
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

            sequence = sequence ?: organism.sequences.first()

            UserOrganismPreference newUserOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
                    , clientToken: clientToken
                    , startbp: sequence.start
                    , endbp: sequence.end
            ).save(insert: true, flush: true)

            return newUserOrganismPreference
        }

        return userOrganismPreference
    }

    Organism getOrganismFromPreferences(String clientToken) {
        getCurrentOrganismPreference(permissionService.currentUser, null, clientToken)?.organism
    }

    Organism getOrganismFromPreferences(User user, String trackName, String clientToken) {
        getCurrentOrganismPreference(user, trackName, clientToken)?.organism
    }

    UserOrganismPreference getCurrentOrganismPreference(String token) {
        getCurrentOrganismPreference(permissionService.getCurrentUser(), null, token)
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
