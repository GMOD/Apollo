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

class VariantServiceIntegrationSpec extends AbstractIntegrationSpec {

    def requestHandlingService
    def featureService
    def featureRelationshipService
    def transcriptService
    def exonService
    def cdsService
    def sequenceService
    def variantAnnotationService

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

        SNV snv = SNV.all.get(0)
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

        SNV snv = SNV.all.get(0)
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

    void "Add a Deletion variant"() {
        given: "A Deletion variant"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"location\":{\"fmin\":649999,\"strand\":1,\"fmax\":650003},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a Deletion variant"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the Deletion"
        assert Deletion.all.size() == 1
        assert Allele.all.size() == 1

        SequenceAlteration variant = SequenceAlteration.all.get(0)
        assert variant.fmin == 649998
        assert variant.fmax == 650003
        assert variant.referenceBases == 'TGCTT'
        assert variant.alternateAlleles.iterator().next().bases == 'T'
        assert variant.alternateAlleles.iterator().next().alterationResidue == 'GCTT'

    }

    void "Add an Insertion variant"() {
        given: "An Insertion variant"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"residues\":\"CCTA\",\"location\":{\"fmin\":650049,\"strand\":1,\"fmax\":650049},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add an Insertion variant"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the Deletion"
        assert Insertion.all.size() == 1
        assert Allele.all.size() == 1

        SequenceAlteration variant = SequenceAlteration.all.get(0)
        assert variant.fmin == 650049
        assert variant.fmax == 650050
        assert variant.referenceBases == 'G'
        assert variant.alternateAlleles.iterator().next().bases == 'GCCTA'
        assert variant.alternateAlleles.iterator().next().alterationResidue == 'CCTA'
    }

    void "Add a de-novo deletion variant"() {
        given: "a de-novo deletion variant"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":747724,\"fmax\":747729},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add the variant"
        requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Deletion.all.size() == 1
        assert Allele.all.size() == 1
        Deletion deletion = Deletion.all.get(0)

        SequenceAlteration variant = Deletion.all.iterator().next()
        assert variant.fmin == 747723
        assert variant.referenceBases == "GCATGT"
        assert variant.alternateAlleles.iterator().next().alterationResidue == "CATGT"
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
        assert variant.fmin == 952874
        assert variant.fmax == 952875
        assert variant.referenceBases == "G"
        assert variant.alternateAlleles.iterator().next().alterationResidue == "AAAAA"
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
        assert variant.alternateAlleles.iterator().next().alterationResidue == "T"
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
        assert variant.alternateAlleles.iterator().next().alterationResidue == "TGA"
        assert variant.alternateAlleles[0].bases == "TGA"
    }

    void "Effect of an Insertion AEC and a SNV on a transcript (single exon) on the forward strand"() {
        given: "transcript GB40814-RA, an Insertion AEC and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":414369,\"strand\":1,\"fmax\":414600},\"name\":\"GB40814-RA\",\"children\":[{\"location\":{\"fmin\":414369,\"strand\":1,\"fmax\":414600},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"residues\":\"TTA\",\"location\":{\"fmin\":414399,\"strand\":1,\"fmax\":414399},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"residues\":\"T\",\"location\":{\"fmin\":414449,\"strand\":1,\"fmax\":414450},\"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Insertion.all.size() == 1

        when: "we add an overlapping SNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)
        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(SequenceAlteration.findByUniqueName(uniqueName))

        then: "we should see the variant"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1
        SNV snv = SNV.all.get(0)
        Allele allele = snv.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTAACTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTGAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTAACTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTGAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTAACTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTGAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"

        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTAACTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTTAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTAACTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTTAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTAACTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTTAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "GTG"
        assert variantEffect.alternateCodon == "GTT"
        assert variantEffect.referenceResidue == "V"
        assert variantEffect.alternateResidue == "V"
        assert variantEffect.proteinPosition == 28
        assert variantEffect.cdnaPosition == 84 - 1
        assert variantEffect.cdsPosition == 84 - 1
    }


    void "Effect of an Insertion AEC and SNV on a transcript (single exon) on the reverse strand"() {
        given: "transcript GB40724-RA, an Insertion AEC and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1214039,\"strand\":-1,\"fmax\":1214249},\"name\":\"GB40724-RA\",\"children\":[{\"location\":{\"fmin\":1214039,\"strand\":-1,\"fmax\":1214249},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1214039,\"strand\":-1,\"fmax\":1214249},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"residues\":\"TAC\",\"location\":{\"fmin\":1214224,\"strand\":1,\"fmax\":1214224},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"residues\":\"G\",\"location\":{\"fmin\":1214149,\"strand\":1,\"fmax\":1214150},\"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Insertion.all.size() == 1

        when: "we add an overlapping SNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)
        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(SequenceAlteration.findByUniqueName(uniqueName))

        then: "we should see the variant"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1
        SNV snv = SNV.all.get(0)
        Allele allele = snv.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGCCTTTTTGTTTGGTCGGATCCAGTAAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTTCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGCCTTTTTGTTTGGTCGGATCCAGTAAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTTCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGCCTTTTTGTTTGGTCGGATCCAGTAAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTTCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGCCTTTTTGTTTGGTCGGATCCAGTAAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTCCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGCCTTTTTGTTTGGTCGGATCCAGTAAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTCCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGCCTTTTTGTTTGGTCGGATCCAGTAAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTCCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "TCC"
        assert variantEffect.alternateCodon == "CCC"
        assert variantEffect.referenceResidue == "S"
        assert variantEffect.alternateResidue == "P"
        assert variantEffect.proteinPosition == 34
        assert variantEffect.cdnaPosition == 103 - 1
        assert variantEffect.cdsPosition == 103 - 1
    }


    void "Effect of an Deletion AEC and a SNV on a transcript on the forward strand"() {
        given: "transcript GB40814-RA, a Deletion AEC and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":414369,\"strand\":1,\"fmax\":414600},\"name\":\"GB40814-RA\",\"children\":[{\"location\":{\"fmin\":414369,\"strand\":1,\"fmax\":414600},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"location\":{\"fmin\":414399,\"strand\":1,\"fmax\":414402},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"residues\":\"T\",\"location\":{\"fmin\":414449,\"strand\":1,\"fmax\":414450},\"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Deletion.all.size() == 1

        when: "we add an overlapping SNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTGAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTGAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTGAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTTAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTTAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTTAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "GTG"
        assert variantEffect.alternateCodon == "GTT"
        assert variantEffect.referenceResidue == "V"
        assert variantEffect.alternateResidue == "V"
        assert variantEffect.proteinPosition == 26
        assert variantEffect.cdnaPosition == 77
        assert variantEffect.cdsPosition == 77

    }


    void "Effect of an Deletion AEC and SNV on a transcript on the reverse strand"() {
        given: "transcript GB40724-RA, a deletion and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1214039,\"strand\":-1,\"fmax\":1214249},\"name\":\"GB40724-RA\",\"children\":[{\"location\":{\"fmin\":1214039,\"strand\":-1,\"fmax\":1214249},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1214039,\"strand\":-1,\"fmax\":1214249},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"location\":{\"fmin\":1214224,\"strand\":1,\"fmax\":1214227},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"residues\":\"G\",\"location\":{\"fmin\":1214149,\"strand\":1,\"fmax\":1214150},\"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Deletion.all.size() == 1

        when: "we add an overlapping SNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGCCTTTTTGTTTGGTCGGATAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTTCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGCCTTTTTGTTTGGTCGGATAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTTCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGCCTTTTTGTTTGGTCGGATAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTCCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGCCTTTTTGTTTGGTCGGATAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTCCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "."
        assert variantEffect.alternateCodon == "."
        assert variantEffect.referenceResidue == "."
        assert variantEffect.alternateResidue == "."
        assert variantEffect.proteinPosition == null
        assert variantEffect.cdnaPosition == 97 - 1
        assert variantEffect.cdsPosition == null
    }

    void "Effect of an Substitution AEC and a SNV on a transcript on the forward strand"() {
        given: "transcript GB40814-RA, a Substitution AEC and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":414369,\"strand\":1,\"fmax\":414600},\"name\":\"GB40814-RA\",\"children\":[{\"location\":{\"fmin\":414369,\"strand\":1,\"fmax\":414600},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"residues\":\"CCC\",\"location\":{\"fmin\":414399,\"strand\":1,\"fmax\":414402},\"type\":{\"name\":\"substitution\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"residues\":\"T\",\"location\":{\"fmin\":414449,\"strand\":1,\"fmax\":414450},\"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Substitution.all.size() == 1

        when: "we add an overlapping SNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTCCCTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTGAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTCCCTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTGAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTCCCTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTGAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTCCCTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTTAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTCCCTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTTAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGTTTGAAAATCAGAGTGAAAGAGCAAGTCCCTTGTTGAATCTACCAAGTTCGTACATACTTCACACAACTTTGGCCGTTAAGTTTCCGCCAATTCACACGCCCATACGCCCCAAGTTAAACTTTGAGGTCAGACTAATTTTATGTCTTCGCTATATCACTAGGAGAATACTATATGTTTCATCAGATACAGTGGCTCAAGTTCCAAGTCTCGAATCTTTAGACGGCTAA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "GTG"
        assert variantEffect.alternateCodon == "GTT"
        assert variantEffect.referenceResidue == "V"
        assert variantEffect.alternateResidue == "V"
        assert variantEffect.proteinPosition == 27
        assert variantEffect.cdnaPosition == 81 - 1
        assert variantEffect.cdsPosition == 81 - 1
    }


    void "Effect of an Substitution AEC and SNV on a transcript on the reverse strand"() {
        given: "transcript GB40724-RA, a Substitution AEC and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1214039,\"strand\":-1,\"fmax\":1214249},\"name\":\"GB40724-RA\",\"children\":[{\"location\":{\"fmin\":1214039,\"strand\":-1,\"fmax\":1214249},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1214039,\"strand\":-1,\"fmax\":1214249},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"residues\":\"CAT\",\"location\":{\"fmin\":1214224,\"strand\":1,\"fmax\":1214227},\"type\":{\"name\":\"substitution\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"residues\":\"G\",\"location\":{\"fmin\":1214149,\"strand\":1,\"fmax\":1214150},\"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Substitution.all.size() == 1

        when: "we add an overlapping SNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGCCTTTTTGTTTGGTCGGATATGAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTTCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGCCTTTTTGTTTGGTCGGATATGAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTTCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGCCTTTTTGTTTGGTCGGATATGAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTTCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGCCTTTTTGTTTGGTCGGATATGAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTCCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGCCTTTTTGTTTGGTCGGATATGAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTCCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGCCTTTTTGTTTGGTCGGATATGAGGGAAGAGGAAATTCAGTAGCACGCATCGCCCATAAATCTGTTCCTCCTACGGTGGAATTTCCCAACAGGTTTCCCGGGAAAACCCAATTGAAGAGCGCTACGTGGAAACTGGCCGCCATGGAACAACCGATTTCGATCGTGAACAGCGAATCTGGTTTACGGAATCTGACTCGTTCAGGTTGA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "TCC"
        assert variantEffect.alternateCodon == "CCC"
        assert variantEffect.referenceResidue == "S"
        assert variantEffect.alternateResidue == "P"
        assert variantEffect.proteinPosition == 33
        assert variantEffect.cdnaPosition == 100 - 1
        assert variantEffect.cdsPosition == 100 - 1
    }


