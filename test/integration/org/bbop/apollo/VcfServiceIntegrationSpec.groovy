package org.bbop.apollo

import grails.test.spock.IntegrationSpec
import htsjdk.variant.vcf.VCFConstants
import htsjdk.variant.vcf.VCFFileReader
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.IgnoreRest

class VcfServiceIntegrationSpec extends AbstractIntegrationSpec {

    def vcfService

    String vcfFile1 = "test/integration/resources/sequences/honeybee-vcf/GATK_sample.vcf.gz"
    String vcfFile2 = "test/integration/resources/sequences/honeybee-vcf/simulated_test.vcf.gz"

    def setup() {
        setupDefaultUserOrg()
        Organism organism = Organism.first()
        organism.directory = "test/integration/resources/sequences/honeybee-tracks"
        organism.save(failOnError: true, flush: true)

        new Sequence(
                length: 1382403
                , seqChunkSize: 20000
                , start: 0
                , end: 1382403
                , organism: organism
                , name: "Group1.1"
        ).save(failOnError: true)

        new Sequence(
                length: 1405242
                , seqChunkSize: 20000
                , start: 0
                , end: 1405242
                , organism: organism
                , name: "Group1.10"
        ).save(failOnError: true)
    }

    def cleanup() {
    }


    void "given a projection and a range, fetch all variants that fall in it"() {
        given: "a projection, query start and end"
        Organism organism = Organism.first()
        String sequenceName = '{"sequenceList":[{"name":"Group1.1","start":0,"end":1382403,"reverse":false}]}'
        int queryStart = 239000
        int queryEnd = 500500
        MultiSequenceProjection projection = projectionService.getProjection(sequenceName, organism)

        when: "we try to open the VCF"
        VCFFileReader vcfFileReader
        try {
            File file = new File(vcfFile1)
            vcfFileReader = new VCFFileReader(file)
        } catch(Exception e) {
            println "${e.printStackTrace()}"
        }

        then: "we should have a proper valid file handle"
        assert vcfFileReader != null

        when: "we first query for a histogram"
        JSONArray binsArray = new JSONArray()
        vcfService.getFeatureDensitiesForRegion(binsArray, organism, projection, vcfFileReader, queryStart, queryEnd, 25, 10460)

        then: "we get the binned histogram array"
        assert binsArray.size() == 25
        assert binsArray.first() == 2
        assert binsArray.last() == 3

        when: "we query  VCF for variants"
        JSONArray featuresArray = new JSONArray()
        vcfService.processProjection(featuresArray, organism, projection, vcfFileReader, queryStart, queryEnd)

        then: "we see the variants"
        assert featuresArray.size() == 5

        then: "variants should have specific properties"
        JSONObject firstVariantObject = featuresArray.first()
        JSONObject lastVariantObject = featuresArray.last()

        assert firstVariantObject.start == 239046
        assert firstVariantObject.end == 239047
        assert firstVariantObject.type == "SNV"
        assert firstVariantObject.description == "SNV C -> G"
        assert firstVariantObject.containsKey(FeatureStringEnum.REFERENCE_ALLELE.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.ALTERNATIVE_ALLELES.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.SCORE.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.GENOTYPES.value)

        assert lastVariantObject.start == 500124
        assert lastVariantObject.end == 500125
        assert lastVariantObject.type == "SNV"
        assert lastVariantObject.description == "SNV A -> G"
        assert lastVariantObject.containsKey(FeatureStringEnum.REFERENCE_ALLELE.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.ALTERNATIVE_ALLELES.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.SCORE.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.GENOTYPES.value)
    }


