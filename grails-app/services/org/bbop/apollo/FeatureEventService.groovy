package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.history.FeatureOperation
import grails.util.Environment

@Transactional
class FeatureEventService {

    def permissionService

    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation,Feature feature){
        if (Environment.current == Environment.TEST) {
            return addNewFeatureEvent(featureOperation,feature,null)
        }
         addNewFeatureEvent(featureOperation,feature,permissionService.currentUser)
    }

    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation,Feature feature,User user){

        FeatureEvent featureEvent = new FeatureEvent(
                editor: user
                ,featureId: feature.id
                ,operation: featureOperation.name()
                ,current: true
                ,dateCreated: new Date()
                ,lastUpdated: new Date()
        ).save()

        return featureEvent

    }

    def deleteHistory(Long featureId) {
        FeatureEvent.deleteAll(FeatureEvent.findAllByFeatureId(featureId))
    }
}
