package org.bbop.apollo

import org.bbop.apollo.projection.AbstractProjection
import org.bbop.apollo.projection.DiscontinuousProjection
import org.bbop.apollo.projection.DiscontinuousProjectionFactory
import org.bbop.apollo.projection.DuplicateTrackProjection

import org.bbop.apollo.projection.Projection
import org.bbop.apollo.projection.ReverseProjection
import org.bbop.apollo.projection.Track
import spock.lang.Specification

/**
 * Created by nathandunn on 8/14/15.
 */
class ProjectionSpec extends Specification{

    def setup() {
    }

    def cleanup() {
    }

    void "confirm that we can generate a duplicate projection"() {

        given:
        Track track1 = new Track()

        when: "we generate a duplicate projection"
        track1.addCoordinate(4,12)
        track1.addCoordinate(70,80)
        Projection projectionTrack1To2 = new DuplicateTrackProjection()
        Track track2 = projectionTrack1To2.projectTrack(track1)

        then: "it should generate forward "
        assert track1.equals(track2)

    }

    void "confirm that we can generate a reverse projection"(){
        given:
        Track track1 = new Track(length: 100)

        when: "we generate a duplicate projection"
        track1.addCoordinate(4,12)
        track1.addCoordinate(70,80)
        Projection projectionTrack1To2 = new ReverseProjection(track1)
        Track track2 = projectionTrack1To2.projectTrack(track1)

        then: "it should generate forward "
        assert 99==projectionTrack1To2.projectValue(0)
        assert 9==projectionTrack1To2.projectValue(90)
        assert !track1.equals(track2)
    }

    void "create a discontinuous projection capable of appropriately "(){

        given:
        Track track1 = new Track(length: 11)
        DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()

        when: "we generate a duplicate projection"
        track1.addCoordinate(2,4)
        track1.addCoordinate(7,8)
        track1.addCoordinate(9,10)
        Track nullTrack = discontinuousProjection.projectTrack(track1)

        then: "with no map in the discontinuous projection, should return same"
        for(i in 0..track1.length){
            assert discontinuousProjection.projectValue(i)==i
        }
        assert nullTrack==track1

        when: "we add some intervals"
        discontinuousProjection.addInterval(2,4)
        discontinuousProjection.addInterval(7,8)
        discontinuousProjection.addInterval(9,10)

        then: "values should be mapped appropriately"
        assert AbstractProjection.UNMAPPED_VALUE == discontinuousProjection.projectValue(0)
        assert AbstractProjection.UNMAPPED_VALUE == discontinuousProjection.projectValue(1)
        assert 0 == discontinuousProjection.projectValue(2)
        assert 1 == discontinuousProjection.projectValue(3)
        assert 2 == discontinuousProjection.projectValue(4)
        assert AbstractProjection.UNMAPPED_VALUE == discontinuousProjection.projectValue(5)
        assert AbstractProjection.UNMAPPED_VALUE == discontinuousProjection.projectValue(6)
        assert 3 == discontinuousProjection.projectValue(7)
        assert 4 == discontinuousProjection.projectValue(8)
        assert 5 == discontinuousProjection.projectValue(9)
        assert 6 == discontinuousProjection.projectValue(10)

        // test reverse values
        assert 2 == discontinuousProjection.reverseProjectValue(0)
        assert 3 == discontinuousProjection.reverseProjectValue(1)
        assert 4 == discontinuousProjection.reverseProjectValue(2)
        assert 7 == discontinuousProjection.reverseProjectValue(3)
        assert 8 == discontinuousProjection.reverseProjectValue(4)
        assert 9 == discontinuousProjection.reverseProjectValue(5)
        assert 10 == discontinuousProjection.reverseProjectValue(6)

        when: "we project a track"
        Track trackOut = discontinuousProjection.projectTrack(track1)

        then: "it should properly projecto out the proper coordinates"
        assert track1.coordinateList.size()==trackOut.coordinateList.size()
        assert 0==trackOut.coordinateList.get(0).min  // 2
        assert 2==trackOut.coordinateList.get(0).max  // 4
        assert 3==trackOut.coordinateList.get(1).min  // 7
        assert 4==trackOut.coordinateList.get(1).max  // 8
        assert 5==trackOut.coordinateList.get(2).min  // 9
        assert 6==trackOut.coordinateList.get(2).max  // 10
    }

    void "try a difference discontinuous projection capable of reverse projection"(){

        given:
        Track track1 = new Track(length: 10)
        DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()

        when: "we generate a duplicate projection"
        track1.addCoordinate(2,4)
        track1.addCoordinate(7,8)
        track1.addCoordinate(9,10)
        discontinuousProjection.addInterval(0,2)
        discontinuousProjection.addInterval(4,6)
        discontinuousProjection.addInterval(8,9)

        then: "values should be mapped appropriately"
        assert 0 == discontinuousProjection.projectValue(0)
        assert 1 == discontinuousProjection.projectValue(1)
        assert 2 == discontinuousProjection.projectValue(2)
        assert AbstractProjection.UNMAPPED_VALUE == discontinuousProjection.projectValue(3)
        assert 3 == discontinuousProjection.projectValue(4)
        assert 4 == discontinuousProjection.projectValue(5)
        assert 5 == discontinuousProjection.projectValue(6)
        assert AbstractProjection.UNMAPPED_VALUE == discontinuousProjection.projectValue(7)
        assert 6 == discontinuousProjection.projectValue(8)
        assert 7 == discontinuousProjection.projectValue(9)
        assert AbstractProjection.UNMAPPED_VALUE == discontinuousProjection.projectValue(10)

        // test reverse values
        assert 0 == discontinuousProjection.reverseProjectValue(0)
        assert 1 == discontinuousProjection.reverseProjectValue(1)
        assert 2 == discontinuousProjection.reverseProjectValue(2)
        assert 4 == discontinuousProjection.reverseProjectValue(3)
        assert 5 == discontinuousProjection.reverseProjectValue(4)
        assert 6 == discontinuousProjection.reverseProjectValue(5)
        assert 8 == discontinuousProjection.reverseProjectValue(6)
        assert 9 == discontinuousProjection.reverseProjectValue(7)

        when: "we project a track"
        Track trackOut = discontinuousProjection.projectTrack(track1)

        then: "it should properly projecto out the proper coordinates"
        assert track1.coordinateList.size()==trackOut.coordinateList.size()
        assert 2==trackOut.coordinateList.get(0).min  // 2
        assert 3==trackOut.coordinateList.get(0).max  // 4
        assert -1==trackOut.coordinateList.get(1).min  // 7
        assert 6==trackOut.coordinateList.get(1).max  // 8
        assert 7==trackOut.coordinateList.get(2).min  // 9
        assert -1==trackOut.coordinateList.get(2).max  // 10

    }

    void "create discontinuous projection"(){

        given: "if we have two sets of tracks"
        Track track1 = new Track(length: 11)
        track1.addCoordinate(2,4)
        track1.addCoordinate(7,8)
        track1.addCoordinate(9,10)
        Track track2 = new Track(length: 7)
        track2.addCoordinate(0,2)
        track2.addCoordinate(3,4)
        track2.addCoordinate(5,6)


        when: "we create a projection from them"
        DiscontinuousProjection projection = DiscontinuousProjectionFactory.getInstance().createProjection(track1)
        Track track3 = projection.projectTrack(track1)


        then: "if we create a track from that projection it should be an equivalent track"
        assert track2==track3
    }

}
