package org.bbop.apollo

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.json.JSONObject

class VcfHandlerServiceIntegrationSpec extends AbstractIntegrationSpec {

    def vcfHandlerService
    def requestHandlingService

    void "Add a handful of variants and export as VCF"() {

        given: "3 variants"
        String addVariant1String = "{ ${testCredentials} \"features\":[{\"reference_allele\":\"G\",\"variant_info\":[{\"tag\":\"dbSNP_150\",\"value\":true},{\"tag\":\"TSA\",\"value\":\"SNV\"},{\"tag\":\"E_Freq\",\"value\":true},{\"tag\":\"E_1000G\",\"value\":true},{\"tag\":\"MA\",\"value\":\"T\"},{\"tag\":\"MAF\",\"value\":\"0.000199681\"},{\"tag\":\"MAC\",\"value\":\"1\"},{\"tag\":\"AA\",\"value\":\"C\"}],\"name\":\"rs576820509\",\"alternate_alleles\":[{\"bases\":\"T\",\"allele_info\":[{\"tag\":\"EAS_AF\",\"value\":\"0.001\"},{\"tag\":\"EUR_AF\",\"value\":\"0\"},{\"tag\":\"AMR_AF\",\"value\":\"0\"},{\"tag\":\"SAS_AF\",\"value\":\"0\"},{\"tag\":\"AFR_AF\",\"value\":\"0\"}]}],\"description\":\"SNV C -> T\",\"location\":{\"strand\":1,\"fmin\":95193,\"fmax\":95194},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"
        String addVariant2String = "{ ${testCredentials} \"features\":[{\"reference_allele\":\"G\",\"variant_info\":[{\"tag\":\"dbSNP_150\",\"value\":true},{\"tag\":\"TSA\",\"value\":\"SNV\"},{\"tag\":\"E_Freq\",\"value\":true},{\"tag\":\"E_1000G\",\"value\":true},{\"tag\":\"MA\",\"value\":\"G\"},{\"tag\":\"MAF\",\"value\":\"0.151158\"},{\"tag\":\"MAC\",\"value\":\"757\"},{\"tag\":\"AA\",\"value\":\"C\"}],\"name\":\"rs544194668\",\"alternate_alleles\":[{\"bases\":\"T\",\"allele_info\":[{\"tag\":\"EAS_AF\",\"value\":\"0.3958\"},{\"tag\":\"EUR_AF\",\"value\":\"0.0765\"},{\"tag\":\"AMR_AF\",\"value\":\"0.0965\"},{\"tag\":\"SAS_AF\",\"value\":\"0.1871\"},{\"tag\":\"AFR_AF\",\"value\":\"0.0234\"}]}],\"description\":\"SNV C -> G\",\"location\":{\"strand\":1,\"fmin\":95439,\"fmax\":95440},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"SNV\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"
        String addVariant3String = "{ ${testCredentials} \"features\":[{\"reference_allele\":\"AAATT\",\"variant_info\":[{\"tag\":\"dbSNP_150\",\"value\":true},{\"tag\":\"TSA\",\"value\":\"deletion\"},{\"tag\":\"E_Freq\",\"value\":true},{\"tag\":\"E_1000G\",\"value\":true},{\"tag\":\"MA\",\"value\":\"-\"},{\"tag\":\"MAF\",\"value\":\"0.000599042\"},{\"tag\":\"MAC\",\"value\":\"3\"}],\"name\":\"rs533528979\",\"alternate_alleles\":[{\"bases\":\"A\",\"allele_info\":[{\"tag\":\"EAS_AF\",\"value\":\"0\"},{\"tag\":\"EUR_AF\",\"value\":\"0\"},{\"tag\":\"AMR_AF\",\"value\":\"0\"},{\"tag\":\"SAS_AF\",\"value\":\"0\"},{\"tag\":\"AFR_AF\",\"value\":\"0.0023\"}]}],\"description\":\"deletion CAAAG -> C\",\"location\":{\"strand\":1,\"fmin\":91868,\"fmax\":91873},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"deletion\"}}],\"track\":\"Group1.10\",\"operation\":\"add_variant\"}"

        when: "we add all the variants"
        requestHandlingService.addVariant(JSON.parse(addVariant1String) as JSONObject)
        requestHandlingService.addVariant(JSON.parse(addVariant2String) as JSONObject)
        requestHandlingService.addVariant(JSON.parse(addVariant3String) as JSONObject)

        then: "we should see all the variants"
        assert SequenceAlteration.count == 3

        when: "we export these variants"
        File tempFile = File.createTempFile("output", ".vcf")
        tempFile.deleteOnExit()
        def variants = SequenceAlteration.all
        vcfHandlerService.writeVariantsToText(Organism.all.first(), variants, tempFile.path, "test")
        String tempFileText = tempFile.text
        print tempFileText

        then: "we should get a valid VCF"
        def lines = tempFile.readLines()
        assert lines.size() == 9
        assert lines[0] == "##fileformat=VCFv4.2"
        assert lines[2] == "##source=test"
        assert lines[4] == "##contig=<ID=Group1.10,length=1405242>"
        assert lines[5] == "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO"
    }
}
