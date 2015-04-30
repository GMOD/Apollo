package org.bbop.apollo

import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum

@Transactional
class PreferenceService {

    def permissionService

    Organism getCurrentOrganismForCurrentUser(){
        return getCurrentOrganism(permissionService.currentUser)
    }

    /**
     * Get the current user preference.
     * If no prefrence, then set one
     * @param user
     * @return
     */
    Organism getCurrentOrganism(User user){
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByCurrentOrganismAndUser(true,user)
        if(!userOrganismPreference){
            userOrganismPreference = UserOrganismPreference.findByUser(user)

            if(!userOrganismPreference){
                Organism organism = permissionService.getOrganisms(user).iterator()?.next()
                if(!organism){
                    throw new PermissionException("User has no access to any organisms!")
                }
                userOrganismPreference = new UserOrganismPreference(
                        user: user
                        ,organism: organism
                        ,sequence: organism.sequences.iterator().next()
                ).save()
            }

            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true )
        }

        return userOrganismPreference.organism
    }

    def setCurrentOrganism(User user,Organism organism) {
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user,organism)
        if(!userOrganismPreference){
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    ,organism: organism
                    ,currentOrganism: true
                    ,sequence: organism.sequences.iterator().next()
            ).save(flush:true)
        }
        else{
            userOrganismPreference.currentOrganism = true ;
            userOrganismPreference.save()
        }
        Session session = SecurityUtils.subject.getSession(false)
        String organismJBrowseDirectory = session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value,organism.directory)

        setOtherCurrentOrganismsFalse(userOrganismPreference,user)
    }

    def setOtherCurrentOrganismsFalse(UserOrganismPreference userOrganismPreference, User user) {
        UserOrganismPreference.executeUpdate("update UserOrganismPreference  pref set pref.currentOrganism = false where pref.id != :prefId and pref.user = :user ",[prefId:userOrganismPreference.id,user:user])
    }

    def setCurrentSequence(User user,Sequence sequence) {
        Organism organism = sequence.organism
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user,organism)
        if(!userOrganismPreference){
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    ,organism: organism
                    ,currentOrganism: true
                    ,sequence: sequence
            ).save(flush:true)
        }
        else{
            userOrganismPreference.currentOrganism = true ;
            userOrganismPreference.sequence = sequence
            userOrganismPreference.save()
        }
        Session session = SecurityUtils.subject.getSession(false)
        String organismJBrowseDirectory = session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value,organism.directory)

        setOtherCurrentOrganismsFalse(userOrganismPreference,user)
    }

    UserOrganismPreference setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp) {
        User currentUser = permissionService.currentUser
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganism(currentUser,true)
        if(!userOrganismPreference){
            userOrganismPreference = UserOrganismPreference.findByUser(currentUser)
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true )
        }
        if(!userOrganismPreference){
            throw new AnnotationException("Organism preference is not set for user")
        }

        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName,userOrganismPreference.organism)
        if(!sequence){
            throw new AnnotationException("Sequence name is invalid ${sequenceName}")
        }

        Session session = SecurityUtils.subject.getSession(false)
        String organismJBrowseDirectory = session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value,sequence.organism.directory)

        userOrganismPreference.sequence = sequence
        userOrganismPreference.setStartbp(startBp ?: 0 )
        userOrganismPreference.setEndbp(endBp ?: sequence.end)
        userOrganismPreference.save(flush: true )

    }
}
