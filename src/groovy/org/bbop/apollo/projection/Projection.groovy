package org.bbop.apollo.projection


/**
 * Created by nathandunn on 8/14/15.
 */
interface Projection {


    /**
     *
     * Probably just works on FeatureLocation
     *
     * @param input
     * @return
     */
    Integer projectValue(Integer input)

    Integer reverseProjectValue(Integer input)

    /**
     * @param trackIn
     * @return
     */
    Track projectTrack(Track trackIn)

}
