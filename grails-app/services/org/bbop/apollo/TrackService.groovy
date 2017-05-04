package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.*
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class TrackService {


    public static String TRACK_NAME_SPLITTER = "::"

    def projectionService
    def trackMapperService

    String getTracks(User user, Organism organism) {
        String trackList = ""
        for (UserPermission userPermission in UserPermission.findAllByUserAndOrganism(user, organism)) {
            trackList += userPermission.trackNames // TODO: add properly
        }
        for (UserGroup userGroup in user.userGroups) {
            trackList += getTracks(userGroup, organism)
        }
        return trackList
    }

    String getTracks(UserGroup group, Organism organism) {
        String trackList = ""
        JSONArray jsonArray = new JSONArray()
        for (GroupPermission groupPermission in GroupPermission.findAllByGroupAndOrganism(group, organism)) {
            trackList += "||" + groupPermission.trackNames // TODO: add properly
        }
        return trackList.trim()
    }

    String getTrackPermissions(UserGroup userGroup, Organism organism) {
        JSONArray jsonArray = new JSONArray()
        for (GroupPermission groupPermission in GroupPermission.findAllByGroupAndOrganism(userGroup, organism)) {
            jsonArray.add(groupPermission as JSON)
        }
        return jsonArray.toString()
    }

    String getTrackPermissions(User user, Organism organism) {
        JSONArray jsonArray = new JSONArray()
        for (UserPermission userPermission in UserPermission.findAllByUserAndOrganism(user, organism)) {
            jsonArray.add(userPermission as JSON)
        }
        String returnString = jsonArray.toString()
        for (UserGroup userGroup in user.userGroups) {
            returnString += getTrackPermissions(userGroup, organism)
        }
        return returnString
    }

    static Map<String, Boolean> mergeTrackVisibilityMaps(Map<String, Boolean> mapA, Map<String, Boolean> mapB) {
        Map<String, Boolean> returnMap = new HashMap<>()
        mapA.keySet().each { it ->
            returnMap.put(it, mapA.get(it))
        }

        mapB.keySet().each { it ->
            if (returnMap.containsKey(it)) {
                returnMap.put(it, returnMap.get(it) || mapB.get(it))
            } else {
                returnMap.put(it, mapB.get(it))
            }
        }
        return returnMap
    }

    public Map<String, Boolean> getTracksVisibleForOrganismAndGroup(Organism organism, UserGroup userGroup) {
        Map<String, Boolean> trackVisibilityMap = new HashMap<>()

        List<GroupTrackPermission> groupPermissions = GroupTrackPermission.findAllByOrganismAndGroup(organism, userGroup)
        for (GroupTrackPermission groupPermission in groupPermissions) {
            JSONObject jsonObject = JSON.parse(groupPermission.trackVisibilities) as JSONObject

            // this should make it default to true if a true is ever given
            jsonObject.keySet().each {
                Boolean visible = trackVisibilityMap.get(it)
                // if null or false, can over-ride to true
                if (!visible) {
                    trackVisibilityMap.put(it, jsonObject.get(it))
                }
            }
        }

        return trackVisibilityMap
    }

    /**
     *
     * * @param trackVisibilityMap  Map of track names and visibility.
     * @param user
     * @param organism
     */
    public void setTracksVisibleForOrganismAndUser(Map<String, Boolean> trackVisibilityMap, Organism organism, User user) {
        UserTrackPermission userTrackPermission = UserTrackPermission.findByOrganismAndUser(organism, user)
        String jsonString = convertHashMapToJsonString(trackVisibilityMap)
        if (!userTrackPermission) {
            userTrackPermission = new UserTrackPermission(
                    user: user
                    , organism: organism
                    , trackVisibilities: jsonString
            ).save(insert: true)
        } else {
            userTrackPermission.trackVisibilities = jsonString
            userTrackPermission.save()
        }
    }

    /**
     * *
     * * @param trackVisibilityMap  Map of track names and visibility.
     * @param group
     * @param organism
     */
    public void setTracksVisibleForOrganismAndGroup(Map<String, Boolean> trackVisibilityMap, Organism organism, UserGroup group) {

        GroupTrackPermission groupTrackPermission = GroupTrackPermission.findByOrganismAndGroup(organism, group)
        String jsonString = convertHashMapToJsonString(trackVisibilityMap)
        if (!groupTrackPermission) {
            groupTrackPermission = new GroupTrackPermission(
                    group: group
                    , organism: organism
                    , trackVisibilities: jsonString
            ).save(insert: true)
        } else {
            groupTrackPermission.trackVisibilities = jsonString
            groupTrackPermission.save()
        }


    }

    public Map<String, Boolean> getTracksVisibleForOrganismAndUser(Organism organism, User user) {
        Map<String, Boolean> trackVisibilityMap = new HashMap<>()

        List<UserTrackPermission> userPermissionList = UserTrackPermission.findAllByOrganismAndUser(organism, user)
        for (UserTrackPermission userPermission in userPermissionList) {
            JSONObject jsonObject = JSON.parse(userPermission.trackVisibilities) as JSONObject

            jsonObject.keySet().each {
                Boolean visible = trackVisibilityMap.get(it)
                // if null or false, can over-ride to true
                if (!visible) {
                    trackVisibilityMap.put(it, jsonObject.get(it))
                }
            }
        }

        for (UserGroup group in user.userGroups) {
            Map<String, Boolean> specificMap = getTracksVisibleForOrganismAndGroup(organism, group)
            trackVisibilityMap = mergeTrackVisibilityMaps(specificMap, trackVisibilityMap)
        }

        return trackVisibilityMap
    }


    private String convertHashMapToJsonString(Map map) {
        JSONObject jsonObject = new JSONObject()
        map.keySet().each {
            jsonObject.put(it, map.get(it))
        }
        return jsonObject.toString()
    }


    def String getSequencePathName(String inputName) {
        if (inputName.contains("/")) {
            String[] tokens = inputName.split("/")
            return tokens.length >= 2 ? tokens[tokens.length - 2] : null
        }
        return null
    }

    // replace index - 2 with sequenceName
    @NotTransactional
    String generateTrackNameForSequence(String inputName, String sequenceName) {
        if (inputName.contains("/")) {
            String[] tokens = inputName.split("/")
            // the sequenceString path should be the second to the last one
            if (tokens.length >= 2) {
                if (sequenceName.contains("{")) {
                    sequenceName = getSequenceNameFromJsonString(sequenceName)
                }
                tokens[tokens.length - 2] = sequenceName
                return tokens.join("/")
            }
        }
        return null
    }

    @NotTransactional
    String getSequenceNameFromJsonString(String inputString) {
        if (inputString.contains("{")) {
            JSONObject jsonObject = JSON.parse(inputString) as JSONObject
            return jsonObject.name
        } else {
            return inputString
        }
    }

    String getTrackPathName(String inputName) {
        if (inputName.contains("/")) {
            String[] tokens = inputName.split("/")
            // the sequenceString path should be the second to the last one
            return tokens.length >= 3 ? tokens[tokens.length - 3] : null
        }
        return null
    }

    JSONArray loadChunkData(String path, String refererLoc, Organism currentOrganism, Integer offset, String trackName) {
        println "loading chunk data with offset ${offset}"
        File file = new File(path)
        String inputText = file.text
        JSONArray coordinateJsonArray = new JSONArray(inputText)
        String sequenceName = projectionService.getSequenceName(file.absolutePath)
        // get the track from the json object

        // TODO: it should look up the OGS track either default or variable
        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, currentOrganism)

        if (projection && projection.containsSequence(sequenceName, currentOrganism.commonName)) {
            ProjectionSequence projectionSequence = projection.getProjectionSequence(sequenceName, currentOrganism.commonName)
            println "found a projection ${projection.size()}"
            for (int i = 0; i < coordinateJsonArray.size(); i++) {
                JSONArray coordinate = coordinateJsonArray.getJSONArray(i)
                projectJsonArray(projection, coordinate, offset, projectionSequence, trackName)
            }
        }
        // at this point do a sanity check on the projected coordinateJsonArray
        sanitizeCoordinateArray(coordinateJsonArray, currentOrganism, trackName)

        return coordinateJsonArray
    }

    JSONObject loadTrackData(String path, String refererLoc, Organism currentOrganism) {
        File file = new File(path)
        if (!file.exists()) {
            log.warn("File ${file.absolutePath} is not present.")
            return null
        }
        String inputText = file.text
        JSONObject trackDataJsonObject = new JSONObject(inputText)
        String sequenceName = projectionService.getSequenceName(file.absolutePath)
        // get the track from the json object

        // TODO: it should look up the OGS track either default or variable
        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, currentOrganism)
        ProjectionSequence projectionSequence = projection.getProjectionSequence(sequenceName, currentOrganism.commonName)

        if (projection && projectionSequence) {
            println "found a projection ${projection.size()}"
            JSONObject intervalsJsonArray = trackDataJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value)

            // get track for service
            JSONArray trackClassesArray = intervalsJsonArray.getJSONArray("classes")
            String trackName = projectionService.getTrackName(file.absolutePath)
            trackMapperService.storeTrack(currentOrganism.commonName, trackName, trackClassesArray)

            JSONArray coordinateJsonArray = intervalsJsonArray.getJSONArray(FeatureStringEnum.NCLIST.value)
            // TODO: prune tracks if sequenceList / start / end exist
            for (int i = 0; i < coordinateJsonArray.size(); i++) {
                JSONArray coordinate = coordinateJsonArray.getJSONArray(i)
                projectJsonArray(projection, coordinate, 0, projectionSequence, trackName)
            }
            // at this point do a sanity check on the projected coordinateJsonArray
            sanitizeCoordinateArray(coordinateJsonArray, currentOrganism, trackName)

        }

        return trackDataJsonObject
    }

    /**
     * Method traverses, recursively, through a given coordinateJsonArray
     * checks if the coordinates of top-level feature and sub-features are valid
     * and eliminates those that are invalid by modifying the input coordinateJsonArray
     *
     * @param coordinateJsonArray
     * @param currentOrganism
     * @param trackName
     */
    protected JSONArray sanitizeCoordinateArray(JSONArray coordinateJsonArray, Organism currentOrganism, String trackName) {
        for (int i = 0; i < coordinateJsonArray.size(); i++) {
            JSONArray coordinateArray = coordinateJsonArray.getJSONArray(i)
            TrackIndex trackIndex = trackMapperService.getIndices(currentOrganism.commonName, trackName, coordinateArray.getInt(0))
            if (coordinateArray.size() == 4) {
                // [4,19636,588668,1]
                if (coordinateArray.getInt(trackIndex.start) == -1 && coordinateArray.getInt(trackIndex.end) == -1) {
                    coordinateJsonArray.remove(coordinateArray)
                    --i
                }
            } else {
                if (coordinateArray.getInt(trackIndex.start) == -1 && coordinateArray.getInt(trackIndex.end) == -1) {
                    // eliminate coordinate array since top level feature has -1 coordinates
                    if (coordinateArray.size() > 10 && coordinateArray.get(coordinateArray.size() - 1) instanceof JSONObject && coordinateArray.getJSONObject(coordinateArray.size() - 1).containsKey("Sublist")) {
                        // coordinateArray has subList and has the same form as that of the coordinateJsonArray which enables recursion
                        JSONArray sanitizedSubListArray = sanitizeCoordinateArray(coordinateArray.getJSONObject(coordinateArray.size() - 1).getJSONArray("Sublist"), currentOrganism, trackName)
                        // we know we want to remove this regardless
                        coordinateJsonArray.remove(coordinateArray)
                        --i
                        // if there is a valid subarray, we pull that to the top
                        if (sanitizedSubListArray) {
                            for (int subIndex = 0; subIndex < sanitizedSubListArray.size(); ++subIndex) {
                                JSONArray validSubArray = sanitizedSubListArray.getJSONArray(subIndex)
                                if (validSubArray.getInt(trackIndex.start) != -1 && validSubArray.getInt(trackIndex.end) != -1) {
                                    ++i
                                    coordinateJsonArray.add(i, validSubArray)
                                }
                            }
                        }
                    } else {
                        coordinateJsonArray.remove(coordinateArray)
                        --i
                    }
                } else {
                    if (trackIndex.hasSubFeatures() && coordinateArray.get(trackIndex.subFeaturesColumn)) {
                        // coordinateArray has subFeaturesColumn
                        JSONArray subFeaturesArray = coordinateArray.getJSONArray(trackIndex.subFeaturesColumn)
                        for (int j = 0; j < subFeaturesArray.size(); j++) {
                            JSONArray subFeature = subFeaturesArray.getJSONArray(j)
                            TrackIndex subFeatureTrackIndex = trackMapperService.getIndices(currentOrganism.commonName, trackName, subFeature.getInt(0))
                            if (subFeature.getInt(subFeatureTrackIndex.start) == -1 && subFeature.getInt(subFeatureTrackIndex.end) == -1) {
                                // eliminate sub feature from subFeaturesColumn of coordinate array
                                coordinateJsonArray.getJSONArray(i).getJSONArray(trackIndex.subFeaturesColumn).remove(subFeature)
                            }
                        }
                    }
                    // TODO: TrackIndex doesn't return the proper value when a coordinateArray has a subList
                    //if (trackIndex.hasSubList() && coordinateArray.get(trackIndex.sublistColumn)) {
                    if (coordinateArray.size() == 12) {
                        // coordinateArray has subList and has the same form as that of the coordinateJsonArray which enables recursion
                        JSONArray sanitizedSubListArray = sanitizeCoordinateArray(coordinateArray.getJSONObject(11).getJSONArray("Sublist"), currentOrganism, trackName)
                        coordinateArray.getJSONObject(11).put("Sublist", sanitizedSubListArray)
                    }
                }
            }
        }

        return coordinateJsonArray
    }

    /**
     * @param projection What is used to do the projection
     * @param coordinate JSONArray to project
     * @param offset Offset of the coordinate (typically due to chunking)
     * @param adjustment This is typically the offset from the prior sequenceString.
     * @return
     */
    JSONArray projectJsonArray(MultiSequenceProjection projection, JSONArray coordinate, Integer offset, ProjectionSequence projectionSequence, String trackName) {

        // TODO: prune out areas obviously invalid (i.e., pre-sanitize)

        // see if there are any subarrays of size >4 where the first one is a number 0-5 and do the same  . . .
        for (int subIndex = 0; subIndex < coordinate.size(); ++subIndex) {
            def subArray = coordinate.get(subIndex)
            if (subArray instanceof JSONArray) {
                projectJsonArray(projection, subArray, offset, projectionSequence, trackName)
            }
            if (subArray instanceof JSONObject && subArray.containsKey("Sublist")) {
                def subArrays2 = subArray.getJSONArray("Sublist")
                projectJsonArray(projection, subArrays2, offset, projectionSequence, trackName)
            }
        }



        if (coordinate.size() >= 3
                && coordinate.get(0) instanceof Integer
//                && coordinate.get(1) instanceof Integer
//                && coordinate.get(2) instanceof Integer
        ) {
            // the problem is that the projectionSequence is incorrect, though its the same name


            TrackIndex trackIndex = trackMapperService.getIndices(projectionSequence.organism, trackName, coordinate.getInt(0))
            // TODO: uncomment this to get 408011 to work and set the projectionSequence.offset to 0
//            List<ProjectionSequence> projectionSequenceList = projection.getProjectionSequences(coordinate.getInt(trackIndex.getStart()),coordinate.getInt(trackIndex.getEnd()))
//            if(projectionSequenceList){
//                projectionSequence = projectionSequenceList.first()
////                offset = projectionSequence.offset
//            }

            Integer oldMin = coordinate.getInt(trackIndex.start) + projectionSequence.originalOffset
            Integer oldMax = coordinate.getInt(trackIndex.end) + projectionSequence.originalOffset
            assert oldMin <= oldMax

            long originalOffsetStart = projectionSequence.originalOffsetStart
            long originalOffsetEnd = projectionSequence.originalOffsetEnd
            long projectedOffset = projectionSequence.projectedOffset
            boolean reverse = projectionSequence.reverse

            Boolean useOffset = true
            // TODO: evaluate if this piece of code is correct
            if (oldMin > projectionSequence.originalOffsetStart + projectionSequence.start || oldMax < projectionSequence.originalOffsetStart + projectionSequence.end) {
                List<ProjectionSequence> projectionSequenceList = projection.getProjectionSequences(oldMin, oldMax)
                if (projectionSequenceList) {
                    def firstProjectionSequence = projectionSequenceList.first()
                    def lastProjectionSequence = projectionSequenceList.last()
                    originalOffsetStart = firstProjectionSequence.originalOffsetStart
                    originalOffsetEnd = lastProjectionSequence.originalOffsetEnd
                    projectedOffset = firstProjectionSequence.projectedOffset
                    reverse = firstProjectionSequence.reverse
                    useOffset = false
                }
            }

            // case 1: coordinates encompass the projection, so both the LHS and RHS will be bound (and partial)
            if (oldMin < originalOffsetStart && oldMax > originalOffsetEnd) {
                oldMin = originalOffsetStart
                oldMax = originalOffsetEnd
            } else
            // case 2: coordinates span the LHS (and are thus partial)
            if (oldMin < originalOffsetStart && oldMax > originalOffsetStart && oldMax < originalOffsetEnd) {
                oldMax = originalOffsetEnd
            } else
            // case 3: coordinates span the RHS of the projection (and are thus partial)
            if (oldMin > originalOffsetStart && oldMin < originalOffsetEnd && oldMax > originalOffsetEnd) {
                oldMax = originalOffsetStart
            }
            // else do nothing, and they will be mapped internally
            // case 4: coordinates are outside the scope of the projection, so no processing
            // case 5: coordinates are internal to the projection, so no processing

            Coordinate newCoordinate = projection.projectCoordinate(oldMin, oldMax - 1)
            if (newCoordinate && newCoordinate.isValid()) {
                if (useOffset) {
                    coordinate.set(trackIndex.start, newCoordinate.min + offset - projectedOffset)
                    coordinate.set(trackIndex.end, newCoordinate.max + offset - projectedOffset + 1)
                } else {
                    coordinate.set(trackIndex.start, newCoordinate.min + offset)
                    coordinate.set(trackIndex.end, newCoordinate.max + offset + 1)
                }
                if (reverse) {
                    int temp = coordinate.get(trackIndex.start)
                    if (trackIndex.strand > 0) {
                        int newStrand = org.bbop.apollo.sequence.Strand.getStrandForValue(coordinate.get(trackIndex.strand)).reverse().value
                        coordinate.set(trackIndex.strand, newStrand)
                    }
                    coordinate.set(trackIndex.start, coordinate.get(trackIndex.end))
                    coordinate.set(trackIndex.end, temp)
                }
            } else {
                // this is valid for very small areas
                log.debug("Invalid mapping of coordinate ${coordinate} -> ${newCoordinate}")
                coordinate.set(trackIndex.start, -1)
                coordinate.set(trackIndex.end, -1)
            }

        }

        return coordinate
    }

    /**
     * merge trackData.json objects from different sequenceString sources . . .
     *
     * 1 - assume already projected
     * 2 - assume in the correct order
     * @param mergeTrackObject
     * @return
     */
    JSONObject mergeTrackObject(Map<String, JSONObject> trackList, MultiSequenceProjection multiSequenceProjection, Organism organism, String trackName) {

        JSONObject finalObject = null
        int endSize = 0
        multiSequenceProjection.projectionChunkList.projectionChunkList.each { chunk ->
            String chunkSequenceName = chunk.sequence.contains("{") ? JSON.parse(chunk.sequence).name : chunk.sequence
            JSONObject jsonObject = trackList.get(chunkSequenceName)
            if (finalObject == null) {
                finalObject = jsonObject
                // get endSize  6
                endSize = jsonObject.intervals.maxEnd
            } else if (!hasOverlappingNcList(finalObject, jsonObject, organism.commonName, trackName)) {
                // ignore formatVersion
                // add featureCount
                finalObject.featureCount = finalObject.featureCount + jsonObject.featureCount

                // somehow add histograms together
                finalObject.histograms = mergeHistograms(finalObject.histograms, jsonObject.histograms)

                // add intervals together starting at end and adding
                finalObject.intervals = mergeIntervals(finalObject.intervals, jsonObject.intervals, chunk.sequenceOffset, organism, trackName)

                // get endSize
                endSize += jsonObject.intervals.maxEnd
            }
        }


        return finalObject
    }

    /**
     * Indicates whether the objects have overlapping nclist objects based on name.
     *
     *
     * Any overlap indicates true.
     *
     * @param intervalsObjectA
     * @param intervalsObjectB
     * @return
     */
    @NotTransactional
    Boolean hasOverlappingNcList(JSONObject intervalsObjectA, JSONObject intervalsObjectB, String organismName, String trackName) {
        JSONArray nclistA = intervalsObjectA.intervals.nclist
        JSONArray nclistB = intervalsObjectB.intervals.nclist

        for (JSONArray ncListEntryA in nclistA) {
            for (JSONArray ncListEntryB in nclistB) {
                if (ncListEntryA.getInt(0) == ncListEntryB.getInt(0)) {
                    int classIndex = ncListEntryA.getInt(0)
                    TrackIndex trackIndex = trackMapperService.getIndices(organismName, trackName, classIndex)
                    println "trackIndex ${trackIndex as JSON} trackNAme ${trackName} and class index ${classIndex} and organism ${organismName}"
                    if (trackIndex.id && ncListEntryA.getString(trackIndex.id) == ncListEntryB.getString(trackIndex.id)) {
                        return true
                    }
                }
            }
        }

//        for(a in nclistA){
//
//        }

        return false

    }
