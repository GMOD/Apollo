package org.bbop.apollo.projection

import groovy.transform.CompileStatic

/**
 * Created by Nathan Dunn on 8/24/15.
 */
@CompileStatic
abstract class AbstractProjection implements ProjectionInterface{

    public final static Integer UNMAPPED_VALUE = -1

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
