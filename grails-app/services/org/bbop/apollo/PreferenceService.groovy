package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class PreferenceService {

    def permissionService

    Organism getCurrentOrganismForCurrentUser(String clientToken) {
        println "PS: getCurrentOrganismForCurrentUser ${clientToken}"
        if (permissionService.currentUser == null) {
            return getOrganismForToken(clientToken)
        } else {
            return getCurrentOrganism(permissionService.currentUser, clientToken)
        }
//        return permissionService.currentUser == null ? null : getCurrentOrganism(permissionService.currentUser,clientToken);
    }

    Organism getOrganismForToken(String s) {
        println "token for org ${s}"
        if (s.isLong()) {
            println "is long "
            return Organism.findById(Long.parseLong(s))
        } else {
            println "is NOT long "
            return Organism.findByCommonNameIlike(s)
        }

    }
/**
 * Get the current user preference.
 * If no preference, then set one
 * @param user
 * @return
 */
    Organism getCurrentOrganism(User user, String clientToken) {
        println "getting current organism for token: ${clientToken}"
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByCurrentOrganismAndUserAndClientToken(true, user, clientToken)

        // if there is not a current one, we see if there is another one for the same token
        if (!userOrganismPreference) {
            userOrganismPreference = UserOrganismPreference.findByCurrentOrganismAndUserAndClientToken(false, user, clientToken)
        }

        // if there are none, then we have to create a new one
        if (!userOrganismPreference) {
            Iterator i = permissionService.getOrganisms(user).iterator();
            if (i.hasNext()) {
                Organism organism = i.next()
                userOrganismPreference = new UserOrganismPreference(
                        user: user
                        , organism: organism
                        , sequence: Sequence.findByOrganism(organism)
                        , currentOrganism: true
                        , clientToken: clientToken
                ).save()
            } else {
                throw new PermissionException("User has no access to any organisms!")
            }
        }

        userOrganismPreference.currentOrganism = true
        userOrganismPreference.save(flush: true)

        return userOrganismPreference.organism
    }


    def setCurrentOrganism(User user, Organism organism, String clientToken) {
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganismAndClientToken(user, organism, clientToken)
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: Sequence.findByOrganism(organism)
                    , clientToken: clientToken
            ).save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
        }
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
            ).save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.sequence = sequence
            userOrganismPreference.save()
            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
        }
    }

    UserOrganismPreference setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp, String clientToken) {
        User currentUser = permissionService.currentUser
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
        userOrganismPreference.save()
    }

    Organism getOrganismFromPreferences(User user, String trackName,String clientToken) {
        if(user!=null) {
            UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(user, true,clientToken)
            if(userOrganismPreference){
                return userOrganismPreference.organism
            }

            if (!userOrganismPreference) {
                userOrganismPreference = UserOrganismPreference.findByUserAndClientTokenAndCurrentOrganism(user,clientToken,false)
                if(userOrganismPreference){
                    setOtherCurrentOrganismsFalse(userOrganismPreference, user,clientToken)
                    userOrganismPreference.currentOrganism = true
                    userOrganismPreference.save(flush: true)
                    return userOrganismPreference.organism
                }
            }

            if (!userOrganismPreference) {
                // find a random organism based on sequence
                Sequence sequence = Sequence.findByName(trackName)
                Organism organism  = sequence.organism

                userOrganismPreference = new UserOrganismPreference(
                        user: user
                        , organism: organism
                        , currentOrganism: true
                        , sequence: sequence
                        , clientToken: clientToken
                ).save(insert: true)
                return userOrganismPreference.organism
            }
        }
        log.warn("No organism preference if no user")
        return null

    }
}
