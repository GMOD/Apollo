package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.*
import org.bbop.apollo.sequence.Strand
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class ProjectionService {

    def assemblageService
    def trackMapperService
    def permissionService


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
        List<Coordinate> coordinates = getCoordinatesFromAssemblage(assemblage)
        MultiSequenceProjection multiSequenceProjection = createMultiSequenceProjection(assemblage, coordinates)
        return multiSequenceProjection
    }


    ProjectionSequence convertJsonToProjectionSequence(JSONObject jSONObject, int index, String organismCommonName) {
        ProjectionSequence projectionSequence = new ProjectionSequence()
        if (jSONObject.start == null) {
            Sequence sequence = Sequence.findByName(jSONObject.name)
            projectionSequence.start = sequence.start
            projectionSequence.end = sequence.end
            projectionSequence.unprojectedLength = sequence.length
        } else {
            projectionSequence.start = jSONObject.start
            projectionSequence.end = jSONObject.end
            projectionSequence.unprojectedLength = jSONObject.length
        }
        projectionSequence.order = index
        projectionSequence.name = jSONObject.name
        projectionSequence.reverse = jSONObject.reverse
        projectionSequence.organism = organismCommonName

        JSONArray featureArray = jSONObject.features
        List<String> features = new ArrayList<>()
        for (int j = 0; featureArray != null && j < featureArray.size(); j++) {
            features.add(featureArray.getString(j))
        }
        projectionSequence.setFeatures(features)

        return projectionSequence
    }

    @NotTransactional
    JSONObject convertToJsonFromProjection(MultiSequenceProjection projection) {
        JSONObject jsonObject = new JSONObject()

        jsonObject.put(FeatureStringEnum.SEQUENCE_LIST.value, convertToJsonArrayFromProjection(projection))

        return jsonObject
    }

    @NotTransactional
    JSONArray convertToJsonArrayFromProjection(MultiSequenceProjection projection) {

        JSONArray projectionJsonArray = new JSONArray()
        for (ProjectionSequence projectionSequence in projection.getProjectedSequences()) {
            JSONObject sequenceJsonObject = convertToJsonFromSequence(projectionSequence, projection.getProjectionForSequence(projectionSequence))
            projectionJsonArray.add(sequenceJsonObject)
        }
        return projectionJsonArray

    }

    /**
     * This will the ability to add discontinuous projections as well.
     * @param projectionSequence
     * @return
     */
    @NotTransactional
    JSONObject convertToJsonFromSequence(ProjectionSequence projectionSequence, DiscontinuousProjection discontinuousProjection) {
        JSONObject sequenceJsonObject = new JSONObject()

        org.codehaus.groovy.runtime.InvokerHelper.setProperties(sequenceJsonObject, projectionSequence.properties)

        if (discontinuousProjection) {
            JSONObject featureObject = new JSONObject()

            if (discontinuousProjection.metadata) {
                JSONObject discontinuousProjectionObject = JSON.parse(discontinuousProjection.metadata) as JSONObject
                featureObject.name = discontinuousProjectionObject.name
            }

            List<Coordinate> coordinateList = discontinuousProjection.getCoordinates() as List<Coordinate>
            JSONArray coordinateArray = new JSONArray()
            for (Coordinate coordinate in coordinateList) {
                JSONObject coordinateObject = new JSONObject(
                        fmin: coordinate.min
                        , fmax: coordinate.max
                        , sequence: projectionSequence
                )
                coordinateArray.add(coordinateObject)
            }
            featureObject.put(FeatureStringEnum.LOCATION.value, coordinateArray)
            sequenceJsonObject.feature = featureObject
        }

        return sequenceJsonObject
    }

    @NotTransactional
    MultiSequenceProjection convertToProjectionFromJson(JSONObject projectionObject) {

        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        Map<String, ProjectionSequence> projectionSequenceList = convertToProjectSequenceFromSequenceJsonObject(multiSequenceProjection, projectionObject).collectEntries() {
            [(it.name): it]
        }
        multiSequenceProjection.addProjectionSequences(projectionSequenceList.values())

        // next we add intervals from our array
        JSONArray sequenceArray = projectionObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value)
        for (JSONObject sequenceObject in sequenceArray) {
//            DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()
            ProjectionSequence projectionSequence = projectionSequenceList.get(sequenceObject.name)
//            if(sequenceObject.containsKey(FeatureStringEnum.LOCATION.value)){
//                for (JSONObject loc in sequenceObject.getJSONArray(FeatureStringEnum.LOCATION.value)) {
//                    Coordinate coordinate = new Coordinate(
//                            min: loc.getInt(FeatureStringEnum.FMIN.value)
//                            , max: loc.getInt(FeatureStringEnum.FMAX.value)
//                            , sequence: projectionSequence
//                    )
//                    multiSequenceProjection.addCoordinate(coordinate)
//                }
//            }
//            else
            if (sequenceObject.containsKey(FeatureStringEnum.FEATURE.value) && sequenceObject.getJSONObject(FeatureStringEnum.FEATURE.value).containsKey(FeatureStringEnum.LOCATION.value)) {
                addCoordinates(sequenceObject.getJSONObject(FeatureStringEnum.FEATURE.value).getJSONArray(FeatureStringEnum.LOCATION.value),multiSequenceProjection,projectionSequence)
            } else if (sequenceObject.containsKey(FeatureStringEnum.LOCATION.value)) {
                addCoordinates(sequenceObject.getJSONArray(FeatureStringEnum.LOCATION.value),multiSequenceProjection,projectionSequence)
            } else {
                if (true) throw new RuntimeException("Should never get here")
                Coordinate coordinate = new Coordinate(
                        sequenceObject.getJSONObject(FeatureStringEnum.FEATURE.value).getInt(FeatureStringEnum.FMIN.value)
                        , sequenceObject.getJSONObject(FeatureStringEnum.FEATURE.value).getInt(FeatureStringEnum.FMAX.value)
                        , projectionSequence
                )
                multiSequenceProjection.addCoordinate(coordinate)
            }
        }

        return multiSequenceProjection

    }

    def addCoordinates(JSONArray jsonArray,MultiSequenceProjection projection,ProjectionSequence projectionSequence) {
        for (JSONObject loc in jsonArray) {
//                    println "loc: ${loc as JSON}"
            Coordinate coordinate = new Coordinate(
                    loc.getInt(FeatureStringEnum.FMIN.value)
                    , loc.getInt(FeatureStringEnum.FMAX.value)
                    , projectionSequence
            )
            projection.addCoordinate(coordinate)
        }
    }


    @NotTransactional
    List<ProjectionSequence> convertToProjectSequenceFromSequenceJsonObject(MultiSequenceProjection multiSequenceProjection, JSONObject sequenceObject) {
        JSONArray jsonArray = sequenceObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value)
        List<ProjectionSequence> projectionSequenceList = convertJsonArrayToSequences(jsonArray)
        multiSequenceProjection.addProjectionSequences(projectionSequenceList)
        return projectionSequenceList
    }

    /**
     * This is used to create locations to be added to a projection.
     *
     *
     *
     * @param assemblage
     * @return
     */
    @Transactional
    List<Coordinate> getCoordinatesFromAssemblage(Assemblage assemblage) {
        List<Coordinate> coordinates = new ArrayList<>()


        JSONArray sequenceListArray = JSON.parse(assemblage.sequenceList) as JSONArray

        JSONObject inputObject = new JSONObject()
        inputObject.put(FeatureStringEnum.SEQUENCE_LIST.value, sequenceListArray)

        for (int i = 0; i < sequenceListArray.size(); i++) {
            JSONObject sequenceObject = sequenceListArray.getJSONObject(i)
            ProjectionSequence projectionSequence = convertJsonToProjectionSequence(sequenceObject, i, assemblage.organism.commonName)
            if (sequenceObject.location) {
                JSONArray locationArray = sequenceObject.location
                for (JSONObject locationObject in locationArray) {
                    coordinates.add(
                            new Coordinate(
                                    locationObject.getInt(FeatureStringEnum.FMIN.value)
                                    , locationObject.getInt(FeatureStringEnum.FMAX.value)
                                    , projectionSequence
                            )
                    )
                }
            } else {
                coordinates.add(
                        new Coordinate(
                                sequenceObject.start
                                , sequenceObject.end,
                                projectionSequence)
                )
            }
        }

        return coordinates
    }

    MultiSequenceProjection createMultiSequenceProjection(Assemblage assemblage, List<Coordinate> coordinates) {


        List<ProjectionSequence> projectionSequenceList = convertJsonArrayToSequences((JSON.parse(assemblage.sequenceList) as JSONArray), assemblage.organism.commonName)

        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences(projectionSequenceList)
        multiSequenceProjection.addCoordinates(coordinates)
        multiSequenceProjection.calculateOffsets()
        Map<String, ProjectionSequence> projectionSequenceMap = [:]

        multiSequenceProjection.projectedSequences.each {
            projectionSequenceMap.put(it.name, it)
        }
//        List<String> sequenceNames = multiSequenceProjection.projectedSequences.name
        // TODO: speed this up by caching sequences
        Sequence.findAllByNameInList(projectionSequenceMap.keySet() as List<String>).each {
            def projectionSequence = projectionSequenceMap.get(it.name)
            projectionSequence.unprojectedLength = it.length
        }

        return multiSequenceProjection
    }

    @NotTransactional
    List<ProjectionSequence> convertJsonArrayToSequences(JSONArray jsonArray, String organismCommonName = null) {
        List<ProjectionSequence> projectionSequenceList = new ArrayList<>()
        jsonArray.eachWithIndex { JSONObject it, int i ->
            organismCommonName = organismCommonName ?: it.organism
            ProjectionSequence projectionSequence = convertJsonToProjectionSequence(it, i, organismCommonName)

            projectionSequenceList.add(projectionSequence)
        }
        return projectionSequenceList
    }

    List<Coordinate> extractHighLevelCoordinates(JSONArray coordinate, Organism organism, String trackName) {
        TrackIndex trackIndex = trackMapperService.getIndices(organism.commonName, trackName, coordinate.getInt(0))

        log.debug "processing high level array  ${coordinate as JSON}"
        // process array in 10
        if (trackIndex.hasSubFeatures()) {
            List<Coordinate> localExonArray = extractExonArrayCoordinates(coordinate.getJSONArray(trackIndex.subFeaturesColumn), organism, trackName)
            return localExonArray
        }
        return new ArrayList<Coordinate>()

    }

    List<Coordinate> extractExonArrayCoordinates(JSONArray coordinate, Organism organism, String trackName) {
        List<Coordinate> coordinates = new ArrayList<>()
        log.debug "processing exon array ${coordinate as JSON}"
        def classType = coordinate.get(0)

        // then we assume tht the rest are arrays if the first are . . and process them accordingly
        if (classType instanceof JSONArray) {
            for (int i = 0; i < coordinate.size(); i++) {
                log.debug "subarray ${coordinate.get(i) as JSON}"
                coordinates.addAll(extractExonArrayCoordinates(coordinate.getJSONArray(i), organism, trackName))
            }
            return coordinates
        }
        TrackIndex trackIndex = trackMapperService.getIndices(organism.commonName, trackName, coordinate.getInt(0))
        String featureType = coordinate.getString(trackIndex.type)
        if (trackIndex.hasSubFeatures()) {
            coordinates.addAll(extractExonArrayCoordinates(coordinate.getJSONArray(trackIndex.subFeaturesColumn), organism, trackName))
        }
        if (trackIndex.hasChunk()) {
            JSONObject sublist = coordinate.getJSONObject(coordinate.size() - 1)
            coordinates.addAll(extractHighLevelCoordinates(sublist.getJSONArray("Sublist"), organism, trackName))
        }

        // TODO: or repeat region?
        if (featureType && featureType == "exon") {
            String sequenceName = coordinate.getString(trackIndex.seqId)
            ProjectionSequence projectionSequence1 = new ProjectionSequence(
                    name: sequenceName
                    , organism: organism.commonName

            )
            coordinates.add(
                    new Coordinate(
                            coordinate.getInt(trackIndex.start)
                            , coordinate.getInt(trackIndex.end) - 1 // the end is exclusive from track, we store inclusive
                            , projectionSequence1
                    )
            )
        }

        return coordinates
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
        ProjectionSequence firstProjectionSequence = multiSequenceProjection.projectedSequences.find() {
            it.name == firstSequenceName
        }

        if (firstProjectionSequence.reverse) {
            Integer calculatedMin = firstProjectionSequence.offset - firstProjectionSequence.start + (firstProjectionSequence.end - firstFeatureLocation.fmin)
            return calculatedMin
        } else {
            Integer calculatedMin = firstProjectionSequence.offset - firstProjectionSequence.start + firstFeatureLocation.fmin
            return calculatedMin
        }

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
        ProjectionSequence lastProjectionSequence = multiSequenceProjection.projectedSequences.find() {
            it.name == lasttSequenceName
        }

        if (lastProjectionSequence.reverse) {
            Integer calculatedMax = lastProjectionSequence.offset - lastProjectionSequence.start + (lastProjectionSequence.end - lastFeatureLocation.fmax)
            return calculatedMax
        } else {
            Integer calculatedMax = lastProjectionSequence.offset - lastProjectionSequence.start + lastFeatureLocation.fmax
            return calculatedMax
        }
    }

    @NotTransactional
    JSONArray getSequenceListJSON(String inputString) {
        JSONObject jsonObject = JSON.parse(inputString) as JSONObject
        return jsonObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value)
    }

    // TODO: constant / read-only, so could always move to a database cache
    @NotTransactional
    def cacheProjection(String projectionString, MultiSequenceProjection multiSequenceProjection) {
        multiSequenceProjectionMap.put(getSequenceListJSON(projectionString), multiSequenceProjection)
    }

    @NotTransactional
    MultiSequenceProjection getCachedProjection(String projectionString) {
        return multiSequenceProjectionMap.get(getSequenceListJSON(projectionString))
    }

