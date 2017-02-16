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
        variantAnnotationService.createAlterationRepresentation(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert true
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
        variantAnnotationService.createAlterationRepresentation(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert true
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
        variantAnnotationService.createAlterationRepresentation(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert true
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
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentation(mrna, sequenceAlterations)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert true
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

    }

    @IgnoreRest
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
        variantAnnotationService.createAlterationRepresentation(mrna, sequenceAlterations)
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
        variantAnnotationService.createAlterationRepresentation(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert true
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
        variantAnnotationService.createAlterationRepresentation(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert true
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
        variantAnnotationService.createAlterationRepresentation(mrna, sa)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert true
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
        long start = System.currentTimeMillis()
        variantAnnotationService.createAlterationRepresentation(mrna, sequenceAlterations)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert true
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
        variantAnnotationService.createAlterationRepresentation(mrna, sequenceAlterations)
        long end = System.currentTimeMillis()
        println "TOTAL TIME TAKEN: ${end - start} ms"

        then: "nothing"
        assert true
    }

}
