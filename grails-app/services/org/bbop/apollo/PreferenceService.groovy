package org.bbop.apollo

import grails.transaction.Transactional


@Transactional
class PreferenceService {

    def permissionService
    def bookmarkService

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
                            , bookmark: Bookmark.findByOrganism(organism)
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
                            , bookmark: Bookmark.findByOrganism(organism)
                            , currentOrganism: true
                    ).save()
                } else {
                    throw new PermissionException("User has no access to any organisms!")
                }
            }

            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true)
        }
        Bookmark bookmark = userOrganismPreference.bookmark
        String sequenceList = bookmark.sequenceList

        return sequenceList
    }

    def setCurrentOrganism(User user, Organism organism) {
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism)
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , bookmark: Bookmark.findByOrganism(organism)
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

    def setCurrentBookmark(User user, Bookmark bookmark) {
        Organism organism = bookmark.organism
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism)
//        Bookmark bookmark = bookmarkService.generateBookmarkForSequence(sequence)
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , bookmark: bookmark
            ).save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user)
        }
        else
        if(!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.bookmark = bookmark
            userOrganismPreference.save()
            setOtherCurrentOrganismsFalse(userOrganismPreference, user)
        }
        else{
            userOrganismPreference.bookmark = bookmark
            userOrganismPreference.save()
        }
    }

    def setCurrentSequence(User user, Sequence sequence) {
        Organism organism = sequence.organism
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism)
        Bookmark bookmark = bookmarkService.generateBookmarkForSequence(sequence)
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , bookmark: bookmark
            ).save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user)
        }
        else
        if(!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.bookmark = bookmark
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

        Bookmark bookmark = bookmarkService.convertStringToBookmark(sequenceName,userOrganismPreference.organism)
        userOrganismPreference.refresh()

        userOrganismPreference.currentOrganism = true
        userOrganismPreference.bookmark = bookmark
        userOrganismPreference.setStartbp(startBp ?: 0)
        userOrganismPreference.setEndbp(endBp ?: bookmark.end)
        userOrganismPreference.save()
    }

}
