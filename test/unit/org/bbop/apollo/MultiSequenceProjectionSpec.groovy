package org.bbop.apollo

import org.bbop.apollo.projection.*
import spock.lang.Ignore
import spock.lang.IgnoreRest
import spock.lang.Specification

/**
 * Created by Nathan Dunn on 8/14/15.
 */
class MultiSequenceProjectionSpec extends Specification {


    void "when adding intervals overlapping intervals should merge"() {

        given: "some intervals"
        ProjectionSequence projectionSequence = new ProjectionSequence(order: 0)
        ProjectionInterface projection = new MultiSequenceProjection()


        when: "we add an interval to a null one"
        projection.addProjectionSequences([projectionSequence])
        projection.addInterval(45, 55, projectionSequence)
        Coordinate coordinate = projection.minMap.values().iterator().next()

        then: "it shows up"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 45
        assert coordinate.max == 55

        when: "we add within that one"
        projection.addInterval(47, 53, projectionSequence)
        coordinate = projection.minMap.values().iterator().next()

        then: "nothing happens"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 45
        assert coordinate.max == 55

        when: "we add a larger one over it"
        projection.addInterval(40, 60, projectionSequence)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge and expand on both sides"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 40
        assert coordinate.max == 60


        when: "we add to the continuous right edge"
        projection.addInterval(60, 65, projectionSequence)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the right edge"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 40
        assert coordinate.max == 65

        when: "we add to the continuous left edge"
        projection.addInterval(35, 40, projectionSequence)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the left edge"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 35
        assert coordinate.max == 65

        when: "we add to the continuous right overlap"
        projection.addInterval(62, 70, projectionSequence)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the right overlap"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 35
        assert coordinate.max == 70

        when: "we add to the continuous left overlap"
        projection.addInterval(30, 37, projectionSequence)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the left overlap"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 30
        assert coordinate.max == 70

        when: "we add another one to the left of all of the others"
        projection.addInterval(10, 15, projectionSequence)
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
        projection.addInterval(80, 85, projectionSequence)
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
        projection.addInterval(75, 77, projectionSequence)
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
        projection.addInterval(20, 25, projectionSequence)
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
        projection.addInterval(19, 26, projectionSequence)
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
        projection.addInterval(22, 76, projectionSequence)
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
        projection.addInterval(18, 22, projectionSequence)
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
        projection.addInterval(76, 78, projectionSequence)
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
        projection.addInterval(30, 40, projectionSequence)
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
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
        )// from 0-99
        MultiSequenceProjection projection = new MultiSequenceProjection()
        projection.addProjectionSequences([sequence1])

        when: "we add the overlapping coordinates"
        projection.addInterval(694694, 694915, sequence1)
        projection.addInterval(694959, 695222, sequence1)

        then: "we should have 1"
        assert projection.size() == 2

        when: "we add some more overlapping ones"
        projection.addInterval(695185, 695546, sequence1)
        projection.addInterval(695511, 695782, sequence1)
        projection.addInterval(695745, 696068, sequence1)
        projection.addInterval(696071, 696395, sequence1)
        projection.addInterval(696559, 697320, sequence1)

        then: "there should be 4 projections"
        assert projection.size() == 4

        when: "we add one more"
        projection.addInterval(697283, 697566, sequence1)


        then: "4 again"
        assert projection.size() == 4

        when: "we add the last one"
        projection.addInterval(696108, 696395, sequence1)

        then: "there should just be the 4"
        assert projection.size() == 4

