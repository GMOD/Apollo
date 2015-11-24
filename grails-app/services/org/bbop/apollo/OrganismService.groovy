package org.bbop.apollo

import grails.transaction.Transactional
import org.hibernate.Hibernate
import org.hibernate.Session

@Transactional
class OrganismService {
    def featureService

    def deleteAllFeaturesForOrganism(Organism organism) {
        def list=Feature.withCriteria() {
            featureLocations {
                sequence {
                    eq("organism",organism)
                }
            }
        }


        def uniqueNames=list.collect {
            it.uniqueName
        }

        list.each {
            it.delete();
        }


        def events=FeatureEvent.withCriteria() {
            'in'("uniqueName",uniqueNames)
        }


        events.each {
            it.delete()
        }


        organism.save(flush: true )
        return list.size()
    }
}
