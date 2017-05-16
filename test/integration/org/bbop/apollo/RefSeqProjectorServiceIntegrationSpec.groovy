package org.bbop.apollo

import htsjdk.samtools.util.SequenceUtil
import spock.lang.Ignore
import spock.lang.IgnoreRest

//import grails.converters.JSON
//import org.codehaus.groovy.grails.web.json.JSONObject
/**
 */
class RefSeqProjectorServiceIntegrationSpec extends AbstractIntegrationSpec {

    def refSeqProjectorService
//    def requestHandlingService

    String un87StartSequence = "ATGCACTGTCAACGTACACGGG" // starts at 0
    String un87EndSequence = "AAAACATAA" // starts at 0
    String elevenFourStartSequence = "ATGTTTGCTTGGGGAACTTGTGTTCTCTATGGATGGAGGTTAAA"
    String elevenFourEndSequence = "AAGGTTACGTTTATATCATTCGAATAATATAAC" // last projected from OGS
    Integer un87Length = 838
    Integer elevenFourLength = 15734

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
                length: 78258
                , seqChunkSize: 20000
                , start: 0
                , end: 78258
                , organism: organism
                , name: "GroupUn87"
        ).save(failOnError: true)
    }

    def cleanup() {
    }

    void "get unprojected single"() {
        given:
        String sequenceName = "GroupUn87"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/155/73d/94/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName}\"}], \"label\":\"${sequenceName}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName, Organism.first())

        then:
        assert returnedSequence.length() == 20000
//        assert returnedSequence.split("ATGAAAGGTAAGTGAATATCAATAT").length==2
        assert returnedSequence.split("ATGCACTGTC").length == 2
    }

    void "get OTHER unprojected single"() {
        given:
        String sequenceName = "Group11.4"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/155/73d/94/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName}\"}], \"label\":\"${sequenceName}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName, Organism.first())

        then:
        assert returnedSequence.length() == 20000
//        assert returnedSequence.length()==20000 + 1 // chunk is inclusive 0 to 20000
        assert returnedSequence.split("ATGTTTGCTTGGG").length == 2
        // original is 75K . . one chunk of that
    }

    void "get unprojected contiguous"() {
        given:
        String sequenceName1 = "GroupUn87"
        String sequenceName2 = "Group11.4"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/428/d69/30/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName1}\"},{\"name\":\"${sequenceName2}\"}], \"label\":\"${sequenceName1}::${sequenceName2}\"}:-1..-1-${chunkNumber}.txt"

        when: "we get the first set of confirmed sequence"
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName, Organism.first())

        then:
        assert returnedSequence.split("ATGCACTGTC").length == 2
        assert returnedSequence.length() == 20000

        when: "we get the next chunk"
        chunkNumber = 1
        dataFileName = "${Organism.first().directory}/seq/428/d69/30/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName1}\"},{\"name\":\"${sequenceName2}\"}], \"label\":\"${sequenceName1}::${sequenceName2}\"}:-1..-1-${chunkNumber}.txt"
        returnedSequence = refSeqProjectorService.projectSequence(dataFileName, Organism.first())

        then: "we should get the next set of confirmed sequence"
        assert returnedSequence.split("ATGAAAGGTAAGTGAATATCAATAT").length == 2
        assert returnedSequence.length() == 20000

    }

    @Ignore
    // ignoring exon projection type
    void "get projected single"() {
        given:
        String sequenceName = "GroupUn87"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/155/73d/94/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName}\"}], \"label\":\"${sequenceName}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName, Organism.first())

        then:
        assert returnedSequence.length() == un87Length
        assert returnedSequence.startsWith(un87StartSequence)
        assert returnedSequence.endsWith(un87EndSequence)
    }

    @Ignore
    // ignoring exon projection type
    void "get OTHER projected single"() {
        given:
        String sequenceName = "Group11.4"
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/155/73d/94/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName}\"}], \"label\":\"${sequenceName}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName, Organism.first())

        then:
        assert returnedSequence.startsWith(elevenFourStartSequence)
        assert returnedSequence.endsWith(elevenFourEndSequence)
        assert returnedSequence.length() == elevenFourLength
    }


    @Ignore
    // ignoring exon projection type
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
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName, Organism.first())

        then:
        assert un87Length + elevenFourLength == returnedSequence.length()
        assert returnedSequence.indexOf(un87StartSequence) == 0
        assert returnedSequence.indexOf(un87EndSequence) == un87Length - un87EndSequence.length()
        assert returnedSequence.split(un87EndSequence).length == 2

        assert returnedSequence.indexOf(elevenFourStartSequence) == elevenFourStartSequenceIndex
        assert returnedSequence.indexOf(elevenFourEndSequence) == elevenFourEndSequenceIndex
        assert returnedSequence.split(elevenFourStartSequence).length == 2
        assert un87Length + elevenFourLength == returnedSequence.length()
    }

    @Ignore
    // ignoring exon projection type
    void "get projected contiguous - reverse"() {
        given:
        String sequenceName1 = "Group11.4"  // 78K unprojected . . . projected: 9966  -> 45575 (4 projections, length ~800)
        String sequenceName2 = "GroupUn87"  // 75K unprojected . . . projected: 10257 -> 64197 (31 projections, length ~15K)
        // total input should 78258 + 75085 = 153343
        Integer chunkNumber = 0
        String dataFileName = "${Organism.first().directory}/seq/aa2/286/99/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceName1}\"},{\"name\":\"${sequenceName2}\"}], \"label\":\"${sequenceName1}::${sequenceName2}\"}:-1..-1-${chunkNumber}.txt"

        when:
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName, Organism.first())

        then:
