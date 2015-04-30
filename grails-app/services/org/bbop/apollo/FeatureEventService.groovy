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
    def requestHandlingService

    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation, Feature feature, JSONObject inputCommand, User user) {
        if (Environment.current == Environment.TEST) {
            return addNewFeatureEventWithUser(featureOperation, feature, inputCommand, null)
        }
        addNewFeatureEventWithUser(featureOperation, feature, inputCommand, user)
    }


    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation, String uniqueName, JSONObject inputCommand, JSONObject jsonObject, User user) {
        if (Environment.current == Environment.TEST) {
            return addNewFeatureEventWithUser(featureOperation, uniqueName, inputCommand, jsonObject, (User) null)
        }
        addNewFeatureEventWithUser(featureOperation, uniqueName, inputCommand, jsonObject, user)
    }

    FeatureEvent addNewFeatureEventWithUser(FeatureOperation featureOperation, String uniqueName, JSONObject commandObject, JSONObject jsonObject, User user) {

        FeatureEvent featureEvent
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(jsonObject)
        featureEvent = new FeatureEvent(
                editor: user
                , uniqueName: uniqueName
                , operation: featureOperation.name()
                , current: true
                , originalJsonCommand: commandObject.toString()
                , newFeaturesJsonArray: newFeatureArray.toString()
                , oldFeaturesJsonArray: new JSONArray().toString()
                , dateCreated: new Date()
                , lastUpdated: new Date()
        ).save()

        return featureEvent

    }

    FeatureEvent addNewFeatureEventWithUser(FeatureOperation featureOperation, Feature feature, JSONObject inputCommand, User user) {
        return addNewFeatureEventWithUser(featureOperation, feature.uniqueName, inputCommand, featureService.convertFeatureToJSON(feature), user)
    }

