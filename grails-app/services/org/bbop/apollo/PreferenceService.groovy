package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONObject


@Transactional
class PreferenceService {

    def permissionService
    def bookmarkService

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

    Organism getOrganismFromInput(JSONObject inputObject) {

        if (inputObject.has(FeatureStringEnum.ORGANISM.value)) {
            String organismString = inputObject.getString(FeatureStringEnum.ORGANISM.value)
            Organism organism = getOrganismForToken(organismString)
            if(organism){
                log.debug "return organism ${organism} by ID ${organismString}"
                return organism
            }
            else{
                log.info "organism not found ${organismString}"
            }
        }
        return null
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

//    def setCurrentOrganism(User user, Organism organism) {
//        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism)
    def setCurrentOrganism(User user, Organism organism, String clientToken) {
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganismAndClientToken(user, organism, clientToken,[max: 1, sort: "lastUpdated", order: "desc"])
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , bookmark: Bookmark.findByOrganism(organism)
//                    , sequence: Sequence.findByOrganism(organism)
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

    def setCurrentBookmark(User user, Bookmark bookmark,String clientToken) {
        Organism organism = bookmark.organism
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism)
//        Bookmark bookmark = bookmarkService.generateBookmarkForSequence(sequence)
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , bookmark: bookmark
                    , clientToken: clientToken
            ).save(flush: true)
            setOtherCurrentOrganismsFalse(userOrganismPreference, user,clientToken)
        }
        else
        if(!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.bookmark = bookmark
            userOrganismPreference.save()
            setOtherCurrentOrganismsFalse(userOrganismPreference, user,clientToken)
        }
        else{
            userOrganismPreference.bookmark = bookmark
            userOrganismPreference.save()
        }
    }

//    def setCurrentSequence(User user, Sequence sequence) {
    def setCurrentSequence(User user, Sequence sequence, String clientToken) {
        Organism organism = sequence.organism
//        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism)
        Bookmark bookmark = bookmarkService.generateBookmarkForSequence(sequence)
        if(user && bookmark){
            user.addToBookmarks(bookmark)
        }
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganismAndClientTokenAndSequence(user, organism, clientToken, sequence,[max: 1, sort: "lastUpdated", order: "desc"])
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , bookmark: bookmark
//                    , sequence: sequence
                    , clientToken: clientToken
            ).save(flush: true,insert: true )
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.bookmark = bookmark
            userOrganismPreference.save()
            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
        }
        setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
    }

    UserOrganismPreference setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp, String clientToken) {
        User currentUser = permissionService.currentUser
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(currentUser, true, clientToken,[max: 1, sort: "lastUpdated", order: "desc"])
        if (!userOrganismPreference) {
            userOrganismPreference = UserOrganismPreference.findByUser(currentUser,[max: 1, sort: "lastUpdated", order: "desc"])
        }
        if (!userOrganismPreference) {
            throw new AnnotationException("Organism preference is not set for user")
        }

        Bookmark bookmark ;
        if(BookmarkService.isProjectionString(sequenceName)){
            JSONObject jsonObject = JSON.parse(sequenceName) as JSONObject
            jsonObject.put(FeatureStringEnum.CLIENT_TOKEN.value,clientToken)
            bookmark = bookmarkService.convertJsonToBookmark(jsonObject)
        }
        else{
            Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, userOrganismPreference.organism)
            bookmark = bookmarkService.generateBookmarkForSequence(sequence)
        }

        userOrganismPreference.refresh()

        userOrganismPreference.clientToken = clientToken
        userOrganismPreference.currentOrganism = true
        userOrganismPreference.bookmark = bookmark
        userOrganismPreference.setStartbp(startBp ?: 0)
        userOrganismPreference.setEndbp(endBp ?: bookmark.end)
        userOrganismPreference.save(flush: true)
    }


    UserOrganismPreference getCurrentOrganismPreference(User user, String trackName, String clientToken) {
        if (!user && !clientToken){
            log.warn("No organism preference if no user ${user} or client token ${clientToken}")
            return null
        }
        // 1 - if a user exists, look up their client token and if they have a current organism.
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(user, true, clientToken,[max: 1, sort: "lastUpdated", order: "desc"])
        if (userOrganismPreference) {
            return userOrganismPreference
        }

        // 2 - if there is not a current organism for that token, then grab the first non-current one (unlikely) and make it current
        userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(user, false, clientToken,[max: 1, sort: "lastUpdated", order: "desc"])
        if (userOrganismPreference) {
            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true, insert: false)
            return userOrganismPreference
        }

        //3 - if none at all exist, we should ignore the client token and look it up by the user (missing), saving it for the current client token
        // we create a new one off of that, but for this client token
        userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(user, true,[max: 1, sort: "lastUpdated", order: "desc"])
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndCurrentOrganism(user, false,[max: 1, sort: "lastUpdated", order: "desc"])
        if (userOrganismPreference) {
            Organism organism = userOrganismPreference.organism
            Bookmark bookmark = trackName ? Bookmark.findByNameAndOrganism(trackName, organism) : userOrganismPreference.bookmark
            UserOrganismPreference newPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , bookmark: bookmark
                    , startbp: userOrganismPreference.startbp
                    , endbp: userOrganismPreference.endbp
                    , clientToken: clientToken
            ).save(insert: true, flush: true)
            return newPreference
        }

        // 4 - if none at all exist, then we create one
        if (!userOrganismPreference) {
            // find a random organism based on sequence
            Bookmark bookmark = Bookmark.findByName(trackName)
            Organism organism = bookmark?.organism
            if(!organism){
                def organisms = permissionService.getOrganisms(user)
                organism = organisms ? organisms.first() : null
            }
            if (!organism && permissionService.isAdmin()) {
                organism = Organism.first()
            }
            if (!organism) {
                throw new PermissionException("User does not have permission for any organisms.")
            }


            if(user){
                UserOrganismPreference newUserOrganismPreference = new UserOrganismPreference(
                        user: user
                        , organism: organism
                        , currentOrganism: true
                        , bookmark: bookmark
                        , clientToken: clientToken
                ).save(insert: true, flush: true)
                return newUserOrganismPreference
            }
            else{
                return null
            }
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
        getCurrentOrganismPreference(permissionService.currentUser, null, token)
    }
}
