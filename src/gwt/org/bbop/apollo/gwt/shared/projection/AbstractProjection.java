package org.bbop.apollo.gwt.shared.projection;

/**
 */
public abstract class AbstractProjection implements ProjectionInterface{
    public final static Integer UNMAPPED_VALUE = -1;

//    @Override
//    public Track projectTrack(Track trackIn) {
//        Track trackOut = new Track(length: trackIn.length)
//        for(Coordinate coordinate in trackIn.coordinateList.sort()){
//            Coordinate returnCoordinate = new Coordinate()
//            returnCoordinate.min = projectValue(coordinate.min)
//            returnCoordinate.max = projectValue(coordinate.max)
//            trackOut.coordinateList.add(returnCoordinate)
//        }
//        return trackOut
//    }

    @Override
    public Coordinate projectCoordinate(int min, int max) {
        return new Coordinate(projectValue(min),projectValue(max));
    }

    @Override
    public Coordinate projectReverseCoordinate(int min, int max) {
        return new Coordinate(projectReverseValue(min),projectReverseValue(max));
    }

    @Override
    public Integer clear() {
        return 0;
    }
}
