package org.bbop.apollo.projection

import org.bbop.apollo.Organism

/**
 *
 * The class represents a single projected view.
 * Within that view, there are multiple ordered sequences (scaffolds).
 * Each sequence can represent the entire sequence, or a portion thereof.
 * Each sequence can have a reverse complement.
 * Each sequence can be repeated with a different portion.  An overlap should be merged.
 * This allows an entire sequence region to be visualized.
 *
 * Within each sequence, only a portion thereof may shown due to a variety of folding.
 * This is represented by the DiscontinuousProjection.
 * The DiscontinuousProjection refer to viewed exons (or an entire unfolded transcript) within a scaffold region.
 *
 *
 * Created by nathandunn on 9/24/15.
 */
class MultiSequenceProjection extends AbstractProjection {

    // if a projection includes multiple sequences, this will include greater than one

    TreeMap<ProjectionSequence, DiscontinuousProjection> sequenceDiscontinuousProjectionMap = new TreeMap<>()

    List<String> chunks = new ArrayList<>()
    ProjectionChunkList projectionChunkList = new ProjectionChunkList()

//    static int DEFAULT_SCAFFOLD_BORDER_LENGTH = 1
    static int DEFAULT_SCAFFOLD_BORDER_LENGTH = 0

    ProjectionSequence getReverseProjectionSequence(Integer input) {
        def projectionSequenceList = []
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            Integer bufferedLength = sequenceDiscontinuousProjectionMap.get(projectionSequence).bufferedLength
            if (input >= projectionSequence.offset && input <= projectionSequence.offset + bufferedLength) {
                projectionSequenceList << projectionSequence
            }
        }
//        if (projectionSequenceList?.size() > 1) {
//            println "overlapping projection sequences ${projectionSequenceList.size()}, choosing last as fmax is exclusive and fmin is inclusive"
//        }
        // because the end-point is exclusive, we should always use the second sequence if there is an overlap
        return projectionSequenceList ? projectionSequenceList.last() : null
    }

    List<ProjectionSequence> getReverseProjectionSequences(Integer minInput, Integer maxInput) {
        List<ProjectionSequence> orderedSequences = []
        Integer startOrder = getReverseProjectionSequence(minInput)?.order
        Integer endOrder = getReverseProjectionSequence(maxInput)?.order
        if (endOrder == null) {
            endOrder = getLastSequence().order
        }

        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            if (projectionSequence.order >= startOrder && projectionSequence.order <= endOrder) {
                orderedSequences << projectionSequence
            }
        }

        return orderedSequences
    }

    /**
     * Find which sequence I am on by iterating over coordinates.
     *
     * I want to return the first projection sequence where the start / end contains the input
     *
     * @param input
     * @return
     */
    ProjectionSequence getProjectionSequence(Integer input) {

        Integer offset = 0
        // should deliver these in order

        for (List<ProjectionSequence> projectionSequenceList in getOrderedSequences().values()) {
            for (ProjectionSequence projectionSequence in projectionSequenceList) {
                if (input >= projectionSequence.start + offset && input <= projectionSequence.end + offset) {
                    return projectionSequence
                }
            }
            // this is if the projectionsequences belong to the same sequence
            offset += projectionSequenceList.first().unprojectedLength
        }

        return null
    }

    /**
     *
     * @return Returns the relative order order of scaffolds in relation to each other.
     */
    TreeMap<Integer, List<ProjectionSequence>> getOrderedSequences() {
        Map<String, Integer> orderedMap = getOrderedSequenceMap()

        TreeMap<Integer, List<ProjectionSequence>> map = new TreeMap<>()

        for (projectionSequence in sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            Integer order = orderedMap.get(projectionSequence.name)
            def projectList = map.get(order) ?: new ArrayList<ProjectionSequence>()
            projectList.add(projectionSequence)
            map.put(order, projectList)
        }

        return map
    }

    Integer projectValue(Integer input, Integer inputOffset, Integer outputOffset) {
        ProjectionSequence projectionSequence = getProjectionSequence(input)
        if (!projectionSequence) {
            return UNMAPPED_VALUE
        }
        DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence)
        // TODO: buffer for scaffolds is currently 1 . . the order
        Integer returnValue = discontinuousProjection.projectValue(input - inputOffset)
        if (projectionSequence.reverse && returnValue != UNMAPPED_VALUE) {
            returnValue = discontinuousProjection.length + (discontinuousProjection.size() - 1) - returnValue
        }
        return returnValue == UNMAPPED_VALUE ? returnValue : returnValue + outputOffset
    }

    Integer projectValue(Integer input) {
        ProjectionSequence projectionSequence = getProjectionSequence(input)
        if (!projectionSequence) {
            return UNMAPPED_VALUE
        }
        return projectValue(input, projectionSequence.originalOffset, projectionSequence.offset)
    }

    Integer projectReverseValue(Integer input) {
        ProjectionSequence projectionSequence = getReverseProjectionSequence(input)
        if (!projectionSequence) {
            return UNMAPPED_VALUE
        }
        return projectReverseValue(input, projectionSequence.offset, projectionSequence.originalOffset)
    }

    Integer projectReverseValue(Integer input, Integer inputOffset, Integer outputOffset) {
        ProjectionSequence projectionSequence = getReverseProjectionSequence(input)
        if (!projectionSequence) {
            return -1
        }
        DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence)

        if (projectionSequence.reverse) {
            // need to flip the reverse value in the context of the projection sequence
            // length - ( i - offset ) + offset
            // length - i + (2 * offset)
            int alteredInput = discontinuousProjection.getBufferedLength(1) - input + projectionSequence.offset
            return discontinuousProjection.projectReverseValue(alteredInput) + outputOffset
        } else {
            return discontinuousProjection.projectReverseValue(input - inputOffset) + outputOffset
        }
    }

    Integer getLength() {
        Map.Entry<ProjectionSequence, DiscontinuousProjection> entry = sequenceDiscontinuousProjectionMap.lastEntry()
        return entry.key.offset + entry.value.length
    }

    String projectSequence(String inputSequence, Integer minCoordinate, Integer maxCoordinate, Integer offset) {
        Integer index = 0
        List<String> sequenceList = []

        // we start at the very bottom and go up
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence)
            Integer sequenceLength = projectionSequence.unprojectedLength
            offset = index

            // case 5: no overlap
            if (index > maxCoordinate || index + sequenceLength < minCoordinate) {
                // do nothing
                println "doing nothing with ${index}-${index + sequenceLength} in ${minCoordinate}-${maxCoordinate}"
            }
            // case 3: inbetween
            else if (minCoordinate > index && maxCoordinate < index + sequenceLength) {
                sequenceList << discontinuousProjection.projectSequence(inputSequence, minCoordinate - index + offset, maxCoordinate - index + offset, offset)
            }
            // case 1: right edge
            else if (minCoordinate > index && maxCoordinate >= index + sequenceLength) {
                sequenceList << discontinuousProjection.projectSequence(inputSequence, minCoordinate - index + offset, sequenceLength + offset, offset)
            }
            // case 2: left edge
            else if (minCoordinate <= index && maxCoordinate < sequenceLength + index) {
                sequenceList << discontinuousProjection.projectSequence(inputSequence, 0 + offset, maxCoordinate - index + offset, offset)
            }
            // case 4: overlap / all
            else if (minCoordinate <= index && maxCoordinate >= index + sequenceLength) {
                sequenceList << discontinuousProjection.projectSequence(inputSequence, 0 + offset, sequenceLength + offset, offset)
            }
