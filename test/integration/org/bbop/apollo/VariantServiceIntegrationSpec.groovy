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

    void "adding a simple SNV"() {
        given: "a SNV"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"location\":{\"fmin\":1296556,\"strand\":1,\"fmax\":1296557},\"name\":\"rs0000000\",\"reference_bases\": \"A\", \"alternate_alleles\": [{\"bases\": \"G\"}], \"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a SNV"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the SNV"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1
    }

    void "adding a multi-alleleic SNV"() {
        given: "a SNV that has 3 alternate alleles"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"location\":{\"fmin\":1296556,\"strand\":1,\"fmax\":1296557},\"name\":\"rs0000000\",\"reference_bases\": \"A\", \"alternate_alleles\": [{\"bases\": \"G\"}, {\"bases\": \"C\"}, {\"bases\": \"T\"}], \"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a SNV"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the SNV and it should have 3 alleles"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 3

        SNV snv = SNV.findByName("rs0000000")
        assert snv.alternateAlleles.size() == 3
    }

    void "adding a multi-allelic SNV and adding additional properties"() {
        given: "a SNV that has 3 alternate alleles and additional properties that describe the SNV and its alleles"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"location\":{\"fmin\":1296556,\"strand\":1,\"fmax\":1296557},\"name\":\"rs0000000\",\"reference_bases\": \"A\", \"alternate_alleles\": [{\"bases\": \"G\",\"allele_frequency\":\"0.0321\", \"provenance\":\"PMID:0000001\", \"allele_info\": [{\"tag\": \"AC\", \"value\": \"32\"}]}, {\"bases\": \"C\", allele_frequency: \"0.00123\", \"provenance\":\"Variant Calling Pipeline v1.2b\", \"allele_info\": [{\"tag\": \"AC\", \"value\": \"7\"}]}, {\"bases\": \"T\", \"allele_frequency\": \"0.00111\", \"provenance\": \"http://datarepository.org/id213141\",  \"allele_info\": [{\"tag\": \"AC\", \"value\": 3}]}], \"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a SNV"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the SNV and it should have 3 alleles, with each alelle having its own properties such as AF and AC"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 3

        SNV snv = SNV.findByName("rs0000000")
        assert snv.alternateAlleles.size() == 3

        snv.alternateAlleles.each {
            println "Allele ${it.toString()}"
            if (it.bases == "G") {
                assert it.alleleFrequency == Float.parseFloat("0.0321")
                assert it.alleleInfo[0].tag == "AC"
                assert it.alleleInfo[0].value == "32"
            }
            else if (it.bases == "C") {
                assert it.alleleFrequency == Float.parseFloat("0.00123")
                assert it.alleleInfo[0].tag == "AC"
                assert it.alleleInfo[0].value == "7"
            }
            else if (it.bases == "T") {
                assert it.alleleFrequency == Float.parseFloat("0.00111")
                assert it.alleleInfo[0].tag == "AC"
                assert it.alleleInfo[0].value == "3"
            }
            else {
                assert false
            }
        }
    }

    void "adding a variant and managing additional alternate alleles"() {
        given: "a SNV"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"location\":{\"fmin\":1296556,\"strand\":1,\"fmax\":1296557},\"name\":\"rs0000000\",\"reference_bases\": \"A\", \"alternate_alleles\": [{\"bases\": \"G\"}], \"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addAlternateAllelesString = "{ ${testCredentials} \"operation\":\"add_alternate_alleles\",\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"alternate_alleles\":[{\"bases\":\"@BASES@\", \"allele_frequency\": \"@ALLELE_FREQUENCY@\", \"provenance\": \"@PROVENANCE@\"}]}],\"track\":\"Group1.10\"}"
        String updateAlternateAllelesString = "{ ${testCredentials} \"operation\":\"update_alternate_alleles\",\"features\":[{\"new_alternate_alleles\":[{\"bases\":\"@NEW_BASES@\", \"allele_frequency\": \"@NEW_ALLELE_FREQUENCY@\", \"provenance\": \"@NEW_PROVENANCE@\"}],\"uniquename\":\"@UNIQUENAME@\",\"old_alternate_alleles\":[{\"bases\":\"@OLD_BASES@\", \"allele_frequency\": \"@OLD_ALLELE_FREQUENCY@\", \"provenance\": \"@OLD_PROVENANCE@\"}]}],\"track\":\"Group1.10\"}"
        String deleteAlternateAllelesString = "{ ${testCredentials} \"operation\":\"delete_alternate_alleles\",\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"alternate_alleles\":[{\"bases\":\"@BASES@\", \"allele_frequency\": \"@ALLELE_FREQUENCY@\", \"provenance\": \"@PROVENANCE@\"}]}],\"track\":\"Group1.10\"}"

        when: "we add a SNV"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the SNV and it should have 1 alternate allele"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

        when: "we add an alternate allele"
        SNV snv = SNV.all.get(0)
        String addAlternateAllelesForSnv1 = addAlternateAllelesString.replace("@UNIQUENAME@", snv.uniqueName).replace("@BASES@", "T").replace("@ALLELE_FREQUENCY@", "0.0145").replace("@PROVENANCE@", "PMID:0000001")
        requestHandlingService.addAlternateAlleles(JSON.parse(addAlternateAllelesForSnv1) as JSONObject)

        then: "we should see 2 alternate alleles"
        assert snv.alternateAlleles.size() == 2

        when: "we modify a SNV's alternate allele by changing its allele frequency"
        String updateAlternateAllelesForSnv1 = updateAlternateAllelesString.replace("@UNIQUENAME@", snv.uniqueName).replace("@OLD_BASES@", "G").replace("@OLD_ALLELE_FREQUENCY@", "").replace("@OLD_PROVENANCE@", "").replace("@NEW_BASES@", "G").replace("@NEW_ALLELE_FREQUENCY@", "0.565").replace("@NEW_PROVENANCE@", "Test dataset 1")
        requestHandlingService.updateAlternateAlleles(JSON.parse(updateAlternateAllelesForSnv1) as JSONObject)

        then: "we should have an alternate allele with the newly added allele frequency"
        def alternateAlleles = snv.alternateAlleles
        for (def allele : alternateAlleles) {
            if (allele.bases == "G") {
                assert allele.alleleFrequency == Float.parseFloat("0.565")
            }
        }

        when: "we delete a SNV's alternate allele"
        String deleteAlternateAllelesSnv1 = deleteAlternateAllelesString.replace("@UNIQUENAME@", snv.uniqueName).replace("@BASES@", "G").replace("@ALLELE_FREQUENCY@", "0.565").replace("@PROVENANCE@", "Test dataset 1")
        requestHandlingService.deleteAlternateAlleles(JSON.parse(deleteAlternateAllelesSnv1) as JSONObject)

        then: "SNV should have only one alternate allele, T"
        assert Allele.all.size() == 1

        def newAlternateAlleles = snv.alternateAlleles
        assert newAlternateAlleles[0].bases == "T"
        assert newAlternateAlleles[0].alleleFrequency == Float.parseFloat("0.0145")

        when: "we add another alternate allele, C"
        String addAlternateAllelesForSnv2 = addAlternateAllelesString.replace("@UNIQUENAME@", snv.uniqueName).replace("@BASES@", "C").replace("@ALLELE_FREQUENCY@", "").replace("@PROVENANCE@", "")
        requestHandlingService.addAlternateAlleles(JSON.parse(addAlternateAllelesForSnv2) as JSONObject)

        then: "the alternate allele should be part of the SNV"
        assert Allele.all.size() == 2

        when: "we delete this newly added allele by providing just the base information"
        String deleteAlternateAllelesSnv2 = deleteAlternateAllelesString.replace("@UNIQUENAME@", snv.uniqueName).replace("@BASES@","C").replace("@ALLELE_FREQUENCY@", "").replace("@PROVENANCE@","")
        requestHandlingService.deleteAlternateAlleles(JSON.parse(deleteAlternateAllelesSnv2) as JSONObject)

        then: "the alternate allele, C, should be removed"
        assert Allele.all.size() == 1
    }

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
