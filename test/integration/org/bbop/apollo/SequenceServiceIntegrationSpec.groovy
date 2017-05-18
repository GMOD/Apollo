package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONObject

import java.util.zip.CRC32

class SequenceServiceIntegrationSpec extends AbstractIntegrationSpec{
    
    def requestHandlingService
    def sequenceService
    def transcriptService


    void "add a simple gene model to get its sequence and a valid GFF3"() {
        
        given: "a simple gene model with 1 mRNA, 1 exon and 1 CDS"
        String jsonString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1248797,\"fmax\":1249052,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40722-RA\",\"children\":[{\"location\":{\"fmin\":1248797,\"fmax\":1249052,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1248797,\"fmax\":1249052,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        
        when: "the gene model is added"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "we should see the appropriate model"
        assert Sequence.count == 1
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 1
        assert CDS.count == 1
        assert FeatureRelationship.count == 3

        String getSequenceString = "{${testCredentials} \"operation\":\"get_sequence\",\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\",\"type\":\"@SEQUENCE_TYPE@\"}"
        String uniqueName = MRNA.findByName("GB40722-RA-00001").uniqueName
        JSONObject commandObject = new JSONObject()
        
        when: "A request is sent for the peptide sequence of the mRNA"
        String getPeptideSequenceString = getSequenceString.replaceAll("@UNIQUENAME@", uniqueName)
        getPeptideSequenceString = getPeptideSequenceString.replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_PEPTIDE.value)
        commandObject = JSON.parse(getPeptideSequenceString) as JSONObject
        JSONObject getPeptideSequenceReturnObject = sequenceService.getSequenceForFeatures(commandObject)
        
        then: "we should get back the expected peptide sequence"
        assert getPeptideSequenceReturnObject.residues != null
        String expectedPeptideSequence = "MSSYTFSQKVSTPIPPEKGSFPLDHEGICKRLMIKYMRCLIENNNENTMCRDIIKEYLSCRMDNELMAREEWSNLGFSDEVKET"
        assert getPeptideSequenceReturnObject.residues == expectedPeptideSequence
        
        when: "A request is sent for the CDS sequence of the mRNA"
        String getCDSSequenceString = getSequenceString.replaceAll("@UNIQUENAME@", uniqueName)
        getCDSSequenceString = getCDSSequenceString.replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_CDS.value)
        commandObject = JSON.parse(getCDSSequenceString) as JSONObject
        JSONObject getCDSSequenceReturnObject = sequenceService.getSequenceForFeatures(commandObject)

        then: "we should get back the expected CDS sequence"
        assert getCDSSequenceReturnObject.residues != null
        String expectedCDSSequence = "ATGTCATCGTATACATTTTCGCAAAAAGTGTCTACACCTATACCTCCAGAAAAGGGTAGTTTTCCACTCGACCATGAGGGTATTTGCAAAAGACTTATGATTAAGTATATGCGCTGTTTAATTGAGAATAATAACGAAAACACGATGTGTCGTGATATTATAAAAGAATACCTTTCTTGTCGAATGGATAATGAGCTTATGGCACGAGAAGAATGGTCTAATCTTGGTTTTTCTGACGAGGTCAAGGAGACATAA"
        assert getCDSSequenceReturnObject.residues == expectedCDSSequence

        when: "A request is sent for the CDNA sequence of the mRNA"
        String getCDNASequenceString = getSequenceString.replaceAll("@UNIQUENAME@", uniqueName)
        getCDNASequenceString = getCDNASequenceString.replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_CDNA.value)
        commandObject = JSON.parse(getCDNASequenceString) as JSONObject
        JSONObject getCDNASequenceReturnObject = sequenceService.getSequenceForFeatures(commandObject)

        then: "we should get back the expected CDNA sequence"
        assert getCDNASequenceReturnObject.residues != null
        String expectedCDNASequence = "ATGTCATCGTATACATTTTCGCAAAAAGTGTCTACACCTATACCTCCAGAAAAGGGTAGTTTTCCACTCGACCATGAGGGTATTTGCAAAAGACTTATGATTAAGTATATGCGCTGTTTAATTGAGAATAATAACGAAAACACGATGTGTCGTGATATTATAAAAGAATACCTTTCTTGTCGAATGGATAATGAGCTTATGGCACGAGAAGAATGGTCTAATCTTGGTTTTTCTGACGAGGTCAAGGAGACATAA"
        assert getCDNASequenceReturnObject.residues == expectedCDNASequence
