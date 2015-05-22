package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
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

        FeatureEvent.executeUpdate("update FeatureEvent  fe set fe.current = :current where fe.uniqueName = :uniqueName", [current: false, uniqueName: uniqueName]);
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(jsonObject)
        FeatureEvent featureEvent = new FeatureEvent(
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

    def addNewFeatureEvent(FeatureOperation featureOperation, String uniqueName, JSONObject inputCommand, JSONObject oldJsonObject, JSONObject newJsonObject, User user) {
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(newJsonObject)
        JSONArray oldFeatureArray = new JSONArray()
        oldFeatureArray.add(oldJsonObject)

        int updated = FeatureEvent.executeUpdate("update FeatureEvent  fe set fe.current = false where fe.uniqueName = :uniqueName", [uniqueName: uniqueName])
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

    /**
     * Count of 0 is the most recent
     * @param uniqueName
     * @param count
     * @return
     */
    FeatureEvent setTransactionForFeature(String uniqueName, int count) {
        log.info "setting previous transactino for feature ${uniqueName} -> ${count}"
        log.info "unique values: ${FeatureEvent.countByUniqueName(uniqueName)} -> ${count}"
        int updated = FeatureEvent.executeUpdate("update FeatureEvent  fe set fe.current = false where fe.uniqueName = :uniqueName", [uniqueName: uniqueName])
        log.debug "updated is ${updated}"
        FeatureEvent featureEvent = FeatureEvent.findByUniqueName(uniqueName, [sort: "dateCreated", order: "asc", max: 1, offset: count])
        log.debug "featureEvent found ${featureEvent}"
        log.debug "featureEvent ${featureEvent.operation} -> ${featureEvent.dateCreated}"
        featureEvent.current = true
        featureEvent.save(flush: true)
        return featureEvent
    }

    def setHistoryState(JSONObject inputObject, int count, boolean confirm) {


        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        log.debug "undo count ${count}"
        if (count < 0) {
            log.warn("Can not undo any further")
            return
        }

        int total = FeatureEvent.countByUniqueName(uniqueName)
        if (count >= total) {
            log.warn("Can not redo any further")
            return
        }


        Sequence sequence = Feature.findByUniqueName(uniqueName).featureLocation.sequence

        JSONObject deleteCommandObject = new JSONObject()
        JSONArray featuresArray = new JSONArray()
        JSONObject featureToDelete = new JSONObject()
        featureToDelete.put(FeatureStringEnum.UNIQUENAME.value, uniqueName)
        featuresArray.add(featureToDelete)
        deleteCommandObject.put(FeatureStringEnum.FEATURES.value, featuresArray)
        deleteCommandObject = permissionService.copyUserName(inputObject, deleteCommandObject)
        deleteCommandObject.put(FeatureStringEnum.SUPPRESS_EVENTS.value, true)

        log.debug "feature event values: ${FeatureEvent.countByUniqueNameAndCurrent(uniqueName, true)} -> ${count}"
        log.debug " final delete JSON ${deleteCommandObject as JSON}"
        requestHandlingService.deleteFeature(deleteCommandObject)
        log.debug "deletion sucess . .  "
        log.debug "2 feature event values: ${FeatureEvent.countByUniqueNameAndCurrent(uniqueName, true)} -> ${count}"

        FeatureEvent featureEvent = setTransactionForFeature(uniqueName, count)
        log.debug "final feature event: ${featureEvent} ->${featureEvent.operation}"
        log.debug "current feature events for unique name ${FeatureEvent.countByUniqueNameAndCurrent(uniqueName, true)}"


        JSONArray jsonArray = (JSONArray) JSON.parse(featureEvent.newFeaturesJsonArray)
        log.debug "array to add size: ${jsonArray.size()} "
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonFeature = jsonArray.getJSONObject(i)

            JSONObject addCommandObject = new JSONObject()
            JSONArray featuresToAddArray = new JSONArray()
            featuresToAddArray.add(jsonFeature)
            addCommandObject.put(FeatureStringEnum.FEATURES.value, featuresToAddArray)

            // we have to explicitly set the track (if we have features ... which we should)
            if(!addCommandObject.containsKey(FeatureStringEnum.TRACK.value) && featuresToAddArray.size()>0  ){
                addCommandObject.put(FeatureStringEnum.TRACK.value, featuresToAddArray.getJSONObject(i).getString(FeatureStringEnum.SEQUENCE.value))
            }

            addCommandObject = permissionService.copyUserName(inputObject, addCommandObject)

            addCommandObject.put(FeatureStringEnum.SUPPRESS_HISTORY.value, true)
            addCommandObject.put(FeatureStringEnum.SUPPRESS_EVENTS.value, true)

            JSONObject returnObject
            log.info "addCommandObject = ${addCommandObject as JSON}"
            if (featureService.isJsonTranscript(jsonFeature)) {
                returnObject = requestHandlingService.addTranscript(addCommandObject)
            } else {
                returnObject = requestHandlingService.addFeature(addCommandObject)
            }

            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: returnObject
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )

            requestHandlingService.fireAnnotationEvent(annotationEvent)
        }


    }

    /**
     * Count back from most recent
     * @param inputObject
     * @param count
     * @param confirm
     * @return
     */
    def redo(JSONObject inputObject, int countForward, boolean confirm) {
        log.info "redoing ${countForward}"
        if (countForward == 0) {
            log.warn "Redo to the same state"
            return
        }
        // count = current - countBackwards
        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        int currentIndex = getCurrentFeatureEventIndex(uniqueName)
        int count = currentIndex + countForward
        log.info "current Index ${currentIndex}"
        log.info "${count} = ${currentIndex}-${countForward}"

        setHistoryState(inputObject, count, confirm)
    }

    int getCurrentFeatureEventIndex(String uniqueName) {
        List<FeatureEvent> currentFeatureEventList = FeatureEvent.findAllByUniqueNameAndCurrent(uniqueName, true, [sort: "dateCreated", order: "asc"])
        if (currentFeatureEventList.size() != 1) {
            throw new AnnotationException("Feature event list is the wrong size ${currentFeatureEventList?.size()}")
        }
        FeatureEvent currentFeatureEvent = currentFeatureEventList.iterator().next()
        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueName(uniqueName, [sort: "dateCreated", order: "asc"])

        int index = 0
        for (FeatureEvent featureEvent in featureEventList) {
            if (featureEvent.id == currentFeatureEvent.id) {
                return index
            }
            ++index
        }

        return -1
    }

    def undo(JSONObject inputObject, int countBackwards, boolean confirm) {
        log.info "undoing ${countBackwards}"
        if (countBackwards == 0) {
            log.warn "Undo to the same state"
            return
        }

        // count = current - countBackwards
        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        int currentIndex = getCurrentFeatureEventIndex(uniqueName)
        int count = currentIndex - countBackwards
        log.info "${count} = ${currentIndex}-${countBackwards}"
        setHistoryState(inputObject, count, confirm)
    }

    int historySize(String uniqueName) {
        FeatureEvent.countByUniqueName(uniqueName)
    }

    FeatureEvent getCurrentFeatureEvent(String uniqueName) {
        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueNameAndCurrent(uniqueName, true, [sort: "dateCreated", order: "asc"])
        if (featureEventList.size() != 1) {
            throw new AnnotationException("Feature event list is the wrong size ${featureEventList?.size()}")
        }
        return featureEventList.get(0)
    }


    List<FeatureEvent> getRecentFeatureEvents(String uniqueName, int count) {
        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueName(uniqueName, [sort: "dateCreated", order: "asc", max: count])
        return featureEventList
    }

    private Boolean compareLocationObjects(JSONObject locationA, JSONObject locationB) {
        if (locationA.getInt(FeatureStringEnum.FMIN.value) != locationB.getInt(FeatureStringEnum.FMIN.value)) return false
        if (locationA.getInt(FeatureStringEnum.FMAX.value) != locationB.getInt(FeatureStringEnum.FMAX.value)) return false
        if (locationA.getInt(FeatureStringEnum.STRAND.value) != locationB.getInt(FeatureStringEnum.STRAND.value)) return false
        return true
    }

}
