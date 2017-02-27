package org.bbop.apollo

import grails.converters.JSON
import liquibase.change.core.AddDefaultValueChange
import org.bbop.apollo.alteration.SequenceAlterationInContext
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.Strand
import spock.lang.Ignore
import spock.lang.IgnoreRest

class VariantAnnotationServiceIntegrationSpec extends AbstractIntegrationSpec {

    def requestHandlingService
    def featureService
    def featureRelationshipService
    def transcriptService
    def exonService
    def cdsService
    def sequenceService
    def variantAnnotationService



    void "incorporate an insertion AEC into a transcript (fwd)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":403999},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "we should see the insertion"
        assert Insertion.count == 1

        when: "we create"
        MRNA mrna = MRNA.all.get(0)
        SequenceAlteration sa = SequenceAlteration.all.get(0)
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentationForAssemblyErrorCorrection(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAACCCGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
    }


    void "incorporate a deletion AEC into a transcript (fwd)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":403974,\"fmax\":403980},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add a deletion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)

        then: "we should see the deletion"
        assert Deletion.count == 1

        when: "we create"
        MRNA mrna = MRNA.all.get(0)
        SequenceAlteration sa = SequenceAlteration.all.get(0)
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentationForAssemblyErrorCorrection(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCCCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
    }


    void "incorporate a substitution AEC into a transcript (fwd)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"GTG\",\"location\":{\"strand\":1,\"fmin\":403949,\"fmax\":403952},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add a substitution AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the deletion"
        assert Substitution.count == 1

        when: "we create"
        MRNA mrna = MRNA.all.get(0)
        SequenceAlteration sa = SequenceAlteration.all.get(0)
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentationForAssemblyErrorCorrection(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGGTGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
    }

    void "incorporate an insertion, deletion and a substitution AEC into a transcript (fwd)"() {
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":403999},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":403974,\"fmax\":403980},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"GTG\",\"location\":{\"strand\":1,\"fmin\":403949,\"fmax\":403952},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add all the AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we create"
        MRNA mrna = MRNA.all.get(0)
        def sequenceAlterations = SequenceAlteration.all
        sequenceAlterations.sort { a,b -> a.fmin <=> b.fmin }
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentationForAssemblyErrorCorrection(mrna, sequenceAlterations)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGGTGTCGATCGTGATGATAAAACATCCCTTCAAAGCTTGGAACAACCCGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
    }

    void "incorporate an insertion variant into a transcript (fwd)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionVariantString = " { ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":404024,\"fmax\":404024},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "we should see the insertion"
        assert Insertion.count == 1
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAATTTGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
    }

    void "incorporate an deletion variant into a transcript (fwd)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addDeletionVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":404005},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addDeletionVariantString) as JSONObject)

        then: "we should see the insertion"
        assert Deletion.count == 1
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"

    }

    void "incorporate an substitution variant into a transcript (fwd)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addSubstitutionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"T\",\"location\":{\"strand\":1,\"fmin\":403974,\"fmax\":403975},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addSubstitutionVariantString) as JSONObject)

        then: "we should see the insertion"
        assert Substitution.count == 1
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCTCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"

    }

    @Ignore
    void "incorporate an insertion, deletion and a substitution variant into a transcript (fwd)"() {
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionVariantString = " { ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":404024,\"fmax\":404024},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"
        String addDeletionVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":404005},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"
        String addSubstitutionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"T\",\"location\":{\"strand\":1,\"fmin\":403974,\"fmax\":403975},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add all the AECs"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)
        requestHandlingService.addVariant(JSON.parse(addDeletionVariantString) as JSONObject)
        requestHandlingService.addVariant(JSON.parse(addSubstitutionVariantString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we create"
        MRNA mrna = MRNA.all.get(0)
        def sequenceAlterations = SequenceAlteration.all
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentationForVariant(mrna, sequenceAlterations)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert true
    }


    void "incorporate an insertion AEC into a transcript (rev)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "we should see the insertion"
        assert Insertion.count == 1

        when: "we create"
        MRNA mrna = MRNA.all.get(0)
        def exons = transcriptService.getSortedExons(mrna, false)
        SequenceAlteration sa = SequenceAlteration.all.get(0)
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentationForAssemblyErrorCorrection(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAAAAGAATATTCAGATTGAAAGAATATTACAATGATCCAAGACAAATTGAAAATTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
    }


    void "incorporate a deletion AEC into a transcript (rev)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598824,\"fmax\":598830},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add a deletion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)

        then: "we should see the deletion"
        assert Deletion.count == 1

        when: "we create"
        MRNA mrna = MRNA.all.get(0)
        SequenceAlteration sa = SequenceAlteration.all.get(0)
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentationForAssemblyErrorCorrection(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAGAATATTCAGATTGAAAGACAATGATCCAAGACAAATTGAAAATTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
    }

    void "incorporate a substitution AEC into a transcript (rev)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598802},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add a substitution AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the deletion"
        assert Substitution.count == 1

        when: "we create"
        MRNA mrna = MRNA.all.get(0)
        SequenceAlteration sa = SequenceAlteration.all.get(0)
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentationForAssemblyErrorCorrection(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAGAATATTCAGATTGAAAGAATATTACAATGATCCAAGACAAATTGAGGGTTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
    }


    void "incorporate an insertion, deletion and a substitution AEC into a transcript (rev)"() {
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598824,\"fmax\":598830},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598802},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add all the AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we create"
        MRNA mrna = MRNA.all.get(0)
        def sequenceAlterations = SequenceAlteration.all
        sequenceAlterations.sort{ a,b -> a.fmin <=> b.fmin }
        println "SEQ ALT SIZE: ${sequenceAlterations.size()} and ORDER:"
        println "${sequenceAlterations.each {println it.class}}"
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentationForAssemblyErrorCorrection(mrna, sequenceAlterations)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAAAAGAATATTCAGATTGAAAGACAATGATCCAAGACAAATTGAGGGTTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
    }

    void "incorporate an insertion variant into a transcript (rev)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"AAA\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "we should see the insertion"
        assert Insertion.count == 1
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATTTTAGAATATTCAGATTGAAAGAATATTACAATGATCCAAGACAAATTGAAAATTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598161
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }

    void "incorporate a deletion variant into a transcript (rev)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addDeletionVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598824,\"fmax\":598830},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add a deletion AEC"
        requestHandlingService.addVariant(JSON.parse(addDeletionVariantString) as JSONObject)

        then: "we should see the deletion"
        assert Deletion.count == 1
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAGAATATTCAGATTGAAAGACAATGATCCAAGACAAATTGAAAATTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598161
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }

    void "incorporate a substitution variant into a transcript (rev)"() {

        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addSubstitutionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"C\",\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598800},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add a substitution AEC"
        requestHandlingService.addVariant(JSON.parse(addSubstitutionVariantString) as JSONObject)

        then: "we should see the deletion"
        assert Substitution.count == 1
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAGAATATTCAGATTGAAAGAATATTACAATGATCCAAGACAAATTGAAAGTTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598161
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }

    @Ignore
    void "incorporate an insertion, deletion and a substitution variant into a transcript (rev)"() {
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"AAA\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"
        String addDeletionVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598824,\"fmax\":598830},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"
        String addSubstitutionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"C\",\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598800},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add all the AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we create"
        MRNA mrna = MRNA.all.get(0)
        def sequenceAlterations = SequenceAlteration.all
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentationForVariant(mrna, sequenceAlterations)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert true
    }

    void "incorporate an insertion AEC and an insertion variant into a transcript (fwd)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":403999},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = " { ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":404024,\"fmax\":404024},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAACCCGTTTTTAATGAGGTATTGAACAGAATTTGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405154
    }


    void "incorporate an deletion AEC and an insertion variant into a transcript (fwd)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":405074,\"fmax\":405079},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":404024,\"fmax\":404024},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAATTTGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405111
    }


    void "incorporate an insertion AEC and an insertion variant into a transcript (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"GAT\",\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598799},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"AAA\",\"location\":{\"strand\":1,\"fmin\":598824,\"fmax\":598824},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAGAATATTCAGATTGAAAGAATATTTTTACAATGATCCAAGACAAATTGAAAAATCTTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598161
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }

    void "incorporate an insertion AEC (2) and an insertion variant into a transcript (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"AA\",\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598799},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"AAA\",\"location\":{\"strand\":1,\"fmin\":598824,\"fmax\":598824},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAGAATATTCAGATTGAAAGAATATTTTTACAATGATCCAAGACAAATTGAAAATTTTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598268
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }


    void "incorporate an deletion AEC (2) and an insertion variant into a transcript (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598208,\"fmax\":598209},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"TTC\",\"location\":{\"strand\":1,\"fmin\":598249,\"fmax\":598249},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAGAATATTCAGATTGAAAGAATATTACAATGATCCAAGACAAATTGAAAATTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACGAAATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598205
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }


    void "incorporate an insertion and deletion AEC and an insertion variant into a transcript (fwd)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":403999},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":403974,\"fmax\":403980},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = " { ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":404024,\"fmax\":404024},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 2

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCCCTTCAAAGCTTGGAACAACCCGTTTTTAATGAGGTATTGAACAGAATTTGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405154
    }


    void "incorporate an insertion and deletion AEC and an insertion variant into a transcript (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"GAT\",\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598799},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = " { ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598824,\"fmax\":598833},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"TGT\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 2

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATACAAGAATATTCAGATTGAACAATGATCCAAGACAAATTGAAAAATCTTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598161
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }

    void "incorporate an insertion, deletion and substitution AEC and an insertion variant into a transcript (fwd)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":403999},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":403974,\"fmax\":403980},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"GTG\",\"location\":{\"strand\":1,\"fmin\":403949,\"fmax\":403952},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = " { ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":404024,\"fmax\":404024},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGGTGTCGATCGTGATGATAAAACATCCCTTCAAAGCTTGGAACAACCCGTTTTTAATGAGGTATTGAACAGAATTTGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405154

    }


    void "incorporate an insertion, deletion and substitution AEC and an insertion variant into a transcript (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"GAT\",\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598799},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = " { ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598824,\"fmax\":598833},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"GGT\",\"location\":{\"strand\":1,\"fmin\":598840,\"fmax\":598843},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"TGT\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATACAAGAATAACCAGATTGAACAATGATCCAAGACAAATTGAAAAATCTTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598161
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }

    void "incorporate an insertion, deletion and substitution AEC (unordered) and an insertion variant into a transcript (fwd)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":403999},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":404029,\"fmax\":404035},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"GTG\",\"location\":{\"strand\":1,\"fmin\":403949,\"fmax\":403952},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = " { ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":404024,\"fmax\":404024},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGGTGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAACCCGTTTTTAATGAGGTATTGAACAGAATTTGTTGCGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405154
    }

    void "incorporate an insertion, deletion and substitution AEC (unordered) and an insertion variant into a transcript (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"GAT\",\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598799},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598786,\"fmax\":598789},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"GGT\",\"location\":{\"strand\":1,\"fmin\":598840,\"fmax\":598843},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"TGT\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATACAAGAATAACCAGATTGAAAGAATATTACAATGATCCAAGACAAATTGAAAAATCTTCTGAAGATAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598161
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }


    void "incorporate an insertion (on exon 1), deletion (on exon 1) and substitution AEC (on exon 2) and an substitution variant (on exon 2) into a transcript (fwd)"() {
        // simplified test for inferVariantEffects()
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"TAC\",\"location\":{\"strand\":1,\"fmin\":403974,\"fmax\":403974},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":404005},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":405099,\"fmax\":405102},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"C\",\"location\":{\"strand\":1,\"fmin\":405074,\"fmax\":405075},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addSubstitutionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCTACGCCTAACCTTCAAAGCTTGGAACAAAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAACAATATGTTTGTCTGTAGCTGTAGCCCCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405154
    }



    void "incorporate an insertion (on exon 1), deletion (on exon 1) and substitution AEC (on exon 2) and an insertion variant (on exon 2) into a transcript (fwd)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"TAC\",\"location\":{\"strand\":1,\"fmin\":403974,\"fmax\":403974},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":404005},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":405099,\"fmax\":405102},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        // insertion variant introduces a frameshift
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCAA\",\"location\":{\"strand\":1,\"fmin\":405074,\"fmax\":405074},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCTACGCCTAACCTTCAAAGCTTGGAACAAAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAACCAAAAATATGTTTGTCTGTAGCTGTAGCCCCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405111
    }

    void "incorporate an insertion (on exon 1), deletion (on exon 1) and substitution AEC (on exon 2) and a substitution variant (on exon 2, that introduces an in-frame stop) into a transcript (fwd)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"TAC\",\"location\":{\"strand\":1,\"fmin\":403974,\"fmax\":403974},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":404005},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":405099,\"fmax\":405102},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        // substitution variant introduces an in-frame stop
        String addSubstitutionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"A\",\"location\":{\"strand\":1,\"fmin\":405081,\"fmax\":405082},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addSubstitutionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCTACGCCTAACCTTCAAAGCTTGGAACAAAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGATTGTCTGTAGCTGTAGCCCCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405082
    }


    void "incorporate an insertion (on exon 1), deletion (on exon 1) and substitution AEC (on exon 2) and an insertion variant (on exon 2) into a transcript (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CTT\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598824,\"fmax\":598830},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"AGG\",\"location\":{\"strand\":1,\"fmin\":598199,\"fmax\":598202},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":598249,\"fmax\":598249},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAAGAGAATATTCAGATTGAAAGACAATGATCCAAGACAAATTGAAAATTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACAAAATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGCCTGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598161
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924

    }


    void "incorporate an insertion (on exon 1), deletion (on exon 1) and substitution AEC (on exon 2) and a substitution variant (on exon 2, that introduces an in-frame stop) into a transcript (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addTranscript2String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CTT\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598824,\"fmax\":598830},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionString = "{ ${testCredentials} \"features\":[{\"residues\":\"AGG\",\"location\":{\"strand\":1,\"fmin\":598199,\"fmax\":598202},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        // Substitution variant that introduces an in-frame stop
        String addSubstitutionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"A\",\"location\":{\"strand\":1,\"fmin\":598232,\"fmax\":598233},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(addTranscript2String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 2

        when: "we add the insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitutionString) as JSONObject)

        then: "we should see the alterations"
        assert SequenceAlteration.count == 3

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addSubstitutionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAAGAGAATATTCAGATTGAAAGACAATGATCCAAGACAAATTGAAAATTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCATGAATATTTATGGTCTTTAAAACTTTAGAAGCCTGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598230
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924

    }

    void "incorporate an insertion variant (exon 2) into a transcript (fwd) and check the CDS"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"GTAA\",\"location\":{\"strand\":1,\"fmin\":405074,\"fmax\":405074},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAGTAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405111
    }

    void "incorporate a deletion variant (exon 2) into a transcript (fwd) and check the CDS"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addDeletionVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":405108,\"fmax\":405109},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addDeletionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405110

    }

    void "incorporate an substitution variant (exon 2) into a transcript (fwd) and check the CDS"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addSubstitutionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"G\",\"location\":{\"strand\":1,\"fmin\":405125,\"fmax\":405126},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addSubstitutionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTGACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405127

    }

    void "incorporate an insertion variant (exon 2) into a transcript (rev) and check the CDS"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"TCA\",\"location\":{\"strand\":1,\"fmin\":598227,\"fmax\":598227},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAGAATATTCAGATTGAAAGAATATTACAATGATCCAAGACAAATTGAAAATTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATGATTTATGGTCTTTAAAACTTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598227
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }

    void "incorporate a deletion variant (exon 2) into a transcript (rev) and check the CDS"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addDeletionVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598207,\"fmax\":598208},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addDeletionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAGAATATTCAGATTGAAAGAATATTACAATGATCCAAGACAAATTGAAAATTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTAGAAGAATGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598205
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }

    void "incorporate a substitution variant (exon 2) into a transcript (rev) and check the CDS"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addSubstitutionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"TTA\",\"location\":{\"strand\":1,\"fmin\":598191,\"fmax\":598194},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add an insertion variant"
        requestHandlingService.addVariant(JSON.parse(addSubstitutionVariantString) as JSONObject)

        then: "sequence in sequenceTrace should be as expected"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATAGAATATTCAGATTGAAAGAATATTACAATGATCCAAGACAAATTGAAAATTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACATCAAACATTTTTTGAAGTTTCAAGAATATTTATGGTCTTTAAAACTTTAGAAGAATGTCAATAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598191
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }

    void "AEC + substitution variant + AEC (fwd)"() {
        // test created to test out a newer implementation of inferVariantEffects() that takes before and after alterationNodeList
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertion1String = "{ ${testCredentials} \"features\":[{\"residues\":\"TGT\",\"location\":{\"strand\":1,\"fmin\":403949,\"fmax\":403949},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion2String = "{ ${testCredentials} \"features\":[{\"residues\":\"TTC\",\"location\":{\"strand\":1,\"fmin\":405124,\"fmax\":405124},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"T\",\"location\":{\"strand\":1,\"fmin\":405073,\"fmax\":405074},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"substitution\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add two insertion AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)

        then: "we see the AECs"
        assert Insertion.count == 2

        when: "we add a substitution variant"
        requestHandlingService.addVariant(JSON.parse(addSubstitutionVariantString) as JSONObject)

        then: "nothing"
        assert Substitution.count == 1
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGTGTATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATATAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTTCTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405076
    }

    void "Combination of AECs + substitution variant (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertion1String = "{ ${testCredentials} \"features\":[{\"residues\":\"TTG\",\"location\":{\"strand\":1,\"fmin\":598199,\"fmax\":598199},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletion1String = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598216,\"fmax\":598219},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion2String = "{ ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":598255,\"fmax\":598255},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletion2String = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598805},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion3String = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addSubstitutionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"A\",\"location\":{\"strand\":1,\"fmin\":598241,\"fmax\":598242},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add two insertion AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion3String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion2String) as JSONObject)

        then: "we see the AECs"
        assert SequenceAlteration.count == 5

        when: "we add a substitution variant"
        requestHandlingService.addVariant(JSON.parse(addSubstitutionVariantString) as JSONObject)

        then: "nothing"
        assert Substitution.count == 1
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATGGGAGAATATTCAGATTGAAAGAATATTACAATGATCCAAGACAAATTTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACAAAATCAAACATTTTTTTAAGTTTCAAGAATATTTATGGTTAAAACTTTAGAAGAATCAAGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598239
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924

    }

    void "AEC + insertion variant + AEC (fwd)"() {
        // test created to test out a newer implementation of inferVariantEffects() that takes before and after alterationNodeList
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertion1String = "{ ${testCredentials} \"features\":[{\"residues\":\"TGT\",\"location\":{\"strand\":1,\"fmin\":403949,\"fmax\":403949},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion2String = "{ ${testCredentials} \"features\":[{\"residues\":\"TTC\",\"location\":{\"strand\":1,\"fmin\":405124,\"fmax\":405124},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"AA\",\"location\":{\"strand\":1,\"fmin\":405104,\"fmax\":405104},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add two insertion AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)

        then: "we see the AECs"
        assert Insertion.count == 2

        when: "we add a substitution variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "nothing"
        assert Insertion.count == 3
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGTGTATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATAACTTGTAACAGTTTAGACATTTTCTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405104
    }


    void "AEC + ATG insertion variant + AEC (fwd)"() {
        // test where the final cdsFmin is right at the insertion variant fmin
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":621650,\"fmax\":622330},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":623090,\"fmax\":623213},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":624547,\"fmax\":624610},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":624680,\"fmax\":624743},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":624885,\"fmax\":624927},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":625015,\"fmax\":625090},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":627962,\"fmax\":628275},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":622270,\"fmax\":628037},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40821-RA\",\"location\":{\"strand\":1,\"fmin\":621650,\"fmax\":628275},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertion1String = " { ${testCredentials} \"features\":[{\"residues\":\"TTG\",\"location\":{\"strand\":1,\"fmin\":622299,\"fmax\":622299},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion2String = " { ${testCredentials} \"features\":[{\"residues\":\"ATA\",\"location\":{\"strand\":1,\"fmin\":624574,\"fmax\":624574},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"ATG\",\"location\":{\"strand\":1,\"fmin\":622231,\"fmax\":622231},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add two insertion AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)

        then: "we see the AECs"
        assert Insertion.count == 2

        when: "we add a substitution variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "nothing"
        assert Insertion.count == 3
        assert variantAnnotationService.sequenceTrace.last() == "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACTTTTTCTTCTCGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGATGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGAAATCGCAAGGATCCACAGATATTATTGCAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGATAAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 622231
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 628037
    }


    void "AEC + Deletion variant + AEC (fwd)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":621650,\"fmax\":622330},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":623090,\"fmax\":623213},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":624547,\"fmax\":624610},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":624680,\"fmax\":624743},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":624885,\"fmax\":624927},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":625015,\"fmax\":625090},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":627962,\"fmax\":628275},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":622270,\"fmax\":628037},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40821-RA\",\"location\":{\"strand\":1,\"fmin\":621650,\"fmax\":628275},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertion1String = " { ${testCredentials} \"features\":[{\"residues\":\"TTG\",\"location\":{\"strand\":1,\"fmin\":622299,\"fmax\":622299},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion2String = " { ${testCredentials} \"features\":[{\"residues\":\"ATA\",\"location\":{\"strand\":1,\"fmin\":624574,\"fmax\":624574},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionVariantString = " { ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":622282,\"fmax\":622287},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add two insertion AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)

        then: "we see the AECs"
        assert Insertion.count == 2

        when: "we add a substitution variant"
        requestHandlingService.addVariant(JSON.parse(addDeletionVariantString) as JSONObject)

        then: "nothing"
        assert Deletion.count == 1
        assert variantAnnotationService.sequenceTrace.last() == "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACTTTTTCTTCTCGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGAAATCGCACCACAGATATTATTGCAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGATAAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 622265
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 628037


    }

    @Ignore
    void "AEC + ATG insertion variant + AEC (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":969795,\"fmax\":970639},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":970738,\"fmax\":970918},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":971094,\"fmax\":971317},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":972090,\"fmax\":972407},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":972657,\"fmax\":972870},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":973808,\"fmax\":973976},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":970501,\"fmax\":973847},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40734-RA\",\"location\":{\"strand\":-1,\"fmin\":969795,\"fmax\":973976},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertion1String = "{ ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":970449,\"fmax\":970449},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion2String = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":970824,\"fmax\":970824},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"CAT\",\"location\":{\"strand\":1,\"fmin\":973850,\"fmax\":973850},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add two insertion AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)

        then: "we see the AECs"
        assert Insertion.count == 2

        when: "we add a substitution variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "nothing"
        assert Insertion.count == 3
        assert variantAnnotationService.sequenceTrace.last() == "CCTATCTTTTAATTAACAAAATTTTTGTTAACAAATTTTCATCAATATGATATTTATACATATATGTTAATAAGAATACATTTATTTCTACTTTTTAAATTTTCTAGTATTATTTACGAAACTTAAATGATAATGAAGCGACTAGGAGATAAATGTTATTGCAAAGACAAGGTATCATCGCCTATCACCAAATACACGGAGCTAGAGCCAGCACTCTGTCAGGAGTTAAAAAAAATTGTGGAGGCCGTTGTAGTTCCAGGAAAAGGATTACTAGCTTGTGACGAGTCTCCTGCATCGTTGCAAAAGCGTTTCGATGAGCTCGGTGTGGAAAATACCGAAACTAATCGGCGCAATTATCGACAAATGTTATTCTCAGCTGATAAGTCTGAATTTTCAAAATGCATAAGTGGTGTTATTCTGCATCATGAGACAGTTTATGAAAAAACGACTGATGGCATCGACATGATCGAACTGTTACGCCAAAGGAATGTCGTGCCCGGTATTAAGGTCGACAAGGGATTGGTCCCTCTTTTTGGCGCGAAAAACGAAAATACCACGGAAGGTCTGGACAATTTGCAAGAAAGATGCATTCAATATAAGAGAGACGGCTGCCATTTTGCCAAATGGAGATGCACATTCAGCATCACGGAGACCACGCCGTCTCAGTTGGCCATGGTTACGAATGCGGATGTCCTCGCAAGATACGCCACTATATGTCAGAGTGCACGAATAGTACCCATAATAGAACCAGAAATATTAAGCCCAGGTGACCATGGAATAAACAAAGCATTAGAAGTTCATGAAGAGGTGTTATCTAACGTAATGCGTGCTTTACACCAACATCGAGTTTACCTCGAAGGTATGATATTGAAATCTGCTATGGTTTTGTCAGGAAGGAAAGAAGAAGTTAATTGTACACCGCAGATTGTGGCTGAACATACGGTATTAGCTTTACAAAGAACAATACCACCAGCGGTGCCAGCTGTCTTATTTCTAAGTGGTGGTCAAACGGACGAAGGGGATTCTGTGATAAATTTAAATGCTATTGTAAATTACGAAGGGAAAAAACCTTGGCAATTGACGTACTGTTATGGGCGTGCCTTACAAAACGAGGTAATGAAAATTTGGAAAGGAAATTCTGCAAAGGTAGCTGAAGCTCAAACCCTGCTTTTGAAAAAGATAAAACTTGCCTCCCAAGCTGCACTAGGACAATTAGAAGTGGAAAACAGTGTGTGCACTAAATAATCCTCTGGTTAATTAACAAATACTCGATTAAAAGACACCAATCATATATATAAAATATATATATATACCTGTATAAATGAATATTGTAATCTGTATAACGAATGTTATGTAATACTTATCTCCAATTTTCAATTGCATTATGCAACTCATCAATCTGATATCAAATACAATACGATTGTCTATTGATTCTCTCGAATATCTTCGATGCAATCGATTTCGACTAATTCATCGACGAGATCATTACCGAGTCATGATGAGATGACATCTTAACATTATTATCAATGAATTTTGTCACGTAAATGATAAAGAATATTTCAACGAACATTTTATCACAAGGATACGTCAACCTTTATAGTGAAATGATGGTATGAGTAAATTCGGCGAATGTCATACAAGGTGCCCTTGATTGACCTCTAAAACAGGGATTCTCAATCGGAGGCTTCTGCGATAATACGTGAGAGTTGGGTCGTACGTTACTCGAAAATTTACCTCACTCTAGTGGTCGATACGGCGTCATTTCCATGGATCCCCACCAACCACGTTCTCTTATTCTTCTAGGCATTATAGACCGGATCATCAACACATGTGCTCGACACCGTCGTAACTATAGAGCATTTATTCAATGTGACTTGATAAACTTATTATATTTGATTCGTTATTTTGATTAAAAAAAATATATAATGTAATATAAAAAATAAATATAAATATA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 970501
        // the below assertion fails for now; revisit later
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 973850
    }

    void "AEC + deletion variant + AEC (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":969795,\"fmax\":970639},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":970738,\"fmax\":970918},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":971094,\"fmax\":971317},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":972090,\"fmax\":972407},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":972657,\"fmax\":972870},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":973808,\"fmax\":973976},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":970501,\"fmax\":973847},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40734-RA\",\"location\":{\"strand\":-1,\"fmin\":969795,\"fmax\":973976},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertion1String = "{ ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":970449,\"fmax\":970449},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion2String = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":970824,\"fmax\":970824},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":970524,\"fmax\":970531},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add two insertion AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)

        then: "we see the AECs"
        assert Insertion.count == 2

        when: "we add a substitution variant"
        requestHandlingService.addVariant(JSON.parse(addDeletionVariantString) as JSONObject)

        then: "nothing"
        assert Deletion.count == 1
        assert variantAnnotationService.sequenceTrace.last() == "CCTATCTTTTAATTAACAAAATTTTTGTTAACAAATTTTCATCAATATGATATTTATACATATATGTTAATAAGAATACATTTATTTCTACTTTTTAAATTTTCTAGTATTATTTACGAAACTTAAATAATGAAGCGACTAGGAGATAAATGTTATTGCAAAGACAAGGTATCATCGCCTATCACCAAATACACGGAGCTAGAGCCAGCACTCTGTCAGGAGTTAAAAAAAATTGTGGAGGCCGTTGTAGTTCCAGGAAAAGGATTACTAGCTTGTGACGAGTCTCCTGCATCGTTGCAAAAGCGTTTCGATGAGCTCGGTGTGGAAAATACCGAAACTAATCGGCGCAATTATCGACAAATGTTATTCTCAGCTGATAAGTCTGAATTTTCAAAATGCATAAGTGGTGTTATTCTGCATCATGAGACAGTTTATGAAAAAACGACTGATGGCATCGACATGATCGAACTGTTACGCCAAAGGAATGTCGTGCCCGGTATTAAGGTCGACAAGGGATTGGTCCCTCTTTTTGGCGCGAAAAACGAAAATACCACGGAAGGTCTGGACAATTTGCAAGAAAGATGCATTCAATATAAGAGAGACGGCTGCCATTTTGCCAAATGGAGATGCACATTCAGCATCACGGAGACCACGCCGTCTCAGTTGGCCATGGTTACGAATGCGGATGTCCTCGCAAGATACGCCACTATATGTCAGAGTGCACGAATAGTACCCATAATAGAACCAGAAATATTAAGCCCAGGTGACCATGGAATAAACAAAGCATTAGAAGTTCATGAAGAGGTGTTATCTAACGTAATGCGTGCTTTACACCAACATCGAGTTTACCTCGAAGGTATGATATTGAAATCTGCTATGGTTTTGTCAGGAAGGAAAGAAGAAGTTAATTGTACACCGCAGATTGTGGCTGAACATACGGTATTAGCTTTACAAAGAACAATACCACCAGCGGTGCCAGCTGTCTTATTTCTAAGTGGTGGTCAAACGGACGAAGGGGATTCTGTGATAAATTTAAATGCTATTGTAAATTACGAAGGGAAAAAACCTTGGCAATTGACGTACTGTTATGGGCGTGCCTTACAAAACGAGGTAATGAAAATTTGGAAAGGAAATTCTGCAAAGGTAGCTGAAGCTCAAACCCTGCTTTTGAAAAAGATAAAACTTGCCTCCCAAGCTGCACTAGGACAATTAAAAACAGTGTGTGCACTAAATAATCCTCTGGTTAATTAACAAATACTCGATTAAAAGACACCAATCATATATATAAAATATATATATATACCTGTATAAATGAATATTGTAATCTGTATAACGAATGTTATGTAATACTTATCTCCAATTTTCAATTGCATTATGCAACTCATCAATCTGATATCAAATACAATACGATTGTCTATTGATTCTCTCGAATATCTTCGATGCAATCGATTTCGACTAATTCATCGACGAGATCATTACCGAGTCATGATGAGATGACATCTTAACATTATTATCAATGAATTTTGTCACGTAAATGATAAAGAATATTTCAACGAACATTTTATCACAAGGATACGTCAACCTTTATAGTGAAATGATGGTATGAGTAAATTCGGCGAATGTCATACAAGGTGCCCTTGATTGACCTCTAAAACAGGGATTCTCAATCGGAGGCTTCTGCGATAATACGTGAGAGTTGGGTCGTACGTTACTCGAAAATTTACCTCACTCTAGTGGTCGATACGGCGTCATTTCCATGGATCCCCACCAACCACGTTCTCTTATTCTTCTAGGCATTATAGACCGGATCATCAACACATGTGCTCGACACCGTCGTAACTATAGAGCATTTATTCAATGTGACTTGATAAACTTATTATATTTGATTCGTTATTTTGATTAAAAAAAATATATAATGTAATATAAAAAATAAATATAAATATA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 970485
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 973847
    }



    void "Combination of AECs + insertion variant (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertion1String = "{ ${testCredentials} \"features\":[{\"residues\":\"TTG\",\"location\":{\"strand\":1,\"fmin\":598199,\"fmax\":598199},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletion1String = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598216,\"fmax\":598219},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion2String = "{ ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":598255,\"fmax\":598255},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletion2String = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598805},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion3String = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertionVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"TT\",\"location\":{\"strand\":1,\"fmin\":598244,\"fmax\":598244},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add two insertion AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion3String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion2String) as JSONObject)

        then: "we see the AECs"
        assert SequenceAlteration.count == 5

        when: "we add a substitution variant"
        requestHandlingService.addVariant(JSON.parse(addInsertionVariantString) as JSONObject)

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATGGGAGAATATTCAGATTGAAAGAATATTACAATGATCCAAGACAAATTTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACAAAATCAAACATTTTAATTGAAGTTTCAAGAATATTTATGGTTAAAACTTTAGAAGAATCAAGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598244
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }

    void "AEC + deletion variant + AEC (fwd)"() {
        // test created to test out a newer implementation of inferVariantEffects() that takes before and after alterationNodeList
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertion1String = "{ ${testCredentials} \"features\":[{\"residues\":\"TGT\",\"location\":{\"strand\":1,\"fmin\":403949,\"fmax\":403949},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion2String = "{ ${testCredentials} \"features\":[{\"residues\":\"TTC\",\"location\":{\"strand\":1,\"fmin\":405124,\"fmax\":405124},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":405115,\"fmax\":405116},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add two insertion AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)

        then: "we see the AECs"
        assert Insertion.count == 2

        when: "we add a substitution variant"
        requestHandlingService.addVariant(JSON.parse(addDeletionVariantString) as JSONObject)

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGTGTATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTAGACATTTTCTCACGGTTATTGGAGAGTCTAATTGCATAA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 403882
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 405119
    }

    void "Combination of AECs + deletion variant (rev)"() {
        String addTranscript1String = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598782,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40755-RA\",\"location\":{\"strand\":-1,\"fmin\":598161,\"fmax\":598924},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addInsertion1String = "{ ${testCredentials} \"features\":[{\"residues\":\"TTG\",\"location\":{\"strand\":1,\"fmin\":598199,\"fmax\":598199},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletion1String = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598216,\"fmax\":598219},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion2String = "{ ${testCredentials} \"features\":[{\"residues\":\"TTT\",\"location\":{\"strand\":1,\"fmin\":598255,\"fmax\":598255},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletion2String = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598799,\"fmax\":598805},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addInsertion3String = "{ ${testCredentials} \"features\":[{\"residues\":\"CCC\",\"location\":{\"strand\":1,\"fmin\":598849,\"fmax\":598849},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addDeletionVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":598233,\"fmax\":598235},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscript1String) as JSONObject)

        then: "we see the transcript"
        assert MRNA.count == 1

        when: "we add two insertion AECs"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion3String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion1String) as JSONObject)
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion2String) as JSONObject)

        then: "we see the AECs"
        assert SequenceAlteration.count == 5

        when: "we add a substitution variant"
        requestHandlingService.addVariant(JSON.parse(addDeletionVariantString) as JSONObject)

        then: "nothing"
        assert variantAnnotationService.sequenceTrace.last() == "ATGAAAGTGCAGATCACTAAAGATCATTCTTCTCATTTGCATTTATTCTACACTGAAACAGGAGAGAAAAGAAATGGGAGAATATTCAGATTGAAAGAATATTACAATGATCCAAGACAAATTTCTGAAGATGTCAGAGAAGTTCTATTGATAATGGAATTACAAAATCAAACATTTTTTGAAGTTTAGAATATTTATGGTTAAAACTTTAGAAGAATCAAGTCAAGAATACCTTGATAAGTATCCTCAAGTATCTTGA"
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmin == 598231
        assert variantAnnotationService.cdsLocationInfoTrace.last().fmax == 598924
    }

}
