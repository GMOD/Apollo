package org.bbop.apollo

import org.bbop.apollo.projection.AbstractProjection
import org.bbop.apollo.projection.Coordinate
import org.bbop.apollo.projection.DiscontinuousProjection
import org.bbop.apollo.projection.DiscontinuousProjectionFactory
import org.bbop.apollo.projection.DuplicateTrackProjection

import org.bbop.apollo.projection.Projection
import org.bbop.apollo.projection.ReverseProjection
import org.bbop.apollo.projection.Track
import spock.lang.Specification

/**
 * Created by Nathan Dunn on 8/14/15.
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

        // in-phase
        assert new Coordinate(min:1,max:2)== discontinuousProjection.projectCoordinate(3,4)
        // right-edge
        assert null== discontinuousProjection.projectCoordinate(4,5)
        // left-edge
        assert null== discontinuousProjection.projectCoordinate(1,2)
        // right-edge overlap
        assert new Coordinate(min:1,max:2)== discontinuousProjection.projectCoordinate(3,5)
        // right-edge overlap . . . A-B?
        assert new Coordinate(min:3,max:5)== discontinuousProjection.projectCoordinate(7,9)
        // left-edge overlap
        assert new Coordinate(min:3,max:4)== discontinuousProjection.projectCoordinate(6,8)
        // AB overlap
        assert new Coordinate(min:1,max:4)== discontinuousProjection.projectCoordinate(3,8)
        // AC overlap
        assert new Coordinate(min:1,max:6)== discontinuousProjection.projectCoordinate(3,10)

        // test reverse values
        assert 2 == discontinuousProjection.projectReverseValue(0)
        assert 3 == discontinuousProjection.projectReverseValue(1)
        assert 4 == discontinuousProjection.projectReverseValue(2)
        assert 7 == discontinuousProjection.projectReverseValue(3)
        assert 8 == discontinuousProjection.projectReverseValue(4)
        assert 9 == discontinuousProjection.projectReverseValue(5)
        assert 10 == discontinuousProjection.projectReverseValue(6)

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


        // in-phase
        assert new Coordinate(min:3,max:4)== discontinuousProjection.projectCoordinate(4,5)
        // right-edge
        assert null== discontinuousProjection.projectCoordinate(2,3)
        // left-edge
        assert null== discontinuousProjection.projectCoordinate(3,4)
        // right-edge overlap
        assert new Coordinate(min:1,max:2)== discontinuousProjection.projectCoordinate(1,3)
        // right-edge overlap
        assert new Coordinate(min:3,max:4)== discontinuousProjection.projectCoordinate(4,5)
        // left-edge overlap
        assert new Coordinate(min:3,max:4)== discontinuousProjection.projectCoordinate(3,5)
        // AB overlap
        assert new Coordinate(min:1,max:4)== discontinuousProjection.projectCoordinate(1,5)
        // AC overlap
        assert new Coordinate(min:1,max:7)== discontinuousProjection.projectCoordinate(1,9)

        // test reverse values
        assert 0 == discontinuousProjection.projectReverseValue(0)
        assert 1 == discontinuousProjection.projectReverseValue(1)
        assert 2 == discontinuousProjection.projectReverseValue(2)
        assert 4 == discontinuousProjection.projectReverseValue(3)
        assert 5 == discontinuousProjection.projectReverseValue(4)
        assert 6 == discontinuousProjection.projectReverseValue(5)
        assert 8 == discontinuousProjection.projectReverseValue(6)
        assert 9 == discontinuousProjection.projectReverseValue(7)

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

    void "when adding intervals overlapping intervals should merge"(){

        given: "some intervals"
        Projection projection = new DiscontinuousProjection()


        when: "we add an interval to a null one"
        projection.addInterval(45,55)
        Coordinate coordinate = projection.minMap.values().iterator().next()

        then: "it shows up"
        projection.minMap.size()==1
        projection.maxMap.size()==1
        assert coordinate.min==45
        assert coordinate.max==55

        when : "we add within that one"
        projection.addInterval(47,53)
        coordinate = projection.minMap.values().iterator().next()

        then: "nothing happens"
        projection.minMap.size()==1
        projection.maxMap.size()==1
        assert coordinate.min==45
        assert coordinate.max==55

        when: "we add a larger one over it"
        projection.addInterval(40,60)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge and expand on both sides"
        projection.minMap.size()==1
        projection.maxMap.size()==1
        assert coordinate.min==40
        assert coordinate.max==60


        when: "we add to the continuous right edge"
        projection.addInterval(60,65)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the right edge"
        projection.minMap.size()==1
        projection.maxMap.size()==1
        assert coordinate.min==40
        assert coordinate.max==65

        when: "we add to the continuous left edge"
        projection.addInterval(35,40)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the left edge"
        projection.minMap.size()==1
        projection.maxMap.size()==1
        assert coordinate.min==35
        assert coordinate.max==65

        when: "we add to the continuous right overlap"
        projection.addInterval(62,70)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the right overlap"
        projection.minMap.size()==1
        projection.maxMap.size()==1
        assert coordinate.min==35
        assert coordinate.max==70

        when: "we add to the continuous left overlap"
        projection.addInterval(30,37)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the left overlap"
        projection.minMap.size()==1
        projection.maxMap.size()==1
        assert coordinate.min==30
        assert coordinate.max==70

        when: "we add another one to the left of all of the others"
        projection.addInterval(10,15)
        Coordinate coordinate0 = projection.minMap.values().getAt(0)
        Coordinate coordinate1 = projection.minMap.values().getAt(1)


        then: "we see another one to the left"
        projection.minMap.size()==2
        projection.maxMap.size()==2
        assert coordinate0.min==10
        assert coordinate0.max==15
        assert coordinate1.min==30
        assert coordinate1.max==70


        when: "we add another one to the right of all of the others"
        projection.addInterval(80,85)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        Coordinate coordinate2 = projection.minMap.values().getAt(2)



        then: "we see another one to the right"
        assert projection.minMap.size()==3
        assert projection.maxMap.size()==3
        assert coordinate0.min==10
        assert coordinate0.max==15
        assert coordinate1.min==30
        assert coordinate1.max==70
        assert coordinate2.min==80
        assert coordinate2.max==85



        when: "we add another one in the middle of all of the others"
        projection.addInterval(75,77)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)
        Coordinate coordinate3 = projection.minMap.values().getAt(3)

        then: "we see another one in the middle"
        assert projection.minMap.size()==4
        assert projection.maxMap.size()==4
        assert coordinate0.min==10
        assert coordinate0.max==15
        assert coordinate1.min==30
        assert coordinate1.max==70
        assert coordinate2.min==75
        assert coordinate2.max==77
        assert coordinate3.min==80
        assert coordinate3.max==85


        when: "we add another one in the middle of all of the others again"
        projection.addInterval(20,25)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)
        coordinate3 = projection.minMap.values().getAt(3)
        Coordinate coordinate4 = projection.minMap.values().getAt(4)

        then: "we see another one in the middle"
        assert projection.minMap.size()==5
        assert projection.maxMap.size()==5
        assert coordinate0.min==10
        assert coordinate0.max==15
        assert coordinate1.min==20
        assert coordinate1.max==25
        assert coordinate2.min==30
        assert coordinate2.max==70
        assert coordinate3.min==75
        assert coordinate3.max==77
        assert coordinate4.min==80
        assert coordinate4.max==85


        when: "we project outside of the center on both sides"
        projection.addInterval(19,26)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)
        coordinate3 = projection.minMap.values().getAt(3)
        coordinate4 = projection.minMap.values().getAt(4)


        then: "it should provide both on most sides"
        assert projection.minMap.size()==5
        assert projection.maxMap.size()==5
        assert coordinate0.min==10
        assert coordinate0.max==15
        assert coordinate1.min==19
        assert coordinate1.max==26
        assert coordinate2.min==30
        assert coordinate2.max==70
        assert coordinate3.min==75
        assert coordinate3.max==77
        assert coordinate4.min==80
        assert coordinate4.max==85


        when: "we add another to overlap "
        projection.addInterval(22,76)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)

        then: "we merge overlapping ones"
        assert projection.minMap.size()==3
        assert projection.maxMap.size()==3
        assert coordinate0.min==10
        assert coordinate0.max==15
        assert coordinate1.min==19
        assert coordinate1.max==77
        assert coordinate2.min==80
        assert coordinate2.max==85

        when: "we add LHS to center"
        projection.addInterval(18,22)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)

        then: "should extend center one to the left"
        assert projection.minMap.size()==3
        assert projection.maxMap.size()==3
        assert coordinate0.min==10
        assert coordinate0.max==15
        assert coordinate1.min==18
        assert coordinate1.max==77
        assert coordinate2.min==80
        assert coordinate2.max==85

        when: "we add RHS to center"
        projection.addInterval(76,78)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)

        then: "should extend center one to the left"
        assert projection.minMap.size()==3
        assert projection.maxMap.size()==3
        assert coordinate0.min==10
        assert coordinate0.max==15
        assert coordinate1.min==18
        assert coordinate1.max==78
        assert coordinate2.min==80
        assert coordinate2.max==85



        when: "we project in the center of the center"
        projection.addInterval(30,40)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)


        then: "nothing should happen"
        assert projection.minMap.size()==3
        assert projection.maxMap.size()==3
        assert coordinate0.min==10
        assert coordinate0.max==15
        assert coordinate1.min==18
        assert coordinate1.max==78
        assert coordinate2.min==80
        assert coordinate2.max==85

    }

    void "we should be able to project substring between projected coordinates"(){

        given: "we have an unprojected and projected string"
        String unprojected = "ZZZZZZAAAYYYYYYYBBXXXXXXCCWW"
        String projected = "AAABBCC"
        Track track1 = new Track(length: 28)
        track1.addCoordinate(6,8)
        track1.addCoordinate(16,17)
        track1.addCoordinate(24,25)

        when: "we add the appropriate intervals"
        DiscontinuousProjection projection = DiscontinuousProjectionFactory.getInstance().createProjection(track1)
        String projectedSequence = projection.projectSequence(unprojected)

        then: "make sure we have the same thing"
        assert projected==projectedSequence
        assert 7==projection.projectReverseValue(1)
        assert 24==projection.projectReverseValue(5)
        assert "ABBC"==projection.projectSequence(unprojected,8,24)
        assert "AAABBC"==projection.projectSequence(unprojected,4,24)
        assert "AAABBCC"==projection.projectSequence(unprojected,4,27)
        assert ""==projection.projectSequence(unprojected,9,12)
        assert "BB"==projection.projectSequence(unprojected,9,18)
        assert "BB"==projection.projectSequence(unprojected,9,17)
        assert "B"==projection.projectSequence(unprojected,9,16)
        assert "B"==projection.projectSequence(unprojected,17,22)
        assert "BC"==projection.projectSequence(unprojected,17,24)

//        when: "we project for a min / max "
////        projectedSequence = projection.projectSequence(unprojected,8,24)
//
//        then: "make sure we have the same thing"
////        assert projected!=projectedSequence
//        assert "ABBC"==projection.projectSequence(unprojected,8,24)

    }

}