    void "Add a transcript on forward strand (without a 5'UTR), and a SNV (on the 2nd exon) to observe the effect of SNV on the transcript"() {
        given: "transcript GB40829-RA and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747760},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747822,\"fmax\":747894},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747946,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40829-RA\",\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"residues\":\"C\",\"location\":{\"fmin\":747874,\"strand\":1,\"fmax\":747875},\"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an overlapping SNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should sese the variant"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCGTAAGCGTTTTTAAACGCCCGACATTTTTCTTCTTTTCCAAGATCTTTGCTTCATGTCGCAGAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGCATGCAACAGGTATATTTACTTCAATTTTATAAATACATATCTCTTTTAAATATTCTTAAAGATATAACTATAGATGATTAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGCATGCAACAGATATAACTATAGATGATTAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGCATGCAACAGATATAACTATAGATGATTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCGTAAGCGTTTTTAAACGCCCGACATTTTTCTTCTTTTCCAAGATCTTTGCTTCATGTCGCAGAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATCCATTTAGAGCATGCAACAGGTATATTTACTTCAATTTTATAAATACATATCTCTTTTAAATATTCTTAAAGATATAACTATAGATGATTAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATCCATTTAGAGCATGCAACAGATATAACTATAGATGATTAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATCCATTTAGAGCATGCAACAGATATAACTATAGATGATTAA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "ATT"
        assert variantEffect.alternateCodon == "ATC"
        assert variantEffect.referenceResidue == "I"
        assert variantEffect.alternateResidue == "I"
        assert variantEffect.proteinPosition == 38
        assert variantEffect.cdnaPosition == 114 - 1
        assert variantEffect.cdsPosition == 114 - 1

    }


    void "Add a transcript on forward strand (without a 5'UTR), an Insertion AEC and a SNV and observe the effect the SNV has on the transcript"() {
        given: "transcript GB40829-RA and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747760},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747822,\"fmax\":747894},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747946,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40829-RA\",\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"features\":[{\"residues\":\"TAC\",\"location\":{\"strand\":1,\"fmin\":747884,\"fmax\":747884},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addVariantString1 = "{ ${testCredentials} \"features\":[{\"alternate_alleles\":[{\"bases\":\"T\"}],\"location\":{\"strand\":1,\"fmin\":747724,\"fmax\":747725},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"},\"reference_bases\":\"C\"}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a overlapping AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Insertion.all.size() == 1

        when: "we add an overlapping SNV variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString1) as JSONObject)

        then: "we should see the variant"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCGTAAGCGTTTTTAAACGCCCGACATTTTTCTTCTTTTCCAAGATCTTTGCTTCATGTCGCAGAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGGTATATTTACTTCAATTTTATAAATACATATCTCTTTTAAATATTCTTAAAGATATAACTATAGATGATTAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTATAGATGATTAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTATAGATGATTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGAGGGCATCAAATGCTCAAAAAGTATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCGTAAGCGTTTTTAAACGCCCGACATTTTTCTTCTTTTCCAAGATCTTTGCTTCATGTCGCAGAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGGTATATTTACTTCAATTTTATAAATACATATCTCTTTTAAATATTCTTAAAGATATAACTATAGATGATTAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGAGGGCATCAAATGCTCAAAAAGTATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTATAGATGATTAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGAGGGCATCAAATGCTCAAAAAGTATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTATAGATGATTAA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "GCA"
        assert variantEffect.alternateCodon == "GTA"
        assert variantEffect.referenceResidue == "A"
        assert variantEffect.alternateResidue == "V"
        assert variantEffect.proteinPosition == 9
        assert variantEffect.cdnaPosition == 26 - 1
        assert variantEffect.cdsPosition == 26 - 1

    }


    void "Add a transcript on reverse strand (without 5'UTR), and a SNV (on the 2nd exon) to observe the effect of SNV on the transcript"() {
        given: "transcript GB40743-RA and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775413},\"name\":\"GB40743-RA\",\"children\":[{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775185},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":775344,\"strand\":-1,\"fmax\":775413},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775413},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"residues\":\"A\",\"location\":{\"fmin\":775149,\"strand\":1,\"fmax\":775150},\"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an overlapping SNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGGTATGAACAATTCTTTCTTTATGATCTTCACTATTATATAAATATTCGTATAAAGATTTCTTTATATAATAGTATAATGATTTATCAAATAAGTTTTAAAGAAAAAGATAATCATACCTGATGTACAATGGGAGCTTCGATCTGTCGAGGCGCTTGCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGGTATGAACAATTCTTTCTTTATGATCTTCACTATTATATAAATATTCGTATAAAGATTTCTTTATATAATAGTATAATGATTTATCAAATAAGTTTTAAAGAAAAAGATAATCATACCTGATGTACAATGGGAGCTTCGATCTGTCGAGGCGCTTGCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGATCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGATCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGATCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "GAC"
        assert variantEffect.alternateCodon == "GAT"
        assert variantEffect.referenceResidue == "D"
        assert variantEffect.alternateResidue == "D"
        assert variantEffect.proteinPosition == 35
        assert variantEffect.cdnaPosition == 105 - 1
        assert variantEffect.cdsPosition == 105 - 1

    }


    void "Add a transcript on reverse strand (without 5'UTR), an Insertion AEC, and a SNV (on the 1st exon) to observe the effect of SNV on the transcript"() {
        given: "transcript GB40743-RA and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775413},\"name\":\"GB40743-RA\",\"children\":[{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775185},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":775344,\"strand\":-1,\"fmax\":775413},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775413},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"residues\":\"TGA\",\"location\":{\"fmin\":775374,\"strand\":1,\"fmax\":775374},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"residues\":\"A\",\"location\":{\"fmin\":775149,\"strand\":1,\"fmax\":775150},\"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a overlapping AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Insertion.all.size() == 1

        when: "we add an overlapping SNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGGTATGAACAATTCTTTCTTTATGATCTTCACTATTATATAAATATTCGTATAAAGATTTCTTTATATAATAGTATAATGATTTATCAAATAAGTTTTAAAGAAAAAGATAATCATACCTGATGTACAATGGGAGCTTCGATCTGTCGAGGCGCTTGCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGGTATGAACAATTCTTTCTTTATGATCTTCACTATTATATAAATATTCGTATAAAGATTTCTTTATATAATAGTATAATGATTTATCAAATAAGTTTTAAAGAAAAAGATAATCATACCTGATGTACAATGGGAGCTTCGATCTGTCGAGGCGCTTGCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGATCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGATCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGATCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "GAC"
        assert variantEffect.alternateCodon == "GAT"
        assert variantEffect.referenceResidue == "D"
        assert variantEffect.alternateResidue == "D"
        assert variantEffect.proteinPosition == 36
        assert variantEffect.cdnaPosition == 108 - 1
        assert variantEffect.cdsPosition == 108 - 1

    }


