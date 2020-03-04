package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.gwt.shared.track.TrackIndex
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.SequenceDTO
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject

import javax.servlet.http.HttpServletResponse
import java.util.zip.GZIPInputStream

@Transactional
class TrackService {

    def preferenceService
    def trackMapperService
    def permissionService
    def configWrapperService

    static final String TRACKLIST = "trackList.json"
    static final String EXTENDED_TRACKLIST = "extendedTrackList.json"

    JSONObject getTrackList(String jbrowseDirectory) {
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

    String getTrackDataFile(String jbrowseDirectory, String trackName, String sequence) {
        JSONObject trackObject = getTrackList(jbrowseDirectory)
        String urlTemplate = null
        for (JSONObject track in trackObject.tracks) {
            if (track.key == trackName) {
                urlTemplate = track.urlTemplate
            }
        }

        return "${urlTemplate.replace("{refseq}", sequence)}"
    }

    JSONElement retrieveFileObject(String jbrowseDirectory, String trackDataFilePath) {

        if (trackDataFilePath.startsWith("http")) {
            trackDataFilePath = trackDataFilePath.replace(" ", "%20")
            if (trackDataFilePath.endsWith(".json")) {
                return JSON.parse(new URL(trackDataFilePath).text)
            } else if (trackDataFilePath.endsWith(".jsonz")) {
                def inputStream = new URL(trackDataFilePath).openStream()
                GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
                String outputString = gzipInputStream.readLines().join("\n")
                return JSON.parse(outputString)
            } else {
                log.error("type not understood: " + trackDataFilePath)
                return null
            }
        } else {
            if (!trackDataFilePath.startsWith("/")) {
                trackDataFilePath = jbrowseDirectory + "/" + trackDataFilePath
            }
            File file = new File(trackDataFilePath)
            if (!file.exists()) {
                log.error "File does not exist ${trackDataFilePath}"
                return null
            }
            return JSON.parse(file.text)
        }
    }

    JSONObject getAllTracks(String organism) throws FileNotFoundException {
        String jbrowseDirectory = preferenceService.getOrganismForToken(organism)?.directory
        JSONObject trackObject = getTrackList(jbrowseDirectory)
        return trackObject
    }

    JSONObject getTrackData(String trackName, String organism, String sequence) throws FileNotFoundException {
        String jbrowseDirectory = preferenceService.getOrganismForToken(organism)?.directory
        String trackDataFilePath = getTrackDataFile(jbrowseDirectory, trackName, sequence)
        return retrieveFileObject(jbrowseDirectory, trackDataFilePath) as JSONObject
    }

    @NotTransactional
    JSONArray getClassesForTrack(String trackName, String organism, String sequence) {
        JSONObject trackObject = getTrackData(trackName, organism, sequence)
        return trackObject.getJSONObject("intervals").getJSONArray("classes")
    }

    def storeTrackData(SequenceDTO sequenceDTO, JSONArray classesForTrack) {
        trackMapperService.storeTrack(sequenceDTO, classesForTrack)
    }

    JSONArray getNCList(String trackName, String organismString, String sequence, Long fmin, Long fmax) {
        assert fmin <= fmax

        // TODO: refactor into a common method
        JSONArray classesForTrack = getClassesForTrack(trackName, organismString, sequence)
        Organism organism = preferenceService.getOrganismForToken(organismString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: organism.commonName
                , trackName: trackName
                , sequenceName: sequence
        )
        this.storeTrackData(sequenceDTO, classesForTrack)

        // 1. get the trackData.json file
        JSONObject trackObject = getTrackData(trackName, organismString, sequence)
        JSONArray nclistArray = trackObject.getJSONObject("intervals").getJSONArray("nclist")

        // 1 - extract the appropriate region for fmin / fmax
        JSONArray filteredList = filterList(nclistArray, fmin, fmax)

        // if the first featured array has a chunk, then we need to evaluate the chunks instead
        if (filteredList) {
            TrackIndex trackIndex = trackMapperService.getIndices(sequenceDTO, filteredList.getJSONArray(0).getInt(0))
            if (trackIndex.hasChunk()) {
                List<JSONArray> chunkList = []
                for (JSONArray chunkArray in filteredList) {
                    JSONArray chunk = getChunkData(sequenceDTO, chunkArray.getInt(trackIndex.getChunk()))
                    def filteredChunkList = filterList(chunk, fmin, fmax)
                    if(filteredChunkList) chunkList.add(filteredChunkList)
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
    JSONArray getChunkData(SequenceDTO sequenceDTO, int chunk) throws FileNotFoundException {
        String jbrowseDirectory = preferenceService.getOrganismForToken(sequenceDTO.organismCommonName)?.directory

        String trackName = sequenceDTO.trackName
        String sequence = sequenceDTO.sequenceName

        String trackDataFilePath = getTrackDataFile(jbrowseDirectory, trackName, sequence)

        trackDataFilePath = trackDataFilePath.replace("trackData.json", "lf-${chunk}.json")

        return retrieveFileObject(jbrowseDirectory, trackDataFilePath) as JSONArray
    }

    @NotTransactional
    def convertIndividualNCListToObject(JSONArray featureArray, SequenceDTO sequenceDTO,long fmin,long fmax) throws FileNotFoundException {

        if (featureArray.size() > 3) {
            if (featureArray[0] instanceof Integer && !(featureArray[2] < fmin || featureArray[1] > fmax)) {
                JSONObject jsonObject = new JSONObject()
                TrackIndex trackIndex = trackMapperService.getIndices(sequenceDTO, featureArray.getInt(0))

                jsonObject.fmin = featureArray[trackIndex.getStart()]
                jsonObject.fmax = featureArray[trackIndex.getEnd()]



                if (trackIndex.source) {
                    jsonObject.source = featureArray[trackIndex.getSource()]
                }
                if (trackIndex.strand) {
                    jsonObject.strand = featureArray[trackIndex.getStrand()]
                }
                if (trackIndex.phase) {
                    jsonObject.phase = featureArray[trackIndex.phase]
                }
                if (trackIndex.type) {
                    jsonObject.type = featureArray[trackIndex.getType()]
                }
                if (trackIndex.score) {
                    jsonObject.score = featureArray[trackIndex.score]
                }
                if (trackIndex.name) {
                    jsonObject.name = featureArray[trackIndex.name]
                }
                if (trackIndex.id) {
                    jsonObject.id = featureArray[trackIndex.id]
                }
                if (trackIndex.seqId) {
                    jsonObject.seqId = featureArray[trackIndex.seqId]
                }
                // sequence source
//                jsonObject.seqId = featureArray[trackIndex.getSeqId()]


                JSONArray childArray = new JSONArray()
                for (int subIndex = 0; subIndex < featureArray.size(); ++subIndex) {
                    def subArray = featureArray.get(subIndex)
                    if (subArray instanceof JSONArray) {
                        def subArray2 = convertAllNCListToObject(subArray, sequenceDTO,fmin,fmax)
                        if(subArray2){
                          childArray.addAll(subArray2)
                        }
                    }
                    if (subArray instanceof JSONObject && subArray.containsKey("Sublist")) {
                        def subArrays2 = subArray.getJSONArray("Sublist")
                        if(subArrays2){
                          def ncListChildArray = convertIndividualNCListToObject(subArrays2, sequenceDTO,fmin,fmax)
                          if(ncListChildArray){
                            childArray.add(ncListChildArray)
                          }
                        }
                    }
                }
                if (childArray) {
                    jsonObject.children = childArray
                }
                return jsonObject
            }
        }
        return convertAllNCListToObject(featureArray, sequenceDTO,fmin,fmax)
    }

    @NotTransactional
    JSONArray convertAllNCListToObject(JSONArray fullArray, SequenceDTO sequenceDTO,long fmin = Long.MIN_VALUE,long fmax = Long.MAX_VALUE) throws FileNotFoundException {
        JSONArray returnArray = new JSONArray()

        for (def jsonArray in fullArray) {
            if (jsonArray instanceof JSONArray) {
              if (!(jsonArray[2] < fmin || jsonArray[1] > fmax)) {
                def convertedObject = convertIndividualNCListToObject(jsonArray, sequenceDTO,fmin,fmax)
                if(convertedObject) {
                  returnArray.add(convertedObject)
                }
              }
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
              // if it contains a subList, filter the sublist and addd to this array, can maybe leave that there
              int subListIndex = 0
              for(input in innerArray){
                if(input instanceof JSONObject && input.containsKey("Sublist")){
                  JSONArray subArrayList = filterList(input.get("Sublist"),fmin,fmax)
                  subArrayList.each{ jsonArray.add(it)}
                }
                ++subListIndex
              }
              jsonArray.add(innerArray)
            }
        }

        return jsonArray
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

    Map<String, Boolean> getTracksVisibleForOrganismAndGroup(Organism organism, UserGroup userGroup) {
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
    void setTracksVisibleForOrganismAndUser(Map<String, Boolean> trackVisibilityMap, Organism organism, User user) {
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
    void setTracksVisibleForOrganismAndGroup(Map<String, Boolean> trackVisibilityMap, Organism organism, UserGroup group) {

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

    Map<String, Boolean> getTracksVisibleForOrganismAndUser(Organism organism, User user) {
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

        TrackCache trackCache = TrackCache.findByOrganismNameAndTrackNameAndSequenceNameAndFminAndFmaxAndType(
                organismString,
                trackName,
                sequenceName,
                fmin,
                fmax,
                type
        )

        if(trackCache){
            trackCache.response = responseString
        }
        else{
            trackCache =  new TrackCache(
                    response: responseString
                    , organismName: organismString
                    , trackName: trackName
                    , sequenceName: sequenceName
                    , fmin: fmin
                    , fmax: fmax
                    , type: type
            )
        }
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
    @NotTransactional
    JSONObject findTrackFromArrayByCategory(JSONArray tracksArray, String category,boolean ignoreCase = true) {
        return findTrackFromArrayByKey(tracksArray,category,"category",ignoreCase)
    }

    /**
     *
     * @param tracksArray
     * @param label
     * @return
     */
    @NotTransactional
    JSONObject findTrackFromArrayByLabel(JSONArray tracksArray, String label,boolean ignoreCase = true) {
        return findTrackFromArrayByKey(tracksArray,label,"label",ignoreCase)
    }

    /**
     *
     * @param tracksArray
     * @param keyValue
     * @paramkey
     * @return
     */
    @NotTransactional
    JSONObject findTrackFromArrayByKey(JSONArray tracksArray, String keyValue, String key,boolean ignoreCase = true) {
        for (int i = 0; i < tracksArray.size(); i++) {
            JSONObject obj = tracksArray.getJSONObject(i)
            if(ignoreCase){
                if (obj.getString(key)?.equalsIgnoreCase(keyValue)) return obj
            }
            else{
                if (obj.getString(key)== keyValue) return obj
            }
        }
        return null
    }

    /**
     *
     * @param tracksArray
     * @param trackName
     * @return
     */
    @NotTransactional
    def removeTrackFromArray(JSONArray tracksArray, String trackName) {
        JSONArray returnArray = new JSONArray()
        for (int i = 0; i < tracksArray.size(); i++) {
            JSONObject obj = tracksArray.getJSONObject(i)
            if (obj.getString("label") != trackName) {
                returnArray.add(obj)
            }
        }

        return returnArray
    }

    /**
     * Removes plugins included in annot.json (which is just WebApollo)
     * @param pluginsArray
     */
    @NotTransactional
    def removeIncludedPlugins(JSONArray pluginsArray) {
        def iterator = pluginsArray.iterator()
        while (iterator.hasNext()) {
            def plugin = iterator.next()
            if (plugin instanceof JSONObject) {
                if (plugin.name == "WebApollo") {
                    iterator.remove()
                }
            } else if (plugin instanceof String) {
                if (plugin == "WebApollo") {
                    iterator.remove()
                }
            }
        }
    }

    @NotTransactional
    JSONArray flattenArray(JSONArray jsonArray, String... types) {

        List<String> typeList = new ArrayList<>()
        types.each { typeList.add(it) }
        JSONArray rootArray = new JSONArray()
        // here we just clone it
        for (def obj in jsonArray) {
            if (obj instanceof JSONObject) {
                if (typeList.contains(obj.type)) {
                    for (JSONObject child in getGeneChildren(obj, typeList)) {
                        rootArray.add(child)
                    }
//                rootArray.add(obj)
                }
            }
//            else if (obj instanceof JSONArray) {
//                rootArray.addAll(flattenArray(obj,types))
////                for (JSONObject child in obj) {
////                    rootArray.add(child)
////                }
//            }
        }

        return rootArray

    }

    @NotTransactional
    JSONArray getGeneChildren(JSONObject jsonObject, List<String> typeList) {
        JSONArray geneChildren = new JSONArray()
        boolean hasGeneChild = false
        Iterator childIterator = jsonObject.children.iterator()
        while (childIterator.hasNext()) {
            def child = childIterator.next()
            if (child instanceof JSONObject) {
                if (typeList.contains(child.type)) {
                    hasGeneChild = true
                    geneChildren.addAll(getGeneChildren(child, typeList))
                }
            }
            // if a subarray instead
            else if (child instanceof JSONArray) {
                for (grandChild in child) {
                    if (typeList.contains(grandChild.type)) {
                        hasGeneChild = true
                        geneChildren.addAll(getGeneChildren(grandChild, typeList))
                    }
                }
                geneChildren.add(jsonObject)

                Iterator iter = child.iterator()
                while (iter.hasNext()) {
                    def object = iter.next()
                    if (typeList.contains(object.type)) {
                        iter.remove()
                    }
                }
            }
        }
        if (!hasGeneChild) {
            geneChildren.add(jsonObject)
        } else {
            Iterator iter = jsonObject.children.iterator()
            while (iter.hasNext()) {
                def object = iter.next()
                if (typeList.contains(object.type)) {
                    iter.remove()
                }
            }

        }
        return geneChildren
    }

    def checkPermission(def request, def response, String organismString) {
        Organism organism = preferenceService.getOrganismForToken(organismString)
        if (organism && (organism.publicMode || permissionService.checkPermissions(PermissionEnum.READ))) {
            return true
        } else {
            // not accessible to the public
            response.status = HttpServletResponse.SC_FORBIDDEN
            return false
        }
    }

    @Transactional(readOnly = true)
    File getExtendedTrackList(Organism organism){
        File returnFile = new File(getExtendedDataDirectory(organism).absolutePath + File.separator + EXTENDED_TRACKLIST)
        if(!returnFile.exists()){
            log.warn("File ${returnFile.absolutePath} does not exist")
        }
        if(!returnFile.canRead()){
            log.warn("File ${returnFile.absolutePath} can not be read")
        }
        if(!returnFile.canWrite()){
            log.warn("File ${returnFile.absolutePath} can not be written to")
        }
        return returnFile
    }

    @Transactional(readOnly = true)
    File getExtendedDataDirectory(Organism organism){
        File returnFile = new File(commonDataDirectory + File.separator + organism.id + "-" + organism.commonName.replaceAll(" ","_"))
        if(!returnFile.exists()){
            log.warn("File ${returnFile.absolutePath} does not exist")
        }
        if(!returnFile.canRead()){
            log.warn("File ${returnFile.absolutePath} can not be read")
        }
        if(!returnFile.canWrite()){
            log.warn("File ${returnFile.absolutePath} can not be written to")
        }
        return returnFile
    }


    @Transactional(readOnly = true)
    String getCommonDataDirectory() {
        // TODO: cache?
        ApplicationPreference commonDataPreference = ApplicationPreference.findByName(FeatureStringEnum.COMMON_DATA_DIRECTORY.value)
        return commonDataPreference?.value
    }

    /**
     *
     * 1. Determine the preferred version
     *    1a. The database one has the source of user, config, or ??  If it is user, then the database is always the default.
     *    1b.
     * 2. See if that version is valid (exists and can write)
     *    2a. Use the next backup (config)
     *    2b. Use the next backup (find a home writeable directory)
     *    2c. Ask the user for help
     * 3. Notify the admin user the first time of where that directory is.
     *
     * The reason for using the database is to remove the configuration detail if startup is easier.
     *
     * If both exist and they match and they are both writeable, then return
     *
     *
     * @return
     */
    def checkCommonDataDirectory() {
        ApplicationPreference commonDataPreference = ApplicationPreference.findByName(FeatureStringEnum.COMMON_DATA_DIRECTORY.value)
        String directory

        try {
            if (commonDataPreference) {
                directory = commonDataPreference.value
                log.debug "Preference exists in database [${directory}]."
                File testDirectory = new File(directory)
                if (!testDirectory.exists()) {
                    log.warn "Directory does not exist so trying to make"
                    assert testDirectory.mkdirs()
                }
                if (testDirectory.exists() && testDirectory.canWrite()) {
                    log.debug "Directory ${directory} exists and is writable so returning"
                    return null
                }
            }

            // if all of the tests fail, then do the next thing
            log.warn "Unable to write to the database directory, so checking the config file"
            directory = configWrapperService.commonDataDirectory
            File testDirectory = new File(directory)
            if (!testDirectory.exists()) {
                assert testDirectory.mkdirs()
            }
            if (testDirectory.exists() && testDirectory.canWrite()) {
                ApplicationPreference applicationPreference = new ApplicationPreference(
                        name: "common_data_directory",
                        value: directory

                ).save(failOnError: true, flush: true)
                log.info("Saving new preference for common data directory ${directory}")
                return null
            }
        } catch (Throwable e) {
            log.error "Unable to write to directory ${directory}. ${e}"
            return "Unable to write to directory ${directory}."
        }
    }


    def updateCommonDataDirectory(String newDirectory) {
        File testDirectory = new File(newDirectory)
        if (!testDirectory.exists()) {
            assert testDirectory.mkdirs()
        }
        if (!testDirectory.exists() || !testDirectory.canWrite()) {
            return "Unable to write to directory ${newDirectory}"
        }
        ApplicationPreference commonDataPreference = ApplicationPreference.findOrSaveByName("common_data_directory")
        commonDataPreference.value = newDirectory
        commonDataPreference.save()
        return null
    }


    @NotTransactional
    def generateJSONForGff3(File inputFile, String trackPath, String jbrowseBinariesPath,String topType=null){
        File fileToExecute = new File(jbrowseBinariesPath + "/flatfile-to-json.pl")
        log.debug "file to execute ${fileToExecute}"
        log.debug "file exists ${fileToExecute.exists()}"
        log.debug "file can execute ${fileToExecute.canExecute()}"
        File trackPathFile = new File(trackPath)
        log.debug "track path ${trackPath} -> exissts ${trackPathFile.exists()} and can write ${trackPathFile.canWrite()}"
        if(!fileToExecute.canExecute()){
            fileToExecute.setExecutable(true,true)
            log.debug "file can execute ${fileToExecute.canExecute()}"
        }
//        bin/flatfile-to-json.pl --[gff|gbk|bed] <flat file> --tracklabel <track name>
        log.debug "input fie ${inputFile}"
        String outputName = inputFile.getName().substring(0,inputFile.getName().lastIndexOf("."))

        def arguments
        if(topType){
//            arguments = [fileToExecute.absolutePath,"--gff",inputFile.absolutePath,"--compress","--type",topType,"--trackLabel",outputName,"--out",trackPath]
            arguments = [fileToExecute.absolutePath,"--gff",inputFile.absolutePath,"--compress","--type",topType,"--trackLabel",outputName,"--out",trackPath]
        }
        else{
            arguments = [fileToExecute.absolutePath,"--gff",inputFile.absolutePath,"--compress","--trackLabel",outputName,"--out",trackPath]
        }
        String executionString = arguments.join(" ")
        log.info "generating NCList with ${executionString}"

        def proc = executionString.execute()
        log.debug "error: ${proc.err.text}"
    }
}
