package org.bbop.apollo

import grails.async.Promise
import grails.transaction.Transactional

@Transactional
class PreferenceService {

    def permissionService

    Organism getCurrentOrganismForCurrentUser(String clientToken) {
        log.debug "PS: getCurrentOrganismForCurrentUser ${clientToken}"
        if (permissionService.currentUser == null) {
            return getOrganismForToken(clientToken)
        } else {
//            return getCurrentOrganism(permissionService.currentUser, clientToken)
            return getOrganismFromPreferences(clientToken)
        }
//        return permissionService.currentUser == null ? null : getCurrentOrganism(permissionService.currentUser,clientToken);
    }

    Organism getOrganismForToken(String s) {
        log.debug "token for org ${s}"
        if (s.isLong()) {
            log.debug "is long "
            return Organism.findById(Long.parseLong(s))
        } else {
            log.debug "is NOT long "
            return Organism.findByCommonNameIlike(s)
        }

    }


    def setCurrentOrganism(User user, Organism organism, String clientToken) {
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientToken(user, organism, clientToken,[sort: "lastUpdated", order: "desc"])
        if(userOrganismPreferences.size()>1){
            log.warn("Multiple preferences found: "+userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(),user,clientToken)
        }

        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: Sequence.findByOrganism(organism)
                    , clientToken: clientToken
            ).save(flush: true,insert:true)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.save(flush: true,insert:false)
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
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientTokenAndSequence(user, organism, clientToken, sequence,[sort: "lastUpdated", order: "desc"])
        if(userOrganismPreferences.size()>1){
            log.warn("Multiple preferences for sequence and organism: "+userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(),user,clientToken)
        }

        UserOrganismPreference userOrganismPreference  = userOrganismPreferences ? userOrganismPreferences.first() : null

        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
                    , clientToken: clientToken
            ).save(flush: true,insert: true )
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.sequence = sequence
            userOrganismPreference.save(flush: true, insert:false)
        }
        setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
    }

    UserOrganismPreference setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp, String clientToken) {
        User currentUser = permissionService.currentUser
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(currentUser, true, clientToken,[sort: "lastUpdated", order: "desc"])
        if(userOrganismPreferences.size()>1){
            log.warn("Multiple preferences found: "+userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(),user,clientToken)
        }
        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (!userOrganismPreference) {
            userOrganismPreference = UserOrganismPreference.findByUser(currentUser,[max: 1, sort: "lastUpdated", order: "desc"])
        }
        if (!userOrganismPreference) {
            throw new AnnotationException("Organism preference is not set for user")
        }

        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, userOrganismPreference.organism)
        if (!sequence) {
            throw new AnnotationException("Sequence name is invalid ${sequenceName}")
        }

        log.debug "version ${userOrganismPreference.version} for ${userOrganismPreference.organism.commonName} ${userOrganismPreference.currentOrganism}"

        userOrganismPreference.refresh()

        userOrganismPreference.clientToken = clientToken
        userOrganismPreference.currentOrganism = true
        userOrganismPreference.sequence = sequence
        userOrganismPreference.setStartbp(startBp ?: 0)
        userOrganismPreference.setEndbp(endBp ?: sequence.end)
        userOrganismPreference.save(flush: true,insert:false)
    }


    UserOrganismPreference getCurrentOrganismPreference(User user, String trackName, String clientToken) {
        if (!user && !clientToken){
            log.warn("No organism preference if no user ${user} or client token ${clientToken}")
            return null
        }
        // 1 - if a user exists, look up their client token and if they have a current organism.
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(user, true, clientToken,[sort: "lastUpdated", order: "desc"])
        if(userOrganismPreferences.size()>1){
            log.warn("Multiple preferences found: "+userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(),user,clientToken)
        }
        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (userOrganismPreference) {
            return userOrganismPreference
        }

        // 2 - if there is not a current organism for that token, then grab the first non-current one (unlikely) and make it current
        userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(user, false, clientToken,[sort: "lastUpdated", order: "desc"])
        if(userOrganismPreferences.size()>1){
            log.warn("Multiple preferences found: "+userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(),user,clientToken)
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
        userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganism(user, true,[sort: "lastUpdated", order: "desc"])
        if(userOrganismPreferences.size()>1){
            log.warn("Multiple preferences found: "+userOrganismPreferences.size())
            setOtherCurrentOrganismsFalse(userOrganismPreferences.first(),user,clientToken)
        }
        userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null

        // just grab an adjacent one for that user, that is not current
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndCurrentOrganism(user, false,[max: 1, sort: "lastUpdated", order: "desc"])
        if (userOrganismPreference) {
            Organism organism = userOrganismPreference.organism
            Sequence sequence = trackName ? Sequence.findByNameAndOrganism(trackName, organism) : userOrganismPreference.sequence
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
            Sequence sequence = trackName ? Sequence.findByName(trackName) : null
            Set<Organism> organisms = permissionService.getOrganisms(user)
//            Organism organism = sequence ? sequence.organism : organisms?.first()
            Organism organism
            if(sequence){
                organism = sequence.organism
            }
            if(!organism && organisms){
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
}