    void "Add a transcript on forward strand (that has a 5'UTR), and a SNV (on the 2nd exon) to observe the effect of SNV on the transcript"() {
        given: "transcript GB40821-RA and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":621650,\"strand\":1,\"fmax\":628275},\"name\":\"GB40821-RA\",\"children\":[{\"location\":{\"fmin\":621650,\"strand\":1,\"fmax\":622330},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":623090,\"strand\":1,\"fmax\":623213},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":624547,\"strand\":1,\"fmax\":624610},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":624680,\"strand\":1,\"fmax\":624743},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":624885,\"strand\":1,\"fmax\":624927},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":625015,\"strand\":1,\"fmax\":625090},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":627962,\"strand\":1,\"fmax\":628275},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":622270,\"strand\":1,\"fmax\":628037},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addVariantString = "{ ${testCredentials} \"operation\":\"add_variant\",\"features\":[{\"residues\":\"T\",\"location\":{\"fmin\":623124,\"strand\":1,\"fmax\":623125},\"type\":{\"name\":\"SNV\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an overlapping SNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert SNV.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACTTTTTCTTCTCGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGAAATCGCAAGGATCCACAGATATTACAAAACCTATCATTTTGGGCCACGATGGCTGGTCCGTAGAAAATCCTGCTTTTAACCTTTCGCATACCAAGACGAGGTCCACTCTCAATATCCAATAATATTTTGCGAATACGTGTTTGGAATGAAAGATTAGAGAAAAACTTGTGTGTTGAGTAATGTTTATCAAGTATTTTTTTTAAGATTGTTATTAACAACGAGTTTTTAATTGTTTATTAGGTTAGGTTAAGTCGTATATCGTTACACATATGGGTGTCACTTGGGAGATAAATTTTCTATTTTTAGAAATATTAGAAAATTAATAATGAAAATTTATCAATGAATATTTATTAAAAATATATTTCTTGACAATTTTTGGTTGTGATATATTAAATAATAATGATTTGACGTTTTATCGATGATTGTATCACTCGATTTAAAAGGAATTTCACTATAAAGATGGAGCCACGTGAATTTATACGTGATATACTTCAATCTATTCATCTCGTAAAAATACAAAAACTATCTAGTTAAGAAATTTCACTTTTTTTAAAAATTCGCTTTAATTTAAAAAACAAATTGAAATTTCTTGATAAATCGAAGGAAGTGATCGAATAATTTTTGTATTTTCACAATTTATACGAAATAAATCTATATTAAAATTCGTACTTTGTTAAAAATATACTCTCAAAAAAAAAATTCTCTTTTAATAAATATTAATGATATCTTCATCTTGGTATGCCAAGGGATAAAAAGAAGATGTAAAATCTTTTCATTTCGAATGATATTTTTTTTTTTCTTTTTTTTTATTTCAGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGGTGAGTCTCACTCATTTTTCCTTCTTGCGAGGAACTTATATTATTATTTCTTCGATAATAAAATATTTTTTTTAGAAATAATTTTTTAATATAAATTATCGTTCATTCAATATTTTGAAAGAGAATTAATTTATAGTTTCGTGAATTTCTTTCGATTTATATTAAAAAAAAAATTGTAATATTGAATAATTAATAATTAATAATTAATATTTCTTGTGGATTTTTTAATTCAAAAATATAAATGAAATGATTAATTAAAAAATCATCAATATTTTATTAAGTAAAATAAATTAATTCCTGATAGAGCAAAAGTGTCAAAATTCTTTCAAAATTGTGCATTTTCGGTAAATTATTAAATGTCACTTTTACTTTCAATCCAAAAACGATAAACATTTAATAACAAGTCCAATGAATAAAATATTTAAAAAACACTTGCTCGTCGTTGAAAAGTAAAGAGACGGGAATCGATCATTTCTCGAAAAGAAAAAGAGAAAAGTTACTTTAAAAGCGAAGAACATTTCAGAAATCCGATGGAAAGCGAGCTAAATAATGTCGAATCCGATGTGTTGGCAACTTCGTGCTTGATCAAATTAACCTTTTTCACCGTTTCAACATCGATAGAAACTTTCGATCTTTCCGCTAATCAAAACTCTTAAAGAGGTAAAAAGTTAAAAGAAAAGAAAAGAAAAGAAAAAAAAGGGGGAGGGGGGAGAAGGGAAAGAGAGAAGGGAAGGAAAGAAAAAAGAAAAAAAGAATGCAAATCGAACAATCGTAATTGCGAGACACCTTGTCGACGATACTTTTCTCCACTGTTATCGATCTGTCTGCCTTTCGTTCATCCAGACGAATAAATCGGCCACGAATTAAACGGGACAATTGTAGAGTAACGAAGCATTAAATCACGAAGTGGCTGTGTATATACAGATACGTTGTCACGAAATGAAATCTTAATTCATTTACGAGTACCTCTTCGTTCCTTTTTTTTTCTTTTTTTTTTTTTTATTCTTTTCAACGTTTTGTGTATACAAGAGAGTTATCGCGTGCGTTTCACGGAACGATAAAAGTTTAAGGTTATGTTTGGTTAGATTAGATTAAGTAACAATTCTTGCGAAAAGAATTTCATCCAATTTCTTTCTTTTTTCTAAAAATTGTTAAAAAATAAAGTAATGATAATTATAATCATCTTTCAATATTAATCAATTTATTATTATCATTGATAAAAATATTTCGTCATTTCTCGAGGTATTTTTAAACAAATCTGTTAATCTTAATAATATTCTTTAAAAAAAATATATATACTTACTCTTTAATTCTTGCATGTACTTCAATCTCAGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGGTATTTTTTTTTTTTAAGAAAAATCAGAGATACGCAGTGGGGAGGTTAATAAATAATATTTAATTTGCAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACGTAAGTTTGTTTTCCAGTCGATGCGTCCCATGTGGAAAGTGCATTCCGTTGTTTTGCCTTCGGATAAGTTGGTGTAGTACGAGGAACAACGCGGTGAAACAATATAGTTACGGTAAATGGTAAATTGCTTTCTGATTTCCAGCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGGTTGGTACTATTCTCGAATAAATTTCTCTCCCCTTCCGATGAAGTTACGCGACCTAACCTCACTTTCTCTATTTGTGTGTTTCGACAGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGTAAAGTGAAAAAAATAATTTAATTAATTTGTTTTTAAGATTAAAAGAATTTTTAAAATTTTTTTGTTCGTGTTTATAGTCTTGTCTCCAGTTTTTTCCTCTTCTCGTAAATATATTAGAAACAAGTGCAAGTATATATCTGCTTTGACGGACTTTCTCACGTTAAAATTTCAATATGATTTCGTTACGAGATTCAAATTTATCTTGAAGGAAGTAATGTTAAAAAATTCATATGATAAAATGGAACTCTATTCTCCTTCTAGATATTCATTCTTAATTAAATGAAAAATTCGTTTCATTACTTGAAAATAGTTTAATAATATTTTTATATAATTAAAACTCGAAAATGTTCTTTTAATTATATTATCTTCTGTTCGAATATTTTTCCACGAGTGATTTTTATTGCTATTATTGCTCTCCGAGTTTACATAAACTTTTGTAAAAAGAAAAGGAAAAAAAATCTTTTGTATCTATCGAAGCTTTACAATTAAAGCTCCAAAGTGATTATAAATTCTTTCAGATTACAGTATCGATTAATTTTTTTTTTTTTTTTTTGCTAAGAACTGTAGTGGGTTAAAAGAAACCGTAATCAGTGTTCTCGATTCAGTATTTAATATTAGGTAAAAGTTATTTTCAATAACAAATAAAAGTTCGTTACGAGATTTTATAACTTTAAAGTGGTTATATAAAATTACTTTTTAAATAAGCGTTAAAAAAAATAGAATTTAGAGGAACATCTCAAGTTCAAAACAGCAAAATTTCAATTATTTATTCCTGATGAAACTTTGAAATATATTTCACATTTTTCTTTATCAAAAATATAATAATAAATTGATGTATCGATTTTTAGCTTTAAATTATTATTATCATTTCATCTAACATGAAAATTAAACAGGAAAGAAAAAACTACATCTTCTATTTTCAACTGTTATAATATTTGTATCGTTTATATTCACTTAAATAACTTGCACGTTTTATCATTCTTTCTCACTCGTGTTTCCATCACTCTCGTACCTATATCAAATTATTCCAGACAACAAAGAACGATCAAAATTACTCGTCGAGGAAAAGAAACAACTTAATCACAATCTTTTCTTCCACTCTCACAATATTCACCTCTGCACAAACCCAATCCAACTACCATAAGTCTACGCCTCTTCTCGAGACTCTCTCCTTCCCCATCTCTTCCCTTATCTCTCATCATCCTCCACGATCCCCACAATAGGATTCTTCCCCACAATTACAAAGGCCTAGCTCGCTGCCTTTCATTTTTAACAATGTGACGGTTAATAGACGGAACGTATCACGCGAATAAGACGTATGATGCTCGTCCATCTTTCGGTGGACGATGACACTAAGCGGTGTCACGTGACACCGTCTTGACTCGCTATTCGACTTCGACCAAAAGATAGAAGAAAGGAGAGGGGGTAAAAGTTTAAAATGGAATTCGTCCGAGGAAACGAATATTTGGAAGGATGGGGGGGGGGGGAAGAAAATATAGATGTCACGATGTGATAATAAGGCGGGACCTCCGTAGCTGTGAAAATTGGATTATAAATGAGTCAAAAGGTGGTATCTAACCGGGACCTGAAGCTACGAAAGCGATTTTCATCTTGTTAATTAGGGCGAACTCGCGGAAATTCGCGGGGTTATATGCGTCGCGTGAACTTGAAGTATATCCCGCCCGAGGGTGTTAAAAGTGTCTGGAAACTTTTCTGCTCGCGCCAGACAACAACTATGGCGGTCGTGCACCGTGAGAAGAGGGAGGGAGAAGGGGGGTCACTAATGGCGGAGAACATCCGAGATCACGGATTTTATTTTATTTTTGATGGTGGATAGATACTTGGGGAATAATGATACCGAGGATGTGATATTTGAATTTTTTTTTCTTTTTTTTTTTGAAGAATAAGAAAAGTTATTATTTTGAGACTAGATTTATATTTTGTATGATTATTTTGAATTAAGTAAATGGTGTGTGGATTACAAAGTTGAGAATAGAAATTGTAAATAGGATATTTAATTGTAATTTGGATATGAAAATATTTTTACATAAATACTCGATAATCGAATTTTTTTAAAAATGCTATGGTGAATTTGTAAACAAAATGTAAGTTAGAATGAAAAACGAGATAGTCGATGTGCGATGTTGGAAAATGTTGAGGGAAATGTTTCGAAATATAATTGAGTAATAAGGAATATTGGCAAGAAGTATTAGTCTTCTCTTGTTTACCTTTCAAGATATGAAACGGTTTCTTGGAAAGTTTTATTTTTACCCTTTACCAACAAGATTCTAGAAAAAAATTAATCAAACTACTTATCAAACAGATAAGATATATCTATATCGGTCAATTATTATAAAAACTTTCAAGTTGTCAGTATTTTACTCGGATAATAATATAGAAATCTTTTTTAATAATTTTGCAAAATTATTATTCGAAAATTTCCATGAAGCAATTTTCATCACTTTCGATAATGAAAACTGTATTCACAATCTAAAATGTTTAATTAAAAGAAACACCATTCTAAAGAAAATACGAAAAAAAAAAATTTATCAATTTCCTGTAAGAAAAAATTTATTTCAACGTGCTACGATAAATGTAATAAATTCATTAAATCAAATTTCTCTTCGAATAAGAATTAAATTTCATTTTTCTTTCTATATTTATTATTTTTATTAAAAAAGGAAAAACTTAATCCAATAAAAAGAAAAGAAAGAAAAAGTTGAAATAAATTGAAACGAAACTAAACCGATGATGATTGACTCGCGTGAGAAAAATCGAAACGTTGAAAGTTCGTTTGAATAGAAAATATTTGCGTCGAAAGAGAAAGAGAAAGAGAGAGAGAAAGAAAAATTCATTGACGCATGTTTCAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACTTTTTCTTCTCGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGAAATCGCAAGGATCCACAGATATTACAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGGAAATCGCAAGGATCCACAGATATTACAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACTTTTTCTTCTCGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGAAATCGCAAGGATCCACAGATATTACAAAACCTATCATTTTGGGCCACGATGGCTGGTCCGTAGAAAATCCTGCTTTTAACCTTTCGCATACCAAGACGAGGTCCACTCTCAATATCCAATAATATTTTGCGAATACGTGTTTGGAATGAAAGATTAGAGAAAAACTTGTGTGTTGAGTAATGTTTATCAAGTATTTTTTTTAAGATTGTTATTAACAACGAGTTTTTAATTGTTTATTAGGTTAGGTTAAGTCGTATATCGTTACACATATGGGTGTCACTTGGGAGATAAATTTTCTATTTTTAGAAATATTAGAAAATTAATAATGAAAATTTATCAATGAATATTTATTAAAAATATATTTCTTGACAATTTTTGGTTGTGATATATTAAATAATAATGATTTGACGTTTTATCGATGATTGTATCACTCGATTTAAAAGGAATTTCACTATAAAGATGGAGCCACGTGAATTTATACGTGATATACTTCAATCTATTCATCTCGTAAAAATACAAAAACTATCTAGTTAAGAAATTTCACTTTTTTTAAAAATTCGCTTTAATTTAAAAAACAAATTGAAATTTCTTGATAAATCGAAGGAAGTGATCGAATAATTTTTGTATTTTCACAATTTATACGAAATAAATCTATATTAAAATTCGTACTTTGTTAAAAATATACTCTCAAAAAAAAAATTCTCTTTTAATAAATATTAATGATATCTTCATCTTGGTATGCCAAGGGATAAAAAGAAGATGTAAAATCTTTTCATTTCGAATGATATTTTTTTTTTTCTTTTTTTTTATTTCAGTTATACCAATATCTGTTGACGGACGCTAAGTTGATGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGGTGAGTCTCACTCATTTTTCCTTCTTGCGAGGAACTTATATTATTATTTCTTCGATAATAAAATATTTTTTTTAGAAATAATTTTTTAATATAAATTATCGTTCATTCAATATTTTGAAAGAGAATTAATTTATAGTTTCGTGAATTTCTTTCGATTTATATTAAAAAAAAAATTGTAATATTGAATAATTAATAATTAATAATTAATATTTCTTGTGGATTTTTTAATTCAAAAATATAAATGAAATGATTAATTAAAAAATCATCAATATTTTATTAAGTAAAATAAATTAATTCCTGATAGAGCAAAAGTGTCAAAATTCTTTCAAAATTGTGCATTTTCGGTAAATTATTAAATGTCACTTTTACTTTCAATCCAAAAACGATAAACATTTAATAACAAGTCCAATGAATAAAATATTTAAAAAACACTTGCTCGTCGTTGAAAAGTAAAGAGACGGGAATCGATCATTTCTCGAAAAGAAAAAGAGAAAAGTTACTTTAAAAGCGAAGAACATTTCAGAAATCCGATGGAAAGCGAGCTAAATAATGTCGAATCCGATGTGTTGGCAACTTCGTGCTTGATCAAATTAACCTTTTTCACCGTTTCAACATCGATAGAAACTTTCGATCTTTCCGCTAATCAAAACTCTTAAAGAGGTAAAAAGTTAAAAGAAAAGAAAAGAAAAGAAAAAAAAGGGGGAGGGGGGAGAAGGGAAAGAGAGAAGGGAAGGAAAGAAAAAAGAAAAAAAGAATGCAAATCGAACAATCGTAATTGCGAGACACCTTGTCGACGATACTTTTCTCCACTGTTATCGATCTGTCTGCCTTTCGTTCATCCAGACGAATAAATCGGCCACGAATTAAACGGGACAATTGTAGAGTAACGAAGCATTAAATCACGAAGTGGCTGTGTATATACAGATACGTTGTCACGAAATGAAATCTTAATTCATTTACGAGTACCTCTTCGTTCCTTTTTTTTTCTTTTTTTTTTTTTTATTCTTTTCAACGTTTTGTGTATACAAGAGAGTTATCGCGTGCGTTTCACGGAACGATAAAAGTTTAAGGTTATGTTTGGTTAGATTAGATTAAGTAACAATTCTTGCGAAAAGAATTTCATCCAATTTCTTTCTTTTTTCTAAAAATTGTTAAAAAATAAAGTAATGATAATTATAATCATCTTTCAATATTAATCAATTTATTATTATCATTGATAAAAATATTTCGTCATTTCTCGAGGTATTTTTAAACAAATCTGTTAATCTTAATAATATTCTTTAAAAAAAATATATATACTTACTCTTTAATTCTTGCATGTACTTCAATCTCAGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGGTATTTTTTTTTTTTAAGAAAAATCAGAGATACGCAGTGGGGAGGTTAATAAATAATATTTAATTTGCAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACGTAAGTTTGTTTTCCAGTCGATGCGTCCCATGTGGAAAGTGCATTCCGTTGTTTTGCCTTCGGATAAGTTGGTGTAGTACGAGGAACAACGCGGTGAAACAATATAGTTACGGTAAATGGTAAATTGCTTTCTGATTTCCAGCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGGTTGGTACTATTCTCGAATAAATTTCTCTCCCCTTCCGATGAAGTTACGCGACCTAACCTCACTTTCTCTATTTGTGTGTTTCGACAGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGTAAAGTGAAAAAAATAATTTAATTAATTTGTTTTTAAGATTAAAAGAATTTTTAAAATTTTTTTGTTCGTGTTTATAGTCTTGTCTCCAGTTTTTTCCTCTTCTCGTAAATATATTAGAAACAAGTGCAAGTATATATCTGCTTTGACGGACTTTCTCACGTTAAAATTTCAATATGATTTCGTTACGAGATTCAAATTTATCTTGAAGGAAGTAATGTTAAAAAATTCATATGATAAAATGGAACTCTATTCTCCTTCTAGATATTCATTCTTAATTAAATGAAAAATTCGTTTCATTACTTGAAAATAGTTTAATAATATTTTTATATAATTAAAACTCGAAAATGTTCTTTTAATTATATTATCTTCTGTTCGAATATTTTTCCACGAGTGATTTTTATTGCTATTATTGCTCTCCGAGTTTACATAAACTTTTGTAAAAAGAAAAGGAAAAAAAATCTTTTGTATCTATCGAAGCTTTACAATTAAAGCTCCAAAGTGATTATAAATTCTTTCAGATTACAGTATCGATTAATTTTTTTTTTTTTTTTTTGCTAAGAACTGTAGTGGGTTAAAAGAAACCGTAATCAGTGTTCTCGATTCAGTATTTAATATTAGGTAAAAGTTATTTTCAATAACAAATAAAAGTTCGTTACGAGATTTTATAACTTTAAAGTGGTTATATAAAATTACTTTTTAAATAAGCGTTAAAAAAAATAGAATTTAGAGGAACATCTCAAGTTCAAAACAGCAAAATTTCAATTATTTATTCCTGATGAAACTTTGAAATATATTTCACATTTTTCTTTATCAAAAATATAATAATAAATTGATGTATCGATTTTTAGCTTTAAATTATTATTATCATTTCATCTAACATGAAAATTAAACAGGAAAGAAAAAACTACATCTTCTATTTTCAACTGTTATAATATTTGTATCGTTTATATTCACTTAAATAACTTGCACGTTTTATCATTCTTTCTCACTCGTGTTTCCATCACTCTCGTACCTATATCAAATTATTCCAGACAACAAAGAACGATCAAAATTACTCGTCGAGGAAAAGAAACAACTTAATCACAATCTTTTCTTCCACTCTCACAATATTCACCTCTGCACAAACCCAATCCAACTACCATAAGTCTACGCCTCTTCTCGAGACTCTCTCCTTCCCCATCTCTTCCCTTATCTCTCATCATCCTCCACGATCCCCACAATAGGATTCTTCCCCACAATTACAAAGGCCTAGCTCGCTGCCTTTCATTTTTAACAATGTGACGGTTAATAGACGGAACGTATCACGCGAATAAGACGTATGATGCTCGTCCATCTTTCGGTGGACGATGACACTAAGCGGTGTCACGTGACACCGTCTTGACTCGCTATTCGACTTCGACCAAAAGATAGAAGAAAGGAGAGGGGGTAAAAGTTTAAAATGGAATTCGTCCGAGGAAACGAATATTTGGAAGGATGGGGGGGGGGGGAAGAAAATATAGATGTCACGATGTGATAATAAGGCGGGACCTCCGTAGCTGTGAAAATTGGATTATAAATGAGTCAAAAGGTGGTATCTAACCGGGACCTGAAGCTACGAAAGCGATTTTCATCTTGTTAATTAGGGCGAACTCGCGGAAATTCGCGGGGTTATATGCGTCGCGTGAACTTGAAGTATATCCCGCCCGAGGGTGTTAAAAGTGTCTGGAAACTTTTCTGCTCGCGCCAGACAACAACTATGGCGGTCGTGCACCGTGAGAAGAGGGAGGGAGAAGGGGGGTCACTAATGGCGGAGAACATCCGAGATCACGGATTTTATTTTATTTTTGATGGTGGATAGATACTTGGGGAATAATGATACCGAGGATGTGATATTTGAATTTTTTTTTCTTTTTTTTTTTGAAGAATAAGAAAAGTTATTATTTTGAGACTAGATTTATATTTTGTATGATTATTTTGAATTAAGTAAATGGTGTGTGGATTACAAAGTTGAGAATAGAAATTGTAAATAGGATATTTAATTGTAATTTGGATATGAAAATATTTTTACATAAATACTCGATAATCGAATTTTTTTAAAAATGCTATGGTGAATTTGTAAACAAAATGTAAGTTAGAATGAAAAACGAGATAGTCGATGTGCGATGTTGGAAAATGTTGAGGGAAATGTTTCGAAATATAATTGAGTAATAAGGAATATTGGCAAGAAGTATTAGTCTTCTCTTGTTTACCTTTCAAGATATGAAACGGTTTCTTGGAAAGTTTTATTTTTACCCTTTACCAACAAGATTCTAGAAAAAAATTAATCAAACTACTTATCAAACAGATAAGATATATCTATATCGGTCAATTATTATAAAAACTTTCAAGTTGTCAGTATTTTACTCGGATAATAATATAGAAATCTTTTTTAATAATTTTGCAAAATTATTATTCGAAAATTTCCATGAAGCAATTTTCATCACTTTCGATAATGAAAACTGTATTCACAATCTAAAATGTTTAATTAAAAGAAACACCATTCTAAAGAAAATACGAAAAAAAAAAATTTATCAATTTCCTGTAAGAAAAAATTTATTTCAACGTGCTACGATAAATGTAATAAATTCATTAAATCAAATTTCTCTTCGAATAAGAATTAAATTTCATTTTTCTTTCTATATTTATTATTTTTATTAAAAAAGGAAAAACTTAATCCAATAAAAAGAAAAGAAAGAAAAAGTTGAAATAAATTGAAACGAAACTAAACCGATGATGATTGACTCGCGTGAGAAAAATCGAAACGTTGAAAGTTCGTTTGAATAGAAAATATTTGCGTCGAAAGAGAAAGAGAAAGAGAGAGAGAAAGAAAAATTCATTGACGCATGTTTCAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACTTTTTCTTCTCGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGAAATCGCAAGGATCCACAGATATTACAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGATGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGGAAATCGCAAGGATCCACAGATATTACAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGATGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "AGG"
        assert variantEffect.alternateCodon == "ATG"
        assert variantEffect.referenceResidue == "R"
        assert variantEffect.alternateResidue == "M"
        assert variantEffect.proteinPosition == 32
        assert variantEffect.cdnaPosition == 715 - 1
        assert variantEffect.cdsPosition == 95 - 1

    }

    @Ignore
    void "Add a transcript on forward strand (without 5'UTR), an Insertion AEC and an Insertion variant to observe the effect on the transcript"() {
        given: "transcript GB40829-RA, an Insertion AEC and an Insertion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747760},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747822,\"fmax\":747894},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747946,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40829-RA\",\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"features\":[{\"residues\":\"TAC\",\"location\":{\"strand\":1,\"fmin\":747884,\"fmax\":747884},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"TGA\",\"location\":{\"strand\":1,\"fmin\":747954,\"fmax\":747954},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a overlapping AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Insertion.all.size() == 1

        when: "we add an overlapping Insertion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Insertion.all.size() == 2
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCGTAAGCGTTTTTAAACGCCCGACATTTTTCTTCTTTTCCAAGATCTTTGCTTCATGTCGCAGAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGGTATATTTACTTCAATTTTATAAATACATATCTCTTTTAAATATTCTTAAAGATATAACTATAGATGATTAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTATAGATGATTAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTATAGATGATTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCGTAAGCGTTTTTAAACGCCCGACATTTTTCTTCTTTTCCAAGATCTTTGCTTCATGTCGCAGAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGGTATATTTACTTCAATTTTATAAATACATATCTCTTTTAAATATTCTTAAAGATATAACTTGAATAGATGATTAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTTGAATAGATGATTAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTTGA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "."
        assert variantEffect.alternateCodon == "."
        assert variantEffect.referenceResidue == null
        assert variantEffect.alternateResidue == null
        assert variantEffect.proteinPosition == null
        assert variantEffect.cdnaPosition == 145 - 1
        assert variantEffect.cdsPosition == 145 - 1

    }

    @Ignore
    void "Add a transcript on forward strand (without 5'UTR), an Insertion AEC and an Insertion variant (that introduces a frameshift) to observe the effect on the transcript"() {
        given: "transcript GB40829-RA, an Insertion AEC and an Insertion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747760},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747822,\"fmax\":747894},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747946,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40829-RA\",\"location\":{\"strand\":1,\"fmin\":747699,\"fmax\":747966},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"features\":[{\"residues\":\"TAC\",\"location\":{\"strand\":1,\"fmin\":747884,\"fmax\":747884},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"T\",\"location\":{\"strand\":1,\"fmin\":747954,\"fmax\":747954},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a overlapping AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Insertion.all.size() == 1

        when: "we add an overlapping Insertion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Insertion.all.size() == 2
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCGTAAGCGTTTTTAAACGCCCGACATTTTTCTTCTTTTCCAAGATCTTTGCTTCATGTCGCAGAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGGTATATTTACTTCAATTTTATAAATACATATCTCTTTTAAATATTCTTAAAGATATAACTATAGATGATTAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTATAGATGATTAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTATAGATGATTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCGTAAGCGTTTTTAAACGCCCGACATTTTTCTTCTTTTCCAAGATCTTTGCTTCATGTCGCAGAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGGTATATTTACTTCAATTTTATAAATACATATCTCTTTTAAATATTCTTAAAGATATAACTTATAGATGATTAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTTATAGATGATTAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGAGGGCATCAAATGCTCAAAAAGCATGTCTTTGGGAAACCGAGCAGACGGAGATCAGCCAAAAAGTGACGCTTTTATGTAGCGAAGAAACACTTGAGCCTGGAAAATACATTCATTTAGAGTACCATGCAACAGATATAACTTATAGATGA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "."
        assert variantEffect.alternateCodon == "."
        assert variantEffect.referenceResidue == null
        assert variantEffect.alternateResidue == null
        assert variantEffect.proteinPosition == null
        assert variantEffect.cdnaPosition == 145 - 1
        assert variantEffect.cdsPosition == 145 - 1

    }

    @Ignore
    void "Add a transcript on reverse strand (without 5'UTR) and an Insertion varaint to observe the effect"() {
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775413},\"name\":\"GB40743-RA\",\"children\":[{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775185},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":775344,\"strand\":-1,\"fmax\":775413},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775413},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"GTA\",\"location\":{\"strand\":1,\"fmin\":775124,\"fmax\":775124},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an overlapping sNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Insertion.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGGTATGAACAATTCTTTCTTTATGATCTTCACTATTATATAAATATTCGTATAAAGATTTCTTTATATAATAGTATAATGATTTATCAAATAAGTTTTAAAGAAAAAGATAATCATACCTGATGTACAATGGGAGCTTCGATCTGTCGAGGCGCTTGCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGGTATGAACAATTCTTTCTTTATGATCTTCACTATTATATAAATATTCGTATAAAGATTTCTTTATATAATAGTATAATGATTTATCAAATAAGTTTTAAAGAAAAAGATAATCATACCTGATGTACAATGGGAGCTTCGATCTGTCGAGGCGCTTGCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATACTTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATACTTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATACTTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "."
        assert variantEffect.alternateCodon == "."
        assert variantEffect.referenceResidue == null
        assert variantEffect.alternateResidue == null
        assert variantEffect.proteinPosition == null
        assert variantEffect.cdnaPosition == 131 - 1
        assert variantEffect.cdsPosition == 131 - 1

    }

