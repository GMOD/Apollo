package org.bbop.apollo

import edu.unc.genomics.io.BigWigFileReader
import org.codehaus.groovy.grails.web.json.JSONArray
import java.nio.file.Path

class BigwigServiceIntegrationSpec extends AbstractIntegrationSpec {

    def bigwigService

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
     * There will be no projections in this one
     */
    void "regular sequence bigwig file"() {
        given:
        List<String> sequenceStrings = ["Group11.4"]
        String dataFileName = "test/integration/resources/sequences/honeybee-bigwig/forager.bw"
        String refererLoc = "{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1:1..16607"
        JSONArray featuresArray = new JSONArray()
        String pathname
        Path path
        BigWigFileReader bigWigFileReader ;

        when: "we get the projected track data "
//        bigwigService.processSequence(featuresArray, sequenceStrings.first(), refererLoc, 0,5000)

        then: "we should have a properly formatted features array"

    }

    void "unprojected bigwig file"() {

        given:
        List<String> sequenceStrings = ["Group11.4"]
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1:1..16607"
        JSONArray featuresArray = new JSONArray()

        when: "we get the projected track data "
//        bigwigService.processProjection(featuresArray, dataFileName, refererLoc, Organism.first())

        then: "we should have a properly formatted features array"

    }

    void "projected bigwig file"() {
    }


    void "contiguous unprojected bigwig file"() {
    }

    void "non-contiguous projected bigwig file"() {
    }

}
