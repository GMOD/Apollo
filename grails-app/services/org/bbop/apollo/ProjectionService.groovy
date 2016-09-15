package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.*
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class ProjectionService {

    def assemblageService
    def trackMapperService
    def permissionService

//    private Map<String, Map<String, ProjectionInterface>> projectionMap = new HashMap<>()

//    private Map<ProjectionDescription, MultiSequenceProjection> multiSequenceProjectionMap = new HashMap<>()
    private Map<String, MultiSequenceProjection> multiSequenceProjectionMap = new HashMap<>()


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

    MultiSequenceProjection createMultiSequenceProjection(Assemblage assemblage) {
        List<Location> locationList = getLocationsFromAssemblage(assemblage)
        return createMultiSequenceProjection(assemblage,locationList)
    }

//    @NotTransactional
//    /**
//     * @deprecated
//     * @param description
//     * @return
//     */
//    MultiSequenceProjection createMultiSequenceProjection(ProjectionDescription description) {
//        List<Location> locationList = getLocationsForSequences(description)
//        return createMultiSequenceProjection(description, locationList)
//    }

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

    ProjectionSequence convertJsonToProjectionSequence(JSONObject jSONObject, int index, Assemblage assemblage){
        ProjectionSequence projectionSequence = new ProjectionSequence()
        if(jSONObject.start==null){
            Sequence sequence = Sequence.findByName(jSONObject.name)
            projectionSequence.start = sequence.start
            projectionSequence.end = sequence.end
            projectionSequence.unprojectedLength = sequence.length
        }
        else{
            projectionSequence.start = jSONObject.start
            projectionSequence.end = jSONObject.end
            projectionSequence.unprojectedLength = jSONObject.length
        }
        projectionSequence.order = index
        projectionSequence.name = jSONObject.name
        projectionSequence.organism = assemblage.organism.commonName

        JSONArray featureArray = jSONObject.features
        List<String> features = new ArrayList<>()
        for (int j = 0; featureArray != null && j < featureArray.size(); j++) {
            features.add(featureArray.getString(j))
        }
        projectionSequence.setFeatures(features)

        return projectionSequence
    }

    List<Location> getLocationsFromAssemblage(Assemblage assemblage) {
        List<Location> locationList = new ArrayList<>()


        JSONArray sequencListArray = JSON.parse(assemblage.sequenceList) as JSONArray

        sequencListArray.eachWithIndex { JSONObject it , int i ->
            ProjectionSequence projectionSequence = convertJsonToProjectionSequence(it,i,assemblage)
            locationList.add(new Location(min: it.start, max: it.end, sequence: projectionSequence))
        }

        return locationList
    }

//    /**
//     * @deprecated
//     * Create an interval for each "sequence" min/max
//     * Create an "fold" for each fold / splitting the interval
//     * @param projectionDescription
//     * @return
//     */
//    List<Location> getLocationsForSequences(ProjectionDescription projectionDescription) {
//        List<Location> locationList = new ArrayList<>()
//
//
//        projectionDescription.sequenceList.each {
//            locationList.add(new Location(min: it.start,max:it.end,sequence: it))
//        }
//
////        // should just return one VERY big location
////        List<String> sequenceList = new ArrayList<>()
////        String organismName = projectionSequences.iterator().next().organism
////        projectionSequences.each {
////            sequenceList.add(it.name)
////        }
////        Organism organism = Organism.findByCommonName(organismName)
////        Map<String, Sequence> sequencMap = new TreeMap<>()
////        Sequence.findAllByNameInListAndOrganism(sequenceList, organism).each {
////            sequencMap.put(it.name, it)
////        }
////
////        projectionSequences.each {
////            Sequence sequence1 = sequencMap.get(it.name)
////            it.id = sequence1.id
////
//////            ProjectionSequence projectionSequence1 = new ProjectionSequence(
//////                  id:sequence1.id
//////                    ,name:sequence1.name
//////                    ,organism: organism
//////                    ,order:locationList.size()
//////            )
////            Location location = new Location(min: 0, max: sequence1.end, sequence: it)
////            locationList.add(location)
////        }
//
//
//
//        return locationList
//    }

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

    MultiSequenceProjection createMultiSequenceProjection(Assemblage assemblage, List<Location> locationList) {

        List<ProjectionSequence> projectionSequenceList = new ArrayList<>()
        (JSON.parse(assemblage.sequenceList) as JSONArray).eachWithIndex { JSONObject it, int i ->
            ProjectionSequence projectionSequence = convertJsonToProjectionSequence(it,i,assemblage)

            projectionSequenceList.add(projectionSequence)
        }

        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
//        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection(sequenceList: projectionSequenceList)
        multiSequenceProjection.addProjectionSequences(projectionSequenceList)
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


//    MultiSequenceProjection createMultiSequenceProjection(ProjectionDescription description, List<Location> locationList) {
//        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection(projectionDescription: description)
//        multiSequenceProjection.addLocations(locationList)
//        multiSequenceProjection.calculateOffsets()
//        Map<String,ProjectionSequence> projectionSequenceMap = [:]
//
//        multiSequenceProjection.projectedSequences.each {
//            projectionSequenceMap.put(it.name,it)
//        }
////        List<String> sequenceNames = multiSequenceProjection.projectedSequences.name
//        // TODO: speed this up by caching sequences
//        Sequence.findAllByNameInList(projectionSequenceMap.keySet() as List<String>).each {
//            def projectionSequence = projectionSequenceMap.get(it.name)
//            projectionSequence.unprojectedLength = it.length
//        }
//
//        return multiSequenceProjection
//    }

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

    JSONObject convertProjectionToAssemblageJsonObject(String putativeProjectionLoc, Organism organism) {
        JSONObject assemblageJsonObject = JSON.parse(putativeProjectionLoc) as JSONObject
        assemblageJsonObject.organism = organism.commonName
        return assemblageJsonObject
    }


    /**
     * Has to be transactional as it might create a assemblage.
     * @param putativeProjectionLoc
     * @param organism
     * @return
     */
    @Transactional
    def getProjection(String putativeProjectionLoc, Organism organism) {
        if (AssemblageService.isProjectionString(putativeProjectionLoc)) {
            JSONObject assemblageJsonObject = convertProjectionToAssemblageJsonObject(putativeProjectionLoc, organism)
            return getProjection(assemblageJsonObject)
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

    /**
     * @deprecated  Use the createMultisequenceProjection method instead
     * @param assemblage
     * @return
     */
    MultiSequenceProjection getProjection(Assemblage assemblage) {
        JSONObject jsonObject = assemblageService.convertAssemblageToJson(assemblage)
        return getProjection(jsonObject)
    }
/**
 * TODO:
 * looks up assemblages based on Ids'
 * Creates a "Projection Description" based on Id's . . .
 * And caches it locally . . .
 *
 * TODO: remove this method?
 * (probably a MultiSequencProjection)
 *
 *{{projection:None},{padding:50},{sequenceLists:[{name:'Group1.1',features:[GB42145-RA]}]}%3A-1..-1
 *
 * @param assemblageArray
 * @return
 */
    @Transactional
    MultiSequenceProjection getProjection(JSONObject assemblageObject) {
        Assemblage assemblage = assemblageService.convertJsonToAssemblage(assemblageObject)
        return createMultiSequenceProjection(assemblage)
    }


    /**
     * We want the minimimum location of a feature in the context of its projection.
     *
     * So should be fmin + offset (all previous lengths) - start
     * @param feature
     * @param assemblage
     * @return
     */
    Integer getMinForFeatureInProjection(Feature feature, MultiSequenceProjection multiSequenceProjection) {
        FeatureLocation firstFeatureLocation = feature.firstFeatureLocation
        String firstSequenceName = firstFeatureLocation.sequence.name
        ProjectionSequence firstProjectionSequence = multiSequenceProjection.projectedSequences.find(){
            it.name == firstSequenceName
        }

        Integer calculatedMin = firstProjectionSequence.offset - firstProjectionSequence.start + firstFeatureLocation.fmin
        return calculatedMin

    }

    /**
     * We want the maximum location of a feature in the context of its projection
     *
     * So should be fmax (of the last sequence) + offset (all previous lengths) - start
     * @param feature
     * @param assemblage
     * @return
     */

    Integer getMaxForFeatureInProjection(Feature feature, MultiSequenceProjection multiSequenceProjection) {
        FeatureLocation lastFeatureLocation = feature.lastFeatureLocation
        String lasttSequenceName = lastFeatureLocation.sequence.name
        ProjectionSequence lastProjectionSequence = multiSequenceProjection.projectedSequences.find(){
            it.name == lasttSequenceName
        }

        Integer calculatedMax = lastProjectionSequence.offset - lastProjectionSequence.start + lastFeatureLocation.fmax
        return calculatedMax
    }

    @NotTransactional
    JSONArray getSequenceListJSON(String inputString){
        JSONObject jsonObject = JSON.parse(inputString) as JSONObject
        return jsonObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value)
    }


    // TODO: constant / read-only, so could always move to a database cache
    @NotTransactional
    def cacheProjection(String projectionString, MultiSequenceProjection multiSequenceProjection) {
        multiSequenceProjectionMap.put(getSequenceListJSON(projectionString),multiSequenceProjection)
    }

    @NotTransactional
    MultiSequenceProjection getCachedProjection(String projectionString) {
        return multiSequenceProjectionMap.get(getSequenceListJSON(projectionString))
    }
}