    @Ignore
    void "Add a transcript on reverse strand (without 5'UTR), an Insertion AEC, and an Insertion variant (on the 2nd exon) to observe the effect of SNV on the transcript"() {
        given: "transcript GB40743-RA and a SNV"
        String addTranscriptString = "{ ${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775413},\"name\":\"GB40743-RA\",\"children\":[{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775185},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":775344,\"strand\":-1,\"fmax\":775413},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":775035,\"strand\":-1,\"fmax\":775413},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"residues\":\"TGA\",\"location\":{\"fmin\":775374,\"strand\":1,\"fmax\":775374},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"GTA\",\"location\":{\"strand\":1,\"fmin\":775124,\"fmax\":775124},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a overlapping AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Insertion.all.size() == 1

        when: "we add an overlapping sNV"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Insertion.all.size() == 2
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGGTATGAACAATTCTTTCTTTATGATCTTCACTATTATATAAATATTCGTATAAAGATTTCTTTATATAATAGTATAATGATTTATCAAATAAGTTTTAAAGAAAAAGATAATCATACCTGATGTACAATGGGAGCTTCGATCTGTCGAGGCGCTTGCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGGTATGAACAATTCTTTCTTTATGATCTTCACTATTATATAAATATTCGTATAAAGATTTCTTTATATAATAGTATAATGATTTATCAAATAAGTTTTAAAGAAAAAGATAATCATACCTGATGTACAATGGGAGCTTCGATCTGTCGAGGCGCTTGCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATACTTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATACTTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGAAAATCAAGTTTGAAATGAATGAAAGTATTCAGATATCAATTGATTTTATTGATTTATATTACTCTCAGATGGCGATTTCCATTTTCATCACGTCCCCATCCGACCACGTATCCAGATCTTCCCACAACATACTTGGCAAGAAGCGCAGGTCCTGTCCAGAGACAGATTGGTCTGATTACATCATTATACTCCACTTTATCTCTCAGACTCAACACTGCTAA"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "."
        assert variantEffect.alternateCodon == "."
        assert variantEffect.referenceResidue == null
        assert variantEffect.alternateResidue == null
        assert variantEffect.proteinPosition == null
        assert variantEffect.cdnaPosition == 134 - 1
        assert variantEffect.cdsPosition == 134 - 1

    }