        when: "we add one of the LHS"
        projection.addInterval(696071, 696390, sequence1)


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
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
        )// from 0-99
        MultiSequenceProjection projection = new MultiSequenceProjection()
        int index = 0
        projection.addProjectionSequences([sequence1])

        when: "we add the overlapping coordinates"
        projection.addInterval(426970, 427288, sequence1)
        projection.addInterval(427273, 427960, sequence1)

        then: "we should have 1"
        assert projection.size() == 1

        when: "when we add non-verlapping coordinates"
        projection.addInterval(427987, 428349, sequence1) // 2
        projection.addInterval(428394, 428830, sequence1) // 3
        projection.addInterval(428905, 429123, sequence1) // 4
        projection.addInterval(429080, 429230, sequence1) // 4
        projection.addInterval(429198, 429434, sequence1) // 4
        projection.addInterval(429406, 429609, sequence1) // 4
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
        projection.addInterval(428187, 428534, sequence1)
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
        projection.addInterval(428528, 428829, sequence1)
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
        projection.addInterval(428905, 429115, sequence1)
        projection.addInterval(429073, 429230, sequence1)
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
        projection.addInterval(429198, 429439, sequence1)
        projection.addInterval(429410, 429605, sequence1)

        then: "we should still have 3"
        assert projection.size() == 3

        when: "we add this last one"
        projection.addInterval(429597, 430007, sequence1)

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
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
        )// from 0-99
        MultiSequenceProjection projection = new MultiSequenceProjection()
        projection.addProjectionSequences([sequence1])
        int index = 0

        when: "we add some normal intervals"
        projection.addInterval(285235, 285658, sequence1)
        projection.addInterval(285628, 285895, sequence1)
        projection.addInterval(285887, 286954, sequence1)
        projection.addInterval(286965, 287209, sequence1)
        projection.addInterval(287225, 287371, sequence1)
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
        projection.addInterval(285192, 286954, sequence1)
        projection.addInterval(286965, 287209, sequence1)
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
        projection.addInterval(287225, 288061, sequence1)

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
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
        )// from 0-99
        MultiSequenceProjection projection = new MultiSequenceProjection()
        projection.addProjectionSequences([sequence1])
        int index = 0

        when: "we add some projections"
        projection.addInterval(1764232, 1764464, sequence1)  // 0
        projection.addInterval(1764440, 1764723, sequence1)  // 0
        projection.addInterval(1764736, 1764943, sequence1)  // 1
        projection.addInterval(1764907, 1765195, sequence1)  // 1
        projection.addInterval(1765229, 1765487, sequence1)  // 2
        projection.addInterval(1765511, 1765761, sequence1)  // 3
        projection.addInterval(1765764, 1766416, sequence1)  // 4
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
        projection.addInterval(1764703, 1765195, sequence1)
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
        projection.addInterval(1765229, 1766403, sequence1)
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
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
        )// from 0-99
        MultiSequenceProjection projection = new MultiSequenceProjection()
        int index = 0
        projection.addProjectionSequences([sequence1])

        when: "we add some intervals"
        projection.addInterval(322874, 323189, sequence1) // 0
        projection.addInterval(323171, 323490, sequence1) // 0
        projection.addInterval(323458, 323739, sequence1) // 0
        projection.addInterval(323719, 323996, sequence1) // 0
        projection.addInterval(323984, 324541, sequence1) // 0
        projection.addInterval(324636, 325100, sequence1) // 1
        projection.addInterval(325109, 325906, sequence1) // 2
        projection.addInterval(325883, 329527, sequence1) // 2
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
        projection.addInterval(323453, 329527, sequence1)
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

    void "another overlap edgecase"() {

        given: "a discontinuous projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
        )// from 0-99
        MultiSequenceProjection projection = new MultiSequenceProjection()
        int index = 0
        projection.addProjectionSequences([sequence1])

        when: "we add a series of intervals"
        projection.addInterval(411456, 411745, sequence1) // 0
        projection.addInterval(411775, 411934, sequence1) // 1
        projection.addInterval(412094, 412542, sequence1) // 2
        projection.addInterval(412570, 412901, sequence1) // 3
        projection.addInterval(412977, 414637, sequence1) // 4
        projection.addInterval(412312, 412542, sequence1) // 2
        index = 0

        then: "we should get 2"
        assert projection.size() == 5
        for (Coordinate coordinate in projection.listCoordinates()) {
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
        projection.addInterval(412570, 413980, sequence1) // 1
        index = 0

        then: "we should get the proper solution"
        assert projection.size() == 5
        for (Coordinate coordinate in projection.listCoordinates()) {
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

    /**
     * map onto
     * input:
     * seq1 a = 10-12
     * seq1 b = 22-25
     * seq2 c = 23-27
     * seq2 d = 60-63
     *
     * folded
     * seq1 a = 0-2
     * seq1 b = 3-6
     * seq2 c = 7-11
     * seq2 d = 12-15
     *
     * // lenghts + offsets should include buffers . . .
     * seq1 . . offset = 0, length = 5 +1 = 6
     * seq2 . . offset = 6 (6+1) = 7, length = 7 + 1 = 8
     */
    void "explicitly test multiple scaffolds"() {

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 100
                , start: 0
                , end: 100
        )// from 0-99
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 2
                , name: "Sequence2"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 100
                , start: 0
                , end: 100
        ) // from 100-200
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2])
        Location location1 = new Location(min: 10, max: 12, sequence: sequence1)
        Location location2 = new Location(min: 22, max: 25, sequence: sequence1)
        Location location3 = new Location(min: 23, max: 27, sequence: sequence2)
        Location location4 = new Location(min: 60, max: 63, sequence: sequence2)



        when: "we create some intervals for a few scaffolds"
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.addLocation(location2)
        multiSequenceProjection.addLocation(location3)
        multiSequenceProjection.addLocation(location4)
        multiSequenceProjection.calculateOffsets()
        List<Coordinate> coordinateCollection = multiSequenceProjection.listCoordinates()
        List<ProjectionSequence> projectionSequenceList = multiSequenceProjection.sequenceDiscontinuousProjectionMap.keySet() as List<ProjectionSequence>
        Integer offset = multiSequenceProjection.projectedSequences.first().unprojectedLength
        ProjectionSequence projectionSequence1 = multiSequenceProjection.getProjectionSequence(10)
        ProjectionSequence projectionSequence2 = multiSequenceProjection.getProjectionSequence(60 + offset)

        then: "we should get a single projection of size 4"
        assert multiSequenceProjection.size() == 4
        coordinateCollection.get(0).min == 10
        coordinateCollection.get(0).max == 12
        coordinateCollection.get(1).min == 22
        coordinateCollection.get(1).max == 25
        coordinateCollection.get(2).min == 23
        coordinateCollection.get(2).max == 27
        coordinateCollection.get(3).min == 60
        coordinateCollection.get(3).max == 63
        assert 0 == projectionSequenceList.get(0).offset
        assert 6 == multiSequenceProjection.sequenceDiscontinuousProjectionMap.get(projectionSequenceList.get(0)).bufferedLength
        assert 6 == projectionSequenceList.get(1).offset
        assert 8 == multiSequenceProjection.sequenceDiscontinuousProjectionMap.get(projectionSequenceList.get(1)).bufferedLength
        assert "Sequence1" == projectionSequence1.name
        assert "Sequence2" == projectionSequence2.name
        assert 0 == projectionSequence1.offset
        assert 6 == projectionSequence2.offset
        assert 0 == projectionSequence1.originalOffset
        assert 100 == projectionSequence2.originalOffset
        assert 6 == multiSequenceProjection.getProjectionSequence(60 + offset).offset

        assert 0 == multiSequenceProjection.projectValue(10)
        assert 2 == multiSequenceProjection.projectValue(12)
        assert 3 == multiSequenceProjection.projectValue(22)
        assert 6 == multiSequenceProjection.projectValue(25)
        assert 6 == multiSequenceProjection.projectValue(offset + 23)
        assert 10 == multiSequenceProjection.projectValue(offset + 27)
        assert 11 == multiSequenceProjection.projectValue(offset + 60)
        assert 14 == multiSequenceProjection.projectValue(offset + 63)


        assert 10 == multiSequenceProjection.projectReverseValue(0)
        assert 12 == multiSequenceProjection.projectReverseValue(2)
        assert 22 == multiSequenceProjection.projectReverseValue(3)
        assert 23 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(6)
        assert 24 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(7)
        assert 27 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(10)
        assert 60 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(11)
        assert 63 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(14)

        when: "we project a sequence through these coordinates"
        // length should be 200
        String inputSequence = "ATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCA"
        String projectedSequence = multiSequenceProjection.projectSequence(inputSequence, 0, 200, 0)

        then: "we should confirm that both the input and retrieved sequence are correct"
        assert 200 == inputSequence.length()
        assert 100 == offset
        assert inputSequence.substring(10, 12) == projectedSequence.substring(0, 2)
        assert inputSequence.substring(22, 25) == projectedSequence.substring(3, 6)
        assert inputSequence.substring(23 + offset, 27 + offset) == projectedSequence.substring(7, 11)
        assert inputSequence.substring(60 + offset, 63 + offset) == projectedSequence.substring(12, 15)
        assert 16 == projectedSequence.length()

        when: "we project a sequence through these smaller coordinates"
        // length should be 200
        projectedSequence = multiSequenceProjection.projectSequence(inputSequence, 50, 150, 0)
//        Integer offset = multiSequenceProjection.projectedSequences.first().unprojectedLength

        then: "we should confirm that both the input and retrieved sequence are correct"
        assert 100 == offset
        assert 5 == projectedSequence.length()
        assert inputSequence.substring(23 + offset, 27 + offset) == projectedSequence.substring(0, 4)
    }

    void "more multi-scaffold tests"() {

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 50
                , start: 0
                , end: 50
        )// from 0-49
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 2
                , name: "Sequence2"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 75
                , start: 0
                , end: 75
        ) // from 50-124
        ProjectionSequence sequence3 = new ProjectionSequence(
                id: 3
                , name: "Sequence3"
                , organism: "Human"
                , order: 2
                , unprojectedLength: 25
                , start: 0
                , end: 25
        ) // from 125-149
        ProjectionSequence sequence4 = new ProjectionSequence(
                id: 4
                , name: "Sequence4"
                , organism: "Human"
                , order: 3
                , unprojectedLength: 50
                , start: 0
                , end: 50
        ) // from 150-200
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2, sequence3, sequence4])
        Location location1 = new Location(min: 10, max: 12, sequence: sequence1) // 3
        Location location2 = new Location(min: 22, max: 25, sequence: sequence1) // 4
        Location location3 = new Location(min: 23, max: 27, sequence: sequence2)  // 5
        Location location4 = new Location(min: 60, max: 63, sequence: sequence2)  // 4
        Location location5 = new Location(min: 5, max: 10, sequence: sequence3)   // 6
        Location location6 = new Location(min: 10, max: 12, sequence: sequence4)  // 3
        // total 25


        when: "we create some intervals for a few scaffolds"
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.addLocation(location2)
        multiSequenceProjection.addLocation(location3)
        multiSequenceProjection.addLocation(location4)
        multiSequenceProjection.addLocation(location5)
        multiSequenceProjection.addLocation(location6)
        multiSequenceProjection.calculateOffsets()
        List<Coordinate> coordinateCollection = multiSequenceProjection.listCoordinates()
        List<ProjectionSequence> projectionSequenceList = multiSequenceProjection.sequenceDiscontinuousProjectionMap.keySet() as List<ProjectionSequence>

        then: "we should get a single projection of size 4"
        // TODO: TEST 4 cases in MultiSequenceProject projectSequence!!!!!!!!
        assert multiSequenceProjection.size() == 6

        when: "we project a sequence through these coordinates"
        // length should be 200
        String inputSequence = "ATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCA"
        String projectedSequence = multiSequenceProjection.projectSequence(inputSequence, 0, 200, 0)
        Integer offset = multiSequenceProjection.projectedSequences.first().unprojectedLength

        then: "we should confirm that both the input and retrieved sequence are correct"
        assert 200 == inputSequence.length()
        assert 50 == offset
        assert 25 == projectedSequence.length()
        assert inputSequence.substring(10, 12) == projectedSequence.substring(0, 2)
        assert inputSequence.substring(22, 25) == projectedSequence.substring(3, 6)
        assert inputSequence.substring(23 + offset, 27 + offset) == projectedSequence.substring(7, 11)
        assert inputSequence.substring(60 + offset, 63 + offset) == projectedSequence.substring(12, 15)

        when: "case 1 and 2: we project a sequence through these smaller coordinates "
        // length should be 200
        projectedSequence = multiSequenceProjection.projectSequence(inputSequence, 50, 150, 0)