/**
 * "histograms": * {* "stats":
 * [{"max": 4,
 "basesPerBin": "200000",
 "mean": 3}],
 "meta":
 [{"arrayParams": {"chunkSize": 10000,
 "length": 3,
 "urlTemplate": "hist-200000-{Chunk}.json"},
 "basesPerBin": "200000"}]}* @param o
 * @param o
 */
    JSONObject mergeHistograms(JSONObject first, JSONObject second) {
        // for stats . . just always take the higher between the two
        JSONObject firstStat = first.getJSONArray("stats").getJSONObject(0)
        JSONObject secondStat = second.getJSONArray("stats").getJSONObject(0)
        Integer firstMax = firstStat.getInt("max")
        Integer secondMax = secondStat.getInt("max")
        Integer firstMean = firstStat.getInt("mean")
        Integer secondMean = secondStat.getInt("mean")
        firstStat.put("max", Math.max(firstMax, secondMax))
        firstStat.put("max", Math.max(firstMax, secondMax))
        // not exactly right . . but would need to be re-aculcated thought evertying likely otherwise
        firstStat.put("mean", Math.max(firstMean, secondMean))
        firstStat.put("mean", Math.max(firstMean, secondMean))

        // for meta . . add length, but everything else should stay the same

        JSONObject firstMeta = first.getJSONArray("meta").getJSONObject(0)
        JSONObject firstArrayParams = firstMeta.getJSONObject("arrayParams")
        JSONObject secondMeta = second.getJSONArray("meta").getJSONObject(0)
        JSONObject secondArrayParams = secondMeta.getJSONObject("arrayParams")
        firstArrayParams.put("length", Math.max(firstArrayParams.getInt("length"), secondArrayParams.getInt("length")))

        return first
    }

    @NotTransactional
    JSONArray nudgeNcListArray(JSONArray coordinate, Long nudgeAmount, Long nudgeIndex, String organismName, String trackName) {
        // see if there are any subarrays of size >4 where the first one is a number 0-5 and do the same  . . .
        for (int subIndex = 0; subIndex < coordinate.size(); ++subIndex) {
            def subArray = coordinate.get(subIndex)
            if (subArray instanceof JSONArray) {
                nudgeNcListArray(subArray, nudgeAmount, nudgeIndex, organismName, trackName)
            } else if (subArray instanceof JSONObject && subArray.containsKey("Sublist")) {
                JSONArray subArray2 = subArray.getJSONArray("Sublist")
                nudgeNcListArray(subArray2, nudgeAmount, nudgeIndex, organismName, trackName)
            }
        }

        if (coordinate.size() > 3
                && coordinate.get(0) instanceof Number
                && coordinate.get(1) instanceof Number
                && coordinate.get(2) instanceof Number
//                && coordinate.get(3) instanceof Integer
        ) {
            TrackIndex trackIndex = trackMapperService.getIndices(organismName, trackName, coordinate.getInt(0))
            coordinate.set(trackIndex.getStart(), coordinate.getInt(trackIndex.getStart()) + nudgeAmount)
            coordinate.set(trackIndex.getEnd(), coordinate.getInt(trackIndex.getEnd()) + nudgeAmount)
//            if(coordinate.get(0)==4){
            if (trackIndex.hasChunk()) {
//                trackIndex.sublistColumn
                // sublist column is the last one?
                Integer arrayIndex = coordinate.size() - 1
                coordinate.set(arrayIndex, coordinate.getInt(arrayIndex) + nudgeIndex)
            }
        }

        return coordinate
    }

    /**
     * count,
     * minStart,(add from the end of the previous one [endSize])
     * maxEnd, (add endSize to this like minStart)
     * lazyClass (just always take the largest between the two)
     * nclist (append the arrays . . . but add the previous endSize)
     * @param first
     * @param second
     * @return
     */
    JSONObject mergeIntervals(JSONObject first, JSONObject second, Long endSize, Organism organism, String trackName) {
        first.put("minStart", first.getInt("minStart") + endSize)
        first.put("maxEnd", first.getInt("maxEnd") + endSize)
        first.put("count", first.getInt("count") + second.getInt("count"))

        // we'll assume that the first and second are consistent ..
        // except that sometimes there is more in onne than the other
        // and we should tkae the largest between the two
        JSONArray firstClassesArray = first.getJSONArray("classes")
        JSONArray secondClassesArray = second.getJSONArray("classes")
        if (secondClassesArray.size() > firstClassesArray.size()) {
            first.put("classes", secondClassesArray)
            first.put("lazyClass", second.getInt("lazyClass"))
        }

        // add the second to the first with endSize added
        JSONArray firstNcListArray = first.getJSONArray("nclist")
        JSONArray secondNcListArray = second.getJSONArray("nclist")


        mergeCoordinateArray(firstNcListArray, secondNcListArray, endSize, organism.commonName, trackName)

        return first
    }

    /**
     * This is a destructive method .. . putting everythin on the second onto the first with an alignment
     * @param firstNcListArray
     * @param secondNcListArray
     * @return
     */
    JSONArray mergeCoordinateArray(JSONArray firstNcListArray, JSONArray secondNcListArray, Long endSize, String organismName, String trackName) {
        int nudgeIndex = firstNcListArray.size()
        for (int i = 0; i < secondNcListArray.size(); i++) {
            def ncListArray = secondNcListArray.get(i)
            if (ncListArray instanceof JSONArray) {
                if (firstNcListArray != secondNcListArray) {
                    nudgeNcListArray(ncListArray, endSize, nudgeIndex, organismName, trackName)
                    firstNcListArray.add(ncListArray)
                }
            }
        }
        return firstNcListArray
    }

    JSONArray mergeCoordinateArray(ArrayList<JSONArray> jsonArrays, List<Integer> endSizes, String organismName, String trackName) {

        JSONArray firstNcListArray = jsonArrays.first()

        for (int i = 1; i < jsonArrays.size(); i++) {
            JSONArray secondArray = jsonArrays.get(i)
            mergeCoordinateArray(firstNcListArray, secondArray, endSizes.get(i - 1), organismName, trackName)
        }

        return firstNcListArray

    }

    @NotTransactional
    String extractLocation(String referer) {
        int startIndex = referer.indexOf("?loc=")
        int endIndex = referer.contains("&") ? referer.indexOf("&") : referer.length()
        String refererLoc = referer.subSequence(startIndex + 5, endIndex)
        refererLoc = URLDecoder.decode(refererLoc, "UTF-8")
        return refererLoc
    }

    JSONObject projectTrackData(JSONArray sequenceArray, String dataFileName, String refererLoc, Organism currentOrganism) {
        Map<String, JSONObject> trackObjectList = new HashMap<>()
        ProjectionChunkList projectionChunkList = new ProjectionChunkList()
        MultiSequenceProjection multiSequenceProjection = projectionService.getProjection(refererLoc, currentOrganism)

        // can probably store the projection chunks
        Integer priorSequenceLength = 0
        Integer priorChunkArrayOffset = 0
        String trackName = null

        // a sequence name
        Map<String, Sequence> sequenceEntryMaps = Sequence.findAllByNameInListAndOrganism(sequenceArray.name as List, currentOrganism).collectEntries() {
            [it.name, it]
        }

        int calculatedEnd = 0
        int calculatedStart = 0
        Map<JSONObject, Long> sequenceMap = new HashMap<>()
        Long fullSequenceId = null
        Integer previousFullSequenceOffset = 0 // we store this until we need it
        Integer fullSequenceOffset = 0

        if (refererLoc.contains(FeatureStringEnum.SEQUENCE_LIST.value)) {
            for (int i = 0; i < sequenceArray.size(); i++) {
                def sequenceObject = sequenceArray.getJSONObject(i)
                Sequence sequence = sequenceEntryMaps.get(sequenceObject.name)
                // add the offset if not the same one, then we now use the offset
                if (sequence.id != fullSequenceId) {
                    fullSequenceOffset += previousFullSequenceOffset
                }
                sequenceObject.start = sequenceObject.start ?: sequence.start
                sequenceObject.end = sequenceObject.end ?: sequence.end
                Integer sequenceStart = sequenceObject.reverse ? sequenceObject.end : sequenceObject.start
                Integer sequenceEnd = sequenceObject.reverse ? sequenceObject.start : sequenceObject.end
                Integer projectedEnd = multiSequenceProjection.projectValue(sequenceEnd+ fullSequenceOffset, fullSequenceOffset, 0)
                Integer projectedStart = multiSequenceProjection.projectValue(sequenceStart+ fullSequenceOffset, fullSequenceOffset, 0)
                calculatedStart += projectedStart
                calculatedEnd += projectedEnd
                JSONObject storeObject = new JSONObject(
                        start: sequenceObject.start
                        , end: sequenceObject.end
                        , name: sequenceObject.name
                )
                sequenceMap.put(storeObject, projectedEnd)

                // if the sequence had changed or is the initial sequence, then we should add the previous offset
                if (sequence.id != fullSequenceId) {
                    // if this is the initial
//                    fullSequenceOffset += sequence.length
//                    fullSequenceId = sequence.id
//                    if(fullSequenceId==null){
//                    fullSequenceOffset = 0
                    fullSequenceId = sequence.id
                    previousFullSequenceOffset = sequence.length
//                    }
//                    else{
//                        fullSequenceOffset += previousFullSequenceOffset
//                        fullSequenceId = sequence.id
//                    }
                }
            }
        } else {
            for (Sequence sequence in sequenceEntryMaps.values()) {
                JSONObject storeObject = new JSONObject(
                        start: sequence.start
                        , end: sequence.end
                        , name: sequence.name
                )
                sequenceMap.put(storeObject, sequence.length)
            }
        }

        Map<JSONObject,ProjectionChunk> previousTrackMap = new HashMap<>()
        for (JSONObject sequenceArrayObject in sequenceArray) {
            ProjectionChunk projectionChunk = new ProjectionChunk(
                    sequence: sequenceArrayObject,
            )
            String sequencePathName = generateTrackNameForSequence(dataFileName, sequenceArrayObject.name)
            trackName = getTrackPathName(sequencePathName)

            // this loads PROJECTED
            JSONObject trackObject = loadTrackData(sequencePathName, refererLoc, currentOrganism)
            if (trackObject) {
                JSONObject intervalsObject = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value)
                JSONArray ncListArray = intervalsObject.getJSONArray(FeatureStringEnum.NCLIST.value)
                if (ncListArray && ncListArray[0].size() == 4) {
                    println "nclist array ${ncListArray}"
                }
                trackMapperService.storeTrack(currentOrganism.commonName, trackName, intervalsObject.getJSONArray("classes"))
                Integer lastLength = 0
                Integer lastChunkArrayOffset = 0
                JSONObject sequenceKey = sequenceMap.keySet().find() {
                    it.name == sequenceArrayObject.name && it.start == sequenceArrayObject.start && it.end == sequenceArrayObject.end
                }
                Integer sequenceLength = sequenceMap.get(sequenceKey)
                for (int i = 0; i < ncListArray.size(); i++) {
                    JSONArray internalArray = ncListArray.getJSONArray(i)
                    TrackIndex trackIndex = trackMapperService.getIndices(currentOrganism.commonName, trackName, internalArray.getInt(0))
                    if (trackIndex.hasChunk()) {
                        projectionChunk.addChunk()
                        Integer chunkIndex = trackIndex.getChunk()
                        Integer chunkID = internalArray.getInt(chunkIndex)
                        projectionChunk.setChunkID(chunkID)
                    }
                    // only take if its greater
                    lastLength = sequenceLength > lastLength ? sequenceLength : lastLength

                    ++lastChunkArrayOffset
                }

                projectionChunk.chunkArrayOffset = priorChunkArrayOffset
                projectionChunk.sequenceOffset = priorSequenceLength

                priorSequenceLength = priorSequenceLength + lastLength
                priorChunkArrayOffset = priorChunkArrayOffset + lastChunkArrayOffset

                // if we are projecting the same object, we don't a
                if(previousTrackMap.containsKey(trackObject)){
                    --priorChunkArrayOffset
//                    ProjectionChunk previousChunk = previousTrackMap.get(trackObject)
//                    previousChunk.chunkArrayOffset = projectionChunk.chunkArrayOffset
//                    previousChunk.sequenceOffset = projectionChunk.sequenceOffset
                }
                else{
                    projectionChunkList.addChunk(projectionChunk)
                }

                trackObjectList.put(sequenceArrayObject.name, trackObject)
                previousTrackMap.put(trackObject,projectionChunk)
            }
        }

        multiSequenceProjection.projectionChunkList = projectionChunkList
        projectionService.cacheProjection(refererLoc, multiSequenceProjection)

        JSONObject trackObject = mergeTrackObject(trackObjectList, multiSequenceProjection, currentOrganism, trackName)

        if (trackObject) {
            if (refererLoc.contains(FeatureStringEnum.SEQUENCE_LIST.value)) {
                trackObject.intervals.minStart = calculatedStart
                trackObject.intervals.maxEnd = calculatedEnd
            } else {
                trackObject.intervals.minStart = multiSequenceProjection.projectValue(trackObject.intervals.minStart)
                trackObject.intervals.maxEnd = multiSequenceProjection.projectValue(trackObject.intervals.maxEnd)
            }

            ProjectionSequence projectionSequenceStart = multiSequenceProjection.getProjectionSequence(trackObject.intervals.minStart)
            ProjectionSequence projectionSequenceEnd = multiSequenceProjection.getProjectionSequence(trackObject.intervals.maxEnd)
            if (projectionSequenceStart?.reverse == projectionSequenceEnd?.reverse) {
                int temp = trackObject.intervals.minStart
                trackObject.intervals.minStart = trackObject.intervals.maxEnd
                trackObject.intervals.maxEnd = temp
            }

            // sort nclist top-level
            JSONObject intervalsObject = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value)
            JSONArray ncListArray = intervalsObject.getJSONArray(FeatureStringEnum.NCLIST.value)
            // sort by column 1
            JSONArray replacementNcListArray = sortNcListArray(ncListArray)

            intervalsObject.put(FeatureStringEnum.NCLIST.value, replacementNcListArray)
            trackObject.put(FeatureStringEnum.INTERVALS.value, intervalsObject)
        }


        return trackObject

    }

    def sortNcListArray(JSONArray ncListArray) {
        JSONArray replacementNcListArray = new JSONArray()
        for (JSONArray array in ncListArray) {
            int index = 0
            for (def replacementArray in replacementNcListArray) {
                if (replacementArray instanceof JSONArray) {
                    if (replacementArray[1] < array[1]) {
                        ++index
                    }
                }
            }
            replacementNcListArray.add(index, array)
        }
        return replacementNcListArray
    }