    @Ignore
    void "Add a transcript on reverse strand (with 5'UTR), an Insertion AEC, and an Insertion variant (on the 2nd exon) to observe the effect of SNV on the transcript"() {
        given: "transcript GB40749-RA, an Insertion AEC and an Insertion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":694293,\"fmax\":694606},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":694918,\"fmax\":695591},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":695882,\"fmax\":696055},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":694440,\"fmax\":695943},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40749-RA\",\"location\":{\"strand\":-1,\"fmin\":694293,\"fmax\":696055},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CCT\",\"location\":{\"strand\":1,\"fmin\":695249,\"fmax\":695249},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"residues\":\"CAC\",\"location\":{\"strand\":1,\"fmin\":694499,\"fmax\":694499},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a overlapping AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Insertion.all.size() == 1

        when: "we add an overlapping Insertion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Insertion.all.size() == 2
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGGTAAGACAGAATTCTCTTGTGCACACAGTATAGCTACAGTATACTCAGGGGACGACGAGTGAAATTTTGGCGTGGTTATGGAAAAAAAAAAAGTACAACTCGTAAAGTTGTTGGAGTAAATGAGTCCCGTTTTTTCATGGCGAATCGTACGTCTCCTTTCCACTCGACGACACAGTTTTCAATTTCATATAATAAAAGCGAATGTGAAAATACGATGCGTATGATTCGTTCGAAAAAGAAAGGCAAAAAAAAAAAAAAAAAAAAAATGAAATGATTTTCTCTCCTAATTAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGGTATATTTCTTCTTTTAGTTTTATATATTTTTTTATATATTATATATATACACGAGTTACGAATAATAACAAAATTTTTTTCGAACCTTGAACGTATGATCAAAATTTCTCATTAAAACATTTGGAACGAAAAATGATAATTAAATATCGTAATCGGATGATTGCAACATATTATATAGTAATACATTATACATACCTATTTTATTTATTTTCTTTAGCAAATTATAAAGTTAATTTTATTGAGTTAATTTTACGATGTTTGAATTAATTAGTGGATACGAGATTATATGGTGTAACGTACATATTTTGTAGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAG"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGGTAAGACAGAATTCTCTTGTGCACACAGTATAGCTACAGTATACTCAGGGGACGACGAGTGAAATTTTGGCGTGGTTATGGAAAAAAAAAAAGTACAACTCGTAAAGTTGTTGGAGTAAATGAGTCCCGTTTTTTCATGGCGAATCGTACGTCTCCTTTCCACTCGACGACACAGTTTTCAATTTCATATAATAAAAGCGAATGTGAAAATACGATGCGTATGATTCGTTCGAAAAAGAAAGGCAAAAAAAAAAAAAAAAAAAAAATGAAATGATTTTCTCTCCTAATTAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGGTATATTTCTTCTTTTAGTTTTATATATTTTTTTATATATTATATATATACACGAGTTACGAATAATAACAAAATTTTTTTCGAACCTTGAACGTATGATCAAAATTTCTCATTAAAACATTTGGAACGAAAAATGATAATTAAATATCGTAATCGGATGATTGCAACATATTATATAGTAATACATTATACATACCTATTTTATTTATTTTCTTTAGCAAATTATAAAGTTAATTTTATTGAGTTAATTTTACGATGTTTGAATTAATTAGTGGATACGAGATTATATGGTGTAACGTACATATTTTGTAGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGTGGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGTGGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGTGGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAG"

        VariantEffect variantEffect = allele.variantEffects.iterator().next()
        assert variantEffect.referenceCodon == "."
        assert variantEffect.alternateCodon == "."
        assert variantEffect.referenceResidue == null
        assert variantEffect.alternateResidue == null
        assert variantEffect.proteinPosition == null
        assert variantEffect.cdnaPosition == 957 - 1
        assert variantEffect.cdsPosition == 845 - 1

    }

