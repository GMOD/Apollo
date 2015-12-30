package org.bbop.apollo


/**
 */
class RefSeqProjectorServiceIntegrationSpec extends AbstractIntegrationSpec {

    def refSeqProjectorService
    def projectionService

    String un87StartSequence = "ATGCACTGTCAACGTACACGGG" // starts at 0
    String un87EndSequence = "AAAACATAA" // starts at 0
    String elevenFourStartSequence = "ATGTTTGCTTGGGGAACTTGTGTTCTCTATGGATGGAGGTTAAA"
    String elevenFourEndSequence = "AAGGTTACGTTTATATCATTCGAATAATATAAC" // last projected from OGS
    Integer un87Length = 838
    Integer elevenFourLength = 15734

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
//        assert returnedSequence.length()==20000 + 1 // chunk is inclusive 0 to 20000
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
        assert returnedSequence.length()==un87Length
        assert returnedSequence.startsWith(un87StartSequence)
        assert returnedSequence.endsWith(un87EndSequence)
    }

    void "get OTHER projected single"() {
        given:
        String sequenceName = "Group11.4"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/155/73d/94/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName}\"}], \"label\":\"${sequenceName}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())

        then:
        assert returnedSequence.startsWith(elevenFourStartSequence)
        assert returnedSequence.endsWith(elevenFourEndSequence)
        assert returnedSequence.length()==elevenFourLength
    }


    void "get projected contiguous"() {
        given:
        String sequenceName1 = "GroupUn87"  // 78,258 unprojected . . . projected: 9966  -> 45575 (4 projections, length ~800)
        String sequenceName2 = "Group11.4"  // 75,085 unprojected . . . projected: 10257 -> 64197 (31 projections, length ~15K)
        Integer elevenFourStartSequenceIndex = un87Length
        Integer elevenFourEndSequenceIndex = un87Length + elevenFourLength - elevenFourEndSequence.length()
        // total input should 78258 + 75085 = 153343
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/aa2/286/99/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName1}\"},{\"name\":\"${sequenceName2}\"}], \"label\":\"${sequenceName1}::${sequenceName2}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())

        then:
        assert un87Length + elevenFourLength ==returnedSequence.length()
        assert returnedSequence.indexOf(un87StartSequence)==0
        assert returnedSequence.indexOf(un87EndSequence)==un87Length-un87EndSequence.length()
        assert returnedSequence.split(un87EndSequence).length==2

        assert returnedSequence.indexOf(elevenFourStartSequence)==elevenFourStartSequenceIndex
        assert returnedSequence.indexOf(elevenFourEndSequence)==elevenFourEndSequenceIndex
        assert returnedSequence.split(elevenFourStartSequence).length==2
        assert un87Length+elevenFourLength==returnedSequence.length()
    }

    void "get projected contiguous - reverse"() {
        given:
        String sequenceName1 = "Group11.4"  // 78K unprojected . . . projected: 9966  -> 45575 (4 projections, length ~800)
        String sequenceName2 = "GroupUn87"  // 75K unprojected . . . projected: 10257 -> 64197 (31 projections, length ~15K)
        // total input should 78258 + 75085 = 153343
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/aa2/286/99/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName1}\"},{\"name\":\"${sequenceName2}\"}], \"label\":\"${sequenceName1}::${sequenceName2}\"}:-1..-1-${chunkNumber}.txt"

//        Integer elevenFourLength = 15764
//        Integer un87Length = 843
//        Integer elevenFourLength = 15734 + 31
//        Integer un87Length = 838 + 6
//        Integer elevenFourLength = 15734 // + 31
//        Integer un87Length = 838 //+ 6
//        String un87StartSequence = "ATGCACTGTCAACGTACACGGG" // starts at 0
//        String un87EndSequence = "AAAACATAA" // starts at 0
//        String elevenFourStartSequence = "ATGTTTGCTTGGGGAACTTGTGTTCTCTATGGATGGAGGTTAAA"
//        String elevenFourEndSequence = "AAGGTTACGTTTATATCATTCGAATAATATAAC" // last projected from OGS

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())

        then:
//        assert 843+15764+1==returnedSequence.length()
        assert un87Length+elevenFourLength==returnedSequence.length()


        assert returnedSequence.indexOf(elevenFourStartSequence)==0
        assert returnedSequence.indexOf(elevenFourEndSequence)==elevenFourLength-elevenFourEndSequence.length()
        assert returnedSequence.split(elevenFourEndSequence).length==2

        assert returnedSequence.indexOf(un87StartSequence)==elevenFourLength
        assert returnedSequence.indexOf(un87EndSequence)==elevenFourLength+un87Length-un87EndSequence.length()
        assert returnedSequence.endsWith(un87EndSequence)

        assert un87Length+elevenFourLength==returnedSequence.length()
    }

    // TODO: add this test for verification
//    void "get unprojected contiguous - three sequences"() {
//        given:
//        String sequenceName1 = "GroupUn87"  // 78K unprojected . . . projected: 9966  -> 45575 (4 projections, length ~800)
//        String sequenceName2 = "Group11.4"  // 75K unprojected . . . projected: 10257 -> 64197 (31 projections, length ~15K)
//
//        String un87StartSequence = "ATGCACTGTCAACGTACACGGG" // starts at 0
//        String un87EndSequence = "AAAACATAA" // starts at 0
//        Integer un87Length = 843
//        Integer elevenFourLength = 15764
//        String elevenFourStartSequence = "ATGTTTGCTTGGGGAACTTGTG"
//        String elevenFourEndSequence = "AGTAAGCTTATTATATTG"
//        Integer elevenFourStartSequenceIndex = un87Length +1
//        Integer elevenFourEndSequenceIndex = un87Length + elevenFourLength
//        // total input should 78258 + 75085 = 153343
//        Integer chunkNumber = 0
//        String dataFileName = "${Organism.first().directory}/seq/aa2/286/99/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName1}\"},{\"name\":\"${sequenceName2}\"}], \"label\":\"${sequenceName1}::${sequenceName2}\"}:-1..-1-${chunkNumber}.txt"
//
//        when:
//        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())
//
//        then:
//        assert returnedSequence.indexOf(un87StartSequence)==0
//        assert returnedSequence.indexOf(un87EndSequence)==un87Length-un87EndSequence.length()
//        assert returnedSequence.split(un87EndSequence).length==2
//
//        assert returnedSequence.indexOf(elevenFourStartSequence)==elevenFourStartSequenceIndex
//        assert returnedSequence.indexOf(elevenFourEndSequence)==elevenFourEndSequenceIndex
//        assert returnedSequence.split(elevenFourStartSequence).length==2
//        assert un87Length+elevenFourLength==returnedSequence.length()
//    }

    // TODO: add all 3
//    void "get projected contiguous - three sequences"() {
//        given:
//        String sequenceName1 = "GroupUn87"  // 78K unprojected . . . projected: 9966  -> 45575 (4 projections, length ~800)
//        String sequenceName2 = "Group11.4"  // 75K unprojected . . . projected: 10257 -> 64197 (31 projections, length ~15K)
//        String sequenceName3 = "Group1.1"  //
//
////        String un87StartSequence = "ATGCACTGTCAACGTACACGGG" // starts at 0
////        String un87EndSequence = "AAAACATAA" // starts at 0
////        String elevenFourStartSequence = "ATGTTTGCTTGGGGAACTTGTG"
////        String elevenFourEndSequence = "AGTAAGCTTATTATATTG"
//        Integer elevenFourStartSequenceIndex = un87Length
//        Integer elevenFourEndSequenceIndex = un87Length + elevenFourLength - elevenFourEndSequence.length()
//        // total input should 78258 + 75085 = 153343
//        Integer chunkNumber = 0
//        String dataFileName = "${Organism.first().directory}/seq/aa2/286/99/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName1}\"},{\"name\":\"${sequenceName2}\"},{\"name\":\"${sequenceName3}\"}], \"label\":\"${sequenceName1}::${sequenceName2}::${sequenceName3}\"}:-1..-1-${chunkNumber}.txt"
//
//        when:
//        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName,Organism.first())
//
//        then:
//        assert un87Length + elevenFourLength ==returnedSequence.length()
//        assert returnedSequence.indexOf(un87StartSequence)==0
//        assert returnedSequence.indexOf(un87EndSequence)==un87Length-un87EndSequence.length()
//        assert returnedSequence.split(un87EndSequence).length==2
//
//        assert returnedSequence.indexOf(elevenFourStartSequence)==elevenFourStartSequenceIndex
//        assert returnedSequence.indexOf(elevenFourEndSequence)==elevenFourEndSequenceIndex
//        assert returnedSequence.split(elevenFourStartSequence).length==2
//        assert un87Length+elevenFourLength==returnedSequence.length()
//    }
}
