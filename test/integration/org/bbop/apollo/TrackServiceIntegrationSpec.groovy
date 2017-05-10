package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.sequence.SequenceDTO
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Ignore
import spock.lang.IgnoreRest

class TrackServiceIntegrationSpec extends AbstractIntegrationSpec {

    def trackService
    def trackMapperService

    def setup() {

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
                length: 1566327
                , seqChunkSize: 20000
                , start: 0
                , end: 1566327
                , organism: organism
                , name: "Group11.6"
        ).save(failOnError: true)

        new Sequence(
                length: 78258
                , seqChunkSize: 20000
                , start: 0
                , end: 78258
                , organism: organism
                , name: "GroupUn87"
        ).save(failOnError: true)

        new Sequence(
                length: 494196
                , seqChunkSize: 20000
                , start: 0
                , end: 494196
                , organism: organism
                , name: "Group4.1"
        ).save(failOnError: true)

        new Sequence(
                length: 3883383
                , seqChunkSize: 20000
                , start: 0
                , end: 3883383
                , organism: organism
                , name: "Group2.19"
        ).save(failOnError: true)
    }

    /**
     *  GroupUn87: Projected: 0,213 <-> 718,838   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *
     *  Group11.4: Projected: 0,2538 <-> 14574,15734  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    void "non-projection of contiguous tracks should work"() {

        given: "proper inputs"
        JSONArray sequenceStrings = new JSONArray('[{name:"GroupUn87"}, {name:"Group11.4"}]')
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())
        Sequence un87Sequence = Sequence.findByName("GroupUn87")
        println trackObject as JSON

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
        assert firstLastArray.getInt(1) == 10257 + un87Sequence.length // start of the last set
        assert firstLastArray.getInt(2) == 18596 + un87Sequence.length // start of the last set
        assert firstLastArray.getInt(3) == 1

        JSONArray lastLastArray = nclist.getJSONArray(8)
        assert lastLastArray.getInt(1) == 62507 + un87Sequence.length // end of the last set
        assert lastLastArray.getInt(2) == 64197 + un87Sequence.length // end of the last set
        assert lastLastArray.getInt(3) == -1
    }

    /**
     *  Group11.4: Projected: 0,2538 <-> 14574,15734  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    void "un-projected 11.4 individually"() {
        given: "proper inputs"
        JSONArray sequenceStrings = new JSONArray('[{name:"Group11.4"}]')
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


    void "test sanitizeCoordinateArray method"() {

        given: "a user, organism, and group"
        User user = User.first()
        Organism organism = Organism.first()
        UserGroup group = UserGroup.first()
        String trackName = "Official Gene Set v3.2"
        String sequenceList = "[{\"name\":\"Group1.1\",\"start\":0,\"end\":1382403,\"reverse\":false}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackDataFileString = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/Group1.1/trackData.json"
        File trackDataFile = new File(trackDataFileString)
        println "file exists ${trackDataFile.exists()}"
        String fileText = trackDataFile.text
        JSONObject trackDataObject = JSON.parse(fileText) as JSONObject
        JSONArray trackDataArray = trackDataObject.getJSONObject("intervals").getJSONArray("classes")
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: organism.commonName
                , trackName: "Official Gene Set v3.2"
                , sequenceName: "Group1.1"
        )
        trackMapperService.storeTrack(sequenceDTO, trackDataArray)

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
        JSONArray payloadOneReturnArray = trackService.sanitizeCoordinateArray(payloadOneArray, sequenceDTO)

        then: "we should see an empty coordinate JSON array"
        println "PAYLOAD ONE RETURN ARRAY: ${payloadOneReturnArray.toString()}"
        assert payloadOneReturnArray.size() == 1
        assert payloadOneArray.size() == payloadOneReturnArray.size()

        when: "we try to sanitize a coordinate JSON array that has 2 sub-features with invalid coordinates"
        JSONArray payloadTwoArray = JSON.parse(payloadTwoString) as JSONArray
        JSONArray payloadTwoReturnArray = trackService.sanitizeCoordinateArray(payloadTwoArray, sequenceDTO)

        then: "we should see a valid coordinate JSON array without those 2 sub-features"
        println "PAYLOAD TWO RETURN ARRAY: ${payloadTwoReturnArray.toString()}"
        assert payloadTwoReturnArray.toString() == sanitizedPayloadTwoString
        assert payloadTwoArray.size() == payloadTwoReturnArray.size()

        when: "we try to sanitize a coordinate JSON array that has a subList whose top-level feature has invalid coordinates"
        JSONArray payloadThreeArray = JSON.parse(payloadThreeString) as JSONArray
        JSONArray payloadThreeReturnArray = trackService.sanitizeCoordinateArray(payloadThreeArray, sequenceDTO)

        then: " we should see a valid coordinate JSON array with an empty subList"
        println "PAYLOAD THREE RETURN ARRAY: ${payloadThreeReturnArray.toString()}"
        assert payloadThreeReturnArray.toString() == sanitizedPayloadThreeString
        assert payloadThreeArray.size() == payloadThreeReturnArray.size()

        when: "we try to sanitize a coordinate JSON array that has a subList whose sub-features have invalid coordinates"
        JSONArray payloadFourArray = JSON.parse(payloadFourString) as JSONArray
        JSONArray payloadFourReturnArray = trackService.sanitizeCoordinateArray(payloadFourArray, sequenceDTO)

        then: "we should see a valid coordinate JSON array that has a subList without those sub-features"
        println "PAYLOAD FOUR RETURN ARRAY: ${payloadFourReturnArray.toString()}"
        assert payloadFourReturnArray.toString() == sanitizedPayloadFourString
        assert payloadFourArray.size() == payloadFourReturnArray.size()
    }

    void "project tracks A1, A2, B1"() {

        given: "proper inputs"
        String sequenceList = '[{"name":"Group11.4", "start":52653, "end":59162, "feature":{"name":"GB52236-RA"}},{"name":"Group11.4", "start":10057, "end":18796, "feature":{"name":"GB52238-RA"}},{"name":"GroupUn87", "start":10311, "end":26919, "feature":{"name":"GB53497-RA"}}]'
        String refererLoc = "{\"name\":\"GB52236-RA (Group11.4)::GB52238-RA (Group11.4)::GB53497-RA (GroupUn87)\", \"padding\":0, \"start\":52853, \"end\":104277, \"sequenceList\":${sequenceList}}:52853..104277"
        String location = ":52853..104277"
        JSONArray sequenceStrings = new JSONArray(sequenceList)
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())
//
        then: "we expect to get sane results"
        assert trackObject.featureCount == 10G
        assert trackObject.intervals.nclist.size() == 3
        assert trackObject.intervals.nclist.getJSONArray(0).getInt(1) == 200 + MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(0).getInt(2) == 6309 + MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(1).getInt(1) == 6709 + MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(1).getInt(2) == 15048 + MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(2).getInt(1) == 15448 + MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(2).getInt(2) == 31656 + MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH

    }

    void "project tracks B1, A1, A2"() {

        given: "proper inputs"
        String sequenceList = "[{\"name\":\"GroupUn87\", \"start\":10311, \"end\":26919, \"feature\":{\"name\":\"GB53497-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}},{\"name\":\"Group11.4\", \"start\":52653, \"end\":59162, \"feature\":{\"name\":\"GB52236-RA\"}}]"
        String refererLoc = "{\"id\":39616, \"name\":\"GB53497-RA (GroupUn87)::GB52238-RA (Group11.4)::GB52236-RA (Group11.4)\", \"padding\":0, \"start\":10511, \"end\":104277, \"sequenceList\":${sequenceList}}:10511..104277"
        String location = ":1..31856"
        JSONArray sequenceStrings = new JSONArray(sequenceList)
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())
//
        then: "we expect to get sane results"
        assert trackObject.featureCount == 10
        assert trackObject.intervals.nclist.size() == 3
        assert trackObject.intervals.nclist.getJSONArray(0).getInt(1) == 200
        assert trackObject.intervals.nclist.getJSONArray(0).getInt(2) == 16408
        assert trackObject.intervals.nclist.getJSONArray(1).getInt(1) == 16808
        assert trackObject.intervals.nclist.getJSONArray(1).getInt(2) == 25147
        assert trackObject.intervals.nclist.getJSONArray(2).getInt(1) == 25547 + org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(2).getInt(2) == 31656 + org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH

    }

    void "get two large scaffold chunks, 1.10::11.6"() {

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":false},{\"name\":\"Group11.6\",\"start\":0,\"end\":1566327,\"reverse\":false}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we ingest the data"
        println "# of sequences ${Sequence.count}"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we expect stuff not to blow up"
        assert trackObject != null

        when: "when we get the nclist"
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order "
        assert nclistArray.size() == 5
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][2] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[2][2]
        assert nclistArray[2][2] < nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[3][2]
        assert nclistArray[3][2] > nclistArray[4][1] // some overlap here
        assert nclistArray[4][1] < nclistArray[4][2]

    }

    void "small scaffold should be reversed properly"() {

        given: "a request for 11.4 (small) forward"
        String sequenceList = "[{\"name\":\"Group11.4\",\"start\":0,\"end\":75085,\"reverse\":false}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":0..1000"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we create a forward request for the 11.4"
        println "# of sequences ${Sequence.count}"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we should get results"
        assert trackObject != null

        when: "we grab the data"
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        println nclistArray[1][2] + " vs " + nclistArray[2][1]

        then: "we expect the order to be correct"
        assert nclistArray.size() == 5
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][2] > nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[2][2]
        assert nclistArray[2][2] < nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[3][2]
        assert nclistArray[3][2] < nclistArray[4][1]
        assert nclistArray[4][1] < nclistArray[4][2]

        when: "We reverse it"
        sequenceList = "[{\"name\":\"Group11.4\",\"start\":0,\"end\":75085,\"reverse\":true}]"
        refererLoc = "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        println "# of sequences ${Sequence.count}"
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we should get results"
        assert trackObject != null

        when: "we grab the data"
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the order to be correct"
        assert nclistArray.size() == 5
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][2] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[2][2]
        assert nclistArray[2][2] > nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[3][2]
        assert nclistArray[3][2] < nclistArray[4][1]
        assert nclistArray[4][1] < nclistArray[4][2]
    }

    void "large scaffold should be reversed properly"() {

        given: "a request for 1.10 (large) forward"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":false}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":0..1000"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we create a forward request for the 1.10"
        println "# of sequences ${Sequence.count}"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we should get results"
        assert trackObject != null

        when: "we grab the data"
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the order to be correct"
        assert nclistArray.size() == 3
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][2] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[2][2]

        when: "We reverse it"
        sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":true}]"
        refererLoc = "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        println "# of sequences ${Sequence.count}"
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we should get results"
        assert trackObject != null

        when: "we grab the data"
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the order to be correct"
        assert nclistArray.size() == 3
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][2] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[2][2]
    }

    void "get two small scaffolds, 11.4,Un87"() {

        given: "proper 11.4 and Un87, should go the duration, though if we reverse 11.4, it should still go the length (beyond the length of the first one)"
        // TODO: set this properly
        String sequenceList = "[{\"name\":\"Group11.4\",\"start\":0,\"end\":75085,\"reverse\":false},{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258,\"reverse\":false}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":0..1000"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we ingest the data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we expect stuff not to blow up"
        assert trackObject != null

        when: "when we get the nclist"
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order "
        assert nclistArray.size() == 9
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[0][5] == "Group11.4"
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[1][5] == "Group11.4"
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[2][5] == "Group11.4"
        assert nclistArray[3][1] < nclistArray[4][1]
        assert nclistArray[3][5] == "Group11.4"
        assert nclistArray[4][1] < nclistArray[5][1]
        assert nclistArray[4][5] == "Group11.4"
        assert nclistArray[5][1] < nclistArray[6][1]
        assert nclistArray[5][5] == "GroupUn87"
        assert nclistArray[6][1] < nclistArray[7][1]
        assert nclistArray[6][5] == "GroupUn87"
        assert nclistArray[7][1] < nclistArray[8][1]
        assert nclistArray[7][5] == "GroupUn87"
        assert nclistArray[8][5] == "GroupUn87"


        when: "we reverse the next one"
        sequenceList = "[{\"name\":\"Group11.4\",\"start\":0,\"end\":75085,\"reverse\":true},{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258,\"reverse\":false}]"
        refererLoc = "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order "
        assert nclistArray.size() == 9
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[0][5] == "Group11.4"
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[1][5] == "Group11.4"
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[2][5] == "Group11.4"
        assert nclistArray[3][1] < nclistArray[4][1]
        assert nclistArray[3][5] == "Group11.4"
        assert nclistArray[4][1] < nclistArray[5][1]
        assert nclistArray[4][5] == "Group11.4"
        assert nclistArray[5][1] < nclistArray[6][1]
        assert nclistArray[5][5] == "GroupUn87"
        assert nclistArray[6][1] < nclistArray[7][1]
        assert nclistArray[6][5] == "GroupUn87"
        assert nclistArray[7][1] < nclistArray[8][1]
        assert nclistArray[7][5] == "GroupUn87"
        assert nclistArray[8][5] == "GroupUn87"

        when: "we reverse both"
        sequenceList = "[{\"name\":\"Group11.4\",\"start\":0,\"end\":75085,\"reverse\":true},{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258,\"reverse\":true}]"
        refererLoc = "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order "
        assert nclistArray.size() == 9
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[0][5] == "Group11.4"
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[1][5] == "Group11.4"
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[2][5] == "Group11.4"
        assert nclistArray[3][1] < nclistArray[4][1]
        assert nclistArray[3][5] == "Group11.4"
        assert nclistArray[4][1] < nclistArray[5][1]
        assert nclistArray[4][5] == "Group11.4"
        assert nclistArray[5][1] < nclistArray[6][1]
        assert nclistArray[5][5] == "GroupUn87"
        assert nclistArray[6][1] < nclistArray[7][1]
        assert nclistArray[6][5] == "GroupUn87"
        assert nclistArray[7][1] < nclistArray[8][1]
        assert nclistArray[7][5] == "GroupUn87"
        assert nclistArray[8][5] == "GroupUn87"
    }

    void "get two large scaffolds, 1.10::11.6 we should be able to reverse the first one and still have it extend properly"() {

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":false},{\"name\":\"Group11.6\",\"start\":0,\"end\":1566327,\"reverse\":false}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we project the data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order "
        assert nclistArray.size() == 5
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[4][1]

        when: "we reverse the next one"
        sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":true},{\"name\":\"Group11.6\",\"start\":0,\"end\":1566327,\"reverse\":false}]"
        refererLoc = "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)


        then: "same set of values, and we are still in both scaffolds"
        assert nclistArray.size() == 5
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[4][1]

        when: "we reverse both"
        sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":true},{\"name\":\"Group11.6\",\"start\":0,\"end\":1566327,\"reverse\":true}]"
        refererLoc = "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)


        then: "same set of values, and we are still in both scaffolds"
        assert nclistArray.size() == 5
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[4][1]
    }

    void "if we have three small feature scaffolds, they should all be offset correctly"() {

        given: "proper input"
        // String http://localhost:8080/apollo/615463246294435289153778572/jbrowse/data/tracks/Official%20Gene%20Set%20v3.2/%7B%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%22start%22:10057,%22end%22:18796,%22reverse%22:false,%22feature%22:%7B%22name%22:%22GB52238-RA%22,%22start%22:10057,%22end%22:18796,%22parent_id%22:%22Group11.4%22%7D%7D,%7B%22name%22:%22GroupUn87%22,%22start%22:29196,%22end%22:30529,%22reverse%22:false,%22feature%22:%7B%22name%22:%22GB53498-RA%22,%22start%22:29196,%22end%22:30529,%22parent_id%22:%22GroupUn87%22%7D%7D,%7B%22name%22:%22Group4.1%22,%22start%22:352310,%22end%22:399504,%22reverse%22:false,%22feature%22:%7B%22name%22:%22GB49640-RA%22,%22start%22:352310,%22end%22:399504,%22parent_id%22:%22Group4.1%22%7D%7D]%7D:-1..-1/trackData.json
//        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":false},{\"name\":\"Group11.6\",\"start\":0,\"end\":1566327,\"reverse\":false}]"
        String sequenceList = "[{\"name\":\"Group11.4\",\"start\":10057,\"end\":18796,\"reverse\":false,\"feature\":{\"name\":\"GB52238-RA\",\"start\":10057,\"end\":18796,\"parent_id\":\"Group11.4\"}},{\"name\":\"GroupUn87\",\"start\":29196,\"end\":30529,\"reverse\":false,\"feature\":{\"name\":\"GB53498-RA\",\"start\":29196,\"end\":30529,\"parent_id\":\"GroupUn87\"}},{\"name\":\"Group4.1\",\"start\":352310,\"end\":399504,\"reverse\":false,\"feature\":{\"name\":\"GB49640-RA\",\"start\":352310,\"end\":399504,\"parent_id\":\"Group4.1\"}}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we project the data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order and there should be NO overlap"
        assert nclistArray.size() == 3
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]

        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][1] < nclistArray[2][1]

        assert nclistArray[2][1] < nclistArray[2][2]
    }

    void "project a single feature from a chunked scaffold"() {

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":891079,\"end\":933237,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40737-RA\",\"start\":891079,\"end\":933237}}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackName = "Official Gene Set v3.2"
        String fileName = "lf-2.json"
        String chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName}"
        String trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we project the data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        MultiSequenceProjection multiSequenceProjection = projectionService.getCachedProjection(refererLoc)
        def projectionChunkList = multiSequenceProjection.projectionChunkList.projectionChunkList

        then: "should we have multiple chunks (1-2) or map chunk 2 to 1 and get lf-1.json instead"
//        assert "Group1.10"==projectionChunkList.get(0).sequence
        assert 1 == projectionChunkList.size()


        when: "when we get lf-2.json (or lf-0.json) it should now work"
        JSONArray trackArray = trackService.projectTrackChunk(fileName, chunkFileName, refererLoc, Organism.first(), trackName)

        then: "we expect the start and the stop to be in order and there should be NO overlap"
        assert trackArray.size() == 1
        assert trackArray[0].size() == 11
    }

//    @IgnoreRest
    void "for a large scaffold (1.10), provide two features (GB40809 and GB40811)"() {
        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":291158,\"end\":315360,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40809-RA\",\"start\":291158,\"end\":315360}},{\"name\":\"Group1.10\",\"start\":366840,\"end\":372101,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40811-RA\",\"start\":366840,\"end\":372101}}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackName = "Official Gene Set v3.2"
        String trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we project the data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        MultiSequenceProjection multiSequenceProjection = projectionService.getCachedProjection(refererLoc)
        def projectionChunkList = multiSequenceProjection.projectionChunkList.projectionChunkList
        JSONArray ncListArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "should we have multiple chunks (1-2) or map chunk 2 to 0 and get lf-1.json instead"
//        assert "Group1.10"==projectionChunkList.get(0).sequence
        assert projectionChunkList.size() == 1
        assert ncListArray.size() == 1
        assert ncListArray[0].size() == 4
        assert ncListArray[0][1] == 0
        assert ncListArray[0][2] == 29463
        assert ncListArray[0][3] == 1


        when: "when we get lf-1.json it should now work"
        String fileName = "lf-1.json"
        String chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName}"
        JSONArray trackArray = trackService.projectTrackChunk(fileName, chunkFileName, refererLoc, Organism.first(), trackName)

        then: "we expect the start and the stop to be in order and there should be NO overlap"
        // index 37 and 42 should be kept
        // index 42 should promote any non-0 "sublists"
        assert trackArray.size() == 2
        assert trackArray[0].size() == 11
        assert trackArray[1].size() == 12

    }

    void "for two large scaffolds (1.10 and 11.6), if the first has one feature (GB40811) regions and the second one has one (GB55200)"() {

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":366840,\"end\":372101,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40811-RA\",\"start\":366840,\"end\":372101}},{\"name\":\"Group11.6\",\"start\":680818,\"end\":758231,\"reverse\":false,\"feature\":{\"parent_id\":\"Group11.6\",\"name\":\"GB55200-RA\",\"start\":680818,\"end\":758231}}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackName = "Official Gene Set v3.2"
        String trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we get the initial track data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        MultiSequenceProjection multiSequenceProjection = projectionService.getCachedProjection(refererLoc)
        def projectionChunkList = multiSequenceProjection.projectionChunkList.projectionChunkList

        then: "should we have multiple chunks (1-2) or map chunk 2 to 1 and get lf-1.json instead"
        assert 2 == projectionChunkList.size()

        when: "we project the first chunk lf-1.json"
        String fileName1 = "lf-1.json"
        String chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName1}"
        JSONArray trackArray = trackService.projectTrackChunk(fileName1, chunkFileName, refererLoc, Organism.first(), trackName)


        then: "we should get a single track defined by GB4076 (which coverse GB40811)"
        assert trackArray.size() == 1
        assert trackArray[0].size() == 12
        assert trackArray[0][6] == "GB40764-RA"
        assert trackArray[0][11]["Sublist"][0][8] == "GB40811-RA"


        when: "when we get lf-2.json (or lf-0.json) it should now work"
        String fileName2 = "lf-2.json"
        trackArray = trackService.projectTrackChunk(fileName2, chunkFileName, refererLoc, Organism.first(), trackName)

        then: "we expect the start and the stop to be in order and there should be NO overlap"
        assert trackArray.size() == 1
        assert trackArray[0].size() == 12
        assert trackArray[0][6] == "GB55200-RA"
        assert trackArray[0][11]["Sublist"].size() == 2

    }

//    @IgnoreRest
    void "for one large scaffolds (1.10), we should view two feature objects (GB40809-RA and GB408011-RA)"() {

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":291158,\"end\":315360,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40809-RA\",\"start\":291158,\"end\":315360}},{\"name\":\"Group1.10\",\"start\":366840,\"end\":372101,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40811-RA\",\"start\":366840,\"end\":372101}}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackName = "Official Gene Set v3.2"
        String trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"

        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we get the initial track data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        JSONArray ncListArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        MultiSequenceProjection multiSequenceProjection = projectionService.getCachedProjection(refererLoc)
        def projectionChunkList = multiSequenceProjection.projectionChunkList.projectionChunkList

        then: "should we have multiple chunks (1-2) or map chunk 2 to 1 and get lf-1.json instead"
        assert ncListArray.size() == 1
        assert ncListArray[0].size() == 4
        assert projectionChunkList.size() == 1

        when: "we project the first chunk lf-1.json"
        String fileName1 = "lf-1.json"
        String chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName1}"
        JSONArray trackArray = trackService.projectTrackChunk(fileName1, chunkFileName, refererLoc, Organism.first(), trackName)


        then: "we should get a single track defined by GB4076 (which covers GB40811)"
        assert trackArray.size() == 2
        assert trackArray[0][8] == "GB40809-RA"
        assert trackArray[0][1] == 200
        assert trackArray[0][2] == 24202 - 200
//        assert trackArray[1][8]=="GB40811-RA"
        assert trackArray[1][8] == "GB40764-RA"
        assert trackArray[1][1] == 24202
        assert trackArray[1][2] == 29463
        // note that we have a sublist as well
//        assert trackArray[1][11]["Sublist"][4]=="GB40811-RA"

    }


    void "for one large scaffolds (1.10), we should view two feature objects, in the same scaffold, but NOT the first chunk (GB40856-RA and GB40857-RA)"() {

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":1216624,\"end\":1235816,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40856-RA\",\"start\":1216624,\"end\":1235816}},{\"name\":\"Group1.10\",\"start\":1241950,\"end\":1247222,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40857-RA\",\"start\":1241950,\"end\":1247222}}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackName = "Official Gene Set v3.2"
        String trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"

        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we get the initial track data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        JSONArray ncListArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        MultiSequenceProjection multiSequenceProjection = projectionService.getCachedProjection(refererLoc)
        def projectionChunkList = multiSequenceProjection.projectionChunkList.projectionChunkList

        then: "should we have a single chunk on 2"
        assert ncListArray.size() == 1
        assert ncListArray[0][1] == 0
        assert ncListArray[0][2] == 24464
        assert ncListArray[0][3] == 2
        assert projectionChunkList.size() == 1


        when: "we project the first chunk lf-1.json"
        String fileName1 = "lf-2.json"
        String chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName1}"
        JSONArray trackArray = trackService.projectTrackChunk(fileName1, chunkFileName, refererLoc, Organism.first(), trackName)


        then: "we should get a single track defined by GB4076 (which covers GB40811)"
        assert trackArray.size() == 2
        assert trackArray[0][8] == "GB40856-RA"
        assert trackArray[0][1] == 200
        assert trackArray[0][2] == 19192 - 200
        assert trackArray[1][8] == "GB40857-RA"
        assert trackArray[1][1] == 19192 + 200
        assert trackArray[1][2] == 24264

    }

//    @IgnoreRest
    void "for one large scaffolds (1.10), we should view two feature objects, in different scaffolds, but NEITHER in the first chunk (GB40856-RA and GB40866-RA)"() {

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":1216624,\"end\":1235816,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40856-RA\",\"start\":1216624,\"end\":1235816}},{\"name\":\"Group1.10\",\"start\":1378918,\"end\":1383692,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40866-RA\",\"start\":1378918,\"end\":1383692}}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackName = "Official Gene Set v3.2"
        String trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"

        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we get the initial track data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        JSONArray ncListArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        MultiSequenceProjection multiSequenceProjection = projectionService.getCachedProjection(refererLoc)
        def projectionChunkList = multiSequenceProjection.projectionChunkList.projectionChunkList

        then: "should we have multiple chunks (0-2) or map chunk 2 to 0 and get lf-0.json instead"
        assert projectionChunkList.size() == 1
        assert ncListArray.size() == 2
        assert ncListArray[0][1] == 0
        assert ncListArray[0][2] == 19192
        assert ncListArray[0][3] == 2
        assert ncListArray[1][1] == 19192
        assert ncListArray[1][2] == 23966 // ?
        assert ncListArray[1][3] == 3


        when: "we project the first chunk lf-2.json"
        String fileName1 = "lf-2.json"
        String chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName1}"
        JSONArray trackArray = trackService.projectTrackChunk(fileName1, chunkFileName, refererLoc, Organism.first(), trackName)


        then: "we should get a single track defined by GB4076 (which covers GB40811)"
        assert trackArray.size() == 1
        assert trackArray[0][6] == "GB40856-RA"
        assert trackArray[0][1] == 200
        assert trackArray[0][2] == 19192 - 200

        when: "we project the first chunk lf-2.json"
        fileName1 = "lf-3.json"
        chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName1}"
        trackArray = trackService.projectTrackChunk(fileName1, chunkFileName, refererLoc, Organism.first(), trackName)


        then: "we should get a single track defined by GB4076 (which covers GB40811)"
        assert trackArray.size() == 1
        assert trackArray[0][8] == "GB40866-RA"
        assert trackArray[0][1] == 19192 + 200
        assert trackArray[0][2] == 23766

    }

//    @IgnoreRest
    void "for one large scaffolds (1.10), we should view two feature objects, but in different chunks (GB40809-RA and GB40856-RA)"() {

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":291158,\"end\":315360,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40809-RA\",\"start\":291158,\"end\":315360}},{\"name\":\"Group1.10\",\"start\":1216624,\"end\":1235816,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40856-RA\",\"start\":1216624,\"end\":1235816}}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackName = "Official Gene Set v3.2"
        String trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"

        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we get the initial track data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        JSONArray ncListArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        MultiSequenceProjection multiSequenceProjection = projectionService.getCachedProjection(refererLoc)
        def projectionChunkList = multiSequenceProjection.projectionChunkList.projectionChunkList

        then: "should we have multiple chunks (0-2) or map chunk 2 to 0 and get lf-0.json instead"
        assert projectionChunkList.size() == 1
        assert ncListArray.size() == 2
        assert ncListArray[0][1] == 0
        assert ncListArray[0][2] == 24202
        assert ncListArray[0][3] == 1
        assert ncListArray[1][1] == 24202
        assert ncListArray[1][2] == 43394
        assert ncListArray[1][3] == 2


        when: "we project the first chunk lf-1.json"
        String fileName1 = "lf-1.json"
        String chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName1}"
        JSONArray trackArray = trackService.projectTrackChunk(fileName1, chunkFileName, refererLoc, Organism.first(), trackName)


        then: "we should get a single track defined by GB40809"
        assert trackArray.size() == 1
        assert trackArray[0][8] == "GB40809-RA"
        assert trackArray[0][1] == 200
        assert trackArray[0][2] == 24002


        when: "we project the first chunk lf-2.json"
        fileName1 = "lf-2.json"
        chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName1}"
        trackArray = trackService.projectTrackChunk(fileName1, chunkFileName, refererLoc, Organism.first(), trackName)


        then: "we should get a single track defined by GB40856"
        assert trackArray.size() == 1
        assert trackArray[0][8] == "GB40856-RA"
        assert trackArray[0][1] == 24402
        assert trackArray[0][2] == 43194

    }

    /**
     *  Two chunks are implied:
     *  - chunk 1 (lf-1.json) on Group1.10 from 19636 to 588668 -> 0-29463 (or so)
     *  - chunk 2 (lf-2.json) on Group11.16 from // 680K to 758K- > 29463 - 106876 , but it is still using chunk 1
     *
     * The first chunk is on Group1.10 should contain GB40809 and GB40811 (2 tracks) from 0-24202 and 24202 to 29463 (roughly),
     * the original coordinates are 291K - 316K  and 366K - 372K
     *
     * The second chunk is on Group11.16 should be from 680K - 758K and maps to 29K to 106K
     *
     */
    void "for two large scaffolds (1.10 and 11.6), if the first has two features (GB40809, GB40811) regions and the second one has one (GB55200)"() {

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":291158,\"end\":315360,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40809-RA\",\"start\":291158,\"end\":315360}},{\"name\":\"Group1.10\",\"start\":366840,\"end\":372101,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40811-RA\",\"start\":366840,\"end\":372101}},{\"name\":\"Group11.6\",\"start\":680818,\"end\":758231,\"reverse\":false,\"feature\":{\"parent_id\":\"Group11.6\",\"name\":\"GB55200-RA\",\"start\":680818,\"end\":758231}}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackName = "Official Gene Set v3.2"
        String trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we get the initial track data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        JSONArray ncListArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        MultiSequenceProjection multiSequenceProjection = projectionService.getCachedProjection(refererLoc)
        def projectionChunkList = multiSequenceProjection.projectionChunkList.projectionChunkList

        then: "should we have multiple chunks (1-2) or map chunk 2 to 1 and get lf-1.json instead"
        // should provide 2 chunks . . .apparently, 1 and 5

        // the first chunks is on Sequence 1.10 and the second chunk is on
        assert ncListArray.size() == 2
        assert ncListArray[0].size() == 4
        assert ncListArray[0][1] == 0
        // TODO: should be 29463, won't affect much as it calls the chunk
        assert ncListArray[0][2] == 24202 + 5261
        assert ncListArray[0][3] == 1
        assert ncListArray[1].size() == 4
        // TODO: should be 29463, won't affect much as it calls the chunk
        assert ncListArray[1][1] == 24202 + 5261
        // TODO: should 106876, won't affect much as it calls the chunk
        assert ncListArray[1][2] == 101615 + 5261
        assert ncListArray[1][3] == 2 // not sure if this is correct
        assert projectionChunkList.size() == 2
        assert projectionChunkList[0].sequenceOffset == 0
        assert projectionChunkList[0].chunkArrayOffset == 0
        assert projectionChunkList[1].sequenceOffset == 29463
        assert projectionChunkList[1].chunkArrayOffset == 1

        when: "we project the first chunk lf-1.json"
        String fileName1 = "lf-1.json"
        String chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName1}"
        JSONArray trackArray = trackService.projectTrackChunk(fileName1, chunkFileName, refererLoc, Organism.first(), trackName)


        then: "we should get a single track defined by GB4076 (which coverse GB40811)"
        assert trackArray.size() == 2
        assert trackArray[0][8] == "GB40809-RA"
        assert trackArray[0][1] == 200
        // TODO: should be 29463, won't affect much as it calls the chunk
        assert trackArray[0][2] == 24202 - 200
        assert trackArray[1][8] == "GB40764-RA"
        assert trackArray[1][1] == 24202
        assert trackArray[1][2] == 29463


        when: "when we get lf-2.json it should now work"
        String fileName2 = "lf-2.json"
        chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName2}"
        trackArray = trackService.projectTrackChunk(fileName2, chunkFileName, refererLoc, Organism.first(), trackName)

        then: "we expect the start and the stop to be in order and there should be NO overlap"
        assert trackArray.size() == 1
        assert trackArray[0].size() == 12
        assert trackArray[0][6] == "GB55200-RA"
        assert trackArray[0][1] == 29663 // ?
        assert trackArray[0][2] == 106676