    @Ignore
    void "Add a transcript on forward strand (without 5'UTR), and a Deletion variant to observe the effect on the transcript"() {
        given: "transcript GB40812-RA and a Deletion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":405099,\"fmax\":405102},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an overlapping Deletion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Deletion.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAAGTAGGTTACATTGTACTTTCAATTTTTATAGAATATTTTCAAAATTTAATTATGAACATAATTGTTCAATAAAAATAAACAATTTATAATAAACATTTCATAACCTATAACTACCATGTTTCATTATTTATTAATATCATATTTTTATTGCTTGAATATTAAATACCATATATTTTAAAATCTTTTATCGTAGGATAAACGCTTAATAAAAATATTACGAAAATTTTCTTTATTCCCGAGATTTAATTTACTTTTTATCATATTGCGCTTTTTTCACTCATTGTCAGTAGGAGAAATAAGTAATCGAAAAGAAGATCGAGAAAATTTGAAAGCATATTTAGCAATCGATGTACAGAGATAAATTATTATCTTGATTTAATAGCAAATAATTTACCAATAACACGTTCGATATCCAACTTAATTTGTTTATATAACGAATCAAGAAAATATAAATTTTTTCTAAAAATCACGAAACTTTTGCAATACTTGTCAAAGAAAGACTATCTAGTTCGTAGAATGTCCGCTAGCGTCACTCATTGCCAGAGTACGTGACGAATGCTTGAGTGTACGAGTCGTGTTTATTTTGTTTTTTTCGAATGTTGTCGCGGAGTTCGAGCGAGCTTTGCGAGTTCGTGAGAAACATATTTCTCGAGATTGTTTTCGCTGGGCTACGACGTACAATGCATTTTTCGTGTCGAATGAAGAAAATAGGCTCAGACTCTCGGGATAAAGATGTGGCAAGAAAACATGTATTTAGAATAGTGAACGCGGTAGATATAAATATCAAATATAAAAAGATTGATAAAAATCTTCGTTCTTATAAATAATTTCTATGCTTGGTTACACGTATTCGCTCTGTCGTCTCAATTTACAGTTGCACTCTGTTCTCTTCGTTAGTGCTATCGTTCGTGTTGGTATATATTAACCAAAAATACATGGAACTTGATACGTCTGTCTATCGAAAAATTAATACAATATCTTACATAGTCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAAGTAGGTTACATTGTACTTTCAATTTTTATAGAATATTTTCAAAATTTAATTATGAACATAATTGTTCAATAAAAATAAACAATTTATAATAAACATTTCATAACCTATAACTACCATGTTTCATTATTTATTAATATCATATTTTTATTGCTTGAATATTAAATACCATATATTTTAAAATCTTTTATCGTAGGATAAACGCTTAATAAAAATATTACGAAAATTTTCTTTATTCCCGAGATTTAATTTACTTTTTATCATATTGCGCTTTTTTCACTCATTGTCAGTAGGAGAAATAAGTAATCGAAAAGAAGATCGAGAAAATTTGAAAGCATATTTAGCAATCGATGTACAGAGATAAATTATTATCTTGATTTAATAGCAAATAATTTACCAATAACACGTTCGATATCCAACTTAATTTGTTTATATAACGAATCAAGAAAATATAAATTTTTTCTAAAAATCACGAAACTTTTGCAATACTTGTCAAAGAAAGACTATCTAGTTCGTAGAATGTCCGCTAGCGTCACTCATTGCCAGAGTACGTGACGAATGCTTGAGTGTACGAGTCGTGTTTATTTTGTTTTTTTCGAATGTTGTCGCGGAGTTCGAGCGAGCTTTGCGAGTTCGTGAGAAACATATTTCTCGAGATTGTTTTCGCTGGGCTACGACGTACAATGCATTTTTCGTGTCGAATGAAGAAAATAGGCTCAGACTCTCGGGATAAAGATGTGGCAAGAAAACATGTATTTAGAATAGTGAACGCGGTAGATATAAATATCAAATATAAAAAGATTGATAAAAATCTTCGTTCTTATAAATAATTTCTATGCTTGGTTACACGTATTCGCTCTGTCGTCTCAATTTACAGTTGCACTCTGTTCTCTTCGTTAGTGCTATCGTTCGTGTTGGTATATATTAACCAAAAATACATGGAACTTGATACGTCTGTCTATCGAAAAATTAATACAATATCTTACATAGTCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAAGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"

//        VariantEffect variantEffect = allele.variantEffects.iterator().next()
//        assert variantEffect.referenceCodon == "."
//        assert variantEffect.alternateCodon == "."
//        assert variantEffect.referenceResidue == null
//        assert variantEffect.alternateResidue == null
//        assert variantEffect.proteinPosition == null
//        assert variantEffect.cdnaPosition == 230 - 1
//        assert variantEffect.cdsPosition == 230 - 1


    }

    @Ignore
    void "Add a transcript on forward strand (without 5'UTR), an Insertion AEC and a Deletion variant to observe the effect on the transcript"() {
        given: "transcript GB40812-RA, an Insertion AEC and a Deletion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"features\":[{\"residues\":\"CTC\",\"location\":{\"strand\":1,\"fmin\":403999,\"fmax\":403999},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":405099,\"fmax\":405102},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an Insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        Insertion.all.size() == 1

        when: "we add an overlapping Deletion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Deletion.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAACTCGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAAGTAGGTTACATTGTACTTTCAATTTTTATAGAATATTTTCAAAATTTAATTATGAACATAATTGTTCAATAAAAATAAACAATTTATAATAAACATTTCATAACCTATAACTACCATGTTTCATTATTTATTAATATCATATTTTTATTGCTTGAATATTAAATACCATATATTTTAAAATCTTTTATCGTAGGATAAACGCTTAATAAAAATATTACGAAAATTTTCTTTATTCCCGAGATTTAATTTACTTTTTATCATATTGCGCTTTTTTCACTCATTGTCAGTAGGAGAAATAAGTAATCGAAAAGAAGATCGAGAAAATTTGAAAGCATATTTAGCAATCGATGTACAGAGATAAATTATTATCTTGATTTAATAGCAAATAATTTACCAATAACACGTTCGATATCCAACTTAATTTGTTTATATAACGAATCAAGAAAATATAAATTTTTTCTAAAAATCACGAAACTTTTGCAATACTTGTCAAAGAAAGACTATCTAGTTCGTAGAATGTCCGCTAGCGTCACTCATTGCCAGAGTACGTGACGAATGCTTGAGTGTACGAGTCGTGTTTATTTTGTTTTTTTCGAATGTTGTCGCGGAGTTCGAGCGAGCTTTGCGAGTTCGTGAGAAACATATTTCTCGAGATTGTTTTCGCTGGGCTACGACGTACAATGCATTTTTCGTGTCGAATGAAGAAAATAGGCTCAGACTCTCGGGATAAAGATGTGGCAAGAAAACATGTATTTAGAATAGTGAACGCGGTAGATATAAATATCAAATATAAAAAGATTGATAAAAATCTTCGTTCTTATAAATAATTTCTATGCTTGGTTACACGTATTCGCTCTGTCGTCTCAATTTACAGTTGCACTCTGTTCTCTTCGTTAGTGCTATCGTTCGTGTTGGTATATATTAACCAAAAATACATGGAACTTGATACGTCTGTCTATCGAAAAATTAATACAATATCTTACATAGTCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAACTCGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAACTCGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCTGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAACTCGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAAGTAGGTTACATTGTACTTTCAATTTTTATAGAATATTTTCAAAATTTAATTATGAACATAATTGTTCAATAAAAATAAACAATTTATAATAAACATTTCATAACCTATAACTACCATGTTTCATTATTTATTAATATCATATTTTTATTGCTTGAATATTAAATACCATATATTTTAAAATCTTTTATCGTAGGATAAACGCTTAATAAAAATATTACGAAAATTTTCTTTATTCCCGAGATTTAATTTACTTTTTATCATATTGCGCTTTTTTCACTCATTGTCAGTAGGAGAAATAAGTAATCGAAAAGAAGATCGAGAAAATTTGAAAGCATATTTAGCAATCGATGTACAGAGATAAATTATTATCTTGATTTAATAGCAAATAATTTACCAATAACACGTTCGATATCCAACTTAATTTGTTTATATAACGAATCAAGAAAATATAAATTTTTTCTAAAAATCACGAAACTTTTGCAATACTTGTCAAAGAAAGACTATCTAGTTCGTAGAATGTCCGCTAGCGTCACTCATTGCCAGAGTACGTGACGAATGCTTGAGTGTACGAGTCGTGTTTATTTTGTTTTTTTCGAATGTTGTCGCGGAGTTCGAGCGAGCTTTGCGAGTTCGTGAGAAACATATTTCTCGAGATTGTTTTCGCTGGGCTACGACGTACAATGCATTTTTCGTGTCGAATGAAGAAAATAGGCTCAGACTCTCGGGATAAAGATGTGGCAAGAAAACATGTATTTAGAATAGTGAACGCGGTAGATATAAATATCAAATATAAAAAGATTGATAAAAATCTTCGTTCTTATAAATAATTTCTATGCTTGGTTACACGTATTCGCTCTGTCGTCTCAATTTACAGTTGCACTCTGTTCTCTTCGTTAGTGCTATCGTTCGTGTTGGTATATATTAACCAAAAATACATGGAACTTGATACGTCTGTCTATCGAAAAATTAATACAATATCTTACATAGTCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAACTCGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGCAAAAGCGAACAAAGTGCTTAGTTATGCAAATGCTTCAATCGACTATGAATATGTTTGGTAACGATGTCGATCGTGATGATAAAACATCGCCTAACCTTCAAAGCTTGGAACAACTCGTTTTTAATGAGGTATTGAACAGAAGTTGCGAGTTTGATGATAAATCGAGCAAGCGGAACGAATCAATGTGGCATTTATTCACAATAAAAATATGTTTGTCTGTAGCTGTAGCATCTTGTAACAGTTTAGACATTTCACGGTTATTGGAGAGTCTAATTGCATAA"

//        VariantEffect variantEffect = allele.variantEffects.iterator().next()
//        assert variantEffect.referenceCodon == "."
//        assert variantEffect.alternateCodon == "."
//        assert variantEffect.referenceResidue == null
//        assert variantEffect.alternateResidue == null
//        assert variantEffect.proteinPosition == null
//        assert variantEffect.cdnaPosition == 233 - 1
//        assert variantEffect.cdsPosition == 233 - 1
    }

