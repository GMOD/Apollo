package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.Strand

class VariantServiceIntegrationSpec extends AbstractIntegrationSpec {

    def requestHandlingService
    def featureService
    def featureRelationshipService
    def transcriptService
    def exonService
    def cdsService
    def sequenceService

    void "Add a transcript and a SNV and observe the effect the SNV has on the transcript"() {
        given: "a transcript and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747760},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747822,\"fmax\":747894},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747946,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40829-RA\",\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"alternate_alleles\":[{\"bases\":\"T\"}],\"location\":{\"strand\":1,\"fmin\":747724,\"fmax\":747725},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"},\"reference_bases\":\"C\"}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a overlapping SNV"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the SNV"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

    }

    void "Add a transcript and an insertion variant and observe the effect the variant has on the transcript"() {
        given: "a transcript and an insertion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747760},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747822,\"fmax\":747894},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747946,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40829-RA\",\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"alternate_alleles\":[{\"bases\":\"GCAT\"}],\"location\":{\"strand\":1,\"fmin\":747749,\"fmax\":747750},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"},\"reference_bases\":\"G\"}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"
        String flipStrandString = "{ ${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"flip_strand\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)
        requestHandlingService.flipStrand(JSON.parse(flipStrandString.replace("@UNIQUENAME@", MRNA.all.get(0).uniqueName)) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a overlapping Indel"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the insertion variant"
        assert Insertion.all.size() == 1
        assert Allele.all.size() == 1
    }

    void "Add a transcript and a deletion variant and observe the effect the variant has on the transcript"() {
        given: "a transcript and a deletion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747760},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747822,\"fmax\":747894},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747946,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40829-RA\",\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"alternate_alleles\":[{\"bases\":\"A\"}],\"location\":{\"strand\":1,\"fmin\":747718,\"fmax\":747723},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"},\"reference_bases\":\"AAAAA\"}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"
        String flipStrandString = "{ ${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"flip_strand\"}"

        when: "we add the transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a overlapping deletion variant"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the deletion variant"
        assert Deletion.all.size() == 1
        assert Allele.all.size() == 1
    }

    void "Add a de-novo deletion variant"() {
        given: "a de-novo deletion variant"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":747724,\"fmax\":747729},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add the variant"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Deletion.all.size() == 1
        assert Allele.all.size() == 1

        SequenceAlteration variant = Deletion.all.iterator().next()

        assert variant.referenceBases == "GCATGT"
        assert variant.alterationResidue == "CATGT"
        assert variant.alternateAlleles[0].bases == "G"
    }

    void "Add a de-novo insertion variant"() {
        given: "a de-novo insertion variant"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"AAAAA\",\"location\":{\"strand\":1,\"fmin\":952874,\"fmax\":952874},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add the variant"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Insertion.all.size() == 1
        assert Allele.all.size() == 1

        SequenceAlteration variant = Insertion.all.iterator().next()

        assert variant.referenceBases == "G"
        assert variant.alterationResidue == "AAAAA"
        assert variant.alternateAlleles[0].bases == "GAAAAA"
    }

    void "Add a de-novo SNV variant"() {
        given: "a de-novo SNV variant"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"T\",\"location\":{\"strand\":1,\"fmin\":952885,\"fmax\":952886},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add the variant"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Substitution.all.size() == 1
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

        SequenceAlteration variant = SNV.all.iterator().next()

        assert variant.referenceBases == "G"
        assert variant.alterationResidue == "T"
        assert variant.alternateAlleles[0].bases == "T"
    }

    void "Add a de-novo MNV variant"() {
        given: "a de-novo MNV variant"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"TGA\",\"location\":{\"strand\":1,\"fmin\":952885,\"fmax\":952888},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"MNV\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add the variant"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Substitution.all.size() == 1
        assert MNV.all.size() == 1
        assert Allele.all.size() == 1

        SequenceAlteration variant = MNV.all.iterator().next()

        assert variant.referenceBases == "GTT"
        assert variant.alterationResidue == "TGA"
        assert variant.alternateAlleles[0].bases == "TGA"
    }
}
