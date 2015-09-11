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
        final Session session = sessionFactory.currentSession

        List topLevelFeatures = session.createSQLQuery("select f.unique_name from feature f join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id}").list()
        println "top level features ${topLevelFeatures.size()}"
        for(def feat in topLevelFeatures){
            println "feat ${feat}"
        }

        // there are a few missing heere
        println "deleted owners " + session.createSQLQuery("delete from feature_grails_user where EXISTS (select 'x' from feature_grails_user fgu JOIN feature f ON fgu.feature_owners_id=f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()

//        println "deleted genotypes " + session.createSQLQuery("delete from feature_genotype where EXISTS (select 'x' from feature_genotype fgu JOIN feature f ON fgu.feature_id=f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()


        println "deleted dbxrefs " + session.createSQLQuery("delete from feature_dbxref where EXISTS (select 'x' from feature_dbxref fgu JOIN feature f ON fgu.feature_featuredbxrefs_id=f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()
        println "deleted property " + session.createSQLQuery("delete from feature_property where EXISTS (select 'x' from feature_property fgu JOIN feature f ON fgu.feature_id =f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()
        println "deleted child relationship " + session.createSQLQuery("delete from feature_relationship where EXISTS (select 'x' from feature_relationship fgu JOIN feature f ON fgu.child_feature_id =f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()
        println "deleted parent relationship " + session.createSQLQuery("delete from feature_relationship where EXISTS (select 'x' from feature_relationship fgu JOIN feature f ON fgu.parent_feature_id =f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()

        // get all of the parent features


        println "deleted location " + session.createSQLQuery("delete from feature_location where EXISTS (select 'x' from feature_location fl join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()

        println "featre events ${FeatureEvent.count}"
        FeatureEvent.findAll().each {
            println it.uniqueName
        }
        println "featres ${Feature.count}"
        Feature.findAll().each {
            println it.uniqueName
        }
        println "deleted feature_event " + session.createSQLQuery("delete from feature_event where unique_name in (:featureList) ").setParameterList("featureList",topLevelFeatures).executeUpdate()
        println "deleted feature " + session.createSQLQuery("delete from feature where not EXISTS (select 'x' from feature f join feature_location fl on fl.feature_id=f.id)").executeUpdate()

        organism.save(flush: true )
        return count
    }
}
