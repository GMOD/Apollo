package org.bbop.apollo.projection

/**
 * Created by ndunn on 8/24/15.
 */
abstract class AbstractProjection implements Projection{

    @Override
    Track projectTrack(Track trackIn) {
        Track trackOut = new Track()
        for(Coordinate coordinate in trackIn.coordinateList.sort()){
            Coordinate returnCoordinate = new Coordinate()
            returnCoordinate.min = projectValue(coordinate.min)
            returnCoordinate.max = projectValue(coordinate.max)
            trackOut.coordinateList.add(returnCoordinate)
        }
        return trackOut
    }
}
