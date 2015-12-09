package org.bbop.apollo

/**
 */
class RefSeqProjectorServiceIntegrationSpec extends AbstractIntegrationSpec {

    def refSeqProjectorService
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

    void "get unprojected single"() {
        given:
        String sequenceName = "GroupUn87"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/155/73d/94/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName}\"}], \"label\":\"${sequenceName}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())

        then:
        assert returnedSequence.length()==20000
//        assert returnedSequence.split("ATGAAAGGTAAGTGAATATCAATAT").length==2
        assert returnedSequence.split("ATGCACTGTC").length==2
    }

    void "get OTHER unprojected single"() {
        given:
        String sequenceName = "Group11.4"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/155/73d/94/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName}\"}], \"label\":\"${sequenceName}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())

        then:
        assert returnedSequence.length()==20000
        assert returnedSequence.split("ATGTTTGCTTGGG").length==2
        // original is 75K . . one chunk of that
    }

    void "get unprojected contiguous"() {
        given:
        String sequenceName1 = "GroupUn87"
        String sequenceName2 = "Group11.4"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/428/d69/30/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName1}\"},{\"name\":\"${sequenceName2}\"}], \"label\":\"${sequenceName1}::${sequenceName2}\"}:-1..-1-${chunkNumber}.txt"

        when: "we get the first set of confirmed sequence"
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())

        then:
        assert returnedSequence.split("ATGCACTGTC").length==2
        assert returnedSequence.length()==20000

        when: "we get the next chunk"
        chunkNumber = 1
        dataFileName = "${Organism.first().directory}/seq/428/d69/30/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName1}\"},{\"name\":\"${sequenceName2}\"}], \"label\":\"${sequenceName1}::${sequenceName2}\"}:-1..-1-${chunkNumber}.txt"
        returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())

        then: "we should get the next set of confirmed sequence"
        assert returnedSequence.split("ATGAAAGGTAAGTGAATATCAATAT").length==2
        assert returnedSequence.length()==20000

    }

    void "get projected single"() {
        given:
        String sequenceName = "GroupUn87"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/155/73d/94/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName}\"}], \"label\":\"${sequenceName}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())

        then:
//        assert returnedSequence.split("ATGAAAGGTGAC").length==2
        assert returnedSequence.length()==843
        assert returnedSequence.indexOf("ATGCACTGTC")==0
        // confirmed in other one
        // original is 78K . . one chunk of that
    }

    void "get OTHER projected single"() {
        given:
        String sequenceName = "Group11.4"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/155/73d/94/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName}\"}], \"label\":\"${sequenceName}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())

        then:
//        assert returnedSequence.split("ATGAAAGGTGAC").length==2
        assert returnedSequence.length()==15764
        assert returnedSequence.indexOf("ATGTTTGCTTGGG")==0
        // confirmed in other one
        // original is 78K . . one chunk of that
    }


    void "get projected contiguous"() {
        given:
        String sequenceName1 = "GroupUn87"
        String sequenceName2 = "Group11.4"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/aa2/286/99/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName1}\"},{\"name\":\"${sequenceName2}\"}], \"label\":\"${sequenceName1}::${sequenceName2}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())

        then:
        assert returnedSequence.split("ATGAAAG").length==2
        assert returnedSequence.length()==19999
    }
}