//        Integer offset = multiSequenceProjection.projectedSequences.first().unprojectedLength

        then: "we should confirm that both the input and retrieved sequence are correct"
        assert 15 == projectedSequence.length()

        when: "we attempt case 3: a subset of a projection sequence"
        projectedSequence = multiSequenceProjection.projectSequence(inputSequence, 60, 120, 0)

        then: "we should see only see all of the coordinates on sequence 3"
        assert 9 == projectedSequence.length()

        when: "we attempt case 4 (and also 1 and 2): we overlap the entire projection sequence space"
        projectedSequence = multiSequenceProjection.projectSequence(inputSequence, 20, 8 + 125, 0)

        then: "we will just project tne entire thing"
        assert 4 + 9 + 4 == projectedSequence.length()

    }

    void "we can get get the proper projection from two contiguous scaffolds"() {

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 100
                , start: 10
                , end: 12
        )// from 0-99
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 100
                , start: 22
                , end: 25
        )// from 0-99
        ProjectionSequence sequence3 = new ProjectionSequence(
                id: 2
                , name: "Sequence2"
                , organism: "Human"
                , order: 2
                , unprojectedLength: 200
                , start: 60
                , end: 63
        ) // from 100-300
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2, sequence3])
        Location location1 = new Location(min: 10, max: 12, sequence: sequence1)
        Location location2 = new Location(min: 22, max: 25, sequence: sequence2)
        Location location4 = new Location(min: 60, max: 63, sequence: sequence3)

        when: "we add the locations"
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.addLocation(location2)
        multiSequenceProjection.addLocation(location4)
        multiSequenceProjection.calculateOffsets()
        ProjectionSequence projectionSequence1 = multiSequenceProjection.getProjectionSequence(10)
        ProjectionSequence projectionSequence2 = multiSequenceProjection.getProjectionSequence(22)
        ProjectionSequence projectionSequence3 = multiSequenceProjection.getProjectionSequence(60 + sequence1.unprojectedLength)

        then: "we should be able to get out the proper projection sequence"
        assert "Sequence1" == projectionSequence1.name
        assert "Sequence1" == projectionSequence2.name
        assert "Sequence2" == projectionSequence3.name
        assert 0 == projectionSequence1.offset
        assert 2 == projectionSequence2.offset
        assert 2 + 3 == projectionSequence3.offset
        assert 0 == projectionSequence1.originalOffset
        assert 0 == projectionSequence2.originalOffset
        assert projectionSequence1.unprojectedLength == projectionSequence3.originalOffset
        multiSequenceProjection.getProjectionSequence(10).order == 0
        multiSequenceProjection.getProjectionSequence(12).order == 0
        multiSequenceProjection.getProjectionSequence(10).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(12).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(22).order == 1
        multiSequenceProjection.getProjectionSequence(25).order == 1
        multiSequenceProjection.getProjectionSequence(22).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(25).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(60 + sequence2.unprojectedLength).order == 2
        multiSequenceProjection.getProjectionSequence(63 + sequence2.unprojectedLength).order == 2
        multiSequenceProjection.getProjectionSequence(60 + sequence2.unprojectedLength).name == "Sequence2"
        multiSequenceProjection.getProjectionSequence(63 + sequence2.unprojectedLength).name == "Sequence2"


    }

    void "we can get get the proper projection from three contiguous scaffolds"() {

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 100
                , start: 10
                , end: 12
        )// from 0-99
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 100
                , start: 22
                , end: 25
        )// from 0-99
        ProjectionSequence sequence3 = new ProjectionSequence(
                id: 2
                , name: "Sequence1"
                , organism: "Human"
                , order: 2
                , unprojectedLength: 100
                , start: 60
                , end: 63
        ) // from 100-200
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2, sequence3])
        Location location1 = new Location(min: 10, max: 12, sequence: sequence1)
        Location location2 = new Location(min: 22, max: 25, sequence: sequence2)
        Location location4 = new Location(min: 60, max: 63, sequence: sequence3)
//        Location location3 = new Location( min: 23,max: 27,sequence: sequence2 )

        when: "we add the locations"
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.addLocation(location2)
//        multiSequenceProjection.addLocation(location3)
        multiSequenceProjection.addLocation(location4)
        multiSequenceProjection.calculateOffsets()

        then: "we should be able to get out the proper projection sequence"
        multiSequenceProjection.getProjectionSequence(10).order == 0
        multiSequenceProjection.getProjectionSequence(12).order == 0
        multiSequenceProjection.getProjectionSequence(10).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(12).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(22).order == 1
        multiSequenceProjection.getProjectionSequence(25).order == 1
        multiSequenceProjection.getProjectionSequence(22).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(25).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(60).order == 2
        multiSequenceProjection.getProjectionSequence(63).order == 2
        multiSequenceProjection.getProjectionSequence(60).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(63).name == "Sequence1"


    }

    void "we can get get the proper projection from three contiguous scaffolds out of order"() {

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 100
                , start: 10
                , end: 12
        )// from 0-99
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 2
                , unprojectedLength: 100
                , start: 22
                , end: 25
        )// from 0-99
        ProjectionSequence sequence3 = new ProjectionSequence(
                id: 2
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 100
                , start: 60
                , end: 63
        ) // from 100-200
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2, sequence3])
        Location location1 = new Location(min: 10, max: 12, sequence: sequence1)
        Location location2 = new Location(min: 22, max: 25, sequence: sequence2)
        Location location4 = new Location(min: 60, max: 63, sequence: sequence3)
