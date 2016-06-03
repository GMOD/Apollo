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
/**
 * Get the current user preference.
 * If no preference, then set one
 * @param user
 * @return
 */
//    Organism getCurrentOrganism(User user, String clientToken) {
//        log.debug "getting current organism for token: ${clientToken}"
//        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByCurrentOrganismAndUserAndClientToken(true, user, clientToken)
//
//        // if there is not a current one, we see if there is another one for the same token
//        if (!userOrganismPreference) {
//            userOrganismPreference = UserOrganismPreference.findByCurrentOrganismAndUserAndClientToken(false, user, clientToken)
//        }
//
//        UserOrganismPermission getOrganismFromPreferences(user,null,clientToken)
//
//        // if there are none, then we have to create a new one
//        if (!userOrganismPreference) {
//            Iterator i = permissionService.getOrganisms(user).iterator();
//            if (i.hasNext()) {
//                Organism organism = i.next()
//                userOrganismPreference = new UserOrganismPreference(
//                        user: user
//                        , organism: organism
//                        , sequence: Sequence.findByOrganism(organism)
//                        , currentOrganism: true
//                        , clientToken: clientToken
//                ).save()
//            } else {
//                throw new PermissionException("User has no access to any organisms!")
//            }
//        }
//
//        userOrganismPreference.currentOrganism = true
//        userOrganismPreference.save(flush: true)
//
//        return userOrganismPreference.organism
//    }


    def setCurrentOrganism(User user, Organism organism, String clientToken) {
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganismAndClientToken(user, organism, clientToken)
//        UserOrganismPreference userOrganismPreference = getCurrentOrganismPreference(user,null,clientToken)
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
        println "setting other orgs false for ${clientToken}"
        UserOrganismPreference.executeUpdate(
                "update UserOrganismPreference  pref set pref.currentOrganism = false " +
                        "where pref.id != :prefId and pref.user = :user and pref.clientToken = :clientToken",
                [prefId: userOrganismPreference.id, user: user, clientToken: clientToken])
    }

    def setCurrentSequence(User user, Sequence sequence, String clientToken) {
        Organism organism = sequence.organism
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganismAndClientTokenAndSequence(user, organism, clientToken, sequence)
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
//        UserOrganismPreference userOrganismPreference = getCurrentOrganismPreference(currentUser,sequenceName,clientToken)
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(currentUser, true, clientToken)
        if (!userOrganismPreference) {
            userOrganismPreference = UserOrganismPreference.findByUser(currentUser)
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
        userOrganismPreference.save(flush: true)
    }


    UserOrganismPreference getCurrentOrganismPreference(User user, String trackName, String clientToken) {
        if (!user && !clientToken){
            log.warn("No organism preference if no user ${user} or client token ${clientToken}")
            return null
        }
        // 1 - if a user exists, look up their client token and if they have a current organism.
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(user, true, clientToken)
        if (userOrganismPreference) {
            return userOrganismPreference
        }

        // 2 - if there is not a current organism for that token, then grab the first non-current one (unlikely) and make it current
        userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(user, false, clientToken)
        if (userOrganismPreference) {
            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true, insert: false)
            return userOrganismPreference
        }

        //3 - if none at all exist, we should ignore the client token and look it up by the user (missing), saving it for the current client token
        // we create a new one off of that, but for this client token
        userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(user, true)
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndCurrentOrganism(user, false)
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
            Sequence sequence = Sequence.findByName(trackName)
            Organism organism = sequence ? sequence.organism : permissionService.getOrganisms(user).first()
            if (!organism && permissionService.isAdmin()) {
                organism = Organism.first()
            }
            if (!organism) {
                throw new PermissionException("User does not have permission for any organisms.")
            }

            UserOrganismPreference newUserOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
                    , clientToken: clientToken
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
