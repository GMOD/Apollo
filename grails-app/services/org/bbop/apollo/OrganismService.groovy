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

        // there are a few missing heere
        println "deleted owners " + session.createSQLQuery("delete from feature_grails_user where EXISTS (select 'x' from feature_grails_user fgu JOIN feature f ON fgu.feature_owners_id=f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()

        println "deleted genotypes " + session.createSQLQuery("delete from feature_genotype where EXISTS (select 'x' from feature_genotype fgu JOIN feature f ON fgu.feature_id=f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()
        println "deleted synonyms " + session.createSQLQuery("delete from feature_synonym where EXISTS (select 'x' from feature_synonym fgu JOIN feature f ON fgu.feature_id=f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()

        println "deleted publication " + session.createSQLQuery("delete from feature_publication where EXISTS (select 'x' from feature_publication fgu JOIN feature f ON fgu.feature_feature_publications_id=f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()
        println "deleted phenotypes " + session.createSQLQuery("delete from feature_feature_phenotypes where EXISTS (select 'x' from feature_feature_phenotypes fgu JOIN feature f ON fgu.feature_id=f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()
        println "deleted synonyms " + session.createSQLQuery("delete from feature_synonym where EXISTS (select 'x' from feature_synonym fgu JOIN feature f ON fgu.feature_id=f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()


        println "deleted dbxrefs " + session.createSQLQuery("delete from feature_dbxref where EXISTS (select 'x' from feature_dbxref fgu JOIN feature f ON fgu.feature_featuredbxrefs_id=f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()
        println "deleted property " + session.createSQLQuery("delete from feature_property where EXISTS (select 'x' from feature_property fgu JOIN feature f ON fgu.feature_id =f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()
        println "deleted child relationship " + session.createSQLQuery("delete from feature_relationship where EXISTS (select 'x' from feature_relationship fgu JOIN feature f ON fgu.child_feature_id =f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()
        println "deleted parent relationship " + session.createSQLQuery("delete from feature_relationship where EXISTS (select 'x' from feature_relationship fgu JOIN feature f ON fgu.parent_feature_id =f.id join feature_location fl on fl.feature_id=f.id join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()

        // get all of the parent features


        println "deleted location " + session.createSQLQuery("delete from feature_location where EXISTS (select 'x' from feature_location fl join sequence s on s.id = fl.sequence_id join organism o ON o.id = s.organism_id where o.id = ${organism.id})").executeUpdate()

        println "deleted feature " + session.createSQLQuery("delete from feature where not EXISTS (select 'x' from feature f join feature_location fl on fl.feature_id=f.id)").executeUpdate()

        println "deleted feature_event " + session.createSQLQuery("delete from feature_event where not exists (select 'x' from feature f, feature_event fe where f.unique_name=f.name) ").executeUpdate()

        organism.save(flush: true )
        return count
    }
}