//        Location location3 = new Location( min: 23,max: 27,sequence: sequence2 )

        when: "we add the locations"
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.addLocation(location2)
//        multiSequenceProjection.addLocation(location3)
        multiSequenceProjection.addLocation(location4)
        multiSequenceProjection.calculateOffsets()
        ProjectionSequence projectionSequence1 = multiSequenceProjection.getProjectionSequence(10)
        ProjectionSequence projectionSequence2 = multiSequenceProjection.getProjectionSequence(22)
        ProjectionSequence projectionSequence3 = multiSequenceProjection.getProjectionSequence(60)

        then: "we should be able to get out the proper projection sequence"
        assert projectionSequence1 == multiSequenceProjection.getProjectionSequence(12)
        assert projectionSequence2 == multiSequenceProjection.getProjectionSequence(25)
        assert projectionSequence3 == multiSequenceProjection.getProjectionSequence(63)
        multiSequenceProjection.getProjectionSequence(10).order == 1
        multiSequenceProjection.getProjectionSequence(12).order == 1
        multiSequenceProjection.getProjectionSequence(10).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(12).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(22).order == 2
        multiSequenceProjection.getProjectionSequence(25).order == 2
        multiSequenceProjection.getProjectionSequence(22).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(25).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(60).order == 0
        multiSequenceProjection.getProjectionSequence(63).order == 0
        multiSequenceProjection.getProjectionSequence(60).name == "Sequence1"
        multiSequenceProjection.getProjectionSequence(63).name == "Sequence1"

        // we should be able to project the proper value as well
        assert 0 == projectionSequence3.offset
        assert 0 == projectionSequence3.originalOffset
        assert 3 == projectionSequence1.offset
        assert 0 == projectionSequence1.originalOffset
        assert 5 == projectionSequence2.offset
        assert 0 == projectionSequence2.originalOffset
        assert 0 == multiSequenceProjection.projectValue(60)
        assert 3 == multiSequenceProjection.projectValue(63)
        assert 3 == multiSequenceProjection.projectValue(10)
        assert 5 == multiSequenceProjection.projectValue(12)
        assert 5 == multiSequenceProjection.projectValue(22)
        assert 8 == multiSequenceProjection.projectValue(25)

    }


    void "simple forward and reverse projection "() {

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 100
                , start: 0
                , end: 100
        )// from 0-99
        MultiSequenceProjection multiSequenceProjection1 = new MultiSequenceProjection()
        multiSequenceProjection1.addProjectionSequences([sequence1])
        Location location1 = new Location(min: 0, max: 100, sequence: sequence1)

        when: "we add a location "
        multiSequenceProjection1.addLocation(location1)
        multiSequenceProjection1.calculateOffsets()

        then: "if we retrieve the projection it should be fine"
        assert 10 == multiSequenceProjection1.projectValue(10)
        assert 12 == multiSequenceProjection1.projectValue(12)
        assert multiSequenceProjection1.isValid()
        assert 10 == multiSequenceProjection1.projectReverseValue(10)
        assert 12 == multiSequenceProjection1.projectReverseValue(12)

        when: "we set it to a reverse projection "
        sequence1.reverse = true

        then: "it should be reversed"
        assert sequence1.length - 10 == multiSequenceProjection1.projectValue(10)
        assert sequence1.length - 12 == multiSequenceProjection1.projectValue(12)
        assert multiSequenceProjection1.isValid()
        assert 10 == multiSequenceProjection1.projectReverseValue(sequence1.length - 10)
        assert 12 == multiSequenceProjection1.projectReverseValue(sequence1.length - 12)


    }

    void "simple forward and reverse projection for multiple sequences"() {

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 100
                , start: 0
                , end: 100
        )// from 0-99
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 1
                , name: "Sequence2"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 50
                , start: 0
                , end: 50
        )// from 0-99
        ProjectionSequence sequence3 = new ProjectionSequence(
                id: 1
                , name: "Sequence3"
                , organism: "Human"
                , order: 2
                , unprojectedLength: 75
                , start: 0
                , end: 75
        )// from 0-99
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2, sequence3])
        Location location1 = new Location(min: 0, max: 100, sequence: sequence1)
        Location location2 = new Location(min: 0, max: 50, sequence: sequence2)
        Location location3 = new Location(min: 0, max: 75, sequence: sequence3)

        when: "we add a location "
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.addLocation(location2)
        multiSequenceProjection.addLocation(location3)
        multiSequenceProjection.calculateOffsets()
        // default setting
        sequence1.reverse = false
        sequence2.reverse = false
        sequence3.reverse = false

        then: "if we retrieve the projection it should be fine"
        assert 10 == multiSequenceProjection.projectValue(10)
        assert 12 == multiSequenceProjection.projectValue(12)
        assert multiSequenceProjection.isValid()
        assert 10 == multiSequenceProjection.projectReverseValue(10)
        assert 12 == multiSequenceProjection.projectReverseValue(12)

        // add 1 for offset
        assert 10 + sequence1.end == multiSequenceProjection.projectValue(10 + sequence1.unprojectedLength)
        assert 12 + sequence1.end == multiSequenceProjection.projectValue(12 + sequence1.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 10 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(10 + sequence1.end)
        assert 12 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(12 + sequence1.end)

        // add 2 for offset
        assert 10 + sequence1.end + sequence2.end == multiSequenceProjection.projectValue(10 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert 12 + sequence1.end + sequence2.end == multiSequenceProjection.projectValue(12 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 10 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(10 + sequence1.end + sequence2.end)
        assert 12 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(12 + sequence1.end + sequence2.end)

        when: "we set it to a reverse projection "
        sequence1.reverse = true
        sequence2.reverse = false
        sequence3.reverse = false

        then: "it should be reversed in the first, but not the lat"
        assert sequence1.length - 10 == multiSequenceProjection.projectValue(10)
        assert sequence1.length - 12 == multiSequenceProjection.projectValue(12)
        assert multiSequenceProjection.isValid()
        assert 10 == multiSequenceProjection.projectReverseValue(sequence1.length - 10)
        assert 12 == multiSequenceProjection.projectReverseValue(sequence1.length - 12)

        assert 10 + sequence1.end + 1 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == multiSequenceProjection.projectValue(10 + sequence1.unprojectedLength)
        assert 12 + sequence1.end + 1 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == multiSequenceProjection.projectValue(12 + sequence1.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 10 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(10 + sequence1.end + 1 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH)
        assert 12 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(12 + sequence1.end + 1 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH)

        assert 10 + sequence1.end + sequence2.end + 2 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == multiSequenceProjection.projectValue(10 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert 12 + sequence1.end + sequence2.end + 2 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == multiSequenceProjection.projectValue(12 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 10 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(10 + sequence1.end + sequence2.end + 2 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH)
        assert 12 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(12 + sequence1.end + sequence2.end + 2 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH)

        when: "we set both to reverse projection "
        sequence1.reverse = true
        sequence2.reverse = true
        sequence3.reverse = false

        then: "we should get both reversed"
        assert sequence1.length - 10 == multiSequenceProjection.projectValue(10)
        assert sequence1.length - 12 == multiSequenceProjection.projectValue(12)
        assert multiSequenceProjection.isValid()
        assert 10 == multiSequenceProjection.projectReverseValue(sequence1.length - 10)
        assert 12 == multiSequenceProjection.projectReverseValue(sequence1.length - 12)

        assert (sequence2.length - 10) + sequence1.end + 1 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == multiSequenceProjection.projectValue(10 + sequence1.unprojectedLength)
        assert (sequence2.length - 12) + sequence1.end + 1 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == multiSequenceProjection.projectValue(12 + sequence1.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 10 + sequence1.unprojectedLength + 1 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == multiSequenceProjection.projectReverseValue((sequence2.length - 10) + sequence1.end)
        assert 12 + sequence1.unprojectedLength + 1 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == multiSequenceProjection.projectReverseValue((sequence2.length - 12) + sequence1.end)

        assert 10 + sequence1.end + sequence2.end == multiSequenceProjection.projectValue(10 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert 12 + sequence1.end + sequence2.end == multiSequenceProjection.projectValue(12 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 10 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(10 + sequence1.end + sequence2.end)
        assert 12 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(12 + sequence1.end + sequence2.end)

        when: "we set only the second to the reverse projection "
        sequence1.reverse = false
        sequence2.reverse = true
        sequence3.reverse = true

        then: "we should only get the second reversed"
        assert 10 == multiSequenceProjection.projectValue(10)
        assert 12 == multiSequenceProjection.projectValue(12)
        assert multiSequenceProjection.isValid()
        assert 10 == multiSequenceProjection.projectReverseValue(10)
        assert 12 == multiSequenceProjection.projectReverseValue(12)

        assert (sequence2.length - 10) + sequence1.end == multiSequenceProjection.projectValue(10 + sequence1.unprojectedLength)
        assert (sequence2.length - 12) + sequence1.end == multiSequenceProjection.projectValue(12 + sequence1.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 10 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue((sequence2.length - 10) + sequence1.end)
        assert 12 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue((sequence2.length - 12) + sequence1.end)

        assert (sequence3.length - 10) + sequence1.end + sequence2.end == multiSequenceProjection.projectValue(10 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert (sequence3.length - 12) + sequence1.end + sequence2.end == multiSequenceProjection.projectValue(12 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 10 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue((sequence3.length - 10) + sequence1.end + sequence2.end)
        assert 12 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue((sequence3.length - 12) + sequence1.end + sequence2.end)
    }

    void "one simple forward and reverse projection test for multiple sequences with discontinuous projection"() {

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 20
                , start: 0
                , end: 19
        )// from 0-99
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1])
        Location location1 = new Location(min: 10, max: 12, sequence: sequence1)
//        Location location2 = new Location(min: 14, max: 16, sequence: sequence1)

        when: "we add a single location "
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.calculateOffsets()
        sequence1.reverse = false

        then: "we should get the proper projections"
        assert 0 == multiSequenceProjection.projectValue(10)
        assert 1 == multiSequenceProjection.projectValue(11)
        assert 2 == multiSequenceProjection.projectValue(12)
        assert multiSequenceProjection.isValid()
        assert 10 == multiSequenceProjection.projectReverseValue(0)
        assert 11 == multiSequenceProjection.projectReverseValue(1)
        assert 12 == multiSequenceProjection.projectReverseValue(2)

        when: "we reverse the sequence"
        sequence1.reverse = true

        then: "we should get the proper reversed projection"
        assert 2 == multiSequenceProjection.projectValue(10)
        assert 1 == multiSequenceProjection.projectValue(11)
        assert 0 == multiSequenceProjection.projectValue(12)
        assert multiSequenceProjection.isValid()
        assert 10 == multiSequenceProjection.projectReverseValue(2)
        assert 11 == multiSequenceProjection.projectReverseValue(1)
        assert 12 == multiSequenceProjection.projectReverseValue(0)

    }

    void "two simple forward and reverse projection test for multiple sequences with discontinuous projection"() {

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 20
                , start: 0
                , end: 20
        )
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 2
                , name: "Sequence2"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 10
                , start: 0
                , end: 10
        )
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2])
        Location location1 = new Location(min: 10, max: 12, sequence: sequence1)
        Location location2 = new Location(min: 5, max: 7, sequence: sequence2)

        when: "we add a single location "
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.addLocation(location2)
        multiSequenceProjection.calculateOffsets()
        sequence1.reverse = false
        sequence2.reverse = false

        then: "we should get the proper projections"
        assert 0 == multiSequenceProjection.projectValue(10)
        assert 1 == multiSequenceProjection.projectValue(11)
        assert 2 == multiSequenceProjection.projectValue(12)
        assert 2 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 5)
        assert 3 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 6)
        assert 4 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 7)
        assert multiSequenceProjection.isValid()
        assert 10 == multiSequenceProjection.projectReverseValue(0)
        assert 11 == multiSequenceProjection.projectReverseValue(1)
        assert sequence1.unprojectedLength + 5 == multiSequenceProjection.projectReverseValue(2)
        // shadows one, both are at two
        assert sequence1.unprojectedLength + 6 == multiSequenceProjection.projectReverseValue(3)
        assert sequence1.unprojectedLength + 7 == multiSequenceProjection.projectReverseValue(4)
//        assert sequence1.unprojectedLength + 8 == multiSequenceProjection.projectReverseValue(5)

        when: "we reverse the sequence"
        sequence1.reverse = true
        sequence2.reverse = false

        then: "we should get the proper reversed projection"
        assert 2 == multiSequenceProjection.projectValue(10)
        assert 1 == multiSequenceProjection.projectValue(11)
        assert 0 == multiSequenceProjection.projectValue(12)
        assert 2 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 5)
        assert 3 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 6)
        assert 4 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 7)
        assert multiSequenceProjection.isValid()
        assert 11 == multiSequenceProjection.projectReverseValue(1)
        assert 12 == multiSequenceProjection.projectReverseValue(0)
        assert sequence1.unprojectedLength + 5 == multiSequenceProjection.projectReverseValue(2)
        assert sequence1.unprojectedLength + 6 == multiSequenceProjection.projectReverseValue(3)
        assert sequence1.unprojectedLength + 7 == multiSequenceProjection.projectReverseValue(4)
