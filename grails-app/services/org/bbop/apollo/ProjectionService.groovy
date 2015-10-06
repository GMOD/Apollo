package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.DiscontinuousProjection
import org.bbop.apollo.projection.Location
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionDescription
import org.bbop.apollo.projection.ProjectionInterface
import org.bbop.apollo.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class ProjectionService {

    def grailsApplication

    // TODO: move to database as JSON
    // track, sequence, projection
    // TODO: should also include organism at some point as well
    // TODO: just turn this into a cache file
    private Map<String, Map<String, ProjectionInterface>> projectionMap = new HashMap<>()

    // description is how the projection was created
//    private Map<ProjectionDescription, Map<ProjectionSequence, MultiSequenceProjection>> multiSequenceProjectionMap = new HashMap<>()
    private Map<ProjectionDescription, MultiSequenceProjection> multiSequenceProjectionMap = new HashMap<>()

    // TODO: should do an actual lookup / query in cache and DB
    @NotTransactional
    Boolean hasProjection(Organism organism, String trackName) {
        return projectionMap.size() > 0
    }

    // TODO: do re-lookup
    /**
     *
     * @param organism
     * @param trackName TODO: this is the REFERENCE track!! .. might be too specific
     * @param sequenceName
     * @return
     */
    @NotTransactional
    ProjectionInterface getProjection(Organism organism, String trackName, String sequenceName) {

        if (!grailsApplication.config.apollo.doProjection) {
            return null
        }

        if (grailsApplication.config.apollo.useMultiSequence) {
            Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)
            ProjectionDescription projectionDescription = new ProjectionDescription(
                    referenceTracks: [trackName]
                    , type: "EXON"
                    , padding: 50
            )
            ProjectionSequence projectionSequence = new ProjectionSequence(
                    id: sequence.id
                    , name: sequence.name
                    , organism: organism.commonName
            )

            projectionDescription.sequenceList = [projectionSequence]

//            return getMultiSequenceProjection(projectionDescription, projectionSequence)
            return multiSequenceProjectionMap.get(projectionDescription)

        } else {
            // TODO . . . not if that is correct . . . or even used
            return projectionMap ? projectionMap.values()?.iterator()?.next()?.get(sequenceName) : null
        }

    }


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
    def createTranscriptProjection(Organism organism, JSONArray tracksArray, Integer padding = 0) {
        // TODO: this is only here for debugging . .
        projectionMap.clear()
        long startTime = System.currentTimeMillis()
        for (int i = 0; i < tracksArray.size(); i++) {
            JSONObject trackObject = tracksArray.getJSONObject(i)
            if (trackObject.containsKey("OGS") && trackObject.getBoolean("OGS") && !projectionMap.containsKey(trackObject.keys())) {
                println "tring to generate projection for ${trackObject.key}"
                String jbrowseDirectory = organism.directory + "/tracks/" + trackObject.key
                File trackDirectory = new File(jbrowseDirectory)
                println "track directory ${trackDirectory.absolutePath}"

                File[] files = FileUtils.listFiles(trackDirectory, FileFilterUtils.nameFileFilter("trackData.json"), TrueFileFilter.INSTANCE)

                println "# of files ${files.length}"

                Map<String, ProjectionInterface> sequenceProjectionMap = new HashMap<>()

                for (File trackDataFile in files) {
//                    println "file ${trackDataFile.absolutePath}"

//                    String sequenceFileName = trackDataFile.absolutePath.substring(trackDirectory.absolutePath.length(),trackDataFile.absolutePath.length()-"trackData.json".length()).replaceAll("/","")
                    String sequenceFileName = getSequenceName(trackDataFile.absolutePath)

//                    println "sequencefileName [${sequenceFileName}]"

                    JSONObject referenceJsonObject = new JSONObject(trackDataFile.text)

                    // TODO: interpret the format properly
                    DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()
                    JSONArray coordinateReferenceJsonArray = referenceJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
                    for (int coordIndex = 0; coordIndex < coordinateReferenceJsonArray.size(); ++coordIndex) {
                        JSONArray coordinate = coordinateReferenceJsonArray.getJSONArray(coordIndex)
                        // TODO: use enums to better track format
                        if (coordinate.getInt(0) == 4) {
                            // projecess the file lf-${coordIndex} instead
                            File chunkFile = new File(trackDataFile.parent + "/lf-${coordIndex + 1}.json")
                            JSONArray chunkReferenceJsonArray = new JSONArray(chunkFile.text)

                            for (int chunkArrayIndex = 0; chunkArrayIndex < chunkReferenceJsonArray.size(); ++chunkArrayIndex) {
                                JSONArray chunkArrayCoordinate = chunkReferenceJsonArray.getJSONArray(chunkArrayIndex)
                                discontinuousProjection.addInterval(chunkArrayCoordinate.getInt(1), chunkArrayCoordinate.getInt(2), padding)
                            }

                        } else {
                            discontinuousProjection.addInterval(coordinate.getInt(1), coordinate.getInt(2), padding)
                        }
                    }

                    sequenceProjectionMap.put(sequenceFileName, discontinuousProjection)
                }

                println "final size: ${trackObject.key} -> ${sequenceProjectionMap.size()}"

                projectionMap.put(trackObject.key, sequenceProjectionMap)
            }
        }
        println "total time ${System.currentTimeMillis() - startTime}"
    }

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
    def createExonLevelProjection(Organism organism, JSONArray tracksArray, Integer padding = 0) {
        // TODO: this is only here for debugging . .
//        projectionMap.clear()
        long startTime = System.currentTimeMillis()
        for (int i = 0; i < tracksArray.size(); i++) {
            JSONObject trackObject = tracksArray.getJSONObject(i)
            if (trackObject.containsKey("OGS") && trackObject.getBoolean("OGS") && !projectionMap.containsKey(trackObject.key)) {
                println "tring to generate projection for ${trackObject.key}"
                String jbrowseDirectory = organism.directory + "/tracks/" + trackObject.key
                File trackDirectory = new File(jbrowseDirectory)
                println "track directory ${trackDirectory.absolutePath}"

                File[] files = FileUtils.listFiles(trackDirectory, FileFilterUtils.nameFileFilter("trackData.json"), TrueFileFilter.INSTANCE)

                println "# of files ${files.length}"

                Map<String, ProjectionInterface> sequenceProjectionMap = new HashMap<>()

                for (File trackDataFile in files) {
//                    String sequenceFileName = trackDataFile.absolutePath.substring(trackDirectory.absolutePath.length(),trackDataFile.absolutePath.length()-"trackData.json".length()).replaceAll("/","")
                    String sequenceFileName = getSequenceName(trackDataFile.absolutePath)

                    JSONObject referenceJsonObject = new JSONObject(trackDataFile.text)
                    if (sequenceFileName.contains("1.10")) {
                        println "traying to create a sequence for ${sequenceFileName} "
                    }

                    // TODO: interpret the format properly
                    DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()
                    JSONArray coordinateReferenceJsonArray = referenceJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
                    for (int coordIndex = 0; coordIndex < coordinateReferenceJsonArray.size(); ++coordIndex) {

                        // TODO: this needs to be recursive
                        JSONArray coordinate = coordinateReferenceJsonArray.getJSONArray(coordIndex)

                        if (coordinate.getInt(0) == 4) {
                            // projecess the file lf-${coordIndex} instead
                            File chunkFile = new File(trackDataFile.parent + "/lf-${coordIndex + 1}.json")
                            JSONArray chunkReferenceJsonArray = new JSONArray(chunkFile.text)

                            for (int chunkArrayIndex = 0; chunkArrayIndex < chunkReferenceJsonArray.size(); ++chunkArrayIndex) {
                                JSONArray chunkArrayCoordinate = chunkReferenceJsonArray.getJSONArray(chunkArrayIndex)
                                processHighLevelArray(discontinuousProjection, chunkArrayCoordinate, padding)
                            }

                        } else {
                            processHighLevelArray(discontinuousProjection, coordinate, padding)
                        }
                    }

//                    println "# of entries: ${discontinuousProjection.minMap.size()}"

                    if (sequenceFileName.contains("1.10")) {
                        println "putting map ${sequenceFileName} into ${discontinuousProjection.size()}"
                    }
                    sequenceProjectionMap.put(sequenceFileName, discontinuousProjection)
                }

                println "final size: ${trackObject.key} -> ${sequenceProjectionMap.size()}"

                projectionMap.put(trackObject.key, sequenceProjectionMap)
            }
        }
        println "total time ${System.currentTimeMillis() - startTime}"
    }


    def processHighLevelArray(DiscontinuousProjection discontinuousProjection, JSONArray coordinate, Integer padding) {
//                        // TODO: use enums to better track format
        int classType = coordinate.getInt(0)
        log.debug "processing high level array  ${coordinate as JSON}"
        String featureType
        switch (classType) {
            case 0:
                featureType = coordinate.getString(9)
                // process array in 10
                processExonArray(discontinuousProjection, coordinate.getJSONArray(10), padding)
                // process sublist if 11 exists
                break
            case 1:
                featureType = coordinate.getString(7)
                println "1 - doing nothing for this . . . no subarray? ${coordinate as JSON}"
                // no subarrays
                break
            case 2:
            case 3:
                featureType = coordinate.getString(6)
                println "2/3 - doing nothing for this . . . no subarray? ${coordinate as JSON}"
                // process array in 10
                // process sublist if 11 exists
                break
            case 4:
                println "not sure how to handle case 4 ${coordinate as JSON}"
                // ignore .  . . not an exon
                break
        }

    }

    def processExonArray(DiscontinuousProjection discontinuousProjection, JSONArray coordinate, Integer padding) {
        log.debug "processing exon array ${coordinate as JSON}"
        def classType = coordinate.get(0)

        // then we assume that the rest are arrays if the first are . . and process them accordingly
        if (classType instanceof JSONArray) {
            for (int i = 0; i < coordinate.size(); i++) {
                log.debug "subarray ${coordinate.get(i) as JSON}"
                processExonArray(discontinuousProjection, coordinate.getJSONArray(i), padding)
            }
            return
        } else {
            // integer
            classType = coordinate.getInt(0)
        }
        String featureType
        switch (classType) {
            case 0:
                log.debug "not sure if this will work . . check! ${coordinate.size()} > 9"
                featureType = coordinate.getString(9)
                if (coordinate.size() >= 10) {
                    processExonArray(discontinuousProjection, coordinate.getJSONArray(10), padding)
                }
                if (coordinate.size() >= 11) {
                    JSONObject sublist = coordinate.getJSONObject(11)
                    processHighLevelArray(discontinuousProjection, sublist.getJSONArray("Sublist"), padding)
                }
                break
            case 1:
            case 2:
                featureType = coordinate.getString(7)
                break
            case 3:
                featureType = coordinate.getString(6)
                break
            case 4:
                println "not sure how to handle case 4 ${coordinate as JSON}"
                break
        }

        // TODO: or repeat region?
        if (featureType && featureType == "exon") {
            discontinuousProjection.addInterval(coordinate.getInt(1), coordinate.getInt(2), padding)
        }

//        }

    }

    /**
     * Anything in this space is assumed to be visible
     * @param sequence
     * @param referenceTrackName
     * @param inputFeaturesArray
     * @return
     */
    @Transactional(readOnly = true)
    JSONArray projectFeatures(Sequence sequence, String referenceTrackName, JSONArray inputFeaturesArray, Boolean reverseProjection) {
        DiscontinuousProjection projection = (DiscontinuousProjection) getProjection(sequence.organism, referenceTrackName, sequence.name)
        println "trying to convert ${inputFeaturesArray as JSON}"
        if (projection) {
            // process location . . .
            projectFeaturesArray(inputFeaturesArray, projection, reverseProjection)
            println "converted ${inputFeaturesArray as JSON}"
        } else {
            println "no conversion?? "
        }
        return inputFeaturesArray
    }

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
        println "loaction object ${locationObject as JSON}"
        Integer fmin = locationObject.has(FeatureStringEnum.FMIN.value) ? locationObject.getInt(FeatureStringEnum.FMIN.value) : null
        Integer fmax = locationObject.has(FeatureStringEnum.FMAX.value) ? locationObject.getInt(FeatureStringEnum.FMAX.value) : null
        println "old values ${fmin}-${fmax}"
        if (reverseProjection) {
            fmin = fmin ? projection.projectReverseValue(fmin) : null
            fmax = fmax ? projection.projectReverseValue(fmax) : null
        } else {
            fmin = fmin ? projection.projectValue(fmin) : null
            fmax = fmax ? projection.projectValue(fmax) : null
        }
        println "new values ${fmin}-${fmax}"
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
        List<Location> locationList = getLocationsForDescription(description)
        return createMultiSequenceProjection(description, locationList)
    }