/**
 * This reverses the strand, fmin, and fmax data
 * @param projectionSequence
 * @param locationObject
 * @return
 */
    @NotTransactional
    JSONObject reverseLocation(ProjectionSequence projectionSequence, JSONObject locationObject) {
        if (projectionSequence.reverse) {
            if (locationObject.containsKey(FeatureStringEnum.STRAND.value)) {
                Strand strand = Strand.getStrandForValue(locationObject.strand)
                strand = strand.reverse();
                locationObject.strand = strand.value
            }
            Integer temp = locationObject.fmin ?: null
            locationObject.fmin = locationObject.fmax
            locationObject.fmax = temp
        }
        return locationObject
    }

    /**
     * Genreates a sequence list from a projection object
     * @param multiSequenceProjection
     * @return
     */
    @NotTransactional
    JSONArray generateSequenceListFromProjection(MultiSequenceProjection multiSequenceProjection) {
        JSONArray sequenceList = new JSONArray()
        for (ProjectionSequence projectionSequence in multiSequenceProjection.projectedSequences) {
            JSONObject sequenceObject = new JSONObject((projectionSequence as JSON).toString())

            // this means we have genome folding here
            DiscontinuousProjection discontinuousProjection = multiSequenceProjection.getProjectionForSequence(projectionSequence)
            if (discontinuousProjection) {
                JSONArray foldingArray = new JSONArray()
                for (Coordinate coordinate in discontinuousProjection.getCoordinates()) {
//                    JSONObject coordinateObject = new JSONObject( (coordinate as JSON).toString())
                    JSONObject coordinateObject = new JSONObject()
                    coordinateObject.put(FeatureStringEnum.FMIN.value, coordinate.min)
                    coordinateObject.put(FeatureStringEnum.FMAX.value, coordinate.max)
//                    coordinateObject.put(FeatureStringEnum.SEQUENCE.value,coordinate.sequence.name)
                    foldingArray.add(coordinateObject)
                }

                sequenceObject.put(FeatureStringEnum.LOCATION.value, foldingArray)
            }
            sequenceList.add(sequenceObject)

        }
        return sequenceList
    }
}
