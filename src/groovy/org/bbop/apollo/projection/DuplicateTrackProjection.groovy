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
    String projectSequence(String inputSequence) {
        return inputSequence
    }
}
