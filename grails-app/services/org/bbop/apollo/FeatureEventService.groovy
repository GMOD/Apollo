package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.history.FeatureOperation
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.grails.plugins.metrics.groovy.Timed

import java.text.DateFormat

/**
 */
@Transactional
class FeatureEventService {

    def permissionService
    def transcriptService
    def featureService
    def requestHandlingService

    /**
     *
     * @param featureOperation
     * @param geneName
     * @param transcriptUniqueName
     * @param commandObject
     * @param jsonObject
     * @param user
     * @return
     */
    FeatureEvent addNewFeatureEvent(FeatureOperation featureOperation, String geneName, String transcriptUniqueName, JSONObject commandObject, JSONObject jsonObject, User user) {
        addNewFeatureEventWithUser(featureOperation, geneName, transcriptUniqueName, commandObject, jsonObject, user)
    }

    FeatureEvent addNewFeatureEventWithUser(FeatureOperation featureOperation, String name, String uniqueName, JSONObject commandObject, JSONObject jsonObject, User user) {
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(jsonObject)
        return addNewFeatureEvent(featureOperation, name, uniqueName, commandObject, new JSONArray(), newFeatureArray, user)

    }

    /**
     * Convention is that 1 is the parent and is returned first in the array.
     * Because we are tracking the split in the actual object blocks, the newJSONArray is also split
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
    @Timed
    List<FeatureEvent> addSplitFeatureEvent(String name1, String uniqueName1, String name2, String uniqueName2
                                            , JSONObject commandObject, JSONObject oldFeatureObject
                                            , JSONArray newFeatureArray
                                            , User user) {
        Map<String, Map<Long, FeatureEvent>> featureEventMap = extractFeatureEventGroup(uniqueName1)
        featureEventMap.putAll(extractFeatureEventGroup(uniqueName2))
        List<FeatureEvent> featureEventList = new ArrayList<>()
        JSONArray oldFeatureArray = new JSONArray()
        oldFeatureArray.add(oldFeatureObject)

        List<FeatureEvent> lastFeatureEventList = findCurrentFeatureEvent(uniqueName1, featureEventMap)
        if (lastFeatureEventList.size() != 1) {
            throw new AnnotationException("Not one current feature event being split for: " + uniqueName1)
        }
        if (!lastFeatureEventList) {
            throw new AnnotationException("Can not find original feature event to split for " + uniqueName1)
        }
        FeatureEvent lastFeatureEvent = lastFeatureEventList[0]
        lastFeatureEvent.current = false;
        lastFeatureEvent.save(flush: true)
        deleteFutureHistoryEvents(lastFeatureEvent)

        Date addDate = new Date()

        JSONArray newFeatureArray1 = new JSONArray()
        JSONArray newFeatureArray2 = new JSONArray()

        newFeatureArray1.add(newFeatureArray.getJSONObject(0))
        newFeatureArray2.add(newFeatureArray.getJSONObject(1))

        FeatureEvent featureEvent1 = new FeatureEvent(
                editor: user
                , name: name1
                , uniqueName: uniqueName1
                , operation: FeatureOperation.SPLIT_TRANSCRIPT
                , current: true
                , originalJsonCommand: commandObject.toString()
                , newFeaturesJsonArray: newFeatureArray1.toString()
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
                , newFeaturesJsonArray: newFeatureArray2.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: addDate
                , lastUpdated: addDate
        ).save()

        lastFeatureEvent.childId = featureEvent1.id
        lastFeatureEvent.childSplitId = featureEvent2.id
        featureEvent2.parentId = lastFeatureEvent.id
        featureEvent1.parentId = lastFeatureEvent.id

        featureEvent1.save()
        featureEvent2.save()
        lastFeatureEvent.save()

        featureEventList.add(featureEvent1)
        featureEventList.add(featureEvent2)


        return featureEventList
    }

    /**
     * Convention is that 1 becomes the child and is returned.
     * Because we are tracking the merge in the actual object blocks, the newJSONArray is also split
     * @param geneName1
     * @param uniqueName1
     * @param geneName2
     * @param uniqueName2
     * @param commandObject
     * @param oldFeatureArray
     * @param newFeatureObject
     * @param user
     * @return
     */
    @Timed
    List<FeatureEvent> addMergeFeatureEvent(String geneName1, String uniqueName1, String geneName2, String uniqueName2, JSONObject commandObject, JSONArray oldFeatureArray, JSONObject newFeatureObject,
                                            User user) {
        List<FeatureEvent> featureEventList = new ArrayList<>()
        Map<String, Map<Long, FeatureEvent>> featureEventMap1 = extractFeatureEventGroup(uniqueName1)
        Map<String, Map<Long, FeatureEvent>> featureEventMap2 = extractFeatureEventGroup(uniqueName2)

        List<FeatureEvent> lastFeatureEventLeftList = findCurrentFeatureEvent(uniqueName1, featureEventMap1)
        if (!lastFeatureEventLeftList) {
            throw new AnnotationException("Can not find original feature event to split for " + uniqueName1)
        }
        List<FeatureEvent> lastFeatureEventRightList = findCurrentFeatureEvent(uniqueName2, featureEventMap2)
        if (!lastFeatureEventRightList) {
            throw new AnnotationException("Can not find original feature event to split for " + uniqueName2)
        }


        FeatureEvent lastFeatureEventLeft = lastFeatureEventLeftList[0]
        FeatureEvent lastFeatureEventRight = lastFeatureEventRightList[0]
        lastFeatureEventLeft.current = false;
        lastFeatureEventRight.current = false;
        lastFeatureEventLeft.save()
        lastFeatureEventRight.save()
//        featureEventMap1 = featureEventMap1.putAll(featureEventMap2)
        deleteFutureHistoryEvents(lastFeatureEventLeft)
        deleteFutureHistoryEvents(lastFeatureEventRight)

        Date addDate = new Date()

        JSONArray newFeatureArray1 = new JSONArray()

        newFeatureArray1.add(newFeatureObject)

        FeatureEvent featureEvent1 = new FeatureEvent(
                editor: user
                , name: geneName1
                , uniqueName: uniqueName1
                , operation: FeatureOperation.MERGE_TRANSCRIPTS
                , current: true
                , originalJsonCommand: commandObject.toString()
                , newFeaturesJsonArray: newFeatureArray1.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: addDate
                , lastUpdated: addDate
        ).save()


        lastFeatureEventLeft.childId = featureEvent1.id
        lastFeatureEventRight.childId = featureEvent1.id
        featureEvent1.parentId = lastFeatureEventLeft.id
        featureEvent1.parentMergeId = lastFeatureEventRight.id

        featureEvent1.save()
        lastFeatureEventLeft.save()
        lastFeatureEventRight.save(flush: true)

        featureEventList.add(featureEvent1)


        return featureEventList
    }