//
        when: "A request is sent for the genomic sequence of the mRNA"
        String getGenomicSequenceString = getSequenceString.replaceAll("@UNIQUENAME@", uniqueName)
        getGenomicSequenceString = getGenomicSequenceString.replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_GENOMIC.value)
        commandObject = JSON.parse(getGenomicSequenceString) as JSONObject
        JSONObject getGenomicSequenceReturnObject = sequenceService.getSequenceForFeatures(commandObject)
//
        then: "we should get back the expected genomic sequence"
        assert getGenomicSequenceReturnObject.residues != null
        String expectedGenomicSequence = "ATGTCATCGTATACATTTTCGCAAAAAGTGTCTACACCTATACCTCCAGAAAAGGGTAGTTTTCCACTCGACCATGAGGGTATTTGCAAAAGACTTATGATTAAGTATATGCGCTGTTTAATTGAGAATAATAACGAAAACACGATGTGTCGTGATATTATAAAAGAATACCTTTCTTGTCGAATGGATAATGAGCTTATGGCACGAGAAGAATGGTCTAATCTTGGTTTTTCTGACGAGGTCAAGGAGACATAA"
        assert getGenomicSequenceReturnObject.residues == expectedGenomicSequence

        when: "A request is sent for the genomic sequence of the mRNA with a flank of 500 bp"
        String getGenomicFlankSequenceString = getSequenceString.replaceAll("@UNIQUENAME@", uniqueName)
        getGenomicFlankSequenceString = getGenomicFlankSequenceString.replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_GENOMIC.value)
        commandObject = JSON.parse(getGenomicFlankSequenceString) as JSONObject
        commandObject.put("flank", 500)
        JSONObject getGenomicFlankSequenceReturnObject = sequenceService.getSequenceForFeatures(commandObject)

        then: "we should get back the expected genomic sequence including the flanking regions"
        assert getGenomicFlankSequenceReturnObject.residues != null
        String expectedGenomicSequenceWithFlank = "TAATAAATTTACTGTGTATTTAATCTGTATTTCTATCTTAAATGTAAGTGCAAAATTTTTTGAAAAAAATTTCATTAATTTTTCTAATGGCAAATGTAAGGACCTCATTTTTTTTATTGACTTATTATTTTTTTAATTAGAATGCAATAGAAATTTTTCTATTTTCATTGTTCAGTAAATAATTATATTTTAAGTTTTCTTTAATTCTATTAAGAATAAAATAAATTCCTGTAAAACTAAAATAAATTATCGAAATTATAATTATTATTTATTTTATATTTAATTTAAATTGGATATCTTTTTTATATTATATAACACGGAGCGTGCGTAAAATGAAGTTAGAAATATCATCGTAAGTGGCGCTAGTATATTTATTATCCCGTAATGGCGGTTATGATTTATCGTGTATTTTGTAATAAGTTTACTTAAAAGAAGTTTATTGAAAATTTTGACTTTTACTTTTCATCGAGTTTTCTGTATGAACGTAATCAGCGGTAGTGATGTCATCGTATACATTTTCGCAAAAAGTGTCTACACCTATACCTCCAGAAAAGGGTAGTTTTCCACTCGACCATGAGGGTATTTGCAAAAGACTTATGATTAAGTATATGCGCTGTTTAATTGAGAATAATAACGAAAACACGATGTGTCGTGATATTATAAAAGAATACCTTTCTTGTCGAATGGATAATGAGCTTATGGCACGAGAAGAATGGTCTAATCTTGGTTTTTCTGACGAGGTCAAGGAGACATAACAGCAGGGATTTATTACTAATCGATTGTCGCAAAGTTGTAGATCACTGTTTTTATCACTTGAATGGTATTTACAAACTTTTTCGCTGGTGATTCTTATTTTATCTTAGATAAGACTTATATGGATTTTAAAGTGAATTTGTAAATTTGTAGCGACATATAACTTATGTACGTATTTTATGAATTATAAATAATCATGCATTTATTCGATATGATTTTTGAATAATGATACAATATAAGTAGAAAATTCTTTTTAACATTCAATCCAATTAAAGTGATCTCAGTGCAGAAAAAGCTTCACAGCAAATATTAAATATTTAAAATAAATTAATAATATAAATATATTCAGTATAGAGATCTAAGGAGAAAAGAAATCATTAATAAAAATACTTTTATTAAATGTAGTTTATTGTTAATTGATGTGATTTAGGTATCTTATATCAGATGGCCTAAAATATTTAATATAGCTATAACTGTAAAAGTTAAAATCATTTCATCTTGA"
        assert getGenomicFlankSequenceReturnObject.residues == expectedGenomicSequenceWithFlank
        
        when: "A request is sent for the GFF3 of a simple gene model"
        String getGff3String = "{${testCredentials} \"operation\":\"get_gff3\",\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        getGff3String = getGff3String.replaceAll("@UNIQUENAME@", uniqueName)
        JSONObject inputObject = JSON.parse(getGff3String) as JSONObject
        Sequence refSequence = Sequence.first()
        FeatureLocation.all.each { featureLocation->
            refSequence.addToFeatureLocations(featureLocation)
        }
        File gffFile = File.createTempFile("feature", ".gff3")
        sequenceService.getGff3ForFeature(inputObject, gffFile)

        then: "we should get a proper GFF3 for the feature"
        String gffFileText = gffFile.text
        assert gffFileText.length() > 0
        log.debug gffFileText
    }

    void "add a gene model with UTRs to get its sequence and a valid GFF3"() {
        
        given: "a gene with a 5' UTR and a 3' UTR"
        String jsonString = "{${testCredentials}  \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":694293,\"fmax\":696055,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40749-RA\",\"children\":[{\"location\":{\"fmin\":695943,\"fmax\":696055,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":694293,\"fmax\":694440,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":694293,\"fmax\":694606,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":694918,\"fmax\":695591,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":695882,\"fmax\":696055,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":694440,\"fmax\":695943,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        
        when: "the gene model is added"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)
        
        then: "we should see the appropriate model"
        assert Sequence.count == 1
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3
        assert CDS.count == 1
