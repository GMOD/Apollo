package org.bbop.apollo

import org.bbop.apollo.projection.AbstractProjection
import org.bbop.apollo.projection.Coordinate
import org.bbop.apollo.projection.DiscontinuousProjection
import org.bbop.apollo.projection.DiscontinuousProjectionFactory
import org.bbop.apollo.projection.DuplicateTrackProjection

import org.bbop.apollo.projection.ProjectionInterface
import org.bbop.apollo.projection.ReverseProjection
import org.bbop.apollo.projection.Track
import spock.lang.Specification

/**
 * Created by Nathan Dunn on 8/14/15.
 */
class DiscontinuousProjectionSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "confirm that we can generate a duplicate projection"() {

        given:
        Track track1 = new Track()

        when: "we generate a duplicate projection"
        track1.addCoordinate(4, 12)
        track1.addCoordinate(70, 80)
        ProjectionInterface projectionTrack1To2 = new DuplicateTrackProjection()
        Track track2 = projectionTrack1To2.projectTrack(track1)

        then: "it should generate forward "
        assert track1.equals(track2)

    }

    void "confirm that we can generate a reverse projection"() {
        given:
        Track track1 = new Track(length: 100)

        when: "we generate a duplicate projection"
        track1.addCoordinate(4, 12)
        track1.addCoordinate(70, 80)
        ProjectionInterface projectionTrack1To2 = new ReverseProjection(track1)
        Track track2 = projectionTrack1To2.projectTrack(track1)

        then: "it should generate forward "
        assert 99 == projectionTrack1To2.projectValue(0)
        assert 9 == projectionTrack1To2.projectValue(90)
        assert !track1.equals(track2)
    }

    void "create a discontinuous projection capable of appropriately "() {

        given:
        Track track1 = new Track(length: 11)
        DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()

        when: "we generate a duplicate projection"
        track1.addCoordinate(2, 4)
        track1.addCoordinate(7, 8)
        track1.addCoordinate(9, 10)
        Track nullTrack = discontinuousProjection.projectTrack(track1)

        then: "with no map in the discontinuous projection, should return same"
        for (i in 0..track1.length) {
            assert discontinuousProjection.projectValue(i) == i
        }
        assert nullTrack == track1

        when: "we add some intervals"
        discontinuousProjection.addInterval(2, 4)
        discontinuousProjection.addInterval(7, 8)
        discontinuousProjection.addInterval(9, 10)

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
        assert new Coordinate(min: 1, max: 2) == discontinuousProjection.projectCoordinate(3, 4)
        // right-edge
        assert null == discontinuousProjection.projectCoordinate(4, 5)
        // left-edge
        assert null == discontinuousProjection.projectCoordinate(1, 2)
        // right-edge overlap
        assert new Coordinate(min: 1, max: 2) == discontinuousProjection.projectCoordinate(3, 5)
        // right-edge overlap . . . A-B?
        assert new Coordinate(min: 3, max: 5) == discontinuousProjection.projectCoordinate(7, 9)
        // left-edge overlap
        assert new Coordinate(min: 3, max: 4) == discontinuousProjection.projectCoordinate(6, 8)
        // AB overlap
        assert new Coordinate(min: 1, max: 4) == discontinuousProjection.projectCoordinate(3, 8)
        // AC overlap
        assert new Coordinate(min: 1, max: 6) == discontinuousProjection.projectCoordinate(3, 10)

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
        assert track1.coordinateList.size() == trackOut.coordinateList.size()
        assert 0 == trackOut.coordinateList.get(0).min  // 2
        assert 2 == trackOut.coordinateList.get(0).max  // 4
        assert 3 == trackOut.coordinateList.get(1).min  // 7
        assert 4 == trackOut.coordinateList.get(1).max  // 8
        assert 5 == trackOut.coordinateList.get(2).min  // 9
        assert 6 == trackOut.coordinateList.get(2).max  // 10
    }

    void "try a difference discontinuous projection capable of reverse projection"() {

        given:
        Track track1 = new Track(length: 10)
        DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()

        when: "we generate a duplicate projection"
        track1.addCoordinate(2, 4)
        track1.addCoordinate(7, 8)
        track1.addCoordinate(9, 10)
        discontinuousProjection.addInterval(0, 2)
        discontinuousProjection.addInterval(4, 6)
        discontinuousProjection.addInterval(8, 9)

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
        assert new Coordinate(min: 3, max: 4) == discontinuousProjection.projectCoordinate(4, 5)
        // right-edge
        assert null == discontinuousProjection.projectCoordinate(2, 3)
        // left-edge
        assert null == discontinuousProjection.projectCoordinate(3, 4)
        // right-edge overlap
        assert new Coordinate(min: 1, max: 2) == discontinuousProjection.projectCoordinate(1, 3)
        // right-edge overlap
        assert new Coordinate(min: 3, max: 4) == discontinuousProjection.projectCoordinate(4, 5)
        // left-edge overlap
        assert new Coordinate(min: 3, max: 4) == discontinuousProjection.projectCoordinate(3, 5)
        // AB overlap
        assert new Coordinate(min: 1, max: 4) == discontinuousProjection.projectCoordinate(1, 5)
        // AC overlap
        assert new Coordinate(min: 1, max: 7) == discontinuousProjection.projectCoordinate(1, 9)

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
        assert track1.coordinateList.size() == trackOut.coordinateList.size()
        assert 2 == trackOut.coordinateList.get(0).min  // 2
        assert 3 == trackOut.coordinateList.get(0).max  // 4
        assert -1 == trackOut.coordinateList.get(1).min  // 7
        assert 6 == trackOut.coordinateList.get(1).max  // 8
        assert 7 == trackOut.coordinateList.get(2).min  // 9
        assert -1 == trackOut.coordinateList.get(2).max  // 10

    }

    void "create discontinuous projection"() {

        given: "if we have two sets of tracks"
        Track track1 = new Track(length: 11)
        track1.addCoordinate(2, 4)
        track1.addCoordinate(7, 8)
        track1.addCoordinate(9, 10)
        Track track2 = new Track(length: 7)
        track2.addCoordinate(0, 2)
        track2.addCoordinate(3, 4)
        track2.addCoordinate(5, 6)


        when: "we create a projection from them"
        DiscontinuousProjection projection = DiscontinuousProjectionFactory.getInstance().createProjection(track1)
        Track track3 = projection.projectTrack(track1)


        then: "if we create a track from that projection it should be an equivalent track"
        assert track2 == track3
    }

    void "when adding intervals overlapping intervals should merge"() {

        given: "some intervals"
        ProjectionInterface projection = new DiscontinuousProjection()


        when: "we add an interval to a null one"
        projection.addInterval(45, 55)
        Coordinate coordinate = projection.minMap.values().iterator().next()

        then: "it shows up"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 45
        assert coordinate.max == 55

        when: "we add within that one"
        projection.addInterval(47, 53)
        coordinate = projection.minMap.values().iterator().next()

        then: "nothing happens"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 45
        assert coordinate.max == 55

        when: "we add a larger one over it"
        projection.addInterval(40, 60)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge and expand on both sides"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 40
        assert coordinate.max == 60


        when: "we add to the continuous right edge"
        projection.addInterval(60, 65)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the right edge"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 40
        assert coordinate.max == 65

        when: "we add to the continuous left edge"
        projection.addInterval(35, 40)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the left edge"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 35
        assert coordinate.max == 65

        when: "we add to the continuous right overlap"
        projection.addInterval(62, 70)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the right overlap"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 35
        assert coordinate.max == 70

        when: "we add to the continuous left overlap"
        projection.addInterval(30, 37)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the left overlap"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 30
        assert coordinate.max == 70

        when: "we add another one to the left of all of the others"
        projection.addInterval(10, 15)
        Coordinate coordinate0 = projection.minMap.values().getAt(0)
        Coordinate coordinate1 = projection.minMap.values().getAt(1)


        then: "we see another one to the left"
        projection.minMap.size() == 2
        projection.maxMap.size() == 2
        assert coordinate0.min == 10
        assert coordinate0.max == 15
        assert coordinate1.min == 30
        assert coordinate1.max == 70


        when: "we add another one to the right of all of the others"
        projection.addInterval(80, 85)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        Coordinate coordinate2 = projection.minMap.values().getAt(2)



        then: "we see another one to the right"
        assert projection.minMap.size() == 3
        assert projection.maxMap.size() == 3
        assert coordinate0.min == 10
        assert coordinate0.max == 15
        assert coordinate1.min == 30
        assert coordinate1.max == 70
        assert coordinate2.min == 80
        assert coordinate2.max == 85



        when: "we add another one in the middle of all of the others"
        projection.addInterval(75, 77)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)
        Coordinate coordinate3 = projection.minMap.values().getAt(3)

        then: "we see another one in the middle"
        assert projection.minMap.size() == 4
        assert projection.maxMap.size() == 4
        assert coordinate0.min == 10
        assert coordinate0.max == 15
        assert coordinate1.min == 30
        assert coordinate1.max == 70
        assert coordinate2.min == 75
        assert coordinate2.max == 77
        assert coordinate3.min == 80
        assert coordinate3.max == 85


        when: "we add another one in the middle of all of the others again"
        projection.addInterval(20, 25)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)
        coordinate3 = projection.minMap.values().getAt(3)
        Coordinate coordinate4 = projection.minMap.values().getAt(4)

        then: "we see another one in the middle"
        assert projection.minMap.size() == 5
        assert projection.maxMap.size() == 5
        assert coordinate0.min == 10
        assert coordinate0.max == 15
        assert coordinate1.min == 20
        assert coordinate1.max == 25
        assert coordinate2.min == 30
        assert coordinate2.max == 70
        assert coordinate3.min == 75
        assert coordinate3.max == 77
        assert coordinate4.min == 80
        assert coordinate4.max == 85


        when: "we project outside of the center on both sides"
        projection.addInterval(19, 26)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)
        coordinate3 = projection.minMap.values().getAt(3)
        coordinate4 = projection.minMap.values().getAt(4)


        then: "it should provide both on most sides"
        assert projection.minMap.size() == 5
        assert projection.maxMap.size() == 5
        assert coordinate0.min == 10
        assert coordinate0.max == 15
        assert coordinate1.min == 19
        assert coordinate1.max == 26
        assert coordinate2.min == 30
        assert coordinate2.max == 70
        assert coordinate3.min == 75
        assert coordinate3.max == 77
        assert coordinate4.min == 80
        assert coordinate4.max == 85


        when: "we add another to overlap "
        projection.addInterval(22, 76)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)

        then: "we merge overlapping ones"
        assert projection.minMap.size() == 3
        assert projection.maxMap.size() == 3
        assert coordinate0.min == 10
        assert coordinate0.max == 15
        assert coordinate1.min == 19
        assert coordinate1.max == 77
        assert coordinate2.min == 80
        assert coordinate2.max == 85

        when: "we add LHS to center"
        projection.addInterval(18, 22)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)

        then: "should extend center one to the left"
        assert projection.minMap.size() == 3
        assert projection.maxMap.size() == 3
        assert coordinate0.min == 10
        assert coordinate0.max == 15
        assert coordinate1.min == 18
        assert coordinate1.max == 77
        assert coordinate2.min == 80
        assert coordinate2.max == 85

        when: "we add RHS to center"
        projection.addInterval(76, 78)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)

        then: "should extend center one to the left"
        assert projection.minMap.size() == 3
        assert projection.maxMap.size() == 3
        assert coordinate0.min == 10
        assert coordinate0.max == 15
        assert coordinate1.min == 18
        assert coordinate1.max == 78
        assert coordinate2.min == 80
        assert coordinate2.max == 85



        when: "we project in the center of the center"
        projection.addInterval(30, 40)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)


        then: "nothing should happen"
        assert projection.minMap.size() == 3
        assert projection.maxMap.size() == 3
        assert coordinate0.min == 10
        assert coordinate0.max == 15
        assert coordinate1.min == 18
        assert coordinate1.max == 78
        assert coordinate2.min == 80
        assert coordinate2.max == 85

    }

    void "we should be able to project substring between projected coordinates"() {

        given: "we have an unprojected and projected string"
        String unprojected = "ZZZZZZAAAYYYYYYYBBXXXXXXCCWW"
        String projected = "AAABBCC"
        Track track1 = new Track(length: 28)
        track1.addCoordinate(6, 8)
        track1.addCoordinate(16, 17)
        track1.addCoordinate(24, 25)

        when: "we add the appropriate intervals"
        DiscontinuousProjection projection = DiscontinuousProjectionFactory.getInstance().createProjection(track1)
        String projectedSequence = projection.projectSequence(unprojected,0,28)

        then: "make sure we have the same thing"
        assert projected == projectedSequence
        assert 7 == projection.projectReverseValue(1)
        assert 24 == projection.projectReverseValue(5)
        assert "ABBC" == projection.projectSequence(unprojected, 8, 24)
        assert "AAABBC" == projection.projectSequence(unprojected, 4, 24)
        assert "AAABBCC" == projection.projectSequence(unprojected, 4, 27)
        assert "" == projection.projectSequence(unprojected, 9, 12)
        assert "BB" == projection.projectSequence(unprojected, 9, 18)
        assert "BB" == projection.projectSequence(unprojected, 9, 17)
        assert "B" == projection.projectSequence(unprojected, 9, 16)
        assert "B" == projection.projectSequence(unprojected, 17, 22)
        assert "BC" == projection.projectSequence(unprojected, 17, 24)

//        when: "we project for a min / max "
////        projectedSequence = projection.projectSequence(unprojected,8,24)
//
//        then: "make sure we have the same thing"
////        assert projected!=projectedSequence
//        assert "ABBC"==projection.projectSequence(unprojected,8,24)

    }

    void "let us try to project sequence features again to make sure it works properly"() {
        given: "an input sequence"
        String inputSequence = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRST"
        Track track1 = new Track(length: 72)
        track1.addCoordinate(8, 16)
        track1.addCoordinate(24, 32)
        track1.addCoordinate(40, 56)


        when: "we project a sequence"
        DiscontinuousProjection projection = DiscontinuousProjectionFactory.getInstance().createProjection(track1)
        String projectedSequence = projection.projectSequence(inputSequence,0,72)

        then: "we should get back the projected sequence"
        assert projectedSequence == inputSequence.substring(8, 16 + 1) + inputSequence.substring(24, 32 + 1) + inputSequence.substring(40, 56 + 1)

    }

    void "overlapping projections with padding should work fine"() {

        given: "we have two sets of tracks that overlap"
        Track track1 = new Track(length: 11)
        track1.addCoordinate(2, 4)
        track1.addCoordinate(7, 8)
        track1.addCoordinate(9, 10)
        Track track2 = new Track(length: 7)
        track2.addCoordinate(0, 2)
        track2.addCoordinate(3, 4)
//        track2.addCoordinate(5,6)
        DiscontinuousProjection projection = new DiscontinuousProjection()

        when: "we create a projection out of the first of them"
        // we'll simuluate this with padding of 1
        track1.coordinateList.each {
            projection.addInterval(it.min, it.max, 1)
        }

        then: "we should make sure that we can see the padding"
        // 1-5, 6-9, 8-11  . . 1-5,6-11
        projection.minMap.size() == 2
        projection.maxMap.size() == 2

        when: "when we clear"
        projection.clear()

        then: "we should not have any left"
        assert projection.size() == 0

        when: "we add track 2 only"
        track2.coordinateList.each {
            projection.addInterval(it.min, it.max, 1)
        }

        then: "we should have the correct number from track 2"
        assert 1 == projection.size()
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert projection.minMap.values().iterator().next().max == 5
        assert projection.minMap.values().iterator().next().min == 0

        when: "when we do this again, but reverse we should have the same outcome"
        projection.clear()
        track2.coordinateList.reverse().each {
            projection.addInterval(it.min, it.max, 1)
        }

        then: "we should have the correct number from track 2"
        assert 1 == projection.size()
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert projection.minMap.values().iterator().next().max == 5
        assert projection.minMap.values().iterator().next().min == 0


        when: "we add the other track and see what happens"
        track1.coordinateList.each {
            projection.addInterval(it.min, it.max, 1)
        }

        then: "we should have the appropriate combined values"
        projection.minMap.size() == 2
        projection.maxMap.size() == 2
        assert projection.minMap.values().iterator().next().max == 5
        assert projection.minMap.values().iterator().next().min == 0
        assert projection.minMap.values().iterator().reverse().next().max == 11
        assert projection.minMap.values().iterator().reverse().next().min == 6

        when: "when we clear it and add them in verse order"
        projection.clear()
        track1.coordinateList.each {
            projection.addInterval(it.min, it.max, 1)
        }
        track2.coordinateList.each {
            projection.addInterval(it.min, it.max, 1)
        }

        then: "we should get the same outcome"
        projection.minMap.size() == 2
        projection.maxMap.size() == 2
        assert projection.minMap.values().iterator().next().max == 5
        assert projection.minMap.values().iterator().next().min == 0
        assert projection.minMap.values().iterator().reverse().next().max == 11
        assert projection.minMap.values().iterator().reverse().next().min == 6

    }

    /**
     * 694694	694915		1	221
     694959	695222	44	2	263
     695185	695546	-37	2	361
     695511	695782	-35	2	271
     695745	696068	-37	2	323
     696071	696395	3	3	324
     696559	697320	164	3	761
     697283	697566	-37	3	283
     696108	696395	-1458	3	287
     */
    void "Group overlaps should produce a nonoverlapping map"() {

        given: "a projection"
        DiscontinuousProjection projection = new DiscontinuousProjection()

        when: "we add the overlapping coordinates"
        projection.addInterval(694694, 694915)
        projection.addInterval(694959, 695222)

        then: "we should have 1"
        assert projection.size() == 2

        when: "we add some more overlapping ones"
        projection.addInterval(695185, 695546)
        projection.addInterval(695511, 695782)
        projection.addInterval(695745, 696068)
        projection.addInterval(696071, 696395)
        projection.addInterval(696559, 697320)

        then: "there should be 4 projections"
        assert projection.size() == 4

        when: "we add one more"
        projection.addInterval(697283, 697566)


        then: "4 again"
        assert projection.size() == 4

        when: "we add the last one"
        projection.addInterval(696108, 696395)

        then: "there should just be the 4"
        assert projection.size() == 4

        when: "we add one of the LHS"
        projection.addInterval(696071, 696390)


        then: "should still be four"
        assert projection.size() == 4

    }

    /**
     426970	427288		1
     427273	427960	-15	1
     427987	428349	27	2
     428394	428830	45	3
     428905	429123	75	4
     429080	429230	-43	4
     429198	429434	-32	4
     429406	429609	-28	4
     428187	428534	-1422	2
     428528	428829	-6	2
     428905	429115	76	2
     429073	429230	-42	2
     429198	429439	-32	2
     429410	429605	-29	2
     429597	430007	-8	2
     */
    void "When an overlap is sort of out of order"() {
        given: "a projection"
        DiscontinuousProjection projection = new DiscontinuousProjection()
        int index = 0

        when: "we add the overlapping coordinates"
        projection.addInterval(426970, 427288)
        projection.addInterval(427273, 427960)

        then: "we should have 1"
        assert projection.size() == 1

        when: "when we add non-verlapping coordinates"
        projection.addInterval(427987, 428349) // 2
        projection.addInterval(428394, 428830) // 3
        projection.addInterval(428905, 429123) // 4
        projection.addInterval(429080, 429230) // 4
        projection.addInterval(429198, 429434) // 4
        projection.addInterval(429406, 429609) // 4
        index = 0


        then: "we should have 4"
        assert projection.size() == 4
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 426970
                    assert coordinate.max == 427960
                    break
                case 1: assert coordinate.min == 427987
                    assert coordinate.max == 428349
                    break
                case 2: assert coordinate.min == 428394
                    assert coordinate.max == 428830
                    break
                case 3: assert coordinate.min == 428905
                    assert coordinate.max == 429609
                    break
            }
            ++index
        }

        when: "we add anoverlapping one"
        projection.addInterval(428187, 428534)
        index = 0

        then: "we should have 3"
        assert projection.size() == 3
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 426970
                    assert coordinate.max == 427960
                    break
                case 1: assert coordinate.min == 427987
                    assert coordinate.max == 428830
                    break
                case 2: assert coordinate.min == 428905
                    assert coordinate.max == 429609
                    break
            }
            ++index
        }


        when: "we add the rest of them, they should continue to overlap"
        projection.addInterval(428528, 428829)
        index = 0

        then: "we should be down to 3"
        assert projection.size() == 3
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 426970
                    assert coordinate.max == 427960
                    break
                case 1: assert coordinate.min == 427987
                    assert coordinate.max == 428830
                    break
                case 2: assert coordinate.min == 428905
                    assert coordinate.max == 429609
                    break
            }
            ++index
        }


        when: "we add the rest"
        projection.addInterval(428905, 429115)
        projection.addInterval(429073, 429230)
        index = 0

        then: "there should not be any change"
        assert projection.size() == 3
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 426970
                    assert coordinate.max == 427960
                    break
                case 1: assert coordinate.min == 427987
                    assert coordinate.max == 428830
                    break
                case 2: assert coordinate.min == 428905
                    assert coordinate.max == 429609
                    break
            }
            ++index
        }


        when: "we add too more in-between"
        projection.addInterval(429198, 429439)
        projection.addInterval(429410, 429605)

        then: "we should still have 3"
        assert projection.size() == 3

        when: "we add this last one"
        projection.addInterval(429597, 430007)

        then: "it should not blow up and we should have 2"
        assert projection.size() == 3
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 426970
                    assert coordinate.max == 427960
                    break
                case 1: assert coordinate.min == 427987
                    assert coordinate.max == 428830
                    break
                case 2: assert coordinate.min == 428905
                    assert coordinate.max == 430007
                    break
            }
            ++index
        }
    }

    /**
     285235,285658
     285628,285895
     285887,286954
     286965,287209
     287225,287371
     285192,286954
     286965,287209
     287225,288061
     */
    void "another overlapping case"() {
        given: "a discontinuous projection"
        DiscontinuousProjection projection = new DiscontinuousProjection()
        int index = 0

        when: "we add some normal intervals"
        projection.addInterval(285235, 285658)
        projection.addInterval(285628, 285895)
        projection.addInterval(285887, 286954)
        projection.addInterval(286965, 287209)
        projection.addInterval(287225, 287371)
        index = 0


        then: "we would expect 3 intervals"
        assert projection.size() == 3
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 285235
                    assert coordinate.max == 286954
                    break
                case 1: assert coordinate.min == 286965
                    assert coordinate.max == 287209
                    break
                case 2: assert coordinate.min == 287225
                    assert coordinate.max == 287371
                    break
            }
            ++index
        }


        when: "you add additional intervals"
        projection.addInterval(285192, 286954)
        projection.addInterval(286965, 287209)
        index = 0

        then: "you would expect the same, but modified"
        assert projection.size() == 3
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 285192
                    assert coordinate.max == 286954
                    break
                case 1: assert coordinate.min == 286965
                    assert coordinate.max == 287209
                    break
                case 2: assert coordinate.min == 287225
                    assert coordinate.max == 287371
                    break
            }
            ++index
        }


        when: "we add the last interval"
        projection.addInterval(287225, 288061)

        then: "we have to see if its the right one"
        assert projection.size() == 3
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 285192
                    assert coordinate.max == 286954
                    break
                case 1: assert coordinate.min == 286965
                    assert coordinate.max == 287209
                    break
                case 2: assert coordinate.min == 287225
                    assert coordinate.max == 288061
                    break
            }
            ++index
        }


    }

    /**
     1764232,1764464
     1764440,1764723
     1764736,1764943
     1764907,1765195
     1765229,1765487
     1765511,1765761
     1765764,1766416
     1764703,1765195
     1765229,1766403
     */
    void "another set of overlap"() {
        given: "a discontinuous projection"
        DiscontinuousProjection projection = new DiscontinuousProjection()
        int index = 0

        when: "we add some projections"
//        projection.addInterval(1762522,1762730)  // 1
//        projection.addInterval(1762713,1762919)  // 1
//        projection.addInterval(1762921,1763214)  // 2
//        projection.addInterval(1763198,1763448)  // 2
//        projection.addInterval(1763421,1763614)  // 2
//        projection.addInterval(1763607,1763893)  // 2

        projection.addInterval(1764232, 1764464)  // 0
        projection.addInterval(1764440, 1764723)  // 0
        projection.addInterval(1764736, 1764943)  // 1
        projection.addInterval(1764907, 1765195)  // 1
        projection.addInterval(1765229, 1765487)  // 2
        projection.addInterval(1765511, 1765761)  // 3
        projection.addInterval(1765764, 1766416)  // 4
        index = 0

        then: "we should see a few"
        assert projection.size() == 5
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 1764232
                    assert coordinate.max == 1764723
                    break
                case 1: assert coordinate.min == 1764736
                    assert coordinate.max == 1765195
                    break
                case 2: assert coordinate.min == 1765229
                    assert coordinate.max == 1765487
                    break
                case 2: assert coordinate.min == 1765511
                    assert coordinate.max == 1765761
                    break
                case 4: assert coordinate.min == 1765764
                    assert coordinate.max == 1766416
                    break
            }
            ++index
        }


        when: "we add an overlapping one"
        projection.addInterval(1764703, 1765195)
        index = 0


        then: "it should overlap properly"
        assert projection.size() == 4
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 1764232
                    assert coordinate.max == 1765195
                    break
                case 1: assert coordinate.min == 1765229
                    assert coordinate.max == 1765487
                    break
                case 2: assert coordinate.min == 1765511
                    assert coordinate.max == 1765761
                    break
                case 3: assert coordinate.min == 1765764
                    assert coordinate.max == 1766416
                    break
            }
            ++index
        }

        when: "we add the last one"
        projection.addInterval(1765229, 1766403)
        index = 0

        then: "it should not blow up"
        assert projection.size() == 2
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 1764232
                    assert coordinate.max == 1765195
                    break
                case 1: assert coordinate.min == 1765229
                    assert coordinate.max == 1766416
                    break
            }
            ++index
        }

    }

    void "another overlap case"() {
        given: "a discontinuous projection"
        DiscontinuousProjection projection = new DiscontinuousProjection()
        int index = 0

        when: "we add some intervals"
        projection.addInterval(322874, 323189) // 0
        projection.addInterval(323171, 323490) // 0
        projection.addInterval(323458, 323739) // 0
        projection.addInterval(323719, 323996) // 0
        projection.addInterval(323984, 324541) // 0
        projection.addInterval(324636, 325100) // 1
        projection.addInterval(325109, 325906) // 2
        projection.addInterval(325883, 329527) // 2
        index = 0


        then: "we should see 1"
        assert projection.size() == 3
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 322874
                    assert coordinate.max == 324541
                    break
                case 1: assert coordinate.min == 324636
                    assert coordinate.max == 325100
                    break
                case 2: assert coordinate.min == 325109
                    assert coordinate.max == 329527
                    break
            }
            ++index
        }

        when: "we add the last one"
        projection.addInterval(323453, 329527)
        index = 0

        then: "we should see the same one"
        assert projection.size() == 1
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 322874
                    assert coordinate.max == 329527
                    break
            }
            ++index
        }

    }

    void "another overlap edgecase"(){

        given: "a discontinuous projection"
        DiscontinuousProjection projection = new DiscontinuousProjection()
        int index = 0

        when: "we add a series of intervals"
        projection.addInterval(411456,411745) // 0
        projection.addInterval(411775,411934) // 1
        projection.addInterval(412094,412542) // 2
        projection.addInterval(412570,412901) // 3
        projection.addInterval(412977,414637) // 4
        projection.addInterval(412312,412542) // 2
        index = 0

        then: "we should get 2"
        assert projection.size() == 5
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 411456
                    assert coordinate.max == 411745
                    break
                case 1: assert coordinate.min == 411775
                    assert coordinate.max == 411934
                    break
                case 2: assert coordinate.min == 412094
                    assert coordinate.max == 412542
                    break
                case 3: assert coordinate.min == 412570
                    assert coordinate.max == 412901
                    break
                case 4: assert coordinate.min == 412977
                    assert coordinate.max == 414637
                    break
            }
            ++index
        }


        when: "we add the last one"
        projection.addInterval(412570,413980) // 1
        index = 0

        then: "we should get the proper solution"
        assert projection.size() == 5
        for (Coordinate coordinate in projection.minMap.values()) {
            switch (index) {
                case 0: assert coordinate.min == 411456
                    assert coordinate.max == 411745
                    break
                case 1: assert coordinate.min == 411775
                    assert coordinate.max == 411934
                    break
                case 2: assert coordinate.min == 412094
                    assert coordinate.max == 412542
                    break
                case 3: assert coordinate.min == 412570
                    assert coordinate.max == 412901
                    break
                case 4: assert coordinate.min == 412977
                    assert coordinate.max == 414637
                    break
            }
            ++index
        }
    }

}
