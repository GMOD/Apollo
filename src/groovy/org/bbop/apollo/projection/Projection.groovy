package org.bbop.apollo.projection

import groovy.transform.CompileStatic


/**
 * Created by nathandunn on 8/14/15.
 */
@CompileStatic
interface Projection {


    /**
     *
     * Probably just works on FeatureLocation
     *
     * @param input
     * @return
     */
    Integer projectValue(Integer input)

    Integer projectReverseValue(Integer input)

    /**
     * @param trackIn
     * @return
     */
    Track projectTrack(Track trackIn)

    Coordinate projectCoordinate(int min, int max)
    Coordinate projectReverseCoordinate(int min, int max)
    Integer getLength()

    String projectSequence(String inputSequence,Integer minCoordinate,Integer maxCoordinate,Integer offset)
}
