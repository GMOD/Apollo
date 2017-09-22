package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.track.TrackIndex
import org.bbop.apollo.sequence.SequenceDTO
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Transactional
class TrackService {

    def preferenceService
    def trackMapperService

    public static String TRACK_NAME_SPLITTER = "::"

    JSONObject getTrackList(String jbrowseDirectory){
        log.debug "got data directory of . . . ? ${jbrowseDirectory}"
        String absoluteFilePath = jbrowseDirectory + "/trackList.json"
        File file = new File(absoluteFilePath);

        if (!file.exists()) {
            log.warn("Could not get for name and path: ${absoluteFilePath}");
            return null;
        }

        // add datasets to the configuration
        JSONObject jsonObject = JSON.parse(file.text) as JSONObject
        return jsonObject
    }

    JSONObject getTrackData(String trackName, String organism, String sequence) {
        String jbrowseDirectory = preferenceService.getOrganismForToken(organism)?.directory
        String trackListPath = "${jbrowseDirectory}/trackList.json"
        JSONObject trackObject = getTrackList(jbrowseDirectory)
        String urlTemplate = null
        for(JSONObject track in trackObject.tracks){
            if(track.key == trackName){
                urlTemplate = track.urlTemplate
            }
        }

        println trackListPath
        println jbrowseDirectory
        println trackObject as JSON

        String trackDataFilePath = "${urlTemplate.replace("{refseq}",sequence)}"
        trackDataFilePath = trackDataFilePath.replace(" ","%20")
        println "final url [${trackDataFilePath}]"

        if(trackDataFilePath.startsWith("http")){
            println "is remote: ${trackDataFilePath}"
            if(trackDataFilePath.endsWith(".json")){
                def dataObject = JSON.parse( new URL( trackDataFilePath).text ) as JSONObject
                return dataObject
            }
            else
            if(trackDataFilePath.endsWith(".jsonz")){
                println "handling jsonz"
                def inputStream = new URL(trackDataFilePath).openStream()
//                println "got bytes: ${gzipBytes.length}"
                GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
                println "init input stream"
                String outputString = gzipInputStream.readLines().join("\n")
                println "output string ${outputString}"
                JSONObject dataObject = JSON.parse( outputString ) as JSONObject
                println "final data object ${dataObject as JSON}"
                return dataObject
            }
            else{
                log.error("type not understood: "+trackDataFilePath)
                return null
            }
        }
        else{
            println "is local file: ${trackDataFilePath}"
            File file = new File(trackDataFilePath)
            if (!file.exists()) {
                log.error "File does not exist ${trackDataFilePath}"
                return null
            }
            return JSON.parse(file.text) as JSONObject
        }
    }

    @NotTransactional
    JSONArray getClassesForTrack(String trackName, String organism, String sequence) {
        JSONObject trackObject = getTrackData(trackName, organism, sequence)
        return trackObject.getJSONObject("intervals").getJSONArray("classes")
    }

    JSONArray getNCList(String trackName, String organismString, String sequence, Long fmin, Long fmax) {
        assert fmin <= fmax

        // TODO: refactor into a common method
        JSONArray clasesForTrack = getClassesForTrack(trackName, organismString, sequence)
        Organism organism = preferenceService.getOrganismForToken(organismString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: organism.commonName
                , trackName: trackName
                , sequenceName: sequence
        )
        trackMapperService.storeTrack(sequenceDTO, clasesForTrack)

        // 1. get the trackData.json file
        JSONObject trackObject = getTrackData(trackName, organismString, sequence)
        JSONArray nclistArray = trackObject.getJSONObject("intervals").getJSONArray("nclist")

        // 1 - extract the appropriate region for fmin / fmax
        JSONArray filteredList = filterList(nclistArray, fmin, fmax)
        log.debug "filtered list size ${filteredList.size()} from original ${nclistArray.size()}"

        // if the first featured array has a chunk, then we need to evaluate the chunks instead
        if (filteredList) {
            TrackIndex trackIndex = trackMapperService.getIndices(sequenceDTO, filteredList.getJSONArray(0).getInt(0))
            if (trackIndex.hasChunk()) {
                List<JSONArray> chunkList = []
                for (JSONArray chunkArray in filteredList) {
                    JSONArray chunk = getChunkData(sequenceDTO, chunkArray.getInt(trackIndex.getChunk()))
                    chunkList.add(filterList(chunk, fmin, fmax))
                }
                JSONArray chunkReturnArray = new JSONArray()
                chunkList.each { ch ->
                    ch.each {
                        chunkReturnArray.add(it)
                    }
                }
                return chunkReturnArray
            }
        }

        return filteredList
    }

    /**
     * reads the file lf-{chunk}.json
     * @param sequenceDTO
     * @param chunk
     * @return
     */
    JSONArray getChunkData(SequenceDTO sequenceDTO, int chunk) {
        String jbrowseDirectory = preferenceService.getOrganismForToken(sequenceDTO.organismCommonName)?.directory
        String trackPath = "${jbrowseDirectory}/tracks/${sequenceDTO.trackName}/${sequenceDTO.sequenceName}"
        String trackDataFilePath = "${trackPath}/lf-${chunk}.json"

        File file = new File(trackDataFilePath)
        if (!file.exists()) {
            log.error "file does not exist ${trackDataFilePath}"
            return null
        }
        return JSON.parse(file.text) as JSONArray
    }

