package org.bbop.apollo.projection

import org.bbop.apollo.Feature

/**
 * Created by nathandunn on 8/14/15.
 */
class Projection {

    TreeMap<Integer,Feature> projectionMap = new TreeMap<>()

    /**
     *
     * Probably just works on FeatureLocation
     *
     * @param min
     * @param max
     * @param feature
     * @return
     */
    Feature project(Integer min,Integer max,Feature feature){
        Feature f2 = new Feature()

        Map.Entry<Integer,Feature> floorEntry = projectionMap.floorEntry(min)


        return f2
    }


    Track project(Track trackIn){
        Track trackOut = new Track()

        return trackOut
    }

}