//        assert 843+15764+1==returnedSequence.length()
        assert un87Length + elevenFourLength == returnedSequence.length()


        assert returnedSequence.indexOf(elevenFourStartSequence) == 0
        assert returnedSequence.indexOf(elevenFourEndSequence) == elevenFourLength - elevenFourEndSequence.length()
        assert returnedSequence.split(elevenFourEndSequence).length == 2

        assert returnedSequence.indexOf(un87StartSequence) == elevenFourLength
        assert returnedSequence.indexOf(un87EndSequence) == elevenFourLength + un87Length - un87EndSequence.length()
        assert returnedSequence.endsWith(un87EndSequence)

        assert un87Length + elevenFourLength == returnedSequence.length()
    }

    void "projected single sequence"() {

        given: "the two unprojected groups"
        Integer start = 10057
        Integer end = 18796
        String dataFileName = URLDecoder.decode("${Organism.first().directory}/seq/220/ca9/1c/%7B%22name%22:%22GB52238-RA%20(Group11.4)%22,%20%22padding%22:0,%20%22start%22:10257,%20%22end%22:18596,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:10057,%20%22end%22:18796,%20%22feature%22:%7B%22name%22:%22GB52238-RA%22%7D%7D]%7D:10257..18596-0.txt")
        String sequence10086 = "ATCTTCTCA"
        String sequence18286 = "GGAAGTGGCAC"


        when: "retrieving "
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName, Organism.first())
        Integer returnedIndex = returnedSequence.indexOf(sequence10086) // should be at 10,087


        then: "found first one"
        assert returnedSequence.split(sequence10086).length == 2
        assert returnedIndex == 10086 - start - 1 // projected - start

        when: "retrieving "
        returnedIndex = returnedSequence.indexOf(sequence18286) // should be at 10,087

        then: "found second one"
        assert returnedSequence.split(sequence18286).length == 2
        assert returnedIndex == 18286 - start - 1 // projected - start // 8229

    }

    void "projected contiguous sequence"() {

        given: "the two unprojected groups"
        Integer start = 45455
        Integer end = 64171
        String dataFileName = URLDecoder.decode("${Organism.first().directory}/seq/d0d/e13/35/%7B%22name%22:%22GB53499-RA%20(GroupUn87)::GB52238-RA%20(Group11.4)%22,%20%22padding%22:0,%20%22start%22:45455,%20%22end%22:64171,%20%22sequenceList%22:[%7B%22name%22:%22GroupUn87%22,%20%22start%22:45255,%20%22end%22:45775,%20%22feature%22:%7B%22name%22:%22GB53499-RA%22%7D%7D,%7B%22name%22:%22Group11.4%22,%20%22start%22:10057,%20%22end%22:18796,%20%22feature%22:%7B%22name%22:%22GB52238-RA%22%7D%7D]%7D:45455..64171-0.txt")
        String sequence10086 = "ATCTTCTCA"
        String sequence18286 = "GGAAGTGGCAC"


        when: "retrieving "
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName, Organism.first())
        Integer returnedIndex = returnedSequence.indexOf(sequence10086) // should be at 10,087


        then: "found first one"
        assert returnedSequence.split(sequence10086).length == 2
        assert returnedIndex == 549 - 1 // projected - start // 549

        when: "retrieving "
        returnedIndex = returnedSequence.indexOf(sequence18286) // should be at 10,087

        then: "found second one"
        assert returnedSequence.split(sequence18286).length == 2
        assert returnedIndex == 8749 - 1 // projected - start // 8749

    }

    void "get unprojected contiguous sequence"() {

        given: "the two unprojected groups"
        String dataFileName = URLDecoder.decode("${Organism.first().directory}/seq/e62/08c/1e/%7B%22name%22:%22GroupUn87::Group11.4%22,%20%22padding%22:0,%20%22start%22:0,%20%22end%22:153343,%20%22sequenceList%22:[%7B%22name%22:%22GroupUn87%22,%20%22start%22:0,%20%22end%22:78258,%22reverse%22:false%7D,%7B%22name%22:%22Group11.4%22,%20%22start%22:0,%20%22end%22:75085,%22reverse%22:false%7D]%7D:0..153343-4.txt", "UTF-8")
        String sequence10086 = "ATCTTCTCA"
        String sequence18286 = "GGAAGTGGCAC"


        when: "retrieving "
        String returnedSequence = refSeqProjectorService.projectSequence(dataFileName, Organism.first())
        Integer returnedIndex = returnedSequence.indexOf(sequence10086) // should be at 10,087


        then: "found first one"
        assert returnedSequence.split(sequence10086).length == 2
        assert returnedIndex == 8344 - 1 // projected - start // 8343
        assert returnedSequence.size() == 20000

        when: "retrieving "
        returnedIndex = returnedSequence.indexOf(sequence18286) // should be at 10,087

        then: "found second one"
        assert returnedSequence.split(sequence18286).length == 2
        assert returnedIndex == 16544 - 1 // projected - start // 8749

    }

    void "get unprojected sequence and reverse sequence"() {

        given: "a single unprojected sequence for 11.4"
        Boolean reverse = false
        Integer chunk = 0
        String sequenceTemplate = URLDecoder.decode("${Organism.first().directory}/seq/f60/9c7/ee/%7B%22description%22:%22Group11.4%22,%20%22padding%22:0,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:0,%20%22end%22:75085,%22reverse%22:@REVERSE@%7D]%7D:-1..-1-@CHUNK@.txt", "UTF-8")
        String fivePrimeSequenceStart = "TGAGAAATAAATATTGAATTGTATTATAACATTAATAATGTAATTAAGTTTTATTTTTGCAA"
        String threePrimeSequenceEnd = "ACCAATTTTATCTGAAACAACTTTTCTTATCATCAACATGCAATATTCCTATTATCAAGTGACATATTCAAAGTGGTCAAGTTATTTTTGT"


        when: "we get the sequence we confirm that the first is 11.4"
        String returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE@", reverse.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "affirm that the start starts with the start and the end ends with the end"
        assert 0 == returnedSequence.indexOf(fivePrimeSequenceStart)

        when: "we go to the end of the sequence"
        reverse = false
        chunk = 3
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE@", reverse.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should get the proper end sequence"
        assert returnedSequence.length() - threePrimeSequenceEnd.length() == returnedSequence.indexOf(threePrimeSequenceEnd)

        when: "we reverse the sequence we should see the opposite start"
        reverse = true
        chunk = 0
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE@", reverse.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should get the proper end sequence"
        assert 0 == returnedSequence.indexOf(threePrimeSequenceEnd.reverse())

        when: "we reverse the sequence we should see the opposite end"
        reverse = true
        chunk = 3
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE@", reverse.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the start at the other end"
        assert returnedSequence.length() - fivePrimeSequenceStart.length() == returnedSequence.indexOf(fivePrimeSequenceStart.reverse())

    }

