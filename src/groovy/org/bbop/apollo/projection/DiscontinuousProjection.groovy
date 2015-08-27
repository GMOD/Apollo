package org.bbop.apollo.projection

/**
 * Created by ndunn on 8/24/15.
 */
class DiscontinuousProjection extends AbstractProjection {

    // projection from X -> X'
    TreeMap<Integer, Coordinate> minMap = new TreeMap<>()
    TreeMap<Integer, Coordinate> maxMap = new TreeMap<>()

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
    Integer projectReverseValue(Integer input) {

        Iterator<Integer> minIterator = minMap.keySet().iterator()
        Iterator<Integer> maxIterator = maxMap.keySet().iterator()
        Integer min, max

        // TODO: for speed generate a reverse map for quick lookup whilst doing this or another operation
        // here we can assume that the input maps onto the current length
        Integer currentLength = 0
        Integer bucketCount = 0
        Integer previousLength = 0
        while (minIterator.hasNext()) {
            min = minIterator.next()
            max = maxIterator.next()
            currentLength += (max - min)
            if (currentLength + bucketCount >= input) {
                return min + input - previousLength - bucketCount
            }
            previousLength += (max - min)
            ++bucketCount
        }
        return UNMAPPED_VALUE
    }

    @Override
    Integer projectValue(Integer input) {
        if (!minMap && !maxMap) {
            return input
        }

        Integer floorMinKey = minMap.floorKey(input)
        Integer ceilMinKey = minMap.ceilingKey(input)

        Integer floorMaxKey = maxMap.floorKey(input)
        Integer ceilMaxKey = maxMap.ceilingKey(input)

//        log.debug "input ${input} minKey ${floorMinKey}-${ceilMinKey}"
//        log.debug "input ${input} maxKey ${floorMaxKey}-${ceilMaxKey}"

        if (floorMinKey == null || ceilMaxKey == null) {
            return UNMAPPED_VALUE
        }

        // if is a hit for min and no max hit, then it is the left-most
        if (floorMinKey == ceilMinKey) {
            if (floorMaxKey == null) {
                return 0
            } else {
//                return input - floorMaxKey
                return projectValue(floorMaxKey) + 1
            }
        }

        // this is the left-most still
        if (floorMinKey != ceilMinKey && floorMaxKey == null) {
            return input - floorMinKey
        }

        // if we are at the max border
        if (floorMaxKey == ceilMaxKey) {
            return input - floorMinKey + projectValue(floorMinKey)
        }

        // if we are inbetween a ceiling max and floor min, then we are in a viable block
        if (input > floorMinKey && input < ceilMaxKey && ceilMinKey >= ceilMaxKey) {
            return input - floorMinKey + projectValue(floorMinKey)
        }

//        log.debug "${input} unable to find match, returning UNMAPPED"
        return UNMAPPED_VALUE
    }

    private Coordinate addCoordinate(int min, int max) {
        println "adding ${min} ${max}"
        Coordinate coordinate = new Coordinate(min: min, max: max)
        minMap.put(min, coordinate)
        maxMap.put(max, coordinate)
        return coordinate
    }

//    private List<Coordinate> removeIntermediates()

    private Coordinate replaceCoordinate(Coordinate coordinate, int min, int max) {
        println "replacing ${coordinate.min}-${coordinate.max} with ${min}-${max}"
        assert minMap.remove(coordinate.min) != null
        assert maxMap.remove(coordinate.max) != null

        Integer nextMin = minMap ? minMap.higherKey(coordinate.min) : null

        while (nextMin && minMap && maxMap && nextMin < max) {
            Coordinate nextMinCoord = minMap.get(nextMin)
            if (nextMinCoord.max > min) {
                println "removing min ${min} -> ${nextMinCoord.min}"
                println "removing max ${max} -> ${nextMinCoord.max}"
                assert minMap.remove(nextMinCoord.min) != null
                assert maxMap.remove(nextMinCoord.max) != null
            }
            nextMin = minMap.higherKey(coordinate.min)
        }

//        while(minMap.size()>=0 && minMap.ceilingKey(min)<max && minMap.ceilingKey(min)>0 ){
//            println "removing min ${min} -> ${minMap.ceilingKey(min)}"
//            println "removing max ${max} -> ${maxMap.ceilingKey(max)}"
//            assert minMap.remove(minMap.ceilingKey(min))!=null
//            assert maxMap.remove(maxMap.ceilingKey(max))!=null
//        }

        return addCoordinate(min, max)
    }