    /**
     * For non-split , non-merge operations
     */
    @Timed
    def addNewFeatureEvent(FeatureOperation featureOperation, String name, String uniqueName, JSONObject inputCommand, JSONArray oldFeatureArray, JSONArray newFeatureArray, User user) {

        Map<String, Map<Long, FeatureEvent>> featureEventMap = extractFeatureEventGroup(uniqueName)


        List<FeatureEvent> lastFeatureEventList = findCurrentFeatureEvent(uniqueName, featureEventMap)
        FeatureEvent lastFeatureEvent = null
        lastFeatureEventList?.each { a ->
            if (a.uniqueName == uniqueName) {
                lastFeatureEvent = a
            }
        }
        if (lastFeatureEvent) {
            lastFeatureEvent.current = false;
            lastFeatureEvent.save(flush: true)
            deleteFutureHistoryEvents(lastFeatureEvent)
        }

        FeatureEvent featureEvent = new FeatureEvent(
                editor: user
                , name: name
                , uniqueName: uniqueName
                , operation: featureOperation.name()
                , current: true
                , parentId: lastFeatureEvent?.id
                , originalJsonCommand: inputCommand.toString()
                , newFeaturesJsonArray: newFeatureArray.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: new Date()
                , lastUpdated: new Date()
        ).save(flush: true)

        // set the children here properly
        if (lastFeatureEvent) {
            lastFeatureEvent.childId = featureEvent.id
            lastFeatureEvent.save(flush: true)
        }

        return featureEvent
    }

