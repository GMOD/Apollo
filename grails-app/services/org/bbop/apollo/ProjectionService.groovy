package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.DiscontinuousProjection
import org.bbop.apollo.projection.Location
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionDescription
import org.bbop.apollo.projection.ProjectionInterface
import org.bbop.apollo.projection.ProjectionSequence
import org.bbop.apollo.projection.TrackIndex
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class ProjectionService {

    def bookmarkService
    def trackMapperService
    def permissionService

//    private Map<String, Map<String, ProjectionInterface>> projectionMap = new HashMap<>()

//    private Map<ProjectionDescription, MultiSequenceProjection> multiSequenceProjectionMap = new HashMap<>()


    @NotTransactional
    String getTrackName(String fileName) {
        String[] tokens = fileName.split("/")
        return tokens[tokens.length - 3]
    }

    @NotTransactional
    String getSequenceName(String fileName) {
        String[] tokens = fileName.split("/")
        return tokens[tokens.length - 2]
    }

    // TODO: do re-lookup
//    def createTranscriptProjection(Organism organism, JSONArray tracksArray, Integer padding, String trackName) {
//        // TODO: this is only here for debugging . .
//        projectionMap.clear()
//        long startTime = System.currentTimeMillis()
//        for (int i = 0; i < tracksArray.size(); i++) {
//            JSONObject trackObject = tracksArray.getJSONObject(i)
//            if (trackObject.containsKey("OGS") && trackObject.getBoolean("OGS") && !projectionMap.containsKey(trackObject.keys())) {
//
//                String jbrowseDirectory = organism.directory + "/tracks/" + trackObject.key
//                File trackDirectory = new File(jbrowseDirectory)
//
//
//                File[] files = FileUtils.listFiles(trackDirectory, FileFilterUtils.nameFileFilter("trackData.json"), TrueFileFilter.INSTANCE)
//
//
//
//                Map<String, ProjectionInterface> sequenceProjectionMap = new HashMap<>()
//
//                for (File trackDataFile in files) {
////
//
////                    String sequenceFileName = trackDataFile.absolutePath.substring(trackDirectory.absolutePath.length(),trackDataFile.absolutePath.length()-"trackData.json".length()).replaceAll("/","")
//                    String sequenceFileName = getSequenceName(trackDataFile.absolutePath)
//
////
//
//                    JSONObject referenceJsonObject = new JSONObject(trackDataFile.text)
//
//                    // TODO: interpret the format properly
//                    DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()
//                    JSONArray coordinateReferenceJsonArray = referenceJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
//                    for (int coordIndex = 0; coordIndex < coordinateReferenceJsonArray.size(); ++coordIndex) {
//                        JSONArray coordinate = coordinateReferenceJsonArray.getJSONArray(coordIndex)
//                        TrackIndex trackIndex = trackMapperService.getIndices(organism.commonName, trackName, coordinate.getInt(0))
//                        // TODO: use enums to better track format
////                        if (coordinate.getInt(0) == 4) {
//                        if (trackIndex.hasChunk()) {
//                            // projecess the file lf-${coordIndex} instead
//                            File chunkFile = new File(trackDataFile.parent + "/lf-${coordIndex + 1}.json")
//                            JSONArray chunkReferenceJsonArray = new JSONArray(chunkFile.text)
//
//                            for (int chunkArrayIndex = 0; chunkArrayIndex < chunkReferenceJsonArray.size(); ++chunkArrayIndex) {
//                                JSONArray chunkArrayCoordinate = chunkReferenceJsonArray.getJSONArray(chunkArrayIndex)
//                                discontinuousProjection.addInterval(chunkArrayCoordinate.getInt(1), chunkArrayCoordinate.getInt(2), padding)
//                            }
//
//                        } else {
//                            discontinuousProjection.addInterval(coordinate.getInt(1), coordinate.getInt(2), padding)
//                        }
//                    }
//
//                    sequenceProjectionMap.put(sequenceFileName, discontinuousProjection)
//                }
//
//
//
//                projectionMap.put(trackObject.key, sequenceProjectionMap)
//            }
//        }
//
//    }

    /**
     *
     * TODO: do re-lookup
     *
     * If in trackList . . or lf-x
     *
     * If type is "mRNA", search for any subarrays including sublist in array
     * If type is "other transcript?", ??
     * If type is "exon", add Interval for min, max, else ginreo
     *
     * The "type" is listed in 0-> column 9
     * The "type" is listed in 1-> column 7
     * The "type" is listed in 2-> column 6
     * The "type" is listed in 3-> column 6
     * The "type" is listed in 4-> column ?
     *
     * // and in the sublist as well (typicallly column 11 of an mRNA . . prob for overlap
     *
     * @param organism
     * @param tracksArray
     * @return
     */
//    def createExonLevelProjection(Organism organism, JSONArray tracksArray, Integer padding) {
//        // TODO: this is only here for debugging . .
////        projectionMap.clear()
//        long startTime = System.currentTimeMillis()
//        for (int i = 0; i < tracksArray.size(); i++) {
//            JSONObject trackObject = tracksArray.getJSONObject(i)
//            if (trackObject.containsKey("OGS") && trackObject.getBoolean("OGS") && !projectionMap.containsKey(trackObject.key)) {
//
//                String jbrowseDirectory = organism.directory + "/tracks/" + trackObject.key
//                File trackDirectory = new File(jbrowseDirectory)
//
//
//                File[] files = FileUtils.listFiles(trackDirectory, FileFilterUtils.nameFileFilter("trackData.json"), TrueFileFilter.INSTANCE)
//
//
//
//                Map<String, ProjectionInterface> sequenceProjectionMap = new HashMap<>()
//
//                for (File trackDataFile in files) {
////                    String sequenceFileName = trackDataFile.absolutePath.substring(trackDirectory.absolutePath.length(),trackDataFile.absolutePath.length()-"trackData.json".length()).replaceAll("/","")
//                    String sequenceFileName = getSequenceName(trackDataFile.absolutePath)
//                    String trackName = getTrackName(trackDataFile.absolutePath)
//
//                    JSONObject referenceJsonObject = new JSONObject(trackDataFile.text)
//                    if (sequenceFileName.contains("1.10")) {
//
//                    }
//
//                    // TODO: interpret the format properly
//                    DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()
//                    JSONArray coordinateReferenceJsonArray = referenceJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
//                    for (int coordIndex = 0; coordIndex < coordinateReferenceJsonArray.size(); ++coordIndex) {
//
//                        // TODO: this needs to be recursive
//                        JSONArray coordinate = coordinateReferenceJsonArray.getJSONArray(coordIndex)
//                        TrackIndex trackIndex = trackMapperService.getIndices(organism.commonName, trackName, coordinate.getInt(0))
//
////                        if (coordinate.getInt(0) == 4) {
//                        if (trackIndex.hasChunk()) {
//                            // projecess the file lf-${coordIndex} instead
//                            File chunkFile = new File(trackDataFile.parent + "/lf-${coordIndex + 1}.json")
//                            JSONArray chunkReferenceJsonArray = new JSONArray(chunkFile.text)
//
//                            for (int chunkArrayIndex = 0; chunkArrayIndex < chunkReferenceJsonArray.size(); ++chunkArrayIndex) {
//                                JSONArray chunkArrayCoordinate = chunkReferenceJsonArray.getJSONArray(chunkArrayIndex)
//                                processHighLevelArray(discontinuousProjection, chunkArrayCoordinate, padding, organism, trackName)
//                            }
//
//                        } else {
//                            processHighLevelArray(discontinuousProjection, coordinate, padding, organism, trackName)
//                        }
//                    }
//
////
//
//                    if (sequenceFileName.contains("1.10")) {
//
//                    }
//                    sequenceProjectionMap.put(sequenceFileName, discontinuousProjection)
//                }
//
//
//
//                projectionMap.put(trackObject.key, sequenceProjectionMap)
//            }
//        }
//
//    }
//
//
//    def processHighLevelArray(DiscontinuousProjection discontinuousProjection, JSONArray coordinate, Integer padding, Organism organism, String trackName) {
////                        // TODO: use enums to better track format
//        TrackIndex trackIndex = trackMapperService.getIndices(organism.commonName, trackName, coordinate.getInt(0))
//        log.debug "processing high level array  ${coordinate as JSON}"
//        String featureType = coordinate.getString(trackIndex.type)
//        if (featureType.equalsIgnoreCase("exon")) {
//            processExonArray(discontinuousProjection, coordinate.getJSONArray(trackIndex.subFeaturesColumn), padding, organism, trackName)
//        }
////        switch (classType) {
////            case 0:
////                featureType = coordinate.getString(9)
////                // process array in 10
////                processExonArray(discontinuousProjection, coordinate.getJSONArray(10), padding)
////                // process sublist if 11 exists
////                break
////            case 1:
////                featureType = coordinate.getString(7)
////
////                // no subarrays
////                break
////            case 2:
////            case 3:
////                featureType = coordinate.getString(6)
////
////                // process array in 10
////                // process sublist if 11 exists
////                break
////            case 4:
////
////                // ignore .  . . not an exon
////                break
////        }
//
//    }
//
//    def processExonArray(DiscontinuousProjection discontinuousProjection, JSONArray coordinate, Integer padding, Organism organism, String trackName) {
//        log.debug "processing exon array ${coordinate as JSON}"
//        def classType = coordinate.get(0)
//        // then we assume that the rest are arrays if the first are . . and process them accordingly
//        if (classType instanceof JSONArray) {
//            for (int i = 0; i < coordinate.size(); i++) {
//                log.debug "subarray ${coordinate.get(i) as JSON}"
//                processExonArray(discontinuousProjection, coordinate.getJSONArray(i), padding, organism, trackName)
//            }
//            return
//        }
//
//        TrackIndex trackIndex = trackMapperService.getIndices(organism.commonName, trackName, coordinate.getInt(0))
//        log.debug "not sure if this will work . . check! ${coordinate.size()} > 9"
//        String featureType = coordinate.getString(trackIndex.type)
//        if (coordinate.size() >= 10) {
//            processExonArray(discontinuousProjection, coordinate.getJSONArray(10), padding, organism, trackName)
//        }
//        if (coordinate.size() >= 11) {
//            JSONObject sublist = coordinate.getJSONObject(11)
//            processHighLevelArray(discontinuousProjection, sublist.getJSONArray("Sublist"), padding, organism, trackName)
//        }
////                break
////        }
//
//        // TODO: or repeat region?
//        if (featureType && featureType.equalsIgnoreCase("exon")) {
//            discontinuousProjection.addInterval(coordinate.getInt(trackIndex.start), coordinate.getInt(trackIndex.end), padding)
//        }
//
//    }

    @NotTransactional
    JSONArray projectFeaturesArray(JSONArray inputFeaturesArray, DiscontinuousProjection projection, Boolean reverseProjection) {
        for (int i = 0; i < inputFeaturesArray.size(); i++) {
            JSONObject inputFeature = inputFeaturesArray.getJSONObject(i)
            projectFeature(inputFeature, projection, reverseProjection)
            if (inputFeature.has(FeatureStringEnum.CHILDREN.value)) {
                JSONArray childFeatures = inputFeature.getJSONArray(FeatureStringEnum.CHILDREN.value)
                projectFeaturesArray(childFeatures, projection, reverseProjection)
            }
        }
        return inputFeaturesArray
    }

    @NotTransactional
    JSONObject projectFeature(JSONObject inputFeature, DiscontinuousProjection projection, Boolean reverseProjection) {
        if (!inputFeature.has(FeatureStringEnum.LOCATION.value)) return inputFeature

        JSONObject locationObject = inputFeature.getJSONObject(FeatureStringEnum.LOCATION.value)
        
        Integer fmin = locationObject.has(FeatureStringEnum.FMIN.value) ? locationObject.getInt(FeatureStringEnum.FMIN.value) : null
        Integer fmax = locationObject.has(FeatureStringEnum.FMAX.value) ? locationObject.getInt(FeatureStringEnum.FMAX.value) : null
        
        if (reverseProjection) {
            fmin = fmin ? projection.projectReverseValue(fmin) : null
            fmax = fmax ? projection.projectReverseValue(fmax) : null
        } else {
            fmin = fmin ? projection.projectValue(fmin) : null
            fmax = fmax ? projection.projectValue(fmax) : null
        }
        
        if (fmin) {
            locationObject.put(FeatureStringEnum.FMIN.value, fmin)
        }
        if (fmax) {
            locationObject.put(FeatureStringEnum.FMAX.value, fmax)
        }
        return inputFeature
    }

//    @NotTransactional
    MultiSequenceProjection createMultiSequenceProjection(ProjectionDescription description) {
        List<Location> locationList = getLocationsForSequences(description)
        return createMultiSequenceProjection(description, locationList)
    }

//    @NotTransactional
//    List<Location> getLocationsForDescription(ProjectionDescription projectionDescription) {
//
//        switch (projectionDescription.projection.toUpperCase()) {
//            case "EXON":
//                return getExonLocations(projectionDescription)
//                break
//            case "TRANSCRIPT":
//                return getTranscriptLocations(projectionDescription.referenceTrack, projectionDescription.padding, projectionDescription)
//                break
//            case "NONE":
//            default:
//                return getSequenceLocations(projectionDescription)
//                break
////            default:
////                log.error "Not sure how we got here "
////                break
//        }
//    }

/**
     * TODO: Get transcript locations
     * @param referenceTracks
     * @param padding
     * @param projectionSequences
     * @return
     */
//    List<Location> getTranscriptLocations(List<String> referenceTrack, int padding, ProjectionDescription projectionDescription) {
//        return new ArrayList<Location>()
//    }

    /**
     * @return
     *
     * for each reference track  (for a given organism . . . read the track for the proper sequences
     */
//    List<Location> getExonLocations(ProjectionDescription projectionDescription) {
//        List<Location> locationList = new ArrayList<>()
//        Organism organism = Organism.findByCommonName(projectionDescription.organism)
//        List<String> referenceTracks = projectionDescription.referenceTrack
//        for (String track in referenceTracks) {
//            JSONArray tracksArray = loadTrackJson(track, organism, projectionDescription)
//            List<Location> exonLocations = createExonLocations(tracksArray, organism, track)
//            locationList.addAll(exonLocations)
//        }
//
//        return locationList
//    }

//    List<Location> createExonLocations(JSONArray jsonArray, Organism organism, String trackName) {
//        List<Location> locationList = new ArrayList<>()
//        for (int i = 0; i < jsonArray.size(); i++) {
//            JSONObject referenceJsonObject = jsonArray.getJSONObject(i)
//            JSONArray coordinateReferenceJsonArray = referenceJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
//
//            for (int coordIndex = 0; coordIndex < coordinateReferenceJsonArray.size(); ++coordIndex) {
//
//                JSONArray coordinate = coordinateReferenceJsonArray.getJSONArray(coordIndex)
//                TrackIndex trackIndex = trackMapperService.getIndices(organism.commonName, trackName, coordinate.getInt(0))
//
////                if (coordinate.getInt(0) == 4) {
//                if (trackIndex.hasChunk()) {
//                    // projecess the file lf-${coordIndex} instead
//                    File chunkFile = new File("${referenceJsonObject.directory}/lf-${coordIndex + 1}.json")
//                    JSONArray chunkReferenceJsonArray = new JSONArray(chunkFile.text)
//                    for (int chunkArrayIndex = 0; chunkArrayIndex < chunkReferenceJsonArray.size(); ++chunkArrayIndex) {
//                        JSONArray chunkArrayCoordinate = chunkReferenceJsonArray.getJSONArray(chunkArrayIndex)
//                        locationList.addAll(extractHighLevelLocations(chunkArrayCoordinate, organism, trackName))
//                    }
//
//                } else {
//                    List<Location> thisLocationList = extractHighLevelLocations(coordinate, organism, trackName)
//                    locationList.addAll(thisLocationList)
//                }
//            }
//        }
//        return locationList
//    }
//
//    JSONArray loadTrackJson(String referenceTrackName, Organism organism, ProjectionDescription projectionDescription) {
//        Map<String, ProjectionSequence> sequenceNames = new HashMap<>()
//        List<String> orderedSequences = new ArrayList<>()
//        projectionDescription.sequenceList.each {
//            sequenceNames.put(it.name, it)
//            orderedSequences.add(it.name)
//        }
//
//        JSONArray returnArray = new JSONArray()
//
//        String jbrowseDirectory = organism.directory + "/tracks/" + referenceTrackName
//        for (String sequenceName in orderedSequences) {
//            String fileName = jbrowseDirectory + "/" + sequenceName + "/trackData.json"
//            File trackDataFile = new File(fileName)
//            String sequenceFileName = getSequenceName(trackDataFile.absolutePath)
//            if (sequenceNames.containsKey(sequenceFileName)) {
//                JSONObject trackObject = new JSONObject(trackDataFile.text)
//                trackMapperService.storeTrack(organism.commonName,referenceTrackName,trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray("classes"))
//                trackObject.directory = trackDataFile.parent
//                trackObject.sequenceName = sequenceNames.get(sequenceFileName).toJSONObject()
//                returnArray.add(returnArray.size(), trackObject)
//            }
//        }
////        }
//        return returnArray
//    }

    /**
     * Create an interval for each "sequence" min/max
     * Create an "fold" for each fold / splitting the interval
     * @param projectionDescription
     * @return
     */
    List<Location> getLocationsForSequences(ProjectionDescription projectionDescription) {
        List<Location> locationList = new ArrayList<>()


        projectionDescription.sequenceList.each {
            locationList.add(new Location(min: it.start,max:it.end,sequence: it))
        }

//        // should just return one VERY big location
//        List<String> sequenceList = new ArrayList<>()
//        String organismName = projectionSequences.iterator().next().organism
//        projectionSequences.each {
//            sequenceList.add(it.name)
//        }
//        Organism organism = Organism.findByCommonName(organismName)
//        Map<String, Sequence> sequencMap = new TreeMap<>()
//        Sequence.findAllByNameInListAndOrganism(sequenceList, organism).each {
//            sequencMap.put(it.name, it)
//        }
//
//        projectionSequences.each {
//            Sequence sequence1 = sequencMap.get(it.name)
//            it.id = sequence1.id
//
////            ProjectionSequence projectionSequence1 = new ProjectionSequence(
////                  id:sequence1.id
////                    ,name:sequence1.name
////                    ,organism: organism
////                    ,order:locationList.size()
////            )
//            Location location = new Location(min: 0, max: sequence1.end, sequence: it)
//            locationList.add(location)
//        }



        return locationList
    }

//    List<Location> getSequenceLocations(ProjectionDescription projectionDescription) {
//        List<ProjectionSequence> projectionSequences = projectionDescription.sequenceList
//        // should just return one VERY big location
//        List<Location> locationList = new ArrayList<>()
//        List<String> sequenceList = new ArrayList<>()
//        String organismName = projectionSequences.iterator().next().organism
//        projectionSequences.each {
//            sequenceList.add(it.name)
//        }
//        Organism organism = Organism.findByCommonName(organismName)
//        Map<String, Sequence> sequencMap = new TreeMap<>()
//        Sequence.findAllByNameInListAndOrganism(sequenceList, organism).each {
//            sequencMap.put(it.name, it)
//        }
//
//        projectionSequences.each {
//            Sequence sequence1 = sequencMap.get(it.name)
//            it.id = sequence1.id
//
////            ProjectionSequence projectionSequence1 = new ProjectionSequence(
////                  id:sequence1.id
////                    ,name:sequence1.name
////                    ,organism: organism
////                    ,order:locationList.size()
////            )
//            Location location = new Location(min: 0, max: sequence1.end, sequence: it)
//            locationList.add(location)
//        }
//
//
//
//        return locationList
//    }

//    @NotTransactional
    MultiSequenceProjection createMultiSequenceProjection(ProjectionDescription description, List<Location> locationList) {
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection(projectionDescription: description)
        multiSequenceProjection.addLocations(locationList)
        multiSequenceProjection.calculateOffsets()
        Map<String,ProjectionSequence> projectionSequenceMap = [:]

        multiSequenceProjection.projectedSequences.each {
            projectionSequenceMap.put(it.name,it)
        }
//        List<String> sequenceNames = multiSequenceProjection.projectedSequences.name
        // TODO: speed this up by caching sequences
        Sequence.findAllByNameInList(projectionSequenceMap.keySet() as List<String>).each {
            def projectionSequence = projectionSequenceMap.get(it.name)
            projectionSequence.unprojectedLength = it.length
        }

        return multiSequenceProjection
    }

    List<Location> extractHighLevelLocations(JSONArray coordinate, Organism organism, String trackName) {
        TrackIndex trackIndex = trackMapperService.getIndices(organism.commonName, trackName, coordinate.getInt(0))

        log.debug "processing high level array  ${coordinate as JSON}"
        // process array in 10
        if (trackIndex.hasSubFeatures()) {
            List<Location> localExonArray = extractExonArrayLocations(coordinate.getJSONArray(trackIndex.subFeaturesColumn), organism, trackName)
            return localExonArray
        }
        return new ArrayList<Location>()

    }

    List<Location> extractExonArrayLocations(JSONArray coordinate, Organism organism, String trackName) {
        List<Location> locationList = new ArrayList<>()
        log.debug "processing exon array ${coordinate as JSON}"
        def classType = coordinate.get(0)

        // then we assume tht the rest are arrays if the first are . . and process them accordingly
        if (classType instanceof JSONArray) {
            for (int i = 0; i < coordinate.size(); i++) {
                log.debug "subarray ${coordinate.get(i) as JSON}"
                locationList.addAll(extractExonArrayLocations(coordinate.getJSONArray(i), organism, trackName))
            }
            return locationList
        }
        TrackIndex trackIndex = trackMapperService.getIndices(organism.commonName, trackName, coordinate.getInt(0))
        String featureType = coordinate.getString(trackIndex.type)
//        if (coordinate.size() >= 10) {
        if (trackIndex.hasSubFeatures()) {
            locationList.addAll(extractExonArrayLocations(coordinate.getJSONArray(trackIndex.subFeaturesColumn), organism, trackName))
        }
//        if (coordinate.size() >= 11) {
        if (trackIndex.hasChunk()) {
            JSONObject sublist = coordinate.getJSONObject(coordinate.size() - 1)
//                    locationList.addAll(extractHighLevelArrayLocations(discontinuousProjection, sublist.getJSONArray("Sublist"), projectionDescription))
            locationList.addAll(extractHighLevelLocations(sublist.getJSONArray("Sublist"), organism, trackName))
        }

        // TODO: or repeat region?
        if (featureType && featureType == "exon") {
            String sequenceName = coordinate.getString(trackIndex.seqId)
            ProjectionSequence projectionSequence1 = new ProjectionSequence(
                    name: sequenceName
                    ,organism: organism.commonName

            )
            locationList.add(
                    new Location(
                            min: coordinate.getInt(trackIndex.start)
                            , max: coordinate.getInt(trackIndex.end)-1 // the end is exclusive from track, we store inclusive
                            , sequence: projectionSequence1
                    )
            )
        }

        return locationList
    }

    JSONObject convertProjectionToBookmarkJsonObject(String putativeProjectionLoc, Organism organism) {
        JSONObject bookmarkJsonObject = JSON.parse(putativeProjectionLoc) as JSONObject
        bookmarkJsonObject.organism = organism.commonName
        return bookmarkJsonObject
    }

    /**
     * This is for the reference so this should always be null AFAIK
     *
     * @param organism
     * @param trackName
     * @param sequenceName
     * @return
     */
//    @NotTransactional
//    ProjectionInterface getProjection(Organism organism, String trackName, String sequenceName) {
//
////
//        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)
//        ProjectionDescription projectionDescription = new ProjectionDescription(
//                referenceTrack: [trackName]
//                , projection: "Exon"
//                , padding: 50
//        )
//        ProjectionSequence projectionSequence = new ProjectionSequence(
//                id: sequence.id
//                , name: sequence.name
//                , organism: organism.commonName
//        )
//
//        projectionDescription.sequenceList = [projectionSequence]
//        return getProjection(projectionDescription)
//    }

    def getProjection(String putativeProjectionLoc, Organism organism) {
        if (BookmarkService.isProjectionString(putativeProjectionLoc)) {
            JSONObject bookmarkJsonObject = convertProjectionToBookmarkJsonObject(putativeProjectionLoc, organism)
            return getProjection(bookmarkJsonObject)
        }
        return null
    }

    def isValidJson(String queryString) {
        try {
            if (JSON.parse(queryString)) {
                return true;
            }
        } catch (Exception e) {
            log.debug "Error parsing string: ${queryString}"
        }
        return false;
    }

    ProjectionDescription convertJsonObjectToProjectDescription(JSONObject bookmarkObject) {
        
        ProjectionDescription projectionDescription = new ProjectionDescription()

        projectionDescription.projection = bookmarkObject.projection ?: "NONE"
        projectionDescription.padding = bookmarkObject.padding ?: 0
        projectionDescription.organism = bookmarkObject.organism
        //projectionDescription.referenceTrack = [bookmarkObject.referenceTrack] as List<String>
//        projectionDescription.referenceTrack = new ArrayList<String>()
//        if (isValidJson((String) bookmarkObject.referenceTrack)) {
//            JSONArray referenceTrackJsonArray = JSON.parse(bookmarkObject.referenceTrack.toString()) as JSONArray
//
//            for (int i = 0; i < referenceTrackJsonArray.size(); i++) {
//                projectionDescription.referenceTrack.add(i, referenceTrackJsonArray.getString(i));
//            }
//        }
//        else {
//            projectionDescription.referenceTrack.add(bookmarkObject.referenceTrack)
//        }

        projectionDescription.sequenceList = new ArrayList<>()


        // TODO: reference / ?
        for (int i = 0; i < bookmarkObject.sequenceList.size(); i++) {
            JSONObject bookmarkSequence = bookmarkObject.sequenceList.getJSONObject(i)
            ProjectionSequence projectionSequence1 = new ProjectionSequence()
            if(bookmarkSequence.start==null){
                Sequence sequence = Sequence.findByName(bookmarkSequence.name)
                projectionSequence1.start = sequence.start
                projectionSequence1.end = sequence.end
            }
            else{
                projectionSequence1.start = bookmarkSequence.start
                projectionSequence1.end = bookmarkSequence.end
            }
            projectionSequence1.setOrder(i)
            projectionSequence1.setName(bookmarkSequence.name)
            projectionSequence1.setOrganism(projectionDescription.organism)

            JSONArray featureArray = bookmarkSequence.features
            List<String> features = new ArrayList<>()
            for (int j = 0; featureArray != null && j < featureArray.size(); j++) {
                features.add(featureArray.getString(j))
            }
            projectionSequence1.setFeatures(features)
            projectionDescription.sequenceList.add(projectionSequence1)
        }
        return projectionDescription
    }

    MultiSequenceProjection getProjection(Bookmark bookmark) {
        JSONObject jsonObject = bookmarkService.convertBookmarkToJson(bookmark)
        return getProjection(jsonObject)
    }
/**
 * TODO:
 * looks up bookmarks based on Ids'
 * Creates a "Projection Description" based on Id's . . .
 * And caches it locally . . .
 *
 * TODO: remove this method?
 * (probably a MultiSequencProjection)
 *
 *{{projection:None},{padding:50},{sequenceLists:[{name:'Group1.1',features:[GB42145-RA]}]}%3A-1..-1
 *
 * @param bookmarkArray
 * @return
 */
    MultiSequenceProjection getProjection(JSONObject bookmarkObject) {
        ProjectionDescription projectionDescription = convertJsonObjectToProjectDescription(bookmarkObject)
        return createMultiSequenceProjection(projectionDescription)
//        ProjectionDescription projectionDescription = convertJsonObjectToProjectDescription(bookmarkObject)
//        if (true || !multiSequenceProjectionMap.containsKey(projectionDescription)) {
//            MultiSequenceProjection multiSequenceProjection = createMultiSequenceProjection(projectionDescription)
//            multiSequenceProjectionMap.put(projectionDescription, multiSequenceProjection)
//        }
//        return multiSequenceProjectionMap.get(projectionDescription)
    }

//    Boolean containsSequence(Map<ProjectionSequence, MultiSequenceProjection> projectionSequenceMultiSequenceProjectionMap, String sequenceName, Long sequenceId, Organism currentOrganism) {
//        ProjectionSequence projectionSequence = new ProjectionSequence(
//                id: sequenceId
//                , name: sequenceName
//                , organism: currentOrganism.commonName
//        )
//        // this guarantees that the query is local to the descrption
//        return projectionSequenceMultiSequenceProjectionMap.containsKey(projectionSequence)
//
//    }

//    def storeProjection(String putativeRefererLocation, MultiSequenceProjection multiSequenceProjection, Organism organism) {
//        JSONObject bookmarkObject = convertProjectionToBookmarkJsonObject(putativeRefererLocation, organism)
//        ProjectionDescription projectionDescription = convertJsonObjectToProjectDescription(bookmarkObject)
////        multiSequenceProjectionMap.put(projectionDescription, multiSequenceProjection)
//    }

//    def clearProjections() {
//        multiSequenceProjectionMap.clear()
//    }
}
