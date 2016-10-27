package org.bbop.apollo.gwt.shared.projection;


import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Created by nathandunn on 10/10/16.
 */
public class DiscontinuousProjection extends AbstractProjection {
    // projection from X -> X'
    TreeMap<Long, Coordinate> minMap = new TreeMap<>();
    TreeMap<Long, Coordinate> maxMap = new TreeMap<>();

    private String metadata; // metadata, potentially JSON

    /**
     * Get the coordinate value out and add some to min
     * <p>
     * To do this efficiently we simply get the interval sizes
     * and stop when we've hit all of the sizes
     *
     * @param input
     * @return
     */
    @Override
    public Long projectReverseValue(Long input) {

        Iterator<Long> minIterator = minMap.keySet().iterator();
        Iterator<Long> maxIterator = maxMap.keySet().iterator();
        Long min, max;

        // TODO: for speed generate a reverse map for quick lookup whilst doing this or another operation
        // here we can assume that the input maps onto the current length
        Long currentLength = 0l;
        Long bucketCount = 0l;
        Long previousLength = 0l;
        while (minIterator.hasNext()) {
            min = minIterator.next();
            max = maxIterator.next();
            currentLength += (max - min);
            if (currentLength + bucketCount >= input) {
                return min + input - previousLength - bucketCount;
            }
            previousLength += (max - min);
            ++bucketCount;
        }
        return UNMAPPED_VALUE;
    }

    @Override
    public Long projectValue(Long input) {
        if (minMap.isEmpty() && maxMap.isEmpty()) {
            return input;
        }

        if (input == null) {
            return UNMAPPED_VALUE;
        }

        Long floorMinKey = minMap.floorKey(input);
        Long ceilMinKey = minMap.ceilingKey(input);

        Long floorMaxKey = maxMap.floorKey(input);
        Long ceilMaxKey = maxMap.ceilingKey(input);

        if (floorMinKey == null || ceilMaxKey == null) {
            return UNMAPPED_VALUE;
        }

        // if is a hit for min and no max hit, then it is the left-most
        if (floorMinKey == ceilMinKey) {
            if (floorMaxKey == null) {
                return 0l;
            } else {
//                return input - floorMaxKey
                return projectValue(floorMaxKey) + 1;
            }
        }

        // this is the left-most still
        if (floorMinKey != ceilMinKey && floorMaxKey == null) {
            return input - floorMinKey;
        }

        // if we are at the max border
        if (floorMaxKey == ceilMaxKey) {
            return input - floorMinKey + projectValue(floorMinKey);
        }

        // if we are inbetween a ceiling max and floor min, then we are in a viable block
        if (input > floorMinKey && input < ceilMaxKey && ceilMinKey >= ceilMaxKey) {
            return input - floorMinKey + projectValue(floorMinKey);
        }

        // if we are inbetween for the last large one on the RHS
        if (floorMaxKey != ceilMaxKey && ceilMinKey == null) {
            return input - floorMinKey + projectValue(floorMinKey);
        }

//        log.debug "${input} unable to find match, returning UNMAPPED"
        return UNMAPPED_VALUE;
    }

    Coordinate addCoordinate(Long min, Long max) {
        Coordinate coordinate = new Coordinate(min, max);
        if (minMap.containsKey(min) && !maxMap.containsKey(max)) {
            throw new RuntimeException("minKey is dupe and should be replaced ${min}::${max}");
        }
        if (!minMap.containsKey(min) && maxMap.containsKey(max)) {
            throw new RuntimeException("maxKey is dupe and should be replaced ${min}::${max}");
        }
        minMap.put(min, coordinate);
        maxMap.put(max, coordinate);
        return coordinate;
    }