    Map<String, Map<Long, FeatureEvent>> extractFeatureEventGroup(String uniqueName, Map<String, Map<Long, FeatureEvent>> featureEventMap = new HashMap<>()) {
        def featureEvents = FeatureEvent.findAllByUniqueName(uniqueName)
        Map<Long, FeatureEvent> longFeatureEventMap = new HashMap<>()
        Set<Long> idsToCollect = new HashSet<>()
        featureEvents.each {
            longFeatureEventMap.put(it.id, it)
            idsToCollect.add(it.childId)
            idsToCollect.add(it.childSplitId)
            idsToCollect.add(it.parentId)
            idsToCollect.add(it.parentMergeId)
            idsToCollect = idsToCollect - longFeatureEventMap.keySet()
        }
        idsToCollect.removeAll(Collections.singleton(null));

        featureEventMap.put(uniqueName, longFeatureEventMap)

        if (idsToCollect) {

            Collection<String> uniqueNames = FeatureEvent.withCriteria {
                and {
                    'in'("id", idsToCollect)
                    not {
                        'in'("uniqueName", featureEventMap.keySet())
                    }
                }
            }.uniqueName.unique()

            uniqueNames.each {
                featureEventMap.putAll(extractFeatureEventGroup(it, featureEventMap))
            }
        }


        return featureEventMap
    }

    def setNotPreviousFutureHistoryEvents(FeatureEvent featureEvent) {
        List<List<FeatureEvent>> featureEventList = findPreviousFeatureEvents(featureEvent)
        featureEventList.each { array ->
            array.each {
                if (it.current) {
                    it.current = false
                    it.save(flush: true)
                }
            }
        }
    }

    def setNotCurrentFutureHistoryEvents(FeatureEvent featureEvent) {
        List<List<FeatureEvent>> featureEventList = findFutureFeatureEvents(featureEvent)
        featureEventList.each { array ->
            array.each {
                if (it.current) {
                    it.current = false
                    it.save(flush: true)
                }
            }
        }
    }

    def deleteFutureHistoryEvents(FeatureEvent featureEvent) {
        List<List<FeatureEvent>> featureEventList = findFutureFeatureEvents(featureEvent)
        int count = 0
        featureEventList.each { it.each { it.delete(); ++count } }
        return count
    }

    FeatureEvent findFeatureEventFromMap(Long parentId, Map<String, Map<Long, FeatureEvent>> featureEventMap) {
        for (Map<Long, FeatureEvent> map in featureEventMap.values()) {
            if (map.containsKey(parentId)) {
                return map.get(parentId)
            }
        }
        return null
    }

    /**
     * Should flatten the tree and add indexes going all the way up on each one.
     *
     * @param featureEvent
     * @param featureEventMap
     * @return
     */
    @Timed
    List<List<FeatureEvent>> findPreviousFeatureEvents(FeatureEvent featureEvent) {
        Map<Integer, Set<FeatureEvent>> map = new TreeMap<Integer, Set<FeatureEvent>>()
        buildMap(featureEvent, map, 0, true, false)
        removeSelfFromMap(featureEvent, map)
        // find and remove self
        List<List<FeatureEvent>> featureEventList = generateArrayFromTree(map)

        def uniqueFeatureEventList = makeUniqueFeatureEvents(featureEventList)
        return uniqueFeatureEventList
    }

    List<List<FeatureEvent>> makeUniqueFeatureEvents(List<List<FeatureEvent>> featureEventList) {
        def uniqueFeatureEventList = featureEventList.unique(false) { a, b ->
            for (aIndex in a) {
                for (bIndex in b) {
                    if (aIndex.id == bIndex.id) {
                        return 0
                    }
                }
            }
            return 1
        }
        return uniqueFeatureEventList
    }

    def removeSelfFromMap(FeatureEvent featureEvent, TreeMap<Integer, Set<FeatureEvent>> map) {
        def iter = map.keySet().iterator()
        def keysToRemove = []
        while (iter.hasNext()) {
            def it = iter.next()
            if (map.get(it).contains(featureEvent)) {
                keysToRemove << it
            }
        }

        keysToRemove.each {
            map.remove(it)
        }
    }
/**
 * Ordered from 0 is first N is last (most recent)
 * @param featureEvent
 * @return
 */
    @Timed
    List<List<FeatureEvent>> findFutureFeatureEvents(FeatureEvent featureEvent) {
        Map<Integer, Set<FeatureEvent>> map = new TreeMap<Integer, Set<FeatureEvent>>()
        buildMap(featureEvent, map, 0, false, true)
        removeSelfFromMap(featureEvent, map)
        // find and remove self
        List<List<FeatureEvent>> featureEventList = generateArrayFromTree(map)

        def uniqueFeatureEventList = makeUniqueFeatureEvents(featureEventList)
        return uniqueFeatureEventList
    }


