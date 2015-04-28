package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.history.FeatureOperation
import grails.util.Environment
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class FeatureEventService {

    def permissionService
    def featureService

    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation,Feature feature){
        if (Environment.current == Environment.TEST) {
            return addNewFeatureEvent(featureOperation,feature,null)
        }
         addNewFeatureEvent(featureOperation,feature,permissionService.currentUser)
    }

    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation,Feature feature,User user){

        JSONObject jsonObject = featureService.convertFeatureToJSON(feature)

        FeatureEvent featureEvent = FeatureEvent.findByUniqueNameAndId(feature.uniqueName,feature.id)

        if(!featureEvent){
            JSONArray newFeatureArray = new JSONArray()
            newFeatureArray.add(jsonObject)
            featureEvent = new FeatureEvent(
                    editor: user
                    ,uniqueName: feature.uniqueName
                    ,operation: featureOperation.name()
                    ,current: true
                    ,newFeaturesJsonArray: newFeatureArray.toString()
                    ,oldFeaturesJsonArray: new JSONArray().toString()
                    ,dateCreated: new Date()
                    ,lastUpdated: new Date()
            ).save()
        }
        else{
            JSONArray newJSONArray = (JSONArray) JSON.parse(featureEvent.newFeaturesJsonArray)
            newJSONArray.put(jsonObject)
            featureEvent.newFeaturesJsonArray = newJSONArray.toString()
            featureEvent.save()
        }

        return featureEvent

    }

    def deleteHistory(String uniqueName) {
        FeatureEvent.deleteAll(FeatureEvent.findAllByUniqueName(uniqueName))
    }
}