    /**
     * Replace an existing coordinate with a new set (e.g., an overlap)
     *
     * @param coordinate
     * @param min
     * @param max
     * @return
     */
    Coordinate replaceCoordinate(Coordinate coordinate, Long min, Long max) {
        assert minMap.containsKey(coordinate.getMin());
        assert maxMap.containsKey(coordinate.getMax());

        minMap.remove(coordinate.getMin());
        maxMap.remove(coordinate.getMax());

        Long nextMin = !minMap.isEmpty() ? minMap.higherKey(coordinate.getMin()) : null;

        Boolean doBreak = false;
        // we have to remove any overlapping elements here
        while (nextMin != null && !minMap.isEmpty() && !maxMap.isEmpty() && nextMin < max && !doBreak) {
            Coordinate nextMinCoord = minMap.get(nextMin);
            if (nextMinCoord.getMax() > min) {
                minMap.remove(nextMinCoord.getMin()) ;
                maxMap.remove(nextMinCoord.getMax()) ;
            } else {
                doBreak = true;
            }
            nextMin = minMap.higherKey(coordinate.getMin());
        }
        return addCoordinate(min, max);
    }

    Coordinate addInterval(Long min, Long max) {
        return addInterval(min, max, 0);
    }

    Coordinate addInterval(Long min, Long max, Integer padding) {
        min -= padding != 0 ? padding : 0;
        max += padding != 0 ? padding : 0;
        min = min < 0 ? 0 : min;
        assert max >= min;

        Long floorMinKey = minMap.floorKey(min);
        Long ceilMinKey = minMap.ceilingKey(min);

        Long floorMaxKey = maxMap.floorKey(max);
        Long ceilMaxKey = maxMap.ceilingKey(max);

        Coordinate floorMinCoord = floorMinKey != null ? minMap.get(floorMinKey) : null;
        Coordinate floorMaxCoord = floorMaxKey != null ? maxMap.get(floorMaxKey) : null;
        Coordinate ceilMaxCoord = ceilMaxKey != null ? maxMap.get(ceilMaxKey) : null;
        Coordinate ceilMinCoord = ceilMinKey != null ? minMap.get(ceilMinKey) : null;

        // no entries at all . . just add
        if (floorMinCoord == null && floorMaxCoord == null && ceilMinCoord == null && ceilMaxCoord == null) {
            return addCoordinate(min, max);
        } else
            // empty floor / LHS side
            if (floorMinCoord == null && floorMaxCoord == null && ceilMinCoord != null && ceilMaxCoord != null) {
                assert ceilMinCoord == ceilMaxCoord;
                if (max < ceilMinCoord.getMin()) {
                    return addCoordinate(min, max);
                }
                return replaceCoordinate(ceilMinCoord, min, ceilMinCoord.getMax());
            } else
                // empty ceil / RHS side
                if (floorMinCoord != null && floorMaxCoord != null && ceilMinCoord == null && ceilMaxCoord == null) {
//            floorMinCoord == floorMaxCoord
                    if (min > floorMaxCoord.getMax()) {
                        return addCoordinate(min, max);
                    }
                    return replaceCoordinate(floorMaxCoord, floorMinCoord.getMin(), max);
                }
        // overlapping within?
        if (floorMinCoord != null && floorMaxCoord == null && ceilMinCoord == null && ceilMaxCoord != null) {
            assert floorMinCoord == ceilMaxCoord;
            return replaceCoordinate(floorMinCoord, Math.min(min, floorMinCoord.getMin()), Math.max(max, ceilMaxCoord.getMax()));
        }
        // overlapping without?
        if (floorMinCoord == null && floorMaxCoord != null && ceilMinCoord != null && ceilMaxCoord == null) {
            assert floorMaxCoord == ceilMinCoord;
            return replaceCoordinate(floorMaxCoord, Math.min(min, floorMaxCoord.getMin()), Math.max(max, ceilMinCoord.getMax()));
        }
        // if we are internal / in the middle
        if (floorMinCoord == null && ceilMinCoord == null && floorMaxCoord != null && ceilMaxCoord != null) {
            return null;
        }
        if (floorMinCoord != null && ceilMinCoord != null && floorMaxCoord != null && ceilMaxCoord == null) {
            if (floorMinCoord == floorMaxCoord && floorMaxCoord == ceilMinCoord) {
                if (max > floorMaxCoord.getMax()) {
                    return replaceCoordinate(floorMinCoord, floorMinCoord.getMin(), max);
                }
                            }
            return null;
        }
        // if we are right on the right edge
        if (floorMinCoord == null && ceilMinCoord != null && floorMaxCoord != null && ceilMaxCoord != null) {
            if (floorMaxCoord == ceilMaxCoord && ceilMaxCoord == ceilMinCoord) {
                if (min < floorMaxCoord.getMin()) {
                    return replaceCoordinate(floorMaxCoord, min, floorMaxCoord.getMax());
                }
                                return null;
            }
                        return null;
        }
        // if we are at the right edge
        if (floorMinCoord == null && ceilMinCoord == null && floorMaxCoord != null && ceilMaxCoord == null) {
            if (min > floorMaxKey) {
                return addCoordinate(min, max);
            }
            return replaceCoordinate(floorMaxCoord, floorMaxCoord.getMin(), max);
        }
        // if we are at the right edge
        if (floorMinCoord != null && floorMaxCoord != null && ceilMinCoord == null && ceilMaxCoord != null && ceilMaxCoord == floorMinCoord) {
            if (min >= floorMinCoord.getMin() && max <= ceilMaxCoord.getMax()) {
                return null;
            }
                        return null;
        }
        // overlapping without?
        if (floorMinCoord != null && floorMaxCoord != null && ceilMinCoord != null && ceilMaxCoord != null) {
            // this overlaps on both sides
            if (floorMinCoord != floorMaxCoord && ceilMinCoord != ceilMaxCoord && floorMaxCoord == ceilMinCoord) {

                if (min < ceilMinCoord.getMin() && min > floorMinCoord.getMax() && max > floorMaxCoord.getMax() && max < ceilMaxCoord.getMin()) {
                    return replaceCoordinate(floorMaxCoord, min, max);
                } else
                    // in-between all, so just add
                    if (min > floorMaxCoord.getMax() && max < ceilMinCoord.getMin()) {
                        return addCoordinate(min, max);
                    }
                    // putting on the LHS
                    else if (min > floorMaxCoord.getMax() && max < ceilMaxCoord.getMax()) {
                        return replaceCoordinate(ceilMinCoord, min, ceilMaxCoord.getMax());
                    }
                    // putting on the RHS
                    else if (min < floorMaxCoord.getMax() && max < ceilMaxCoord.getMin()) {
                        return replaceCoordinate(floorMinCoord, floorMinCoord.getMin(), max);
                    } else if (min < floorMaxCoord.getMin() && max < ceilMaxCoord.getMin()) {
                        return replaceCoordinate(floorMinCoord, floorMinCoord.getMin(), ceilMaxCoord.getMax());
                    } else if (min > floorMinCoord.getMin() && min < floorMinCoord.getMax() && max > ceilMaxCoord.getMin() && max < ceilMaxCoord.getMax()) {
                        return replaceCoordinate(floorMinCoord, floorMinCoord.getMin(), ceilMaxCoord.getMax());
                    } else {
                        Long newMin = min > floorMinCoord.getMax() ? floorMinCoord.getMin() : min;
                        Long newMax = max < ceilMaxCoord.getMin() ? max : ceilMaxCoord.getMax();
                        return replaceCoordinate(floorMinCoord, newMin, newMax);
                    }
            } else if (floorMinCoord != floorMaxCoord && ceilMinCoord == ceilMaxCoord) {
                return replaceCoordinate(floorMinCoord, Math.min(min, floorMinCoord.getMin()), Math.max(max, ceilMaxCoord.getMax()));
            }
            // if we have coordinates on either side
            else if (floorMinCoord == floorMaxCoord && ceilMinCoord == ceilMaxCoord && ceilMinCoord != floorMinCoord) {
                // in-between all, so just add
                if (min > floorMaxKey && max < ceilMinKey) {
                    return addCoordinate(min, max);
                }
                // putting on the LHS
                else if (min > floorMaxKey && max < ceilMaxCoord.getMax()) {
                    return replaceCoordinate(ceilMinCoord, min, ceilMaxCoord.getMax());
                }
                // putting on the RHS
                else if (min < floorMaxCoord.getMax() && max < ceilMaxCoord.getMin()) {
                    return replaceCoordinate(floorMinCoord, floorMinCoord.getMin(), max);
                }
                // bridging two intervals
                else if (min > floorMinCoord.getMin() && max < ceilMaxCoord.getMax() && min < ceilMaxCoord.getMin() && max > floorMinCoord.getMax()) {
                    return replaceCoordinate(floorMinCoord, floorMinCoord.getMin(), ceilMaxCoord.getMax());
                } else {
                    Long newMin = min > floorMinCoord.getMax() ? floorMinCoord.getMin() : min;
                    Long newMax = max < ceilMaxCoord.getMin() ? max : ceilMaxCoord.getMax();
                    return replaceCoordinate(floorMinCoord, newMin, newMax);
                }
            }
            // sitting on the right edge, internal
            else if (floorMinCoord == floorMaxCoord && ceilMinCoord != ceilMaxCoord && floorMaxCoord == ceilMaxCoord) {
                return null;
            }
            // in the case they are in-between an existing scaffold
            else if (floorMinCoord == ceilMaxCoord && ceilMinCoord != ceilMaxCoord && floorMaxCoord != floorMinCoord && floorMaxCoord != ceilMinCoord) {
                return null;
            } else if (floorMinCoord == ceilMinCoord && ceilMinCoord != ceilMaxCoord && floorMaxCoord != floorMinCoord && floorMaxCoord != ceilMinCoord) {
                return replaceCoordinate(floorMinCoord, min, ceilMaxCoord.getMax());
            } else if (floorMaxCoord == ceilMaxCoord && ceilMinCoord != ceilMaxCoord && floorMaxCoord != floorMinCoord && floorMinCoord != ceilMinCoord) {
                return replaceCoordinate(floorMinCoord, floorMinCoord.getMin(), ceilMaxCoord.getMax());
            } else if (floorMinCoord == floorMaxCoord && floorMaxCoord == ceilMinCoord && floorMinCoord != ceilMaxCoord) {
                return null;
            }

            return addCoordinate(min, max);

        } else {
            return addCoordinate(min, max);
        }


    }