//        assert sequence1.unprojectedLength + 8 == multiSequenceProjection.projectReverseValue(5)

        when: "we reverse both sequences"
        sequence1.reverse = false
        sequence2.reverse = true

        then: "we should get the proper reversed projection"
        assert 0 == multiSequenceProjection.projectValue(10)
        assert 1 == multiSequenceProjection.projectValue(11)
        assert 2 == multiSequenceProjection.projectValue(12)
        assert 4 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 5)
        assert 3 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 6)
        assert 2 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 7)
        assert multiSequenceProjection.isValid()
        assert 10 == multiSequenceProjection.projectReverseValue(0)
        assert 11 == multiSequenceProjection.projectReverseValue(1)
        assert multiSequenceProjection.projectReverseValue(4) > 0
        assert multiSequenceProjection.projectReverseValue(3) > 0
        assert sequence1.unprojectedLength + 5 == multiSequenceProjection.projectReverseValue(4)
        assert sequence1.unprojectedLength + 6 == multiSequenceProjection.projectReverseValue(3)
        assert sequence1.unprojectedLength + 7 == multiSequenceProjection.projectReverseValue(2)

    }


    void "simple forward and reverse projection for multiple sequences with discontinuous projection"() {

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 100
                , start: 0
                , end: 100
        )// from 0-99
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 1
                , name: "Sequence2"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 50
                , start: 0
                , end: 50
        )// from 0-99
        ProjectionSequence sequence3 = new ProjectionSequence(
                id: 1
                , name: "Sequence3"
                , organism: "Human"
                , order: 2
                , unprojectedLength: 75
                , start: 0
                , end: 75
        )// from 0-99
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2, sequence3])
        Location location1 = new Location(min: 10, max: 12, sequence: sequence1)
        Location location2 = new Location(min: 4, max: 25, sequence: sequence2)
        Location location3 = new Location(min: 40, max: 60, sequence: sequence3)

        when: "we add a location "
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.addLocation(location2)
        multiSequenceProjection.addLocation(location3)
        multiSequenceProjection.calculateOffsets()
        // default setting
        sequence1.reverse = false
        sequence2.reverse = false
        sequence3.reverse = false

        then: "if we retrieve the projection it should be fine"
        // sequence 1
        assert 0 == multiSequenceProjection.projectValue(10)
        assert 2 == multiSequenceProjection.projectValue(12)
        assert multiSequenceProjection.isValid()
        assert 10 == multiSequenceProjection.projectReverseValue(0)
        assert sequence1.unprojectedLength + 4 == multiSequenceProjection.projectReverseValue(2)

        // sequence 2
        assert 0 + 2 + 1 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == multiSequenceProjection.projectValue(4 + sequence1.unprojectedLength)
        assert (15 - 4) + 2 + 1 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == multiSequenceProjection.projectValue(15 + sequence1.unprojectedLength)
        assert multiSequenceProjection.isValid()
        // it is choosing the first projection sequence as this is overlapping
        assert sequence1.unprojectedLength + 4 == multiSequenceProjection.projectReverseValue(2)
        assert sequence1.unprojectedLength + 5 == multiSequenceProjection.projectReverseValue(3)
        assert 15 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue((15 - 4) + 2 + 1 * MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH)

        // sequence 3
        assert (12 - 10) + (25 - 4) == multiSequenceProjection.projectValue(40 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert (60 - 40) + (12 - 10) + (25 - 4) == multiSequenceProjection.projectValue(60 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 1 + 40 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue((12 - 10) + (25 - 4) + 1)
        assert 60 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue((60 - 40) + (12 - 10) + (25 - 4))

        when: "we set it to a reverse projection "
        sequence1.reverse = true
        sequence2.reverse = false
        sequence3.reverse = false

        then: "it should be reversed in the first"
        // sequence 1
        assert 0 == multiSequenceProjection.projectValue(12)
        assert 1 == multiSequenceProjection.projectValue(11)
        assert 2 == multiSequenceProjection.projectValue(10)
        assert multiSequenceProjection.isValid()
        assert 12 == multiSequenceProjection.projectReverseValue(0)
        assert 11 == multiSequenceProjection.projectReverseValue(1)
        // now we are the second sequence region
        assert sequence1.unprojectedLength + 4 == multiSequenceProjection.projectReverseValue(2)

        // sequence 2
        assert 0 + 2 == multiSequenceProjection.projectValue(4 + sequence1.unprojectedLength)
        assert (15 - 4) + 2 == multiSequenceProjection.projectValue(15 + sequence1.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 4 + 1 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(0 + 2 + 1)
        assert 15 + 1 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue((15 - 4) + 2 + 1)

        // sequence 3
        assert (12 - 10) + (25 - 4) == multiSequenceProjection.projectValue(40 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert (60 - 40) + (12 - 10) + (25 - 4) == multiSequenceProjection.projectValue(60 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 1 + 40 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue((12 - 10) + (25 - 4) + 1)
        assert 60 + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue((60 - 40) + (12 - 10) + (25 - 4))

        when: "we set both to reverse projection "
        sequence1.reverse = true
        sequence2.reverse = true
        sequence3.reverse = false

        then: "we should get both reversed"
        assert 2 == multiSequenceProjection.projectValue(10)
        assert 1 == multiSequenceProjection.projectValue(11)
        assert 0 == multiSequenceProjection.projectValue(12)
        assert multiSequenceProjection.isValid()
        assert sequence1.unprojectedLength + 25 == multiSequenceProjection.projectReverseValue(2)
        assert 11 == multiSequenceProjection.projectReverseValue(1)
        assert 12 == multiSequenceProjection.projectReverseValue(0)

        // reverse 10 or 12 in second sequence and add first sequence
        assert ((25 - 4) - (10 - 4) + 2) == multiSequenceProjection.projectValue(10 + sequence1.unprojectedLength)
        assert ((25 - 4) - (12 - 4) + 2) == multiSequenceProjection.projectValue(12 + sequence1.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 10 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(((25 - 4) - (10 - 4) + 2))
        assert 12 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(((25 - 4) - (12 - 4) + 2))

        // in third sequence now
        assert 23 == multiSequenceProjection.projectValue(40 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert 43 == multiSequenceProjection.projectValue(60 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert (40) + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(23)
        assert (41) + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(23 + 1)
        assert (60) + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(43)

        when: "we set only the second to the reverse projection "
        sequence1.reverse = false
        sequence2.reverse = true
        sequence3.reverse = true

        then: "we should only get the second reversed"
        assert 0 == multiSequenceProjection.projectValue(10)
        assert 2 == multiSequenceProjection.projectValue(12)
        assert multiSequenceProjection.isValid()
        assert 10 == multiSequenceProjection.projectReverseValue(0)
        assert 25 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(2)

        assert ((25 - 4) - (10 - 4) + 2) == multiSequenceProjection.projectValue(10 + sequence1.unprojectedLength)
        assert ((25 - 4) - (12 - 4) + 2) == multiSequenceProjection.projectValue(12 + sequence1.unprojectedLength)
        assert multiSequenceProjection.isValid()
        assert 10 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(((25 - 4) - (10 - 4) + 2))
        assert 12 + sequence1.unprojectedLength == multiSequenceProjection.projectReverseValue(((25 - 4) - (12 - 4) + 2))

        assert 43 == multiSequenceProjection.projectValue(40 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert 42 == multiSequenceProjection.projectValue(41 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert 23 == multiSequenceProjection.projectValue(60 + sequence1.unprojectedLength + sequence2.unprojectedLength)
        assert multiSequenceProjection.isValid()
        // projects into the wrong spot
        assert (60) + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(23)
        assert (59) + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(23 + 1)
        assert (40) + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(43)
        assert (41) + sequence1.unprojectedLength + sequence2.unprojectedLength == multiSequenceProjection.projectReverseValue(42)
    }

    void "project reverse projection with two exons / discontinuous regions"() {

        given: "a single projection sequence with two exons as discontinuous projections(A1A2, B1B2)"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 100
                , start: 0
                , end: 100
        )// from 0-99
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()

        when: "it should render normally"
        multiSequenceProjection.addProjectionSequences([sequence1])
        sequence1.reverse = false
        multiSequenceProjection.addLocation(new Location(min: 10, max: 12, sequence: sequence1))
        multiSequenceProjection.addLocation(new Location(min: 15, max: 20, sequence: sequence1))
        multiSequenceProjection.calculateOffsets()

        then: "it should render forward in a familiar manner"
        assert 0 == multiSequenceProjection.projectValue(10)
        assert 2 == multiSequenceProjection.projectValue(12)
        assert multiSequenceProjection.isValid()
        assert 10 == multiSequenceProjection.projectReverseValue(0)
        assert 12 == multiSequenceProjection.projectReverseValue(2)
        assert 3 == multiSequenceProjection.projectValue(15)
        assert 8 == multiSequenceProjection.projectValue(20)
        assert multiSequenceProjection.isValid()
        assert 15 == multiSequenceProjection.projectReverseValue(3)
        assert 20 == multiSequenceProjection.projectReverseValue(8)


        when: "we reverse it"
        sequence1.reverse = true
        println multiSequenceProjection.projectReverseValue(8)
        println multiSequenceProjection.projectReverseValue(7)
        println multiSequenceProjection.projectReverseValue(6)
        println multiSequenceProjection.projectReverseValue(5)
        println multiSequenceProjection.projectReverseValue(4)
        println multiSequenceProjection.projectReverseValue(0)

        then: "we expect the projections coordinates to reverse B2B1A2A1"
        assert 8 == multiSequenceProjection.projectValue(10)
        assert 7 == multiSequenceProjection.projectValue(11)
        assert 6 == multiSequenceProjection.projectValue(12)
        assert multiSequenceProjection.isValid()
        assert 5 == multiSequenceProjection.projectValue(15)
        assert 0 == multiSequenceProjection.projectValue(20)

        assert 10 == multiSequenceProjection.projectReverseValue(8)
        assert 11 == multiSequenceProjection.projectReverseValue(7)
        assert 12 == multiSequenceProjection.projectReverseValue(6)
        assert 15 == multiSequenceProjection.projectReverseValue(5)
        assert 20 == multiSequenceProjection.projectReverseValue(0)


    }

    void "project reverse projection with two exons / discontinuous regions over two projection sequences"() {

        given: "given two projection sequence with two exons each (A1A2B1B2) and (C1C2D1D2)"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 20
                , start: 0
                , end: 20
        )
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 2
                , name: "Sequence2"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 10
                , start: 0
                , end: 10
        )
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2])


        when: "it should render normally"
        multiSequenceProjection.addLocation(new Location(min: 10,max: 12,sequence: sequence1))
        multiSequenceProjection.addLocation(new Location(min: 14,max: 18,sequence: sequence1))
        multiSequenceProjection.addLocation(new Location(min: 2,max: 4,sequence: sequence2))
        multiSequenceProjection.addLocation(new Location(min: 6,max: 9,sequence: sequence2))
        multiSequenceProjection.calculateOffsets()

        then: "it should render forward in a familiar manner"
        assert multiSequenceProjection.isValid()
        assert 0 == multiSequenceProjection.projectValue(10)
        assert 2 == multiSequenceProjection.projectValue(12)
        assert 3 == multiSequenceProjection.projectValue(14)
        assert 7 == multiSequenceProjection.projectValue(18)
        assert 7 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 2)
        assert 9 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 4)
        assert 10 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 6)
        assert 13 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 9)

        assert 10 == multiSequenceProjection.projectReverseValue(0)
        assert 12 == multiSequenceProjection.projectReverseValue(2)
        assert 14 == multiSequenceProjection.projectReverseValue(3)
        assert 17 == multiSequenceProjection.projectReverseValue(6)
//        assert 18 == multiSequenceProjection.projectReverseValue(7) // projected to the second one
        assert sequence1.unprojectedLength + 2 == multiSequenceProjection.projectReverseValue(7)
        assert sequence1.unprojectedLength + 4 == multiSequenceProjection.projectReverseValue(9)
        assert sequence1.unprojectedLength + 6 == multiSequenceProjection.projectReverseValue(10)
        assert sequence1.unprojectedLength + 9 == multiSequenceProjection.projectReverseValue(13)

        when: "we reverse the first one "
        sequence1.reverse = true
        sequence2.reverse = false

        then: "we expect the projections coordinates to reverse B2B1A2A1,C1C2D1D2"
        assert multiSequenceProjection.isValid()
        assert 7 == multiSequenceProjection.projectValue(10)
        assert 5 == multiSequenceProjection.projectValue(12)
        assert 4 == multiSequenceProjection.projectValue(14)
        assert 0 == multiSequenceProjection.projectValue(18)
        assert 7 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 2)
        assert 9 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 4)
        assert 10 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 6)
        assert 13 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 9)

        assert 18 == multiSequenceProjection.projectReverseValue(0)
        assert 16 == multiSequenceProjection.projectReverseValue(2)
        assert 15 == multiSequenceProjection.projectReverseValue(3)
        assert 14 == multiSequenceProjection.projectReverseValue(4)
        assert sequence1.unprojectedLength + 2 == multiSequenceProjection.projectReverseValue(7)
        assert sequence1.unprojectedLength + 4 == multiSequenceProjection.projectReverseValue(9)
        assert sequence1.unprojectedLength + 6 == multiSequenceProjection.projectReverseValue(10)
        assert sequence1.unprojectedLength + 9 == multiSequenceProjection.projectReverseValue(13)

        when: "we reverse the second one and not the first one"
        sequence1.reverse = false
        sequence2.reverse = true

        then: "we expect the projections coordinates to reverse A1A2B1B2,D2D1C2C1"
        assert 0 == multiSequenceProjection.projectValue(10)
        assert 2 == multiSequenceProjection.projectValue(12)
        assert 3 == multiSequenceProjection.projectValue(14)
        assert 7 == multiSequenceProjection.projectValue(18)
        assert 13 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 2)
        assert 11 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 4)
        assert 10 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 6)
        assert 7 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 9)

        assert 10 == multiSequenceProjection.projectReverseValue(0)
        assert 12 == multiSequenceProjection.projectReverseValue(2)
        assert 14 == multiSequenceProjection.projectReverseValue(3)
        assert 17 == multiSequenceProjection.projectReverseValue(6)