//    @NotTransactional
    List<Location> getLocationsForDescription(ProjectionDescription projectionDescription) {

        switch (projectionDescription.type.toUpperCase()) {
            case "EXON":
                return getExonLocations(projectionDescription.referenceTracks, projectionDescription.padding, projectionDescription)
                break
            case "TRANSCRIPT":
                return getTranscriptLocations(projectionDescription.referenceTracks, projectionDescription.padding, projectionDescription)
                break
            case "NONE":
                return getSequenceLocations(projectionDescription)
                break
            default:
                log.error "Not sure how we got here "
                break
        }
    }

    /**
     * TODO:
     * @param referenceTracks
     * @param padding
     * @param projectionSequences
     * @return
     */
    List<Location> getTranscriptLocations(List<String> referenceTracks, int padding, ProjectionDescription projectionDescription) {
        return new ArrayList<Location>()
    }

    /**
     * TODO:
     * @param referenceTracks
     * @param padding
     * @param projectionSequences
     * @return
     *
     * for each reference track  (for a given organism . . . read the track for the proper sequences
     */
    List<Location> getExonLocations(List<String> referenceTracks, ProjectionDescription projectionDescription) {
        List<Location> locationList = new ArrayList<>()
        Organism organism  = Organism.findByCommonName(projectionDescription.organism)
        for (String track in referenceTracks) {
            JSONArray tracksArray = loadTrackJson(track,organism,projectionDescription)
            locationList.addAll(createExonLocations(tracksArray,projectionDescription))
        }

        return locationList
    }

    List<Location> createExonLocations(JSONArray jsonArray,ProjectionDescription projectionDescription) {
        List<Location> locationList = new ArrayList<>()
        for(int i = 0 ; i < jsonArray.size() ; i++){
            JSONObject referenceJsonObject = jsonArray.getJSONObject(i)
            JSONArray coordinateReferenceJsonArray = referenceJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

            for (int coordIndex = 0; coordIndex < coordinateReferenceJsonArray.size(); ++coordIndex) {

                JSONArray coordinate = coordinateReferenceJsonArray.getJSONArray(coordIndex)

                if (coordinate.getInt(0) == 4) {
                    // projecess the file lf-${coordIndex} instead
                    File chunkFile = new File("${referenceJsonObject.directory}/lf-${coordIndex + 1}.json")
                    JSONArray chunkReferenceJsonArray = new JSONArray(chunkFile.text)

                    for (int chunkArrayIndex = 0; chunkArrayIndex < chunkReferenceJsonArray.size(); ++chunkArrayIndex) {
                        JSONArray chunkArrayCoordinate = chunkReferenceJsonArray.getJSONArray(chunkArrayIndex)
//                        locationList.addAll(extractHighLevelArrayLocations(discontinuousProjection, chunkArrayCoordinate, projectionDescription))
                        locationList.addAll(extractHighLevelLocations( chunkArrayCoordinate, projectionDescription))
//                                processHighLevelArray(discontinuousProjection, chunkArrayCoordinate,padding)
                    }

                } else {
//                    List<Location> thisLocationList = extractHighLevelArrayLocations(discontinuousProjection, coordinate, projectionDescription)
                    List<Location> thisLocationList = extractHighLevelLocations( coordinate, projectionDescription)
                    locationList.addAll(thisLocationList)
//                            processHighLevelArray(discontinuousProjection, coordinate,padding)
                }
            }

//                    println "# of entries: ${discontinuousProjection.minMap.size()}"

//            if (sequenceFileName.contains("1.10")) {
//                println "putting map ${sequenceFileName} into ${discontinuousProjection.size()}"
//            }

        }
    }

    JSONArray loadTrackJson(String referenceTrackName, Organism organism, ProjectionDescription projectionDescription) {
        Map<String,ProjectionSequence> sequenceNames = new HashMap<>()
        projectionDescription.sequenceList.each {
            sequenceNames.put(it.name,it)
        }
        JSONArray returnArray = new JSONArray()

        String jbrowseDirectory = organism.directory + "/tracks/" + referenceTrackName
        File trackDirectory = new File(jbrowseDirectory)
        File[] files = FileUtils.listFiles(trackDirectory, FileFilterUtils.nameFileFilter("trackData.json"), TrueFileFilter.INSTANCE)
        for(File trackDataFile in files){
            String sequenceFileName = getSequenceName(trackDataFile.absolutePath)
            if(sequenceNames.containsKey(sequenceFileName)){
                JSONObject trackObject = new JSONObject(trackDataFile.text)
                trackObject.directory = trackDataFile.parent
                trackObject.sequenceName = sequenceNames.get(sequenceFileName) as JSONObject
                returnArray.add(trackObject)
            }
        }
        return returnArray
    }

    List<Location> getSequenceLocations(ProjectionDescription projectionDescription) {
        List<ProjectionSequence> projectionSequences = projectionDescription.sequenceList
        // should just return one VERY big location
        List<Location> locationList = new ArrayList<>()
        List<String> sequenceList = new ArrayList<>()
        String organismName = projectionSequences.iterator().next().organism
        projectionSequences.each {
            sequenceList.add(it.name)
        }
        Organism organism = Organism.findByCommonName(organismName)
        Map<String, Sequence> sequencMap = new TreeMap<>()
        Sequence.findAllByNameInListAndOrganism(sequenceList, organism).each {
            sequencMap.put(it.name, it)
        }

        projectionSequences.each {
            Sequence sequence1 = sequencMap.get(it.name)
            it.id = sequence1.id

//            ProjectionSequence projectionSequence1 = new ProjectionSequence(
//                  id:sequence1.id
//                    ,name:sequence1.name
//                    ,organism: organism
//                    ,order:locationList.size()
//            )
            Location location = new Location(min: 0, max: sequence1.end, sequence: it)
            locationList.add(location)
        }



        return locationList
    }

    @NotTransactional
    MultiSequenceProjection createMultiSequenceProjection(ProjectionDescription description, List<Location> locationList) {
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection(projectionDescription: description)
        multiSequenceProjection.addLocations(locationList)
        return multiSequenceProjection
    }

    List<Location> extractHighLevelLocations(JSONArray coordinate, ProjectionDescription projectionDescription) {
        int classType = coordinate.getInt(0)
        log.debug "processing high level array  ${coordinate as JSON}"
        String featureType
        switch (classType) {
            case 0:
                featureType = coordinate.getString(9)
                // process array in 10
                List<Location> localExonArray = extractExonArrayLocations( coordinate.getJSONArray(10), projectionDescription)
                return localExonArray
                // process sublist if 11 exists
                break
            case 1:
                featureType = coordinate.getString(7)
                println "1 - doing nothing for this . . . no subarray? ${coordinate as JSON}"
                // no subarrays
                break
            case 2:
            case 3:
                featureType = coordinate.getString(6)
                println "2/3 - doing nothing for this . . . no subarray? ${coordinate as JSON}"
                // process array in 10
                // process sublist if 11 exists
                break
            case 4:
                println "not sure how to handle case 4 ${coordinate as JSON}"
                // ignore .  . . not an exon
                break
        }

        return new ArrayList<Location>()

    }

    List<Location> extractExonArrayLocations(JSONArray coordinate, ProjectionDescription projectionDescription) {
        List<Location> locationList = new ArrayList<>()
        log.debug "processing exon array ${coordinate as JSON}"
        def classType = coordinate.get(0)

        // then we assume tht the rest are arrays if the first are . . and process them accordingly
        if (classType instanceof JSONArray) {
            for (int i = 0; i < coordinate.size(); i++) {
                log.debug "subarray ${coordinate.get(i) as JSON}"
                locationList.addAll(extractExonArrayLocations( coordinate.getJSONArray(i), projectionDescription))
            }
            return locationList
        } else {
            // integer
            classType = coordinate.getInt(0)
        }
        String featureType
        switch (classType) {
            case 0:
                log.debug "not sure if this will work . . check! ${coordinate.size()} > 9"
                featureType = coordinate.getString(9)
                if (coordinate.size() >= 10) {
                    locationList.addAll(extractExonArrayLocations(coordinate.getJSONArray(10), projectionDescription))
                }
                if (coordinate.size() >= 11) {
                    JSONObject sublist = coordinate.getJSONObject(11)
//                    locationList.addAll(extractHighLevelArrayLocations(discontinuousProjection, sublist.getJSONArray("Sublist"), projectionDescription))
                    locationList.addAll(extractHighLevelLocations( sublist.getJSONArray("Sublist"), projectionDescription))
                }
                break
            case 1:
            case 2:
                featureType = coordinate.getString(7)
                break
            case 3:
                featureType = coordinate.getString(6)
                break
            case 4:
                println "not sure how to handle case 4 ${coordinate as JSON}"
                break
        }

        // TODO: or repeat region?
        if (featureType && featureType == "exon") {
//            discontinuousProjection.addInterval(coordinate.getInt(1), coordinate.getInt(2), projectionDescription.padding)
            ProjectionSequence projectionSequence1 = new ProjectionSequence(
                    // tODO: add relevant sequence stuff, etc.
            )
            locationList.add(
                    new Location(
                            min: coordinate.getInt(1) - projectionDescription.padding
                            , max: coordinate.getInt(2) + projectionDescription.padding
                            , sequence: projectionSequence1
                    )
            )
//                    coordinate.getInt(1), coordinate.getInt(2), projectionDescription.padding)
        }

        return locationList
    }
