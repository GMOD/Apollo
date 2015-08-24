package org.bbop.apollo.projection

/**
 * Created by ndunn on 8/24/15.
 */
class IntronProjection extends AbstractProjection{

    // projection from X -> X'
    TreeMap<Integer,Integer> projectionMap = new TreeMap<>()

    @Override
    Integer projectValue(Integer input) {

        return input
    }

}
