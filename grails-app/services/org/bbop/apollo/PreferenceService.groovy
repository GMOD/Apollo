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

    def setCurrentOrganism(User user, Organism organism) {
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism)
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: Sequence.findByOrganism(organism)
            ).save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user)
        }
    }

    protected def setOtherCurrentOrganismsFalse(UserOrganismPreference userOrganismPreference, User user) {
        UserOrganismPreference.executeUpdate(
                "update UserOrganismPreference  pref set pref.currentOrganism = false " +
                        "where pref.id != :prefId and pref.user = :user",
                [prefId: userOrganismPreference.id, user: user])
    }

    def setCurrentSequence(User user, Sequence sequence) {
        Organism organism = sequence.organism
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism)
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
            ).save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user)
        } else
        if(!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.sequence = sequence
            userOrganismPreference.save()
            setOtherCurrentOrganismsFalse(userOrganismPreference, user)
        }
    }

    UserOrganismPreference setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp) {
        User currentUser = permissionService.currentUser
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(currentUser, true)
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

        userOrganismPreference.currentOrganism = true
        userOrganismPreference.sequence = sequence
        userOrganismPreference.setStartbp(startBp ?: 0)
        userOrganismPreference.setEndbp(endBp ?: sequence.end)
        userOrganismPreference.save()
    }

}
