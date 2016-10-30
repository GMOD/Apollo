package org.bbop.apollo.gwt.shared.projection

import spock.lang.IgnoreRest
import spock.lang.Specification

/**
 * Created by Nathan Dunn on 8/14/15.
 */
class DiscontinuousProjectionSpec extends Specification {

    void "we can remove tree maps"(){
        given: "a treemaps"
        TreeMap<Long,Coordinate> treeMap = new TreeMap<>()
        Coordinate coordinate1 = new Coordinate(45,55)

        when: "we add a var"
        treeMap.put(45l,coordinate1)

        then: "we should have size 1"
        assert treeMap.size()==1

        when: "we remove it"
        treeMap.remove(45l)

        then: "we should have size 0"
        assert treeMap.size()==0
    }

    void "replace coordinates should work properly"(){
        given: "a projection"
        DiscontinuousProjection projection = new DiscontinuousProjection()
        Coordinate coordinate1 = new Coordinate(45,55)
        Coordinate coordinate2 = new Coordinate(40,60)

        when: "we add the first coordinate"
        projection.addCoordinate(45,55)

        then: "we should only have the first coordinate"
        assert projection.size()==1

        when: "we replace that coordinate"
        projection.replaceCoordinate(coordinate1,40l,60l)

        then: "we should ust have the one"
        assert projection.size()==1
    }


    void "try a difference discontinuous projection capable of reverse projection"() {

        given:
        DiscontinuousProjection discontinuousProjection = new DiscontinuousProjection()

        when: "we generate a duplicate projection"
        discontinuousProjection.addInterval(0, 2)
        discontinuousProjection.addInterval(4, 6)
        discontinuousProjection.addInterval(8, 9)

        then: "values should be mapped appropriately"
        assert 0l == discontinuousProjection.projectValue(0)
        assert 1l == discontinuousProjection.projectValue(1)
        assert 2l == discontinuousProjection.projectValue(2)
        assert AbstractProjection.UNMAPPED_VALUE == discontinuousProjection.projectValue(3)
        assert 3l == discontinuousProjection.projectValue(4)
        assert 4l == discontinuousProjection.projectValue(5)
        assert 5l == discontinuousProjection.projectValue(6)
        assert AbstractProjection.UNMAPPED_VALUE == discontinuousProjection.projectValue(7)
        assert 6l == discontinuousProjection.projectValue(8)
        assert 7l == discontinuousProjection.projectValue(9)
        assert AbstractProjection.UNMAPPED_VALUE == discontinuousProjection.projectValue(10)

        // in-phase
        assert new Coordinate(3,4) == discontinuousProjection.projectCoordinate(4, 5)
        // right-edge
        assert null == discontinuousProjection.projectCoordinate(2, 3)
        // left-edge
        assert null == discontinuousProjection.projectCoordinate(3, 4)
        // right-edge overlap
        assert new Coordinate(1,2) == discontinuousProjection.projectCoordinate(1, 3)
        // right-edge overlap
        assert new Coordinate(3, 4) == discontinuousProjection.projectCoordinate(4, 5)
        // left-edge overlap
        assert new Coordinate(3, 4) == discontinuousProjection.projectCoordinate(3, 5)
        // AB overlap
        assert new Coordinate(1, 4) == discontinuousProjection.projectCoordinate(1, 5)
        // AC overlap
        assert new Coordinate(1,  7) == discontinuousProjection.projectCoordinate(1, 9)

        // test reverse values
        assert 0l == discontinuousProjection.projectReverseValue(0)
        assert 1l == discontinuousProjection.projectReverseValue(1)
        assert 2l == discontinuousProjection.projectReverseValue(2)
        assert 4l == discontinuousProjection.projectReverseValue(3)
        assert 5l == discontinuousProjection.projectReverseValue(4)
        assert 6l == discontinuousProjection.projectReverseValue(5)
        assert 8l == discontinuousProjection.projectReverseValue(6)
        assert 9l == discontinuousProjection.projectReverseValue(7)

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
        assert coordinate.min == 45l
        assert coordinate.max == 55l

        when: "we add within that one"
        projection.addInterval(47, 53)
        coordinate = projection.minMap.values().iterator().next()

        then: "nothing happens"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 45l
        assert coordinate.max == 55l

        when: "we add a larger one over it"
        println "A values: ${projection}"
        projection.addInterval(40, 60)
        println "B values: ${projection}"
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge and expand on both sides"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 40l
        assert coordinate.max == 60l


        when: "we add to the continuous right edge"
        projection.addInterval(60, 65)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the right edge"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 40l
        assert coordinate.max == 65l

        when: "we add to the continuous left edge"
        projection.addInterval(35, 40)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the left edge"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 35l
        assert coordinate.max == 65l

        when: "we add to the continuous right overlap"
        projection.addInterval(62, 70)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the right overlap"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 35l
        assert coordinate.max == 70l

        when: "we add to the continuous left overlap"
        projection.addInterval(30, 37)
        coordinate = projection.minMap.values().iterator().next()

        then: "we merge the two on the left overlap"
        projection.minMap.size() == 1
        projection.maxMap.size() == 1
        assert coordinate.min == 30l
        assert coordinate.max == 70l

        when: "we add another one to the left of all of the others"
        projection.addInterval(10, 15)
        Coordinate coordinate0 = projection.minMap.values().getAt(0)
        Coordinate coordinate1 = projection.minMap.values().getAt(1)


        then: "we see another one to the left"
        projection.minMap.size() == 2
        projection.maxMap.size() == 2
        assert coordinate0.min == 10l
        assert coordinate0.max == 15l
        assert coordinate1.min == 30l
        assert coordinate1.max == 70l


        when: "we add another one to the right of all of the others"
        projection.addInterval(80, 85)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        Coordinate coordinate2 = projection.minMap.values().getAt(2)



        then: "we see another one to the right"
        assert projection.minMap.size() == 3
        assert projection.maxMap.size() == 3
        assert coordinate0.min == 10l
        assert coordinate0.max == 15l
        assert coordinate1.min == 30l
        assert coordinate1.max == 70l
        assert coordinate2.min == 80l
        assert coordinate2.max == 85l



        when: "we add another one in the middle of all of the others"
        projection.addInterval(75, 77)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)
        Coordinate coordinate3 = projection.minMap.values().getAt(3)

