package org.bbop.apollo.projection

import org.bbop.apollo.Feature
import org.bbop.apollo.FeatureLocation

/**
 * Created by nathandunn on 8/14/15.
 */
class Projection {

    TreeMap<Integer,FeatureLocation> projectionMap = new TreeMap<>()

    /**
     *
     * Probably just works on FeatureLocation
     *
     * @param min
     * @param max
     * @param feature
     * @return
     */
    FeatureLocation project(Integer min,Integer max,FeatureLocation featureLocation){
        FeatureLocation f2 = new FeatureLocation()

        Map.Entry<Integer,FeatureLocation> floorEntry = projectionMap.floorEntry(min)


        return f2
    }

    /**
     * Itera
     * @param trackInte over feature mins to produce a feature max
     * @return
     */
    Track project(Track trackIn){
        Track trackOut = new Track()


        return trackOut
    }

}