//        assert 18 == multiSequenceProjection.projectReverseValue(7) // projected to the second one
        assert sequence1.unprojectedLength + 9 == multiSequenceProjection.projectReverseValue(7)
        assert sequence1.unprojectedLength + 7 == multiSequenceProjection.projectReverseValue(9)
        assert sequence1.unprojectedLength + 6 == multiSequenceProjection.projectReverseValue(10)
        assert sequence1.unprojectedLength + 2 == multiSequenceProjection.projectReverseValue(13)

        when: "we reverse them both "
        sequence1.reverse = true
        sequence2.reverse = true

        then: "we expect the projections coordinates to reverse B2B1A2A1,D2D1C2C1"
        assert 7 == multiSequenceProjection.projectValue(10)
        assert 5 == multiSequenceProjection.projectValue(12)
        assert 4 == multiSequenceProjection.projectValue(14)
        assert 0 == multiSequenceProjection.projectValue(18)
        assert 13 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 2)
        assert 11 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 4)
        assert 10 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 6)
        assert 7 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 9)

        assert 18 == multiSequenceProjection.projectReverseValue(0)
        assert 16 == multiSequenceProjection.projectReverseValue(2)
        assert 15 == multiSequenceProjection.projectReverseValue(3)
        assert 14 == multiSequenceProjection.projectReverseValue(4)

        assert sequence1.unprojectedLength + 9 == multiSequenceProjection.projectReverseValue(7)
        assert sequence1.unprojectedLength + 7 == multiSequenceProjection.projectReverseValue(9)
        assert sequence1.unprojectedLength + 6 == multiSequenceProjection.projectReverseValue(10)
        assert sequence1.unprojectedLength + 2 == multiSequenceProjection.projectReverseValue(13)
    }

    void "single feature projected in limited range (projecting a gene)"() {
        given: "a single projection sequence"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 100
                , start: 30
                , end: 70
        )// from 0-99
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1])
        Location location1 = new Location(min: 30, max: 70, sequence: sequence1)

        when: "we add a location"
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.calculateOffsets()

        then: "we should see only the limited range"
        assert multiSequenceProjection.isValid()
        assert 0==multiSequenceProjection.projectValue(30)
        assert 40==multiSequenceProjection.projectValue(70)

        when: "we reverse the sequence"
        sequence1.reverse = true

        then: "we should see only the reversed range"
        assert multiSequenceProjection.isValid()
        assert 40==multiSequenceProjection.projectValue(30)
        assert 0==multiSequenceProjection.projectValue(70)
    }


    void "multiple feature projected in limited range (projecting two genes)"() {
        given: ""
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 100
                , start: 30
                , end: 70
        )// from 0-99
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 1
                , name: "Sequence2"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 50
                , start: 20
                , end: 30
        )// from 0-99
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2])
        Location location1 = new Location(min: 30, max: 70, sequence: sequence1)
        Location location2 = new Location(min: 20, max: 30, sequence: sequence2)

        when: "we add both locations"
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.addLocation(location2)
        multiSequenceProjection.calculateOffsets()

        then: "we should see everyting in the right order"
        assert 0==multiSequenceProjection.projectValue(30)
        assert 40==multiSequenceProjection.projectValue(70)
        assert 40==multiSequenceProjection.projectValue(20+sequence1.unprojectedLength)
        assert 50==multiSequenceProjection.projectValue(30+sequence1.unprojectedLength)

        when: "we reverse the first one"
        sequence1.reverse = true
        sequence2.reverse = false

        then: "we should see everyting in the right order"
        assert 40==multiSequenceProjection.projectValue(30)
        assert 0==multiSequenceProjection.projectValue(70)
        assert 40==multiSequenceProjection.projectValue(20+sequence1.unprojectedLength)
        assert 50==multiSequenceProjection.projectValue(30+sequence1.unprojectedLength)

        when: "we reverse the second one"
        sequence1.reverse = false
        sequence2.reverse = true

        then: "we should see everyting in the right order"
        assert 0==multiSequenceProjection.projectValue(30)
        assert 40==multiSequenceProjection.projectValue(70)
        assert 50==multiSequenceProjection.projectValue(20+sequence1.unprojectedLength)
        assert 40==multiSequenceProjection.projectValue(30+sequence1.unprojectedLength)
    }

    void "limited range project reverse projection with two exons / discontinuous regions over two projection sequences"() {

        given: "given two projection sequence with two exons each (A1A2B1B2) and (C1C2D1D2)"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 20
                , start: 5
                , end: 20
        )
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 2
                , name: "Sequence2"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 10
                , start: 1
                , end: 10
        )
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2])


        when: "it should render normally"
        multiSequenceProjection.addLocation(new Location(min: 10,max: 12,sequence: sequence1))
        multiSequenceProjection.addLocation(new Location(min: 14,max: 18,sequence: sequence1))
        multiSequenceProjection.addLocation(new Location(min: 2,max: 4,sequence: sequence2))
        multiSequenceProjection.addLocation(new Location(min: 6,max: 9,sequence: sequence2))
        multiSequenceProjection.calculateOffsets()

        then: "it should render forward in a familiar manner"
        assert multiSequenceProjection.isValid()
        assert 0 == multiSequenceProjection.projectValue(10)
        assert 2 == multiSequenceProjection.projectValue(12)
        assert 3 == multiSequenceProjection.projectValue(14)
        assert 7 == multiSequenceProjection.projectValue(18)
        assert 7 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 2)
        assert 9 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 4)
        assert 10 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 6)
        assert 13 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 9)

        assert 10 == multiSequenceProjection.projectReverseValue(0)
        assert 12 == multiSequenceProjection.projectReverseValue(2)
        assert 14 == multiSequenceProjection.projectReverseValue(3)
        assert 17 == multiSequenceProjection.projectReverseValue(6)
