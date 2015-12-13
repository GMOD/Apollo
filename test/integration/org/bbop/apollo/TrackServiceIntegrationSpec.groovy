package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class TrackServiceIntegrationSpec extends AbstractIntegrationSpec {

    def trackService
    def projectionService

    def setup() {
        setupDefaultUserOrg()
        Organism organism = Organism.first()
        organism.directory = "test/integration/resources/sequences/honeybee-tracks/"
        organism.save(failOnError: true, flush: true)

        new Sequence(
                length: 75085
                , seqChunkSize: 20000
                , start: 0
                , end: 75085
                , organism: organism
                , name: "Group11.4"
        ).save(failOnError: true)

        new Sequence(
                length: 78258
                , seqChunkSize: 20000
                , start: 0
                , end: 78258
                , organism: organism
                , name: "GroupUn87"
        ).save(failOnError: true)
        projectionService.clearProjections()
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
        assert firstArray.getInt(3) == 1

        JSONArray lastFirstArray = nclist.getJSONArray(3)
//        assert firstArray.getInt(2)==0
        assert lastFirstArray.getInt(1) == 45455// end of the first set
        assert lastFirstArray.getInt(2) == 45575// end of the first set
        assert lastFirstArray.getInt(3) == 1 // end of the first set

        // the next array should go somewhere completely else
        JSONArray firstLastArray = nclist.getJSONArray(4)
        assert firstLastArray.getInt(1) == 10257 + 45575 // start of the last set
        assert firstLastArray.getInt(2) == 18596 + 45575 // start of the last set
        assert firstLastArray.getInt(3) == 1

        JSONArray lastLastArray = nclist.getJSONArray(8)
        assert lastLastArray.getInt(1) == 62507 + 45575 // end of the last set
        assert lastLastArray.getInt(2) == 64197 + 45575 // end of the last set
        assert lastLastArray.getInt(3) == -1
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
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    void "Projected 11.4 individually with padding"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.4"]
        Integer padding = 20
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\": ${padding}, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":${padding}, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1:1..16607"

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
        assert firstLastArray.getInt(1) == 0 + padding // end of the first set
        assert firstLastArray.getInt(2) == 2546 + (padding + (padding * (8 * 2))) // end of the first set

        JSONArray lastLastArray = nclist.getJSONArray(4)
        Integer paddingCount = 55
        assert lastLastArray.getInt(1) == 14601 + (padding * paddingCount)// end of the last set
        assert lastLastArray.getInt(2) == 15764 + (padding * (paddingCount + 6))// end of the last set . ..  including exons
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
     *  GroupUn87: Projected: 0,213 <-> 723,843   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    void "exon projections of contiguous tracks should work with padding"() {

        given: "proper inputs"
        List<String> sequenceStrings = ["GroupUn87", "Group11.4"]
        Integer padding = 20
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":${padding}, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":${padding}, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 10
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 9
        JSONArray firstArray = nclist.getJSONArray(0)
        Integer paddingCount = 1
        assert firstArray.getInt(1) == 0 + padding * paddingCount
        assert firstArray.getInt(2) == 213 + padding * paddingCount

        when: "adjust "
        paddingCount += 10

        then:
        JSONArray lastFirstArray = nclist.getJSONArray(3)
        assert lastFirstArray.getInt(1) == 723 + padding * paddingCount // end of first set
        assert lastFirstArray.getInt(2) == 843 + padding * paddingCount // end of the first set

        // the next array should start at the end of thast one
        when: "adjust "
        paddingCount += 1

        then:
        JSONArray firstLastArray = nclist.getJSONArray(4)
        assert firstLastArray.getInt(1) == 0 + 843 + padding * paddingCount
        assert firstLastArray.getInt(2) == 2546 + 843 + padding * (paddingCount+16)

        when: "adjust "
        paddingCount += 16 + 2 + 36 //?

        then:
        JSONArray lastLastArray = nclist.getJSONArray(8)
        assert lastLastArray.getInt(1) == 14601 + 843 + padding * paddingCount
        assert lastLastArray.getInt(2) == 15764 + 843 + padding * (paddingCount+6)
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
        String trackName = "Official Gene Set v3.2"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceStrings[0]}\"},{\"name\":\"${sequenceStrings[1]}\"}], \"label\":\"${sequenceStrings.join('::')}\"}:-1..-1/trackData.json"
        String chunk1 = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${sequenceStrings[0]}/lf-1.json"
        String chunk2 = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${sequenceStrings[0]}/lf-2.json"
        String chunk3 = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${sequenceStrings[1]}/lf-1.json"
        // don't need to test chunk5 as well
        String chunk5 = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${sequenceStrings[1]}/lf-3.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"${trackName}\", \"sequenceList\":[{\"name\":\"${sequenceStrings[0]}\"},{\"name\":\"${sequenceStrings[1]}\"}], \"label\":\"${sequenceStrings.join('::')}\"}:-1..-1:1..16607"
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
        JSONArray chunk1Data = trackService.loadChunkData(chunk1, refererLoc, Organism.first(), 0,trackName)
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
        JSONArray chunk2Data = trackService.loadChunkData(chunk2, refererLoc, Organism.first(), 0,trackName)
        array = chunk2Data.getJSONArray(0)

        then: "we should get the properly projected chunks for 2"
        assert array.getInt(1) == 91399
        assert array.getInt(2) == 95943

        when:
        array = chunk2Data.getJSONArray(chunk2Data.size() - 1)

        then:
        assert array.getInt(1) == 169097
        assert array.getInt(2) == 169359

        when: "we load the third chunk using the offset from previous sequence group"
        JSONArray chunk3Data = trackService.loadChunkData(chunk3, refererLoc, Organism.first(), 169359,trackName)
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

        when: "we load the last chunk using the offset from previous sequence group"
//        *  (lf-3 . . 16 pieces, 201344 <=>  206511 first, 227803 <=> 230587 last ) ,
        JSONArray chunk5Data = trackService.loadChunkData(chunk5, refererLoc, Organism.first(), 169359,trackName)
        array = chunk5Data.getJSONArray(0)

        then: "confirm that chunk 5 is projected "
        assert chunk5Data.size() == 15
        assert array.getInt(1) == 201344 + 169359
        assert array.getInt(2) == 206511 + 169359

        when:
        array = chunk5Data.getJSONArray(chunk5Data.size() - 1)

        then:
        assert array.getInt(1) == 227803 + 169359
        assert array.getInt(2) == 230587 + 169359
    }

    void "test sanitizeCoordinateArray method"() {

        given: "a user, organism, and group"
        User user = User.first()
        Organism organism = Organism.first()
        UserGroup group = UserGroup.first()
        String trackName = "Official Gene Set v3.2"

        // top-level feature has -1 coordinates
        String payloadOneString = "[[0,-1,-1,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,509862,511494,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"
        String sanitizedPayloadOneString = "[]"

        // 2 sub-features have -1 coordinates
        String payloadTwoString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,-1,-1,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,-1,-1,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,509862,511494,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"
        String sanitizedPayloadTwoString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,509862,511494,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"

        // top-level feature in subListColumn has -1 coordinates
        String payloadThreeString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,-1,-1,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"
        String sanitizedPayloadThreeString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[]}]]"

        // 2 sub-features in subListColumn has -1 coordinates
        String payloadFourString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,509862,511494,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,-1,-1,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,-1,-1,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"
        String sanitizedPayloadFourString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,509862,511494,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"

        when: "we try to sanitize a coordinate JSON array that has a top-level feature with invalid coordinates"
        JSONArray payloadOneArray = JSON.parse(payloadOneString) as JSONArray
        JSONArray payloadOneReturnArray = trackService.sanitizeCoordinateArray(payloadOneArray, organism, trackName)

        then: "we should see an empty coordinate JSON array"
        println "PAYLOAD ONE RETURN ARRAY: ${payloadOneReturnArray.toString()}"
        assert payloadOneReturnArray.size() == 0
        assert payloadOneArray.size() == payloadOneReturnArray.size()

        when: "we try to sanitize a coordinate JSON array that has 2 sub-features with invalid coordinates"
        JSONArray payloadTwoArray = JSON.parse(payloadTwoString) as JSONArray
        JSONArray payloadTwoReturnArray = trackService.sanitizeCoordinateArray(payloadTwoArray, organism, trackName)

        then: "we should see a valid coordinate JSON array without those 2 sub-features"
        println "PAYLOAD TWO RETURN ARRAY: ${payloadTwoReturnArray.toString()}"
        assert payloadTwoReturnArray.toString() == sanitizedPayloadTwoString
        assert payloadTwoArray.size() == payloadTwoReturnArray.size()

        when: "we try to sanitize a coordinate JSON array that has a subList whose top-level feature has invalid coordinates"
        JSONArray payloadThreeArray = JSON.parse(payloadThreeString) as JSONArray
        JSONArray payloadThreeReturnArray = trackService.sanitizeCoordinateArray(payloadThreeArray, organism, trackName)

        then:" we should see a valid coordinate JSON array with an empty subList"
        println "PAYLOAD THREE RETURN ARRAY: ${payloadThreeReturnArray.toString()}"
        assert payloadThreeReturnArray.toString() == sanitizedPayloadThreeString
        assert payloadThreeArray.size() == payloadThreeReturnArray.size()

        when: "we try to sanitize a coordinate JSON array that has a subList whose sub-features have invalid coordinates"
        JSONArray payloadFourArray = JSON.parse(payloadFourString) as JSONArray
        JSONArray payloadFourReturnArray = trackService.sanitizeCoordinateArray(payloadFourArray, organism, trackName)

        then: "we should see a valid coordinate JSON array that has a subList without those sub-features"
        println "PAYLOAD FOUR RETURN ARRAY: ${payloadFourReturnArray.toString()}"
        assert payloadFourReturnArray.toString() == sanitizedPayloadFourString
        assert payloadFourArray.size() == payloadFourReturnArray.size()
    }

}
