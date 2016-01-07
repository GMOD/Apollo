package org.bbop.apollo

import org.bbop.apollo.projection.*
import spock.lang.Specification

/**
 * Created by Nathan Dunn on 8/14/15.
 */
class MultiSequenceProjectionSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "when adding intervals overlapping intervals should merge"() {

        given: "some intervals"
        ProjectionDescription projectionDescription = new ProjectionDescription()
        ProjectionSequence projectionSequence = new ProjectionSequence(order: 0)
        projectionDescription.sequenceList = [projectionSequence]
        ProjectionInterface projection = new MultiSequenceProjection(projectionDescription: projectionDescription)


        when: "we add an interval to a null one"
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
                ,name: "Sequence1"
                ,organism: "Human"
        )// from 0-99
        ProjectionDescription projectionDescription = new ProjectionDescription(
                referenceTrack: []
                ,sequenceList: [sequence1]
                , projection: "exon" // probably ignored here
                ,padding: 0
        )
        MultiSequenceProjection projection = new MultiSequenceProjection(projectionDescription: projectionDescription)

        when: "we add the overlapping coordinates"
        projection.addInterval(694694, 694915,sequence1)
        projection.addInterval(694959, 695222,sequence1)

        then: "we should have 1"
        assert projection.size() == 2

        when: "we add some more overlapping ones"
        projection.addInterval(695185, 695546,sequence1)
        projection.addInterval(695511, 695782,sequence1)
        projection.addInterval(695745, 696068,sequence1)
        projection.addInterval(696071, 696395,sequence1)
        projection.addInterval(696559, 697320,sequence1)

        then: "there should be 4 projections"
        assert projection.size() == 4

        when: "we add one more"
        projection.addInterval(697283, 697566,sequence1)


        then: "4 again"
        assert projection.size() == 4

        when: "we add the last one"
        projection.addInterval(696108, 696395,sequence1)

        then: "there should just be the 4"
        assert projection.size() == 4

        when: "we add one of the LHS"
        projection.addInterval(696071, 696390,sequence1)


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
                ,name: "Sequence1"
                ,organism: "Human"
        )// from 0-99
        ProjectionDescription projectionDescription = new ProjectionDescription(
                referenceTrack: []
                ,sequenceList: [sequence1]
                , projection: "exon" // probably ignored here
                ,padding: 0
        )
        MultiSequenceProjection projection = new MultiSequenceProjection(projectionDescription: projectionDescription)
        int index = 0

        when: "we add the overlapping coordinates"
        projection.addInterval(426970, 427288,sequence1)
        projection.addInterval(427273, 427960,sequence1)

        then: "we should have 1"
        assert projection.size() == 1

        when: "when we add non-verlapping coordinates"
        projection.addInterval(427987, 428349,sequence1) // 2
        projection.addInterval(428394, 428830,sequence1) // 3
        projection.addInterval(428905, 429123,sequence1) // 4
        projection.addInterval(429080, 429230,sequence1) // 4
        projection.addInterval(429198, 429434,sequence1) // 4
        projection.addInterval(429406, 429609,sequence1) // 4
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
        projection.addInterval(428187, 428534,sequence1)
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
        projection.addInterval(428528, 428829,sequence1)
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
        projection.addInterval(428905, 429115,sequence1)
        projection.addInterval(429073, 429230,sequence1)
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
        projection.addInterval(429198, 429439,sequence1)
        projection.addInterval(429410, 429605,sequence1)

        then: "we should still have 3"
        assert projection.size() == 3

        when: "we add this last one"
        projection.addInterval(429597, 430007,sequence1)

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
                ,name: "Sequence1"
                ,organism: "Human"
        )// from 0-99
        ProjectionDescription projectionDescription = new ProjectionDescription(
                referenceTrack: []
                ,sequenceList: [sequence1]
                , projection: "exon" // probably ignored here
                ,padding: 0
        )
        MultiSequenceProjection projection = new MultiSequenceProjection(projectionDescription: projectionDescription)
        int index = 0

        when: "we add some normal intervals"
        projection.addInterval(285235, 285658,sequence1)
        projection.addInterval(285628, 285895,sequence1)
        projection.addInterval(285887, 286954,sequence1)
        projection.addInterval(286965, 287209,sequence1)
        projection.addInterval(287225, 287371,sequence1)
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
        projection.addInterval(285192, 286954,sequence1)
        projection.addInterval(286965, 287209,sequence1)
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
        projection.addInterval(287225, 288061,sequence1)

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
                ,name: "Sequence1"
                ,organism: "Human"
        )// from 0-99
        ProjectionDescription projectionDescription = new ProjectionDescription(
                referenceTrack: []
                ,sequenceList: [sequence1]
                , projection: "exon" // probably ignored here
                ,padding: 0
        )
        MultiSequenceProjection projection = new MultiSequenceProjection(projectionDescription: projectionDescription)
        int index = 0

        when: "we add some projections"
        projection.addInterval(1764232, 1764464,sequence1)  // 0
        projection.addInterval(1764440, 1764723,sequence1)  // 0
        projection.addInterval(1764736, 1764943,sequence1)  // 1
        projection.addInterval(1764907, 1765195,sequence1)  // 1
        projection.addInterval(1765229, 1765487,sequence1)  // 2
        projection.addInterval(1765511, 1765761,sequence1)  // 3
        projection.addInterval(1765764, 1766416,sequence1)  // 4
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
        projection.addInterval(1764703, 1765195,sequence1)
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
        projection.addInterval(1765229, 1766403,sequence1)
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
                ,name: "Sequence1"
                ,organism: "Human"
        )// from 0-99
        ProjectionDescription projectionDescription = new ProjectionDescription(
                referenceTrack: []
                ,sequenceList: [sequence1]
                , projection: "exon" // probably ignored here
                ,padding: 0
        )
        MultiSequenceProjection projection = new MultiSequenceProjection(projectionDescription: projectionDescription)
        int index = 0

        when: "we add some intervals"
        projection.addInterval(322874, 323189,sequence1) // 0
        projection.addInterval(323171, 323490,sequence1) // 0
        projection.addInterval(323458, 323739,sequence1) // 0
        projection.addInterval(323719, 323996,sequence1) // 0
        projection.addInterval(323984, 324541,sequence1) // 0
        projection.addInterval(324636, 325100,sequence1) // 1
        projection.addInterval(325109, 325906,sequence1) // 2
        projection.addInterval(325883, 329527,sequence1) // 2
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
        projection.addInterval(323453, 329527,sequence1)
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
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                ,name: "Sequence1"
                ,organism: "Human"
        )// from 0-99
        ProjectionDescription projectionDescription = new ProjectionDescription(
                referenceTrack: []
                ,sequenceList: [sequence1]
                , projection: "exon" // probably ignored here
                ,padding: 0
        )
        MultiSequenceProjection projection = new MultiSequenceProjection(projectionDescription: projectionDescription)
        int index = 0

        when: "we add a series of intervals"
        projection.addInterval(411456,411745,sequence1) // 0
        projection.addInterval(411775,411934,sequence1) // 1
        projection.addInterval(412094,412542,sequence1) // 2
        projection.addInterval(412570,412901,sequence1) // 3
        projection.addInterval(412977,414637,sequence1) // 4
        projection.addInterval(412312,412542,sequence1) // 2
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
        projection.addInterval(412570,413980,sequence1) // 1
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
    void "explicitly test multiple scaffolds"(){

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
               id: 1
                ,name: "Sequence1"
                ,organism: "Human"
                ,order: 0
                ,unprojectedLength: 100
        )// from 0-99
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 2
                ,name: "Sequence2"
                ,organism: "Human"
                ,order: 1
                ,unprojectedLength: 100
        ) // from 100-200
        ProjectionDescription projectionDescription = new ProjectionDescription(
                referenceTrack: []
                ,sequenceList: [sequence1,sequence2]
                , projection: "exon" // probably ignored here
                ,padding: 0
        )
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection(projectionDescription: projectionDescription)
        Location location1 = new Location( min: 10 ,max: 12 ,sequence: sequence1 )
        Location location2 = new Location( min: 22 ,max: 25 ,sequence: sequence1 )
        Location location3 = new Location( min: 23,max: 27,sequence: sequence2 )
        Location location4 = new Location( min: 60,max: 63,sequence: sequence2 )



        when: "we create some intervals for a few scaffolds"
        multiSequenceProjection.addLocation(location1)
        multiSequenceProjection.addLocation(location2)
        multiSequenceProjection.addLocation(location3)
        multiSequenceProjection.addLocation(location4)
        multiSequenceProjection.calculateOffsets()
        List<Coordinate> coordinateCollection = multiSequenceProjection.listCoordinates()
        List<ProjectionSequence> projectionSequenceList = multiSequenceProjection.sequenceDiscontinuousProjectionMap.keySet() as List<ProjectionSequence>

        then: "we should get a single projection of size 4"
        assert multiSequenceProjection.size()==4
        coordinateCollection.get(0).min==10
        coordinateCollection.get(0).max==12
        coordinateCollection.get(1).min==22
        coordinateCollection.get(1).max==25
        coordinateCollection.get(2).min==23
        coordinateCollection.get(2).max==27
        coordinateCollection.get(3).min==60
        coordinateCollection.get(3).max==63
        assert 0==projectionSequenceList.get(0).offset
        assert 6==multiSequenceProjection.sequenceDiscontinuousProjectionMap.get(projectionSequenceList.get(0)).bufferedLength
        assert 7==projectionSequenceList.get(1).offset
        assert 8==multiSequenceProjection.sequenceDiscontinuousProjectionMap.get(projectionSequenceList.get(1)).bufferedLength
        assert "Sequence1"==multiSequenceProjection.getProjectionSequence(10).name
        assert "Sequence2"==multiSequenceProjection.getProjectionSequence(60+25).name
        assert 7==multiSequenceProjection.getProjectionSequence(60+25).offset

        assert 0==multiSequenceProjection.projectValue(10)
        assert 2==multiSequenceProjection.projectValue(12)
        assert 3==multiSequenceProjection.projectValue(22)
        assert 6==multiSequenceProjection.projectValue(25)
        assert 7==multiSequenceProjection.projectValue(25+23)
        assert 11==multiSequenceProjection.projectValue(25+27)
        assert 12==multiSequenceProjection.projectValue(25+60)
        assert 15==multiSequenceProjection.projectValue(25+63)


        assert 10==multiSequenceProjection.projectReverseValue(0)
        assert 12==multiSequenceProjection.projectReverseValue(2)
        assert 22==multiSequenceProjection.projectReverseValue(3)
        assert 25==multiSequenceProjection.projectReverseValue(6)