//            else{
//                throw new RuntimeException("Should not get here: ${minCoordinate},${maxCoordinate}")
//            }
            index += sequenceLength
        }

        // not really used .  .. .  but otherwise would carve up into different bits
        return sequenceList.join("")
    }

    List<Coordinate> listCoordinates() {
        List<Coordinate> coordinateList = new ArrayList<>()
        for (def projection in sequenceDiscontinuousProjectionMap.values()) {
            coordinateList.addAll(projection.minMap.values() as List<Coordinate>)
        }
        return coordinateList
    }

    def addInterval(int min, int max, ProjectionSequence sequence) {
        println "adding interval ${min} ${max} ${sequence}"
        Location location = new Location(min: min, max: max, sequence: sequence)
        addLocation(location)
    }


    Integer size() {
        Integer count = 0
        for (def projection in sequenceDiscontinuousProjectionMap.values()) {
            count += projection.size()
        }

        return count
    }

    def addLocations(List<Location> locationList) {
        for (Location location in locationList) {
            addLocation(location)
        }
    }

    @Override
    Integer clear() {
        int size = sequenceDiscontinuousProjectionMap.size()
        sequenceDiscontinuousProjectionMap.clear()
        return size
    }

// here we are adding a location to project
    def addLocation(Location location) {
        // if a single projection . . the default .. then assert that it is the same sequence / projection
        ProjectionSequence projectionSequence = getProjectionSequenceForLocation(location)
        DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence)
        if (discontinuousProjection) {
            discontinuousProjection.addInterval(location.min, location.max, 0)
        } else {
            DiscontinuousProjection thisDiscontinuousProjection = new DiscontinuousProjection()
            thisDiscontinuousProjection.addInterval(location.min, location.max, 0)
            sequenceDiscontinuousProjectionMap.put(projectionSequence, thisDiscontinuousProjection)
        }
    }

    /**
     * Finds the most appropriate sequence projection for a given location
     * @param location
     * @return
     */
    ProjectionSequence getProjectionSequenceForLocation(Location location) {
        ProjectionSequence matchSequence = location.sequence
        TreeMap<Integer, ProjectionSequence> projectionSequenceTreeMap = new TreeMap<>()
        sequenceDiscontinuousProjectionMap.keySet().each {
            int score = it.name == matchSequence.name ? 1 : 0
            score += it.start == matchSequence.start ? 1 : 0
            score += it.end == matchSequence.end ? 1 : 0
            projectionSequenceTreeMap.put(score, it)
        }
        return projectionSequenceTreeMap.lastEntry().value
    }

    /**
     * This is done at the end to make offsets render properly
     */
    def calculateOffsets() {
        Integer currentOrder = 0
        Integer lastLength = 0
        Integer originalLength = 0
        def projectionSequences = sequenceDiscontinuousProjectionMap.keySet().sort() { a, b ->
            a.order <=> b.order
        }

        // generate set of projection sequences
        // they may have different offsets, but different originalOffset
        Map<String, Integer> originalOffsetMap = generateOriginalOffsetsForSequences(projectionSequences)

        for (ProjectionSequence projectionSequence in projectionSequences) {
            DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence)

            projectionSequence.offset = lastLength
            projectionSequence.originalOffset = originalOffsetMap.get(projectionSequence.name)

            assert projectionSequence.unprojectedLength != null
            assert projectionSequence.unprojectedLength > 0
            lastLength += discontinuousProjection.bufferedLength
            lastLength += DEFAULT_SCAFFOLD_BORDER_LENGTH
            ++currentOrder
        }

        return projectionSequences
    }

    private
    static Map<String, Integer> generateOriginalOffsetsForSequences(List<ProjectionSequence> projectionSequences) {
        Map<String, Integer> returnMap = new HashMap<>()
        int originalOffset = 0

        projectionSequences.each {
            if (!returnMap.containsKey(it.name)) {
                returnMap.put(it.name, originalOffset)
                originalOffset += it.unprojectedLength
            }
        }

        return returnMap
    }

    ProjectionSequence getProjectionSequence(String sequenceName, Organism organism) {
        return getProjectionSequence(sequenceName, null, organism)
    }

    ProjectionSequence getProjectionSequence(String sequenceName, Long sequenceId, Organism organism) {
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet()) {
            if (projectionSequence.name == sequenceName) {
                if (projectionSequence.organism && organism) {
                    if (projectionSequence.organism != organism.commonName) {
                        return projectionSequence
                    }
                }
                if (projectionSequence.id && sequenceId) {
                    if (projectionSequence.id != sequenceId) {
                        return null
                    }
                }
                return projectionSequence
            }
        }
        return null
    }

    Boolean containsSequence(String sequenceName, Organism organism) {
        return containsSequence(sequenceName, null, organism)
    }

    Boolean containsSequence(String sequenceName, Long sequenceId, Organism organism) {
        return getProjectionSequence(sequenceName, sequenceId, organism) != null
    }


    public String toString() {
        return "MultiSequenceProjection{" +
                "sequenceDiscontinuousProjectionMap=" + sequenceDiscontinuousProjectionMap +
                '}';
    }


    TreeMap<Integer, Coordinate> getMinMap() {
        Map<Integer, Coordinate> minMap = new TreeMap<>()
        List<ProjectionSequence> projectionSequenceList = sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order } as List

        for (ProjectionSequence projectionSequence : projectionSequenceList) {
            Map<Integer, Coordinate> returnMap = new TreeMap<>()

            sequenceDiscontinuousProjectionMap.get(projectionSequence).minMap.each {
                Coordinate coordinate = new Coordinate(min: it.value.min, max: it.value.max)
                coordinate.addOffset(projectionSequence.originalOffset)
                returnMap.put(it.key + projectionSequence.originalOffset, coordinate)
            }
            minMap.putAll(returnMap)
        }

        return minMap
    }

    TreeMap<Integer, Coordinate> getMaxMap() {
        Map<Integer, Coordinate> maxMap = new TreeMap<>()
        List<ProjectionSequence> projectionSequenceList = sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order } as List

        for (ProjectionSequence projectionSequence : projectionSequenceList) {
            Map<Integer, Coordinate> returnMap = new TreeMap<>()
            // add a set with an offset
            sequenceDiscontinuousProjectionMap.get(projectionSequence).maxMap.each {
                Coordinate coordinate = new Coordinate(min: it.value.min, max: it.value.max)
                coordinate.addOffset(projectionSequence.originalOffset)
                returnMap.put(it.key + projectionSequence.originalOffset, coordinate)
            }
            maxMap.putAll(returnMap)
        }

        return maxMap
    }

    Coordinate getMaxCoordinate(ProjectionSequence projectionSequence = null) {
        if (projectionSequence == null) {
            return getMaxMap().lastEntry().value
        }
        return sequenceDiscontinuousProjectionMap.get(projectionSequence).maxMap.lastEntry().value
    }

    Integer getOffsetForSequence(String sequenceName) {
        if (projectionChunkList) {
            ProjectionChunk projectionChunk = projectionChunkList.findProjectChunkForName(sequenceName)
            if (projectionChunk) {
                return projectionChunk.sequenceOffset
            }
        }
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet()) {
            if (sequenceName == projectionSequence.name) {
                return projectionSequence.originalOffset
            }
        }
        return 0
    }

    ProjectionSequence getLastSequence() {
        return projectedSequences.last()
    }

    List<ProjectionSequence> getProjectedSequences() {
        List<ProjectionSequence> orderedSequences = sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }
        return orderedSequences
    }

    /**
     * Returns the first entry of a ProjectionSequence
     * @return
     */
    Map<String, Integer> getOrderedSequenceMap() {
        Map<String, Integer> returnMap = new HashMap<>()
        getProjectedSequences().each { it ->
            if (!returnMap.containsKey(it.name)) {
                returnMap.put(it.name, it.order)
            }
        }
        return returnMap
    }

    Boolean overlaps(ProjectionSequence projectionSequenceA, ProjectionSequence projectionSequenceB) {
        assert projectionSequenceA.name==projectionSequenceB.name
        if (projectionSequenceA.start <= projectionSequenceB.start && projectionSequenceA.end >= projectionSequenceB.start) {
            return true
        }
        if (projectionSequenceA.start <= projectionSequenceB.end && projectionSequenceA.start >= projectionSequenceB.end) {
            return true
        }
        return false
    }

    ProjectionSequence overlaps(ProjectionSequence projectionSequence) {
        for (ProjectionSequence aProjSequence in getProjectedSequences()) {
            if (aProjSequence.name == projectionSequence.name) {
                if (overlaps(aProjSequence, projectionSequence)) {
                    return aProjSequence
                }
            }
        }
        return null
    }

    /**
     * Merge ProjectionSequenceA with ProjectionSequenceB
     *
     * We assume that they are overlapped and belong to the same one.
     *
     * Return ProjectionSequenceA
     *
     * @param projectionSequenceA
     * @param projectionSequenceB
     * @return
     */
    ProjectionSequence merge(ProjectionSequence projectionSequenceA, ProjectionSequence projectionSequenceB) {
        if(projectionSequenceB.start < projectionSequenceA.start){
            projectionSequenceA.start = projectionSequenceB.start
        }
        if(projectionSequenceB.end > projectionSequenceA.end){
            projectionSequenceA.end = projectionSequenceB.end
        }

        return projectionSequenceA
    }

    def addProjectionSequences(List<ProjectionSequence> theseProjectionSequences) {
        theseProjectionSequences.each {
            ProjectionSequence overlappingProjectionSequence = overlaps(it)
            if (overlappingProjectionSequence) {
                sequenceDiscontinuousProjectionMap.remove(overlappingProjectionSequence)
                overlappingProjectionSequence = merge(overlappingProjectionSequence, it)
                sequenceDiscontinuousProjectionMap.put(overlappingProjectionSequence, null)
            } else {
                sequenceDiscontinuousProjectionMap.put(it, null)
            }
        }
    }

    /**
     * - No overlap (just merge)
     * - Discontinuous projection must be contained within the ProjectionSequence
     * - Projection Sequence must be sequentially ordered
     *
     * @return
     */
    Boolean isValid() {

        Map<String, Boolean> reverseMap = new HashMap<>()

        for (ProjectionSequence projectionSequence in getProjectedSequences()) {
            if (reverseMap.containsKey(projectionSequence.name)) {
                if (reverseMap.get(projectionSequence.name) != projectionSequence.reverse) {
                    return false
                }
            } else {
                reverseMap.put(projectionSequence.name, projectionSequence.reverse)
            }
        }
        return true
    }
}
