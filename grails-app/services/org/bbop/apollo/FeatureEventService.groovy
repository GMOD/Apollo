package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.history.FeatureOperation
import grails.util.Environment
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 */
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
        if (!lastFeatureEvent) {
            throw new AnnotationException("Can not find original feature event to split for " + uniqueName1)
        }
        lastFeatureEvent.current = false;
        lastFeatureEvent.save()
        deleteFutureHistoryEvents(lastFeatureEvent)

        Date addDate = new Date()

        FeatureEvent featureEvent1 = new FeatureEvent(
                editor: user
                , name: name1
                , uniqueName: uniqueName1
                , operation: FeatureOperation.SPLIT_TRANSCRIPT
                , current: true
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
                , originalJsonCommand: commandObject.toString()
                , newFeaturesJsonArray: newFeatureArray.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: addDate
                , lastUpdated: addDate
        ).save()

        lastFeatureEvent.childId = featureEvent1.id
        lastFeatureEvent.childSplitId = featureEvent2.id
        featureEvent2.parentId = lastFeatureEvent.id
        featureEvent1.parentId = lastFeatureEvent.id
        featureEvent2.save()
        lastFeatureEvent.save()


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
                , parentId: lastFeatureEvent ? lastFeatureEvent.id : null
                , originalJsonCommand: inputCommand.toString()
                , newFeaturesJsonArray: newFeatureArray.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: new Date()
                , lastUpdated: new Date()
        ).save()

        if (lastFeatureEvent) {
            lastFeatureEvent.childId = featureEvent.id
            lastFeatureEvent.save()
        }

        return featureEvent
    }

    def setNotCurrentFutureHistoryEvents(FeatureEvent featureEvent) {
        Set<List<FeatureEvent>> featureEventList = findAllFutureFeatureEvents(featureEvent)
        featureEventList.each { array ->
            array.each {
                if (it.current) {
                    it.current = false
                    it.save()
                }
            }
        }
    }

    def deleteFutureHistoryEvents(FeatureEvent featureEvent) {
        Set<List<FeatureEvent>> featureEventList = findAllFutureFeatureEvents(featureEvent)
        int count = 0
        featureEventList.each { it.each { it.delete(); ++count } }
        return count
//        return FeatureEvent.deleteAll(featureEventList.find().eac)
    }

    Set<List<FeatureEvent>> findAllPreviousFeatureEvents(FeatureEvent featureEvent) {
        Set<List<FeatureEvent>> featureEventList = new HashSet<>()
        Long parentId = featureEvent.parentId
        FeatureEvent parentFeatureEvent = parentId ? FeatureEvent.findById(parentId) : null

        while (parentFeatureEvent) {

            List<FeatureEvent> featureArrayList = new ArrayList<>()
            featureArrayList.add(parentFeatureEvent)


            FeatureEvent parentMergeFeatureEvent = featureEvent.parentMergeId ? FeatureEvent.findById(featureEvent.parentMergeId) : null
            if (parentMergeFeatureEvent) {
                featureArrayList.add(parentMergeFeatureEvent)
                featureEventList.addAll(findAllPreviousFeatureEvents(parentMergeFeatureEvent))
            }


            featureEventList.add(featureArrayList)
            featureEventList.addAll(findAllPreviousFeatureEvents(parentFeatureEvent))


            parentId = parentFeatureEvent.parentId
            parentFeatureEvent = parentId ? FeatureEvent.findById(parentId) : null


        }

//        parentId = featureEvent.parentMergeId
//        parentFeatureEvent = parentId ? FeatureEvent.findById(parentId) : null
//        while (parentFeatureEvent) {
//            featureEventList.add(parentFeatureEvent)
//            featureEventList.addAll(findAllPreviousFeatureEvents(parentFeatureEvent))
//            parentId = parentFeatureEvent.parentMergeId
//            parentFeatureEvent = parentId ? FeatureEvent.findById(parentId) : null
//        }

        return featureEventList
    }

    Set<List<FeatureEvent>> findAllFutureFeatureEvents(FeatureEvent featureEvent) {
        Set<List<FeatureEvent>> featureEventList = new HashSet<>()

        Long childId = featureEvent.childId
        FeatureEvent childFeatureEvent = childId ? FeatureEvent.findById(childId) : null
        while (childFeatureEvent) {
            List<FeatureEvent> featureArrayList = new ArrayList<>()
            featureArrayList.add(childFeatureEvent)

            FeatureEvent childSplitFeatureEvent = featureEvent.childSplitId ? FeatureEvent.findById(featureEvent.childSplitId) : null
            if (childSplitFeatureEvent) {
                featureArrayList.add(childSplitFeatureEvent)
                featureEventList.addAll(findAllFutureFeatureEvents(childSplitFeatureEvent))
            }
            featureEventList.addAll(findAllFutureFeatureEvents(childFeatureEvent))
            featureEventList.add(featureArrayList)

            childId = childFeatureEvent.childId
            childFeatureEvent = childId ? FeatureEvent.findById(childId) : null
        }

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
        FeatureEvent.deleteAll(getHistory(uniqueName))
//        FeatureEvent.deleteAll(FeatureEvent.findAllByUniqueName(uniqueName))
    }

/**
 * CurrentIndex of 0 is the oldest.  Highest number is the most recent
 * This returns an array.  We could have any number of splits going forward, so we have to return an array here.
 * @param uniqueName
 * @param currentIndex
 * @return
 */
    List<FeatureEvent> setTransactionForFeature(String uniqueName, int currentIndex) {
        log.info "setting previous transaction for feature ${uniqueName} -> ${currentIndex}"
        log.info "unique values: ${FeatureEvent.countByUniqueName(uniqueName)} -> ${currentIndex}"
        List<FeatureEvent> featureEventList = getHistory(uniqueName)
        FeatureEvent currentFeatureEvent = null
        for (int i = 0; i < featureEventList.size(); i++) {
            FeatureEvent featureEvent = featureEventList.get(i)
            if (i == currentIndex && !featureEvent.current) {
                featureEvent.current = true
                featureEvent.save()
                currentFeatureEvent = featureEvent
            } else if (i != currentIndex && featureEvent.current) {
                featureEvent.current = false
                featureEvent.save()
            }
        }

        if (!currentFeatureEvent) {
            log.warn "Did we forget to change the feature event?"
            return [findCurrentFeatureEvent(uniqueName)]
        }

        setNotCurrentFutureHistoryEvents(currentFeatureEvent)

//        log.debug "updated is ${updated}"
        return [findCurrentFeatureEvent(currentFeatureEvent.uniqueName)]
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
 * Should returned sorted most recent at 0, latest at end
 *
 * If splits occur we need to show them
 * @param uniqueName
 * @return
 */
    List<List<FeatureEvent>> getHistory(String uniqueName) {
        FeatureEvent currentFeatureEvent = findCurrentFeatureEvent(uniqueName)

        // if we revert a split or do a merge
        if (!currentFeatureEvent) return [[]]

        Set<List<FeatureEvent>> featureEvents = findAllPreviousFeatureEvents(currentFeatureEvent)
        featureEvents.addAll(findAllFutureFeatureEvents(currentFeatureEvent))
        featureEvents.add([currentFeatureEvent])

        return (featureEvents as List).sort() { a, b ->
            a.dateCreated <=> b.dateCreated
        }
    }

}