//        assert 25+23==multiSequenceProjection.projectReverseValue(7)
//        assert 25+27==multiSequenceProjection.projectReverseValue(11)
//        assert 25+60==multiSequenceProjection.projectReverseValue(12)
//        assert 25+63==multiSequenceProjection.projectReverseValue(15)
        assert 23==multiSequenceProjection.projectReverseValue(7)
        assert 27==multiSequenceProjection.projectReverseValue(11)
        assert 60==multiSequenceProjection.projectReverseValue(12)
        assert 63==multiSequenceProjection.projectReverseValue(15)

        when: "we project a sequence through these coordinates"
        // length should be 200
        String inputSequence = "ATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCA"
        String projectedSequence = multiSequenceProjection.projectSequence(inputSequence,0,200,0)
        Integer offset = multiSequenceProjection.projectedSequences.first().unprojectedLength

        then: "we should confirm that both the input and retrieved sequence are correct"
        assert 200==inputSequence.length()
        assert 100==offset
        assert inputSequence.substring(10,12)==projectedSequence.substring(0,2)
        assert inputSequence.substring(22,25)==projectedSequence.substring(3,6)
        assert inputSequence.substring(23+offset,27+offset)==projectedSequence.substring(7,11)
        assert inputSequence.substring(60+offset,63+offset)==projectedSequence.substring(12,15)
        assert 16==projectedSequence.length()

        when: "we project a sequence through these smaller coordinates"
        // length should be 200
        projectedSequence = multiSequenceProjection.projectSequence(inputSequence,50,150,0)