//    @IgnoreRest
    void "get unprojected contiguous sequence and reverse sequence for 11.4 and Un87 contiguously"() {

        given: "a single unprojected sequence for 11.4 and Un87"
        Boolean reverse11_4 = false
        Boolean reverseUn87 = false
        Integer chunk = 0
        Integer length11_4 = 75085 // Un87 starts at 75086
        Integer lengthUn87 = 78258 // Total length is 153343
        Integer chunkSize = 20000
        Integer lastChunk = Math.ceil((length11_4 + lengthUn87) / chunkSize) - 1
        Integer chunk3Length = length11_4 - (chunkSize * 3) // this is the chunk offset

        // 11.4 and then Un87
        String sequenceTemplate = URLDecoder.decode("${Organism.first().directory}/seq/051/49c/cb/%7B%22description%22:%22Group11.4::GroupUn87%22,%20%22padding%22:0,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:0,%20%22end%22:75085,%20%22reverse%22:@REVERSE1@%7D,%7B%22name%22:%22GroupUn87%22,%20%22start%22:0,%20%22end%22:78258,%20%22reverse%22:@REVERSE2@%7D]%7D:-1..-1-@CHUNK@.txt", "UTF-8")
        String fivePrimeSequenceStart11_4 = "TGAGAAATAAATATTGAATTGTATTATAACATTAATAATGTAATTAAGTTTTATTTTTGCAA"
        String threePrimeSequenceEnd11_4 = "ACCAATTTTATCTGAAACAACTTTTCTTATCATCAACATGCAATATTCCTATTATCAAGTGACATATTCAAAGTGGTCAAGTTATTTTTGT"
        String fivePrimeSequenceStartUn87 = "TCAATTTCGTCGTAAATGTTGGTATAATTTGTGATCTTTTCTTATTAGAAATAGATAACACACAAAAACATATATATG"
        String threePrimeSequenceEndUn87 = "TATATAGTACTTTATAATTCCTAAGATCAAGTTCCTCGCAGATTTTTAATTGAATTTATATGTAAAAATTGCAAAGAAAGCTATAAAACTTGTATGTAACCGGATAGCAAATAT"


        when: "we get the sequence we confirm that the first is 11.4"
        String returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "affirm that the start starts with the start and the end ends with the end"
        assert 0 == returnedSequence.indexOf(fivePrimeSequenceStart11_4)

        when: "we go to the end of the first sequence"
        reverse11_4 = false
        reverseUn87 = false
        chunk = 3
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should get the proper end sequence for 11.4 and start sequence for un87"
        assert chunk3Length - threePrimeSequenceEnd11_4.length() == returnedSequence.indexOf(threePrimeSequenceEnd11_4)
        assert chunk3Length == returnedSequence.indexOf(fivePrimeSequenceStartUn87)

        when: "we go to the end of the last sequence"
        chunk = lastChunk // this is the last chunk
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should get the proper end sequence for Un87"
        assert length11_4 + lengthUn87 - threePrimeSequenceEndUn87.length() - (chunkSize * lastChunk) == returnedSequence.indexOf(threePrimeSequenceEndUn87)


        when: "we reverse both sequences we should see the opposite start"
        reverse11_4 = true
        reverseUn87 = true
        chunk = 0
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should get the proper 11.4 end sequence as above"
        assert 0 == returnedSequence.indexOf(threePrimeSequenceEnd11_4.reverse())

        when: "we reverse the sequence we should see the opposite start of 11.4 and the end of Un87"
        chunk = 3
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the start at the other end"
//        assert length11_4-fivePrimeSequenceStart11_4.length()==returnedSequence.indexOf(fivePrimeSequenceStart11_4.reverse())
        assert length11_4 - (chunk * chunkSize) - fivePrimeSequenceStart11_4.length() == returnedSequence.indexOf(fivePrimeSequenceStart11_4.reverse())
        assert length11_4 - (chunk * chunkSize) == returnedSequence.indexOf(threePrimeSequenceEndUn87.reverse())
        // TODO: has to match the first of one and the last of another

        when: "we reverse the sequence we should and go to the end, we should see the reverse start of Un87"
        chunk = lastChunk
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the reverse the same"
        // it is at the end of this block
        assert length11_4 + lengthUn87 - ((chunk) * chunkSize) - fivePrimeSequenceStartUn87.length() == returnedSequence.indexOf(fivePrimeSequenceStartUn87.reverse())

        when: "we reverse only the second sequence we should see the opposite end"
        // if 11.4 is false and Un87 is true
        reverse11_4 = false
        reverseUn87 = true
        chunk = 0
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should still see the appropriate set"
        assert 0 == returnedSequence.indexOf(fivePrimeSequenceStart11_4)

        when: "we look at the next chunk"
        chunk = 3
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should still see the appropriate the proper boundary here"
        assert chunk3Length - threePrimeSequenceEnd11_4.length() == returnedSequence.indexOf(threePrimeSequenceEnd11_4)
        assert length11_4 - (chunk * chunkSize) == returnedSequence.indexOf(threePrimeSequenceEndUn87.reverse())

        when: "we look at the last chunk"
        chunk = lastChunk
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should still see the appropriate the proper boundary here"
        assert length11_4 + lengthUn87 - ((chunk) * chunkSize) - fivePrimeSequenceStartUn87.length() == returnedSequence.indexOf(fivePrimeSequenceStartUn87.reverse())

        when: "we reverse only the first sequence we should see the opposite end"
        // if 11.4 is false and Un87 is true
        reverse11_4 = true
        reverseUn87 = false
        chunk = 0
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should still see the appropriate set"
        assert 0 == returnedSequence.indexOf(threePrimeSequenceEnd11_4.reverse())

        when: "we look at the next chunk"
        chunk = 3
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should still see the appropriate the proper boundary here"
        assert length11_4 - (chunk * chunkSize) - fivePrimeSequenceStart11_4.length() == returnedSequence.indexOf(fivePrimeSequenceStart11_4.reverse())
        assert chunk3Length == returnedSequence.indexOf(fivePrimeSequenceStartUn87)

        when: "we look at the last chunk"
        chunk = 7
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should still see the appropriate the proper boundary here"
        assert length11_4 + lengthUn87 - threePrimeSequenceEndUn87.length() - (chunkSize * lastChunk) == returnedSequence.indexOf(threePrimeSequenceEndUn87)
    }

    void "get the PROJECTED SINGLE sequence and reverse sequence for ONE feature of 11.4"() {

        given: "gene GB52236-RA on 11.4"
        // got these off of the feature, so doing this here
        String gb52236_fivePrime = SequenceUtil.reverseComplement("TAAAGATATCTTCCTTAATGGCGGCTTCGTCGCCTCATTCAAAGTATTATCACTCTTATTATTTAATGAT" +
                "AATTTCTCATCGTATAGTATCATATATGCAAGATGGCGGAACAGAGCCAACAAGATTTTAATAGAAGTGC" +
                "GTTGACAGTTCGATATGGACGGGTAATTAACATATACTTTTATTAATATATGTTTAAAGTGTTATTTTAG")

        String gb52236_threePrime = SequenceUtil.reverseComplement("TAAATGAAAGTGTGACCCCATACTGGTGAACCACCAGATCTGCGTACTACTCCAGTCTTTTGTTTTCCTG" +
                "AGCGACCTTTGTCAGGTAATAAGTAACTGAAAGTAATACAAATTAATATAAATTTATCTTTTTTCTTTTA" +
                "TTTCAATTATGATTAGATAATAATAAATAATAAAGATGTGTAAATTTTAGTAGTCTTATTAATCATTCAT" +
                "ATCATAAATAAATAGTGGA")

        String sequenceTemplate = URLDecoder.decode("${Organism.first().directory}/seq/f60/9c7/ee/%7B%22description%22:%22Group11.4%22,%20%22padding%22:0,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:0,%20%22end%22:75085,%20%22reverse%22:@REVERSE@%7D]%7D:-1..-1-@CHUNK@.txt","UTF-8")
        String featureTemplate = URLDecoder.decode("${Organism.first().directory}/seq/641/b27/07/%7B%22description%22:%22GB52236-RA%20(Group11.4)%22,%20%22padding%22:0,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:52653,%20%22end%22:59162,%20%22reverse%22:@REVERSE@,%20%22feature%22:%7B%22name%22:%22GB52236-RA%22,%20%22start%22:52653,%20%22end%22:59162,%20%22parent_id%22:%22Group11.4%22%7D%7D]%7D:-1..-1-@CHUNK@.txt","UTF-8")
        String returnedSequence
        Boolean reverse = false
        Integer chunk = null

        when: "the we view the full scaffold"
        reverse=false
        chunk=2
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE@", reverse.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "the returned sequence should contain the proper feature"
        assert returnedSequence.indexOf(gb52236_fivePrime)>0
        assert returnedSequence.indexOf(gb52236_threePrime)>0

        when: "the we view the specific feature"
        reverse=false
        chunk=0 // there is only a single feature chunk
        returnedSequence = refSeqProjectorService.projectSequence(featureTemplate.replace("@REVERSE@", reverse.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "verify that it is there and has the right sequences when viewed partially"
        assert returnedSequence.indexOf(gb52236_fivePrime)>0
        assert returnedSequence.indexOf(gb52236_threePrime)>0

        when: "the we view the specific feature in reverse"
        reverse=true
        returnedSequence = refSeqProjectorService.projectSequence(featureTemplate.replace("@REVERSE@", reverse.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "verify that it is there and has the right sequences when viewed partially in reverse"
        assert returnedSequence.indexOf(gb52236_fivePrime.reverse())>0
        assert returnedSequence.indexOf(gb52236_threePrime.reverse())>0
    }

    /**
     * Note, features should only ever be projected in the same direction.
     * This tests projecting features is separate directions, as well, but the UI disallows this.
     */
    void "get the PROJECTED SINGLE sequence and reverse sequence for TWO features of 11.4"() {
        given: "gene GB52236-RA on 11.4"
        // got these off of the feature, so doing this here
        String gb52236_fivePrime = SequenceUtil.reverseComplement("TAAAGATATCTTCCTTAATGGCGGCTTCGTCGCCTCATTCAAAGTATTATCACTCTTATTATTTAATGAT" +
                "AATTTCTCATCGTATAGTATCATATATGCAAGATGGCGGAACAGAGCCAACAAGATTTTAATAGAAGTGC" +
                "GTTGACAGTTCGATATGGACGGGTAATTAACATATACTTTTATTAATATATGTTTAAAGTGTTATTTTAG")
        String gb52236_threePrime = SequenceUtil.reverseComplement("TAAATGAAAGTGTGACCCCATACTGGTGAACCACCAGATCTGCGTACTACTCCAGTCTTTTGTTTTCCTG" +
                "AGCGACCTTTGTCAGGTAATAAGTAACTGAAAGTAATACAAATTAATATAAATTTATCTTTTTTCTTTTA" +
                "TTTCAATTATGATTAGATAATAATAAATAATAAAGATGTGTAAATTTTAGTAGTCTTATTAATCATTCAT" +
                "ATCATAAATAAATAGTGGA")
        String gb52238_fivePrime = "ATGTTTGCTTGGGGAACTTGTGTTCTCTATGGATGGAGGTTAAAG"
        String gb52238_threePrime = "ATCATATCCCCTATATCAAAGAATTAAATAATTAA"
        String sequenceTemplate = URLDecoder.decode("${Organism.first().directory}/seq/f60/9c7/ee/%7B%22description%22:%22Group11.4%22,%20%22padding%22:0,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:0,%20%22end%22:75085,%20%22reverse%22:@REVERSE@%7D]%7D:-1..-1-@CHUNK@.txt","UTF-8")
        String featureGb52236Template = URLDecoder.decode("${Organism.first().directory}/seq/641/b27/07/%7B%22description%22:%22GB52236-RA%20(Group11.4)%22,%20%22padding%22:0,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:52653,%20%22end%22:59162,%20%22reverse%22:@REVERSE@,%20%22feature%22:%7B%22name%22:%22GB52236-RA%22,%20%22start%22:52653,%20%22end%22:59162,%20%22parent_id%22:%22Group11.4%22%7D%7D]%7D:-1..-1-@CHUNK@.txt","UTF-8")
        String featureGb52238Template = URLDecoder.decode("${Organism.first().directory}/seq/1be/086/a3/%7B%22id%22:42540,%20%22name%22:%22Group11.4GB52238-RA%20Group11.4%22,%20%22description%22:%22GB52238-RA%20(Group11.4)%22,%20%22padding%22:0,%20%22start%22:10057,%20%22end%22:18796,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:10057,%20%22end%22:18796,%20%22reverse%22:@REVERSE@,%20%22feature%22:%7B%22start%22:10057,%20%22name%22:%22GB52238-RA%22,%20%22end%22:18796,%20%22parent_id%22:%22Group11.4%22%7D%7D]%7D:10057..18796-@CHUNK@.txt","UTF-8")
        String featureGb52236And52238Template = URLDecoder.decode("${Organism.first().directory}/seq/87f/888/1b/%7B%22description%22:%22GB52236-RA%20(Group11.4)::GB52238-RA%20(Group11.4)%22,%20%22padding%22:0,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:52653,%20%22end%22:59162,%20%22reverse%22:@REVERSE1@,%20%22feature%22:%7B%22name%22:%22GB52236-RA%22,%20%22start%22:52653,%20%22end%22:59162,%20%22parent_id%22:%22Group11.4%22%7D%7D,%7B%22name%22:%22Group11.4%22,%20%22start%22:10057,%20%22end%22:18796,%20%22reverse%22:@REVERSE2@,%20%22feature%22:%7B%22name%22:%22GB52238-RA%22,%20%22start%22:10057,%20%22end%22:18796,%20%22parent_id%22:%22Group11.4%22%7D%7D]%7D:-1..-1-@CHUNK@.txt","UTF-8")
        String featureGb52238And52236Template = URLDecoder.decode("${Organism.first().directory}/seq/876/56b/87/%7B%22description%22:%22GB52238-RA%20(Group11.4)::GB52236-RA%20(Group11.4)%22,%20%22padding%22:0,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:10057,%20%22end%22:18796,%20%22reverse%22:@REVERSE1@,%20%22feature%22:%7B%22start%22:10057,%20%22name%22:%22GB52238-RA%22,%20%22end%22:18796,%20%22parent_id%22:%22Group11.4%22%7D%7D,%7B%22name%22:%22Group11.4%22,%20%22start%22:52653,%20%22end%22:59162,%20%22reverse%22:@REVERSE2@,%20%22feature%22:%7B%22start%22:52653,%20%22name%22:%22GB52236-RA%22,%20%22end%22:59162,%20%22parent_id%22:%22Group11.4%22%7D%7D]%7D:-1..-1-@CHUNK@.txt","UTF-8")
        Boolean reverse = false
        Integer chunk = 0

        when: "we view only 11.4 forward, we should see all"
        String returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE@", reverse.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the correct sequences"
        assert returnedSequence.indexOf(gb52238_fivePrime)>0
        assert returnedSequence.indexOf(gb52238_threePrime)>0
        assert returnedSequence.indexOf(gb52236_fivePrime)<0
        assert returnedSequence.indexOf(gb52236_threePrime)<0

        when: "we view only 11.4 forward, we should see all"
        chunk = 2
        returnedSequence = refSeqProjectorService.projectSequence(sequenceTemplate.replace("@REVERSE@", reverse.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the correct sequences"
        assert returnedSequence.indexOf(gb52238_fivePrime)<0
        assert returnedSequence.indexOf(gb52238_threePrime)<0
        assert returnedSequence.indexOf(gb52236_fivePrime)>0
        assert returnedSequence.indexOf(gb52236_threePrime)>0

        when: "we view the projected gb52236 11.4 forward, we should see all"
        chunk = 0
        returnedSequence = refSeqProjectorService.projectSequence(featureGb52236Template.replace("@REVERSE@", reverse.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the correct sequences"
        assert returnedSequence.indexOf(gb52236_fivePrime)>0
        assert returnedSequence.indexOf(gb52236_threePrime)>0
        // should not be there (sanity)
        assert returnedSequence.indexOf(gb52238_fivePrime)<0
        assert returnedSequence.indexOf(gb52238_threePrime)<0

        when: "we view the projected gb52238 11.4 forward, we should see all"
        returnedSequence = refSeqProjectorService.projectSequence(featureGb52238Template.replace("@REVERSE@", reverse.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the correct sequences"
        assert returnedSequence.indexOf(gb52238_fivePrime)>0
        assert returnedSequence.indexOf(gb52238_threePrime)>0
        // should not be there (sanity)
        assert returnedSequence.indexOf(gb52236_fivePrime)<0
        assert returnedSequence.indexOf(gb52236_threePrime)<0

        when: "we view both project gb52236 gb52238 11.4 forward, we should see all"
        Boolean reverse1 = false
        Boolean reverse2 = false
        returnedSequence = refSeqProjectorService.projectSequence(featureGb52236And52238Template.replace("@REVERSE1@", reverse1.toString()).replace("@REVERSE2@", reverse2.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())
        Integer totalLength = returnedSequence.length()

        then: "we should see the correct sequences"
        assert returnedSequence.indexOf(gb52236_fivePrime)>0
        assert returnedSequence.indexOf(gb52236_threePrime)>0
        assert returnedSequence.indexOf(gb52238_fivePrime)>0
        assert returnedSequence.indexOf(gb52238_threePrime)>0
        assert returnedSequence.length()<20000

        assert returnedSequence.indexOf(gb52236_threePrime) < returnedSequence.indexOf(gb52238_fivePrime)

        assert returnedSequence.indexOf(gb52236_fivePrime) > returnedSequence.indexOf(gb52236_threePrime)
        assert returnedSequence.indexOf(gb52238_fivePrime) < returnedSequence.indexOf(gb52238_threePrime)

        when: "we view both reverse projection of gb52236 gb52238 11.4 forward, we should see all"
        reverse1 = true
        reverse2 = true
        returnedSequence = refSeqProjectorService.projectSequence(featureGb52236And52238Template.replace("@REVERSE1@", reverse1.toString()).replace("@REVERSE2@", reverse2.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the correct sequences reversed"
        assert returnedSequence.indexOf(gb52236_fivePrime.reverse())>0
        assert returnedSequence.indexOf(gb52236_threePrime.reverse())>0
        assert returnedSequence.indexOf(gb52238_fivePrime.reverse())>0
        assert returnedSequence.indexOf(gb52238_threePrime.reverse())>0
        assert returnedSequence.length()<20000
        assert returnedSequence.length()==totalLength

        // it remaines reversed, because the order is not actually reversed here but separately
        assert returnedSequence.indexOf(gb52236_threePrime.reverse()) < returnedSequence.indexOf(gb52238_fivePrime.reverse())

        assert returnedSequence.indexOf(gb52236_fivePrime.reverse()) < returnedSequence.indexOf(gb52236_threePrime.reverse())
        assert returnedSequence.indexOf(gb52238_fivePrime.reverse()) > returnedSequence.indexOf(gb52238_threePrime.reverse())

        assert returnedSequence.indexOf(gb52236_fivePrime.reverse()) < returnedSequence.indexOf(gb52238_fivePrime.reverse())

        when: "we view both reverse the first only of gb52236 gb52238 11.4 forward, we should see all"
        reverse1 = true
        reverse2 = false
        returnedSequence = refSeqProjectorService.projectSequence(featureGb52236And52238Template.replace("@REVERSE1@", reverse1.toString()).replace("@REVERSE2@", reverse2.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the first sequence reversed"
        assert returnedSequence.length()==totalLength
        assert returnedSequence.indexOf(gb52236_fivePrime.reverse())>0
        assert returnedSequence.indexOf(gb52236_threePrime.reverse())>0
        assert returnedSequence.indexOf(gb52238_fivePrime)>0
        assert returnedSequence.indexOf(gb52238_threePrime)>0

        assert returnedSequence.indexOf(gb52236_fivePrime.reverse()) < returnedSequence.indexOf(gb52238_fivePrime)

        assert returnedSequence.indexOf(gb52236_fivePrime.reverse()) < returnedSequence.indexOf(gb52236_threePrime.reverse())
        assert returnedSequence.indexOf(gb52238_fivePrime) < returnedSequence.indexOf(gb52238_threePrime)


        when: "we view both reverse the first only of gb52236 gb52238 11.4 forward, we should see all"
        reverse1 = false
        reverse2 = true
        returnedSequence = refSeqProjectorService.projectSequence(featureGb52236And52238Template.replace("@REVERSE1@", reverse1.toString()).replace("@REVERSE2@", reverse2.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the last sequence reversed"
        assert returnedSequence.length()==totalLength
        assert returnedSequence.indexOf(gb52236_fivePrime)>0
        assert returnedSequence.indexOf(gb52236_threePrime)>0
        assert returnedSequence.indexOf(gb52238_fivePrime.reverse())>0
        assert returnedSequence.indexOf(gb52238_threePrime.reverse())>0

        assert returnedSequence.indexOf(gb52236_threePrime) < returnedSequence.indexOf(gb52238_fivePrime.reverse())

        assert returnedSequence.indexOf(gb52238_fivePrime.reverse()) > returnedSequence.indexOf(gb52238_threePrime.reverse())
        assert returnedSequence.indexOf(gb52236_fivePrime) > returnedSequence.indexOf(gb52236_threePrime)




        when: "we flip it (GB52238 and then GB52236) and see what we get"
        reverse1 = false
        reverse2 = false
        returnedSequence = refSeqProjectorService.projectSequence(featureGb52238And52236Template.replace("@REVERSE1@", reverse1.toString()).replace("@REVERSE2@", reverse2.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the same sequences in the forward order"
        assert returnedSequence.length()==totalLength
        assert returnedSequence.indexOf(gb52238_fivePrime)>0
        assert returnedSequence.indexOf(gb52238_threePrime)>0
        assert returnedSequence.indexOf(gb52236_fivePrime)>0
        assert returnedSequence.indexOf(gb52236_threePrime)>0

        assert returnedSequence.indexOf(gb52238_threePrime) < returnedSequence.indexOf(gb52236_fivePrime)

        assert returnedSequence.indexOf(gb52238_fivePrime) < returnedSequence.indexOf(gb52238_threePrime)
        assert returnedSequence.indexOf(gb52236_fivePrime) > returnedSequence.indexOf(gb52236_threePrime)

        when: "we flip it and reverse both (GB52238 and then GB52236) and see what we get"
        reverse1 = true
        reverse2 = true
        returnedSequence = refSeqProjectorService.projectSequence(featureGb52238And52236Template.replace("@REVERSE1@", reverse1.toString()).replace("@REVERSE2@", reverse2.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the same sequences in the forward order"
        assert returnedSequence.length()==totalLength
        assert returnedSequence.indexOf(gb52238_fivePrime.reverse())>0
        assert returnedSequence.indexOf(gb52238_threePrime.reverse())>0
        assert returnedSequence.indexOf(gb52236_fivePrime.reverse())>0
        assert returnedSequence.indexOf(gb52236_threePrime.reverse())>0

        assert returnedSequence.indexOf(gb52238_threePrime.reverse()) < returnedSequence.indexOf(gb52236_fivePrime.reverse())

        assert returnedSequence.indexOf(gb52238_fivePrime.reverse()) > returnedSequence.indexOf(gb52238_threePrime.reverse())
        assert returnedSequence.indexOf(gb52236_fivePrime.reverse()) < returnedSequence.indexOf(gb52236_threePrime.reverse())

        when: "we flip it and reverse first (GB52238 and then GB52236) and see what we get"
        reverse1 = true
        reverse2 = false
        returnedSequence = refSeqProjectorService.projectSequence(featureGb52238And52236Template.replace("@REVERSE1@", reverse1.toString()).replace("@REVERSE2@", reverse2.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the same sequences in the forward order"
        assert returnedSequence.length()==totalLength
        assert returnedSequence.indexOf(gb52238_fivePrime.reverse())>0
        assert returnedSequence.indexOf(gb52238_threePrime.reverse())>0
        assert returnedSequence.indexOf(gb52236_fivePrime)>0
        assert returnedSequence.indexOf(gb52236_threePrime)>0

        assert returnedSequence.indexOf(gb52238_threePrime.reverse()) < returnedSequence.indexOf(gb52236_fivePrime)

        assert returnedSequence.indexOf(gb52238_fivePrime.reverse()) > returnedSequence.indexOf(gb52238_threePrime.reverse())
        assert returnedSequence.indexOf(gb52236_fivePrime) > returnedSequence.indexOf(gb52236_threePrime)


        when: "we flip it and reverse last (GB52238 and then GB52236) and see what we get"
        reverse1 = false
        reverse2 = true
        returnedSequence = refSeqProjectorService.projectSequence(featureGb52238And52236Template.replace("@REVERSE1@", reverse1.toString()).replace("@REVERSE2@", reverse2.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())

        then: "we should see the same sequences in the forward order"
        assert returnedSequence.length()==totalLength
        assert returnedSequence.indexOf(gb52238_fivePrime)>0
        assert returnedSequence.indexOf(gb52238_threePrime)>0
        assert returnedSequence.indexOf(gb52236_fivePrime.reverse())>0
        assert returnedSequence.indexOf(gb52236_threePrime.reverse())>0

        assert returnedSequence.indexOf(gb52238_threePrime) < returnedSequence.indexOf(gb52236_fivePrime.reverse())

        assert returnedSequence.indexOf(gb52238_fivePrime) < returnedSequence.indexOf(gb52238_threePrime)
        assert returnedSequence.indexOf(gb52236_fivePrime.reverse()) < returnedSequence.indexOf(gb52236_threePrime.reverse())

    }

    void "get the PROJECTED contiguous sequence and reverse sequence for TWO features of 11.4 and Un87 contiguously"() {
        given: "two 11.4 and Un87 features"

        // 11.4 strings
        String gb52236_fivePrime = SequenceUtil.reverseComplement("TAAAGATATCTTCCTTAATGGCGGCTTCGTCGCCTCATTCAAAGTATTATCACTCTTATTATTTAATGAT" +
                "AATTTCTCATCGTATAGTATCATATATGCAAGATGGCGGAACAGAGCCAACAAGATTTTAATAGAAGTGC" +
                "GTTGACAGTTCGATATGGACGGGTAATTAACATATACTTTTATTAATATATGTTTAAAGTGTTATTTTAG")
        String gb52236_threePrime = SequenceUtil.reverseComplement("TAAATGAAAGTGTGACCCCATACTGGTGAACCACCAGATCTGCGTACTACTCCAGTCTTTTGTTTTCCTG" +
                "AGCGACCTTTGTCAGGTAATAAGTAACTGAAAGTAATACAAATTAATATAAATTTATCTTTTTTCTTTTA" +
                "TTTCAATTATGATTAGATAATAATAAATAATAAAGATGTGTAAATTTTAGTAGTCTTATTAATCATTCAT" +
                "ATCATAAATAAATAGTGGA")
        String gb52238_fivePrime = "ATGTTTGCTTGGGGAACTTGTGTTCTCTATGGATGGAGGTTAAAG"
        String gb52238_threePrime = "ATCATATCCCCTATATCAAAGAATTAAATAATTAA"

        // Un87 strings
        String gb53496 = "ATGCACTGTCAACGTACACGGGCAGCCTTAAGATGTCATGCAGAAGCACGACAGTTAACTTTAAAGCAGT" +
                "TAAATTCTATTCTATCATTTTTTTTATTCTATCAACGTAATGAAACGAAGCAAGAAATTTGGTTACCAGA" +
                "TATCGGAACTTTCGGTTCTTTTGTATCTAAACGTCAACGTGTATCTAAAAATATAATGGCAGTAAAATAC" +
                "TAA"
        String gb53498_fivePrime = "ATGAAAGGTAAGTGAATATCAATATAGAATTCACATCTAGAATTTCTTTTTATGTAAAAACGAAGTAACT"
        String gb53498_threePrime = "ATACAGAAGTAAGTATTTCTATAATAATTTTATAAAAAATATAATAACAAATAATAAGATAATAGGAATA" +
                "ATTAAATGAAATAAAAATATTAG"
        Integer chunk
        Boolean reverse11_4
        Boolean reverseUn87
        // 11_4 and then Un87
        // 55236, 55238, 53498, and 53496 L to R
        String featureTemplate = URLDecoder.decode("${Organism.first().directory}/seq/b94/197/2b/%7B%22id%22:43685,%20%22name%22:%22Group11.4GB52236-RA%20Group11.4Group11.4GB52238-RA%20Group11.4GB52236-RA%20Group11.4Group11.4GroupUn87GB5%22,%20%22description%22:%22GB52236-RA%20(Group11.4)::GB52238-RA%20(Group11.4)::GB53498-RA%20(GroupUn87)::GB53496-RA%20(GroupUn87)%22,%20%22padding%22:0,%20%22start%22:52653,%20%22end%22:10379,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:52653,%20%22end%22:59162,%20%22reverse%22:@REVERSE1@,%20%22feature%22:%7B%22start%22:52653,%20%22name%22:%22GB52236-RA%22,%20%22end%22:59162,%20%22parent_id%22:%22Group11.4%22%7D%7D,%7B%22name%22:%22Group11.4%22,%20%22start%22:10057,%20%22end%22:18796,%20%22reverse%22:@REVERSE1@,%20%22feature%22:%7B%22start%22:10057,%20%22name%22:%22GB52238-RA%22,%20%22end%22:18796,%20%22parent_id%22:%22Group11.4%22%7D%7D,%7B%22name%22:%22GroupUn87%22,%20%22start%22:29196,%20%22end%22:30529,%20%22reverse%22:@REVERSE2@,%20%22feature%22:%7B%22start%22:29196,%20%22name%22:%22GB53498-RA%22,%20%22end%22:30529,%20%22parent_id%22:%22GroupUn87%22%7D%7D,%7B%22name%22:%22GroupUn87%22,%20%22start%22:9766,%20%22end%22:10379,%20%22reverse%22:@REVERSE2@,%20%22feature%22:%7B%22start%22:9766,%20%22name%22:%22GB53496-RA%22,%20%22end%22:10379,%20%22parent_id%22:%22GroupUn87%22%7D%7D]%7D:52653..10379-@CHUNK@.txt","UTF-8")



        when: "we just view the features"
        reverse11_4 = false
        reverseUn87 = false
        chunk = 0
        String returnedSequence = refSeqProjectorService.projectSequence(featureTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())
        Integer index52236_fivePrime = returnedSequence.indexOf(gb52236_fivePrime)
        Integer index52236_threePrime = returnedSequence.indexOf(gb52236_threePrime)
        Integer index52238_fivePrime = returnedSequence.indexOf(gb52238_fivePrime)
        Integer index52238_threePrime = returnedSequence.indexOf(gb52238_threePrime)
        Integer index53498_fivePrime = returnedSequence.indexOf(gb53498_fivePrime)
        Integer index53498_threePrime = returnedSequence.indexOf(gb53498_threePrime)
        Integer index53496 = returnedSequence.indexOf(gb53496)
        Integer totalLength = returnedSequence.length()

        then: "we should see them all still"
        assert index52236_fivePrime > 0
        assert index52236_threePrime > 0
        assert index52238_fivePrime > 0
        assert index52238_threePrime > 0
        assert index53498_fivePrime > 0
        assert index53498_threePrime > 0
        assert index53496 > 0
        assert index52236_fivePrime > index52236_threePrime // negative strand
        assert index52236_threePrime < index52238_fivePrime
        assert index52238_fivePrime < index52238_threePrime
        assert index52238_threePrime < index53498_fivePrime
        assert index53498_fivePrime < index53498_threePrime
        assert index53498_threePrime < index53496
        assert totalLength < 20000


        when: "we reverse the features view"
        reverse11_4 = true
        reverseUn87 = true
        chunk = 0
        returnedSequence = refSeqProjectorService.projectSequence(featureTemplate.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())
        index52236_fivePrime = returnedSequence.indexOf(gb52236_fivePrime.reverse())
        index52236_threePrime = returnedSequence.indexOf(gb52236_threePrime.reverse())
        index52238_fivePrime = returnedSequence.indexOf(gb52238_fivePrime.reverse())
        index52238_threePrime = returnedSequence.indexOf(gb52238_threePrime.reverse())
        index53498_fivePrime = returnedSequence.indexOf(gb53498_fivePrime.reverse())
        index53498_threePrime = returnedSequence.indexOf(gb53498_threePrime.reverse())
        index53496 = returnedSequence.indexOf(gb53496.reverse())
        totalLength = returnedSequence.length()

        then: "we should see them all still"
        assert returnedSequence.length()==totalLength
        assert totalLength == 17194
        assert index52236_fivePrime > 0
        assert index52236_threePrime > 0
        assert index52238_fivePrime > 0
        assert index52238_threePrime > 0
        assert index53498_fivePrime > 0
        assert index53498_threePrime > 0
        assert index53496 > 0
        assert index52236_fivePrime < index52236_threePrime // negative strand
        assert index52236_threePrime < index52238_fivePrime
        assert index52238_fivePrime > index52238_threePrime
        assert index52238_threePrime < index53498_fivePrime
        assert index53498_fivePrime > index53498_threePrime
        assert index53498_threePrime < index53496
    }

    // TODO: we can try doing a full sequence case, but I think we've already done this
//    void "we can calculate multi-scaffold features"(){
//
//        given: "two 11.4 and Un87 features"
//
//        // 11.4 strings
//        String gb52236_fivePrime = SequenceUtil.reverseComplement("TAAAGATATCTTCCTTAATGGCGGCTTCGTCGCCTCATTCAAAGTATTATCACTCTTATTATTTAATGAT" +
//                "AATTTCTCATCGTATAGTATCATATATGCAAGATGGCGGAACAGAGCCAACAAGATTTTAATAGAAGTGC" +
//                "GTTGACAGTTCGATATGGACGGGTAATTAACATATACTTTTATTAATATATGTTTAAAGTGTTATTTTAG")
//        String gb52236_threePrime = SequenceUtil.reverseComplement("TAAATGAAAGTGTGACCCCATACTGGTGAACCACCAGATCTGCGTACTACTCCAGTCTTTTGTTTTCCTG" +
//                "AGCGACCTTTGTCAGGTAATAAGTAACTGAAAGTAATACAAATTAATATAAATTTATCTTTTTTCTTTTA" +
//                "TTTCAATTATGATTAGATAATAATAAATAATAAAGATGTGTAAATTTTAGTAGTCTTATTAATCATTCAT" +
//                "ATCATAAATAAATAGTGGA")
//        String gb52238_fivePrime = "ATGTTTGCTTGGGGAACTTGTGTTCTCTATGGATGGAGGTTAAAG"
//        String gb52238_threePrime = "ATCATATCCCCTATATCAAAGAATTAAATAATTAA"
//
//        // Un87 strings
//        String gb53496 = "ATGCACTGTCAACGTACACGGGCAGCCTTAAGATGTCATGCAGAAGCACGACAGTTAACTTTAAAGCAGT" +
//                "TAAATTCTATTCTATCATTTTTTTTATTCTATCAACGTAATGAAACGAAGCAAGAAATTTGGTTACCAGA" +
//                "TATCGGAACTTTCGGTTCTTTTGTATCTAAACGTCAACGTGTATCTAAAAATATAATGGCAGTAAAATAC" +
//                "TAA"
//        String gb53498_fivePrime = "ATGAAAGGTAAGTGAATATCAATATAGAATTCACATCTAGAATTTCTTTTTATGTAAAAACGAAGTAACT"
//        String gb53498_threePrime = "ATACAGAAGTAAGTATTTCTATAATAATTTTATAAAAAATATAATAACAAATAATAAGATAATAGGAATA" +
//                "ATTAAATGAAATAAAAATATTAG"
//        Integer chunk = 0
//        Boolean reverse11_4
//        Boolean reverseUn87
//        // 55236, 55238, 53498, and 53496 L to R
//        String sequence11_4AndUn87Template = URLDecoder.decode("${Organism.first().directory}/seq/051/49c/cb/%7B%22description%22:%22Group11.4::GroupUn87%22,%20%22padding%22:0,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:0,%20%22end%22:75085,%20%22reverse%22:@REVERSE1@%7D,%7B%22name%22:%22GroupUn87%22,%20%22start%22:0,%20%22end%22:78258,%20%22reverse%22:@REVERSE2@%7D]%7D:-1..-1-@CHUNK@.txt","UTF-8")
//        String sequenceUn87Andll_4Template = URLDecoder.decode("${Organism.first().directory}/seq/f60/e8f/a2/%7B%22description%22:%22GroupUn87::Group11.4%22,%20%22padding%22:0,%20%22sequenceList%22:[%7B%22name%22:%22GroupUn87%22,%20%22start%22:0,%20%22end%22:78258,%20%22reverse%22:@REVERSE1@%7D,%7B%22name%22:%22Group11.4%22,%20%22start%22:0,%20%22end%22:75085,%20%22reverse%22:@REVERSE2@%7D]%7D:-1..-1-@CHUNK@.txt","UTF-8")
//
//        // Un87And11_4
//        String featureTemplate = URLDecoder.decode("${Organism.first().directory}/seq/b94/197/2b/%7B%22id%22:43685,%20%22name%22:%22Group11.4GB52236-RA%20Group11.4Group11.4GB52238-RA%20Group11.4GB52236-RA%20Group11.4Group11.4GroupUn87GB5%22,%20%22description%22:%22GB52236-RA%20(Group11.4)::GB52238-RA%20(Group11.4)::GB53498-RA%20(GroupUn87)::GB53496-RA%20(GroupUn87)%22,%20%22padding%22:0,%20%22start%22:52653,%20%22end%22:10379,%20%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%20%22start%22:52653,%20%22end%22:59162,%20%22reverse%22:@REVERSE1@,%20%22feature%22:%7B%22start%22:52653,%20%22name%22:%22GB52236-RA%22,%20%22end%22:59162,%20%22parent_id%22:%22Group11.4%22%7D%7D,%7B%22name%22:%22Group11.4%22,%20%22start%22:10057,%20%22end%22:18796,%20%22reverse%22:@REVERSE1@,%20%22feature%22:%7B%22start%22:10057,%20%22name%22:%22GB52238-RA%22,%20%22end%22:18796,%20%22parent_id%22:%22Group11.4%22%7D%7D,%7B%22name%22:%22GroupUn87%22,%20%22start%22:29196,%20%22end%22:30529,%20%22reverse%22:@REVERSE2@,%20%22feature%22:%7B%22start%22:29196,%20%22name%22:%22GB53498-RA%22,%20%22end%22:30529,%20%22parent_id%22:%22GroupUn87%22%7D%7D,%7B%22name%22:%22GroupUn87%22,%20%22start%22:9766,%20%22end%22:10379,%20%22reverse%22:@REVERSE2@,%20%22feature%22:%7B%22start%22:9766,%20%22name%22:%22GB53496-RA%22,%20%22end%22:10379,%20%22parent_id%22:%22GroupUn87%22%7D%7D]%7D:52653..10379-@CHUNK@.txt","UTF-8")

//        when: "we project them all contiguously together"
//        String returnedSequence = refSeqProjectorService.projectSequence(sequence11_4AndUn87Template.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())
//        Integer index52236_fivePrime = returnedSequence.indexOf(gb52236_fivePrime)
//        Integer index52236_threePrime = returnedSequence.indexOf(gb52236_threePrime)
//        Integer index52238_fivePrime = returnedSequence.indexOf(gb52238_fivePrime)
//        Integer index52238_threePrime = returnedSequence.indexOf(gb52238_threePrime)
//        Integer index53498_fivePrime = returnedSequence.indexOf(gb53498_fivePrime)
//        Integer index53498_threePrime = returnedSequence.indexOf(gb53498_threePrime)
//        Integer index53496 = returnedSequence.indexOf(gb53496)
//        Integer totalLength = returnedSequence.length()
//
//        then: "we should see all of the strings"
//        assert index52236_fivePrime > 0
//        assert index52236_threePrime > 0
//        assert index52238_fivePrime > 0
//        assert index52238_threePrime > 0
//        assert index53498_fivePrime > 0
//        assert index53498_threePrime > 0
//        assert index53496 > 0
//        assert index52236_fivePrime < index52236_threePrime
//        assert index52236_threePrime < index52238_fivePrime
//        assert index52238_fivePrime < index52238_threePrime
//        assert index52238_threePrime < index53498_fivePrime
//        assert index53498_fivePrime < index52236_threePrime
//        assert index52236_threePrime < index53496
//        assert totalLength == 20000
//
//        when: "we project them all contiguously together, but reverse them all "
//        reverse11_4 = true
//        reverseUn87 = true
//        returnedSequence = refSeqProjectorService.projectSequence(sequence11_4AndUn87Template.replace("@REVERSE1@", reverse11_4.toString()).replace("@REVERSE2@", reverseUn87.toString()).replace("@CHUNK@", chunk.toString()), Organism.first())
//
//        then: "we should see all of the strings"
//
//        when: "we project them all contiguously together, but reverse the first one"
//        reverse11_4 = true
//        reverseUn87 = false
//
//        then: "we should see all of the strings"
//
//        when: "we project them all contiguously together, but reverse the second one"
//        reverse11_4 = false
//        reverseUn87 = true
//
//        then: "we should see all of the strings"
//        assert false
//
//        // Un87And11_4
//        when: "we flip them around"
//        reverse11_4 = false
//        reverseUn87 = false
//
//        then: "we expect to see them in flipped order "
//
//        // Un87And11_4
//        when: "we flip them around"
//        reverse11_4 = true
//        reverseUn87 = true
//
//        then: "we expect to see them in flipped order reversed"
//    }
//

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
