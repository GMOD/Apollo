package org.bbop.apollo.gwt.shared.projection;


/**
 * Created by nathandunn on 10/10/16.
 */
public abstract class AbstractProjection implements ProjectionInterface{

    public final static Long UNMAPPED_VALUE = -1l;

    @Override
    public Coordinate projectCoordinate(Long min, Long max) {
        return new Coordinate(projectValue(min),projectValue(max));
    }

    @Override
    public Coordinate unProjectCoordinate(Long min, Long max) {
        return new Coordinate(unProjectValue(min), unProjectValue(max));
    }

    @Override
    public Integer clear() {
        return 0;
    }
}