    @NotTransactional
    JSONObject convertIndividualNCListToObject(JSONArray featureArray, SequenceDTO sequenceDTO) {
        JSONObject jsonObject = new JSONObject()

        if (featureArray.size() > 3) {
            if (featureArray[0] instanceof Integer) {
                TrackIndex trackIndex = trackMapperService.getIndices(sequenceDTO, featureArray.getInt(0))

                jsonObject.fmin = featureArray[trackIndex.getStart()]
                jsonObject.fmax = featureArray[trackIndex.getEnd()]
                if (trackIndex.strand) {
                    jsonObject.strand = featureArray[trackIndex.getStrand()]
                }
//                if (trackIndex.source) {
//                    jsonObject.source = featureArray[trackIndex.getSource()]
//                }
                if (trackIndex.type) {
                    jsonObject.type = featureArray[trackIndex.getType()]
                }
                if (trackIndex.id) {
                    jsonObject.name = featureArray[trackIndex.id]
                }
                if (trackIndex.phase) {
                    jsonObject.phase = featureArray[trackIndex.phase]
                }
                // sequence source
//                jsonObject.seqId = featureArray[trackIndex.getSeqId()]


                JSONArray childArray = new JSONArray()
                for (int subIndex = 0; subIndex < featureArray.size(); ++subIndex) {
                    def subArray = featureArray.get(subIndex)
                    if (subArray instanceof JSONArray) {
                        def subArray2 = convertAllNCListToObject(subArray, sequenceDTO)
                        childArray.addAll(subArray2)
                    }
                    if (subArray instanceof JSONObject && subArray.containsKey("Sublist")) {
                        def subArrays2 = subArray.getJSONArray("Sublist")
                        childArray.add(convertIndividualNCListToObject(subArrays2, sequenceDTO))
                    }
                }
                if (childArray) {
                    jsonObject.children = childArray
                }
            }
        }



        return jsonObject
    }

    @NotTransactional
    JSONArray convertAllNCListToObject(JSONArray fullArray, SequenceDTO sequenceDTO) {
        JSONArray returnArray = new JSONArray()

        for (def jsonArray in fullArray) {
            if (jsonArray instanceof JSONArray) {
                returnArray.add(convertIndividualNCListToObject(jsonArray, sequenceDTO))
            }
        }

        return returnArray
    }

    @NotTransactional
    JSONArray filterList(JSONArray inputArray, long fmin, long fmax) {
        if (fmin < 0 && fmax < 0) return inputArray

        JSONArray jsonArray = new JSONArray()

        for (innerArray in inputArray) {
            // if there is an overlap
            if (!(innerArray[2] < fmin || innerArray[1] > fmax)) {
                // then no
                jsonArray.add(innerArray)
            }
        }

        return jsonArray
    }

    // TODO
    JSONObject getNCListAsBioLink(JSONArray jsonArray) {
        null
    }

    // TODO: implement with track permissions
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

    // TODO: implement with track permissions
    String getTracks(UserGroup group, Organism organism) {
        String trackList = ""
        JSONArray jsonArray = new JSONArray()
        for (GroupPermission groupPermission in GroupPermission.findAllByGroupAndOrganism(group, organism)) {
            trackList += "||" + groupPermission.trackNames // TODO: add properly
        }
        return trackList.trim()
    }

    // TODO: implement with track permissions
    String getTrackPermissions(UserGroup userGroup, Organism organism) {
        JSONArray jsonArray = new JSONArray()
        for (GroupPermission groupPermission in GroupPermission.findAllByGroupAndOrganism(userGroup, organism)) {
            jsonArray.add(groupPermission as JSON)
        }
        return jsonArray.toString()
    }

    // TODO: implement with track permissions
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

    @NotTransactional
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

    String checkCache(String organismString, String trackName, String sequence, String featureName, String type, Map paramMap) {
        String mapString = paramMap ? (paramMap as JSON).toString() : null
        return TrackCache.findByOrganismNameAndTrackNameAndSequenceNameAndFeatureNameAndTypeAndParamMap(organismString, trackName, sequence, featureName, type, mapString)?.response
    }

    String checkCache(String organismString, String trackName, String sequence, Long fmin, Long fmax, String type, Map paramMap) {
        String mapString = paramMap ? (paramMap as JSON).toString() : null
        return TrackCache.findByOrganismNameAndTrackNameAndSequenceNameAndFminAndFmaxAndTypeAndParamMap(organismString, trackName, sequence, fmin, fmax, type, mapString)?.response
    }

    @Transactional
    def cacheRequest(String responseString, String organismString, String trackName, String sequenceName, String featureName, String type, Map paramMap) {
        TrackCache trackCache = new TrackCache(
                response: responseString
                , organismName: organismString
                , trackName: trackName
                , sequenceName: sequenceName
                , featureName: featureName
                , type: type
        )
        if (paramMap) {
            trackCache.paramMap = (paramMap as JSON).toString()
        }
        trackCache.save()
    }

    @Transactional
    def cacheRequest(String responseString, String organismString, String trackName, String sequenceName, Long fmin, Long fmax, String type, Map paramMap) {
        TrackCache trackCache = new TrackCache(
                response: responseString
                , organismName: organismString
                , trackName: trackName
                , sequenceName: sequenceName
                , fmin: fmin
                , fmax: fmax
                , type: type
        )
        if (paramMap) {
            trackCache.paramMap = (paramMap as JSON).toString()
        }
        trackCache.save()
    }

    /**
     *
     * @param tracksArray
     * @param trackName
     * @return
     */
    def findTrackFromArray(JSONArray tracksArray, String trackName) {
        for (int i = 0; i < tracksArray.size(); i++) {
            JSONObject obj = tracksArray.getJSONObject(i)
            if (obj.getString("label") == trackName) return obj
        }

        return null
    }
}
