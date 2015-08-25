package org.bbop.apollo.projection

/**
 * Created by ndunn on 8/24/15.
 */
class DiscontinuousProjection extends AbstractProjection{

    // projection from X -> X'
    TreeMap<Integer,Coordinate> minMap = new TreeMap<>()
    TreeMap<Integer,Coordinate> maxMap = new TreeMap<>()

    @Override
    Integer projectValue(Integer input) {
        if(!minMap && !maxMap){
            return input
        }

        Integer floorMinKey = minMap.floorKey(input)
        Integer ceilMinKey = minMap.ceilingKey(input)

        Integer floorMaxKey = maxMap.floorKey(input)
        Integer ceilMaxKey = maxMap.ceilingKey(input)

        println "minKey ${floorMinKey}-${ceilMinKey}"
        println "maxKey ${floorMaxKey}-${ceilMaxKey}"

        if(!floorMinKey || !ceilMaxKey){
            return UNMAPPED_VALUE
        }

        // if is a hit for min and no max hit, then it is the left-most
        if(floorMinKey==ceilMinKey){
            if(!floorMaxKey){
                return 0
            }
            else{
//                return input - floorMaxKey
                return projectValue(floorMaxKey)+1
            }
        }



        // this is the left-most still
        if(floorMinKey!=ceilMinKey && !floorMaxKey){
            return input - floorMinKey
        }

        // if we are at the max border
        if(floorMaxKey==ceilMaxKey){
            return input - floorMinKey + projectValue(floorMinKey)
        }

        // we are in no-man's land
        if(input > floorMaxKey && input < ceilMinKey){
            return UNMAPPED_VALUE
        }

        return input
    }

    def addInterval(int min, int max) {
        Coordinate coordinate = new Coordinate(min:min,max:max)
        minMap.put(min,coordinate)
        maxMap.put(max,coordinate)
    }
}