/**
     * @deprecated . .. don't want to pass a projection here
     * @param discontinuousProjection
     * @param coordinate
     * @param projectionDescription
     * @return
     */
    List<Location> extractHighLevelArrayLocations(DiscontinuousProjection discontinuousProjection, JSONArray coordinate, ProjectionDescription projectionDescription) {
//                        // TODO: use enums to better track format
        int classType = coordinate.getInt(0)
        log.debug "processing high level array  ${coordinate as JSON}"
        String featureType
        switch (classType) {
            case 0:
                featureType = coordinate.getString(9)
                // process array in 10
                List<Location> localExonArray = extractExonArray(discontinuousProjection, coordinate.getJSONArray(10), projectionDescription)
                return localExonArray
                // process sublist if 11 exists
                break
            case 1:
                featureType = coordinate.getString(7)
                println "1 - doing nothing for this . . . no subarray? ${coordinate as JSON}"
                // no subarrays
                break
            case 2:
            case 3:
                featureType = coordinate.getString(6)
                println "2/3 - doing nothing for this . . . no subarray? ${coordinate as JSON}"
                // process array in 10
                // process sublist if 11 exists
                break
            case 4:
                println "not sure how to handle case 4 ${coordinate as JSON}"
                // ignore .  . . not an exon
                break
        }

        return new ArrayList<Location>()

    }

    /**
     * @deprecated
     * @param discontinuousProjection
     * @param coordinate
     * @param projectionDescription
     * @return
     */
    List<Location> extractExonArray(DiscontinuousProjection discontinuousProjection, JSONArray coordinate, ProjectionDescription projectionDescription) {
        List<Location> locationList = new ArrayList<>()
        log.debug "processing exon array ${coordinate as JSON}"
        def classType = coordinate.get(0)

        // then we assume that the rest are arrays if the first are . . and process them accordingly
        if (classType instanceof JSONArray) {
            for (int i = 0; i < coordinate.size(); i++) {
                log.debug "subarray ${coordinate.get(i) as JSON}"
                locationList.addAll(extractExonArray(discontinuousProjection, coordinate.getJSONArray(i), projectionDescription))
            }
            return locationList
        } else {
            // integer
            classType = coordinate.getInt(0)
        }
        String featureType
        switch (classType) {
            case 0:
                log.debug "not sure if this will work . . check! ${coordinate.size()} > 9"
                featureType = coordinate.getString(9)
                if (coordinate.size() >= 10) {
                    locationList.addAll(extractExonArray(discontinuousProjection, coordinate.getJSONArray(10), projectionDescription))
                }
                if (coordinate.size() >= 11) {
                    JSONObject sublist = coordinate.getJSONObject(11)
                    locationList.addAll(extractHighLevelArrayLocations(discontinuousProjection, sublist.getJSONArray("Sublist"), projectionDescription))
                }
                break
            case 1:
            case 2:
                featureType = coordinate.getString(7)
                break
            case 3:
                featureType = coordinate.getString(6)
                break
            case 4:
                println "not sure how to handle case 4 ${coordinate as JSON}"
                break
        }

        // TODO: or repeat region?
        if (featureType && featureType == "exon") {
//            discontinuousProjection.addInterval(coordinate.getInt(1), coordinate.getInt(2), projectionDescription.padding)
            ProjectionSequence projectionSequence1 = new ProjectionSequence(
                    // tODO: add relevant sequence stuff, etc.
            )
            locationList.add(
                    new Location(
                            min: coordinate.getInt(1) - projectionDescription.padding
                            , max: coordinate.getInt(2) + projectionDescription.padding
                            , sequence: projectionSequence1
                    )
            )
//                    coordinate.getInt(1), coordinate.getInt(2), projectionDescription.padding)
        }

        return locationList
    }

    List<Location> extractExonLocations(Organism organism, JSONArray tracksArray, ProjectionDescription projectionDescription) {
        List<Location> locationList = new ArrayList<>()

        // TODO: this is only here for debugging . .
//        projectionMap.clear()
        long startTime = System.currentTimeMillis()
        for (int i = 0; i < tracksArray.size(); i++) {
            JSONObject trackObject = tracksArray.getJSONObject(i)
            if (trackObject.containsKey("OGS") && trackObject.getBoolean("OGS") && !projectionMap.containsKey(trackObject.key)) {
                println "tring to generate projection for ${trackObject.key}"
                String jbrowseDirectory = organism.directory + "/tracks/" + trackObject.key
                File trackDirectory = new File(jbrowseDirectory)
                println "track directory ${trackDirectory.absolutePath}"

                File[] files = FileUtils.listFiles(trackDirectory, FileFilterUtils.nameFileFilter("trackData.json"), TrueFileFilter.INSTANCE)

                println "# of files ${files.length}"

                Map<String, ProjectionInterface> sequenceProjectionMap = new HashMap<>()

                for (File trackDataFile in files) {
//                    String sequenceFileName = trackDataFile.absolutePath.substring(trackDirectory.absolutePath.length(),trackDataFile.absolutePath.length()-"trackData.json".length()).replaceAll("/","")
                    String sequenceFileName = getSequenceName(trackDataFile.absolutePath)

                    JSONObject referenceJsonObject = new JSONObject(trackDataFile.text)
                    if (sequenceFileName.contains("1.10")) {
                        println "traying to create a sequence for ${sequenceFileName} "
                    }

                    // TODO: interpret the format properly
                    DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()
                    JSONArray coordinateReferenceJsonArray = referenceJsonObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
                    for (int coordIndex = 0; coordIndex < coordinateReferenceJsonArray.size(); ++coordIndex) {

                        // TODO: this needs to be recursive
                        JSONArray coordinate = coordinateReferenceJsonArray.getJSONArray(coordIndex)

                        if (coordinate.getInt(0) == 4) {
                            // projecess the file lf-${coordIndex} instead
                            File chunkFile = new File(trackDataFile.parent + "/lf-${coordIndex + 1}.json")
                            JSONArray chunkReferenceJsonArray = new JSONArray(chunkFile.text)

                            for (int chunkArrayIndex = 0; chunkArrayIndex < chunkReferenceJsonArray.size(); ++chunkArrayIndex) {
                                JSONArray chunkArrayCoordinate = chunkReferenceJsonArray.getJSONArray(chunkArrayIndex)
                                locationList.addAll(extractHighLevelArrayLocations(discontinuousProjection, chunkArrayCoordinate, projectionDescription))
//                                processHighLevelArray(discontinuousProjection, chunkArrayCoordinate,padding)
                            }

                        } else {
                            List<Location> thisLocationList = extractHighLevelArrayLocations(discontinuousProjection, coordinate, projectionDescription)
                            locationList.addAll(thisLocationList)
//                            processHighLevelArray(discontinuousProjection, coordinate,padding)
                        }
                    }

//                    println "# of entries: ${discontinuousProjection.minMap.size()}"

                    if (sequenceFileName.contains("1.10")) {
                        println "putting map ${sequenceFileName} into ${discontinuousProjection.size()}"
                    }
                    sequenceProjectionMap.put(sequenceFileName, discontinuousProjection)
                }

                println "final size: ${trackObject.key} -> ${sequenceProjectionMap.size()}"

                projectionMap.put(trackObject.key, sequenceProjectionMap)
            }
        }
        println "total time ${System.currentTimeMillis() - startTime}"


        return locationList
    }

    def getProjection(String putativeProjectionLoc, Organism organism) {
        if (putativeProjectionLoc.startsWith("{\"projection\":")) {
            JSONObject bookmarkJsonObject = JSON.parse(putativeProjectionLoc) as JSONObject
            bookmarkJsonObject.organism = organism.commonName
            return getProjection(bookmarkJsonObject)
        }
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

        println "gettting projeciton ${bookmarkObject}"
        ProjectionDescription projectionDescription = new ProjectionDescription()

        projectionDescription.type = bookmarkObject.projection
        projectionDescription.padding = bookmarkObject.padding
        projectionDescription.organism = bookmarkObject.organism
        projectionDescription.referenceTracks = [bookmarkObject.referenceTrack] as List<String>
        projectionDescription.sequenceList = new ArrayList<>()

        // TODO: reference / ?
        for (int i = 0; i < bookmarkObject.sequences.size(); i++) {
            JSONObject bookmarkSequence = bookmarkObject.sequences.getJSONObject(i)
            ProjectionSequence projectionSequence1 = new ProjectionSequence()
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


        if (!multiSequenceProjectionMap.containsKey(projectionDescription)) {
            MultiSequenceProjection multiSequenceProjection = createMultiSequenceProjection(projectionDescription)

            multiSequenceProjectionMap.put(projectionDescription, multiSequenceProjection)
        }
        return multiSequenceProjectionMap.get(projectionDescription)
        // return with appropriate sequence if one . .

//        projectionDescription.getSequenceList().iterator().next().

//        return null
    }

    Boolean containsSequence(Map<ProjectionSequence, MultiSequenceProjection> projectionSequenceMultiSequenceProjectionMap, String sequenceName, Long sequenceId, Organism currentOrganism) {
        ProjectionSequence projectionSequence = new ProjectionSequence(
                id: sequenceId
                , name: sequenceName
                , organism: currentOrganism.commonName
        )
        // this guarantees that the query is local to the descrption
        return projectionSequenceMultiSequenceProjectionMap.containsKey(projectionSequence)

    }
}
