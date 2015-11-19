package org.bbop.apollo

import edu.unc.genomics.io.BigWigFileReader
import org.bbop.apollo.projection.MultiSequenceProjection
import org.codehaus.groovy.grails.web.json.JSONArray

import java.nio.file.FileSystems
import java.nio.file.Path

class BigwigServiceIntegrationSpec extends AbstractIntegrationSpec {

    def bigwigService
    def projectionService

    String bigwigFile = "test/integration/resources/sequences/honeybee-bigwig/forager-small.bw"

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


    void "make sure we can read the file and that its valid and contains the proper groups"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.4", "GroupUn87"]

        when: "we get the projected track data "
        File file = new File(bigwigFile)
        Path path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)

        then: "we expect to get sane results"
        assert 2409==bigWigFileReader.getChrStart(sequenceStrings.first())
        assert 2169==bigWigFileReader.getChrStart(sequenceStrings.last())
        assert 74715==bigWigFileReader.getChrStop(sequenceStrings.first())
        assert 54047==bigWigFileReader.getChrStop(sequenceStrings.last())
    }

    /**
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    void "ref-seq only projection for a baseline on 11.4 individually"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.4"]
        JSONArray featuresArray = new JSONArray()
        File file = new File(bigwigFile)
        Path path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)

        when: "we get the projected track data "
        JSONArray resultArray = bigwigService.processSequence(featuresArray, sequenceStrings.first(), bigWigFileReader,-25001,0)

        then: "we expect to get sane results"
        assert resultArray.size()==232

        when: "we get the projected track data "
        featuresArray = new JSONArray()
        resultArray = bigwigService.processSequence(featuresArray, sequenceStrings.first(), bigWigFileReader,49999,75000)

        then: "we expect to get sane results"
        assert resultArray.size()==190
    }

    /**
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    void "un-projected 11.4 individually"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.4"]
        JSONArray featuresArray = new JSONArray()
        String refererLoc = "{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1:1..16607"
        File file = new File(bigwigFile)
        Path path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)

        when: "we get the projected track data "
        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, Organism.first())
        JSONArray resultArray = bigwigService.processProjection(featuresArray, projection, bigWigFileReader, -50001,0)

        then: "we expect to get sane results"
        assert featuresArray.size()==501

        when: "we get the projected track data "
        featuresArray = new JSONArray()
        resultArray = bigwigService.processProjection(featuresArray, projection, bigWigFileReader, 149999,200000)

        then: "we expect to get sane results"
        assert resultArray.size()==501
    }

    /**
     *  GroupUn87: Projected: 0,213 <-> 723,843   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    void "non-projection of contiguous tracks should work"() {

        given: "proper inputs"
        List<String> sequenceStrings = ["GroupUn87", "Group11.4"]
        String refererLoc = "{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
//        JSONObject trackObject = bigwigService.processProjection(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
    }

    /**
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    void "Projected 11.4 individually"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.4"]
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
//        JSONObject trackObject = bigwigService.processProjection(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
    }

    /**
     *
     *  GroupUn87: Projected: 0,213 <-> 723,843   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    void "exon projections of contiguous tracks should work"() {

        given: "proper inputs"
        List<String> sequenceStrings = ["GroupUn87", "Group11.4"]
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
//        JSONObject trackObject = bigwigService.processProjection(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
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
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceStrings[0]}\"},{\"name\":\"${sequenceStrings[1]}\"}], \"label\":\"${sequenceStrings.join('::')}\"}:-1..-1:1..16607"
        JSONArray array

        when: "we get the projected track data "
//        JSONObject trackObject = bigwigService.processProjection(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
    }
}
