package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import grails.util.Environment
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
        if (Environment.current == Environment.TEST) {
            return addNewFeatureEventWithUser(featureOperation, geneName, transcriptUniqueName, commandObject, jsonObject, (User) null)
        }
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
        lastFeatureEvent.save()
        deleteFutureHistoryEvents(lastFeatureEvent, featureEventMap)

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
//        if (lastFeatureEventLeftList.size() != 2) {
//            throw new AnnotationException("Not two current feature event being merged for: " + uniqueName1)
//        }
        if (!lastFeatureEventLeftList) {
            throw new AnnotationException("Can not find original feature event to split for " + uniqueName1)
        }
        List<FeatureEvent> lastFeatureEventRightList = findCurrentFeatureEvent(uniqueName2, featureEventMap2)
//        if (lastFeatureEventRightList.size() != 1) {
//            throw new AnnotationException("Not one current feature event being merged for: " + uniqueName2)
//        }
        if (!lastFeatureEventRightList) {
            throw new AnnotationException("Can not find original feature event to split for " + uniqueName2)
        }


        FeatureEvent lastFeatureEventLeft = lastFeatureEventLeftList[0]
        FeatureEvent lastFeatureEventRight = lastFeatureEventRightList[0]
        lastFeatureEventLeft.current = false;
        lastFeatureEventRight.current = false;
        lastFeatureEventLeft.save()
        lastFeatureEventRight.save()
        featureEventMap1 = featureEventMap1.putAll(featureEventMap2)
        deleteFutureHistoryEvents(lastFeatureEventLeft, featureEventMap1)
        deleteFutureHistoryEvents(lastFeatureEventRight, featureEventMap1)

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
        lastFeatureEventRight.save()

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
            lastFeatureEvent.save()
            deleteFutureHistoryEvents(lastFeatureEvent, featureEventMap)
        }

        FeatureEvent featureEvent = new FeatureEvent(
                editor: user
                , name: name
                , uniqueName: uniqueName
                , operation: featureOperation.name()
                , current: true
                , parentId: lastFeatureEvent?.id
//                , parentMergeId: lastFeatureEventList && lastFeatureEventList.size() > 1 ? lastFeatureEventList[1].id : null
                , originalJsonCommand: inputCommand.toString()
                , newFeaturesJsonArray: newFeatureArray.toString()
                , oldFeaturesJsonArray: oldFeatureArray.toString()
                , dateCreated: new Date()
                , lastUpdated: new Date()
        ).save()

        // set the children here properly
        if (lastFeatureEvent) {
            lastFeatureEvent.childId = featureEvent.id
            lastFeatureEvent.save()
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

//            List<String> uniqueNames = (List<String>) FeatureEvent.executeQuery("select distinct fe.uniqueName from FeatureEvent fe where fe.id in (:idsList) and uniqueName not in (:uniqueNames)",[idsList:idsToCollect,uniqueNames: featureEventMap.keySet()])
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

    def setNotPreviousFutureHistoryEvents(FeatureEvent featureEvent, Map<String, Map<Long, FeatureEvent>> featureEventMap) {
        List<List<FeatureEvent>> featureEventList = findAllPreviousFeatureEvents(featureEvent, featureEventMap)
        featureEventList.each { array ->
            array.each {
                if (it.current) {
                    it.current = false
                    it.save()
                }
            }
        }
    }

    def setNotCurrentFutureHistoryEvents(FeatureEvent featureEvent, Map<String, Map<Long, FeatureEvent>> featureEventMap) {
        List<List<FeatureEvent>> featureEventList = findAllFutureFeatureEvents(featureEvent, featureEventMap)
        featureEventList.each { array ->
            array.each {
                if (it.current) {
                    it.current = false
                    it.save()
                }
            }
        }
    }

    def deleteFutureHistoryEvents(FeatureEvent featureEvent, Map<String, Map<Long, FeatureEvent>> featureEventMap) {
        List<List<FeatureEvent>> featureEventList = findAllFutureFeatureEvents(featureEvent, featureEventMap)
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

    @Timed
    List<List<FeatureEvent>> findAllPreviousFeatureEvents(FeatureEvent featureEvent, Map<String, Map<Long, FeatureEvent>> featureEventMap = null) {
        featureEventMap = featureEventMap ?: extractFeatureEventGroup(featureEvent.uniqueName)
        List<List<FeatureEvent>> featureEventList = new ArrayList<>()
        Long parentId = featureEvent.parentId
//        FeatureEvent parentFeatureEvent = parentId ? FeatureEvent.findById(parentId) : null
        FeatureEvent parentFeatureEvent = parentId ? findFeatureEventFromMap(parentId, featureEventMap) : null

        if (parentFeatureEvent) {

            List<FeatureEvent> featureArrayList = new ArrayList<>()
            featureArrayList.add(parentFeatureEvent)

//            FeatureEvent parentMergeFeatureEvent = featureEvent.parentMergeId ? FeatureEvent.findById(featureEvent.parentMergeId) : null
            FeatureEvent parentMergeFeatureEvent = featureEvent.parentMergeId ? findFeatureEventFromMap(featureEvent.parentMergeId, featureEventMap) : null
            if (parentMergeFeatureEvent) {
                featureArrayList.add(parentMergeFeatureEvent)
                featureEventList.addAll(0,findAllPreviousFeatureEvents(parentMergeFeatureEvent, featureEventMap))
            }


            featureEventList.add(featureArrayList)
            featureEventList.addAll(0,findAllPreviousFeatureEvents(parentFeatureEvent, featureEventMap))

//            parentId = parentFeatureEvent.parentId
//            parentFeatureEvent = parentId ? findFeatureEventFromMap(parentId,featureEventMap) : null
        }

//        def sortedFeatureEventList = featureEventList.sort(true) { a, b ->
//            b[0].dateCreated <=> a[0].dateCreated
//        }
//
//        def uniqueFeatureEventList = sortedFeatureEventList.unique(true) { a, b ->
//            a[0].id <=> b[0].id
//        }
//        def uniqueFeatureEventList = featureEventList.unique(false) { a, b ->
//            a[0].id <=> b[0].id
//        }
        def uniqueFeatureEventList = featureEventList.unique(false) { a, b ->
            for (aIndex in a) {
                for (bIndex in b) {
                    if (aIndex.id == bIndex.id) {
                        return 0
                    }
                }
            }
            return 1
//            a[0].id <=> b[0].id
        }

        return uniqueFeatureEventList
    }

    /**
     * Ordered from 0 is first N is last (most recent)
     * @param featureEvent
     * @return
     */
    @Timed
    List<List<FeatureEvent>> findAllFutureFeatureEvents(FeatureEvent featureEvent, Map<String, Map<Long, FeatureEvent>> featureEventMap = null) {
        featureEventMap = featureEventMap ?: extractFeatureEventGroup(featureEvent.uniqueName)
        List<List<FeatureEvent>> featureEventList = new ArrayList<>()

        Long childId = featureEvent.childId
//        FeatureEvent childFeatureEvent = childId ? FeatureEvent.findById(childId) : null
        FeatureEvent childFeatureEvent = childId ? findFeatureEventFromMap(childId, featureEventMap) : null
        if (childFeatureEvent) {
            List<FeatureEvent> featureArrayList = new ArrayList<>()
            featureArrayList.add(childFeatureEvent)

//            FeatureEvent childSplitFeatureEvent = featureEvent.childSplitId ? FeatureEvent.findById(featureEvent.childSplitId) : null
            FeatureEvent childSplitFeatureEvent = featureEvent.childSplitId ? findFeatureEventFromMap(featureEvent.childSplitId, featureEventMap) : null
            if (childSplitFeatureEvent) {
                featureArrayList.add(childSplitFeatureEvent)
                featureEventList.addAll(0,findAllFutureFeatureEvents(childSplitFeatureEvent, featureEventMap))
            }

            // if there is a parent merge . .  we just include that parent in the history (not everything)
            // we have to assume that there is a previous feature event (a merge can never be first)
//            FeatureEvent parentMergeFeatureEvent = featureEvent.parentMergeId ? FeatureEvent.findById(featureEvent.parentMergeId) : null
            FeatureEvent parentMergeFeatureEvent = featureEvent.parentMergeId ? findFeatureEventFromMap(featureEvent.parentMergeId, featureEventMap) : null
            if (parentMergeFeatureEvent && featureEventList) {
                featureEventList.get(featureEventList.size() - 1).add(parentMergeFeatureEvent)
            }

            featureEventList.addAll(0,findAllFutureFeatureEvents(childFeatureEvent, featureEventMap))
            featureEventList.add(0,featureArrayList)

//            childId = childFeatureEvent.childId
//            childFeatureEvent = childId ? FeatureEvent.findById(childId) : null
//            childFeatureEvent = childId ? findFeatureEventFromMap(childId,featureEventMap) : null
        }

//        def sortedFeaturedEvents = featureEventList.sort(true) { a, b ->
//            a[0].dateCreated <=> b[0].dateCreated
//        }

        def uniqueFeatureEventList = featureEventList.unique(false) { a, b ->
            for (aIndex in a) {
                for (bIndex in b) {
                    if (aIndex.id == bIndex.id) {
                        return 0
                    }
                }
            }
            return 1
//            a[0].id <=> b[0].id
        }
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

//    /**
//     * @deprecated
//     */
//    FeatureEvent addNewFeatureEventWithUser(FeatureOperation featureOperation, Feature feature, JSONObject inputCommand, User user) {
//        return addNewFeatureEventWithUser(featureOperation, feature.name, feature.uniqueName, inputCommand, featureService.convertFeatureToJSON(feature), user)
//    }

//    def deleteHistoryAsync(String uniqueName) {
//        Promise memberDeleteDeltas = task {
//            deleteHistory(uniqueName)
//        }
//    }


    def deleteHistory(String uniqueName) {
        int count = 0
        getHistory(uniqueName).each { array ->
            array.each {
                it.delete()
                ++count
            }
        }
        return count
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

        Map<String, Map<Long, FeatureEvent>> featureEventMap = extractFeatureEventGroup(uniqueName)

        log.info "setting previous transaction for feature ${uniqueName} -> ${currentIndex}"
        log.info "unique values: ${FeatureEvent.countByUniqueName(uniqueName)} -> ${currentIndex}"
        List<List<FeatureEvent>> featureEventList = getHistory(uniqueName, featureEventMap)
        FeatureEvent currentFeatureEvent = null
        for (int i = 0; i < featureEventList.size(); i++) {
            List<FeatureEvent> featureEventArray = featureEventList.get(i)
            for (FeatureEvent featureEvent in featureEventArray) {
                if (i <= currentIndex) {
                    featureEvent.current = true
                    featureEvent.save()
                    currentFeatureEvent = featureEvent
                    if (i > 0) {
                        if (featureEventList.get(i).size() < featureEventList.get(i - 1).size()) {
                            featureEventList.get(i - 1).find() { it.uniqueName == uniqueName }.each() {
                                it.current = false
                                it.save()
                            }
                        } else {
                            featureEventList.get(i - 1).each() {
                                it.current = false
                                it.save()
                            }
                        }
                    }
                }
            }
        }

        if (!currentFeatureEvent) {
            log.warn "Did we forget to change the feature event?"
//            findCurrentFeatureEvent(uniqueName,featureEventMap)
        }
        setNotPreviousFutureHistoryEvents(currentFeatureEvent, featureEventMap)
        setNotCurrentFutureHistoryEvents(currentFeatureEvent, featureEventMap)

//        log.debug "updated is ${updated}"
        def returnEvent = findCurrentFeatureEvent(currentFeatureEvent.uniqueName, featureEventMap)
        return returnEvent
    }

    def setHistoryState(JSONObject inputObject, int count, boolean confirm) {

        String uniqueName = inputObject.getString(FeatureStringEnum.UNIQUENAME.value)
        log.debug "undo count ${count}"
        if (count < 0) {
            log.warn("Can not undo any further")
            return
        }

//        int total = FeatureEvent.countByUniqueName(uniqueName)
        int total = getHistory(uniqueName).size()
        if (count >= total) {
            log.warn("Can not redo any further")
            return
        }

        Sequence sequence = Feature.findByUniqueName(uniqueName).featureLocation.sequence
        log.debug "sequence: ${sequence}"


        def newUniqueNames = getHistory(uniqueName)[count].collect() {
            it.uniqueName
        }


        deleteCurrentState(inputObject, uniqueName, newUniqueNames, sequence)

        List<FeatureEvent> featureEventArray = setTransactionForFeature(uniqueName, count)
//        log.debug "final feature event: ${featureEvent} ->${featureEvent.operation}"
//        log.debug "current feature events for unique name ${FeatureEvent.countByUniqueNameAndCurrent(uniqueName, true)}"

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
                    log.debug "original command object = ${originalCommandObject as JSON}"
                    log.debug "final command object = ${addCommandObject as JSON}"


                    returnObject = requestHandlingService.addTranscript(addCommandObject)
                    transcriptsToCheckForIsoformOverlap.add(jsonFeature.getString("uniquename"))

                } else {
                    returnObject = requestHandlingService.addFeature(addCommandObject)
                }

//                AnnotationEvent annotationEvent = new AnnotationEvent(
//                        features: returnObject
//                        , sequence: sequence
//                        , operation: AnnotationEvent.Operation.UPDATE
//                )
//
//                requestHandlingService.fireAnnotationEvent(annotationEvent)
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

    def deleteCurrentState(JSONObject inputObject, String uniqueName, List<String> newUniqueNames, Sequence sequence) {

        Map<String, Map<Long, FeatureEvent>> featureEventMap = extractFeatureEventGroup(uniqueName)

        // need to get uniqueNames for EACH current featureEvent
        for (FeatureEvent deleteFeatureEvent in findCurrentFeatureEvent(uniqueName, featureEventMap)) {
            JSONObject deleteCommandObject = new JSONObject()
            JSONArray featuresArray = new JSONArray()
            log.debug "delete feature event uniqueNamee: ${deleteFeatureEvent.uniqueName}"
            JSONObject featureToDelete = new JSONObject()
            featureToDelete.put(FeatureStringEnum.UNIQUENAME.value, deleteFeatureEvent.uniqueName)
            featuresArray.add(featureToDelete)

            log.debug "inputObject ${inputObject as JSON}"
            log.debug "deleteCommandObject ${deleteCommandObject as JSON}"

            if (!deleteCommandObject.containsKey(FeatureStringEnum.TRACK.value)) {
//            for(int i = 0 ; i < featuresArray.size() ; i++){
                deleteCommandObject.put(FeatureStringEnum.TRACK.value, sequence.name)
//            }
            }
            deleteCommandObject.put(FeatureStringEnum.SUPPRESS_HISTORY, true)
            log.debug "final deleteCommandObject ${deleteCommandObject as JSON}"

            deleteCommandObject.put(FeatureStringEnum.FEATURES.value, featuresArray)
            deleteCommandObject = permissionService.copyUserName(inputObject, deleteCommandObject)

            log.debug " final delete JSON ${deleteCommandObject as JSON}"
//            FeatureEvent.withNewTransaction {
            // suppress any events that are not part of the new state
            log.debug "newUniqueNames ${newUniqueNames} vs uniqueName ${uniqueName} vs df-uniqueName ${deleteFeatureEvent.uniqueName}"
            requestHandlingService.deleteFeature(deleteCommandObject)
//            }
            log.debug "deletion sucess . .  "
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
        int index = -1
////        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueName(uniqueName, [sort: "dateCreated", order: "asc"])
//
        index = getDeepestIndex(-1, currentFeatureEvent, featureEventMap)
        return index
//        getDeepestIndex(-1,currentFeatureEvent,featureEventMap)
//        while (currentFeatureEvent) {
//            ++index
//            currentFeatureEvent = currentFeatureEvent.parentId ? findFeatureEventFromMap(currentFeatureEvent.parentId, featureEventMap) : null
//        }
//        return index
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

    def undo(JSONObject inputObject, int countBackwards, boolean confirm) {
        log.info "undoing ${countBackwards}"
        if (countBackwards == 0) {
            log.warn "Undo to the same state"
            return
        }

        String uniqueName = inputObject.get(FeatureStringEnum.UNIQUENAME.value)
        int currentIndex = getCurrentFeatureEventIndex(uniqueName)
        int count = currentIndex - countBackwards
        println "${count} = ${currentIndex}-${countBackwards}"
        setHistoryState(inputObject, count, confirm)
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
        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueNameAndCurrent(uniqueName, true)
        if (featureEventList.size() != 1) {
            log.debug("No current feature events for ${uniqueName}: " + featureEventList.size())
            return null
        }

        FeatureEvent currentFeatureEvent = featureEventList.first()
        int index = getCurrentFeatureEventIndex(uniqueName)

        // we have the "current" . .  we have to go back to see if there are any
        // splits in the history and include those as well
        // its okay if we grab either side of this array
        // just arbitrarily get the first one
        List<List<FeatureEvent>> previousFeatureEvents = findAllPreviousFeatureEvents(currentFeatureEvent, featureEventMap)
        if (!previousFeatureEvents) {
            def futureEvents = findAllFutureFeatureEvents(featureEventList[0], featureEventMap)
            // if we have a future event and it is a merge, then we have multiple "current"
            if (futureEvents && futureEvents.get(0).get(0).parentMergeId) {
                if (futureEvents.get(0).get(0).parentMergeId != currentFeatureEvent.id) {
                    return [currentFeatureEvent, FeatureEvent.findById(futureEvents.get(0).get(0).parentMergeId)]
                } else {
                    return [currentFeatureEvent, FeatureEvent.findById(futureEvents.get(0).get(0).parentId)]
                }
            } else {
                return [currentFeatureEvent]
            }
        }

        // we only really want to show history for either that match
        // its possible that neither one has a matching uniqueName .
        FeatureEvent firstFeatureEvent = previousFeatureEvents[0].find() {
            it.uniqueName == uniqueName
        }
        // example we reverting backwards
        if (!firstFeatureEvent) {
            firstFeatureEvent = previousFeatureEvents[0][0]
        }

        // or index== 0
        if (currentFeatureEvent.id == firstFeatureEvent.id) {
            return [currentFeatureEvent]
        }

        // an index of 1 is 1 in the future.  This returns exclusive future, so we need to
        // substract 1 from the index
        def allFutureEvents = findAllFutureFeatureEvents(firstFeatureEvent, featureEventMap)
//        if(index>allFutureEvents.size()){
//            def futureEvent = allFutureEvents.last()
//            if(futureEvent.first().current){
//                return futureEvent
//            }
//            else{
//                throw new RuntimeException("Problem undoing")
//            }
//        }
//        else{
        def futureEvents = allFutureEvents[index - 1]
        def returnEvents = [currentFeatureEvent]
        futureEvents.each {
            if (it.current && it.id != currentFeatureEvent.id) {
                returnEvents << it
            }
        }

        return returnEvents
//        }
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
        List<FeatureEvent> currentFeatureEvent = findCurrentFeatureEvent(uniqueName, featureEventMap)

        // if we revert a split or do a merge
        if (!currentFeatureEvent) return []

        List<List<FeatureEvent>> featureEvents = new ArrayList<>()

        for (FeatureEvent featureEvent in currentFeatureEvent) {
            def previousFeatureEvent = findAllPreviousFeatureEvents(featureEvent, featureEventMap)
            featureEvents.addAll(previousFeatureEvent)
        }
        featureEvents.add(currentFeatureEvent)
        // finding future events handles splits correctly, so we only need to manage this branch
        for (FeatureEvent featureEvent in currentFeatureEvent) {
            if (featureEvent.uniqueName == uniqueName) {
                featureEvents.addAll(findAllFutureFeatureEvents(featureEvent, featureEventMap))
            }
        }

        // if we have a split, it will pick up the same values twice
        // so we need to filter those out
        // sort by what is on top of what
        // if we have a merge, where one of the merges has history, but the other doesn't
//        def sortedFeatureEvents = featureEvents.sort(false) { a, b ->
////            if(a[0].parentId==null){
////                return -1
////            }
////            if(b[0].parentId==null){
////                return 1
////            }
////            if(b[0].parentId==a[0].childId){
////                return -1
////            }
////            if(a[0].parentId==b[0].childId){
////                return 1
////            }
////            else{
////                println "invalid sort criteria, using date "
//                a[0].dateCreated <=> b[0].dateCreated
////            }
//        }
//        def uniqueFeatureEvents = sortedFeatureEvents.unique(false) { a, b ->

        def sortedFeatureEvents = generatedSortedFeatures(featureEvents,featureEventMap)
//        def sortedFeatureEvents = featureEvents.sort(false) { a, b ->
////            for (aIndex in a) {
////                if(aIndex.parentId==null){
////                    return -1
////                }
////                else{
////                    for (bIndex in b) {
////                        if(bIndex.parentId==null){
////
////                        }
////                    }
////                }
////            if(a[0].parentId==null){
////                return -1
////            }
////            if(b[0].parentId==null){
////                return 1
////            }
////            if(b[0].parentId==a[0].childId){
////                return -1
////            }
////            if(a[0].parentId==b[0].childId){
////                return 1
////            }
////            else{
////                println "invalid sort criteria, using date "
////                a[0].dateCreated <=> b[0].dateCreated
//            }
//        }
        def uniqueFeatureEvents = sortedFeatureEvents.unique(false) { a, b ->
            for (aIndex in a) {
                for (bIndex in b) {
                    if (aIndex.id == bIndex.id) {
                        return 0
                    }
                }
            }
            return 1
//            a[0].id <=> b[0].id
        }
        return uniqueFeatureEvents
    }

    List<List<FeatureEvent>> generatedSortedFeatures(List<List<FeatureEvent>> featureEventList, Map<String, Map<Long, FeatureEvent>> featureEventMap) {
        if(true){
          return featureEventList
        }

//        List<List<FeatureEvent>> returnList = new ArrayList<>()

        // get roots
//        List<List<FeatureEvent>> rootList = getRoots(featureEventList, featureEventMap)

//
//        for(int i = 0 ; i < featureEventList ; i++){
//            List<FeatureEvent> featureEvents = featureEventList.get(i)
//        }
//
//
//        return returnList
    }

//    List<List<FeatureEvent>> getRoots(List<List<FeatureEvent>> lists, Map<String, Map<Long, FeatureEvent>> stringMapMap) {
//        feat
//    }

    JSONObject generateHistory(JSONObject historyContainer, JSONArray featuresArray) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (int i = 0; i < featuresArray.size(); ++i) {
            JSONObject jsonFeature = featuresArray.getJSONObject(i);
            String uniqueName = jsonFeature.get(FeatureStringEnum.UNIQUENAME.value)
            Feature feature = Feature.findByUniqueName(uniqueName)

            JSONArray history = new JSONArray();
            jsonFeature.put(FeatureStringEnum.HISTORY.value, history);
            List<List<FeatureEvent>> transactionList = getHistory(feature.uniqueName)
            for (int j = 0; j < transactionList.size(); ++j) {
                // not sure if this is correct, or if I should just add both?
                List<FeatureEvent> transactionSet = transactionList[j]
                FeatureEvent transaction = transactionSet[0]
                // prefer a predecessor that is not "ADD_TRANSCRIPT"?
                transactionSet.each {
                    if (transaction.operation == FeatureOperation.ADD_TRANSCRIPT) {
                        if (it.operation != FeatureOperation.ADD_TRANSCRIPT) {
                            transaction = it
                        }
                    }
                }
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
                        // TODO: this needs to be functional
//                        if (transaction.getOperation().equals(FeatureOperation.SPLIT_TRANSCRIPT)) {
//                            Feature newFeature = Feature.findByUniqueName(featureJsonObject.getString(FeatureStringEnum.UNIQUENAME.value))
//                            if (overlapperService.overlaps(feature.featureLocation, newFeature.featureLocation, true)) {
//                                historyFeatures.put(featureJsonObject);
//                            }
//                        }
//                        else{
//                            historyFeatures.put(featureJsonObject);
//                        }
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