    @Ignore
    void "Add a transcript on forward strand (with 5'UTR), a Deletion AEC and a Deletion variant to observe the effect on the transcript"() {
        given: "transcript GB40807-RA, a Deletion AEC and a Deletion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":208175,\"fmax\":208544},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":208735,\"fmax\":210517},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":208322,\"fmax\":209434},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40807-RA\",\"location\":{\"strand\":1,\"fmin\":208175,\"fmax\":210517},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":208349,\"fmax\":208352},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":208799,\"fmax\":208805},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an Deletion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        Deletion.all.size() == 1

        when: "we add an overlapping Deletion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Deletion.all.size() == 2
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATGTACGTAATATAAATACATAATATTATATATATATATATATATATATATATATAATTATCAATTAACAAATGTATAAATTATTTATAAATTTTAAATACACTATATATTTAAGAAATTAATTTTTTTTGTATTTTTATATTTTTTTTCTAAATAAAGTATATATAATAATAGTAACTAAATATTATTGCAGCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGCGTGTTTTCATGATGCAAAATTTTCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATGTACGTAATATAAATACATAATATTATATATATATATATATATATATATATATAATTATCAATTAACAAATGTATAAATTATTTATAAATTTTAAATACACTATATATTTAAGAAATTAATTTTTTTTGTATTTTTATATTTTTTTTCTAAATAAAGTATATATAATAATAGTAACTAAATATTATTGCAGCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCCATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCCATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGCGTGTTTTCATGATGCAAAATTTTCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCCATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAA"

//        VariantEffect variantEffect = allele.variantEffects.iterator().next()
//        assert variantEffect.referenceCodon == "."
//        assert variantEffect.alternateCodon == "."
//        assert variantEffect.referenceResidue == null
//        assert variantEffect.alternateResidue == null
//        assert variantEffect.proteinPosition == null
//        assert variantEffect.cdnaPosition == 430 - 1
//        assert variantEffect.cdsPosition == 283 - 1

    }

    @Ignore
    void "Add a transcript on forward strand (with 5'UTR), a Deletion AEC (on exon 2) and a Deletion variant (on exon 1) to observe the effect on the transcript"() {
        // another test that swaps the location of deletions
        given: "transcript GB40807-RA, a Deletion AEC and a Deletion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":208175,\"fmax\":208544},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":208735,\"fmax\":210517},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":208322,\"fmax\":209434},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40807-RA\",\"location\":{\"strand\":1,\"fmin\":208175,\"fmax\":210517},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":208799,\"fmax\":208805},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addVariantString  = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":208349,\"fmax\":208352},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add an Deletion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        Deletion.all.size() == 1

        when: "we add an overlapping Deletion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Deletion.all.size() == 2
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATGTACGTAATATAAATACATAATATTATATATATATATATATATATATATATATAATTATCAATTAACAAATGTATAAATTATTTATAAATTTTAAATACACTATATATTTAAGAAATTAATTTTTTTTGTATTTTTATATTTTTTTTCTAAATAAAGTATATATAATAATAGTAACTAAATATTATTGCAGCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCCATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCCATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCCATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATGTACGTAATATAAATACATAATATTATATATATATATATATATATATATATATAATTATCAATTAACAAATGTATAAATTATTTATAAATTTTAAATACACTATATATTTAAGAAATTAATTTTTTTTGTATTTTTATATTTTTTTTCTAAATAAAGTATATATAATAATAGTAACTAAATATTATTGCAGCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCCATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCCATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGCGTGTTTTCATGATGCAAAATTTTCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCCATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAA"

//        VariantEffect variantEffect = allele.variantEffects.iterator().next()
//        assert variantEffect.referenceCodon == "."
//        assert variantEffect.alternateCodon == "."
//        assert variantEffect.referenceResidue == null
//        assert variantEffect.alternateResidue == null
//        assert variantEffect.proteinPosition == null
//        assert variantEffect.cdnaPosition == 174 - 1
//        assert variantEffect.cdsPosition == 27 - 1

    }

    @Ignore
    void "Add a transcript on reverse strand (without 5'UTR) and a Deletion variant to observe the effect on the transcript"() {
        given: "transcript GB40738-RA and a Deletion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":874076,\"fmax\":874091},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874214,\"fmax\":874252},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874336,\"fmax\":874361},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874076,\"fmax\":874361},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40738-RA\",\"location\":{\"strand\":-1,\"fmin\":874076,\"fmax\":874361},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":874224,\"fmax\":874227},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a Deletion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Deletion.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGATACTCACTTGTAAATGTATAAGTAAGTAATTAATTATATTATTGTAAATATCAATGTAATATAATCAATAGATTTATTAATTTTTCTTTTTTTTTTTTTTTTTAGATGATGGAAAATATTCTTTTCAACGATGCTTTATTCAGGTATTGAGGTATTGAATATCGATAAAGGGCTGTCTCTTTTTTTTTTCCTACTATCTCAAATTAGTTATTCTTTTTTAAGTTAATTCGAATTATAAAAGAATTGATATATGATTTTTCTTTCAGAAGTCAATCATTTGA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGATACTCACTTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGATGCTTTATTCAGAAGTCAATCATTTGA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGATACTCACTTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGATGCTTTATTCAGAAGTCAATCATTTGA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGATACTCACTTGTAAATGTATAAGTAAGTAATTAATTATATTATTGTAAATATCAATGTAATATAATCAATAGATTTATTAATTTTTCTTTTTTTTTTTTTTTTTAGATGATGGAAAATATTCTTTTCAACGCTTTATTCAGGTATTGAGGTATTGAATATCGATAAAGGGCTGTCTCTTTTTTTTTTCCTACTATCTCAAATTAGTTATTCTTTTTTAAGTTAATTCGAATTATAAAAGAATTGATATATGATTTTTCTTTCAGAAGTCAATCATTTGA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGATACTCACTTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGCTTTATTCAGAAGTCAATCATTTGA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGATACTCACTTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGCTTTATTCAGAAGTCAATCATTTGA"

//        VariantEffect variantEffect = allele.variantEffects.iterator().next()
//        assert variantEffect.referenceCodon == "."
//        assert variantEffect.alternateCodon == "."
//        assert variantEffect.referenceResidue == null
//        assert variantEffect.alternateResidue == null
//        assert variantEffect.proteinPosition == null
//        assert variantEffect.cdnaPosition == ?
//        assert variantEffect.cdsPosition == ?
    }

    @Ignore
    void "Add a transcript on reverse strand (without 5'UTR), a Insertion AEC and a Deletion variant to observe the effect on the transcript"() {
        given: "transcript GB40738-RA, Insertion AEC and a Deletion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":874076,\"fmax\":874091},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874214,\"fmax\":874252},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874336,\"fmax\":874361},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874076,\"fmax\":874361},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40738-RA\",\"location\":{\"strand\":-1,\"fmin\":874076,\"fmax\":874361},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"features\":[{\"residues\":\"ATT\",\"location\":{\"strand\":1,\"fmin\":874349,\"fmax\":874349},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"insertion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":874224,\"fmax\":874227},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a Insertion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Insertion.all.size() == 1

        when: "we add a Deletion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Deletion.all.size() == 1
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGATACTCACTAATTGTAAATGTATAAGTAAGTAATTAATTATATTATTGTAAATATCAATGTAATATAATCAATAGATTTATTAATTTTTCTTTTTTTTTTTTTTTTTAGATGATGGAAAATATTCTTTTCAACGATGCTTTATTCAGGTATTGAGGTATTGAATATCGATAAAGGGCTGTCTCTTTTTTTTTTCCTACTATCTCAAATTAGTTATTCTTTTTTAAGTTAATTCGAATTATAAAAGAATTGATATATGATTTTTCTTTCAGAAGTCAATCATTTGA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGATACTCACTAATTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGATGCTTTATTCAGAAGTCAATCATTTGA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGATACTCACTAATTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGATGCTTTATTCAGAAGTCAATCATTTGA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGATACTCACTAATTGTAAATGTATAAGTAAGTAATTAATTATATTATTGTAAATATCAATGTAATATAATCAATAGATTTATTAATTTTTCTTTTTTTTTTTTTTTTTAGATGATGGAAAATATTCTTTTCAACGCTTTATTCAGGTATTGAGGTATTGAATATCGATAAAGGGCTGTCTCTTTTTTTTTTCCTACTATCTCAAATTAGTTATTCTTTTTTAAGTTAATTCGAATTATAAAAGAATTGATATATGATTTTTCTTTCAGAAGTCAATCATTTGA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGATACTCACTAATTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGCTTTATTCAGAAGTCAATCATTTGA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGATACTCACTAATTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGCTTTATTCAGAAGTCAATCATTTGA"

//        VariantEffect variantEffect = allele.variantEffects.iterator().next()
//        assert variantEffect.referenceCodon == "."
//        assert variantEffect.alternateCodon == "."
//        assert variantEffect.referenceResidue == null
//        assert variantEffect.alternateResidue == null
//        assert variantEffect.proteinPosition == null
//        assert variantEffect.cdnaPosition == ?
//        assert variantEffect.cdsPosition == ?
    }

