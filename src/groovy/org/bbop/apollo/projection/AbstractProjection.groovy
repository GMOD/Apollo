package org.bbop.apollo.projection

/**
 * Created by ndunn on 8/24/15.
 */
abstract class AbstractProjection implements Projection{

    public final static Integer UNMAPPED_VALUE = -1

    @Override
    Track projectTrack(Track trackIn) {
        Track trackOut = new Track(length: trackIn.length)
        for(Coordinate coordinate in trackIn.coordinateList.sort()){
            Coordinate returnCoordinate = new Coordinate()
            returnCoordinate.min = projectValue(coordinate.min)
            returnCoordinate.max = projectValue(coordinate.max)
            trackOut.coordinateList.add(returnCoordinate)
        }
        return trackOut
    }
}
