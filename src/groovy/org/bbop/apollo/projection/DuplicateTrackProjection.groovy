package org.bbop.apollo.projection

import groovy.transform.CompileStatic

/**
 * Created by Nathan Dunn on 8/24/15.
 */
@CompileStatic
class DuplicateTrackProjection extends AbstractProjection{


    @Override
    Integer projectValue(Integer input) {
        return input
    }

    @Override
    Integer projectReverseValue(Integer input) {
        return input
    }

    @Override
    Integer getLength() {
        return -1
    }

    @Override
    String projectSequence(String inputSequence,Integer minCoordinate ,Integer maxCoordinate,Integer offset ) {
        minCoordinate = minCoordinate >=0 ? minCoordinate : 0
        maxCoordinate = maxCoordinate >=0 ? maxCoordinate : inputSequence.length()
        return inputSequence.substring( minCoordinate , maxCoordinate )
    }
}
