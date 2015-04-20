package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class PreferenceService {

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
        UserOrganismPreference.executeUpdate("update UserOrganismPreference  pref set pref.currentOrganism = false where pref.id != :prefId ",[prefId:userOrganismPreference.id])
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
        UserOrganismPreference.executeUpdate("update UserOrganismPreference  pref set pref.currentOrganism = false where pref.id != :prefId ",[prefId:userOrganismPreference.id])
    }
}