/**
 * Get chunk index for files of the pattern: lf-${X}.json
 * @param fileName
 * @return
 */
    private static Integer getChunkIndex(String fileName) {
        String finalString = fileName.substring(3, fileName.length() - 5)
        return Integer.parseInt(finalString)
    }

    JSONArray projectTrackChunk(String fileName, String dataFileName, String refererLoc, Organism currentOrganism, String trackName) {
        List<JSONArray> trackArrayList = new ArrayList<>()
        List<Integer> sequenceLengths = []

        MultiSequenceProjection multiSequenceProjection = projectionService.getCachedProjection(refererLoc)
        multiSequenceProjection = multiSequenceProjection ?: projectionService.getProjection(refererLoc, currentOrganism)

        Integer chunkIndex = getChunkIndex(fileName)
        ProjectionChunk projectionChunk = multiSequenceProjection.projectionChunkList.findProjectChunkForIndex(chunkIndex)
        println "chunk index: ${chunkIndex}"
        String sequenceString = projectionChunk.sequence
        Integer sequenceOffset = projectionChunk.sequenceOffset
        // calculate offset for chunk and replace the filename
        // should be the start of the string
        String originalFileName = "lf-${chunkIndex - projectionChunk.chunkArrayOffset}.json"
        dataFileName = dataFileName.replaceAll(fileName, originalFileName)

        // next we want to load tracks from the REAL paths . .  .
        String sequencePathName = generateTrackNameForSequence(dataFileName, sequenceString)
        // this loads PROJECTED
        JSONArray coordinateArray = loadChunkData(sequencePathName, refererLoc, currentOrganism, sequenceOffset, trackName)
        trackArrayList.add(coordinateArray)
        String sequenceName = getSequenceNameFromJsonString(sequenceString)
        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, currentOrganism)
        sequenceLengths << sequence.end

        JSONArray trackArray = mergeCoordinateArray(trackArrayList, sequenceLengths, currentOrganism.commonName, trackName)

        trackArray = sortNcListArray(trackArray)

        return trackArray
    }
}
