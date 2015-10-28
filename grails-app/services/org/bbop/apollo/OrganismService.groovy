package org.bbop.apollo

import grails.transaction.Transactional
import org.hibernate.Hibernate
import org.hibernate.Session

@Transactional
class OrganismService {

    def sessionFactory
    def featureService

    /**
     * A good example of executing bulk operations from hibernate
     * http://mrhaki.blogspot.com/2014/03/grails-goodness-using-hibernate-native.html
     * @param organism
     * @return
     */
    def deleteAllFeaturesForOrganism(Organism organism) {

        // the very slow way

        int count = 0
        //final Session session = sessionFactory.currentSession

        def list=Feature.withCriteria() {
            featureLocations {
                sequence {
                    eq("organism",organism)
                }
            }
        }

        list.each {
            it.delete();
        }


        organism.save(flush: true )
        return count
    }
}
