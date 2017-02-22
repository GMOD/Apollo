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

    }

    @Ignore
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

    }

}