//        assert trackArray[0][10]["Sublist"].size() == 2

    }

//    @IgnoreRest
    // TODO: fix as part of of #1562
    @Ignore
    void "on two large scaffolds, if the the first has one feature Group2.19 (GB55415-RA) and the second has two features Group1.10 (GB40809-RA, GB40811-RA)"() {
        given: "proper input"
        // TODO: encode
        String sequenceList = "[{\"name\":\"Group2.19\",\"start\":1660760,\"end\":1661749,\"reverse\":false,\"feature\":{\"parent_id\":\"Group2.19\",\"name\":\"GB55415-RA\",\"start\":1660760,\"end\":1661749}},{\"name\":\"Group1.10\",\"start\":291158,\"end\":315360,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40809-RA\",\"start\":291158,\"end\":315360}},{\"name\":\"Group1.10\",\"start\":366840,\"end\":372101,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40811-RA\",\"start\":366840,\"end\":372101}}]"
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackName = "Official Gene Set v3.2"
        String trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we get the initial track data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        JSONArray ncListArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        MultiSequenceProjection multiSequenceProjection = projectionService.getCachedProjection(refererLoc)
        def projectionChunkList = multiSequenceProjection.projectionChunkList.projectionChunkList

        then: "should we have multiple chunks (1-2) or map chunk 2 to 1 and get lf-1.json instead"
        // should provide 2 chunks . . .apparently, 1 and 5

        // the first chunks is on Sequence 1.10 and the second chunk is on
        assert projectionChunkList.size() == 2
        assert ncListArray.size() == 2
        assert ncListArray[0].size() == 4
        assert ncListArray[0][1] == 0
        // TODO: should be 29463, won't affect much as it calls the chunk
        assert ncListArray[0][2] == 989
        assert ncListArray[0][3] == 1 // I think this should be 1

        assert ncListArray[1].size() == 4
        // TODO: should be 29463, won't affect much as it calls the chunk
        assert ncListArray[1][1] == 989
        // TODO: should 106876, won't affect much as it calls the chunk
        assert ncListArray[1][2] == 25191 // don't know
        assert ncListArray[1][3] == 2 // I think this should be 2

        when: "we project the first chunk lf-1.json"
        String fileName1 = "lf-1.json"
        String chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName1}"
        JSONArray trackArray = trackService.projectTrackChunk(fileName1, chunkFileName, refererLoc, Organism.first(), trackName)


        then: "we should get a single track defined by GB4076 (which coverse GB40811)"
        assert trackArray.size() == 1
        assert trackArray[0][5] == "GB55415-RA"
        assert trackArray[0][1] == 200
        // TODO: should be 29463, won't affect much as it calls the chunk
        assert trackArray[0][2] == 789

        when: "we project the second chunk lf-2.json"
        String fileName2 = "lf-2.json"
        chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName2}"
        trackArray = trackService.projectTrackChunk(fileName2, chunkFileName, refererLoc, Organism.first(), trackName)


        then: "we should get a single track defined by GB4076 (which coverse GB40811)"
        assert trackArray.size() == 2
        assert trackArray[0][5] == "GB40809-RA"
        assert trackArray[0][1] == 200
        // TODO: should be 29463, won't affect much as it calls the chunk
        assert trackArray[0][2] == 789

        assert trackArray[1][5] == "GB40811-RA"
        assert trackArray[1][1] == 200 // TODO:
        // TODO: should be 29463, won't affect much as it calls the chunk
        assert trackArray[1][2] == 789

    }

