package org.bbop.apollo

import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class TrackServiceIntegrationSpec extends AbstractIntegrationSpec {

    def trackService

    def setup() {
        setupDefaultUserOrg()
        Organism organism = Organism.first()
        organism.directory = "test/integration/resources/sequences/honeybee-tracks/"
        organism.save(failOnError: true, flush: true)

        new Sequence(
                length: 1405242
                , seqChunkSize: 20000
                , start: 0
                , end: 1405242
                , organism: organism
                , name: "Group11.4"
        ).save(failOnError: true)

        new Sequence(
                length: 1405242
                , seqChunkSize: 20000
                , start: 0
                , end: 1405242
                , organism: organism
                , name: "GroupUn87"
        ).save(failOnError: true)
    }

    def cleanup() {
    }

    /**
     *  GroupUn87: Projected: 0,213 <-> 723,843   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    void "non-projection of contiguous tracks should work"() {

        given: "proper inputs"
        List<String> sequenceStrings = ["GroupUn87", "Group11.4"]
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 10
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 9
        JSONArray firstArray = nclist.getJSONArray(0)
        assert firstArray.getInt(1) == 9966
        assert firstArray.getInt(2) == 10179

        JSONArray lastFirstArray = nclist.getJSONArray(3)
//        assert firstArray.getInt(2)==0
        assert lastFirstArray.getInt(1) == 45455// end of the first set
        assert lastFirstArray.getInt(2) == 45575// end of the first set

        // the next array should go somewhere completely else
        JSONArray firstLastArray = nclist.getJSONArray(4)
        assert firstLastArray.getInt(1) == 10257 + 45575 // end of the first set
        assert firstLastArray.getInt(2) == 18596 + 45575 // end of the first set

        JSONArray lastLastArray = nclist.getJSONArray(8)
        assert lastLastArray.getInt(1) == 62507 + 45575 // end of the last set
        assert lastLastArray.getInt(2) == 64197 + 45575 // end of the last set
    }

    /**
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    void "un-projected 11.4 individually"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.4"]
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 6
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 5

        // the next array should go somewhere completely else
        JSONArray firstLastArray = nclist.getJSONArray(0)
        assert firstLastArray.getInt(1) == 10257// end of the first set
//        assert firstLastArray.getInt(2)==185696+45575// end of the first set
        assert firstLastArray.getInt(2) == 18596// end of the first set

        JSONArray lastLastArray = nclist.getJSONArray(4)
        assert lastLastArray.getInt(1) == 62507// end of the last set
        assert lastLastArray.getInt(2) == 64197// end of the last set
    }

    /**
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    void "Projected 11.4 individually"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.4"]
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 6
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 5

        // the next array should go somewhere completely else
        JSONArray firstLastArray = nclist.getJSONArray(0)
        assert firstLastArray.getInt(1) == 0// end of the first set
//        assert firstLastArray.getInt(2)==185696+45575// end of the first set
        assert firstLastArray.getInt(2) == 2546// end of the first set

        JSONArray lastLastArray = nclist.getJSONArray(4)
        assert lastLastArray.getInt(1) == 14601// end of the last set
        assert lastLastArray.getInt(2) == 15764// end of the last set
    }

    /**
     *
     *  GroupUn87: Projected: 0,213 <-> 723,843   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    void "exon projections of contiguous tracks should work"() {

        given: "proper inputs"
        List<String> sequenceStrings = ["GroupUn87", "Group11.4"]
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 10
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 9
        JSONArray firstArray = nclist.getJSONArray(0)
        assert firstArray.getInt(1) == 0
        assert firstArray.getInt(2) == 213

        JSONArray lastFirstArray = nclist.getJSONArray(3)
        assert lastFirstArray.getInt(1) == 723
        assert lastFirstArray.getInt(2) == 843  // end of the first set

        // the next array should start at the end of thast one
        JSONArray firstLastArray = nclist.getJSONArray(4)
        assert firstLastArray.getInt(1) == 0 + 843
        assert firstLastArray.getInt(2) == 2546 + 843

        JSONArray lastLastArray = nclist.getJSONArray(8)
        assert lastLastArray.getInt(1) == 14601 + 843
        assert lastLastArray.getInt(2) == 15764 + 843
    }

    /**
     *
     *  Group11.6: Unprojected (2 chunks) first chunk, 6958 <=> 1080855  second chunk, 1083799 <=> 1494475 Unprojected: 9966,10179 (first)  45455,45575 (last)
     *  (lf-1 . . 61 pieces, 6958 <=> 8455 first, 1078032 <=> 1080855 last ) ,
     *  (lf-2 . . 43 pieces, 1083799 <=>  1102753 first, 1494115 <=> 1494475 last ) ,
     *
     *  Projected: (2 chunks) first chunk, 0 <=> 91398 second chunk, 91399 <=> 169359
     *  (lf-1 . . 61 pieces, 0 <=> 1281 first, 89936 <=> 91398 last ) ,
     *  (lf-2 . . 43 pieces, 91399 <=>  95943 first, 169097 <=> 169359 last ) ,
     *
     * TODO: // look at this
     *  Group1.10: Unprojected (3 chunks)
     *  first chunk: 19636 <=>  588668  second chunk: 588729 <=> 1267170  third chunk: 1268021 ,1405215 (second)
     *  (lf-1 . . 57 pieces, 19636 <=> 31167 first, 582938 <=> 588668 last ) ,
     *  (lf-2 . . 61 pieces, 588729 <=>  594164 first, 1261785 <=> 1267170 last ) ,
     *  (lf-3 . . 16 pieces, 1268021 <=>  1277382 first, 1389396 <=> 1405215 last ) ,
     *
     *  Group1.10: Projected (3 chunks)
     *  first chunk: 0 <=> 108503 second chunk: 108504 <=> 201343 third chunk: 201344 ,230587
     *  (lf-1 . . 57 pieces, 0 <=> 874 first, 107145 <=> 108503 last ) ,
     *  (lf-2 . . 61 pieces, 108504 <=> 109549  first, 195958 <=> 201343 last ) ,
     *  (lf-3 . . 16 pieces, 201344 <=>  206511 first, 227803 <=> 230587 last ) ,
     */
    void "chunking / chunking projection"() {

        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.6", "Group1.10"]
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceStrings[0]}\"},{\"name\":\"${sequenceStrings[1]}\"}], \"label\":\"${sequenceStrings.join('::')}\"}:-1..-1/trackData.json"
        String chunk1 = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${sequenceStrings[0]}/lf-1.json"
        String chunk2 = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${sequenceStrings[0]}/lf-2.json"
        String chunk3 = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${sequenceStrings[1]}/lf-1.json"
        // don't need to test chunk5 as well
        String chunk5 = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${sequenceStrings[1]}/lf-3.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceStrings[0]}\"},{\"name\":\"${sequenceStrings[1]}\"}], \"label\":\"${sequenceStrings.join('::')}\"}:-1..-1:1..16607"
        JSONArray array

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 5

        when:
        array = nclist.getJSONArray(0)

        then:
        assert array.getInt(0) == 4 // it is a chunk
        assert array.getInt(1) == 0
        assert array.getInt(2) == 91398

        when:
        array = nclist.getJSONArray(1)

        then:
        assert array.getInt(0) == 4
        assert array.getInt(1) == 91399
        assert array.getInt(2) == 169359 // end of the first set

        when:
        array = nclist.getJSONArray(2)

        then:
        assert array.getInt(0) == 4
        assert array.getInt(1) == 0 + 169359
        assert array.getInt(2) == 108503 + 169359

        when:
        array = nclist.getJSONArray(3)

        then:
        assert array.getInt(0) == 4
        assert array.getInt(1) == 108504 + 169359
        assert array.getInt(2) == 201343 + 169359

        when:
        array = nclist.getJSONArray(4)

        then:
        assert array.getInt(0) == 4
        assert array.getInt(1) == 201344 + 169359
        assert array.getInt(2) == 230587 + 169359

        when: "we load the first of the chunk data"
        JSONArray chunk1Data = trackService.loadChunkData(chunk1, refererLoc, Organism.first(), 0)
        array = chunk1Data.getJSONArray(0)

        then: "confirm that chunk 1 is projected "
        assert chunk1Data.size() == 61
        assert array.getInt(1) == 0
        assert array.getInt(2) == 1281

        when:
        array = chunk1Data.getJSONArray(60)

        then:
        assert array.getInt(1) == 89936
        assert array.getInt(2) == 91398


        when: "we load the second chunk"
        JSONArray chunk2Data = trackService.loadChunkData(chunk2, refererLoc, Organism.first(), 0)
        array = chunk2Data.getJSONArray(0)

        then: "we should get the properly projected chunks for 2"
        assert array.getInt(1) == 91399
        assert array.getInt(2) == 95943

        when:
        array = chunk2Data.getJSONArray(42)

        then:
        assert array.getInt(1) == 169097
        assert array.getInt(2) == 169359

        when: "we load the third chunk"
        JSONArray chunk3Data = trackService.loadChunkData(chunk3, refererLoc, Organism.first(), 169359)
        array = chunk3Data.getJSONArray(0)

        then: "confirm that chunk 3 is projected "
        assert chunk3Data.size() == 57
        assert array.getInt(1) == 0 + 169359
        assert array.getInt(2) == 874 + 169359

        when:
        array = chunk3Data.getJSONArray(56)

        then:
        assert array.getInt(1) == 107145 + 169359
        assert array.getInt(2) == 108503 + 169359

        when: "we load the third chunk"
//        *  (lf-3 . . 16 pieces, 201344 <=>  206511 first, 227803 <=> 230587 last ) ,
        JSONArray chunk5Data = trackService.loadChunkData(chunk5, refererLoc, Organism.first(), 201343)
        array = chunk5Data.getJSONArray(0)

        then: "confirm that chunk 3 is projected "
        assert chunk5Data.size() == 16
        assert array.getInt(1) == 201344 + 169359
        assert array.getInt(2) == 206511 + 169359

        when:
        array = chunk5Data.getJSONArray(15)

        then:
        assert array.getInt(1) == 227803 + 169359
        assert array.getInt(2) == 230587 + 169359
    }
}