    @Timed
    def addNewFeatureEvent(FeatureOperation featureOperation, String name, String uniqueName, JSONObject inputCommand, JSONObject oldJsonObject, JSONObject newJsonObject, User user) {
        JSONArray newFeatureArray = new JSONArray()
        newFeatureArray.add(newJsonObject)
        JSONArray oldFeatureArray = new JSONArray()
        oldFeatureArray.add(oldJsonObject)

        return addNewFeatureEvent(featureOperation, name, uniqueName, inputCommand, oldFeatureArray, newFeatureArray, user)
    }

    /**
     * Evaluates if history can be set to a certain position.
     * Should mirror setTransactionForFeature
     * @param uniqueName
     * @param count
     * @return Error message
     */
    Boolean evaluateSetTransactionForFeature(String uniqueName, int newIndex) throws AnnotationException{
        try {
            log.debug "setting previous transaction for feature ${uniqueName} -> ${newIndex}"
            log.debug "unique values: ${FeatureEvent.countByUniqueName(uniqueName)} -> ${newIndex}"

            // find the current index of the current feature
            Integer currentIndex = getCurrentFeatureEventIndex(uniqueName)
            log.debug "deepest current index ${currentIndex}"

            List<FeatureEvent> currentFeatureEventArray = findCurrentFeatureEvent(uniqueName)
            log.debug "current feature event array ${currentFeatureEventArray as JSON}"
            FeatureEvent currentFeatureEvent = currentFeatureEventArray.find() { it.uniqueName == uniqueName }
            currentFeatureEvent = currentFeatureEvent ?: currentFeatureEventArray.first()

            log.debug "current feature event ${currentFeatureEvent as JSON}"
            // if the current index is GREATER, then find the future indexes and set appropriately
            if (newIndex > currentIndex) {
                List<List<FeatureEvent>> futureFeatureEvents = findFutureFeatureEvents(currentFeatureEvent)
                currentFeatureEventArray = futureFeatureEvents.get(newIndex - currentIndex - 1)
                // subtract one for the index offset
            }
            // if the current index is LESS, then find the previous indexes and set appropriately
            else if (newIndex < currentIndex) {
                List<List<FeatureEvent>> previousFeatureEvents = findPreviousFeatureEvents(currentFeatureEvent)
    //            currentFeatureEventArray = previousFeatureEvents.get(newIndex)
                if (newIndex >= previousFeatureEvents.size()) {
                    throw new AnnotationException("Can not undo this operation due to a split or a merge.  Try to undo or redo using a different genomic feature.")
                }
            }
            return true
        } catch (e) {
            // just pass it through
            if(e instanceof AnnotationException){
                throw e
            }
            else{
                throw new AnnotationException("Can not set history for this operation.  Try to undo or redo using a different genomic feature. ${e.message}")
            }
        }
    }

    /**
     * CurrentIndex of 0 is the oldest.  Highest number is the most recent
     * This returns an array.  We could have any number of splits going forward, so we have to return an array here.
     * @param uniqueName
     * @param newIndex
     * @return
     */
    List<FeatureEvent> setTransactionForFeature(String uniqueName, int newIndex) {

        Map<String, Map<Long, FeatureEvent>> featureEventMap = extractFeatureEventGroup(uniqueName)

        log.debug "setting previous transaction for feature ${uniqueName} -> ${newIndex}"
        log.debug "unique values: ${FeatureEvent.countByUniqueName(uniqueName)} -> ${newIndex}"

        // find the current index of the current feature
        Integer currentIndex = getCurrentFeatureEventIndex(uniqueName)
        log.debug "deepest current index ${currentIndex}"

        List<FeatureEvent> currentFeatureEventArray = findCurrentFeatureEvent(uniqueName)
        log.debug "current feature event array ${currentFeatureEventArray}"
        FeatureEvent currentFeatureEvent = currentFeatureEventArray.find() { it.uniqueName == uniqueName }
        currentFeatureEvent = currentFeatureEvent ?: currentFeatureEventArray.first()

        log.debug "current feature event ${currentFeatureEvent}"
        // if the current index is GREATER, then find the future indexes and set appropriately
        if (newIndex > currentIndex) {
            List<List<FeatureEvent>> futureFeatureEvents = findFutureFeatureEvents(currentFeatureEvent)
            currentFeatureEventArray = futureFeatureEvents.get(newIndex - currentIndex - 1)
            // subtract one for the index offset
            currentFeatureEventArray.each {
                it.current = true
                it.save()
            }
        }
        // if the current index is LESS, then find the previous indexes and set appropriately
        else if (newIndex < currentIndex) {
            List<List<FeatureEvent>> previousFeatureEvents = findPreviousFeatureEvents(currentFeatureEvent)
            currentFeatureEventArray = previousFeatureEvents.get(newIndex)
            currentFeatureEventArray.each {
                it.current = true
                it.save()
            }
        } else {
            log.warn("Setting history to same place ${currentIndex} -> ${newIndex}")
            return findCurrentFeatureEvent(uniqueName, featureEventMap)
        }

        currentFeatureEvent = currentFeatureEventArray.find() { it.uniqueName == uniqueName }
        currentFeatureEvent = currentFeatureEvent ?: currentFeatureEventArray.first()

        setNotPreviousFutureHistoryEvents(currentFeatureEvent)
        setNotCurrentFutureHistoryEvents(currentFeatureEvent)

        currentFeatureEvent.save(flush: true)

        return findCurrentFeatureEvent(uniqueName, featureEventMap)
    }

