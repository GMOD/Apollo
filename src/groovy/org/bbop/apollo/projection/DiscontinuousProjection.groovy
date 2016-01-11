package org.bbop.apollo.projection

import groovy.transform.CompileStatic

/**
 * Created by Nathan Dunn on 8/24/15.
 */
@CompileStatic
public class DiscontinuousProjection extends AbstractProjection {

    
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
    public Integer projectReverseValue(Integer input) {

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
    public Integer projectValue(Integer input) {
        if (!minMap && !maxMap) {
            return input
        }

        if (input == null) {
            return UNMAPPED_VALUE
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

        // if we are inbetween for the last large one on the RHS
        if (floorMaxKey != ceilMaxKey && ceilMinKey == null) {
            return input - floorMinKey + projectValue(floorMinKey)
        }

//        log.debug "${input} unable to find match, returning UNMAPPED"
        return UNMAPPED_VALUE
    }

    private Coordinate addCoordinate(int min, int max) {
        Coordinate coordinate = new Coordinate(min: min, max: max)
        if (minMap.containsKey(min) && !maxMap.containsKey(max)) {
            throw new RuntimeException("minKey is dupe and should be replaced ${min}::${max}")
        }
        if (!minMap.containsKey(min) && maxMap.containsKey(max)) {
            throw new RuntimeException("maxKey is dupe and should be replaced ${min}::${max}")
        }
        minMap.put(min, coordinate)
        maxMap.put(max, coordinate)
        return coordinate
    }

//    private List<Coordinate> removeIntermediates()

    /**
     * Replace an existing coordinate with a new set (e.g., an overlap)
     * @param coordinate
     * @param min
     * @param max
     * @return
     */
    private Coordinate replaceCoordinate(Coordinate coordinate, int min, int max) {
//        println "replace coordinate ${coordinate.min}/${coordinate.max}->${min}/${max}"
        assert minMap.remove(coordinate.min) != null
        assert maxMap.remove(coordinate.max) != null

        Integer nextMin = minMap ? minMap.higherKey(coordinate.min) : null

        def doBreak = false
        // we have to remove any overlapping elements here
        while (nextMin && minMap && maxMap && nextMin < max && !doBreak) {
            Coordinate nextMinCoord = minMap.get(nextMin)
//            println "nextMinCoord.max ${nextMinCoord.max} > ${min}"
            if (nextMinCoord.max > min) {
                assert minMap.remove(nextMinCoord.min) != null
                assert maxMap.remove(nextMinCoord.max) != null
            } else {
                doBreak = true
            }
            nextMin = minMap.higherKey(coordinate.min)
//            println "nextMin ${coordinate.min} -> higher key -> ${nextMin} < ${max} "
        }
        return addCoordinate(min, max)
    }


    def addInterval(int min, int max, Integer padding = 0) {
        min -= padding ?: 0
        max += padding ?: 0
        min = min < 0 ? 0 : min
//        println "${min},${max}"
//        max = max > length ? length: max
        assert max >= min

        Integer floorMinKey = minMap.floorKey(min)
        Integer ceilMinKey = minMap.ceilingKey(min)

        Integer floorMaxKey = maxMap.floorKey(max)
        Integer ceilMaxKey = maxMap.ceilingKey(max)

        Coordinate floorMinCoord = floorMinKey ? minMap.get(floorMinKey) : null
        Coordinate floorMaxCoord = floorMaxKey ? maxMap.get(floorMaxKey) : null
        Coordinate ceilMaxCoord = ceilMaxKey ? maxMap.get(ceilMaxKey) : null
        Coordinate ceilMinCoord = ceilMinKey ? minMap.get(ceilMinKey) : null

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
//            floorMinCoord == floorMaxCoord
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
        // if we are internal / in the middle
        if (floorMinCoord == null && ceilMinCoord == null && floorMaxCoord != null && ceilMaxCoord != null) {
            return null
        }
        if (floorMinCoord != null && ceilMinCoord != null && floorMaxCoord != null && ceilMaxCoord == null) {
            if (floorMinCoord == floorMaxCoord && floorMaxCoord == ceilMinCoord) {
                if (max > floorMaxCoord.max) {
                    return replaceCoordinate(floorMinCoord, floorMinCoord.min, max)
                }
                println "not sure how to handle this piece "
            }
            return null
        }
        // if we are right on the right edge
        if (floorMinCoord == null && ceilMinCoord != null && floorMaxCoord != null && ceilMaxCoord != null) {
            if (floorMaxCoord == ceilMaxCoord && ceilMaxCoord == ceilMinCoord) {
                if (min < floorMaxCoord.min) {
                    return replaceCoordinate(floorMaxCoord, min, floorMaxCoord.max)
                }
                println "not sure how we got here"
                return null
            }
            println "or here either"
            return null
        }
        // if we are at the right edge
        if (floorMinCoord == null && ceilMinCoord == null && floorMaxCoord != null && ceilMaxCoord == null) {
            if (min > floorMaxKey) {
                return addCoordinate(min, max)
            }
            return replaceCoordinate(floorMaxCoord, floorMaxCoord.min, max)
        }
        // if we are at the right edge
        if (floorMinCoord != null && floorMaxCoord != null && ceilMinCoord == null && ceilMaxCoord != null && ceilMaxCoord == floorMinCoord) {
            if (min >= floorMinCoord.min && max <= ceilMaxCoord.max) {
                return null
            }
            println("Not sure what to do with this")
            return null
        }
        // overlapping without?
        if (floorMinCoord != null && floorMaxCoord != null && ceilMinCoord != null && ceilMaxCoord != null) {
            // this overlaps on both sides
            if (floorMinCoord != floorMaxCoord && ceilMinCoord != ceilMaxCoord && floorMaxCoord == ceilMinCoord) {

                if (min < ceilMinCoord.min && min > floorMinCoord.max && max > floorMaxCoord.max && max < ceilMaxCoord.min) {
                    return replaceCoordinate(floorMaxCoord, min, max)
                } else
                // in-between all, so just add
                if (min > floorMaxCoord.max && max < ceilMinCoord.min) {
                    return addCoordinate(min, max)
                }
                // putting on the LHS
                else if (min > floorMaxCoord.max && max < ceilMaxCoord.max) {
                    return replaceCoordinate(ceilMinCoord, min, ceilMaxCoord.max)
                }
                // putting on the RHS
                else if (min < floorMaxCoord.max && max < ceilMaxCoord.min) {
                    return replaceCoordinate(floorMinCoord, floorMinCoord.min, max)
                } else if (min < floorMaxCoord.min && max < ceilMaxCoord.min) {
                    return replaceCoordinate(floorMinCoord, floorMinCoord.min, ceilMaxCoord.max)
                } else if (min > floorMinCoord.min && min < floorMinCoord.max && max > ceilMaxCoord.min && max < ceilMaxCoord.max) {
                    return replaceCoordinate(floorMinCoord, floorMinCoord.min, ceilMaxCoord.max)
                } else {
                    int newMin = min > floorMinCoord.max ? floorMinCoord.min : min
                    int newMax = max < ceilMaxCoord.min ? max : ceilMaxCoord.max
                    return replaceCoordinate(floorMinCoord, newMin, newMax)
                }
            } else if (floorMinCoord != floorMaxCoord && ceilMinCoord == ceilMaxCoord) {
                return replaceCoordinate(floorMinCoord, Math.min(min, floorMinCoord.min), Math.max(max, ceilMaxCoord.max))
            }
            // if we have coordinates on either side
            else if (floorMinCoord == floorMaxCoord && ceilMinCoord == ceilMaxCoord && ceilMinCoord != floorMinCoord) {
                // in-between all, so just add
                if (min > floorMaxKey && max < ceilMinKey) {
                    return addCoordinate(min, max)
                }
                // putting on the LHS
                else if (min > floorMaxKey && max < ceilMaxCoord.max) {
                    return replaceCoordinate(ceilMinCoord, min, ceilMaxCoord.max)
                }
                // putting on the RHS
                else if (min < floorMaxCoord.max && max < ceilMaxCoord.min) {
                    return replaceCoordinate(floorMinCoord, floorMinCoord.min, max)
                }
                // bridging two intervals
                else if (min > floorMinCoord.min && max < ceilMaxCoord.max && min < ceilMaxCoord.min && max > floorMinCoord.max) {
                    return replaceCoordinate(floorMinCoord, floorMinCoord.min, ceilMaxCoord.max)
                } else {
                    int newMin = min > floorMinCoord.max ? floorMinCoord.min : min
                    int newMax = max < ceilMaxCoord.min ? max : ceilMaxCoord.max
                    return replaceCoordinate(floorMinCoord, newMin, newMax)
                }
            }
            // sitting on the right edge, internal
            else if (floorMinCoord == floorMaxCoord && ceilMinCoord != ceilMaxCoord && floorMaxCoord == ceilMaxCoord) {
                return null
            }
            // in the case they are in-between an existing scaffold
            else if (floorMinCoord == ceilMaxCoord && ceilMinCoord != ceilMaxCoord && floorMaxCoord != floorMinCoord && floorMaxCoord != ceilMinCoord) {
                return null
            } else if (floorMinCoord == ceilMinCoord && ceilMinCoord != ceilMaxCoord && floorMaxCoord != floorMinCoord && floorMaxCoord != ceilMinCoord) {
                return replaceCoordinate(floorMinCoord, min, ceilMaxCoord.max)
            } else if (floorMaxCoord == ceilMaxCoord && ceilMinCoord != ceilMaxCoord && floorMaxCoord != floorMinCoord && floorMinCoord != ceilMinCoord) {
                return replaceCoordinate(floorMinCoord, floorMinCoord.min, ceilMaxCoord.max)
            } else if (floorMinCoord == floorMaxCoord && floorMaxCoord == ceilMinCoord && floorMinCoord != ceilMaxCoord) {
//                return replaceCoordinate(floorMinCoord,floorMinCoord.min,ceilMaxCoord.max)
                return null
            }

            return addCoordinate(min, max)

        } else {
//            println "else else ${min}/${max}"
            return addCoordinate(min, max)
        }


    }


    @Override
    public Track projectTrack(Track trackIn) {
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
    public Coordinate projectCoordinate(int min, int max) {
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
    public Coordinate projectReverseCoordinate(int min, int max) {
        int newMin = projectReverseValue(min)
        int newMax = projectReverseValue(max)
        if (newMin < 0 && newMax < 0) return null
    }

    public Integer getOriginalLength() {
        return maxMap.values().last().max
    }

    /**
     * This allows for a space between each coordindate
     * @param buffer
     * @return
     */
    public Integer getBufferedLength(Integer buffer = 1) {
        return length + buffer * (size() - 1)
    }

    @Override
    public Integer getLength() {
        int returnValue = 0
        for (Coordinate coordinate : minMap.values()) {
            returnValue += coordinate.length;
        }
        return returnValue
    }

//    String projectSequence(String inputSequence) {
//        projectSequence(inputSequence, -1, -1, 0)
//    }

    String projectSequence(String inputSequence, Integer minCoordinate, Integer maxCoordinate) {
        projectSequence(inputSequence, minCoordinate, maxCoordinate, 0)
    }

    @Override
    String projectSequence(String inputSequence, Integer minCoordinate, Integer maxCoordinate, Integer offset) {
        String returnSequence = ""
        Iterator<Coordinate> minKeyIterator = minMap.values().iterator()
        minCoordinate = minCoordinate >= 0 ? minCoordinate : 0
        maxCoordinate = maxCoordinate >= 0 ? maxCoordinate : inputSequence.length()
        println "minCoordinate = ${minCoordinate}"
        println "maxCoordinate = ${maxCoordinate}"
        println "offset = ${offset}"
        println "# of min maps ${minMap.size()}"

        while (minKeyIterator.hasNext()) {
            Coordinate coordinate = minKeyIterator.next()
            println "coodinate coord ${coordinate.min}::${coordinate.max} vs ${inputSequence.length()}"
            Integer offsetMinCoordinate = coordinate.min + offset
            Integer offsetMaxCoordinate = coordinate.max + offset
            println "offset coord ${offsetMinCoordinate}::${offsetMaxCoordinate} vs ${inputSequence.length()}"
            println "min/max ${minCoordinate}::${maxCoordinate} vs ${inputSequence.length()}"
            // 6 cases
            // case 1, max < minCoordinate . . .ignore
            // case 5, min > maxCoordinate  . . .ignore
            if (offsetMaxCoordinate < minCoordinate || offsetMinCoordinate > maxCoordinate) {
                // do nothing
            }
            // case 6, overlaps all the way, min < minCoordinate, max > maxCoordinate, add minCoorindate, maxCoordinate
            else if (offsetMinCoordinate <= minCoordinate && offsetMaxCoordinate >= maxCoordinate) {
                returnSequence += inputSequence.substring(minCoordinate, maxCoordinate + 1)
            }
            // case 2, left-hand edge , min < minCoordinate , max > minCoordinate, add minCoordinate, max
            else if (offsetMinCoordinate <= minCoordinate && offsetMaxCoordinate >= minCoordinate) {
                returnSequence += inputSequence.substring(minCoordinate, offsetMaxCoordinate + 1)
            }
            // case 3, inside min > minCoordinate , max < maxCoordinate . . . add as-is, min/ max
            else if (offsetMinCoordinate > minCoordinate && offsetMaxCoordinate <= maxCoordinate) {
                returnSequence += inputSequence.substring(offsetMinCoordinate, offsetMaxCoordinate + 1)
            }
            // case 4, right-hand edge min < maxCoordinate , max > maxCoordinate . . . add min, maxCoordinate
            else if (offsetMinCoordinate <= maxCoordinate && offsetMaxCoordinate > maxCoordinate) {
                returnSequence += inputSequence.substring(offsetMinCoordinate, maxCoordinate + 1)
            } else {
                println "what is this error case? "
            }

        }

        return returnSequence
    }


    @Override
    public String toString() {
        String returnString = "DiscontinuousProjection{"

        minMap.values().each { it ->
            returnString += "[${it.min}::${it.max}]"
        }

        returnString += '}';

        return returnString
    }

    Integer size() {
        if (!minMap) {
            return 0
        }
        assert minMap.size() == maxMap.size()
        return minMap.size()
    }

    @Override
    Integer clear() {
        int returnValue = minMap.size()
//        assert returnValue == maxMap.size()
        minMap.clear()
        maxMap.clear()
        return returnValue
    }
}
