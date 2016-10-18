package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

class OverlapperServiceIntegrationSpec extends AbstractIntegrationSpec{

    def requestHandlingService


    void "isoform overlap test for GB40772-RA loci"() {

        given: "A set of isoforms at GB40772-RA loci"
        String mainTranscript1 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":222081,\"strand\":-1,\"fmax\":222245},\"name\":\"GB40772-RA\",\"children\":[{\"location\":{\"fmin\":222081,\"strand\":-1,\"fmax\":222245},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String mainTranscript2 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":222043,\"strand\":1,\"fmax\":222267},\"name\":\"au9.g284.t1\",\"children\":[{\"location\":{\"fmin\":222043,\"strand\":1,\"fmax\":222267},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        // isoforms for mainTranscript1 : GB40772-RA
        String isoform1 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":222081,\"strand\":-1,\"fmax\":222211},\"name\":\"au12.g285.t1\",\"children\":[{\"location\":{\"fmin\":222081,\"strand\":-1,\"fmax\":222211},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String isoform2 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":222081,\"strand\":-1,\"fmax\":222245},\"name\":\"fgeneshpp_with_rnaseq_Group1.10_57_mRNA\",\"children\":[{\"location\":{\"fmin\":222081,\"strand\":-1,\"fmax\":222245},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String isoform3 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":222081,\"strand\":-1,\"fmax\":222183},\"name\":\"au9.g283.t1\",\"children\":[{\"location\":{\"fmin\":222081,\"strand\":-1,\"fmax\":222183},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        // isoform for mainTranscript2 : au9.g284.t1
        String isoform4 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":222041,\"strand\":1,\"fmax\":222267},\"name\":\"au12.g286.t1\",\"children\":[{\"location\":{\"fmin\":222041,\"strand\":1,\"fmax\":222267},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        when: "we add mainTranscript1 and mainTranscript2"
        JSONObject mainTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(mainTranscript1) as JSONObject)
        JSONObject mainTranscript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(mainTranscript2) as JSONObject)

        then: "we should see 2 Genes and 2 MRNAs"
        assert Gene.count == 2
        assert MRNA.count == 2
        JSONObject features1 = mainTranscript1ReturnObject.get("features")
        JSONObject features2 = mainTranscript2ReturnObject.get("features")
        String mrna1Parent = features1.parent_id
        String mrna2Parent = features2.parent_id

        when: "we add isoform1: au12.g285.t1"
        JSONObject isoform1ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform1) as JSONObject)

        then: "we should expect the added isoform to have its parent similar to mainTranscript1"
        JSONObject isoform1Features = isoform1ReturnObject.get("features")
        String isoform1Parent = isoform1Features.parent_id

        assert isoform1Parent == mrna1Parent

        when: "we add isoform2: fgeneshpp_with_rnaseq_Group1.10_57_mRNA"
        JSONObject isoform2ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform2) as JSONObject)

        then: "we should expect the added isoform to have its parent similar to mainTranscript1"
        JSONObject isoform2Features = isoform2ReturnObject.get("features")
        String isoform2Parent = isoform2Features.parent_id

        assert isoform2Parent == mrna1Parent

        when: "we add isoform3: au9.g283.t1"
        JSONObject isoform3ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform3) as JSONObject)

        then: "we should expect the added isoform to have its parent similar to mainTranscript1"
        JSONObject isoform3Features = isoform3ReturnObject.get("features")
        String isoform3Parent = isoform3Features.parent_id

        assert isoform3Parent == mrna1Parent

        when: "we add isoform4: au12.g286.t1"
        JSONObject isoform4ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform4) as JSONObject)

        then: "we should expect the added isoform to have its parent similar to mainTranscript2"
        JSONObject isoform4Features = isoform4ReturnObject.get("features")
        String isoform4Parent = isoform4Features.parent_id

        assert isoform4Parent == mrna2Parent
    }

    void "isoform overlap test for GB40740-RA loci"() {

        given: "A set of isoforms at GB40740-RA loci"
        String mainTranscript1 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":787022,\"strand\":-1,\"fmax\":836988},\"name\":\"GB40740-RA\",\"children\":[{\"location\":{\"fmin\":787022,\"strand\":-1,\"fmax\":787740},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":787022,\"strand\":-1,\"fmax\":788349},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":789768,\"strand\":-1,\"fmax\":790242},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":791007,\"strand\":-1,\"fmax\":791466},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":791853,\"strand\":-1,\"fmax\":792220},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":793652,\"strand\":-1,\"fmax\":793876},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":806935,\"strand\":-1,\"fmax\":807266},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":828378,\"strand\":-1,\"fmax\":829272},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":836953,\"strand\":-1,\"fmax\":836988},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":787740,\"strand\":-1,\"fmax\":836988},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",\"clientToken\":\"123123\"}"

        // isoforms for mainTranscript1
        String overlappingTranscript1 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":828378,\"strand\":-1,\"fmax\":829272},\"name\":\"fgeneshpp_with_rnaseq_Group1.10_188_mRNA\",\"children\":[{\"location\":{\"fmin\":828378,\"strand\":-1,\"fmax\":829272},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",\"clientToken\":\"123123\"}"
        String overlappingTranscript2 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":825210,\"strand\":-1,\"fmax\":831086},\"name\":\"fgeneshpp_with_rnaseq_Group1.10_188_mRNA\",\"children\":[{\"location\":{\"fmin\":827237,\"strand\":-1,\"fmax\":827327},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":825210,\"strand\":-1,\"fmax\":825237},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":831083,\"strand\":-1,\"fmax\":831086},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":828378,\"strand\":-1,\"fmax\":829272},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":825210,\"strand\":-1,\"fmax\":831086},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",\"clientToken\":\"123123\"}"

        when: "we add mainTranscript1"
        JSONObject mainTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(mainTranscript1) as JSONObject)

        then: "we should see 1 Gene and 1 MRNA"
        assert Gene.count == 1
        assert MRNA.count == 1
        JSONObject features1 = mainTranscript1ReturnObject.get("features")
        String mrna1Parent = features1.parent_id

        when: "we add overlappingTranscript1: fgeneshpp_with_rnaseq_Group1.10_188_mRNA"
        JSONObject overlappingTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(overlappingTranscript1) as JSONObject)

        then: "we should expect the added transcript to have its parent different from mainTranscript1"
        JSONObject overlappingTranscript1Features = overlappingTranscript1ReturnObject.get("features")
        String overlappingTranscript1Parent = overlappingTranscript1Features.parent_id

        assert overlappingTranscript1Parent != mrna1Parent

        when: "we add overlappingTranscript2: fgeneshpp_with_rnaseq_Group1.10_188_mRNA"
        JSONObject overlappingTranscript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(overlappingTranscript2) as JSONObject)

        then: "we should expect the added transcript to have its parent different from mainTranscript1"
        JSONObject overlappingTranscript2Features = overlappingTranscript2ReturnObject.get("features")
        String overlappingTranscript2Parent = overlappingTranscript2Features.parent_id

        assert overlappingTranscript2Parent != mrna1Parent
    }

    void "isoform overlap test for GB40730-RA loci"() {

        given: "A set of isoforms at GB40730-RA loci"
        String mainTranscript1 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1003945,\"strand\":-1,\"fmax\":1018115},\"name\":\"GB40730-RA\",\"children\":[{\"location\":{\"fmin\":1003945,\"strand\":-1,\"fmax\":1004025},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1003945,\"strand\":-1,\"fmax\":1004076},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1004578,\"strand\":-1,\"fmax\":1004734},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1004809,\"strand\":-1,\"fmax\":1004914},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1005040,\"strand\":-1,\"fmax\":1005140},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1009738,\"strand\":-1,\"fmax\":1009989},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1010378,\"strand\":-1,\"fmax\":1010633},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1011391,\"strand\":-1,\"fmax\":1011775},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1017983,\"strand\":-1,\"fmax\":1018115},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1004025,\"strand\":-1,\"fmax\":1018115},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"use_cds\":\"true\"}],\"track\":\"Group1.10\",'clientToken':'123123'}"
        String mainTranscript2 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1007258,\"strand\":1,\"fmax\":1012113},\"name\":\"GB40834-RA\",\"children\":[{\"location\":{\"fmin\":1007258,\"strand\":1,\"fmax\":1007559},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1009993,\"strand\":1,\"fmax\":1010650},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1011441,\"strand\":1,\"fmax\":1012113},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1007258,\"strand\":1,\"fmax\":1007577},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1009768,\"strand\":1,\"fmax\":1010650},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1011441,\"strand\":1,\"fmax\":1012113},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1007559,\"strand\":1,\"fmax\":1009993},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"use_cds\":\"true\"}],\"track\":\"Group1.10\",'clientToken':'123123'}"

        // isoforms for mainTranscript1 : GB40730-RA
        String isoform1 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1002555,\"strand\":-1,\"fmax\":1011938},\"name\":\"au8.g331.t1\",\"children\":[{\"location\":{\"fmin\":1002555,\"strand\":-1,\"fmax\":1002672},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1011721,\"strand\":-1,\"fmax\":1011938},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1002555,\"strand\":-1,\"fmax\":1002711},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1004578,\"strand\":-1,\"fmax\":1004734},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1004809,\"strand\":-1,\"fmax\":1004914},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1005040,\"strand\":-1,\"fmax\":1005140},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1009738,\"strand\":-1,\"fmax\":1009989},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1010378,\"strand\":-1,\"fmax\":1010633},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1011391,\"strand\":-1,\"fmax\":1011938},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1002672,\"strand\":-1,\"fmax\":1011721},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"use_cds\":\"true\"}],\"track\":\"Group1.10\",'clientToken':'123123'}"

        // isoforms for mainTranscript2 : GB40834-RA
        String isoform2 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1009768,\"strand\":1,\"fmax\":1010650},\"name\":\"fgeneshpp_with_rnaseq_Group1.10_219_mRNA\",\"children\":[{\"location\":{\"fmin\":1009768,\"strand\":1,\"fmax\":1010650},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}, \"use_cds\":\"true\"}],\"track\":\"Group1.10\",'clientToken':'123123'}"
        String isoform3 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1007258,\"strand\":1,\"fmax\":1012113},\"name\":\"fgeneshpp_with_rnaseq_Group1.10_219_mRNA\",\"children\":[{\"location\":{\"fmin\":1007258,\"strand\":1,\"fmax\":1007577},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1009768,\"strand\":1,\"fmax\":1010650},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1011441,\"strand\":1,\"fmax\":1012113},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1007258,\"strand\":1,\"fmax\":1007559},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1009993,\"strand\":1,\"fmax\":1010650},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1011441,\"strand\":1,\"fmax\":1012113},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1007559,\"strand\":1,\"fmax\":1009993},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}, \"use_cds\":\"true\"}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"use_cds\":\"true\"}],\"track\":\"Group1.10\",'clientToken':'123123'}"

        when: "we add mainTranscript1 and mainTranscript2"
        JSONObject mainTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(mainTranscript1) as JSONObject)
        JSONObject mainTranscript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(mainTranscript2) as JSONObject)

        then: "we should see 2 Genes and 2 MRNAs"
        assert Gene.count == 2
        assert MRNA.count == 2
        JSONObject features1 = mainTranscript1ReturnObject.get("features")
        JSONObject features2 = mainTranscript2ReturnObject.get("features")
        String mrna1Parent = features1.parent_id
        String mrna2Parent = features2.parent_id

        when: "we add isoform1: au8.g331.t1"
        JSONObject isoform1ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform1) as JSONObject)

        then: "we should expect the added isoform to have its parent similar to mainTranscript1"
        JSONObject isoform1Features = isoform1ReturnObject.get("features")
        String isoform1Parent = isoform1Features.parent_id

        assert isoform1Parent == mrna1Parent

        when: "we add isoform2: fgeneshpp_with_rnaseq_Group1.10_219_mRNA"
        JSONObject isoform2ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform2) as JSONObject)

        then: "we should expect the added isoform to not have its parent similar to mainTranscript2"
        JSONObject isoform2Features = isoform2ReturnObject.get("features")
        String isoform2Parent = isoform2Features.parent_id

        assert isoform2Parent != mrna2Parent

        when: "we add isoform3: fgeneshpp_with_rnaseq_Group1.10_219_mRNA"
        JSONObject isoform3ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform3) as JSONObject)

        then: "we should expect the added isoform to have its parent similar to mainTranscript2"
        JSONObject isoform3Features = isoform3ReturnObject.get("features")
        String isoform3Parent = isoform3Features.parent_id

        assert isoform3Parent == mrna2Parent
    }

    void "isoform overlap test for GB40797-RA loci"() {

        given: "A set of isoforms at GB40797-RA loci"
        String mainTranscript1 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":128706,\"strand\":1,\"fmax\":136964},\"name\":\"GB40797-RA\",\"children\":[{\"location\":{\"fmin\":136502,\"strand\":1,\"fmax\":136964},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":128706,\"strand\":1,\"fmax\":128768},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131040,\"strand\":1,\"fmax\":131220},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131322,\"strand\":1,\"fmax\":131487},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131749,\"strand\":1,\"fmax\":131964},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":132025,\"strand\":1,\"fmax\":132264},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":132394,\"strand\":1,\"fmax\":132620},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":133815,\"strand\":1,\"fmax\":133832},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":134173,\"strand\":1,\"fmax\":134353},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":134476,\"strand\":1,\"fmax\":134684},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":135693,\"strand\":1,\"fmax\":136260},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":136335,\"strand\":1,\"fmax\":136964},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":128706,\"strand\":1,\"fmax\":136502},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",'clientToken':'123123'}"

        // isoforms for mainTranscript1 : GB40797-RA
        String isoform1 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":130713,\"strand\":1,\"fmax\":133159},\"name\":\"au9.g263.t1\",\"children\":[{\"location\":{\"fmin\":130713,\"strand\":1,\"fmax\":130945},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131040,\"strand\":1,\"fmax\":131044},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":132749,\"strand\":1,\"fmax\":133159},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":130713,\"strand\":1,\"fmax\":130945},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131040,\"strand\":1,\"fmax\":131220},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131322,\"strand\":1,\"fmax\":131487},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131749,\"strand\":1,\"fmax\":131964},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":132025,\"strand\":1,\"fmax\":132264},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":132394,\"strand\":1,\"fmax\":132620},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":132699,\"strand\":1,\"fmax\":133159},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":131044,\"strand\":1,\"fmax\":132749},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",'clientToken':'123123'}"
        String isoform2 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":133703,\"strand\":1,\"fmax\":139070},\"name\":\"au9.g264.t1\",\"children\":[{\"location\":{\"fmin\":133703,\"strand\":1,\"fmax\":133832},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":134173,\"strand\":1,\"fmax\":134185},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":136502,\"strand\":1,\"fmax\":138237},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":138489,\"strand\":1,\"fmax\":139070},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":133703,\"strand\":1,\"fmax\":133832},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":134173,\"strand\":1,\"fmax\":134353},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":134476,\"strand\":1,\"fmax\":134684},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":135693,\"strand\":1,\"fmax\":136260},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":136335,\"strand\":1,\"fmax\":138237},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":138489,\"strand\":1,\"fmax\":139070},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":134185,\"strand\":1,\"fmax\":136502},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",'clientToken':'123123'}"
        String isoform3 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":130711,\"strand\":1,\"fmax\":130945},\"name\":\"au12.g266.t1\",\"children\":[{\"location\":{\"fmin\":130711,\"strand\":1,\"fmax\":130945},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",'clientToken':'123123'}"

        when: "we add mainTranscript1"
        JSONObject mainTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(mainTranscript1) as JSONObject)

        then: "we should see 1 Gene and 1 MRNA"
        assert Gene.count == 1
        assert MRNA.count == 1
        JSONObject features1 = mainTranscript1ReturnObject.get("features")
        String mrna1Parent = features1.parent_id

        when: "we add isoform1: au9.g263.t1"
        JSONObject isoform1ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform1) as JSONObject)

        then: "we should expect the added isoform to have its parent similar to mainTranscript1"
        JSONObject isoform1Features = isoform1ReturnObject.get("features")
        String isoform1Parent = isoform1Features.parent_id

        assert isoform1Parent == mrna1Parent

        when: "we add isoform2: au9.g264.t1"
        JSONObject isoform2ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform2) as JSONObject)

        then: "we should expect the added isoform to have its parent similar to mainTranscript1"
        JSONObject isoform2Features = isoform2ReturnObject.get("features")
        String isoform2Parent = isoform2Features.parent_id

        assert isoform2Parent == mrna1Parent

        when: "we add isoform3: au12.g266.t1"
        JSONObject isoform3ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform3) as JSONObject)

        then: "we should expect the added isoform to have its parent similar to mainTranscript1"
        JSONObject isoform3Features = isoform3ReturnObject.get("features")
        String isoform3Parent = isoform3Features.parent_id

        assert isoform3Parent != mrna1Parent
    }

    void "isoform overlap test for GB40810-RA loci"() {

        given: "A set of isoforms at GB40810-RA loci"
        String mainTranscript1 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":335756,\"strand\":1,\"fmax\":337187},\"name\":\"GB40810-RA\",\"children\":[{\"location\":{\"fmin\":335756,\"strand\":1,\"fmax\":336018},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":335756,\"strand\":1,\"fmax\":336120},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":336248,\"strand\":1,\"fmax\":336302},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":336471,\"strand\":1,\"fmax\":336855},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":336923,\"strand\":1,\"fmax\":336954},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":337080,\"strand\":1,\"fmax\":337187},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":336018,\"strand\":1,\"fmax\":337187},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",'clientToken':'123123'}"

        // isoforms for mainTranscript1
        String isoform1 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":335908,\"strand\":1,\"fmax\":336120},\"name\":\"au12.g294.t1\",\"children\":[{\"location\":{\"fmin\":335908,\"strand\":1,\"fmax\":336120},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",'clientToken':'123123'}"
        String isoform2 = "{${testCredentials} \"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":335944,\"strand\":1,\"fmax\":337531},\"name\":\"au8.g298.t1\",\"children\":[{\"location\":{\"fmin\":335944,\"strand\":1,\"fmax\":336018},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":337187,\"strand\":1,\"fmax\":337531},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":335944,\"strand\":1,\"fmax\":336120},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":336248,\"strand\":1,\"fmax\":336302},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":336471,\"strand\":1,\"fmax\":336855},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":336923,\"strand\":1,\"fmax\":337531},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":336018,\"strand\":1,\"fmax\":337187},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\",'clientToken':'123123'}"

        when: "we add mainTranscript1"
        JSONObject mainTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(mainTranscript1) as JSONObject)

        then: "we should see 1 Gene and 1 MRNA"
        assert Gene.count == 1
        assert MRNA.count == 1
        JSONObject features1 = mainTranscript1ReturnObject.get("features")
        String mrna1Parent = features1.parent_id

        when: "we add isoform1: au12.g294.t1"
        JSONObject isoform1ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform1) as JSONObject)

        then: "we should expect the added isoform to have its parent similar to mainTranscript1"
        JSONObject isoform1Features = isoform1ReturnObject.get("features")
        String isoform1Parent = isoform1Features.parent_id

        assert isoform1Parent == mrna1Parent

        when: "we add isoform2: au8.g298.t1"
        JSONObject isoform2ReturnObject = requestHandlingService.addTranscript(JSON.parse(isoform2) as JSONObject)

        then: "we should expect the added isoform to have its parent similar to mainTranscript1"
        JSONObject isoform2Features = isoform2ReturnObject.get("features")
        String isoform2Parent = isoform2Features.parent_id

        assert isoform2Parent == mrna1Parent
    }
}