    def addInterval(int min, int max) {
        assert max >= min

        println "adding interval ${min}-${max}"
        Integer floorMinKey = minMap.floorKey(min)
        Integer ceilMinKey = minMap.ceilingKey(min)

        Integer floorMaxKey = maxMap.floorKey(max)
        Integer ceilMaxKey = maxMap.ceilingKey(max)

        println "floor ${min} min[${floorMinKey}]-max[${floorMaxKey}]"
        println "ceil ${max} min[${ceilMinKey}]-max[${ceilMaxKey}]"

        Coordinate floorMinCoord = floorMinKey ? minMap.get(floorMinKey) : null
        Coordinate floorMaxCoord = floorMaxKey ? maxMap.get(floorMaxKey) : null
        Coordinate ceilMaxCoord = ceilMaxKey ? maxMap.get(ceilMaxKey) : null
        Coordinate ceilMinCoord = ceilMinKey ? minMap.get(ceilMinKey) : null

        println "floorMinCoord ${floorMinCoord}"
        println "floorMaxCoord ${floorMaxCoord}"
        println "ceilMinCoord ${ceilMinCoord}"
        println "ceilMaxCoord ${ceilMaxCoord}"

        // no entries at all . . just add
        if (floorMinCoord == null && floorMaxCoord == null && ceilMinCoord == null && ceilMaxCoord == null) {
            return addCoordinate(min, max)
        } else
        // empty floor / LHS side
        if (floorMinCoord == null && floorMaxCoord == null && ceilMinCoord != null && ceilMaxCoord != null) {
            assert ceilMinCoord == ceilMaxCoord
            if (max < ceilMinCoord.min) {
                return addCoordinate(min, max)
            }
            return replaceCoordinate(ceilMinCoord, min, ceilMinCoord.max)
        } else
        // empty ceil / RHS side
        if (floorMinCoord != null && floorMaxCoord != null && ceilMinCoord == null && ceilMaxCoord == null) {
            assert ceilMinCoord == ceilMaxCoord
            if (min > floorMaxCoord.max) {
                return addCoordinate(min, max)
            }
            return replaceCoordinate(floorMaxCoord, floorMinCoord.min, max)
        }
        // overlapping within?
        if (floorMinCoord != null && floorMaxCoord == null && ceilMinCoord == null && ceilMaxCoord != null) {
            assert floorMinCoord == ceilMaxCoord

            return replaceCoordinate(floorMinCoord, Math.min(min, floorMinCoord.min), Math.max(max, ceilMaxCoord.max))
        }
        // overlapping without?
        if (floorMinCoord == null && floorMaxCoord != null && ceilMinCoord != null && ceilMaxCoord == null) {
            assert floorMaxCoord == ceilMinCoord
            return replaceCoordinate(floorMaxCoord, Math.min(min, floorMaxCoord.min), Math.max(max, ceilMinCoord.max))
        }
        // overlapping without?
        if (floorMinCoord != null && floorMaxCoord != null && ceilMinCoord != null && ceilMaxCoord != null) {
            // this overlaps on both sides
            if (floorMinCoord != floorMaxCoord && ceilMinCoord!=ceilMaxCoord && floorMaxCoord==ceilMinCoord) {
//                else{
//                    println "not sure what this condition is"
//                }

                if(min < ceilMinCoord.min && min > floorMinCoord.max && max > floorMaxCoord.max && max < ceilMaxCoord.min){
                    return replaceCoordinate(floorMaxCoord,min,max)
                }
                else
                // in-between all, so just add
                if(min > floorMaxCoord.max && max < ceilMinCoord.min ){
                    return addCoordinate(min, max)
                }
                // putting on the LHS
                else
                if(min > floorMaxCoord.max && max < ceilMaxCoord.max){
                    return replaceCoordinate(ceilMinCoord, min, ceilMaxCoord.max)
                }
                // putting on the RHS
                else
                if(min < floorMaxCoord.max && max < ceilMaxCoord.min){
                    return replaceCoordinate(floorMinCoord, floorMinCoord.min , max)
                }
                else
                if(min < floorMaxCoord.min && max < ceilMaxCoord.min){
                    return replaceCoordinate(floorMinCoord, floorMinCoord.min, ceilMaxCoord.max)
                }
                else
                if(min > floorMinCoord.min && min < floorMinCoord.max && max > ceilMaxCoord.min && max < ceilMaxCoord.max){
                    return replaceCoordinate(floorMinCoord, floorMinCoord.min, ceilMaxCoord.max)
                }
                else{
                    int newMin = min > floorMinCoord.max ? floorMinCoord.min : min
                    int newMax = max < ceilMaxCoord.min ? max : ceilMaxCoord.max
                    return replaceCoordinate(floorMinCoord, newMin, newMax)
                }

//                else
//                if(min < floorMaxCoord.min && max < ceilMaxCoord.min){
//                    return replaceCoordinate(floorMinCoord, floorMinCoord.min, ceilMaxCoord.max)
//                }
            }
            else
            if (floorMinCoord != floorMaxCoord && ceilMinCoord==ceilMaxCoord) {
                return replaceCoordinate(floorMinCoord, Math.min(min, floorMinCoord.min), Math.max(max, ceilMaxCoord.max))
            }
            else
            // if we have coordinates on either side
            if(floorMinCoord == floorMaxCoord && ceilMinCoord == ceilMaxCoord && ceilMinCoord != floorMinCoord ){
                // in-between all, so just add
                if(min > floorMaxKey && max < ceilMinKey){
                    return addCoordinate(min, max)
                }
                // putting on the LHS
                else
                if(min > floorMaxKey && max < ceilMaxCoord.max){
                    return replaceCoordinate(ceilMinCoord, min, ceilMaxCoord.max)
                }
                // putting on the RHS
                else
                if(min < floorMaxCoord.max && max < ceilMaxCoord.min){
                    return replaceCoordinate(floorMinCoord, floorMinCoord.min , max)
                }
                else{
                    int newMin = min > floorMinCoord.max ? floorMinCoord.min : min
                    int newMax = max < ceilMaxCoord.min ? max : ceilMaxCoord.max
                    return replaceCoordinate(floorMinCoord, newMin, newMax)
                }
            }
            // in the case they are in-between an existing scaffold
            else
            if(floorMinCoord == ceilMaxCoord && ceilMinCoord != ceilMaxCoord && floorMaxCoord != floorMinCoord && floorMaxCoord != ceilMinCoord ){
                return null
            }

            return addCoordinate(min, max)

        } else {
            println "ELSE condition . . "
            return addCoordinate(min, max)
        }

//        if(max <= ceilMaxKey  && min >= floorMinKey){
//            Coordinate minCoordinate = minMap.get(floorMinKey)
//            Coordinate maxCoordinate = maxMap.get(ceilMaxKey)
//
//            if(minCoordinate==maxCoordinate){
//                // we are a subset
//                return
//            }
//
//        }


    }