//        assert 18 == multiSequenceProjection.projectReverseValue(7) // projected to the second one
        assert sequence1.unprojectedLength + 2 == multiSequenceProjection.projectReverseValue(7)
        assert sequence1.unprojectedLength + 4 == multiSequenceProjection.projectReverseValue(9)
        assert sequence1.unprojectedLength + 6 == multiSequenceProjection.projectReverseValue(10)
        assert sequence1.unprojectedLength + 9 == multiSequenceProjection.projectReverseValue(13)

        when: "we reverse the first one "
        sequence1.reverse = true
        sequence2.reverse = false

        then: "we expect the projections coordinates to reverse B2B1A2A1,C1C2D1D2"
        assert multiSequenceProjection.isValid()
        assert 7 == multiSequenceProjection.projectValue(10)
        assert 5 == multiSequenceProjection.projectValue(12)
        assert 4 == multiSequenceProjection.projectValue(14)
        assert 0 == multiSequenceProjection.projectValue(18)
        assert 7 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 2)
        assert 9 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 4)
        assert 10 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 6)
        assert 13 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 9)

        assert 18 == multiSequenceProjection.projectReverseValue(0)
        assert 16 == multiSequenceProjection.projectReverseValue(2)
        assert 15 == multiSequenceProjection.projectReverseValue(3)
        assert 14 == multiSequenceProjection.projectReverseValue(4)
        assert sequence1.unprojectedLength + 2 == multiSequenceProjection.projectReverseValue(7)
        assert sequence1.unprojectedLength + 4 == multiSequenceProjection.projectReverseValue(9)
        assert sequence1.unprojectedLength + 6 == multiSequenceProjection.projectReverseValue(10)
        assert sequence1.unprojectedLength + 9 == multiSequenceProjection.projectReverseValue(13)

        when: "we reverse the second one and not the first one"
        sequence1.reverse = false
        sequence2.reverse = true

        then: "we expect the projections coordinates to reverse A1A2B1B2,D2D1C2C1"
        assert 0 == multiSequenceProjection.projectValue(10)
        assert 2 == multiSequenceProjection.projectValue(12)
        assert 3 == multiSequenceProjection.projectValue(14)
        assert 7 == multiSequenceProjection.projectValue(18)
        assert 13 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 2)
        assert 11 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 4)
        assert 10 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 6)
        assert 7 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 9)

        assert 10 == multiSequenceProjection.projectReverseValue(0)
        assert 12 == multiSequenceProjection.projectReverseValue(2)
        assert 14 == multiSequenceProjection.projectReverseValue(3)
        assert 17 == multiSequenceProjection.projectReverseValue(6)
