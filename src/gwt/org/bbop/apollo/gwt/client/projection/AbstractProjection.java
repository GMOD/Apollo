package org.bbop.apollo.gwt.client.projection;


/**
 * Created by nathandunn on 10/10/16.
 */
public abstract class AbstractProjection implements ProjectionInterface{

    public final static Integer UNMAPPED_VALUE = -1;

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
