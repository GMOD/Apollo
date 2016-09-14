package org.bbop.apollo.projection

import groovy.transform.CompileStatic

/**
 * Created by Nathan Dunn on 8/24/15.
 */
@CompileStatic
abstract class AbstractProjection implements ProjectionInterface{

    public final static Integer UNMAPPED_VALUE = -1

    @Override
    public Track projectTrack(Track trackIn) {
        Track trackOut = new Track(length: trackIn.length)
        for(Coordinate coordinate in trackIn.coordinateList.sort()){
            Coordinate returnCoordinate = new Coordinate()
            returnCoordinate.min = projectValue(coordinate.min)
            returnCoordinate.max = projectValue(coordinate.max)
            trackOut.coordinateList.add(returnCoordinate)
        }
        return trackOut
    }

    @Override
    public Coordinate projectCoordinate(int min, int max) {
        return new Coordinate(min:projectValue(min),max:projectValue(max))
    }

    @Override
    public Coordinate projectReverseCoordinate(int min, int max) {
        return new Coordinate(min:projectReverseValue(min),max:projectReverseValue(max))
    }

    @Override
    Integer clear() {
        0
    }
}
