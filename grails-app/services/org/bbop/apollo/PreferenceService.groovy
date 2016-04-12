package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class PreferenceService {

    def permissionService

    Organism getCurrentOrganismForCurrentUser() {
        return permissionService.currentUser == null ? null : getCurrentOrganism(permissionService.currentUser);
    }

    /**
     * Get the current user preference.
     * If no preference, then set one
     * @param user
     * @return
     */
    Organism getCurrentOrganism(User user) {
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
                            , sequence: Sequence.findByOrganism(organism)
                            , currentOrganism: true
                    ).save()
                } else {
                    throw new PermissionException("User has no access to any organisms!")
                }
            }

            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true)
        }

        return userOrganismPreference.organism
    }

    Sequence getCurrentSequenceForCurrentUser() {
        return getCurrentSequence(permissionService.currentUser)
    }

    /**
     * Get the current user preference.
     * If no preference, then set one
     * @param user
     * @return
     */
    Sequence getCurrentSequence(User user) {
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
                            , sequence: Sequence.findByOrganism(organism)
                            , currentOrganism: true
                    ).save()
                } else {
                    throw new PermissionException("User has no access to any organisms!")
                }
            }

            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true)
        }

        return userOrganismPreference.sequence
    }

    def setCurrentOrganism(User user, Organism organism,String clientToken) {
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganismAndClientToken(user, organism,clientToken)
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: Sequence.findByOrganism(organism)
                    , clientToken: clientToken
            ).save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user,clientToken)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user,clientToken)
        }
    }

    protected static  def setOtherCurrentOrganismsFalse(UserOrganismPreference userOrganismPreference, User user,String clientToken) {
        println "setting other orgs false for ${clientToken}"
        UserOrganismPreference.executeUpdate(
                "update UserOrganismPreference  pref set pref.currentOrganism = false " +
                        "where pref.id != :prefId and pref.user = :user and pref.clientToken = :clientToken",
                [prefId: userOrganismPreference.id, user: user,clientToken: clientToken])
    }

    def setCurrentSequence(User user, Sequence sequence,String clientToken) {
        Organism organism = sequence.organism
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganismAndClientToken(user, organism,clientToken)
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
                    ,clientToken: clientToken
            ).save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user,clientToken)
        } else
        if(!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.sequence = sequence
            userOrganismPreference.save()
            setOtherCurrentOrganismsFalse(userOrganismPreference, user,clientToken)
        }
    }

    UserOrganismPreference setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp,String clientToken) {
        User currentUser = permissionService.currentUser
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(currentUser, true,clientToken)
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

}
