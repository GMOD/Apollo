package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.history.FeatureOperation
import grails.util.Environment
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class FeatureEventService {

    def permissionService
    def featureService

    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation, Feature feature, User user) {
        if (Environment.current == Environment.TEST) {
            return addNewFeatureEventWithUser(featureOperation, feature, null)
        }
        addNewFeatureEventWithUser(featureOperation, feature, user)
    }


    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation, String uniqueName, JSONObject jsonObject, User user) {
        if (Environment.current == Environment.TEST) {
            return addNewFeatureEventWithUser(featureOperation, uniqueName, jsonObject, (User) null)
        }
        addNewFeatureEventWithUser(featureOperation, uniqueName, jsonObject, user)
    }

    FeatureEvent addNewFeatureEventWithUser(FeatureOperation featureOperation, String uniqueName, JSONObject jsonObject, User user) {

        FeatureEvent featureEvent
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(jsonObject)
        featureEvent = new FeatureEvent(
                editor: user
                , uniqueName: uniqueName
                , operation: featureOperation.name()
                , current: true
                , newFeaturesJsonArray: newFeatureArray.toString()
                , oldFeaturesJsonArray: new JSONArray().toString()
                , dateCreated: new Date()
                , lastUpdated: new Date()
        ).save()

        return featureEvent

    }

    FeatureEvent addNewFeatureEventWithUser(FeatureOperation featureOperation, Feature feature, User user) {
        return addNewFeatureEventWithUser(featureOperation, feature.uniqueName, featureService.convertFeatureToJSON(feature), user)
    }

//    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation, String uniqueName, JSONObject oldJsonObject,JSONObject newJsonObject) {
//        return addNewFeatureEventWithUser(featureOperation,uniqueName,oldJsonObject,newJsonObject,User user)
//    }

    def addNewFeatureEvent(FeatureOperation featureOperation, String uniqueName, JSONObject oldJsonObject, JSONObject newJsonObject, User user) {
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(newJsonObject)
        JSONArray oldFeatureArray = new JSONArray()
        oldFeatureArray.add(oldJsonObject)
        FeatureEvent featureEvent = new FeatureEvent(
                editor: user
                , uniqueName: uniqueName
                , operation: featureOperation.name()
                , current: true
                , newFeaturesJsonArray: newFeatureArray.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: new Date()
                , lastUpdated: new Date()
        ).save()

        return featureEvent
    }

    def deleteHistory(String uniqueName) {
        FeatureEvent.deleteAll(FeatureEvent.findAllByUniqueName(uniqueName))
    }

    /**
     * Count of 0 is the most recent
     * @param uniqueName
     * @param count
     * @return
     */
    FeatureEvent getPastEvent(String uniqueName, int count) {
        FeatureEvent.executeUpdate("update FeatureEvent  fe set fe.current = false where fe.uniqueName = :uniqueName",[uniqueName: uniqueName])
        FeatureEvent featureEvent = FeatureEvent.findByUniqueName(uniqueName,[sort:"dateCreated",order:"desc",max:1,offset:count])
        featureEvent.current = true
        featureEvent.save(flush: true)
        return featureEvent
    }

    int historySize(String uniqueName) {
        FeatureEvent.countByUniqueName(uniqueName)
    }

    /**
     * Count of 0 is the very FIRST one
     * @param uniqueName
     * @param count
     * @return
     */
    FeatureEvent getFutureEvent(String uniqueName, int count) {
        FeatureEvent.executeUpdate("update FeatureEvent  fe set fe.current = false where fe.uniqueName = :uniqueName",[uniqueName: uniqueName])
        FeatureEvent featureEvent = FeatureEvent.findByUniqueName(uniqueName,[sort:"dateCreated",order:"asc",max:1,offset:count])
        featureEvent.current = true
        featureEvent.save(flush: true)
        return featureEvent
    }

    def undo(JSONObject inputObject, int count, boolean confirm) {
        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        // TODO: I think that this gives up if you are already at the most recent transaction
//        if (historyStore.getCurrentIndexForFeature(uniqueName) + (count - 1) >= historyStore.getHistorySizeForFeature(uniqueName) - 1) {
//            continue;
//        }

        FeatureEvent featureEvent = getPastEvent(uniqueName, count)
        // set current to one past then
        getPastEvent(uniqueName, count+1)

        switch(featureEvent.operation){
            case FeatureOperation.ADD_FEATURE:
                break;
            default:
                println "unadled operation "
                break ;
        }

    }

    def redo(JSONObject inputObject, int count, boolean confirm) {
        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        FeatureEvent featureEvent = getFutureEvent(uniqueName, count)
    }
}
