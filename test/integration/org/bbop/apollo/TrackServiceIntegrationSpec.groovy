package org.bbop.apollo

import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class TrackServiceIntegrationSpec extends AbstractIntegrationSpec{

    def trackService

    def setup() {
        setupDefaultUserOrg()
        Organism organism = Organism.first()
        organism.directory = "test/integration/resources/sequences/honeybee-tracks/"
        organism.save(failOnError: true,flush: true )

        new Sequence(
                length: 1405242
                ,seqChunkSize: 20000
                ,start: 0
                ,end: 1405242
                ,organism: organism
                ,name: "Group11.4"
        ).save(failOnError: true)

        new Sequence(
                length: 1405242
                ,seqChunkSize: 20000
                ,start: 0
                ,end: 1405242
                ,organism: organism
                ,name: "GroupUn87"
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
        List<String> sequenceStrings = ["GroupUn87","Group11.4"]
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings,dataFileName,refererLoc,Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 10
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size()==9
        JSONArray firstArray = nclist.getJSONArray(0)
        assert firstArray.getInt(1)==9966
        assert firstArray.getInt(2)==10179

        JSONArray lastFirstArray = nclist.getJSONArray(3)
//        assert firstArray.getInt(2)==0
        assert lastFirstArray.getInt(1)==45455// end of the first set
        assert lastFirstArray.getInt(2)==45575// end of the first set

        // the next array should go somewhere completely else
        JSONArray firstLastArray = nclist.getJSONArray(4)
        assert firstLastArray.getInt(1)==10257+45575+1// end of the first set
//        assert firstLastArray.getInt(2)==185696+45575// end of the first set
        assert firstLastArray.getInt(2)==18596+45575+1// end of the first set

        JSONArray lastLastArray = nclist.getJSONArray(8)
        assert lastLastArray.getInt(1)==62507+45575+1// end of the last set
        assert lastLastArray.getInt(2)==64197+45575+1// end of the last set
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
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings,dataFileName,refererLoc,Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 6
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size()==5

        // the next array should go somewhere completely else
        JSONArray firstLastArray = nclist.getJSONArray(0)
        assert firstLastArray.getInt(1)==10257// end of the first set
//        assert firstLastArray.getInt(2)==185696+45575// end of the first set
        assert firstLastArray.getInt(2)==18596// end of the first set

        JSONArray lastLastArray = nclist.getJSONArray(4)
        assert lastLastArray.getInt(1)==62507// end of the last set
        assert lastLastArray.getInt(2)==64197// end of the last set
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
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings,dataFileName,refererLoc,Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 6
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size()==5

        // the next array should go somewhere completely else
        JSONArray firstLastArray = nclist.getJSONArray(0)
        assert firstLastArray.getInt(1)==0// end of the first set
//        assert firstLastArray.getInt(2)==185696+45575// end of the first set
        assert firstLastArray.getInt(2)==2546// end of the first set

        JSONArray lastLastArray = nclist.getJSONArray(4)
        assert lastLastArray.getInt(1)==14601// end of the last set
        assert lastLastArray.getInt(2)==15764// end of the last set
    }

    /**
     *
     *  GroupUn87: Projected: 0,213 <-> 723,843   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    void "exon projections of contiguous tracks should work"() {

        given: "proper inputs"
        List<String> sequenceStrings = ["GroupUn87","Group11.4"]
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings,dataFileName,refererLoc,Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 10
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size()==9
        JSONArray firstArray = nclist.getJSONArray(0)
        assert firstArray.getInt(1)==0
        assert firstArray.getInt(2)==213

        JSONArray lastFirstArray = nclist.getJSONArray(3)
        assert lastFirstArray.getInt(1)==723
        assert lastFirstArray.getInt(2)==843  // end of the first set

        // the next array should start at the end of thast one
        JSONArray firstLastArray = nclist.getJSONArray(4)
        assert firstLastArray.getInt(1)==0+843+1
        assert firstLastArray.getInt(2)==2546+843+1

        JSONArray lastLastArray = nclist.getJSONArray(8)
        assert lastLastArray.getInt(1)==14601+843+1
        assert lastLastArray.getInt(2)==15764+843+1
    }
}
