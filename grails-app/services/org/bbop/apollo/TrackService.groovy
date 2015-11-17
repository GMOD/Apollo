package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.Coordinate
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionChunk
import org.bbop.apollo.projection.ProjectionChunkList
import org.bbop.apollo.projection.ProjectionInterface
import org.bbop.apollo.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class TrackService {


    public static String TRACK_NAME_SPLITTER = "::"

    def projectionService

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

    /**
     * For the track in question, grab the array that matches the top-level name.
     * @param sequence
     * @param trackName
     * @param nameLookup
     * @return
     */
    JSONArray getTrackData(Sequence sequence, String trackName, String nameLookup) {
        String jbrowseDirectory = sequence.organism.directory + "/tracks/" + trackName
        File trackDirectory = new File(jbrowseDirectory)
        println "track directory ${trackDirectory.absolutePath}"
        String sequenceDirectory = jbrowseDirectory + "/" + sequence.name
        File trackDataFile = new File(sequenceDirectory + "/trackData.json")
        println "track data file ${trackDataFile.absolutePath}"
        assert trackDataFile.exists()
        println "looking up ${nameLookup}"

        JSONObject referenceJsonObject = new JSONObject(trackDataFile.text)
        JSONArray coordinateReferenceJsonArray = referenceJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        for (int coordIndex = 0; coordIndex < coordinateReferenceJsonArray.size(); ++coordIndex) {
            JSONArray coordinate = coordinateReferenceJsonArray.getJSONArray(coordIndex)
            // TODO: use enums to better track format
            if (coordinate.getInt(0) == 4 || coordinate.getInt(0) == 3) {
                // projecess the file lf-${coordIndex} instead
                File chunkFile = new File(trackDataFile.parent + "/lf-${coordIndex + 1}.json")
                JSONArray chunkReferenceJsonArray = new JSONArray(chunkFile.text)

                for (int chunkArrayIndex = 0; chunkArrayIndex < chunkReferenceJsonArray.size(); ++chunkArrayIndex) {
                    JSONArray chunkArrayCoordinate = chunkReferenceJsonArray.getJSONArray(chunkArrayIndex)
                    JSONArray returnArray = findCoordinateName(chunkArrayCoordinate, nameLookup)
                    if (returnArray) return returnArray
                }

            } else {
                JSONArray returnArray = findCoordinateName(coordinate, nameLookup)
                if (returnArray) return returnArray
//                discontinuousProjection.addInterval(coordinate.getInt(1), coordinate.getInt(2),padding)
            }
        }

        // return an empty array if not found
        return new JSONArray()
    }

    JSONArray findCoordinateName(JSONArray coordinate, String nameLookup) {
        String name = nameLookup.toLowerCase()

        int classType = coordinate.getInt(0)
        for (int i = 0; i < coordinate.size(); i++) {
            switch (classType) {
                case 0:
                    if (coordinate.getString(6).toLowerCase().contains(name)) {
                        return coordinate
                    }
                    // search sublist
                    if (coordinate.size() > 11) {
                        println "coordinate > 11 ${coordinate as JSON}"
                        JSONObject subList = coordinate.getJSONObject(11)
//                        println "subList ${subList as JSON}"
//                        JSONObject subOject = subList.getJSONObject("Sublist")
                        JSONArray subArray = subList.getJSONArray("Sublist")
                        for (int subIndex = 0; subIndex < subArray.size(); ++subIndex) {
                            println "subArray ${subArray as JSON}"

                            JSONArray subSubArray = subArray.getJSONArray(subIndex)
                            if (subSubArray.getInt(0) == 0) {
                                if (subSubArray.getString(6).toLowerCase().contains(name)) {
                                    return subSubArray
                                }
                            }
                        }
                    }
                    break
                case 1:
                    if (coordinate.getString(8).toLowerCase().contains(name)) {
                        return coordinate
                    }
                    break
                case 2:
                case 3:
                    if (coordinate.getString(8).toLowerCase().contains(name)) {
                        return coordinate
                    }
                    break
                case 4:
                    println "can not process case 4 ${coordinate as JSON}"
                    break
            }


        }


        return null
    }


    def String getSequencePathName(String inputName) {
        if (inputName.contains("/")) {
            String[] tokens = inputName.split("/")
            // the sequence path should be the second to the last one
            return tokens.length >= 2 ? tokens[tokens.length - 2] : null
        }
        return null
    }

    // replace index - 2 with sequenceName
    String generateTrackNameForSequence(String inputName, String sequenceName) {
        if (inputName.contains("/")) {
            String[] tokens = inputName.split("/")
            // the sequence path should be the second to the last one
            if (tokens.length >= 2) {
                tokens[tokens.length - 2] = sequenceName
                return tokens.join("/")
            }
        }
        return null
    }

    def String getTrackPathName(String inputName) {
        if (inputName.contains("/")) {
            String[] tokens = inputName.split("/")
            // the sequence path should be the second to the last one
            return tokens.length >= 3 ? tokens[tokens.length - 3] : null
        }
        return null
    }

    def JSONArray loadChunkData(String path, String refererLoc, Organism currentOrganism,Integer offset) {
        println "loading chunk data with offset ${offset}"
        File file = new File(path)
        String inputText = file.text
        JSONArray coordinateJsonArray = new JSONArray(inputText)
        String sequenceName = projectionService.getSequenceName(file.absolutePath)
        // get the track from the json object

        // TODO: it should look up the OGS track either default or variable
        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, currentOrganism)

        if (projection && projection.containsSequence(sequenceName, currentOrganism)) {
            println "found a projection ${projection.size()}"
            for (int i = 0; i < coordinateJsonArray.size(); i++) {
                JSONArray coordinate = coordinateJsonArray.getJSONArray(i)
                projectJsonArray(projection, coordinate,offset)
            }
        }
        return coordinateJsonArray
    }

    def JSONObject loadTrackData(String path, String refererLoc, Organism currentOrganism) {
        File file = new File(path)
        String inputText = file.text
        JSONObject trackDataJsonObject = new JSONObject(inputText)
        String sequenceName = projectionService.getSequenceName(file.absolutePath)
        // get the track from the json object

        // TODO: it should look up the OGS track either default or variable
        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, currentOrganism)
        ProjectionSequence projectionSequence = projection.getProjectionSequence(sequenceName,currentOrganism)

        if (projection && projectionSequence) {
            println "found a projection ${projection.size()}"
            JSONObject intervalsJsonArray = trackDataJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value)
            JSONArray coordinateJsonArray = intervalsJsonArray.getJSONArray(FeatureStringEnum.NCLIST.value)
            for (int i = 0; i < coordinateJsonArray.size(); i++) {
                JSONArray coordinate = coordinateJsonArray.getJSONArray(i)
                projectJsonArray(projection, coordinate,0,projectionSequence.originalOffset)
            }
        }
        return trackDataJsonObject
    }

    JSONArray projectJsonArray(MultiSequenceProjection projection, JSONArray coordinate) {
        return projectJsonArray(projection,coordinate,0,0)
    }

    JSONArray projectJsonArray(MultiSequenceProjection projection, JSONArray coordinate,Integer offset,Integer adjustment) {

        // see if there are any subarrays of size >4 where the first one is a number 0-5 and do the same  . . .
        for (int subIndex = 0; subIndex < coordinate.size(); ++subIndex) {
            def subArray = coordinate.get(subIndex)
            if (subArray instanceof JSONArray) {
                projectJsonArray(projection, subArray,offset,adjustment)
            }
        }

        if (coordinate.size() >= 3
                && coordinate.get(0) instanceof Integer
                && coordinate.get(1) instanceof Integer
                && coordinate.get(2) instanceof Integer
        ) {
            Integer oldMin = coordinate.getInt(1)+adjustment
            Integer oldMax = coordinate.getInt(2)+adjustment
            Coordinate newCoordinate = projection.projectCoordinate(oldMin, oldMax)
            if (newCoordinate && newCoordinate.isValid()) {
                coordinate.set(1, newCoordinate.min+offset-adjustment)
                coordinate.set(2, newCoordinate.max+offset-adjustment)
            } else {
                log.error("Invalid mapping of coordinate ${coordinate} -> ${newCoordinate}")
                coordinate.set(1, -1)
                coordinate.set(2, -1)
            }
        }

        return coordinate
    }

    /**
     * merge trackData.json objects from different sequence sources . . .
     *
     * 1 - assume already projected
     * 2 - assume in the correct order
     * @param mergeTrackObject
     * @return
     */
    def JSONObject mergeTrackObject(List<JSONObject> trackList) {

        JSONObject finalObject = null
        int endSize = 0
        for (JSONObject jsonObject in trackList) {
            if (finalObject == null) {
                finalObject = jsonObject
                // get endSize
                endSize = jsonObject.intervals.maxEnd
            } else {
                // ignore formatVersion
                // add featureCount
                finalObject.featureCount = finalObject.featureCount + jsonObject.featureCount

                // somehow add histograms together
                finalObject.histograms = mergeHistograms(finalObject.histograms, jsonObject.histograms)

                // add intervals together starting at end and adding
                finalObject.intervals = mergeIntervals(finalObject.intervals, jsonObject.intervals, endSize)

                // get endSize
                endSize += jsonObject.intervals.maxEnd
            }
        }


        return finalObject
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

    JSONArray nudgeNcListArray(JSONArray coordinate, Integer nudgeAmount,Integer nudgeIndex) {
        // see if there are any subarrays of size >4 where the first one is a number 0-5 and do the same  . . .
        for (int subIndex = 0; subIndex < coordinate.size(); ++subIndex) {
            def subArray = coordinate.get(subIndex)
            if (subArray instanceof JSONArray) {
                nudgeNcListArray(subArray, nudgeAmount,nudgeIndex)
            }
        }

        if (coordinate.size() >= 3
                && coordinate.get(0) instanceof Integer
                && coordinate.get(1) instanceof Integer
                && coordinate.get(2) instanceof Integer
                && coordinate.get(3) instanceof Integer
        ) {
            coordinate.set(1, coordinate.getInt(1) + nudgeAmount)
            coordinate.set(2, coordinate.getInt(2) + nudgeAmount)
            coordinate.set(3, coordinate.getInt(3) + nudgeIndex)
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
    JSONObject mergeIntervals(JSONObject first, JSONObject second, int endSize) {
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


        mergeCoordinateArray(firstNcListArray,secondNcListArray,endSize)

        return first
    }

    /**
     * This is a destructive method .. . putting everythin on the second onto the first with an alignment
     * @param firstNcListArray
     * @param secondNcListArray
     * @return
     */
    JSONArray mergeCoordinateArray(JSONArray firstNcListArray,JSONArray secondNcListArray,int endSize) {
        int nudgeIndex = firstNcListArray.size()
        for (int i = 0; i < secondNcListArray.size(); i++) {
            def ncListArray = secondNcListArray.get(i)
            if (ncListArray instanceof JSONArray) {
                nudgeNcListArray(ncListArray, endSize,nudgeIndex)
                firstNcListArray.add(ncListArray)
            }
        }
        return firstNcListArray
    }

    JSONArray mergeCoordinateArray(ArrayList<JSONArray> jsonArrays,List<Integer> endSizes) {

        JSONArray firstNcListArray = jsonArrays.first()

        for(int i = 1 ; i < jsonArrays.size() ; i++){
            JSONArray secondArray = jsonArrays.get(i)
            mergeCoordinateArray(firstNcListArray,secondArray,endSizes.get(i-1))
        }

//        for (int i = 0; i < secondNcListArray.size(); i++) {
//            def ncListArray = secondNcListArray.get(i)
//            if (ncListArray instanceof JSONArray) {
//                nudgeNcListArray(ncListArray, endSize)
//                firstNcListArray.add(ncListArray)
//            }
//        }

        return firstNcListArray

    }

    JSONObject projectTrackData(ArrayList<String> sequenceStrings, String dataFileName, String refererLoc, Organism currentOrganism) {
        List<JSONObject> trackObjectList = new ArrayList<>()
        ProjectionChunkList projectionChunkList = new ProjectionChunkList()

        // can probably store the projection chunks
        Integer priorSequenceLength = 0
        Integer priorChunkArrayOffset = 0
        for (sequence in sequenceStrings) {
            ProjectionChunk projectionChunk = new ProjectionChunk(
                    sequence: sequence
            )
            String sequencePathName = generateTrackNameForSequence(dataFileName, sequence)
            // this loads PROJECTED
            JSONObject trackObject = loadTrackData(sequencePathName, refererLoc, currentOrganism)
            JSONArray ncListArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
            Integer lastLength = 0
            Integer lastChunkArrayOffset = 0
            for (int i = 0; i < ncListArray.size(); i++) {
                JSONArray internalArray = ncListArray.getJSONArray(i)
                if(internalArray.getInt(0)==4){
                    projectionChunk.addChunk()
                }
                lastLength = internalArray.getInt(2)
                ++lastChunkArrayOffset
            }

            projectionChunk.chunkArrayOffset = priorChunkArrayOffset
            projectionChunk.sequenceOffset = priorSequenceLength

            priorSequenceLength = priorSequenceLength + lastLength
            priorChunkArrayOffset = priorChunkArrayOffset + lastChunkArrayOffset

            projectionChunkList.addChunk(projectionChunk)

            trackObjectList.add(trackObject)
        }



        MultiSequenceProjection multiSequenceProjection = projectionService.getProjection(refererLoc, currentOrganism)
        multiSequenceProjection.projectionChunkList = projectionChunkList
        projectionService.storeProjection(refererLoc, multiSequenceProjection, currentOrganism)

        JSONObject trackObject = mergeTrackObject(trackObjectList)

        trackObject.intervals.minStart = multiSequenceProjection.projectValue(trackObject.intervals.minStart)
        trackObject.intervals.maxEnd = multiSequenceProjection.projectValue(trackObject.intervals.maxEnd)

        return trackObject

    }

    /**
     * Get chunk index for files of the pattern: lf-${X}.json
     * @param fileName
     * @return
     */
    private Integer getChunkIndex(String fileName) {
        String finalString = fileName.substring(3, fileName.length() - 5)
        return Integer.parseInt(finalString)
    }

    JSONArray projectTrackChunk(String fileName, String dataFileName, String refererLoc, Organism currentOrganism) {
        List<JSONArray> trackArrayList = new ArrayList<>()
        List<Integer> sequenceLengths = []

        MultiSequenceProjection multiSequenceProjection = projectionService.getProjection(refererLoc, currentOrganism)

        Integer chunkIndex = getChunkIndex(fileName)
        ProjectionChunk projectionChunk=  multiSequenceProjection.projectionChunkList.findProjectChunkForIndex(chunkIndex)
        String sequenceString = projectionChunk.sequence
        Integer sequenceOffset = projectionChunk.sequenceOffset
        // calculate offset for chunk and replace the filename
        // should be the start of the string
        String originalFileName = "lf-${chunkIndex-projectionChunk.chunkArrayOffset}.json"
        dataFileName = dataFileName.replaceAll(fileName, originalFileName)

        // next we want to load tracks from the REAL paths . .  .
        String sequencePathName = generateTrackNameForSequence(dataFileName, sequenceString)
        // this loads PROJECTED
        JSONArray coordinateArray = loadChunkData(sequencePathName, refererLoc, currentOrganism,sequenceOffset)
        trackArrayList.add(coordinateArray)
        Sequence sequence = Sequence.findByNameAndOrganism(sequenceString, currentOrganism)
        sequenceLengths << sequence.end

        JSONArray trackArray = mergeCoordinateArray(trackArrayList, sequenceLengths)
        return trackArray
    }
}
