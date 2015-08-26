package org.bbop.apollo.projection

/**
 * Created by ndunn on 8/24/15.
 */
class DuplicateTrackProjection extends AbstractProjection{

    @Override
    Integer projectValue(Integer input) {
        return input
    }

    @Override
    Integer reverseProjectValue(Integer input) {
        return input
    }
}