//        assert 18 == multiSequenceProjection.projectReverseValue(7) // projected to the second one
        assert sequence1.unprojectedLength + 9 == multiSequenceProjection.projectReverseValue(7)
        assert sequence1.unprojectedLength + 7 == multiSequenceProjection.projectReverseValue(9)
        assert sequence1.unprojectedLength + 6 == multiSequenceProjection.projectReverseValue(10)
        assert sequence1.unprojectedLength + 2 == multiSequenceProjection.projectReverseValue(13)

        when: "we reverse them both "
        sequence1.reverse = true
        sequence2.reverse = true

        then: "we expect the projections coordinates to reverse B2B1A2A1,D2D1C2C1"
        assert 7 == multiSequenceProjection.projectValue(10)
        assert 5 == multiSequenceProjection.projectValue(12)
        assert 4 == multiSequenceProjection.projectValue(14)
        assert 0 == multiSequenceProjection.projectValue(18)
        assert 13 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 2)
        assert 11 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 4)
        assert 10 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 6)
        assert 7 == multiSequenceProjection.projectValue(sequence1.unprojectedLength + 9)

        assert 18 == multiSequenceProjection.projectReverseValue(0)
        assert 16 == multiSequenceProjection.projectReverseValue(2)
        assert 15 == multiSequenceProjection.projectReverseValue(3)
        assert 14 == multiSequenceProjection.projectReverseValue(4)

        assert sequence1.unprojectedLength + 9 == multiSequenceProjection.projectReverseValue(7)
        assert sequence1.unprojectedLength + 7 == multiSequenceProjection.projectReverseValue(9)
        assert sequence1.unprojectedLength + 6 == multiSequenceProjection.projectReverseValue(10)
        assert sequence1.unprojectedLength + 2 == multiSequenceProjection.projectReverseValue(13)
    }

    void "test non-overlap and validity"(){
        given: "two non-overlapping projection sequences for different regions"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 20
                , start: 5
                , end: 10
        )
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 2
                , name: "Sequence1"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 20
                , start:12
                , end: 14
        )
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()

        when: "when we set the same projections"
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2])
        multiSequenceProjection.addLocation(new Location(min: 5,max:10,sequence:sequence1))
        multiSequenceProjection.addLocation(new Location(min: 12,max:14,sequence:sequence2))
        multiSequenceProjection.calculateOffsets()


        then: "it is still valid and we have two projection sequences"
        assert multiSequenceProjection.isValid()
        assert multiSequenceProjection.getProjectedSequences().size()==2
        assert multiSequenceProjection.getProjectedSequences().first().start==5
        assert multiSequenceProjection.getProjectedSequences().first().end==10
        assert multiSequenceProjection.getProjectedSequences().last().start==12
        assert multiSequenceProjection.getProjectedSequences().last().end==14

        when: "we change the reverse of one"
        sequence1.reverse = true

        then: "should not be valid "
        assert !multiSequenceProjection.isValid()

        when: "we change the reverse of one"
        sequence2.reverse = true

        then: "should be valid "
        assert multiSequenceProjection.isValid()
    }


    void "test overlap"(){
        given: "two non-overlapping projection sequences for different regions"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                , name: "Sequence1"
                , organism: "Human"
                , order: 0
                , unprojectedLength: 20
                , start: 5
                , end: 10
        )
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 2
                , name: "Sequence1"
                , organism: "Human"
                , order: 1
                , unprojectedLength: 20
                , start:8
                , end: 15
        )
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection()

        when: "when we set the same projections"
        multiSequenceProjection.addProjectionSequences([sequence1, sequence2])
        multiSequenceProjection.addLocation(new Location(min: 5,max:10,sequence:sequence1))
        multiSequenceProjection.addLocation(new Location(min: 8,max:15,sequence:sequence2))
        multiSequenceProjection.calculateOffsets()


        then: "it is still valid and we have two projection sequences"
        assert multiSequenceProjection.isValid()
        assert multiSequenceProjection.getProjectedSequences().size()==1
        assert multiSequenceProjection.getProjectedSequences().first().start==5
        assert multiSequenceProjection.getProjectedSequences().first().end==15

        when: "we change the reverse of one"
        sequence1.reverse = true

        then: "should be valid "
        assert multiSequenceProjection.isValid()
    }

}
