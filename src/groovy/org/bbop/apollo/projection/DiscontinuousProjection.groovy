package org.bbop.apollo.projection

/**
 * Created by ndunn on 8/24/15.
 */
class DiscontinuousProjection extends AbstractProjection{

    // projection from X -> X'
    TreeMap<Integer,Coordinate> minMap = new TreeMap<>()
    TreeMap<Integer,Coordinate> maxMap = new TreeMap<>()

    /**
     * Get the coordinate value out and add some to min
     *
     * To do this efficiently we simply get the interval sizes
     * and stop when we've hit all of the sizes
     *
     * @param input
     * @return
     */
    @Override
    Integer projectReverseValue(Integer input){

        Iterator<Integer> minIterator = minMap.keySet().iterator()
        Iterator<Integer> maxIterator = maxMap.keySet().iterator()
        Integer min, max

        // TODO: for speed generate a reverse map for quick lookup whilst doing this or another operation
        // here we can assume that the input maps onto the current length
        Integer currentLength = 0
        Integer bucketCount = 0
        Integer previousLength = 0
        while(minIterator.hasNext()){
            min = minIterator.next()
            max = maxIterator.next()
            currentLength += (max - min)
            if(currentLength+bucketCount>=input){
                return min + input - previousLength - bucketCount
            }
            previousLength += (max - min)
            ++bucketCount
        }
        return UNMAPPED_VALUE
    }

    @Override
    Integer projectValue(Integer input) {
        if(!minMap && !maxMap){
            return input
        }

        Integer floorMinKey = minMap.floorKey(input)
        Integer ceilMinKey = minMap.ceilingKey(input)

        Integer floorMaxKey = maxMap.floorKey(input)
        Integer ceilMaxKey = maxMap.ceilingKey(input)

//        log.debug "input ${input} minKey ${floorMinKey}-${ceilMinKey}"
//        log.debug "input ${input} maxKey ${floorMaxKey}-${ceilMaxKey}"

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

        // if we are inbetween a ceiling max and floor min, then we are in a viable block
        if(input > floorMinKey && input < ceilMaxKey &&  ceilMinKey >= ceilMaxKey){
            return input - floorMinKey + projectValue(floorMinKey)
        }

//        log.debug "${input} unable to find match, returning UNMAPPED"
        return UNMAPPED_VALUE
    }

    def addInterval(int min, int max) {
        assert max>=min

        Integer floorMinKey = minMap.floorKey(min)
        Integer ceilMinKey = minMap.ceilingKey(min)

        Integer floorMaxKey = maxMap.floorKey(max)
        Integer ceilMaxKey = maxMap.ceilingKey(max)


        if(max <= ceilMaxKey  && min >= floorMinKey){
            Coordinate minCoordinate = minMap.get(floorMinKey)
            Coordinate maxCoordinate = maxMap.get(ceilMaxKey)

            if(minCoordinate==maxCoordinate){
                // we are a subset
                return
            }

        }


        Coordinate coordinate = new Coordinate(min:min,max:max)
        minMap.put(min,coordinate)
        maxMap.put(max,coordinate)
    }

    @Override
    Track projectTrack(Track trackIn) {
        Track trackOut = new Track()
        Integer trackLength = 0

        for(Coordinate coordinate in trackIn.coordinateList.sort()){
            Coordinate returnCoordinate = new Coordinate()
            returnCoordinate.min = projectValue(coordinate.min)
            returnCoordinate.max = projectValue(coordinate.max)
            trackOut.coordinateList.add(returnCoordinate)
            trackLength = returnCoordinate.max
        }
        trackOut.length = trackLength+1
        return trackOut
    }

    @Override
    Coordinate projectCoordinate(int min, int max) {
        int newMin = projectValue(min)
        int newMax = projectValue(max)
        if(newMin>=0 && newMax >=0){
            return new Coordinate(min:newMin,max:newMax)
        }
        else
        if( (newMin<0 && newMax<0)) {
            return null
        }
        else
        if(newMin>=0) {
            // newMin is less than 0 so find the next one higher and move up
            Integer floorMaxKey = projectValue(maxMap.floorKey(max))
            if(floorMaxKey > newMin){
                return new Coordinate(min:newMin,max:floorMaxKey)
            }
            else{
                return null
//                throw new RuntimeException("can not get correct value  ${min}->${newMin}, ${max}->${newMax}/${floorMaxKey}")
            }
        }
        else
        if(newMax>=0) {
            // newMin is less than 0 so find the next one higher and move up
            Integer ceilMinKey = projectValue(minMap.ceilingKey(min))
            if(ceilMinKey < newMax){
                return new Coordinate(min:ceilMinKey,max:newMax)
            }
            else{
                return null
//                throw new RuntimeException("can not get correctvalue  ${min}->${newMin}/${ceilMinKey}, ${max}->${newMax}")
            }
        }
        else{
            throw new RuntimeException("not sure how we got here ${min}->${newMin}, ${max}->${newMax}")
        }
    }

    @Override
    Coordinate projectReverseCoordinate(int min, int max) {
        int newMin = projectReverseValue(min)
        int newMax = projectReverseValue(max)
        if(newMin<0 && newMax<0) return null
    }
}