//        assert FeatureLocation.count == 6 + FlankingRegion.count
        assert FeatureRelationship.count == 5

        String getSequenceString = "{${testCredentials} \"operation\":\"get_sequence\",\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\",\"type\":\"@SEQUENCE_TYPE@\"}"
        String uniqueName = MRNA.findByName("GB40749-RA-00001").uniqueName
        JSONObject commandObject = new JSONObject()
        
        when: "A request is sent for the peptide sequence of the mRNA"
        String getPeptideSequenceString = getSequenceString.replaceAll("@UNIQUENAME@", uniqueName)
        getPeptideSequenceString = getPeptideSequenceString.replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_PEPTIDE.value)
        commandObject = JSON.parse(getPeptideSequenceString) as JSONObject
        JSONObject getPeptideSequenceReturnObject = sequenceService.getSequenceForFeatures(commandObject)

        then: "we should get back the expected peptide sequence"
        assert getPeptideSequenceReturnObject.residues != null
        String expectedPeptideSequence = "MPRCLIKSMTRYRKTDNSSEVEAELPWTPPSSVDAKRKHQIKDNSTKCNNIWTSSRLPIVTRYTFNKENNIFWNKELNIADVELGSRNFSEIENTIPSTTPNVSVNTNQAMVDTSNEQKVEKVQIPLPSNAKKVEYPVNVSNNEIKVAVNLNRMFDGAENQTTSQTLYIATNKKQIDSQNQYLGGNMKTTGVENPQNWKRNKTMHYCPYCRKSFDRPWVLKGHLRLHTGERPFECPVCHKSFADRSNLRAHQRTRNHHQWQWRCGECFKAFSQRRYLERHCPEACRKYRISQRREQNCS"
        assert getPeptideSequenceReturnObject.residues == expectedPeptideSequence

        when: "A request is sent for the CDS sequence of the mRNA"
        String getCDSSequenceString = getSequenceString.replaceAll("@UNIQUENAME@", uniqueName)
        getCDSSequenceString = getCDSSequenceString.replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_CDS.value)
        commandObject = JSON.parse(getCDSSequenceString) as JSONObject
        JSONObject getCDSSequenceReturnObject = sequenceService.getSequenceForFeatures(commandObject)

        then: "we should get back the expected CDS sequence"
        assert getCDSSequenceReturnObject.residues != null
        String expectedCDSSequence = "ATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAG"
        assert getCDSSequenceReturnObject.residues == expectedCDSSequence

        when: "A request is sent for the CDNA sequence of the mRNA"
        String getCDNASequenceString = getSequenceString.replaceAll("@UNIQUENAME@", uniqueName)
        getCDNASequenceString = getCDNASequenceString.replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_CDNA.value)
        commandObject = JSON.parse(getCDNASequenceString) as JSONObject
        JSONObject getCDNASequenceReturnObject = sequenceService.getSequenceForFeatures(commandObject)

        then: "we should get back the expected CDNA sequence"
        assert getCDNASequenceReturnObject.residues != null
        String expectedCDNASequence = "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        assert getCDNASequenceReturnObject.residues == expectedCDNASequence