//        Integer offset = multiSequenceProjection.projectedSequences.first().unprojectedLength

        then: "we should confirm that both the input and retrieved sequence are correct"
//        assert 200==inputSequence.length()
        assert 100==offset
//        assert inputSequence.substring(10,12)==projectedSequence.substring(0,2)
//        assert inputSequence.substring(22,25)==projectedSequence.substring(3,6)
        assert 5==projectedSequence.length()
        assert inputSequence.substring(23+offset,27+offset)==projectedSequence.substring(0,4)
    }

    void "more multi-scaffold tests"(){

        given: "a projection"
        ProjectionSequence sequence1 = new ProjectionSequence(
                id: 1
                ,name: "Sequence1"
                ,organism: "Human"
                ,order: 0
                ,unprojectedLength: 50
        )// from 0-49
        ProjectionSequence sequence2 = new ProjectionSequence(
                id: 2
                ,name: "Sequence2"
                ,organism: "Human"
                ,order: 1
                ,unprojectedLength: 75
        ) // from 50-124
        ProjectionSequence sequence3 = new ProjectionSequence(
                id: 3
                ,name: "Sequence3"
                ,organism: "Human"
                ,order: 2
                ,unprojectedLength: 25
        ) // from 125-149
        ProjectionSequence sequence4 = new ProjectionSequence(
                id: 4
                ,name: "Sequence4"
                ,organism: "Human"
                ,order: 3
                ,unprojectedLength: 50
        ) // from 150-200
        ProjectionDescription projectionDescription = new ProjectionDescription(
                referenceTrack: []
                ,sequenceList: [sequence1,sequence2,sequence3,sequence4]
                , projection: "exon" // probably ignored here
                ,padding: 0
        )
        MultiSequenceProjection multiSequenceProjection = new MultiSequenceProjection(projectionDescription: projectionDescription)
        Location location1 = new Location( min: 10 ,max: 12 ,sequence: sequence1 ) // 3
        Location location2 = new Location( min: 22 ,max: 25 ,sequence: sequence1 ) // 4
        Location location3 = new Location( min: 23,max: 27,sequence: sequence2 )  // 5
        Location location4 = new Location( min: 60,max: 63,sequence: sequence2 )  // 4
        Location location5 = new Location( min: 5,max: 10,sequence: sequence3 )   // 6
        Location location6 = new Location( min: 10,max: 12,sequence: sequence4 )  // 3
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
        assert multiSequenceProjection.size()==6

        when: "we project a sequence through these coordinates"
        // length should be 200
        String inputSequence = "ATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCAATGCA"
        String projectedSequence = multiSequenceProjection.projectSequence(inputSequence,0,200,0)
        Integer offset = multiSequenceProjection.projectedSequences.first().unprojectedLength

        then: "we should confirm that both the input and retrieved sequence are correct"
        assert 200==inputSequence.length()
        assert 50==offset
        assert 25==projectedSequence.length()
        assert inputSequence.substring(10,12)==projectedSequence.substring(0,2)
        assert inputSequence.substring(22,25)==projectedSequence.substring(3,6)
        assert inputSequence.substring(23+offset,27+offset)==projectedSequence.substring(7,11)
        assert inputSequence.substring(60+offset,63+offset)==projectedSequence.substring(12,15)

        when: "case 1 and 2: we project a sequence through these smaller coordinates "
        // length should be 200
        projectedSequence = multiSequenceProjection.projectSequence(inputSequence,50,150,0)
//        Integer offset = multiSequenceProjection.projectedSequences.first().unprojectedLength

        then: "we should confirm that both the input and retrieved sequence are correct"
        assert 15==projectedSequence.length()

        when: "we attempt case 3: a subset of a projection sequence"
        projectedSequence = multiSequenceProjection.projectSequence(inputSequence,60,120,0)

        then: "we should see only see all of the coordinates on sequence 3"
        assert 9==projectedSequence.length()

        when: "we attempt case 4 (and also 1 and 2): we overlap the entire projection sequence space"
        projectedSequence = multiSequenceProjection.projectSequence(inputSequence,20,8+125,0)

        then: "we will just project tne entire thing"
        assert 4+9+4==projectedSequence.length()

    }

}