    @Override
    Track projectTrack(Track trackIn) {
        Track trackOut = new Track()
        Integer trackLength = 0

        for (Coordinate coordinate in trackIn.coordinateList.sort()) {
            Coordinate returnCoordinate = new Coordinate()
            returnCoordinate.min = projectValue(coordinate.min)
            returnCoordinate.max = projectValue(coordinate.max)
            trackOut.coordinateList.add(returnCoordinate)
            trackLength = returnCoordinate.max
        }
        trackOut.length = trackLength + 1
        return trackOut
    }

    @Override
    Coordinate projectCoordinate(int min, int max) {
        int newMin = projectValue(min)
        int newMax = projectValue(max)
        if (newMin >= 0 && newMax >= 0) {
            return new Coordinate(min: newMin, max: newMax)
        } else if ((newMin < 0 && newMax < 0)) {
            return null
        } else if (newMin >= 0) {
            // newMin is less than 0 so find the next one higher and move up
            Integer floorMaxKey = projectValue(maxMap.floorKey(max))
            if (floorMaxKey > newMin) {
                return new Coordinate(min: newMin, max: floorMaxKey)
            } else {
                return null
//                throw new RuntimeException("can not get correct value  ${min}->${newMin}, ${max}->${newMax}/${floorMaxKey}")
            }
        } else if (newMax >= 0) {
            // newMin is less than 0 so find the next one higher and move up
            Integer ceilMinKey = projectValue(minMap.ceilingKey(min))
            if (ceilMinKey < newMax) {
                return new Coordinate(min: ceilMinKey, max: newMax)
            } else {
                return null
//                throw new RuntimeException("can not get correctvalue  ${min}->${newMin}/${ceilMinKey}, ${max}->${newMax}")
            }
        } else {
            throw new RuntimeException("not sure how we got here ${min}->${newMin}, ${max}->${newMax}")
        }
    }

    @Override
    Coordinate projectReverseCoordinate(int min, int max) {
        int newMin = projectReverseValue(min)
        int newMax = projectReverseValue(max)
        if (newMin < 0 && newMax < 0) return null
    }
}