//
        when: "A request is sent for the genomic sequence of the mRNA"
        String getGenomicSequenceString = getSequenceString.replaceAll("@UNIQUENAME@", uniqueName)
        getGenomicSequenceString = getGenomicSequenceString.replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_GENOMIC.value)
        commandObject = JSON.parse(getGenomicSequenceString) as JSONObject
        JSONObject getGenomicSequenceReturnObject = sequenceService.getSequenceForFeatures(commandObject)
//
        then: "we should get back the expected genomic sequence"
        assert getGenomicSequenceReturnObject.residues != null
        String expectedGenomicSequence = "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGGTAAGACAGAATTCTCTTGTGCACACAGTATAGCTACAGTATACTCAGGGGACGACGAGTGAAATTTTGGCGTGGTTATGGAAAAAAAAAAAGTACAACTCGTAAAGTTGTTGGAGTAAATGAGTCCCGTTTTTTCATGGCGAATCGTACGTCTCCTTTCCACTCGACGACACAGTTTTCAATTTCATATAATAAAAGCGAATGTGAAAATACGATGCGTATGATTCGTTCGAAAAAGAAAGGCAAAAAAAAAAAAAAAAAAAAAATGAAATGATTTTCTCTCCTAATTAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGGTATATTTCTTCTTTTAGTTTTATATATTTTTTTATATATTATATATATACACGAGTTACGAATAATAACAAAATTTTTTTCGAACCTTGAACGTATGATCAAAATTTCTCATTAAAACATTTGGAACGAAAAATGATAATTAAATATCGTAATCGGATGATTGCAACATATTATATAGTAATACATTATACATACCTATTTTATTTATTTTCTTTAGCAAATTATAAAGTTAATTTTATTGAGTTAATTTTACGATGTTTGAATTAATTAGTGGATACGAGATTATATGGTGTAACGTACATATTTTGTAGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        assert getGenomicSequenceReturnObject.residues == expectedGenomicSequence

        when: "A request is sent for the genomic sequence of the mRNA with a flank of 500 bp"
        String getGenomicFlankSequenceString = getSequenceString.replaceAll("@UNIQUENAME@", uniqueName)
        getGenomicFlankSequenceString = getGenomicFlankSequenceString.replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_GENOMIC.value)
        commandObject = JSON.parse(getGenomicFlankSequenceString) as JSONObject
        commandObject.put("flank", 500)
        JSONObject getGenomicFlankSequenceReturnObject = sequenceService.getSequenceForFeatures(commandObject)

        then: "we should get back the expected genomic sequence including the flanking regions"
        assert getGenomicFlankSequenceReturnObject.residues != null
        String expectedGenomicSequenceWithFlank = "GAAACGTACACAAACCGTTTCGACTGTTTGTACGTTACTCTTGAGTTCAAATTTACTCGACCTTTTCGAAACTAGCTGAACTTGAATATACTACTTTTGTACCTCTTAATAATAATTGATAATTATTATAAAATTAACAATAATTAATTCTATATAAAAAAAAAAAAAAAAAATAATAATATATTCCTCTACTATATTAAATTTATTTTTCAAATTACGTGGTTTCTCATATAAAAAAAAATTATTAATTTTATTAATCATAATTTTATATATATATATATATATATATATATATATATATATATAAAAAATAAAAATATATTCTTGAAATGAATAATTTTAAAAAACGATTCGATAATTCGAAAAATTATCATTTTTCATTTCCTATTATGAATGGGAAAAGAAATCGGCATTAATGGTACAGATAGAAGGTACGCATTTTTTCGCGTTAGCTATGGGTGAATCTTTCGACATTACAGTACTGATGGCACAGAAGGTGACAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGGTAAGACAGAATTCTCTTGTGCACACAGTATAGCTACAGTATACTCAGGGGACGACGAGTGAAATTTTGGCGTGGTTATGGAAAAAAAAAAAGTACAACTCGTAAAGTTGTTGGAGTAAATGAGTCCCGTTTTTTCATGGCGAATCGTACGTCTCCTTTCCACTCGACGACACAGTTTTCAATTTCATATAATAAAAGCGAATGTGAAAATACGATGCGTATGATTCGTTCGAAAAAGAAAGGCAAAAAAAAAAAAAAAAAAAAAATGAAATGATTTTCTCTCCTAATTAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGGTATATTTCTTCTTTTAGTTTTATATATTTTTTTATATATTATATATATACACGAGTTACGAATAATAACAAAATTTTTTTCGAACCTTGAACGTATGATCAAAATTTCTCATTAAAACATTTGGAACGAAAAATGATAATTAAATATCGTAATCGGATGATTGCAACATATTATATAGTAATACATTATACATACCTATTTTATTTATTTTCTTTAGCAAATTATAAAGTTAATTTTATTGAGTTAATTTTACGATGTTTGAATTAATTAGTGGATACGAGATTATATGGTGTAACGTACATATTTTGTAGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTGCACATGTAGTACTATAAAATCGTATAAAATTATATTATAAAATCACTTTCTCATACCTTTTTTTCATACTGTTGATTTTATATTAAATTTGTTGTATTCTAAAATTGCTAAAATTATTGATTTGATATAAATTCATTGTTCTTTCCCATAAATAATAATGGTTCGATTATAATATCGAAAGAAATATTTGAATCATGAAAAGGATAAAACACTGATATAAATTCGAATAGTTTTATGTAATATTTTTGTTCACTAACAATATTTTATATAAAATTAATAAATATTAATTATTCTAGAATTGTTACTCTTTTATGTAATAGAATTATTGAAACAGTTAACATAATAGTAAATACGTTAATATACATTCGAATATATTCGGAATATTCTGAAAATACGCCTTAAATTGCACAATTAAGTGTGGTTATCGATAGAGTGCGCGCAATAATGTAACGGTTTCTGATCTGAGTGTTTTTAATCTTTCATGGCGCATGTCGTGTACG"
        assert getGenomicFlankSequenceReturnObject.residues == expectedGenomicSequenceWithFlank
        
        when: "A request is sent for the GFF3 of a gene with UTRs"
        String getGff3String = "{\"operation\":\"get_gff3\",\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\"}"
        getGff3String = getGff3String.replaceAll("@UNIQUENAME@", uniqueName)
        JSONObject inputObject = JSON.parse(getGff3String) as JSONObject
        Sequence refSequence = Sequence.first()
        FeatureLocation.all.each { featureLocation->
            refSequence.addToFeatureLocations(featureLocation)
        }
        
        File gffFile = File.createTempFile("feature", ".gff3")
        sequenceService.getGff3ForFeature(inputObject, gffFile)

        then: "we should get a proper GFF3 for the feature"
        String gffFileText = gffFile.text
        assert gffFileText.length() > 0
        log.debug gffFileText
    }
    

    void "Add 2 genes and get their GFF3"() {
        
        given: "a simple gene model with 1 mRNA, 1 exon and 1 CDS"
        String jsonString1 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1248797,\"fmax\":1249052,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40722-RA\",\"children\":[{\"location\":{\"fmin\":1248797,\"fmax\":1249052,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1248797,\"fmax\":1249052,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonObject1 = JSON.parse(jsonString1) as JSONObject

        String jsonString2 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":729928,\"fmax\":730304,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40827-RA\",\"children\":[{\"location\":{\"fmin\":729928,\"fmax\":730010,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":730296,\"fmax\":730304,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":729928,\"fmax\":730304,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonObject2 = JSON.parse(jsonString2) as JSONObject

        when: "the gene model is added"
        JSONObject returnObject1 = requestHandlingService.addTranscript(jsonObject1)
        JSONObject returnObject2 = requestHandlingService.addTranscript(jsonObject2)
        
        then: "we should see the appropriate model"
        assert Gene.count == 2
        assert MRNA.count == 2

        when: "A request is sent for the GFF3 of a list of genes"
        String uniqueName1 = MRNA.findByName("GB40722-RA-00001").uniqueName
        String uniqueName2 = MRNA.findByName("GB40827-RA-00001").uniqueName
        String getGff3String = "{\"operation\":\"get_gff3\",\"features\":[{\"uniquename\":\"@UNIQUENAME1@\"}, {\"uniquename\":\"@UNIQUENAME2@\"}],\"track\":\"Group1.10\"}"
        getGff3String = getGff3String.replaceAll("@UNIQUENAME1@", uniqueName1)
        getGff3String = getGff3String.replaceAll("@UNIQUENAME2@", uniqueName2)
        JSONObject inputObject = JSON.parse(getGff3String) as JSONObject
        Sequence refSequence = Sequence.first()
        FeatureLocation.all.each { featureLocation->
            refSequence.addToFeatureLocations(featureLocation)
        }
        File gffFile = File.createTempFile("feature", ".gff3");
        sequenceService.getGff3ForFeature(inputObject, gffFile)
        
        then: "we should get a proper GFF3 for each feature"
        String gffFileText = gffFile.text
        assert gffFileText.length() > 0
        log.debug gffFileText
    }

    void "Add a gene and get the complete peptide sequence as well as peptide sequence of each exon"() {
        //GB40744-RA
        given: "a gene model with 6 exons, a 5'UTR and a 3'UTR"
        String jsonString = "{ ${testCredentials}  \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":768063},\"name\":\"GB40744-RA\",\"children\":[{\"location\":{\"fmin\":767945,\"strand\":-1,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":763070},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":763513},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765327,\"strand\":-1,\"fmax\":765472},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765551,\"strand\":-1,\"fmax\":766176},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":766255,\"strand\":-1,\"fmax\":767133},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767207,\"strand\":-1,\"fmax\":767389},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767485,\"strand\":-1,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":763070,\"strand\":-1,\"fmax\":767945},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",\"clientToken\":\"123123\"}"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        
        when: "The gene model is added"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)
        
        then: "we should see the appropriate model"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 6
        
        when: "a request is sent for the peptide sequence"
        String uniqueName = MRNA.findByName("GB40744-RA-00001").uniqueName
        String getPeptideSequenceTemplateString = "{\"operation\":\"get_sequence\",\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\",\"type\":\"@SEQUENCE_TYPE@\"}"
        String getPeptideSequenceString = getPeptideSequenceTemplateString.replaceAll("@UNIQUENAME@", uniqueName)
        getPeptideSequenceString = getPeptideSequenceString.replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_PEPTIDE.value)
        JSONObject commandObject = JSON.parse(getPeptideSequenceString) as JSONObject
        JSONObject getPeptideSequenceReturnObject = sequenceService.getSequenceForFeatures(commandObject)
        
        then: "we should get back the expected peptide sequence"
        assert getPeptideSequenceReturnObject.residues != null
        String expectedPeptideSequence = "MEEMDLGSDESTDKNLHEKSVNRKIESEMIENETEEIKDEFDEEQDKDNDDDDDDDDDDDDEEDADEAEVKILETSLAHNPYDYASHVALINKLQKMGELERLRAAREDMSSKYPLSPDLWLSWMRDEIKLATTIEQKAEVVKLCERAVKDYLAVEVWLEYLQFSIGNMGTEKDAAKNVRHLFERALTDVGLHTIKGAIIWEAFREFEAVLYALIDPLNQAERKEQLERIGNLFKRQLACPLLDMEKTYEEYEAWRHGDGTEAVIDDKIIIGGYNRALSKLNLRLPYEEKIVSAQTENELLDSYKIYLSYEQRNGDPGRITVLYERAITDLSLEMSIWLDYLKYLEENIKIESVLDQVYQRALRNVPWCAKIWQKWIRSYEKWNKSVLEVQTLLENALAAGFSTAEDYRNLWITYLEYLRRKIDRYSTDEGKQLEILRNTFNRACEHLAKSFGLEGDPNCIILQYWARTEAIHANNMEKTRSLWADILSQGHSGTASYWLEYISLERCYGDTKHLRKLFQKALTMVKDWPESIANSWIDFERDEGTLEQMEICEIRTKEKLDKVAEERQKMQQMSNHELSPLQNKKTLKRKQDETGKWKNLGSSPTKITKVEMQIKPKIRESRLNFEKNADSEEQKLKTAPPPGFKMPENEQMEIDNMNEMDDKSTVFISNLDYTASEEEVRNALQPAGPITMFKMIRDYKGRSKGYCYVQLSNIEAIDKALQLDRTPIRGRPMFVSKCDPNRTRGSGFKYSCSLEKNKLFVKGLPVSTTKEDLEEIFKVHGALKEVRIVTYRNGHSKGLAYIEFKDENSAAKALLATDGMKIADKIISVAISQPPERKKVPATEEPLLVKSLGGTTVSRTTFGMPKTLLSMVPRTVKTAATNGSANVPGNGVAPKMNNQDFRNMLLNKK"
        assert getPeptideSequenceReturnObject.residues == expectedPeptideSequence
        
        when: "a request is sent for the peptide sequence of each exon"
        Transcript transcript = Transcript.findByName("GB40744-RA-00001")
        def exons = transcriptService.getSortedExons(transcript, true)
        def exonUniqueNameList = []
        for (Exon exon : exons) {
            exonUniqueNameList.add(exon.uniqueName)
        }

        String getExon1PeptideString = getPeptideSequenceTemplateString.replaceAll("@UNIQUENAME@", exonUniqueNameList[0]).replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_PEPTIDE.value)
        println "GET EXON STRING: ${getExon1PeptideString}"
        commandObject = JSON.parse(getExon1PeptideString) as JSONObject
        String exon1PeptideSequence = sequenceService.getSequenceForFeatures(commandObject).residues

        String getExon2PeptideString = getPeptideSequenceTemplateString.replaceAll("@UNIQUENAME@", exonUniqueNameList[1]).replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_PEPTIDE.value)
        commandObject = JSON.parse(getExon2PeptideString) as JSONObject
        String exon2PeptideSequence = sequenceService.getSequenceForFeatures(commandObject).residues
        
        String getExon3PeptideString = getPeptideSequenceTemplateString.replaceAll("@UNIQUENAME@", exonUniqueNameList[2]).replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_PEPTIDE.value)
        commandObject = JSON.parse(getExon3PeptideString) as JSONObject
        String exon3PeptideSequence = sequenceService.getSequenceForFeatures(commandObject).residues
        
        String getExon4PeptideString = getPeptideSequenceTemplateString.replaceAll("@UNIQUENAME@", exonUniqueNameList[3]).replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_PEPTIDE.value)
        commandObject = JSON.parse(getExon4PeptideString) as JSONObject
        String exon4PeptideSequence = sequenceService.getSequenceForFeatures(commandObject).residues
        
        String getExon5PeptideString = getPeptideSequenceTemplateString.replaceAll("@UNIQUENAME@", exonUniqueNameList[4]).replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_PEPTIDE.value)
        commandObject = JSON.parse(getExon5PeptideString) as JSONObject
        String exon5PeptideSequence = sequenceService.getSequenceForFeatures(commandObject).residues
        
        String getExon6PeptideString = getPeptideSequenceTemplateString.replaceAll("@UNIQUENAME@", exonUniqueNameList[5]).replaceAll("@SEQUENCE_TYPE@", FeatureStringEnum.TYPE_PEPTIDE.value)
        commandObject = JSON.parse(getExon6PeptideString) as JSONObject
        String exon6PeptideSequence = sequenceService.getSequenceForFeatures(commandObject).residues
        
        
        then: "we should get the expected peptide sequence"
        assert exon1PeptideSequence == "MEEMDLGSDESTDKNLHEKSVNRKIESEMIENETEEIKDEFDEEQDKDNDDDDDDDDDDDDEEDADEAEVKILETSLAHNPYDYASHVALINKLQKMGELERLRAAREDMSSKYPLSPDLWLSWMRDEIKLATTIEQKAEVVKLCERAVKDYL"
        assert exon2PeptideSequence == "VEVWLEYLQFSIGNMGTEKDAAKNVRHLFERALTDVGLHTIKGAIIWEAFREFEAVLYAL"
        assert exon3PeptideSequence == "IDPLNQAERKEQLERIGNLFKRQLACPLLDMEKTYEEYEAWRHGDGTEAVIDDKIIIGGYNRALSKLNLRLPYEEKIVSAQTENELLDSYKIYLSYEQRNGDPGRITVLYERAITDLSLEMSIWLDYLKYLEENIKIESVLDQVYQRALRNVPWCAKIWQKWIRSYEKWNKSVLEVQTLLENALAAGFSTAEDYRNLWITYLEYLRRKIDRYSTDEGKQLEILRNTFNRACEHLAKSFGLEGDPNCIILQYWARTEAIHANNMEKTRSLWADILSQGHSGTASYWLEYISLE"
        assert exon4PeptideSequence == "CYGDTKHLRKLFQKALTMVKDWPESIANSWIDFERDEGTLEQMEICEIRTKEKLDKVAEERQKMQQMSNHELSPLQNKKTLKRKQDETGKWKNLGSSPTKITKVEMQIKPKIRESRLNFEKNADSEEQKLKTAPPPGFKMPENEQMEIDNMNEMDDKSTVFISNLDYTASEEEVRNALQPAGPITMFKMIRDYKGRSKGYCYVQLSNI"
        assert exon5PeptideSequence == "EAIDKALQLDRTPIRGRPMFVSKCDPNRTRGSGFKYSCSLEKNKLFVK"
        assert exon6PeptideSequence == "LPVSTTKEDLEEIFKVHGALKEVRIVTYRNGHSKGLAYIEFKDENSAAKALLATDGMKIADKIISVAISQPPERKKVPATEEPLLVKSLGGTTVSRTTFGMPKTLLSMVPRTVKTAATNGSANVPGNGVAPKMNNQDFRNMLLNKK"
    }
    
    void "testing sequence retrieval functions"() {
        //GB40744-RA
        given: "a gene model with 6 exons, a 5'UTR and a 3'UTR"
        String jsonString = "{ ${testCredentials}  \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":768063},\"name\":\"GB40744-RA\",\"children\":[{\"location\":{\"fmin\":767945,\"strand\":-1,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":763070},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":763513},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765327,\"strand\":-1,\"fmax\":765472},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765551,\"strand\":-1,\"fmax\":766176},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":766255,\"strand\":-1,\"fmax\":767133},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767207,\"strand\":-1,\"fmax\":767389},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767485,\"strand\":-1,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":763070,\"strand\":-1,\"fmax\":767945},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",\"clientToken\":\"123123\"}"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        when: "The gene model is added"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "we should see the appropriate model"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 6
   
        when: "we getSequences"
        CRC32 crc = new CRC32();
        crc.update("Group1.1".getBytes());
        String hex = String.format("%08x", crc.getValue())

        then: "it is split appropriately"
        String []dirs = sequenceService.splitStringByNumberOfCharacters(hex, 3)
        assert dirs == ['e15','221','82']


        when: "we call getResiduesForFeature"
        String geneSequence = sequenceService.getResiduesFromFeature(Gene.findByName("GB40744-RA"))
        
        
        Gene gene = Gene.findByName("GB40744-RA")
        def featureLocationList = gene.getFeatureLocation()
        println "featureLocationList : ${featureLocationList}"

        then: "we have it"
        println "Gene Sequence: ${geneSequence}"
        assert geneSequence.length() == 6521
    }
    
    
}