    void "given a sequence and a range, fetch all variants that fall in it"() {
        given: "a sequence, query start and end"
        Organism organism = Organism.first()
        String sequenceName = 'Group1.10'
        int queryStart = 10000
        int queryEnd = 12500

        when: "we try to open the VCF"
        VCFFileReader vcfFileReader
        try {
            File file = new File(vcfFile1)
            vcfFileReader = new VCFFileReader(file)
        } catch(Exception e) {
            println "${e.printStackTrace()}"
        }

        then: "we should have a proper valid file handle"
        assert vcfFileReader != null

        when: "we first query for a histogram"
        JSONArray binsArray = new JSONArray()
        vcfService.getFeatureDensitiesForRegion(binsArray, organism, sequenceName, vcfFileReader, queryStart, queryEnd, 25, 100)

        then: "we get the binned histogram array"
        assert binsArray.size() == 25
        assert binsArray[4] == 1
        assert binsArray[22] == 1

        when: "we query VCF for variants"
        JSONArray featuresArray = new JSONArray()
        vcfService.processSequence(featuresArray, organism, sequenceName, vcfFileReader, queryStart, queryEnd)

        then: "we see the variants"
        assert featuresArray.size() == 2

        then: "variants should have specific properties"
        JSONObject firstVariantObject = featuresArray.first()
        JSONObject lastVariantObject = featuresArray.last()

        assert firstVariantObject.start == 10455
        assert firstVariantObject.end == 10456
        assert firstVariantObject.type == "SNV"
        assert firstVariantObject.description == "SNV T -> G"
        assert firstVariantObject.containsKey(FeatureStringEnum.REFERENCE_ALLELE.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.ALTERNATIVE_ALLELES.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.SCORE.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.GENOTYPES.value)

        assert lastVariantObject.start == 12225
        assert lastVariantObject.end == 12226
        assert lastVariantObject.type == "SNV"
        assert lastVariantObject.description == "SNV A -> G"
        assert lastVariantObject.containsKey(FeatureStringEnum.REFERENCE_ALLELE.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.ALTERNATIVE_ALLELES.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.SCORE.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.GENOTYPES.value)
    }

    void "given a reverse projection and a range, fetch all variants that fall int it"() {
        given: "a projection, query start and end"
        Organism organism = Organism.first()
        String sequenceName = '{"sequenceList":[{"name":"Group1.1","start":0,"end":1382403,"reverse":true}]}'
        int queryStart = 880000
        int queryEnd = 1200000
        MultiSequenceProjection projection = projectionService.getProjection(sequenceName, organism)

        when: "we try to open the VCF"
        VCFFileReader vcfFileReader
        try {
            File file = new File(vcfFile1)
            vcfFileReader = new VCFFileReader(file)
        } catch(Exception e) {
            println "${e.printStackTrace()}"
        }

        then: "we should have a proper valid file handle"
        assert vcfFileReader != null

        when: "we first query for a histogram"
        JSONArray binsArray = new JSONArray()
        vcfService.getFeatureDensitiesForRegion(binsArray, organism, projection, vcfFileReader, queryStart, queryEnd, 25, 12800)

        then: "we get the binned histogram array"
        assert binsArray.size() == 25
        assert binsArray[4] == 2
        assert binsArray[24] == 3

        when: "we query VCF for variants"
        JSONArray featuresArray = new JSONArray()
        vcfService.processProjection(featuresArray, organism, projection, vcfFileReader, queryStart, queryEnd)

        then: "we see the variants"
        assert featuresArray.size() == 5

        then: "variants should have specific properties"
        JSONObject firstVariantObject = featuresArray.first()
        JSONObject lastVariantObject = featuresArray.last()

        assert firstVariantObject.start == 1143356
        assert firstVariantObject.end == 1143357
        assert firstVariantObject.type == "SNV"
        assert firstVariantObject.description == "SNV C -> G"
        assert firstVariantObject.containsKey(FeatureStringEnum.REFERENCE_ALLELE.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.ALTERNATIVE_ALLELES.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.SCORE.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.GENOTYPES.value)

        assert lastVariantObject.start == 882278
        assert lastVariantObject.end == 882279
        assert lastVariantObject.type == "SNV"
        assert lastVariantObject.description == "SNV A -> G"
        assert lastVariantObject.containsKey(FeatureStringEnum.REFERENCE_ALLELE.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.ALTERNATIVE_ALLELES.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.SCORE.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.GENOTYPES.value)
    }