//    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation, String uniqueName, JSONObject oldJsonObject,JSONObject newJsonObject) {
//        return addNewFeatureEventWithUser(featureOperation,uniqueName,oldJsonObject,newJsonObject,User user)
//    }

    def addNewFeatureEvent(FeatureOperation featureOperation, String uniqueName, JSONObject inputCommand, JSONObject oldJsonObject, JSONObject newJsonObject, User user) {
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(newJsonObject)
        JSONArray oldFeatureArray = new JSONArray()
        oldFeatureArray.add(oldJsonObject)
        FeatureEvent featureEvent = new FeatureEvent(
                editor: user
                , uniqueName: uniqueName
                , operation: featureOperation.name()
                , current: true
                , originalJsonCommand: inputCommand.toString()
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

    int historySize(String uniqueName) {
        FeatureEvent.countByUniqueName(uniqueName)
    }

    /**
     * Count of 0 is the most recent
     * @param uniqueName
     * @param count
     * @return
     */
    FeatureEvent setPreviousTransactionForFeature(String uniqueName, int count) {
        FeatureEvent.executeUpdate("update FeatureEvent  fe set fe.current = false where fe.uniqueName = :uniqueName", [uniqueName: uniqueName])
        FeatureEvent featureEvent = FeatureEvent.findByUniqueName(uniqueName, [sort: "dateCreated", order: "desc", max: 1, offset: count])
        featureEvent.current = true
        featureEvent.save(flush: true)
        return featureEvent
    }

    /**
     * Count of 0 is the very FIRST one
     * @param uniqueName
     * @param count
     * @return
     */
    FeatureEvent getNextTransactionsForFeature(String uniqueName, int count) {
        FeatureEvent.executeUpdate("update FeatureEvent  fe set fe.current = false where fe.uniqueName = :uniqueName", [uniqueName: uniqueName])
        FeatureEvent featureEvent = FeatureEvent.findByUniqueName(uniqueName, [sort: "dateCreated", order: "asc", max: 1, offset: count])
        featureEvent.current = true
        featureEvent.save(flush: true)
        return featureEvent
    }

    def undo(JSONObject inputObject, int count, boolean confirm) {
        println "undo count ${count}"
        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        // TODO: I think that this gives up if you are already at the most recent transaction
//        if (historyStore.getCurrentIndexForFeature(uniqueName) + (count - 1) >= historyStore.getHistorySizeForFeature(uniqueName) - 1) {
//            continue;
//        }

        FeatureEvent featureEvent = setPreviousTransactionForFeature(uniqueName, count - 1)

        println "feature event gotten ${featureEvent.operation}"
        // set current to one past then
//        setPreviousTransactionForFeature(uniqueName, count+1)

        switch (featureEvent.operation) {
            case FeatureOperation.ADD_FEATURE:
                requestHandlingService.deleteFeature((JSONObject) JSON.parse(featureEvent.originalJsonCommand))
                break;
            case FeatureOperation.DELETE_FEATURE:
                requestHandlingService.addFeature((JSONObject) JSON.parse(featureEvent.originalJsonCommand))
                break;
            case FeatureOperation.ADD_EXON:
                // add exon expects:
                // feature 0 = transcript
                // feature 1 = exon 1 to delete
                // feature 2 = exon 2 to delete
                // etc. etc.
                // correlate commands:  features[ uniquename:"AAA",uniquename:"BBB"] // etc.
                // to what is in children: of the transcript in the transcript features . . .
//                JSONObject command = (JSONObject) JSON.parse(featureEvent.originalJsonCommand)
                JSONObject transcriptObject = ((JSONArray) JSON.parse(featureEvent.newFeaturesJsonArray)).getJSONObject(0)
                println "oroginal object : ${transcriptObject as JSON}"
                JSONObject command = JSON.parse(featureEvent.originalJsonCommand)
                // first one is transcript and then the exons that were added, which we need to grab the locations of
                JSONArray exonArray = command.getJSONArray(FeatureStringEnum.FEATURES.value)
                // we have to find the exon to delete based on start and stop location
                JSONArray featuresArray = new JSONArray()
//                JSONObject transcriptObject = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0)
                featuresArray.add(transcriptObject)
                JSONArray childrenArray = transcriptObject.getJSONArray(FeatureStringEnum.CHILDREN.value)
                for (int i = 0; i < childrenArray.size(); i++) {
                    JSONObject childObject = childrenArray.getJSONObject(i)
                    //
                    if (childObject.getJSONObject(FeatureStringEnum.TYPE.value).getString("name") == "exon") {
                        JSONObject locationObject = childObject.getJSONObject(FeatureStringEnum.LOCATION.value)
                        for (int j = 1; j < exonArray.size(); j++) {
                            JSONObject exonObject = exonArray.getJSONObject(j)
                            Boolean objectsAreSameLocation = compareLocationObjects(locationObject, exonObject.getJSONObject(FeatureStringEnum.LOCATION.value))
                            if (objectsAreSameLocation) {
                                featuresArray.add(childObject)
                            }
                        }
                    }
                }

                command.put(FeatureStringEnum.FEATURES.value, featuresArray)
                command.put(AnnotationEditorController.REST_OPERATION, FeatureOperation.DELETE_EXON.toLower())
                Feature.withNewTransaction {
                    return requestHandlingService.deleteExon(command)
                }
                break;
            case FeatureOperation.DELETE_EXON:
                println "olf features json array = ${featureEvent.oldFeaturesJsonArray}"
                JSONObject jsonObject = ((JSONArray) JSON.parse(featureEvent.oldFeaturesJsonArray)).getJSONObject(0)
                println "oroginal object : ${jsonObject as JSON}"
                JSONObject command = (JSONObject) JSON.parse(featureEvent.originalJsonCommand)
                Set<String> exonsToAdd = new HashSet<>()
                JSONArray exonArray = command.getJSONArray(FeatureStringEnum.FEATURES.value)
                for (int i = 0; i < exonArray.size(); i++) {
                    exonsToAdd.add(exonArray.getJSONObject(i).getString(FeatureStringEnum.UNIQUENAME.value))
                }
                // add exon expects:
                // feature 0 = transcript
                // feature 1 = exon 1 to add
                // feature 2 = exon 2 to add
                // etc. etc.
                // correlate commands:  features[ uniquename:"AAA",uniquename:"BBB"] // etc.
                // to what is in children: of the transcript in the transcript features . . .
                JSONArray featuresArray = new JSONArray()
                JSONObject transcriptObject = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0)
                featuresArray.add(transcriptObject)
                JSONArray childrenArray = transcriptObject.getJSONArray(FeatureStringEnum.CHILDREN.value)
                for (int i = 0; i < childrenArray.size(); i++) {
                    JSONObject childObject = childrenArray.getJSONObject(i)
                    if (exonsToAdd.contains(childObject.getString(FeatureStringEnum.UNIQUENAME.value))) {
                        featuresArray.add(childObject)
                    }
                }

                command.put(FeatureStringEnum.FEATURES.value, featuresArray)
                command.put(AnnotationEditorController.REST_OPERATION, FeatureOperation.ADD_EXON.toLower())
//                        {"operation":"delete_feature","username":"ndunn@me.com","track":"Annotations-Group1.1","features":[{"date_creation":1430346621566,"location":{"fmin":992748,"strand":1,"fmax":993041},"sequence":"Group1.1","name":"6fa9b21c-39fe-4f20-8d21-476c8de2f63d-exon","parent_type":{"name":"mRNA","cv":{"name":"sequence"}},"owner":"None","properties":[{"value":"None","type":{"name":"owner","cv":{"name":"feature_property"}}}],"type":{"name":"exon","cv":{"name":"sequence"}},"uniquename":"6fa9b21c-39fe-4f20-8d21-476c8de2f63d","notes":[],"date_last_modified":1430346621663,"parent_id":"93f6958e-b3c6-4761-8f4b-c6e570194b34"}]}
//                        { \"track\": \"Annotations-Group1.1\", \"features\": [ {\"uniquename\": \"93f6958e-b3c6-4761-8f4b-c6e570194b34\"}, {\"location\":{\"fmin\":992748,\"fmax\":993041,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}], \"operation\": \"add_exon\" }


                return requestHandlingService.addExon(command)
                break;
            case FeatureOperation.SET_EXON_BOUNDARIES:
                JSONObject originalCommand = (JSONObject) JSON.parse(featureEvent.originalJsonCommand)
                println "original command : ${originalCommand as JSON}"
                JSONArray features = originalCommand.getJSONArray(FeatureStringEnum.FEATURES.value)

                println "old features now: ${JSON.parse(featureEvent.oldFeaturesJsonArray) as JSON}"

                JSONObject originalExon = ((JSONArray) JSON.parse(featureEvent.oldFeaturesJsonArray)).getJSONObject(0)
                println "original exon ${originalExon as JSON}"

                for (int i = 0; i < features.size(); i++) {
                    JSONObject featureObject = features.getJSONObject(i)
                    featureObject.put(FeatureStringEnum.LOCATION.value, originalExon.getJSONObject(FeatureStringEnum.LOCATION.value))
                }

                println "final command : ${originalCommand as JSON}"
                return requestHandlingService.setExonBoundaries(originalCommand)

                break;
            default:
                println "unadled operation "
                break;
        }

    }

    private Boolean compareLocationObjects(JSONObject locationA, JSONObject locationB) {
        if (locationA.getInt(FeatureStringEnum.FMIN.value) != locationB.getInt(FeatureStringEnum.FMIN.value)) return false
        if (locationA.getInt(FeatureStringEnum.FMAX.value) != locationB.getInt(FeatureStringEnum.FMAX.value)) return false
        if (locationA.getInt(FeatureStringEnum.STRAND.value) != locationB.getInt(FeatureStringEnum.STRAND.value)) return false
        return true
    }

    def redo(JSONObject inputObject, int count, boolean confirm) {
        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        FeatureEvent featureEvent = getNextTransactionsForFeature(uniqueName, count)
    }
}
