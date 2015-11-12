package org.bbop.apollo.projection

import groovy.transform.CompileStatic

/**
 * Created by Nathan Dunn on 8/24/15.
 */
@CompileStatic
public class ReverseProjection extends AbstractProjection{


    Integer trackLength

    public ReverseProjection(Track inputTrack){
        trackLength = inputTrack.length
    }

    @Override
    Integer projectReverseValue(Integer input) {
        return input
    }

    @Override
    Integer projectValue(Integer input) {
        if(input < trackLength && input >= 0 ){
            return trackLength - input - 1
        }

        return -1
    }

    @Override
    Integer getLength() {
        return null
    }

    @Override
    String projectSequence(String inputSequence,Integer minCoordinate,Integer maxCoordinate,Integer offset) {
        // TODO: make offset count?  ?
        minCoordinate = minCoordinate >=0 ? minCoordinate : 0
        maxCoordinate = maxCoordinate >=0 ? maxCoordinate : inputSequence.length()
        return inputSequence.reverse().substring( minCoordinate , maxCoordinate )
    }
}