        then: "we see another one in the middle"
        assert projection.minMap.size() == 4
        assert projection.maxMap.size() == 4
        assert coordinate0.min == 10l
        assert coordinate0.max == 15l
        assert coordinate1.min == 30l
        assert coordinate1.max == 70l
        assert coordinate2.min == 75l
        assert coordinate2.max == 77l
        assert coordinate3.min == 80l
        assert coordinate3.max == 85l


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
        assert coordinate0.min == 10l
        assert coordinate0.max == 15l
        assert coordinate1.min == 20l
        assert coordinate1.max == 25l
        assert coordinate2.min == 30l
        assert coordinate2.max == 70l
        assert coordinate3.min == 75l
        assert coordinate3.max == 77l
        assert coordinate4.min == 80l
        assert coordinate4.max == 85l


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
        assert coordinate0.min == 10l
        assert coordinate0.max == 15l
        assert coordinate1.min == 19l
        assert coordinate1.max == 26l
        assert coordinate2.min == 30l
        assert coordinate2.max == 70l
        assert coordinate3.min == 75l
        assert coordinate3.max == 77l
        assert coordinate4.min == 80l
        assert coordinate4.max == 85l


        when: "we add another to overlap "
        projection.addInterval(22, 76)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)

        then: "we merge overlapping ones"
        assert projection.minMap.size() == 3
        assert projection.maxMap.size() == 3
        assert coordinate0.min == 10l
        assert coordinate0.max == 15l
        assert coordinate1.min == 19l
        assert coordinate1.max == 77l
        assert coordinate2.min == 80l
        assert coordinate2.max == 85l

        when: "we add LHS to center"
        projection.addInterval(18, 22)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)

        then: "should extend center one to the left"
        assert projection.minMap.size() == 3
        assert projection.maxMap.size() == 3
        assert coordinate0.min == 10l
        assert coordinate0.max == 15l
        assert coordinate1.min == 18l
        assert coordinate1.max == 77l
        assert coordinate2.min == 80l
        assert coordinate2.max == 85l

        when: "we add RHS to center"
        projection.addInterval(76, 78)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)

        then: "should extend center one to the left"
        assert projection.minMap.size() == 3
        assert projection.maxMap.size() == 3
        assert coordinate0.min == 10l
        assert coordinate0.max == 15l
        assert coordinate1.min == 18l
        assert coordinate1.max == 78l
        assert coordinate2.min == 80l
        assert coordinate2.max == 85l



        when: "we project in the center of the center"
        projection.addInterval(30, 40)
        coordinate0 = projection.minMap.values().getAt(0)
        coordinate1 = projection.minMap.values().getAt(1)
        coordinate2 = projection.minMap.values().getAt(2)


        then: "nothing should happen"
        assert projection.minMap.size() == 3
        assert projection.maxMap.size() == 3
        assert coordinate0.min == 10l
        assert coordinate0.max == 15l
        assert coordinate1.min == 18l
        assert coordinate1.max == 78l
        assert coordinate2.min == 80l
        assert coordinate2.max == 85l

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
