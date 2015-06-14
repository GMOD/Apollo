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


    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation, String name, String uniqueName, JSONObject inputCommand, JSONObject jsonObject, User user) {
        if (Environment.current == Environment.TEST) {
            return addNewFeatureEventWithUser(featureOperation, name, uniqueName, inputCommand, jsonObject, (User) null)
        }
        addNewFeatureEventWithUser(featureOperation, name, uniqueName, inputCommand, jsonObject, user)
    }

    FeatureEvent addNewFeatureEventWithUser(FeatureOperation featureOperation, String name, String uniqueName, JSONObject commandObject, JSONObject jsonObject, User user) {
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(jsonObject)
        return addNewFeatureEvent(featureOperation, name, uniqueName, commandObject, new JSONArray(), newFeatureArray, user)

    }

/**
 * Convention is that 1 is the parent and is returned first in the array.
 * @param name1
 * @param uniqueName1
 * @param name2
 * @param uniqueName2
 * @param commandObject
 * @param oldFeatureObject
 * @param newFeatureArray
 * @param user
 * @return
 */
    List<FeatureEvent> addSplitFeatureEvent(String name1, String uniqueName1, String name2, String uniqueName2, JSONObject commandObject, JSONObject oldFeatureObject, JSONArray newFeatureArray,
                                            User user) {
        List<FeatureEvent> featureEventList = new ArrayList<>()
        JSONArray oldFeatureArray = new JSONArray()
        oldFeatureArray.add(oldFeatureObject)

        FeatureEvent lastFeatureEvent = findCurrentFeatureEvent(uniqueName1)
        if (lastFeatureEvent) {
            lastFeatureEvent.childUniqueName = uniqueName1
            lastFeatureEvent.childSplitUniqueName = uniqueName2
            lastFeatureEvent.current = false;
            lastFeatureEvent.save()

            deleteFutureHistoryEvents(lastFeatureEvent)
        }

        Date addDate = new Date()

        FeatureEvent featureEvent1 = new FeatureEvent(
                editor: user
                , name: name1
                , uniqueName: uniqueName1
                , operation: FeatureOperation.SPLIT_TRANSCRIPT
                , current: true
                , parentUniqueName: uniqueName1
                , originalJsonCommand: commandObject.toString()
                , newFeaturesJsonArray: newFeatureArray.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: addDate
                , lastUpdated: addDate
        ).save()

        FeatureEvent featureEvent2 = new FeatureEvent(
                editor: user
                , name: name2
                , uniqueName: uniqueName2
                , operation: FeatureOperation.SPLIT_TRANSCRIPT
                , current: true
                , parentUniqueName: uniqueName1
                , originalJsonCommand: commandObject.toString()
                , newFeaturesJsonArray: newFeatureArray.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: addDate
                , lastUpdated: addDate
        ).save()

        featureEvent1.dateCreated = featureEvent2.dateCreated
        featureEvent1.save()

        featureEventList.add(featureEvent1)
        featureEventList.add(featureEvent2)

        return featureEventList
    }

    /**
     * For non-split , non-merge operations
     */
    def addNewFeatureEvent(FeatureOperation featureOperation, String name, String uniqueName, JSONObject inputCommand, JSONArray oldFeatureArray, JSONArray newFeatureArray, User user) {
//        int updated = FeatureEvent.executeUpdate("update FeatureEvent  fe set fe.current = false where fe.uniqueName = :uniqueName", [uniqueName: uniqueName])
        FeatureEvent lastFeatureEvent = findCurrentFeatureEvent(uniqueName)
        if (lastFeatureEvent) {
            lastFeatureEvent.childUniqueName = uniqueName
            lastFeatureEvent.current = false;
            lastFeatureEvent.save()

            deleteFutureHistoryEvents(lastFeatureEvent)
        }

        FeatureEvent featureEvent = new FeatureEvent(
                editor: user
                , name: name
                , uniqueName: uniqueName
                , operation: featureOperation.name()
                , current: true
                , parentUniqueName: uniqueName
                , originalJsonCommand: inputCommand.toString()
                , newFeaturesJsonArray: newFeatureArray.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: new Date()
                , lastUpdated: new Date()
        ).save()

        return featureEvent
    }

    def setNotCurrentFutureHistoryEvents(FeatureEvent featureEvent) {
        Set<FeatureEvent> featureEventList = findAllFutureFeatureEvents(featureEvent)
        featureEventList.each {
            if(it.current){
                it.current=false
                it.save()
            }
        }
    }

    def deleteFutureHistoryEvents(FeatureEvent featureEvent) {
        Set<FeatureEvent> featureEventList = findAllFutureFeatureEvents(featureEvent)
        return FeatureEvent.deleteAll(featureEventList)
    }

    Set<FeatureEvent> findAllPreviousFeatureEvents(FeatureEvent featureEvent) {
        Set<FeatureEvent> featureEventList = new HashSet<>()
        if (featureEvent.parentUniqueName) {
//            for(FeatureEvent parentFeatureEvent in FeatureEvent.findAllByUniqueNameAndDateCreatedLessThan(featureEvent.parentUniqueName, featureEvent.dateCreated)){
            for (FeatureEvent parentFeatureEvent in FeatureEvent.findAllByUniqueName(featureEvent.parentUniqueName)) {
                // this is likely a rounding error
                // this is sometimes off by 3-5 ns . . . annoying
                if(parentFeatureEvent.dateCreated.time < featureEvent.dateCreated.time-10){
                    featureEventList.add(parentFeatureEvent)
                    featureEventList.addAll(findAllPreviousFeatureEvents(parentFeatureEvent))
                }
            }
        }

        if (featureEvent.parentMergeUniqueName) {
            for (FeatureEvent parentFeatureEvent in FeatureEvent.findAllByUniqueName(featureEvent.parentMergeUniqueName)) {
                if(parentFeatureEvent.dateCreated.time < featureEvent.dateCreated.time-10){
                    featureEventList.add(parentFeatureEvent)
                    featureEventList.addAll(findAllPreviousFeatureEvents(parentFeatureEvent))
                }
            }
        }

        return featureEventList
    }

    Set<FeatureEvent> findAllFutureFeatureEvents(FeatureEvent featureEvent) {
        Set<FeatureEvent> featureEventList = new HashSet<>()
        if (featureEvent.childUniqueName) {
            for(FeatureEvent childFeatureEvent in FeatureEvent.findAllByUniqueNameAndDateCreatedGreaterThan(featureEvent.childUniqueName, featureEvent.dateCreated)){
                if(childFeatureEvent.dateCreated.time-10 > featureEvent.dateCreated.time){
                    featureEventList.add(childFeatureEvent)
                    featureEventList.addAll(findAllFutureFeatureEvents(childFeatureEvent))
                }
            }
        }

        if (featureEvent.childSplitUniqueName) {
//            featureEventList.addAll(FeatureEvent.findAllByUniqueNameAndDateCreatedGreaterThan(featureEvent.childSplitUniqueName, featureEvent.dateCreated))
            for(FeatureEvent childFeatureEvent in FeatureEvent.findAllByUniqueNameAndDateCreatedGreaterThan(featureEvent.childSplitUniqueName, featureEvent.dateCreated)){
                if(childFeatureEvent.dateCreated.time-10 > featureEvent.dateCreated.time){
                    featureEventList.add(childFeatureEvent)
                    featureEventList.addAll(findAllFutureFeatureEvents(childFeatureEvent))
                }
            }
        }

        // for each split in the feature events, we also need to process??

        return featureEventList
    }


    def addNewFeatureEvent(FeatureOperation featureOperation, String name, String uniqueName, JSONObject inputCommand, JSONObject oldJsonObject, JSONObject newJsonObject, User user) {
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(newJsonObject)
        JSONArray oldFeatureArray = new JSONArray()
        oldFeatureArray.add(oldJsonObject)

        return addNewFeatureEvent(featureOperation, name, uniqueName, inputCommand, oldFeatureArray, newFeatureArray, user)
    }

    FeatureEvent addNewFeatureEventWithUser(FeatureOperation featureOperation, Feature feature, JSONObject inputCommand, User user) {
        return addNewFeatureEventWithUser(featureOperation, feature.name, feature.uniqueName, inputCommand, featureService.convertFeatureToJSON(feature), user)
    }


    def deleteHistory(String uniqueName) {
        FeatureEvent.deleteAll(FeatureEvent.findAllByUniqueName(uniqueName))
    }

    /**
     * Count of 0 is the most recent
     * @param uniqueName
     * @param count backwards
     * @return
     */
    FeatureEvent setTransactionForFeature(String uniqueName, int count) {
        log.info "setting previous transactino for feature ${uniqueName} -> ${count}"
        log.info "unique values: ${FeatureEvent.countByUniqueName(uniqueName)} -> ${count}"
//        int updated = FeatureEvent.executeUpdate("update FeatureEvent  fe set fe.current = false where fe.uniqueName = :uniqueName", [uniqueName: uniqueName])
        List<FeatureEvent> featureEventList = getHistory(uniqueName)
        FeatureEvent currentFeatureEvent = null
        for (int i = 0; i < featureEventList.size(); i++) {
            FeatureEvent featureEvent = featureEventList.get(i)
            if (i == count && !featureEvent.current) {
                featureEvent.current = true
                featureEvent.save()
                currentFeatureEvent = featureEvent
            } else if (i != count && featureEvent.current) {
                featureEvent.current = false
                featureEvent.save()
            }
        }

        if(!currentFeatureEvent){
            log.warn "Did we forget to change the feature event?"
            return findCurrentFeatureEvent(uniqueName)
        }

        setNotCurrentFutureHistoryEvents(currentFeatureEvent)


//        log.debug "updated is ${updated}"
        return findCurrentFeatureEvent(currentFeatureEvent.uniqueName)
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
        JSONObject originalCommandObject = (JSONObject) JSON.parse(featureEvent.originalJsonCommand)
        log.debug "array to add size: ${jsonArray.size()} "
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonFeature = jsonArray.getJSONObject(i)

            JSONObject addCommandObject = new JSONObject()
            JSONArray featuresToAddArray = new JSONArray()
            featuresToAddArray.add(jsonFeature)
            addCommandObject.put(FeatureStringEnum.FEATURES.value, featuresToAddArray)

            // we have to explicitly set the track (if we have features ... which we should)
            if (!addCommandObject.containsKey(FeatureStringEnum.TRACK.value) && featuresToAddArray.size() > 0) {
                addCommandObject.put(FeatureStringEnum.TRACK.value, featuresToAddArray.getJSONObject(i).getString(FeatureStringEnum.SEQUENCE.value))
            }

            addCommandObject = permissionService.copyUserName(inputObject, addCommandObject)

            addCommandObject.put(FeatureStringEnum.SUPPRESS_HISTORY.value, true)
            addCommandObject.put(FeatureStringEnum.SUPPRESS_EVENTS.value, true)

            JSONObject returnObject
            if (featureService.isJsonTranscript(jsonFeature)) {
                // set the original gene name
//                if (originalCommandObject.containsKey(FeatureStringEnum.NAME.value)) {
////                    addCommandObject.put(FeatureStringEnum.GENE_NAME.value, originalCommandObject.getString(FeatureStringEnum.NAME.value))
                for (int k = 0; k < featuresToAddArray.size(); k++) {
                    JSONObject featureObject = featuresToAddArray.getJSONObject(k)
//                        featureObject.put(FeatureStringEnum.GENE_NAME.value, originalCommandObject.getString(FeatureStringEnum.NAME.value))
                    featureObject.put(FeatureStringEnum.GENE_NAME.value, featureEvent.name)
                }
//                }
                println "original command object = ${originalCommandObject as JSON}"
                println "final command object = ${addCommandObject as JSON}"

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

        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        int currentIndex = getCurrentFeatureEventIndex(uniqueName)
        int count = currentIndex - countBackwards
        log.info "${count} = ${currentIndex}-${countBackwards}"
        setHistoryState(inputObject, count, confirm)
    }

//    int historySize(String uniqueName) {
//        FeatureEvent.countByUniqueName(uniqueName)
//    }
//
//    FeatureEvent getCurrentFeatureEvent(String uniqueName) {
//        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueNameAndCurrent(uniqueName, true, [sort: "dateCreated", order: "asc"])
//        if (featureEventList.size() != 1) {
//            throw new AnnotationException("Feature event list is the wrong size ${featureEventList?.size()}")
//        }
//        return featureEventList.get(0)
//    }
//
//
//    List<FeatureEvent> getRecentFeatureEvents(String uniqueName, int count) {
//        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueName(uniqueName, [sort: "dateCreated", order: "asc", max: count])
//        return featureEventList
//    }
//
//    private Boolean compareLocationObjects(JSONObject locationA, JSONObject locationB) {
//        if (locationA.getInt(FeatureStringEnum.FMIN.value) != locationB.getInt(FeatureStringEnum.FMIN.value)) return false
//        if (locationA.getInt(FeatureStringEnum.FMAX.value) != locationB.getInt(FeatureStringEnum.FMAX.value)) return false
//        if (locationA.getInt(FeatureStringEnum.STRAND.value) != locationB.getInt(FeatureStringEnum.STRAND.value)) return false
//        return true
//    }
    FeatureEvent findCurrentFeatureEvent(String uniqueName) {
        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueNameAndCurrent(uniqueName, true)
        if (featureEventList.size() != 1) {
            log.warn("No current feature events for ${uniqueName}: " + featureEventList.size())
            return null
        }
        return featureEventList.first()
    }

    /**
     * This is the root uniqueName
     * @param uniqueName
     * @return
     */
    List<FeatureEvent> getHistory(String uniqueName) {
        FeatureEvent currentFeatureEvent = findCurrentFeatureEvent(uniqueName)
        Set<FeatureEvent> featureEvents = findAllPreviousFeatureEvents(currentFeatureEvent)
        featureEvents.addAll(findAllFutureFeatureEvents(currentFeatureEvent))
        featureEvents.add(currentFeatureEvent)


        return (featureEvents as List).sort() { a, b ->
            a.dateCreated <=> b.dateCreated
        }
    }

}