    @Ignore
    void "Add a transcript on reverse strand (without 5'UTR), a Deletion AEC and a Deletion variant to observe the effect on the transcript"() {
        given: "transcript GB40738-RA and a Deletion variant"
        String addTranscriptString = "{ ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":874076,\"fmax\":874091},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874214,\"fmax\":874252},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874336,\"fmax\":874361},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874076,\"fmax\":874361},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40738-RA\",\"location\":{\"strand\":-1,\"fmin\":874076,\"fmax\":874361},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":874349,\"fmax\":874353},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":874224,\"fmax\":874227},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a Deletion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Deletion.all.size() == 1

        when: "we add a Deletion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Deletion.all.size() == 2
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "ATGATACTTGTAAATGTATAAGTAAGTAATTAATTATATTATTGTAAATATCAATGTAATATAATCAATAGATTTATTAATTTTTCTTTTTTTTTTTTTTTTTAGATGATGGAAAATATTCTTTTCAACGATGCTTTATTCAGGTATTGAGGTATTGAATATCGATAAAGGGCTGTCTCTTTTTTTTTTCCTACTATCTCAAATTAGTTATTCTTTTTTAAGTTAATTCGAATTATAAAAGAATTGATATATGATTTTTCTTTCAGAAGTCAATCATTTGA"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "ATGATACTTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGATGCTTTATTCAGAAGTCAATCATTTGA"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGATACTTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGATGCTTTATTCAGAAGTCAATCATTTGA"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "ATGATACTTGTAAATGTATAAGTAAGTAATTAATTATATTATTGTAAATATCAATGTAATATAATCAATAGATTTATTAATTTTTCTTTTTTTTTTTTTTTTTAGATGATGGAAAATATTCTTTTCAACGCTTTATTCAGGTATTGAGGTATTGAATATCGATAAAGGGCTGTCTCTTTTTTTTTTCCTACTATCTCAAATTAGTTATTCTTTTTTAAGTTAATTCGAATTATAAAAGAATTGATATATGATTTTTCTTTCAGAAGTCAATCATTTGA"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "ATGATACTTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGCTTTATTCAGAAGTCAATCATTTGA"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGATACTTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGCTTTATTCAGAAGTCAATCATTTGA"

//        VariantEffect variantEffect = allele.variantEffects.iterator().next()
//        assert variantEffect.referenceCodon == "."
//        assert variantEffect.alternateCodon == "."
//        assert variantEffect.referenceResidue == null
//        assert variantEffect.alternateResidue == null
//        assert variantEffect.proteinPosition == null
//        assert variantEffect.cdnaPosition == ?
//        assert variantEffect.cdsPosition == ?
    }

    @Ignore
    void "Add a transcript on reverse strand (with 5'UTR), a Deletion AEC and a Deletion variant to observe the effect on the transcript"() {
        given: "transcript GB40749-RA, a Deletion AEC and a Deletion variant"
        String addTranscriptString = " { ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":694293,\"fmax\":694606},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":694918,\"fmax\":695591},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":695882,\"fmax\":696055},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":694440,\"fmax\":695943},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40749-RA\",\"location\":{\"strand\":-1,\"fmin\":694293,\"fmax\":696055},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":695574,\"fmax\":695577},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":694499,\"fmax\":694502},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a Deletion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Deletion.all.size() == 1

        when: "we add a Deletion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Deletion.all.size() == 2
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGGTAAGACAGAATTCTCTTGTGCACACAGTATAGCTACAGTATACTCAGGGGACGACGAGTGAAATTTTGGCGTGGTTATGGAAAAAAAAAAAGTACAACTCGTAAAGTTGTTGGAGTAAATGAGTCCCGTTTTTTCATGGCGAATCGTACGTCTCCTTTCCACTCGACGACACAGTTTTCAATTTCATATAATAAAAGCGAATGTGAAAATACGATGCGTATGATTCGTTCGAAAAAGAAAGGCAAAAAAAAAAAAAAAAAAAAAATGAAATGATTTTCTCTCCTAATTAGTAGAGGCAGAATTATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGGTATATTTCTTCTTTTAGTTTTATATATTTTTTTATATATTATATATATACACGAGTTACGAATAATAACAAAATTTTTTTCGAACCTTGAACGTATGATCAAAATTTCTCATTAAAACATTTGGAACGAAAAATGATAATTAAATATCGTAATCGGATGATTGCAACATATTATATAGTAATACATTATACATACCTATTTTATTTATTTTCTTTAGCAAATTATAAAGTTAATTTTATTGAGTTAATTTTACGATGTTTGAATTAATTAGTGGATACGAGATTATATGGTGTAACGTACATATTTTGTAGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAG"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGGTAAGACAGAATTCTCTTGTGCACACAGTATAGCTACAGTATACTCAGGGGACGACGAGTGAAATTTTGGCGTGGTTATGGAAAAAAAAAAAGTACAACTCGTAAAGTTGTTGGAGTAAATGAGTCCCGTTTTTTCATGGCGAATCGTACGTCTCCTTTCCACTCGACGACACAGTTTTCAATTTCATATAATAAAAGCGAATGTGAAAATACGATGCGTATGATTCGTTCGAAAAAGAAAGGCAAAAAAAAAAAAAAAAAAAAAATGAAATGATTTTCTCTCCTAATTAGTAGAGGCAGAATTATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGGTATATTTCTTCTTTTAGTTTTATATATTTTTTTATATATTATATATATACACGAGTTACGAATAATAACAAAATTTTTTTCGAACCTTGAACGTATGATCAAAATTTCTCATTAAAACATTTGGAACGAAAAATGATAATTAAATATCGTAATCGGATGATTGCAACATATTATATAGTAATACATTATACATACCTATTTTATTTATTTTCTTTAGCAAATTATAAAGTTAATTTTATTGAGTTAATTTTACGATGTTTGAATTAATTAGTGGATACGAGATTATATGGTGTAACGTACATATTTTGTAGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAG"

//        VariantEffect variantEffect = allele.variantEffects.iterator().next()
//        assert variantEffect.referenceCodon == "."
//        assert variantEffect.alternateCodon == "."
//        assert variantEffect.referenceResidue == null
//        assert variantEffect.alternateResidue == null
//        assert variantEffect.proteinPosition == null
//        assert variantEffect.cdnaPosition == ?
//        assert variantEffect.cdsPosition == ?
    }

    @Ignore
    void "Add a transcript on reverse strand (with 5'UTR), a Deletion AEC (that introduces a frameshift) and a Deletion variant to observe the effect on the transcript"() {
        // Logic in VAS still needs fixing
        given: "transcript GB40749-RA, a Deletion AEC and a Deletion variant"
        String addTranscriptString = " { ${testCredentials} \"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":694293,\"fmax\":694606},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":694918,\"fmax\":695591},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":695882,\"fmax\":696055},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":694440,\"fmax\":695943},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40749-RA\",\"location\":{\"strand\":-1,\"fmin\":694293,\"fmax\":696055},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String addAssemblyErrorCorrectionString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":695574,\"fmax\":695576},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"
        String addVariantString = "{ ${testCredentials} \"features\":[{\"location\":{\"strand\":1,\"fmin\":694499,\"fmax\":694502},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_sequence_alteration\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should see the transcript"
        assert MRNA.all.size() == 1

        when: "we add a Deletion AEC"
        requestHandlingService.addSequenceAlteration(JSON.parse(addAssemblyErrorCorrectionString) as JSONObject)

        then: "we should see the AEC"
        assert Deletion.all.size() == 1

        when: "we add a Deletion variant"
        JSONObject returnObject = requestHandlingService.addVariant(JSON.parse(addVariantString) as JSONObject)

        then: "we should see the variant"
        assert Deletion.all.size() == 2
        assert Allele.all.size() == 1

        String uniqueName = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).get(0).get(FeatureStringEnum.UNIQUENAME.value)
        SequenceAlteration variant = SequenceAlteration.findByUniqueName(uniqueName)
        def sequenceTrace = variantAnnotationService.calculateEffectOfVariant(variant)
        Allele allele = variant.alternateAlleles.iterator().next()

        def sequences = sequenceTrace.get(allele)
        // transcript genomic sequence with AECs
        assert sequences[0] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGGTAAGACAGAATTCTCTTGTGCACACAGTATAGCTACAGTATACTCAGGGGACGACGAGTGAAATTTTGGCGTGGTTATGGAAAAAAAAAAAGTACAACTCGTAAAGTTGTTGGAGTAAATGAGTCCCGTTTTTTCATGGCGAATCGTACGTCTCCTTTCCACTCGACGACACAGTTTTCAATTTCATATAATAAAAGCGAATGTGAAAATACGATGCGTATGATTCGTTCGAAAAAGAAAGGCAAAAAAAAAAAAAAAAAAAAAATGAAATGATTTTCTCTCCTAATTAGTAGAGGCAGAATTACTGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGGTATATTTCTTCTTTTAGTTTTATATATTTTTTTATATATTATATATATACACGAGTTACGAATAATAACAAAATTTTTTTCGAACCTTGAACGTATGATCAAAATTTCTCATTAAAACATTTGGAACGAAAAATGATAATTAAATATCGTAATCGGATGATTGCAACATATTATATAGTAATACATTATACATACCTATTTTATTTATTTTCTTTAGCAAATTATAAAGTTAATTTTATTGAGTTAATTTTACGATGTTTGAATTAATTAGTGGATACGAGATTATATGGTGTAACGTACATATTTTGTAGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDNA sequence with AECs
        assert sequences[1] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACTGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDS sequence with AECs
        assert sequences[2] == "ATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAG"
        // transcript genomic sequence with AECs + Variant
        assert sequences[3] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGGTAAGACAGAATTCTCTTGTGCACACAGTATAGCTACAGTATACTCAGGGGACGACGAGTGAAATTTTGGCGTGGTTATGGAAAAAAAAAAAGTACAACTCGTAAAGTTGTTGGAGTAAATGAGTCCCGTTTTTTCATGGCGAATCGTACGTCTCCTTTCCACTCGACGACACAGTTTTCAATTTCATATAATAAAAGCGAATGTGAAAATACGATGCGTATGATTCGTTCGAAAAAGAAAGGCAAAAAAAAAAAAAAAAAAAAAATGAAATGATTTTCTCTCCTAATTAGTAGAGGCAGAATTACTGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGGTATATTTCTTCTTTTAGTTTTATATATTTTTTTATATATTATATATATACACGAGTTACGAATAATAACAAAATTTTTTTCGAACCTTGAACGTATGATCAAAATTTCTCATTAAAACATTTGGAACGAAAAATGATAATTAAATATCGTAATCGGATGATTGCAACATATTATATAGTAATACATTATACATACCTATTTTATTTATTTTCTTTAGCAAATTATAAAGTTAATTTTATTGAGTTAATTTTACGATGTTTGAATTAATTAGTGGATACGAGATTATATGGTGTAACGTACATATTTTGTAGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDNA sequence with AECs + Variant
        assert sequences[4] == "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACTGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        // transcript CDS sequence with AECs + Variant
        assert sequences[5] == "ATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAG"

//        VariantEffect variantEffect = allele.variantEffects.iterator().next()
//        assert variantEffect.referenceCodon == "."
//        assert variantEffect.alternateCodon == "."
//        assert variantEffect.referenceResidue == null
//        assert variantEffect.alternateResidue == null
//        assert variantEffect.proteinPosition == null
//        assert variantEffect.cdnaPosition == ?
//        assert variantEffect.cdsPosition == ?
    }
}