//    @IgnoreRest
    void "view a small scaffold"(){
        given: "proper input"
        Integer maxValue = 78258
        String sequenceList = '[{"name":"GroupUn87","start":0,"end":78258,"reverse":false,"organism":"Honeybee","location":[{"fmin":0,"fmax":78258}]}]'
        String refererLoc = "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackName = "Official Gene Set v3.2"
        String trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we get the initial track data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        JSONArray ncListArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we should get 4 features in the correct order"
        assert ncListArray.size()==4
        assert ncListArray[0][3]==1
        assert ncListArray[0][2]-ncListArray[0][1]==10179-9966
        assert ncListArray[1][3]==1
        assert ncListArray[1][2]-ncListArray[1][1]==26719-10511
        assert ncListArray[2][3]==1
        assert ncListArray[2][2]-ncListArray[2][1]==30329-29396
        assert ncListArray[3][3]==1
        assert ncListArray[3][2]-ncListArray[3][1]==45575-45455

        assert ncListArray[0][2]==10179
        assert ncListArray[3][2]==45575


        when: "we reverse this"
        sequenceList = '[{"name":"GroupUn87","start":0,"end":78258,"reverse":true,"organism":"Honeybee","location":[{"fmin":0,"fmax":78258}]}]'
        refererLoc = "{\"sequenceList\":${sequenceList}}"
        trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        ncListArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we should get the same 4 features reversed"
        assert ncListArray.size()==4
        assert ncListArray[3][3]==-1
        assert ncListArray[3][2]-ncListArray[3][1]==10179-9966
        assert ncListArray[2][3]==-1
        assert ncListArray[2][2]-ncListArray[2][1]==26719-10511
        assert ncListArray[1][3]==-1
        assert ncListArray[1][2]-ncListArray[1][1]==30329-29396
        assert ncListArray[0][3]==-1
        assert ncListArray[0][2]-ncListArray[0][1]==45575-45455


        assert ncListArray[3][1]==78258 - 10179
        assert ncListArray[0][1]==78258 - 45575
    }

}
