package org.bbop.apollo.projection

import org.bbop.apollo.Organism

/**
 * Created by nathandunn on 9/24/15.
 */
class MultiSequenceProjection extends DiscontinuousProjection {

    // if a projection includes multiple sequences, this will include greater than one
    TreeMap<ProjectionSequence, DiscontinuousProjection> sequenceDiscontinuousProjectionMap = new TreeMap<>()
    ProjectionDescription projectionDescription  // description of how this is generated


    List<String> chunks = new ArrayList<>()
//    Map<String, Integer> chunkOffsets = new HashMap<>()
    ProjectionChunkList projectionChunkList = new ProjectionChunkList()

    ProjectionSequence getReverseProjectionSequence(Integer input) {
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet()) {
            if (input >= projectionSequence.offset && input <= projectionSequence.offset + sequenceDiscontinuousProjectionMap.get(projectionSequence).bufferedLength) {
                return projectionSequence
            }
        }
        return null
    }

    /**
     * Find which sequence I am on by iterating over coordinates
     * @param input
     * @return
     */
    ProjectionSequence getProjectionSequence(Integer input) {

        Integer offset = 0
        // should deliver these in order
        for (projectionSequence in sequenceDiscontinuousProjectionMap.keySet()) {
            DiscontinuousProjection projection = sequenceDiscontinuousProjectionMap.get(projectionSequence)
            if(input >= offset && input <= projection.originalLength + offset){
                return projectionSequence
            }
            offset += projection.originalLength
//            for (coordinate in projection.minMap.values()) {
//                if (input >= coordinate.min + offset && input <= coordinate.max + offset) {
//                    return projectionSequence
//                }
//            }
//            offset += projection.minMap.values().last().max
        }
        return null
    }

    @Override
    Integer projectValue(Integer input) {
        ProjectionSequence projectionSequence = getProjectionSequence(input)
        if (!projectionSequence) return -1
        return sequenceDiscontinuousProjectionMap.get(projectionSequence).projectValue(input - projectionSequence.originalOffset)  \
          + projectionSequence.offset
    }

    @Override
    Integer projectReverseValue(Integer input) {
        ProjectionSequence projectionSequence = getReverseProjectionSequence(input)
        if (!projectionSequence) return -1
        return sequenceDiscontinuousProjectionMap.get(projectionSequence).projectReverseValue(input - projectionSequence.offset) + projectionSequence.originalOffset
    }

    @Override
    Integer getLength() {
        Map.Entry<ProjectionSequence, DiscontinuousProjection> entry = sequenceDiscontinuousProjectionMap.lastEntry()
        return entry.key.offset + entry.value.length
    }

    @Override
    String projectSequence(String inputSequence, Integer minCoordinate, Integer maxCoordinate, Integer offset) {
        // not really used .  .. .  but otherwise would carve up into different bits
        return null
    }

    List<Coordinate> listCoordinates() {
        List<Coordinate> coordinateList = new ArrayList<>()
        for (def projection in sequenceDiscontinuousProjectionMap.values()) {
            coordinateList.addAll(projection.minMap.values() as List<Coordinate>)
        }
        return coordinateList
    }

    def addInterval(int min, int max, ProjectionSequence sequence) {
        Location location = new Location(min: min, max: max, sequence: sequence)
        addLocation(location)
    }


    @Override
    Integer size() {
        Integer count = 0

        for (def projection in sequenceDiscontinuousProjectionMap.values()) {
            count += projection.size()
        }

        return count
    }

    def addLocations(List<Location> locationList) {
        locationList.each {
            addLocation(it)
        }
    }

    @Override
    Integer clear() {
        return sequenceDiscontinuousProjectionMap.clear()
    }

// here we are adding a location to project
    def addLocation(Location location) {
        // if a single projection . . the default .. then assert that it is the same sequence / projection
        ProjectionSequence projectionSequence = getProjectionSequence(location)
        if (!projectionSequence) {
            projectionSequence = location.sequence

            Integer order = findSequenceOrder(projectionSequence)
            projectionSequence.order = order

            DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()
            discontinuousProjection.addInterval(location.min, location.max, projectionDescription.padding)
            sequenceDiscontinuousProjectionMap.put(projectionSequence, discontinuousProjection)
        } else {
            sequenceDiscontinuousProjectionMap.get(projectionSequence).addInterval(location.min, location.max, projectionDescription.padding)
        }
    }

    Integer findSequenceOrder(ProjectionSequence projectionSequence) {
        List<ProjectionSequence> projectionSequenceList = projectionDescription.sequenceList
        int index = 0
        for (ProjectionSequence projectionSequence1 in projectionSequenceList) {
            if (projectionSequence1.id == projectionSequence.id) {
                return index
            }
            ++index
        }
        return -1
    }

    ProjectionSequence getProjectionSequence(Location location) {
//        if (sequenceDiscontinuousProjectionMap.containsKey(location.sequence)) {
        // should be a pretty limited set
        if(!location) return null
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet()) {
            if (projectionSequence.equals(location.sequence)) {
                return projectionSequence
            }
        }
//        }
        return null
    }

    /**
     * This is done at the end to make offsets render properly
     */
    def calculateOffsets() {
        Integer currentOrder = 0
        Integer lastLength = 0
        Integer originalLength = 0
        sequenceDiscontinuousProjectionMap.keySet().each {
            DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(it)
            if (currentOrder > 0) {
                it.offset = lastLength + 1
                it.originalOffset = originalLength
            }

            lastLength += discontinuousProjection.bufferedLength
            originalLength += discontinuousProjection.originalLength
            ++currentOrder
        }
    }

    Boolean containsSequence(String sequenceName, Organism organism) {
        return containsSequence(sequenceName, null, organism)
    }

    Boolean containsSequence(String sequenceName, Long sequenceId, Organism organism) {
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet()) {
            if (projectionSequence.name == sequenceName) {
                if (projectionSequence.organism && organism) {
                    if (projectionSequence.organism != organism.commonName) {
                        return false
                    }
                }
                if (projectionSequence.id && sequenceId) {
                    if (projectionSequence.id != sequenceId) {
                        return false
                    }
                }
                return true
            }
        }
        return false
    }


    @Override
    public String toString() {
        return "MultiSequenceProjection{" +
                "sequenceDiscontinuousProjectionMap=" + sequenceDiscontinuousProjectionMap +
                ", projectionDescription=" + projectionDescription +
                '}';
    }

    /**
     * Looks up the length of the stored projection sequence
     * @param sequenceName
     * @return
     */
    Integer findProjectSequenceLength(String sequenceName) {
        for (ProjectionSequence projectionSequence1 in sequenceDiscontinuousProjectionMap.keySet()) {
            if (projectionSequence1.name == sequenceName) {
                DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence1)
//                println "projected sequence ${discontinuousProjection.projectSequence(sequenceName)}"
                Integer calculatedLength = discontinuousProjection.projectValue(discontinuousProjection.originalLength)
                println "calculated length ${calculatedLength} from ${discontinuousProjection.originalLength}"
                return calculatedLength
            }
        }
        null
    }
}