    void "fetch variants and its genotypes"() {
        given: "a projection, query start and end"
        Organism organism = Organism.first()
        String sequenceName = '{"sequenceList":[{"name":"Group1.1","start":0,"end":1382403,"reverse":false}]}'
        int queryStart = 2000
        int queryEnd = 5000
        MultiSequenceProjection projection = projectionService.getProjection(sequenceName, organism)

        when: "we try to open the VCF"
        VCFFileReader vcfFileReader
        try {
            File file = new File(vcfFile2)
            vcfFileReader = new VCFFileReader(file)
        } catch(Exception e) {
            println "${e.printStackTrace()}"
        }

        then: "we should have a proper valid file handle"
        assert vcfFileReader != null

        when: "we query VCF for variants"
        JSONArray featuresArray = new JSONArray()
        vcfService.processProjection(featuresArray, organism, projection, vcfFileReader, queryStart, queryEnd)

        then: "we see the variants"
        assert featuresArray.size() == 11

        then: "variants should have specific properties"
        JSONObject firstVariantObject = featuresArray.first()
        JSONObject lastVariantObject = featuresArray.last()

        assert firstVariantObject.start == 2999
        assert firstVariantObject.end == 3000
        assert firstVariantObject.type == "SNV"
        assert firstVariantObject.name == "rs17883296"
        assert firstVariantObject.description == "SNV G -> T"
        assert firstVariantObject.containsKey(FeatureStringEnum.REFERENCE_ALLELE.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.ALTERNATIVE_ALLELES.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.SCORE.value)
        assert firstVariantObject.containsKey(FeatureStringEnum.GENOTYPES.value)

        assert lastVariantObject.start == 4073
        assert lastVariantObject.end == 4074
        assert lastVariantObject.type == "SNV"
        assert lastVariantObject.name == "rs17884260"
        assert lastVariantObject.description == "SNV T -> G"
        assert lastVariantObject.containsKey(FeatureStringEnum.REFERENCE_ALLELE.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.ALTERNATIVE_ALLELES.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.SCORE.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.GENOTYPES.value)
        assert lastVariantObject.containsKey(FeatureStringEnum.FILTER.value)

        when: "we get the genotypes for a specific variant"
        JSONObject firstVariantGenotypeObject = new JSONObject()
        vcfService.getGenotypes(firstVariantGenotypeObject, organism, projection, vcfFileReader, firstVariantObject.getInt("start"), firstVariantObject.getInt("end"))
        JSONObject lastVariantGenotypeObject = new JSONObject()
        vcfService.getGenotypes(lastVariantGenotypeObject, organism, projection, vcfFileReader, lastVariantObject.getInt("start"), lastVariantObject.getInt("end"))

        then: "we should have all the genotypes"
        JSONObject sampleGenotypeObject = firstVariantGenotypeObject.getJSONObject("HG00100")
        assert firstVariantGenotypeObject.size() == 1094
        assert lastVariantGenotypeObject.size() == 1094
        assert sampleGenotypeObject.containsKey("GT")
        assert sampleGenotypeObject.containsKey("AP")
    }


    void "fetch variants for an invalid range"() {
        given: "a projection, query start and end"
        Organism organism = Organism.first()
        String sequenceName = '{"sequenceList":[{"name":"Group1.1","start":0,"end":1382403,"reverse":false}]}'
        int invalidQueryStart1 = -50000
        int invalidQueryEnd1 = -10000
        int invalidQueryStart2 = 1400000
        int invalidQueryEnd2 = 1450000
        int invalidQueryStart3 = -1000
        int invalidQueryEnd3 = 4000
        int invalidQueryStart4 = 1382000
        int invalidQueryEnd4 = 1385000

        MultiSequenceProjection projection = projectionService.getProjection(sequenceName, organism)

        when: "we try to open the VCF"
        VCFFileReader vcfFileReader
        try {
            File file = new File(vcfFile2)
            vcfFileReader = new VCFFileReader(file)
        } catch(Exception e) {
            println "${e.printStackTrace()}"
        }

        then: "we should have a proper valid file handle"
        assert vcfFileReader != null

        when: "we query VCF for variants with an invalid query start and end"
        JSONArray featuresArray = new JSONArray()
        vcfService.processProjection(featuresArray, organism, projection, vcfFileReader, invalidQueryStart1, invalidQueryEnd1)

        then: "we see an empty array"
        assert featuresArray.size() == 0

        when: "we query VCF for variants with an query start and end larger than the scaffold size itself"
        featuresArray.clear()
        vcfService.processProjection(featuresArray, organism, projection, vcfFileReader, invalidQueryStart2, invalidQueryEnd2)

        then: "we see an empty array"
        assert featuresArray.size() == 0

        when: "we query VCF for variants with an invalid start but a valid end"
        featuresArray.clear()
        vcfService.processProjection(featuresArray, organism, projection, vcfFileReader, invalidQueryStart3, invalidQueryEnd3)

        then: "we see the results for an adjusted region"
        assert featuresArray.size() == 8

        when: "we query VCF for variants with a valid start but an invalid end"
        featuresArray.clear()
        vcfService.processProjection(featuresArray, organism, projection, vcfFileReader, invalidQueryStart4, invalidQueryEnd4)

        then: "we see the results for an adjusted region"
        assert featuresArray.size() == 2
    }

}
