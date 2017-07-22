package org.bbop.apollo

import edu.unc.genomics.io.BigWigFileReader
import grails.converters.JSON
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import spock.lang.Ignore

import java.nio.file.FileSystems
import java.nio.file.Path

class BigwigServiceIntegrationSpec extends AbstractIntegrationSpec {

    def bigwigService
    def projectionService

    String bigwigForagerFile = "test/integration/resources/sequences/honeybee-bigwig/forager-small.bw"
    String bigwigVolvoxSineFile = "test/integration/resources/sequences/volvox-bigwig/volvox_sine.bw"

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
//        projectionService.clearProjections()
    }

    def cleanup() {
    }


    void "make sure we can read the file and that its valid and contains the proper groups"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.4", "GroupUn87"]

        when: "we get the projected track data "
        File file = new File(bigwigForagerFile)
        Path path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)

        then: "we expect to get sane results"
        assert 2409 == bigWigFileReader.getChrStart(sequenceStrings.first())
        assert 2169 == bigWigFileReader.getChrStart(sequenceStrings.last())
        assert 74715 == bigWigFileReader.getChrStop(sequenceStrings.first())
        assert 54047 == bigWigFileReader.getChrStop(sequenceStrings.last())
    }

    /**
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    void "ref-seq only projection for a baseline on 11.4 individually"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.4"]
        JSONArray featuresArray = new JSONArray()
        File file = new File(bigwigForagerFile)
        Path path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)

        when: "we get the projected track data "
        bigwigService.processSequence(featuresArray, sequenceStrings.first(), bigWigFileReader, -25001, 0)

        then: "we expect to get sane results"
        assert featuresArray.size() == 275

        when: "we get the projected track data "
        featuresArray = new JSONArray()
        bigwigService.processSequence(featuresArray, sequenceStrings.first(), bigWigFileReader, 49999, 75000)

        then: "we expect to get sane results"
        assert featuresArray.size() == 226
    }

    /**
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    void "un-projected 11.4 individually"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.4"]
        JSONArray featuresArray = new JSONArray()
        String refererLoc = createReferLoc(sequenceStrings, 0, "None")
        File file = new File(bigwigForagerFile)
        Path path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)

        when: "we get the projected track data "
        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, Organism.first())
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, -50001, 0)

        then: "we expect to get sane results"
        assert featuresArray.size() == 0

        when: "we get the projected track data "
        featuresArray = new JSONArray()
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, 49999, 100000)

        then: "we expect to get sane results"
        assert featuresArray.size() == 167
    }

    private String createReferLoc(ArrayList<String> sequenceStrings, int padding, String projectionType) {
        String sequenceListString = ""
        sequenceStrings.eachWithIndex { it, index ->
            sequenceListString += "{\"name\":\"${it}\"}"
            if (index < sequenceStrings.size() - 1) {
                sequenceListString += ","
            }

        }
        return "{\"padding\":${padding}, \"projection\":\"${projectionType}\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[${sequenceListString}], \"label\":\"${sequenceStrings.join("::")}\"}:-1..-1:1..16607"
    }

/**
 *  GroupUn87
 */
    void "un-projected Un87 individually"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["GroupUn87"]
        String refererLoc = createReferLoc(sequenceStrings, 0, "None")
        JSONArray featuresArray = new JSONArray()
        File file = new File(bigwigForagerFile)
        Path path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)

        when: "we get the projected track data "
        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, Organism.first())
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, -50001, 0)

        then: "we expect to get sane results"
        assert featuresArray.size() == 0

        when: "we get the projected track data "
        featuresArray = new JSONArray()
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, 0, 49999)

        then: "we expect to get sane results"
        assert featuresArray.size() == 200

        when: "now on the outside of the next one"
        featuresArray = new JSONArray()
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, 49999, 100000)

        then: "we expect to get sane results"
        assert featuresArray.size() == 3
    }

    /**
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    @Ignore
    // ignoring the Exon type of projection
    void "Projected 11.4 individually"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.4"]
        JSONArray featuresArray = new JSONArray()
        String refererLoc = createReferLoc(sequenceStrings, 0, "Exon")
        File file = new File(bigwigForagerFile)
        Path path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)

        when: "we get the projected track data "
        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, Organism.first())
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, -50001, 0)

        then: "we expect to get sane results"
        assert featuresArray.size() == 502

        when: "we get the projected track data "
        featuresArray = new JSONArray()
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, -1, 50000)

        then: "we expect to get sane results"
        assert featuresArray.size() == 507
    }

    /**
     *  GroupUn87
     */
    @Ignore
    // ignoring the Exon type of projection
    void "Projected Un87 individually"() {
        given: "proper inputs"
        List<String> sequenceStrings = ["GroupUn87"]
        JSONArray featuresArray = new JSONArray()
        String refererLoc = createReferLoc(sequenceStrings, 0, "Exon")
        File file = new File(bigwigForagerFile)
        Path path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)

        when: "we get the projected track data "
        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, Organism.first())
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, -50001, 0)

        then: "we expect to get sane results"
        assert featuresArray.size() == 504

        when: "we get the projected track data "
        featuresArray = new JSONArray()
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, -1, 50000)

        then: "we expect to get sane results"
        assert featuresArray.size() == 833
    }

    /**
     *  GroupUn87: Projected: 0,213 <-> 723,843   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    @Ignore
    void "non-projection of contiguous tracks should work"() {

        given: "proper inputs"
        List<String> sequenceStrings = ["GroupUn87", "Group11.4"]
        JSONArray featuresArray = new JSONArray()
        String refererLoc = createReferLoc(sequenceStrings, 0, "None")
        File file = new File(bigwigForagerFile)
        Path path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)

        when: "we get the projected track data "
        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, Organism.first())
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, -50001, 0)

        then: "we expect to get sane results"
        assert featuresArray.size() == 501

        when: "we get the projected track data "
        featuresArray = new JSONArray()
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, 49999, 100000)

        then: "we expect to get sane results"
        assert featuresArray.size() == 501
    }

    /**
     *
     *  GroupUn87: Projected: 0,213 <-> 723,843   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *  Group11.4: Projected: 0,2546 <-> 14601,15764  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    @Ignore
    void "exon projections of contiguous tracks should work"() {

        given: "proper inputs"
        List<String> sequenceStrings = ["GroupUn87", "Group11.4"]
        JSONArray featuresArray = new JSONArray()
        String refererLoc = createReferLoc(sequenceStrings, 0, "Exon")
        File file = new File(bigwigForagerFile)
        Path path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)

        when: "we get the projected track data "
        MultiSequenceProjection projection = projectionService.getProjection(refererLoc, Organism.first())
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, -50001, 0)

        then: "we expect to get sane results"
        assert featuresArray.size() == 501

        when: "we get the projected track data "
        featuresArray = new JSONArray()
        bigwigService.processProjection(featuresArray, projection, bigWigFileReader, 149999, 200000)

        then: "we expect to get sane results"
        assert featuresArray.size() == 501
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
    @Ignore
    void "chunking / chunking projection"() {

        given: "proper inputs"
        List<String> sequenceStrings = ["Group11.6", "Group1.10"]
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceStrings[0]}\"},{\"name\":\"${sequenceStrings[1]}\"}], \"label\":\"${sequenceStrings.join('::')}\"}:-1..-1:1..16607"
        JSONArray array

        when: "we get the projected track data "
//        JSONObject trackObject = bigwigService.processProjection(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
    }

    void "test volvox sine"() {
        given: "input volvox sine"
        def sineWaveUnProjected = new JSONArray()
        def sineWaveProjected = new JSONArray()
        def sineWaveProjectedReversed = new JSONArray()
        File file
        Path path
        String sequenceName = "ctgA"
        String organismName = "Volvox"
        int start = 0
        int end = 200

        when: "we read the volvox as a regular sine wave as a sequence"
        file = new File(bigwigVolvoxSineFile)
        path = FileSystems.getDefault().getPath(file.absolutePath)
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)
        sineWaveUnProjected = bigwigService.processSequence(sineWaveUnProjected, sequenceName, bigWigFileReader, start, end)

        then: "we get expected results"
        assert sineWaveUnProjected.length() > 0


        when: "we read the volvox within a projection"
        MultiSequenceProjection projection = new MultiSequenceProjection()
        ProjectionSequence projectionSequence = new ProjectionSequence(
                start: start,
                end: end,
                name: sequenceName,
                organism: organismName,
                order: 0
        )
        projection.addProjectionSequences([projectionSequence])
        projection.addInterval(start, end, projectionSequence)

        bigWigFileReader = new BigWigFileReader(path)
        sineWaveProjected = bigwigService.processProjection(sineWaveProjected, projection, bigWigFileReader, start, end)
        int last = sineWaveProjected.size()-1

        then: "we should see identical results as before"
        assert sineWaveProjected.length() == sineWaveUnProjected.length()
        assert sineWaveProjected[0].start == sineWaveUnProjected[0].start
        assert sineWaveProjected[0].end == sineWaveUnProjected[0].end
        assert sineWaveProjected[0].score == sineWaveUnProjected[0].score
        assert sineWaveProjected[1].start == sineWaveUnProjected[1].start
        assert sineWaveProjected[1].end == sineWaveUnProjected[1].end
        assert sineWaveProjected[1].score == sineWaveUnProjected[1].score
        assert sineWaveProjected[last].start == sineWaveUnProjected[last].start
        assert sineWaveProjected[last].end == sineWaveUnProjected[last].end
        assert sineWaveProjected[last].score == sineWaveUnProjected[last].score

        assert sineWaveProjected == sineWaveUnProjected

        when: "we read the volvox within a reverse projection"
        projectionSequence.reverse = true

        sineWaveProjectedReversed = bigwigService.processProjection(sineWaveProjectedReversed, projection, bigWigFileReader, start, end)
        last = sineWaveProjectedReversed.size()-1
        println sineWaveProjectedReversed as JSON

        then: "we should see identical results as before"
        assert sineWaveProjected.length() == sineWaveProjectedReversed.length()

    }
}
