package org.bbop.apollo.projection


/**
 * Created by nathandunn on 8/14/15.
 */
interface Projection {


    /**
     *
     * Probably just works on FeatureLocation
     *
     * @param min
     * @param max
     * @param feature
     * @return
     */
    Integer projectValue(Integer input)

    /**
     * Itera
     * @param trackInte over feature mins to produce a feature max
     * @return
     */
    Track projectTrack(Track trackIn)

}
