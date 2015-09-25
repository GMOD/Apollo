package org.bbop.apollo.projection

/**
 * Created by nathandunn on 9/24/15.
 */
class MultiSequenceProjection extends DiscontinuousProjection{

    // if a projection includes multiple sequences, this will include greater than one
    TreeMap<ProjectionSequence, DiscontinuousProjection> sequenceDiscontinuousProjectionMap = new TreeMap<>()

    ProjectionSequence getProjectionSequence(Integer input) {
        for(ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet()){
            if(input >= projectionSequence.offset && input <= sequenceDiscontinuousProjectionMap.get(projectionSequence).length){
                return projectionSequence
            }
        }
        return null
    }

    @Override
    Integer projectValue(Integer input) {
        ProjectionSequence projectionSequence = getProjectionSequence(input)
        if (!projectionSequence) return -1
        return sequenceDiscontinuousProjectionMap.get(projectionSequence).projectValue(input - projectionSequence.offset)
    }

    @Override
    Integer projectReverseValue(Integer input) {
        ProjectionSequence projectionSequence = getProjectionSequence(input)
        if (!projectionSequence) return -1
        return sequenceDiscontinuousProjectionMap.get(projectionSequence).projectReverseValue(input - projectionSequence.offset)
    }

    @Override
    Integer getLength() {
        Map.Entry<ProjectionSequence,DiscontinuousProjection> entry = sequenceDiscontinuousProjectionMap.lastEntry()
        return entry.key.offset + entry.value.length
    }

    @Override
    String projectSequence(String inputSequence, Integer minCoordinate, Integer maxCoordinate, Integer offset) {
        // not really used .  .. .  but otherwise would carve up into different bits
        return null
    }

//    @Override
//    Track projectTrack(Track trackIn) {
//        return null
//    }
//
//    @Override
//    Coordinate projectCoordinate(int min, int max) {
//        return null
//    }
//
//    @Override
//    Coordinate projectReverseCoordinate(int min, int max) {
//        return null
//    }

    @Override
    Integer clear() {
        return sequenceDiscontinuousProjectionMap.clear()
    }
// here we are adding a location to project
    def addLocation(ProjectionDescription projectionDescription,Location location) {
        // if a single projection . . the default .. then assert that it is the same sequence / projection
        ProjectionSequence projectionSequence = getProjectionSequence(location)

        if(projectionSequence){
            sequenceDiscontinuousProjectionMap.get(projectionSequence).addInterval(location.min,location.max,projectionDescription.padding)
        }
    }


    ProjectionSequence getProjectionSequence(Location location){
        if(sequenceDiscontinuousProjectionMap.containsKey(location.sequence)){
            // should be a pretty limited set
            for(ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet()){
                if(projectionSequence.equals(location.sequence)){
                    return projectionSequence
                }
            }
        }
        return null
    }

}
