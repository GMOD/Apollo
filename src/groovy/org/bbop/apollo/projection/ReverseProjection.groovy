package org.bbop.apollo.projection

/**
 * Created by ndunn on 8/24/15.
 */
public class ReverseProjection extends AbstractProjection{


    Integer trackLength

    public ReverseProjection(Track inputTrack){
        trackLength = inputTrack.length
    }

    @Override
    Integer reverseProjectValue(Integer input) {
        return input
    }

    @Override
    Integer projectValue(Integer input) {
        if(input < trackLength && input >= 0 ){
            return trackLength - input - 1
        }

        return -1
    }

}
