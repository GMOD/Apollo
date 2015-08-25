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

        println "input ${input} minKey ${floorMinKey}-${ceilMinKey}"
        println "input ${input} maxKey ${floorMaxKey}-${ceilMaxKey}"

        if(floorMinKey==null || ceilMaxKey==null){
            return UNMAPPED_VALUE
        }

        // if is a hit for min and no max hit, then it is the left-most
        if(floorMinKey==ceilMinKey){
            if(floorMaxKey==null){
                return 0
            }
            else{
//                return input - floorMaxKey
                return projectValue(floorMaxKey)+1
            }
        }



        // this is the left-most still
        if(floorMinKey!=ceilMinKey && floorMaxKey==null){
            return input - floorMinKey
        }

        // if we are at the max border
        if(floorMaxKey==ceilMaxKey){
            return input - floorMinKey + projectValue(floorMinKey)
        }


        println "unable to find match, returning UNMAPPED"
        return UNMAPPED_VALUE
    }

    def addInterval(int min, int max) {
        assert max>=min
        Coordinate coordinate = new Coordinate(min:min,max:max)
        minMap.put(min,coordinate)
        maxMap.put(max,coordinate)
    }
}