    @Override
    public Coordinate projectCoordinate(Long min, Long max) {
        Long newMin = projectValue(min);
        Long newMax = projectValue(max);
        if (newMin >= 0 && newMax >= 0) {
            return new Coordinate(newMin, newMax);
        } else if ((newMin < 0 && newMax < 0)) {
            return null;
        } else if (newMin >= 0) {
            // newMin is less than 0 so find the next one higher and move up
            Long floorMaxKey = projectValue(maxMap.floorKey(max));
            if (floorMaxKey > newMin) {
                return new Coordinate(newMin, floorMaxKey);
            } else {
                return null;
            }
        } else if (newMax >= 0) {
            // newMin is less than 0 so find the next one higher and move up
            Long ceilMinKey = projectValue(minMap.ceilingKey(min));
            if (ceilMinKey < newMax) {
                return new Coordinate(ceilMinKey, newMax);
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("not sure how we got here ${min}->${newMin}, ${max}->${newMax}");
        }
    }

    @Override
    public Coordinate projectReverseCoordinate(Long min, Long max) {
        Long newMin = projectReverseValue(min);
        Long newMax = projectReverseValue(max);
        if (newMin < 0 && newMax < 0) {
            return null;
        }
        return new Coordinate(newMin, newMax);
    }

    //    /**
//     * This allows for a space between each coordindate
//     *
//     * @param buffer
//     * @return
//     */
    public Long getBufferedLength(Integer buffer) {
        return getLength() + buffer * (size() - 1);
    }

    public Long getBufferedLength() {
        return getBufferedLength(1);
    }

    @Override
    public Long getLength() {
        Long returnValue = 0l;
        for (Coordinate coordinate : minMap.values()) {
            returnValue += coordinate.getLength();
        }
        return returnValue;
    }

//    String projectSequence(String inputSequence, Long minCoordinate, Long maxCoordinate) {
//        projectSequence(inputSequence, minCoordinate, maxCoordinate, 0)
//    }

    @Override
    public String projectSequence(String inputSequence, Long minCoordinate, Long maxCoordinate, Long offset) {
        String returnSequence = "";
        Iterator<Coordinate> minKeyIterator = minMap.values().iterator();
        minCoordinate = minCoordinate >= 0 ? minCoordinate : 0;
        maxCoordinate = maxCoordinate >= 0 ? maxCoordinate : inputSequence.length();

        while (minKeyIterator.hasNext()) {
            Coordinate coordinate = minKeyIterator.next();
                        Long offsetMinCoordinate = coordinate.getMin() + offset;
            Long offsetMaxCoordinate = coordinate.getMax() + offset;
                                    // 6 cases
            // case 1, max < minCoordinate . . .ignore
            // case 5, min > maxCoordinate  . . .ignore
            if (offsetMaxCoordinate < minCoordinate || offsetMinCoordinate > maxCoordinate) {
                // do nothing
            }
            // case 6, overlaps all the way, min < minCoordinate, max > maxCoordinate, add minCoorindate, maxCoordinate
            else if (offsetMinCoordinate <= minCoordinate && offsetMaxCoordinate >= maxCoordinate) {
                returnSequence += inputSequence.substring(minCoordinate.intValue(), maxCoordinate.intValue() + 1);
            }
            // case 2, left-hand edge , min < minCoordinate , max > minCoordinate, add minCoordinate, max
            else if (offsetMinCoordinate <= minCoordinate && offsetMaxCoordinate >= minCoordinate) {
                returnSequence += inputSequence.substring(minCoordinate.intValue(), offsetMaxCoordinate.intValue() + 1);
            }
            // case 3, inside min > minCoordinate , max < maxCoordinate . . . add as-is, min/ max
            else if (offsetMinCoordinate > minCoordinate && offsetMaxCoordinate <= maxCoordinate) {
                returnSequence += inputSequence.substring(offsetMinCoordinate.intValue(), offsetMaxCoordinate.intValue() + 1);
            }
            // case 4, right-hand edge min < maxCoordinate , max > maxCoordinate . . . add min, maxCoordinate
            else if (offsetMinCoordinate <= maxCoordinate && offsetMaxCoordinate > maxCoordinate) {
                returnSequence += inputSequence.substring(offsetMinCoordinate.intValue(), maxCoordinate.intValue() + 1);
            } else {
                            }

        }

        return returnSequence;
    }


    @Override
    public String toString() {
        String returnString = "DiscontinuousProjection{";

        for (Coordinate coordinate : minMap.values()) {
            returnString += "[" + coordinate.getMin() + "::" + coordinate.getMax() + "]";
        }

        returnString += '}';

        return returnString;
    }

    Integer size() {
        if (minMap.isEmpty()) {
            return 0;
        }
        assert minMap.size() == maxMap.size();
        return minMap.size();
    }

    @Override
    public Integer clear() {
        Integer returnValue = minMap.size();
        minMap.clear();
        maxMap.clear();
        return returnValue;
    }

    public Collection<Coordinate> getCoordinates() {
        assert minMap.size() == maxMap.size();
        return minMap.values();
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