    def setHistoryState(JSONObject inputObject, int count) {

        String uniqueName = inputObject.getString(FeatureStringEnum.UNIQUENAME.value)
        log.debug "undo count ${count}"
        if (count < 0) {
            log.warn("Can not undo any further")
            return
        }

        List<List<FeatureEvent>> history = getHistory(uniqueName)
        int total = history.size()
        if (total == 0) {
            Set<String> uniqueNames = extractFeatureEventGroup(uniqueName).keySet()
            uniqueNames.remove(uniqueName)
            for (int i = 0; total == 0 && i < uniqueNames.size(); i++) {
                String name = uniqueNames[i]
                total = getHistory(name)?.size()
                uniqueName = total > 0 ? name : uniqueNames
            }
        }
        if (count >= total) {
            log.warn("Can not redo any further")
            return
        }

        def newUniqueNames = history[count].collect() {
            it.uniqueName
        }

        Sequence sequence = Feature.findByUniqueNameInList(newUniqueNames).featureLocation.sequence
        log.debug "sequence: ${sequence}"



        assert evaluateSetTransactionForFeature(uniqueName, count)
        deleteCurrentState(inputObject, newUniqueNames, sequence)
        List<FeatureEvent> featureEventArray = setTransactionForFeature(uniqueName, count)

        def transcriptsToCheckForIsoformOverlap = []
        featureEventArray.each { featureEvent ->
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
                    addCommandObject.put(FeatureStringEnum.TRACK.value, featuresToAddArray.getJSONObject(0).getString(FeatureStringEnum.SEQUENCE.value))
                }

                addCommandObject = permissionService.copyRequestValues(inputObject, addCommandObject)

                addCommandObject.put(FeatureStringEnum.SUPPRESS_HISTORY.value, true)


                if (featureService.isJsonTranscript(jsonFeature)) {
                    // set the original gene name
                    addCommandObject.put(FeatureStringEnum.SUPPRESS_EVENTS.value, true)
                    for (int k = 0; k < featuresToAddArray.size(); k++) {
                        JSONObject featureObject = featuresToAddArray.getJSONObject(k)
                        featureObject.put(FeatureStringEnum.GENE_NAME.value, featureEvent.name)
                    }
                    log.debug "original command object = ${originalCommandObject as JSON}"
                    log.debug "final command object = ${addCommandObject as JSON}"
                    requestHandlingService.addTranscript(addCommandObject)
                    transcriptsToCheckForIsoformOverlap.add(jsonFeature.getString("uniquename"))

                } else {
                    addCommandObject.put(FeatureStringEnum.SUPPRESS_EVENTS.value, false)
                    requestHandlingService.addFeature(addCommandObject)
                }

            }
        }

        // after all the transcripts from the feature event has been added, applying isoform overlap rule
        Set transcriptsToUpdate = new HashSet()
        transcriptsToCheckForIsoformOverlap.each {
            transcriptsToUpdate.add(it)
            transcriptsToUpdate.addAll(featureService.handleDynamicIsoformOverlap(Transcript.findByUniqueName(it)).uniqueName)
        }

        // firing update annotation event
        if (transcriptsToUpdate.size() > 0) {
            JSONObject updateFeatureContainer = requestHandlingService.createJSONFeatureContainer()
            transcriptsToUpdate.each {
                Transcript transcript = Transcript.findByUniqueName(it)
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript))
            }
            if (sequence) {
                AnnotationEvent annotationEvent = new AnnotationEvent(
                        features: updateFeatureContainer,
                        sequence: sequence,
                        operation: AnnotationEvent.Operation.UPDATE
                )
                requestHandlingService.fireAnnotationEvent(annotationEvent)
            }
        }

        return featureEventArray

    }


    def deleteCurrentState(JSONObject inputObject, List<String> newUniqueNames, Sequence sequence) {
        for (uniqueName in newUniqueNames) {
            deleteCurrentState(inputObject, uniqueName, sequence)
        }
    }


    def deleteCurrentState(JSONObject inputObject, String uniqueName, Sequence sequence) {

        Map<String, Map<Long, FeatureEvent>> featureEventMap = extractFeatureEventGroup(uniqueName)

        // need to get uniqueNames for EACH current featureEvent
        def currentFeatureEvents = findCurrentFeatureEvent(uniqueName, featureEventMap)
        for (FeatureEvent deleteFeatureEvent in currentFeatureEvents) {
            JSONObject deleteCommandObject = new JSONObject()
            JSONArray featuresArray = new JSONArray()
            log.debug "delete feature event uniqueNamee: ${deleteFeatureEvent.uniqueName}"
            JSONObject featureToDelete = new JSONObject()
            featureToDelete.put(FeatureStringEnum.UNIQUENAME.value, deleteFeatureEvent.uniqueName)
            featuresArray.add(featureToDelete)

            log.debug "inputObject ${inputObject as JSON}"
            log.debug "deleteCommandObject ${deleteCommandObject as JSON}"

            if (!deleteCommandObject.containsKey(FeatureStringEnum.TRACK.value)) {
                deleteCommandObject.put(FeatureStringEnum.TRACK.value, sequence.name)
            }
            deleteCommandObject.put(FeatureStringEnum.SUPPRESS_HISTORY, true)
            log.debug "final deleteCommandObject ${deleteCommandObject as JSON}"

            deleteCommandObject.put(FeatureStringEnum.FEATURES.value, featuresArray)
            deleteCommandObject = permissionService.copyRequestValues(inputObject, deleteCommandObject)

            log.debug " final delete JSON ${deleteCommandObject as JSON}"
            // suppress any events that are not part of the new state
            requestHandlingService.deleteFeature(deleteCommandObject)
            log.debug "deletion success . .  "
        }

    }

    /**
     * Count back from most recent
     * @param inputObject
     * @param count
     * @param confirm
     * @return
     */
    def redo(JSONObject inputObject, int countForward) {
        log.info "redoing ${countForward}"
        if (countForward == 0) {
            log.warn "Redo to the same state"
            return
        }
        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        int currentIndex = getCurrentFeatureEventIndex(uniqueName)
//        Set<String> uniqueNames = extractFeatureEventGroup(uniqueName).keySet()
//        assert uniqueNames.remove(uniqueName)
//        uniqueNames.each {
//            currentIndex = Math.max(getCurrentFeatureEventIndex(it), currentIndex)
//        }
        int count = currentIndex + countForward
        log.info "current Index ${currentIndex}"
        log.info "${count} = ${currentIndex}-${countForward}"
        setHistoryState(inputObject, count)
    }

    /**
     * We count backwards in order to get the correct count.
     * @param uniqueName
     * @return
     */
    int getCurrentFeatureEventIndex(String uniqueName, Map<String, Map<Long, FeatureEvent>> featureEventMap = null) {

        List<FeatureEvent> currentFeatureEventList = FeatureEvent.findAllByUniqueNameAndCurrent(uniqueName, true, [sort: "dateCreated", order: "asc"])
        featureEventMap = extractFeatureEventGroup(uniqueName)
        if (!currentFeatureEventList) {
            featureEventMap.keySet().each {
                if (!currentFeatureEventList && uniqueName != it) {
                    currentFeatureEventList = FeatureEvent.findAllByUniqueNameAndCurrent(it, true, [sort: "dateCreated", order: "asc"])
                }
            }
        }

        if (currentFeatureEventList.size() != 1) {
            throw new AnnotationException("Feature event list is the wrong size ${currentFeatureEventList?.size()}")
        }
        FeatureEvent currentFeatureEvent = currentFeatureEventList.iterator().next()
        def history = getHistory(uniqueName)
        Integer deepestIndex = history.size() - 1
        def previousEvents = findPreviousFeatureEvents(currentFeatureEvent)
        def futureEvents = findFutureFeatureEvents(currentFeatureEvent)
        def offset = deepestIndex - futureEvents.size() - previousEvents.size()
        def returnIndex = previousEvents.size() + offset
        return returnIndex
//        Integer deepestIndex = getDeepestIndex(-1, currentFeatureEvent, featureEventMap)
    }

    int getDeepestIndex(int index, FeatureEvent currentFeatureEvent, Map<String, Map<Long, FeatureEvent>> featureEventMap) {
        ++index
        FeatureEvent featureEvent1 = null
        FeatureEvent featureEvent2 = null
        if (currentFeatureEvent.parentId) {
            featureEvent1 = findFeatureEventFromMap(currentFeatureEvent.parentId, featureEventMap)
        }
        if (currentFeatureEvent.parentMergeId) {
            featureEvent2 = findFeatureEventFromMap(currentFeatureEvent.parentMergeId, featureEventMap)
        }
        Integer p1Id = featureEvent1 ? getDeepestIndex(index, featureEvent1, featureEventMap) : index
        Integer p2Id = featureEvent2 ? getDeepestIndex(index, featureEvent2, featureEventMap) : index
        return Math.max(p1Id, p2Id)
    }

    def undo(JSONObject inputObject, int countBackwards) {
        log.info "undoing ${countBackwards}"
        if (countBackwards == 0) {
            log.warn "Undo to the same state"
            return
        }

        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        int currentIndex = getCurrentFeatureEventIndex(uniqueName)
        int count = currentIndex - countBackwards
        log.debug "${count} = ${currentIndex}-${countBackwards}"
        setHistoryState(inputObject, count)
    }

    /**
     * We find the current one for the uniqueName and its index
     * We then count forward by the same amount and return that.
     * @param uniqueName
     * @return
     */
    @Timed
    List<FeatureEvent> findCurrentFeatureEvent(String uniqueName, Map<String, Map<Long, FeatureEvent>> featureEventMap = null) {
        featureEventMap = featureEventMap ?: extractFeatureEventGroup(uniqueName)

        List<FeatureEvent> currentFeatureEvents = []
        featureEventMap.values().each {
            it.values().each() { featureEvent ->
                if (featureEvent.current) {
                    currentFeatureEvents.add(featureEvent)
                }
            }
        }

        return currentFeatureEvents

    }

    /**
     * This is the root uniqueName
     * Should returned sorted most recent at 0, latest at end
     *
     * If splits occur we need to show them
     * @param uniqueName
     * @return
     */
    List<List<FeatureEvent>> getHistory(String uniqueName, Map<String, Map<Long, FeatureEvent>> featureEventMap = null) {
        featureEventMap = featureEventMap ?: extractFeatureEventGroup(uniqueName)


        List<FeatureEvent> currentFeatureEvents = findCurrentFeatureEvent(uniqueName, featureEventMap)
        // if we revert a split or do a merge
        if (!currentFeatureEvents) {
            return new ArrayList<List<FeatureEvent>>()
        }


        TreeMap<Integer, Set<FeatureEvent>> unindexedMap = new TreeMap<>()
        buildMap(currentFeatureEvents[0], unindexedMap, 0, true, true)

        List<List<FeatureEvent>> featureEvents = generateArrayFromTree(unindexedMap)

        // if we have a split, it will pick up the same values twice
        // so we need to filter those out
        // sort by what is on top of what
        // if we have a merge, where one of the merges has history, but the other doesn't
        def uniqueFeatureEvents = featureEvents.unique(false) { a, b ->
            for (aIndex in a) {
                for (bIndex in b) {
                    if (aIndex.id == bIndex.id) {
                        return 0
                    }
                }
            }
            return 1
        }
        return uniqueFeatureEvents
    }

    List<List<FeatureEvent>> generateArrayFromTree(TreeMap<Integer, Set<FeatureEvent>> unindexedMap) {
        List<List<FeatureEvent>> featureEvents = new ArrayList<List<FeatureEvent>>()
        for (int i = 0; i < unindexedMap.size(); i++) {
            featureEvents.add(new ArrayList<FeatureEvent>())
        }

        if (unindexedMap) {
            Integer offset = unindexedMap.keySet().first()
            for (index in unindexedMap.keySet()) {
                featureEvents.set(index - offset, unindexedMap.get(index) as List<FeatureEvent>)
            }
        }
        return featureEvents
    }

    def buildMap(FeatureEvent featureEvent, TreeMap<Integer, Set<FeatureEvent>> unindexedMap, int index, Boolean includePrevious, Boolean includeFuture) {

        if (!featureEvent) return

        def featureEventSet = unindexedMap.get(index)
        if (!featureEventSet) {
            featureEventSet = new HashSet<FeatureEvent>()
        } else if (featureEventSet.contains(featureEvent)) {
            return
        }
        featureEventSet.add(featureEvent)
        unindexedMap.put(index, featureEventSet)

        if (featureEvent.parentId && includePrevious) {
            FeatureEvent parentFeatureEvent = FeatureEvent.findById(featureEvent.parentId)
            buildMap(parentFeatureEvent, unindexedMap, index - 1, includePrevious, includeFuture)

            if (featureEvent.parentMergeId) {
                FeatureEvent mergeFeatureEvent = FeatureEvent.findById(featureEvent.parentMergeId)
                buildMap(mergeFeatureEvent, unindexedMap, index - 1, includePrevious, includeFuture)
            }
        }
        if (featureEvent.childId && includeFuture) {
            FeatureEvent childFeatureEvent = FeatureEvent.findById(featureEvent.childId)
            buildMap(childFeatureEvent, unindexedMap, index + 1, includePrevious, includeFuture)

            if (featureEvent.childSplitId) {
                FeatureEvent splitFeatureEvent = FeatureEvent.findById(featureEvent.childSplitId)
                buildMap(splitFeatureEvent, unindexedMap, index + 1, includePrevious, includeFuture)
            }
        }
    }

    JSONObject generateHistory(JSONObject historyContainer, JSONArray featuresArray) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (int i = 0; i < featuresArray.size(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)

            List<FeatureEvent> currentFeatureEventList = findCurrentFeatureEvent(uniqueName)
            def thisFeatureEvent = currentFeatureEventList.find() { it.uniqueName == uniqueName }
            thisFeatureEvent = thisFeatureEvent ?: currentFeatureEventList.first()
            def futureEvents = findFutureFeatureEvents(thisFeatureEvent)
            def previousEvents = findPreviousFeatureEvents(thisFeatureEvent)


            JSONArray history = new JSONArray();
            jsonFeature.put(FeatureStringEnum.HISTORY.value, history);
            List<List<FeatureEvent>> transactionList = new ArrayList<>()
            transactionList.addAll(previousEvents)
            transactionList.add(currentFeatureEventList)
            transactionList.addAll(futureEvents)


            for (int j = 0; j < transactionList.size(); ++j) {
                // not sure if this is correct, or if I should just add both?
                List<FeatureEvent> transactionSet = transactionList[j]
                FeatureEvent transaction = transactionSet.find() { it.uniqueName == uniqueName }
                transaction = transaction ?: transactionSet.first()
                // prefer a predecessor that is not "ADD_TRANSCRIPT"?

                JSONObject historyItem = new JSONObject();
                historyItem.put(AbstractApolloController.REST_OPERATION, transaction.operation.name());
                historyItem.put(FeatureStringEnum.EDITOR.value, transaction.getEditor()?.username);
                historyItem.put(FeatureStringEnum.DATE.value, dateFormat.format(transaction.dateCreated));
                if (transaction.current) {
                    historyItem.put(FeatureStringEnum.CURRENT.value, true);
                } else {
                    historyItem.put(FeatureStringEnum.CURRENT.value, false);
                }
                JSONArray historyFeatures = new JSONArray();
                historyItem.put(FeatureStringEnum.FEATURES.value, historyFeatures);

                if (transaction.newFeaturesJsonArray) {
                    JSONArray newFeaturesJsonArray = (JSONArray) JSON.parse(transaction.newFeaturesJsonArray)
                    for (int featureIndex = 0; featureIndex < newFeaturesJsonArray.size(); featureIndex++) {
                        JSONObject featureJsonObject = newFeaturesJsonArray.getJSONObject(featureIndex)
                        historyFeatures.put(featureJsonObject);
                    }
                    history.put(historyItem);
                }
            }
            historyContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature);
        }
        return historyContainer
    }
}
