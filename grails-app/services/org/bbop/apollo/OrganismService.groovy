package org.bbop.apollo

import grails.transaction.Transactional
import org.hibernate.Hibernate

@Transactional
class OrganismService {

    def featureService


    def deleteAllFeaturesForOrganism(Organism organism) {

        // the very slow way

        int count = 0

        // there are a few missing heere
//        psql $DBARG -c  "delete from feature_grails_user";
//        psql $DBARG -c  "delete from feature_dbxref";
//        psql $DBARG -c  "delete from feature_property";
//        psql $DBARG -c  "delete from feature_relationship";
//        psql $DBARG -c  "delete from feature_location";
//        psql $DBARG -c  "delete from feature";
//        psql $DBARG -c  "delete from feature_event";

        def features = organism.sequences.featureLocations.feature
        def featureLocations = features.featureLocations
        println "feature locations ${featureLocations.size()}"
        Feature.deleteAll(features)
        FeatureLocation.deleteAll(featureLocations)
//        Feature.deleteAll(organism.sequences.featureLocations)
        organism.sequences.featureLocations.feature.clear()
        organism.sequences.featureLocations.clear()

        // not sure if this will scale properly
//        def featureIds = Feature.executeQuery("select f.id from Feature f join f.featureLocations fl join fl.sequence s join s.organism o where o = :organism and f.childFeatureRelationships is empty ",[organism:organism])
//        println "feature ids ${featureIds}"
//
//        Feature.executeUpdate("delete from feature_grails_user where feature_owners_id in (:featureIds)",[featureIds,featureIds])


        organism.save(flush: true )
//        Feature.executeUpdate("delete from feature where exists (select 'x' from feature f join feature_locations on f.idfeature_locations fl join fl.sequence s join s.organism o where o = :organism)",[organism:organism])
//            featureService.deleteFeature(it)
//            ++count
//        }
        return count
    }
}
