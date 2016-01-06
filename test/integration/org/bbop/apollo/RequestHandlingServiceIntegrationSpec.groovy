package org.bbop.apollo

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.Strand
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class RequestHandlingServiceIntegrationSpec extends AbstractIntegrationSpec {

    def requestHandlingService
    def featureRelationshipService
    def transcriptService
    def exonService
    def sequenceService
    def projectionService

    def setup() {
        setupDefaultUserOrg()
        projectionService.clearProjections()
    }

    def cleanup() {
    }

    void "add transcript with UTR"() {

        given: "a transcript with a UTR"
        String jsonString = " { \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235534,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1216850,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1224676,\"fmax\":1224823,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1228682,\"fmax\":1228825,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235237,\"fmax\":1235396,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235487,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1235534,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"

        String validJSONString = "{\"features\":[{\"location\":{\"fmin\":1216824,\"strand\":1,\"fmax\":1235616},\"parent_type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235237,\"strand\":1,\"fmax\":1235396},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"@TRANSCRIPT_NAME@\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1216824,\"strand\":1,\"fmax\":1216850},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"0992325F0DD2290AB58EA37ECF2DA2E7\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1235487,\"strand\":1,\"fmax\":1235616},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"1C091FE87A8133803A69887F38FBDC4C\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209542,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1224676,\"strand\":1,\"fmax\":1224823},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"6D2E15D6DA759C523B79B96795927CAF\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1228682,\"strand\":1,\"fmax\":1228825},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"99C2A027C87DBDBC5536503D5C38F21C\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1216824,\"strand\":1,\"fmax\":1235534},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"994B96C6594F5DB1B6C836E6E0EDE2A6\",\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"}],\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"5A8C864885BC71606E120322CE0EC28C\",\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209602,\"parent_id\":\"8B9E9AC4D0DB90464F26B2F77A1E09B4\"}]}"


        when: "You add a transcript via JSON"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        JSONObject correctJsonReturnObject = JSON.parse(validJSONString) as JSONObject

        then: "there should be no features"
        assert Feature.count == 0
        assert FeatureLocation.count == 0
        assert Sequence.count == 1
        JSONArray mrnaArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == mrnaArray.size()
        assert 7 == getCodingArray(jsonObject).size()



        when: "you parse add a transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)



        then: "You should see that transcript"
        assert Sequence.count == 1
        // there are 6 exons, but 2 of them overlap . . . so this is correct
        assert Exon.count == 5
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1
        def allFeatures = Feature.all

        // this is the new part
        assert FeatureLocation.count == 8
        assert Feature.count == 8


        JSONArray returnedCodingArray = getCodingArray(returnObject)
        JSONArray validCodingArray = getCodingArray(correctJsonReturnObject)
        assert returnedCodingArray.size() == validCodingArray.size()

//        assert "ADD"==returnObject.getString("operation")
//        assert Gene.count == 1
//        assert Gene.first().name=="Bob1"

    }


    void "add a transcript which is a single exon needs to translate correctly"() {

        given: "the input string "
        String jsonString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB42152-RA\",\"children\":[{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}], \"operation\": \"add_transcript\" }"

        when: "You add a transcript via JSON"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        then: "there should be no features"
        assert Feature.count == 0
        assert FeatureLocation.count == 0
        assert Sequence.count == 1
        JSONArray mrnaArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == mrnaArray.size()
        JSONArray codingArray = mrnaArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 1 == codingArray.size()

        when: "it gets added"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)


        then: "we should see the appropriate stuff"
        assert Sequence.count == 1
        // there are 6 exons, but 2 of them overlap . . . so this is correct
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1
        assert Exon.count == 1
//        int flankingCount = FlankingRegion.count
        int flankingCount = 0
        assert Feature.count == 4 + flankingCount
        assert FeatureLocation.count == 4 + flankingCount
        assert FeatureRelationship.count == 3

        Gene gene = Gene.first()
        assert featureRelationshipService.getParentForFeature(gene) == null
        assert featureRelationshipService.getChildren(gene).size() == 1
        MRNA mrna = featureRelationshipService.getChildForFeature(gene, MRNA.ontologyId)
        assert mrna.id == MRNA.first().id
        List<Feature> childFeatureRelationships = featureRelationshipService.getParentsForFeature(mrna)
        assert 1 == childFeatureRelationships.size()
        Feature parentFeature = featureRelationshipService.getParentForFeature(mrna)
        assert parentFeature != null
        assert featureRelationshipService.getParentForFeature(mrna).id == gene.id
        // should be an exon and a CDS . . .
        assert featureRelationshipService.getChildren(mrna).size() == 2
        Exon exon = featureRelationshipService.getChildForFeature(mrna, Exon.ontologyId)
        CDS cds = featureRelationshipService.getChildForFeature(mrna, CDS.ontologyId)
        assert exon != null
        assert cds != null
//        MRNA mrna = featureRelationshipService.getChildForFeature(mrna)

    }

    /**
     * TODO: note, this sequence is for 1.1
     */
//    void "adding a transcript that returns a missing feature location in the mRNA"(){
//
//        given: "a input JSON string"
//        String jsonString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":976735,\"fmax\":995721,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB42183-RA\",\"children\":[{\"location\":{\"fmin\":995216,\"fmax\":995721,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":976735,\"fmax\":976888,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":992139,\"fmax\":992559,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":992748,\"fmax\":993041,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":993307,\"fmax\":995721,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":976735,\"fmax\":995216,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
//
//        when: "we parse the string"
//        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
//
//        then: "we get a valid json object and no features"
//        assert Feature.count == 0
//
//        when: "we add it to a UTR"
//        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)
//
//        then: "we should get a transcript back" // we currently get nothing
//
////        def allFeatures = Feature.all
////        int flankingRegionCount = FlankingRegion.count
////        assert Feature.count == 7 + flankingRegionCount
//        assert returnObject.getString('operation')=="ADD"
//        assert returnObject.getBoolean('sequenceAlterationEvent')==false
//        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
//        assert 1==featuresArray.size()
//        JSONObject mrna = featuresArray.getJSONObject(0)
//        assert "GB42183-RA-00001"==mrna.getString(FeatureStringEnum.NAME.value)
//        JSONArray children = mrna.getJSONArray(FeatureStringEnum.CHILDREN.value)
//        assert 5==children.size()
//        for(int i = 0 ; i < 5 ; i++){
//            JSONObject codingObject = children.get(i)
//            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
//            assert locationObject!=null
//        }
//
//    }

    /**
     * TODO: note, this sequence is for 1.1
     */
//    void "adding another transcript with UTR fails to add GB42152-RA"(){
//        given: "a input JSON string"
//        String jsonString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":561645,\"fmax\":566383,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB42152-RA\",\"children\":[{\"location\":{\"fmin\":566169,\"fmax\":566383,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":561645,\"fmax\":562692,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":561645,\"fmax\":564771,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":564936,\"fmax\":565087,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":565410,\"fmax\":565655,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":566040,\"fmax\":566383,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":562692,\"fmax\":566169,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
//
//        when: "we parse the string"
//        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
//
//        then: "we get a valid json object and no features"
//        assert Feature.count == 0
//
//        when: "we add it to a UTR"
//        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)
//
//        then: "we should get a transcript back" // we currently get nothing
//        int flankingRegionCount = FlankingRegion.count
//        assert Feature.count == 7 + flankingRegionCount
////        log.debug returnObject as JSON
//        assert returnObject.getString('operation')=="ADD"
//        assert returnObject.getBoolean('sequenceAlterationEvent')==false
//        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
//        assert 1==featuresArray.size()
//        JSONObject mrna = featuresArray.getJSONObject(0)
//        assert "GB42152-RA-00001"==mrna.getString(FeatureStringEnum.NAME.value)
//        JSONArray children = mrna.getJSONArray(FeatureStringEnum.CHILDREN.value)
//        assert 5==children.size()
//        for(int i = 0 ; i < 5 ; i++){
//            JSONObject codingObject = children.get(i)
//            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
//            assert locationObject!=null
//        }
//
//    }

    void "add a transcript with UTR"() {

        given: "a valid JSON gtring"
//        String validInputString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1287738,\"fmax\":1289338,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40717-RA\",\"children\":[{\"location\":{\"fmin\":1289034,\"fmax\":1289338,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1287738,\"fmax\":1288189,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1287738,\"fmax\":1288308,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1288385,\"fmax\":1288491,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1288554,\"fmax\":1288630,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1288942,\"fmax\":1289338,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1288189,\"fmax\":1289034,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String apollo2InputString = "{\"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1289338},\"name\":\"GB40717-RA\",\"children\":[{\"location\":{\"fmin\":1289034,\"strand\":-1,\"fmax\":1289338},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1288189},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1288308},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1288385,\"strand\":-1,\"fmax\":1288491},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1288554,\"strand\":-1,\"fmax\":1288630},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1288942,\"strand\":-1,\"fmax\":1289338},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1288189,\"strand\":-1,\"fmax\":1289034},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String validResponseString = "{\"operation\":\"ADD\",\"sequenceAlterationEvent\":false,\"features\":[{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1289338},\"parent_type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}},\"name\":\"GB40717-RA\",\"children\":[{\"location\":{\"fmin\":1288942,\"strand\":-1,\"fmax\":1289338},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"57ED4B570156EED14312AFB1DC306F2B\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720165,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1288554,\"strand\":-1,\"fmax\":1288630},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"47FBDA96DC15CE1E17E27BD31FB22FE1\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720162,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1288308},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"CBDF08580305EE5199AA49E1B69283FC\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720164,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1288189,\"strand\":-1,\"fmax\":1289034},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"8673F11418891E01DF749A5607D5AE36\",\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720162,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1288385,\"strand\":-1,\"fmax\":1288491},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"30D14467AF67DCBD987DC99B6400F171\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720162,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"}],\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"15495DE1AEB1F224FD04CDB1AF67C166\",\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720165,\"parent_id\":\"B74B31812E992AB75B5E741AC6A3158A\"}]}"

//        String validFeatureString = "{\"features\":[{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1289338},\"parent_type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}},\"name\":\"GB40717-RA\",\"children\":[{\"location\":{\"fmin\":1288942,\"strand\":-1,\"fmax\":1289338},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"57ED4B570156EED14312AFB1DC306F2B\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720165,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1288554,\"strand\":-1,\"fmax\":1288630},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"47FBDA96DC15CE1E17E27BD31FB22FE1\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720162,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1288308},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"CBDF08580305EE5199AA49E1B69283FC\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720164,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1288189,\"strand\":-1,\"fmax\":1289034},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"8673F11418891E01DF749A5607D5AE36\",\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720162,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1288385,\"strand\":-1,\"fmax\":1288491},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"30D14467AF67DCBD987DC99B6400F171\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720162,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"}],\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"15495DE1AEB1F224FD04CDB1AF67C166\",\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720165,\"parent_id\":\"B74B31812E992AB75B5E741AC6A3158A\"}]}"


        when: "we parse the string"
        JSONObject jsonObject = JSON.parse(apollo2InputString) as JSONObject
        JSONObject validJsonObject = JSON.parse(validResponseString) as JSONObject
        JSONArray validCodingArray = getCodingArray(validJsonObject)
//
        then: "we get a valid json object and no features"
        assert Feature.count == 0
//
        when: "add UTR transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)
        JSONArray returnCodingArray = getCodingArray(returnObject)

        then: "we should get no noncanonical splice sites"
        def allFeatures = Feature.all
        assert Gene.count == 1
        assert MRNA.count == 1
        assert CDS.count == 1
        assert Exon.count == 4
//        assert NonCanonicalFivePrimeSpliceSite.count == 0
//        assert NonCanonicalThreePrimeSpliceSite.count == 0

        // not sure if the non-canonical are supposed to be there or not since at the edges
//        assert validCodingArray.size() == returnCodingArray.size()-2


    }

    void "adding an exon to an existing transcript"() {

        given: "a input addTranscriptFeaturesArrayJSON string"
        String jsonString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":219994,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40772-RA\",\"children\":[{\"location\":{\"fmin\":222109,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":220044,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":222081,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":222109,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String exonString = "{ \"track\": \"Group1.10\", \"features\": [ {\"uniquename\": \"@TRANSCRIPT_NAME@\"}, {\"location\":{\"fmin\":218197,\"fmax\":218447,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}], \"operation\": \"add_exon\" }"
        String validExonString = "{\"features\":[{\"location\":{\"fmin\":218197,\"strand\":-1,\"fmax\":222245},\"parent_type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}},\"name\":\"GB40772-RA\",\"children\":[{\"location\":{\"fmin\":219994,\"strand\":-1,\"fmax\":222109},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"45F17D57F6025D3508087E86126E2285\",\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499882556,\"parent_id\":\"@TRANSCRIPT_NAME@\"},{\"location\":{\"fmin\":222081,\"strand\":-1,\"fmax\":222245},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"E7C8734B188875CEE3EC78690FE3F656\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499879959,\"parent_id\":\"@TRANSCRIPT_NAME@\"},{\"location\":{\"fmin\":218197,\"strand\":-1,\"fmax\":218447},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"uniquename\":\"5BD40EB6906EE9E744C099A3E9F84163\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499882554,\"parent_id\":\"@TRANSCRIPT_NAME@\"},{\"location\":{\"fmin\":218447,\"strand\":-1,\"fmax\":218447},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"@TRANSCRIPT_NAME@-non_canonical_three_prime_splice_site-218447\",\"type\":{\"name\":\"non_canonical_three_prime_splice_site\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499882557,\"parent_id\":\"@TRANSCRIPT_NAME@\"},{\"location\":{\"fmin\":219994,\"strand\":-1,\"fmax\":220044},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"380947D69FD4CBE555F9AC46596178CD\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499879958,\"parent_id\":\"@TRANSCRIPT_NAME@\"},{\"location\":{\"fmin\":219994,\"strand\":-1,\"fmax\":219994},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"@TRANSCRIPT_NAME@-non_canonical_five_prime_splice_site-219994\",\"type\":{\"name\":\"non_canonical_five_prime_splice_site\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499882556,\"parent_id\":\"@TRANSCRIPT_NAME@\"}],\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"@TRANSCRIPT_NAME@\",\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499882556,\"parent_id\":\"176745425D3DA350EEFBD5A150554210\"}]}"

        when: "we parse the string"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        then: "we get a valid json object and no features"
        assert Feature.count == 0

        when: "we add the first transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "we should get a transcript back" // we currently get nothing
//        log.debug returnObject as JSON
        assert returnObject.getString('operation') == "ADD"
        assert returnObject.getBoolean('sequenceAlterationEvent') == false
        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == featuresArray.size()
        JSONObject mrna = featuresArray.getJSONObject(0)
        assert Gene.count == 1
        assert MRNA.count == 1
        // we are losing an exon somewhere!
        assert Exon.count == 2
        assert CDS.count == 1
//        assert NonCanonicalFivePrimeSpliceSite.count==1
//        assert NonCanonicalThreePrimeSpliceSite.count==1
//        assert Feature.count == 5
        assert "GB40772-RA-00001" == mrna.getString(FeatureStringEnum.NAME.value)
        String transcriptUniqueName = mrna.getString(FeatureStringEnum.UNIQUENAME.value)
        JSONArray children = mrna.getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 3 == children.size()
        for (int i = 0; i < 3; i++) {
            JSONObject codingObject = children.get(i)
            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
            assert locationObject != null
        }


        when: "we parse the string"
        exonString = exonString.replaceAll("@TRANSCRIPT_NAME@", transcriptUniqueName)
        JSONObject exonJsonObject = JSON.parse(exonString) as JSONObject
        JSONObject validExonJsonObject = JSON.parse(validExonString) as JSONObject
//
//        then: "we get a valid json object and no features"
//        assert Feature.count == 7

//        when: "we add the exon explicitly"
        JSONObject returnedAfterExonObject = requestHandlingService.addExon(exonJsonObject)

        then: "we should see an exon added"
        assert returnedAfterExonObject != null
        log.debug Feature.count
        assert Feature.count > 5
        JSONArray returnFeaturesArray = returnedAfterExonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert returnFeaturesArray.size() == 1
        JSONObject mRNAObject = returnFeaturesArray.get(0)
        assert mRNAObject.getString(FeatureStringEnum.NAME.value) == "GB40772-RA-00001"
        JSONArray childrenArray = mRNAObject.getJSONArray(FeatureStringEnum.CHILDREN.value)
        def allFeatures = Feature.all
        assert Gene.count == 1
        assert MRNA.count == 1
        // we are losing an exon somewhere!
        assert Exon.count == 3
        assert CDS.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert childrenArray.size() == 6


    }

    void "flip strand on an existing transcript"() {

        given: "a input JSON string"
        String jsonString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":219994,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40772-RA\",\"children\":[{\"location\":{\"fmin\":222109,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":220044,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":222081,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":222109,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String commandString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_NAME@\" } ], \"operation\": \"flip_strand\" }"

        when: "we parse the string"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        then: "we get a valid json object and no features"
        assert Feature.count == 0

        when: "we add the first transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "we should get a transcript back"
        assert returnObject.getString('operation') == "ADD"
        assert returnObject.getBoolean('sequenceAlterationEvent') == false
        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == featuresArray.size()
        JSONObject mrna = featuresArray.getJSONObject(0)
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2
        assert CDS.count == 1
        assert "GB40772-RA-00001" == mrna.getString(FeatureStringEnum.NAME.value)
        String transcriptUniqueName = mrna.getString(FeatureStringEnum.UNIQUENAME.value)
        JSONArray children = mrna.getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 3 == children.size()
        for (int i = 0; i < 3; i++) {
            JSONObject codingObject = children.get(i)
            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
            assert locationObject != null
        }


        when: "we flip the strand"
        commandString = commandString.replaceAll("@TRANSCRIPT_NAME@", transcriptUniqueName)
        JSONObject commandObject = JSON.parse(commandString) as JSONObject
        JSONObject returnedAfterExonObject = requestHandlingService.flipStrand(commandObject)

        then: "we should see that we flipped the strand"
        assert returnedAfterExonObject != null
        log.debug Feature.count
        assert Feature.count > 5
        JSONArray returnFeaturesArray = returnedAfterExonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert returnFeaturesArray.size() == 1
        JSONObject mRNAObject = returnFeaturesArray.get(0)
        assert mRNAObject.getString(FeatureStringEnum.NAME.value) == "GB40772-RA-00001"
        JSONArray childrenArray = mRNAObject.getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert Gene.count == 1
        assert MRNA.count == 1
        // we are losing an exon somewhere!
        assert Exon.count == 2
        assert CDS.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert childrenArray.size() == 5

        when: "we flip it back the other way"
        returnedAfterExonObject = requestHandlingService.flipStrand(commandObject)
        returnFeaturesArray = returnedAfterExonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        mRNAObject = returnFeaturesArray.get(0)
        childrenArray = mRNAObject.getJSONArray(FeatureStringEnum.CHILDREN.value)

        then: "we should have no splice sites"
        log.debug Feature.count
        assert Feature.count == 5
        assert returnFeaturesArray.size() == 1
        assert mRNAObject.getString(FeatureStringEnum.NAME.value) == "GB40772-RA-00001"
        assert Gene.count == 1
        assert MRNA.count == 1
        // we are losing an exon somewhere!
        assert childrenArray.size() == 3
        assert Exon.count == 2
        assert CDS.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
    }

    void "delete an entire transcript"() {

        given: "a input JSON string"
        String jsonString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":219994,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40772-RA\",\"children\":[{\"location\":{\"fmin\":222109,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":220044,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":222081,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":222109,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String commandString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_NAME@\" } ], \"operation\": \"delete_feature\" }"

        when: "we parse the string"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        then: "we get a valid json object and no features"
        assert Feature.count == 0

        when: "we add the first transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "we should get a transcript back"
        assert returnObject.getString('operation') == "ADD"
        assert returnObject.getBoolean('sequenceAlterationEvent') == false
        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == featuresArray.size()
        JSONObject mrna = featuresArray.getJSONObject(0)
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2
        assert CDS.count == 1
        assert "GB40772-RA-00001" == mrna.getString(FeatureStringEnum.NAME.value)
        String transcriptUniqueName = mrna.getString(FeatureStringEnum.UNIQUENAME.value)
        JSONArray children = mrna.getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 3 == children.size()
        for (int i = 0; i < 3; i++) {
            JSONObject codingObject = children.get(i)
            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
            assert locationObject != null
        }


        when: "we delete the transcript"
        commandString = commandString.replaceAll("@TRANSCRIPT_NAME@", transcriptUniqueName)
        JSONObject commandObject = JSON.parse(commandString) as JSONObject
        JSONObject returnedAfterExonObject = requestHandlingService.deleteFeature(commandObject)

        then: "we should see that it is removed"
        def allFeatures = Feature.all
        assert returnedAfterExonObject != null
        assert Feature.count == 0
        JSONArray returnFeaturesArray = returnedAfterExonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert returnFeaturesArray.size() == 0
    }

    void "we should be able to delete an isoform"() {

        given: "a input JSON string"
        String jsonString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":219994,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40772-RA\",\"children\":[{\"location\":{\"fmin\":222109,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":220044,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":222081,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":222109,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String commandString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_NAME@\" } ], \"operation\": \"delete_feature\" }"

        when: "we parse the string"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        then: "we get a valid json object and no features"
        assert Feature.count == 0

        when: "we add the first transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "we should get a transcript back"
        assert returnObject.getString('operation') == "ADD"
        assert returnObject.getBoolean('sequenceAlterationEvent') == false
        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == featuresArray.size()
        JSONObject mrna = featuresArray.getJSONObject(0)
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2
        assert CDS.count == 1
        assert "GB40772-RA-00001" == mrna.getString(FeatureStringEnum.NAME.value)
        String transcriptUniqueName = mrna.getString(FeatureStringEnum.UNIQUENAME.value)
        JSONArray children = mrna.getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 3 == children.size()
        for (int i = 0; i < 3; i++) {
            JSONObject codingObject = children.get(i)
            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
            assert locationObject != null
        }

        when: "we add the second transcript"
        returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "we should get a second isoform back"
        assert returnObject.getString('operation') == "ADD"
        assert returnObject.getBoolean('sequenceAlterationEvent') == false
        assert Gene.count == 1
        assert MRNA.count == 2
        assert Exon.count == 4
        assert CDS.count == 2
        assert 1 == MRNA.countByName("GB40772-RA-00002")
        assert 1 == MRNA.countByName("GB40772-RA-00001")


        when: "we delete the transcript"
        commandString = commandString.replaceAll("@TRANSCRIPT_NAME@", transcriptUniqueName)
        JSONObject commandObject = JSON.parse(commandString) as JSONObject
        JSONObject returnedAfterExonObject = requestHandlingService.deleteFeature(commandObject)

        then: "we should see that it is removed with one left"
//        def allFeatures = Feature.all
        assert 1 == featuresArray.size()
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2
        assert CDS.count == 1
        assert "GB40772-RA-00002" == MRNA.first().name
        assert returnedAfterExonObject != null

        when: "we delete the second transcript"
        String transcriptUniqueName2 = MRNA.first().uniqueName
        commandString = commandString.replaceAll(transcriptUniqueName, transcriptUniqueName2)
        commandObject = JSON.parse(commandString) as JSONObject
        returnedAfterExonObject = requestHandlingService.deleteFeature(commandObject)


        then: "we should see that all are removed"
        assert returnedAfterExonObject != null
        assert Feature.count == 0
    }


    void "splitting an exon should work and handle CDS calculations properly"() {

        given: "a input JSON string"
        String jsonAddTranscriptString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":202764,\"fmax\":205331,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40806-RA\",\"children\":[{\"location\":{\"fmin\":202764,\"fmax\":203242,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":204576,\"fmax\":205331,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":202764,\"fmax\":205331,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String commandString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON_NAME@\", \"location\": { \"fmax\": 204749, \"fmin\": 204750 } } ], \"operation\": \"split_exon\" }"
        JSONObject jsonAddTranscriptObject = JSON.parse(jsonAddTranscriptString) as JSONObject
//        String validResponseString = "{\"features\":[{\"location\":{\"fmin\":202764,\"strand\":1,\"fmax\":205331},\"parent_type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}},\"name\":\"GB40806-RA\",\"children\":[{\"location\":{\"fmin\":204750,\"strand\":1,\"fmax\":205331},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"uniquename\":\"A183623EE72EA859B745AF2349E8740E\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1426004351236,\"parent_id\":\"926D93FC00DE8F9350AF62A41BA0B3CD\"},{\"location\":{\"fmin\":204576,\"strand\":1,\"fmax\":204749},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"D5B021BFDD01D3D14E78157E4E850267\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1426004351236,\"parent_id\":\"926D93FC00DE8F9350AF62A41BA0B3CD\"},{\"location\":{\"fmin\":204750,\"strand\":1,\"fmax\":204750},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"926D93FC00DE8F9350AF62A41BA0B3CD-non_canonical_three_prive_splice_site-204750\",\"type\":{\"name\":\"non_canonical_three_prime_splice_site\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1426004351237,\"parent_id\":\"926D93FC00DE8F9350AF62A41BA0B3CD\"},{\"location\":{\"fmin\":204749,\"strand\":1,\"fmax\":204749},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"926D93FC00DE8F9350AF62A41BA0B3CD-non_canonical_five_prive_splice_site-204749\",\"type\":{\"name\":\"non_canonical_five_prime_splice_site\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1426004351237,\"parent_id\":\"926D93FC00DE8F9350AF62A41BA0B3CD\"},{\"location\":{\"fmin\":202764,\"strand\":1,\"fmax\":204756},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"42043B4EE8A57AF22BECE7682665DB9A\",\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1426004351237,\"parent_id\":\"926D93FC00DE8F9350AF62A41BA0B3CD\"},{\"location\":{\"fmin\":202764,\"strand\":1,\"fmax\":203242},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"CB50327870DCD2B3DBE8CF26F9A9400E\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1426004313199,\"parent_id\":\"926D93FC00DE8F9350AF62A41BA0B3CD\"}],\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"926D93FC00DE8F9350AF62A41BA0B3CD\",\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1426004351237,\"parent_id\":\"1A3B547A870C12D9BEE39F9CDAEB8EE7\"}]}"

        when: "we add the first transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonAddTranscriptObject)

        then: "we should get a transcript back"
        assert returnObject.getString('operation') == "ADD"
        assert returnObject.getBoolean('sequenceAlterationEvent') == false
        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == featuresArray.size()
        JSONObject mrna = featuresArray.getJSONObject(0)
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2
        assert CDS.count == 1
        assert "GB40806-RA-00001" == mrna.getString(FeatureStringEnum.NAME.value)
//        String transcriptUniqueName = mrna.getString(FeatureStringEnum.UNIQUENAME.value)
        JSONArray children = mrna.getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 3 == children.size()
        for (int i = 0; i < 3; i++) {
            JSONObject codingObject = children.getJSONObject(i)
            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
            assert locationObject != null
        }

        when: "we get the sorted exons"
        List<Exon> sortedExons = exonService.getSortedExons(MRNA.first())

        then: "there should be 2 and in the right order"
        assert sortedExons.size() == 2
        assert sortedExons.get(0).featureLocation.fmax < sortedExons.get(1).featureLocation.fmin
        String exonToSplitUniqueName = sortedExons.get(1).uniqueName
        assert CDS.first().featureLocation.fmin == MRNA.first().featureLocation.fmin
        assert CDS.first().featureLocation.fmax == MRNA.first().featureLocation.fmax



        when: "we split an exon"
        commandString = commandString.replaceAll("@EXON_NAME@", exonToSplitUniqueName)
        JSONObject commandObject = JSON.parse(commandString) as JSONObject
        JSONObject returnedAfterExonObject = requestHandlingService.splitExon(commandObject)
        JSONArray returnFeaturesArray = returnedAfterExonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject returnMRNA = returnFeaturesArray.getJSONObject(0)
        JSONArray returnedChildren = returnMRNA.getJSONArray(FeatureStringEnum.CHILDREN.value)
        List<Exon> finalSortedExons = exonService.getSortedExons(MRNA.first())
        Exon lastExon = finalSortedExons.get(2)

        then: "we should see that it is removed"
        def allFeatures = Feature.all
        assert returnFeaturesArray.size() == 1
        assert returnedAfterExonObject != null
        assert 3 == returnedAfterExonObject.size() // operation update, features, sequence_alt_event, etc.
        assert Gene.count == 1
        assert MRNA.count == 1

        // the 6 children
        assert Exon.count == 3
        assert CDS.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert "GB40806-RA-00001" == returnMRNA.getString(FeatureStringEnum.NAME.value)
        assert 6 == returnedChildren.size()
        for (int i = 0; i < 6; i++) {
            JSONObject codingObject = returnedChildren.getJSONObject(i)
            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
            assert locationObject != null
        }
        assert finalSortedExons.size() == 3
        assert CDS.first().featureLocation.fmin == MRNA.first().featureLocation.fmin
        assert CDS.first().featureLocation.fmax < MRNA.first().featureLocation.fmax
        assert CDS.first().featureLocation.fmax < MRNA.first().featureLocation.fmax

        // the end of the CDS should be on the last exon
        assert CDS.first().featureLocation.fmax < lastExon.featureLocation.fmax
        assert CDS.first().featureLocation.fmax > lastExon.featureLocation.fmin


    }


    void "splitting a transcript should work properly"() {

        given: "a input JSON string"
        String jsonAddTranscriptString = "{\"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":202764,\"fmax\":205331,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40806-RA\",\"children\":[{\"location\":{\"fmin\":202764,\"fmax\":203242,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":204576,\"fmax\":205331,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":202764,\"fmax\":205331,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String commandString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON1_UNIQUENAME@\" }, { \"uniquename\": \"@EXON2_UNIQUENAME@\" } ], \"operation\": \"split_transcript\" }"
        JSONObject jsonAddTranscriptObject = JSON.parse(jsonAddTranscriptString) as JSONObject

        when: "we add the first transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonAddTranscriptObject)

        then: "we should get a transcript back"
        assert returnObject.getString('operation') == "ADD"
        assert returnObject.getBoolean('sequenceAlterationEvent') == false
        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == featuresArray.size()
        JSONObject mrna = featuresArray.getJSONObject(0)
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2
        assert CDS.count == 1
        assert "GB40806-RA-00001" == mrna.getString(FeatureStringEnum.NAME.value)
//        String transcriptUniqueName = mrna.getString(FeatureStringEnum.UNIQUENAME.value)
        JSONArray children = mrna.getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 3 == children.size()
        for (int i = 0; i < 3; i++) {
            JSONObject codingObject = children.getJSONObject(i)
            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
            assert locationObject != null
        }

        when: "we get the sorted exons"
        List<Exon> sortedExons = exonService.getSortedExons(MRNA.first())

        then: "there should be 2 and in the right order"
        assert sortedExons.size() == 2
        assert sortedExons.get(0).featureLocation.fmax < sortedExons.get(1).featureLocation.fmin
//        String exonToSplitUniqueName = sortedExons.get(1).uniqueName
        assert CDS.first().featureLocation.fmin == MRNA.first().featureLocation.fmin
        assert CDS.first().featureLocation.fmax == MRNA.first().featureLocation.fmax



        when: "we split a transcript "
        String uniqueName1 = sortedExons.get(0).uniqueName
        String uniqueName2 = sortedExons.get(1).uniqueName
        commandString = commandString.replaceAll("@EXON1_UNIQUENAME@", uniqueName1)
        commandString = commandString.replaceAll("@EXON2_UNIQUENAME@", uniqueName2)

        JSONObject commandObject = JSON.parse(commandString) as JSONObject
        JSONObject returnedAfterExonObject = requestHandlingService.splitTranscript(commandObject)
        JSONArray returnFeaturesArray = returnedAfterExonObject.getJSONArray(FeatureStringEnum.FEATURES.value)


        then: "we should see 2 genes, each belonging to a transcript and having a single gene and exon"
        def allFeatures = Feature.all
        assert MRNA.count == 2
        assert Exon.count == 2
        assert Gene.count == 2
        assert CDS.count == 2
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert NonCanonicalFivePrimeSpliceSite.count == 0

        List<Gene> allGenes = Gene.all
        assert allGenes.get(0).id != allGenes.get(1).id
        assert allGenes.get(0).name != allGenes.get(1).name

    }

    void "merging transcript on the same strand leaves leftover stuff and doesn't update correctly"() {

        given: "the GB40788-RA and GB40787-RA"
        String gb40787String = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40787-RA\",\"children\":[{\"location\":{\"fmin\":77860,\"fmax\":77944,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":78049,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String gb40788String = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40788-RA\",\"children\":[{\"location\":{\"fmin\":65107,\"fmax\":65286,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":71477,\"fmax\":71651,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":75270,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonAddTranscriptObject1 = JSON.parse(gb40787String) as JSONObject
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40788String) as JSONObject
        String mergeTranscriptString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" }, { \"uniquename\": \"@TRANSCRIPT2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\" }"


        when: "we add both transcripts"
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)
        requestHandlingService.addTranscript(jsonAddTranscriptObject2)


        then: "we should see 2 genes, 2 transcripts, 5 exons, 2 CDS, no noncanonical splice sites"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0


        when: "we merge the transcripts"
        String uniqueName1 = MRNA.findByName("GB40787-RA-00001").uniqueName
        String uniqueName2 = MRNA.findByName("GB40788-RA-00001").uniqueName
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", uniqueName1)
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT2_UNIQUENAME@", uniqueName2)
        JSONObject commandObject = JSON.parse(mergeTranscriptString) as JSONObject
        JSONObject returnedAfterExonObject = requestHandlingService.mergeTranscripts(commandObject)


        then: "we should see 1 gene, 1 transcripts, 5 exons, 1 CDS, 1 3' noncanonical splice site and 1 5' noncanonical splice site"
        def allFeatures = Feature.all
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1

    }

    /**
     * https://github.com/GMOD/Apollo/issues/413
     */
    void "merging transcript with an upstream isoform effects the non-merged isoform"() {

        given: "the GB40788-RA and GB40787-RA"
        String gb40787String = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40787-RA\",\"children\":[{\"location\":{\"fmin\":77860,\"fmax\":77944,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":78049,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String gb40788String = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40788-RA\",\"children\":[{\"location\":{\"fmin\":65107,\"fmax\":65286,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":71477,\"fmax\":71651,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":75270,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonAddTranscriptObject1 = JSON.parse(gb40787String) as JSONObject
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40788String) as JSONObject
        String mergeTranscriptString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" }, { \"uniquename\": \"@TRANSCRIPT2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\" }"


        when: "we add three transcripts"
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)
        requestHandlingService.addTranscript(jsonAddTranscriptObject2)


        then: "we should see 2 genes, 3 transcripts, 7 exons, 3 CDS, no noncanonical splice sites"
        assert Gene.count == 2
        assert MRNA.count == 3
        assert CDS.count == 3
        assert Exon.count == 7
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0


        when: "we merge the transcripts"
        String uniqueName1 = MRNA.findByName("GB40787-RA-00001").uniqueName
        String uniqueName2 = MRNA.findByName("GB40788-RA-00001").uniqueName
        String uniqueName3 = MRNA.findByName("GB40787-RA-00002").uniqueName
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", uniqueName2)
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT2_UNIQUENAME@", uniqueName1)
        JSONObject commandObject = JSON.parse(mergeTranscriptString) as JSONObject
        JSONObject returnedAfterExonObject = requestHandlingService.mergeTranscripts(commandObject)


        then: "we should see 2 gene, 2 transcripts, 5 exons, 2 CDS, 1 3' noncanonical splice site and 1 5' noncanonical splice site"
        def allFeatures = Feature.all
        assert Gene.count == 2
        assert MRNA.count == 2
        assert Exon.count == 7
        assert CDS.count == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1

        when: "we get the transcripts and gene that should be left"
        // MRNA bigMRNA = MRNA.findByName("GB40788-RA-00001")
        // the big mRNA will be GB40787-RA-00001, since its the 5' most transcript
        MRNA bigMRNA = MRNA.findByName("GB40787-RA-00001")
        def remainingMRNA = MRNA.all - bigMRNA
        MRNA undisturbedMRNA = remainingMRNA.get(0)

        then: "this one should be long-gone"
        assert undisturbedMRNA != null
        assert bigMRNA != null
        assert undisturbedMRNA.featureLocation.fmax > undisturbedMRNA.featureLocation.fmin
        assert undisturbedMRNA.featureLocation.fmax - undisturbedMRNA.featureLocation.fmin > 0
        assert 0 == MRNA.countByName("GB40788-RA-00001")
        assert undisturbedMRNA.parentFeatureRelationships.size() == 2 + 1 + 0
        assert bigMRNA.parentFeatureRelationships.size() == 5 + 1 + 2

    }


    void "adding transcripts on overlapping opposite strands fails (should just be two genes and transcripts)"() {

        given: "the GB40781-RA and GB40800-RA"
        String gb40781 = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":157004,\"fmax\":160632,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40781-RA\",\"children\":[{\"location\":{\"fmin\":160342,\"fmax\":160632,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":159606,\"fmax\":159737,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":158839,\"fmax\":158850,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":157004,\"fmax\":157168,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":157004,\"fmax\":157898,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":158015,\"fmax\":158410,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":158530,\"fmax\":158850,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":159606,\"fmax\":159737,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":160342,\"fmax\":160632,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":157168,\"fmax\":158839,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String gb40800 = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":160307,\"fmax\":162089,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40800-RA\",\"children\":[{\"location\":{\"fmin\":160307,\"fmax\":160485,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":161876,\"fmax\":162089,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":160307,\"fmax\":160628,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":160961,\"fmax\":161035,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":161365,\"fmax\":161454,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":161556,\"fmax\":161660,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":161746,\"fmax\":162089,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":160485,\"fmax\":161876,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonAddTranscriptObject1 = JSON.parse(gb40781) as JSONObject
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40800) as JSONObject

        when: "we add GB40781-RA"
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)

        then: "we should see 1 genes, 1 transcript, 5 exons, 1 CDS, no noncanonical splice sites"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert CDS.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0


        when: "we add GB40800-RA"
        requestHandlingService.addTranscript(jsonAddTranscriptObject2)

        then: "we should see 2 genes, 2 transcripts, 10 exons, 2 CDS, 1 3' noncanonical splice site and 1 5' noncanonical splice site"
        def allFeatures = Feature.all
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 10
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1

    }

    void "should be able to delete multiple exons"() {

        given: "the GB40800-RA"
        String gb40800 = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":160307,\"fmax\":162089,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40800-RA\",\"children\":[{\"location\":{\"fmin\":160307,\"fmax\":160485,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":161876,\"fmax\":162089,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":160307,\"fmax\":160628,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":160961,\"fmax\":161035,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":161365,\"fmax\":161454,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":161556,\"fmax\":161660,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":161746,\"fmax\":162089,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":160485,\"fmax\":161876,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"


        String removeExonCommand = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON1_UNIQUENAME@\" } ], \"operation\": \"delete_feature\" }"
        String removeExonCommand2 = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON2_UNIQUENAME@\" },{ \"uniquename\": \"@EXON3_UNIQUENAME@\" } ], \"operation\": \"delete_feature\" }"
//        String removeExon1 = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON1_UNIQUENAME@\" } ], \"operation\": \"delete_feature\" }"
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40800) as JSONObject

        when: "we add GB40800-RA"
        JSONObject addedTranscriptObject = requestHandlingService.addTranscript(jsonAddTranscriptObject2)

        then: "we should see 1 genes, 1 transcript, 5 exons, 1 CDS, no noncanonical splice sites"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert CDS.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        String exon1UniqueName
        String exon2UniqueName
        String exon3UniqueName
        JSONArray childrenArray = addedTranscriptObject.getJSONArray(FeatureStringEnum.FEATURES.value).getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value)

        for (int i = 0; i < childrenArray.size(); i++) {
            JSONObject childObject = childrenArray.getJSONObject(i)
            if (childObject.type.name == "exon") {
                switch (childObject.location.fmin) {
                    case 160961:
                        exon1UniqueName = childObject.uniquename
                        break;
                    case 161746:
                        exon2UniqueName = childObject.uniquename
                        break
                    case 161365:
                        exon3UniqueName = childObject.uniquename
                        break

                }

            }
        }
        assert exon1UniqueName != null
        assert exon2UniqueName != null
        assert exon3UniqueName != null


        when: "we delete 1 exon and things are great"
        JSONObject removeExonObject1 = JSON.parse(removeExonCommand.replace("@EXON1_UNIQUENAME@", exon1UniqueName)) as JSONObject
        JSONObject deletedObjectCommand = requestHandlingService.deleteFeature(removeExonObject1)
        println "deleted object command ${deletedObjectCommand as JSON}"

        then: "we delete 2 exons and things are okay"
        def allFeatures = Feature.all
        assert Gene.count == 1
        assert MRNA.count == 1
        assert CDS.count == 1
        assert Exon.count == 4
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 1

        when: "we delete 2 exon and things are great"
        removeExonObject1 = JSON.parse(removeExonCommand2.replace("@EXON2_UNIQUENAME@", exon2UniqueName).replace("@EXON3_UNIQUENAME@", exon3UniqueName)) as JSONObject
        deletedObjectCommand = requestHandlingService.deleteFeature(removeExonObject1)
        println "deleted object command ${deletedObjectCommand as JSON}"
        allFeatures = Feature.all

        then: "Deleting objects"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert CDS.count == 1
        assert Exon.count == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
    }

    void "add insertion at exon 1 of gene GB40807-RA"() {
        given: "given a gene GB40807-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":210517},\"name\":\"GB40807-RA\",\"children\":[{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":208322},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":209434,\"strand\":1,\"fmax\":210517},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":208544},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208735,\"strand\":1,\"fmax\":210517},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208322,\"strand\":1,\"fmax\":209434},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertionString = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"GGG\",\"location\":{\"fmin\":208499,\"strand\":1,\"fmax\":208499},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptideString = "MRVFMMQNFHRLTLFIWLVSILTLSISDEIKTDGNTSLTLNINNVDSDSKTEHRISSSSGIQFMPESVNSKKIQNQSIATPLVAGEGGPISLIPPTQQTSTISHLKDVTDNLDLQDNLSQKEDDILYVKKKKNTSKIVSRKGADNGNISIKMTLSNDTKPIIEFSTIASNISNNAKIDINMNNSKSNVSDKNINKASNIIVNNTLYLTNVTQKLLSVTTSSVQEHKPKPTATVIESNNDKQAFIPHTKGSRLGMPKKIDYVLPVIVTLIALPVLGAIIFMVYKQGRDCWDKRHYRRMDFLIDGMYND"
        String expectedCdnaString = "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAGGGATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        String expectedCdsString = "ATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAGGGATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAA"
        String expectedGenomicString = "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAGGGATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATGTACGTAATATAAATACATAATATTATATATATATATATATATATATATATATAATTATCAATTAACAAATGTATAAATTATTTATAAATTTTAAATACACTATATATTTAAGAAATTAATTTTTTTTGTATTTTTATATTTTTTTTCTAAATAAAGTATATATAATAATAGTAACTAAATATTATTGCAGCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"

        when: "when we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2

        when: "we add an insertion of GGG to exon 1 at position 208500"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40807-RA-00001")

        String peptideString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdnaString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cdsString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)
        String genomicString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we should have the expected sequences"
        assert peptideString == expectedPeptideString
        assert cdnaString == expectedCdnaString
        assert cdsString == expectedCdsString
        assert genomicString == expectedGenomicString
    }

    void "add insertion at 5'UTR of gene GB40807-RA"() {
        given: "given a gene GB40807-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":210517},\"name\":\"GB40807-RA\",\"children\":[{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":208322},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":209434,\"strand\":1,\"fmax\":210517},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":208544},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208735,\"strand\":1,\"fmax\":210517},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208322,\"strand\":1,\"fmax\":209434},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertionString = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"AAA\",\"location\":{\"fmin\":208299,\"strand\":1,\"fmax\":208299},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptideString = "MRVFMMQNFHRLTLFIWLVSILTLSISDEIKTDGNTSLTLNINNVDSDSKTEHRISSSSIQFMPESVNSKKIQNQSIATPLVAGEGGPISLIPPTQQTSTISHLKDVTDNLDLQDNLSQKEDDILYVKKKKNTSKIVSRKGADNGNISIKMTLSNDTKPIIEFSTIASNISNNAKIDINMNNSKSNVSDKNINKASNIIVNNTLYLTNVTQKLLSVTTSSVQEHKPKPTATVIESNNDKQAFIPHTKGSRLGMPKKIDYVLPVIVTLIALPVLGAIIFMVYKQGRDCWDKRHYRRMDFLIDGMYND"
        String expectedCdnaString = "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        String expectedCdsString = "ATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAA"
        String expectedGenomicString = "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATGTACGTAATATAAATACATAATATTATATATATATATATATATATATATATATAATTATCAATTAACAAATGTATAAATTATTTATAAATTTTAAATACACTATATATTTAAGAAATTAATTTTTTTTGTATTTTTATATTTTTTTTCTAAATAAAGTATATATAATAATAGTAACTAAATATTATTGCAGCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"

        when: "when we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2

        when: "we add an insertion of AAA to the 5'UTR at position 208300"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40807-RA-00001")

        String peptideString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdnaString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cdsString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)
        String genomicString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we should have the expected sequences"
        assert peptideString == expectedPeptideString
        assert cdnaString == expectedCdnaString
        assert cdsString == expectedCdsString
        assert genomicString == expectedGenomicString
    }

    void "add insertion at exon 2 of gene GB40807-RA"() {
        given: "given a gene GB40807-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":210517},\"name\":\"GB40807-RA\",\"children\":[{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":208322},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":209434,\"strand\":1,\"fmax\":210517},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":208544},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208735,\"strand\":1,\"fmax\":210517},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208322,\"strand\":1,\"fmax\":209434},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertionString = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"AAA\",\"location\":{\"fmin\":208899,\"strand\":1,\"fmax\":208899},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptideString = "MRVFMMQNFHRLTLFIWLVSILTLSISDEIKTDGNTSLTLNINNVDSDSKTEHRISSSSIQFMPESVNSKKIQNQSIATPLVAGEGGPISLIPPTQQTSTISHLKDVTDNLDLQDNLSQKEDDILYVKKKKKNTSKIVSRKGADNGNISIKMTLSNDTKPIIEFSTIASNISNNAKIDINMNNSKSNVSDKNINKASNIIVNNTLYLTNVTQKLLSVTTSSVQEHKPKPTATVIESNNDKQAFIPHTKGSRLGMPKKIDYVLPVIVTLIALPVLGAIIFMVYKQGRDCWDKRHYRRMDFLIDGMYND"
        String expectedCdnaString = "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        String expectedCdsString = "ATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAA"
        String expectedGenomicString = "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATGTACGTAATATAAATACATAATATTATATATATATATATATATATATATATATAATTATCAATTAACAAATGTATAAATTATTTATAAATTTTAAATACACTATATATTTAAGAAATTAATTTTTTTTGTATTTTTATATTTTTTTTCTAAATAAAGTATATATAATAATAGTAACTAAATATTATTGCAGCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"

        when: "when we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2

        when: "we add an insertion AAA to exon 2 at position 208900"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40807-RA-00001")

        String peptideString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdnaString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cdsString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)
        String genomicString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we should have the expected sequences"
        assert peptideString == expectedPeptideString
        assert cdnaString == expectedCdnaString
        assert cdsString == expectedCdsString
        assert genomicString == expectedGenomicString
    }

    void "add insertion at 3'UTR of gene GB40807-RA"() {
        given: "given a gene GB40807-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":210517},\"name\":\"GB40807-RA\",\"children\":[{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":208322},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":209434,\"strand\":1,\"fmax\":210517},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208175,\"strand\":1,\"fmax\":208544},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208735,\"strand\":1,\"fmax\":210517},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":208322,\"strand\":1,\"fmax\":209434},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertionString = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"AAA\",\"location\":{\"fmin\":209449,\"strand\":1,\"fmax\":209449},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptideString = "MRVFMMQNFHRLTLFIWLVSILTLSISDEIKTDGNTSLTLNINNVDSDSKTEHRISSSSIQFMPESVNSKKIQNQSIATPLVAGEGGPISLIPPTQQTSTISHLKDVTDNLDLQDNLSQKEDDILYVKKKKNTSKIVSRKGADNGNISIKMTLSNDTKPIIEFSTIASNISNNAKIDINMNNSKSNVSDKNINKASNIIVNNTLYLTNVTQKLLSVTTSSVQEHKPKPTATVIESNNDKQAFIPHTKGSRLGMPKKIDYVLPVIVTLIALPVLGAIIFMVYKQGRDCWDKRHYRRMDFLIDGMYND"
        String expectedCdnaString = "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGAAAATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"
        String expectedCdsString = "ATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAA"
        String expectedGenomicString = "GCAATAGTTGCGTGCTTATGATGGAGCAAACAGTTTCTTAGTGGTTGAGACCACTTTTTTTTTAGTTTTTCTATATTTTTATAAAAGTTTTAACCAGATTTATCTGCAAAGAATCGTATCAGAAAATAAAATTTTATAATTAAAATAATGCGTGTTTTCATGATGCAAAATTTTCATCGATTAACTCTATTTATATGGCTTGTATCTATTCTAACCTTATCTATCAGTGATGAAATTAAAACAGATGGTAATACATCCTTAACATTAAATATAAACAATGTTGATAGTGATTCCAAAACCGAACATCGAATTTCATCTTCATCAATTCAATTTATGCCTGAATCCGTTAATTCTAAAAAAATACAGAATGTACGTAATATAAATACATAATATTATATATATATATATATATATATATATATAATTATCAATTAACAAATGTATAAATTATTTATAAATTTTAAATACACTATATATTTAAGAAATTAATTTTTTTTGTATTTTTATATTTTTTTTCTAAATAAAGTATATATAATAATAGTAACTAAATATTATTGCAGCAAAGTATTGCCACTCCTTTAGTTGCTGGAGAAGGTGGTCCAATATCACTTATACCTCCTACTCAGCAAACATCTACTATTTCCCATTTAAAAGATGTTACTGATAATTTAGATTTACAAGATAATTTATCACAAAAAGAAGATGATATTTTATACGTAAAGAAAAAAAAGAATACTTCTAAAATCGTGTCGAGAAAAGGAGCAGATAATGGAAATATTTCTATTAAAATGACATTATCAAATGACACAAAACCTATTATTGAATTTTCAACAATAGCAAGTAATATTTCTAATAATGCAAAAATTGATATAAATATGAATAATTCAAAATCAAATGTTAGTGATAAAAATATAAATAAAGCTTCAAATATAATTGTAAATAATACTTTATATTTAACAAATGTAACTCAAAAATTATTAAGTGTAACAACATCATCAGTCCAAGAACATAAACCTAAACCAACTGCAACAGTAATAGAATCTAATAATGATAAACAAGCATTTATACCTCATACTAAAGGTTCACGCTTAGGAATGCCAAAGAAAATTGATTATGTCTTACCAGTTATTGTTACTCTTATAGCTCTACCAGTTTTGGGTGCTATTATTTTCATGGTTTATAAACAAGGTAGAGATTGTTGGGATAAAAGACACTACCGACGAATGGATTTTCTTATTGATGGCATGTACAATGATTAATACTTATAAATATGAAAATATCACTTAATTCGGCTCATAATTTTCATTCATATATGCAATACATATACATAACAGTTGAATATACTATTTTGCCATTATAGCTAAAAAAAACATTATATTTCAATTATATATAATTTTTTATTTTACTGCAATTTTCTGCATATACTTTTATCATGCTACTGCCTTAATATGAGATTTGTTATTATATATTAATTAGTATCATGTTTATAATTTTAGACAAATGGTGCATAGGAAAGACAATATGGAAATAACAACAAATTTACAATTATAGCAATAACAATTTATTATGAATTCTAAGAGTGAAGTACTTTTAAAATAAAGATTTTATCTTAATTTATAAAATAATTAATGACACTTTTATAATTGTATATTAAAGCAATTTTTAAAATTAGAGATTTTTAATTACATTACTTTTTCATAAAAATTTTTAATAAAAAAATAAATGTGCCAAGAATTTTTGATTATGAAACCAGTGATATGTTAATGTTTTTTCTTCCAGTATATATAAAAGTAAGTTTTTTTGATATGAAAAAACATATTTATATTTTGATATTGTAATTTAAATTTGTTTTTAATTATATTTCCATATGATTTCCTCTTCATTAAAATTTGATTTTATTTTTTAAATTTTATAAAATGCTCTTTATATTACAAATTGTAAAATAGTAGTATCTAGTTCGCCAAAGAAGTCATTCATATAATTTGATGTTTGCATTTACTTATTATAATTATTATGTGTTATTATCTTTTTACTTATGTTTTCGAAAAAATTTGTTTATATAAATTGATAATTATAATTACAAATGAAAGAATAAAATGGACATTAAATGTCCATTTGTAAAATTATCATATTATAAAATATATAAGCAATGATTTATGCAATTACTTTATCTAATAAGGTTGCTGCAATTGTTATTAATGCTAGTAGAATTTTACGAACTTTTTTATCTTTTTTAACGTTCGTAAAATTTGTATATTATTCAGATATAATAAAGCAATAACTATTTTTATATATGTATGTAAAAAAATTATTCATATTCTTATAAAATATAAGTACTTGTAATT"

        when: "when we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2

        when: "we add an insertion of AAA to 3'UTR at position 209450"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40807-RA-00001")

        String peptideString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdnaString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cdsString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)
        String genomicString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we should have the expected sequences"
        assert peptideString == expectedPeptideString
        assert cdnaString == expectedCdnaString
        assert cdsString == expectedCdsString
        assert genomicString == expectedGenomicString
    }

    void "add insertion at intron of gene GB40721-RA"() {
        given: "given a gene GB40721-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":1254490,\"strand\":-1,\"fmax\":1254801},\"name\":\"GB40721-RA\",\"children\":[{\"location\":{\"fmin\":1254490,\"strand\":-1,\"fmax\":1254586},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1254747,\"strand\":-1,\"fmax\":1254801},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1254490,\"strand\":-1,\"fmax\":1254801},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertionString = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"AAAAAAAAA\",\"location\":{\"fmin\":1254599,\"strand\":1,\"fmax\":1254599},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptideString = "MRAAHLPFSGKVEGAHAMFLASDTSLTPCIQNIPSIRFLPSTRLLIDTF"
        String expectedCdnaString = "ATGCGTGCGGCCCATCTGCCGTTTAGCGGTAAGGTGGAGGGCGCTCATGCTATGTTTTTGGCATCTGACACATCTCTGACACCTTGCATTCAGAACATTCCATCCATTAGATTTCTGCCCTCGACCAGGTTACTTATAGATACGTTCTAA"
        String expectedCdsString = "ATGCGTGCGGCCCATCTGCCGTTTAGCGGTAAGGTGGAGGGCGCTCATGCTATGTTTTTGGCATCTGACACATCTCTGACACCTTGCATTCAGAACATTCCATCCATTAGATTTCTGCCCTCGACCAGGTTACTTATAGATACGTTCTAA"
        String expectedGenomicString = "ATGCGTGCGGCCCATCTGCCGTTTAGCGGTAAGGTGGAGGGCGCTCATGCTATGGTATGTCTTAATGTTAGATTATAAGTAGCCTTTTTGTATAAAGTATATTGAGGTATAAAACCCTTAAATTGAATTACTGTACAGAATGTGTAGAAATAGTGAATAATAATATTATGGAATATATTTTATTTCTCATTTAAATGAAACTTTTTTTTTTTAAGTTGTTTCAGTTTTTGGCATCTGACACATCTCTGACACCTTGCATTCAGAACATTCCATCCATTAGATTTCTGCCCTCGACCAGGTTACTTATAGATACGTTCTAA"

        when: "we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2

        when: "we add an insertion of AAAAAAAAA to an intron at position 1254600"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40721-RA-00001")

        String peptideString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdnaString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cdsString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)
        String genomicString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we should have the expected sequences"
        assert peptideString == expectedPeptideString
        assert cdnaString == expectedCdnaString
        assert cdsString == expectedCdsString
        assert genomicString == expectedGenomicString
    }

    void "add insertion at exon 1 of gene GB40721-RA"() {
        given: "given a gene GB40721-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":1254490,\"strand\":-1,\"fmax\":1254801},\"name\":\"GB40721-RA\",\"children\":[{\"location\":{\"fmin\":1254490,\"strand\":-1,\"fmax\":1254586},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1254747,\"strand\":-1,\"fmax\":1254801},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1254490,\"strand\":-1,\"fmax\":1254801},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertionString = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"TTT\",\"location\":{\"fmin\":1254774,\"strand\":1,\"fmax\":1254774},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptideString = "MRAAHLPFSKGKVEGAHAMFLASDTSLTPCIQNIPSIRFLPSTRLLIDTF"
        String expectedCdnaString = "ATGCGTGCGGCCCATCTGCCGTTTAGCAAAGGTAAGGTGGAGGGCGCTCATGCTATGTTTTTGGCATCTGACACATCTCTGACACCTTGCATTCAGAACATTCCATCCATTAGATTTCTGCCCTCGACCAGGTTACTTATAGATACGTTCTAA"
        String expectedCdsString = "ATGCGTGCGGCCCATCTGCCGTTTAGCAAAGGTAAGGTGGAGGGCGCTCATGCTATGTTTTTGGCATCTGACACATCTCTGACACCTTGCATTCAGAACATTCCATCCATTAGATTTCTGCCCTCGACCAGGTTACTTATAGATACGTTCTAA"
        String expectedGenomicString = "ATGCGTGCGGCCCATCTGCCGTTTAGCAAAGGTAAGGTGGAGGGCGCTCATGCTATGGTATGTCTTAATGTTAGATTATAAGTAGCCTTTTTGTATAAAGTATATTGAGGTATAAAACCCTTAAATTGAATTACTGTACAGAATGTGTAGAAATAGTGAATAATAATATTATGGAATATATTTTATTTCTCATTTAAATGAAACTTAAGTTGTTTCAGTTTTTGGCATCTGACACATCTCTGACACCTTGCATTCAGAACATTCCATCCATTAGATTTCTGCCCTCGACCAGGTTACTTATAGATACGTTCTAA"

        when: "we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2

        when: "we add an insertion of TTT to exon 1 at position 1254775"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40721-RA-00001")

        String peptideString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdnaString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cdsString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)
        String genomicString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we should have the expected sequences"
        assert peptideString == expectedPeptideString
        assert cdnaString == expectedCdnaString
        assert cdsString == expectedCdsString
        assert genomicString == expectedGenomicString
    }

    void "add insertion at exon 2 of gene GB40721-RA"() {
        given: "given a gene GB40721-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":1254490,\"strand\":-1,\"fmax\":1254801},\"name\":\"GB40721-RA\",\"children\":[{\"location\":{\"fmin\":1254490,\"strand\":-1,\"fmax\":1254586},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1254747,\"strand\":-1,\"fmax\":1254801},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1254490,\"strand\":-1,\"fmax\":1254801},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertionString = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"TTT\",\"location\":{\"fmin\":1254549,\"strand\":1,\"fmax\":1254549},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptideString = "MRAAHLPFSGKVEGAHAMFLASDTSLTPCIQKNIPSIRFLPSTRLLIDTF"
        String expectedCdnaString = "ATGCGTGCGGCCCATCTGCCGTTTAGCGGTAAGGTGGAGGGCGCTCATGCTATGTTTTTGGCATCTGACACATCTCTGACACCTTGCATTCAAAAGAACATTCCATCCATTAGATTTCTGCCCTCGACCAGGTTACTTATAGATACGTTCTAA"
        String expectedCdsString = "ATGCGTGCGGCCCATCTGCCGTTTAGCGGTAAGGTGGAGGGCGCTCATGCTATGTTTTTGGCATCTGACACATCTCTGACACCTTGCATTCAAAAGAACATTCCATCCATTAGATTTCTGCCCTCGACCAGGTTACTTATAGATACGTTCTAA"
        String expectedGenomicString = "ATGCGTGCGGCCCATCTGCCGTTTAGCGGTAAGGTGGAGGGCGCTCATGCTATGGTATGTCTTAATGTTAGATTATAAGTAGCCTTTTTGTATAAAGTATATTGAGGTATAAAACCCTTAAATTGAATTACTGTACAGAATGTGTAGAAATAGTGAATAATAATATTATGGAATATATTTTATTTCTCATTTAAATGAAACTTAAGTTGTTTCAGTTTTTGGCATCTGACACATCTCTGACACCTTGCATTCAAAAGAACATTCCATCCATTAGATTTCTGCCCTCGACCAGGTTACTTATAGATACGTTCTAA"

        when: "we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2

        when: "we add an insertion of TTT to exon 2 at position 1254550"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40721-RA-00001")

        String peptideString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdnaString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cdsString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)
        String genomicString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we should have the expected sequences"
        assert peptideString == expectedPeptideString
        assert cdnaString == expectedCdnaString
        assert cdsString == expectedCdsString
        assert genomicString == expectedGenomicString
    }

    void "add insertion at 5'UTR of gene GB40749-RA"() {
        given: "given a gene GB40749-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":694293,\"strand\":-1,\"fmax\":696055},\"name\":\"GB40749-RA\",\"children\":[{\"location\":{\"fmin\":695943,\"strand\":-1,\"fmax\":696055},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":694293,\"strand\":-1,\"fmax\":694440},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":694293,\"strand\":-1,\"fmax\":694606},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":694918,\"strand\":-1,\"fmax\":695591},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":695882,\"strand\":-1,\"fmax\":696055},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":694440,\"strand\":-1,\"fmax\":695943},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertionString = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"AAAAAA\",\"location\":{\"fmin\":695949,\"strand\":1,\"fmax\":695949},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptideString = "MPRCLIKSMTRYRKTDNSSEVEAELPWTPPSSVDAKRKHQIKDNSTKCNNIWTSSRLPIVTRYTFNKENNIFWNKELNIADVELGSRNFSEIENTIPSTTPNVSVNTNQAMVDTSNEQKVEKVQIPLPSNAKKVEYPVNVSNNEIKVAVNLNRMFDGAENQTTSQTLYIATNKKQIDSQNQYLGGNMKTTGVENPQNWKRNKTMHYCPYCRKSFDRPWVLKGHLRLHTGERPFECPVCHKSFADRSNLRAHQRTRNHHQWQWRCGECFKAFSQRRYLERHCPEACRKYRISQRREQNCS"
        String expectedCdnaString = "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCTTTTTTGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        String expectedCdsString = "ATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAG"
        String expectedGenomicString = "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCTTTTTTGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGGTAAGACAGAATTCTCTTGTGCACACAGTATAGCTACAGTATACTCAGGGGACGACGAGTGAAATTTTGGCGTGGTTATGGAAAAAAAAAAAGTACAACTCGTAAAGTTGTTGGAGTAAATGAGTCCCGTTTTTTCATGGCGAATCGTACGTCTCCTTTCCACTCGACGACACAGTTTTCAATTTCATATAATAAAAGCGAATGTGAAAATACGATGCGTATGATTCGTTCGAAAAAGAAAGGCAAAAAAAAAAAAAAAAAAAAAATGAAATGATTTTCTCTCCTAATTAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGGTATATTTCTTCTTTTAGTTTTATATATTTTTTTATATATTATATATATACACGAGTTACGAATAATAACAAAATTTTTTTCGAACCTTGAACGTATGATCAAAATTTCTCATTAAAACATTTGGAACGAAAAATGATAATTAAATATCGTAATCGGATGATTGCAACATATTATATAGTAATACATTATACATACCTATTTTATTTATTTTCTTTAGCAAATTATAAAGTTAATTTTATTGAGTTAATTTTACGATGTTTGAATTAATTAGTGGATACGAGATTATATGGTGTAACGTACATATTTTGTAGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"

        when: "we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3

        when: "we add an insertion of AAAAAA to the 5'UTR at position 695950"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40749-RA-00001")

        String peptideString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdnaString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cdsString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)
        String genomicString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we should have the expected sequences"
        assert peptideString == expectedPeptideString
        assert cdnaString == expectedCdnaString
        assert cdsString == expectedCdsString
        assert genomicString == expectedGenomicString
    }

    void "add insertion at 3'UTR of gene GB40749-RA"() {
        given: "given a gene GB40749-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":694293,\"strand\":-1,\"fmax\":696055},\"name\":\"GB40749-RA\",\"children\":[{\"location\":{\"fmin\":695943,\"strand\":-1,\"fmax\":696055},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":694293,\"strand\":-1,\"fmax\":694440},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":694293,\"strand\":-1,\"fmax\":694606},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":694918,\"strand\":-1,\"fmax\":695591},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":695882,\"strand\":-1,\"fmax\":696055},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":694440,\"strand\":-1,\"fmax\":695943},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertionString = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"AAAA\",\"location\":{\"fmin\":694399,\"strand\":1,\"fmax\":694399},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptideString = "MPRCLIKSMTRYRKTDNSSEVEAELPWTPPSSVDAKRKHQIKDNSTKCNNIWTSSRLPIVTRYTFNKENNIFWNKELNIADVELGSRNFSEIENTIPSTTPNVSVNTNQAMVDTSNEQKVEKVQIPLPSNAKKVEYPVNVSNNEIKVAVNLNRMFDGAENQTTSQTLYIATNKKQIDSQNQYLGGNMKTTGVENPQNWKRNKTMHYCPYCRKSFDRPWVLKGHLRLHTGERPFECPVCHKSFADRSNLRAHQRTRNHHQWQWRCGECFKAFSQRRYLERHCPEACRKYRISQRREQNCS"
        String expectedCdnaString = "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTTTTTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"
        String expectedCdsString = "ATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAG"
        String expectedGenomicString = "CAAATGCCTGTCGAACGTGTGACAGTGGTTTGCTCCATCGCTGTTGCAACGGCCAACACTTTATCGTATTTCGTTCTTTTTTTAGCTGTGGCCGTTTCATCGTCGCGAAAATATGCCTCGTTGCTTGATCAAATCGATGACAAGGTATAGGAAGACCGATAATTCTTCCGAAGGTAAGACAGAATTCTCTTGTGCACACAGTATAGCTACAGTATACTCAGGGGACGACGAGTGAAATTTTGGCGTGGTTATGGAAAAAAAAAAAGTACAACTCGTAAAGTTGTTGGAGTAAATGAGTCCCGTTTTTTCATGGCGAATCGTACGTCTCCTTTCCACTCGACGACACAGTTTTCAATTTCATATAATAAAAGCGAATGTGAAAATACGATGCGTATGATTCGTTCGAAAAAGAAAGGCAAAAAAAAAAAAAAAAAAAAAATGAAATGATTTTCTCTCCTAATTAGTAGAGGCAGAATTACCATGGACTCCGCCATCGTCGGTCGACGCGAAGAGAAAACATCAGATTAAAGACAATTCCACGAAATGCAATAATATATGGACCTCCTCGAGATTGCCAATTGTAACACGTTACACGTTCAATAAAGAGAACAACATATTTTGGAACAAGGAGTTGAATATAGCAGACGTGGAATTGGGCTCGAGAAATTTTTCCGAGATTGAGAATACGATACCGTCGACCACTCCGAATGTCTCTGTGAATACCAATCAGGCAATGGTGGACACGAGCAATGAGCAAAAGGTGGAAAAAGTGCAAATACCATTGCCCTCGAACGCGAAAAAAGTAGAGTATCCGGTAAACGTGAGTAACAACGAGATCAAGGTGGCTGTGAATTTGAATAGGATGTTCGATGGGGCTGAGAATCAGACCACCTCGCAGACTTTGTATATTGCCACGAATAAGAAACAGATTGATTCCCAGAATCAATATTTAGGAGGGAATATGAAAACTACGGGGGTGGAGAATCCCCAGAATTGGAAGAGAAATAAAACTATGCATTATTGCCCTTATTGTCGCAAGAGTTTCGATCGTCCATGGGTTTTGAAGGGTCATCTGCGTCTTCACACGGGTGAACGACCTTTTGAATGTCCGGTCTGCCATAAATCCTTTGCCGATCGGTATATTTCTTCTTTTAGTTTTATATATTTTTTTATATATTATATATATACACGAGTTACGAATAATAACAAAATTTTTTTCGAACCTTGAACGTATGATCAAAATTTCTCATTAAAACATTTGGAACGAAAAATGATAATTAAATATCGTAATCGGATGATTGCAACATATTATATAGTAATACATTATACATACCTATTTTATTTATTTTCTTTAGCAAATTATAAAGTTAATTTTATTGAGTTAATTTTACGATGTTTGAATTAATTAGTGGATACGAGATTATATGGTGTAACGTACATATTTTGTAGATCAAATTTACGTGCGCATCAAAGGACTCGGAATCACCATCAATGGCAATGGCGATGCGGGGAATGTTTCAAAGCATTCTCGCAAAGACGATATTTAGAACGACATTGCCCCGAAGCTTGTAGAAAATATCGAATATCTCAAAGGAGGGAACAGAATTGTAGTTAGAAGGCAAATTTTATTTCTTTAGTATAAACATATTTTTATATTTTTTGAAATATCTAATGTAATATATTAAATGTATTTCGTTAATTAACACTGTAAAATTTGAATTCGAAATATCACTGTATTGTTATTCTAATATACATATATATATGTG"

        when: "we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3

        when: "we add an insertion of AAAA to the 3'UTR at position 694400"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40749-RA-00001")

        String peptideString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdnaString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cdsString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)
        String genomicString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we should have the expected sequences"
        assert peptideString == expectedPeptideString
        assert cdnaString == expectedCdnaString
        assert cdsString == expectedCdsString
        assert genomicString == expectedGenomicString
    }

    void "add start codon insertion at 5'UTR of gene GB40843-RA"() {
        given: "given a gene GB40843-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":1092561,\"strand\":1,\"fmax\":1095202},\"name\":\"GB40843-RA\",\"children\":[{\"location\":{\"fmin\":1092561,\"strand\":1,\"fmax\":1092941},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1094825,\"strand\":1,\"fmax\":1095202},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1092561,\"strand\":1,\"fmax\":1093530},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1093608,\"strand\":1,\"fmax\":1094085},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1094159,\"strand\":1,\"fmax\":1094487},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1094623,\"strand\":1,\"fmax\":1095202},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1092941,\"strand\":1,\"fmax\":1094825},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertionString = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"ATG\",\"location\":{\"fmin\":1092929,\"strand\":1,\"fmax\":1092929},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptideString = "MYLIIMTLDNSWHIIKTAAAENKHELVLSGPAISELIQKRGFDKSLFNLEHLNYLNITQTCLHEVSEEIEKLQNLTTLVLHSNEISKIPSSIGKLEKLKVLDCSRNKLTSLPDEIGKLPQLTTMNFSSNFLKSLPTQIDNVKLTVLDLSNNQFEDFPDVCYTELVHLSEIYVMGNQIKEIPTIINQLPSLKIINVANNRISVIPGEIGDCNKLKELQLKGNPLSDKRLLKLVDQCHNKQILEYVKLHCPKQDGSVNSNSKSKKGKKVQKLSESENNANVMDNLTHKMKILKMANDTPVIKVTKYVKNIRPYIAACIVRNMSFTEDSFKKFIQLQTKLHDGICEKRNAATIATHDLKLITTGDLTYTAKPPNELEIKPLMRNKVYTGSELFKQLQTEADNLRKEKKRNVYSGIHKYLYLLEGKPYFPCLLDASEQVISFPPITNSDITKMSINTQAILIEVTSASSYQICRNVLDQFLKELVTFGLGCVSEQENASNYHKLIIEQVKVVDMEGNMKLVYPSRADLNFAENFITVLRE"
        String expectedCdnaString = "AATTAATAGTATTCATTTTTTATCTTTTGATCAAGCTCATATTTATAATTAAATATCAAAATATAATTCATAGATAACATTAAAATTTGTTTATAGTTTAATTCACTAAAATACGTTTATCATTATTGTTTAATCAAATTATATGTGACGGAAACTATCCGTTATATATCTCTACAAAACTTTTATTAGATTTAGGTTATTTGATGTCTCGTCTTAAATTCATTTATTCTTTTCAATCGCAATTTTTAAATTGCATATGTACGTAGATGTCGTTATAATCGTATAACGCATGTTTCAAATTGATCAACCCGGCAAGTAACCTTGAAACTACGTAAATAACATTACCCTTTTATTAAAAATTCTTTTAAATGTACTTAATAATAATGACGCTCGATAATTCATGGCATATAATCAAAACTGCAGCTGCAGAGAATAAACATGAATTAGTGTTGTCAGGTCCGGCAATCTCTGAATTGATTCAAAAAAGAGGCTTCGACAAGTCTTTATTTAACCTAGAGCATTTGAATTATTTGAATATTACTCAGACATGTTTACACGAAGTGTCAGAAGAAATTGAAAAATTGCAAAATCTAACAACATTGGTATTGCATTCGAATGAGATTTCGAAGATACCTAGCTCAATCGGGAAATTAGAAAAACTAAAGGTTCTCGATTGCTCTAGGAACAAGTTGACGTCTTTACCAGATGAAATCGGTAAACTTCCACAATTAACAACCATGAACTTCAGTTCAAATTTTTTGAAATCATTACCTACGCAAATTGACAATGTTAAGTTGACTGTTTTGGATCTTTCGAACAATCAGTTTGAAGATTTTCCTGATGTGTGCTATACAGAATTGGTTCATCTTTCTGAAATCTATGTCATGGGAAATCAAATAAAAGAAATTCCTACAATTATAAATCAGCTTCCATCTTTAAAAATAATAAATGTAGCAAACAATAGAATTTCAGTTATTCCAGGTGAGATTGGTGATTGTAATAAACTCAAAGAACTTCAATTAAAAGGAAATCCATTATCAGACAAAAGATTATTAAAATTAGTTGATCAATGTCATAATAAACAAATATTAGAATATGTAAAATTGCATTGTCCTAAACAAGATGGTTCTGTCAATTCAAATTCAAAATCAAAAAAAGGAAAAAAGGTACAAAAATTATCAGAAAGTGAAAATAATGCTAATGTAATGGATAATTTGACACACAAAATGAAAATATTAAAAATGGCAAATGATACACCAGTAATTAAGGTTACAAAATATGTGAAAAATATCAGACCTTACATTGCAGCTTGTATTGTTAGAAATATGAGCTTCACAGAAGATAGTTTTAAAAAATTTATTCAACTTCAAACTAAATTACATGATGGCATATGTGAAAAAAGAAATGCAGCTACTATTGCTACACATGACTTAAAATTGATTACCACTGGAGATTTAACATACACAGCAAAACCACCAAATGAATTAGAAATCAAGCCATTAATGCGCAATAAAGTTTATACTGGTTCTGAACTATTCAAACAGTTACAAACTGAAGCTGATAATTTAAGGAAAGAAAAAAAACGCAATGTATATTCTGGCATTCATAAATATCTTTATCTATTAGAAGGTAAACCTTACTTTCCTTGTTTGTTAGATGCTTCAGAACAAGTTATATCATTTCCACCTATAACGAATAGTGATATTACAAAAATGTCAATAAATACCCAAGCAATATTAATAGAAGTAACCAGTGCTTCATCCTATCAAATTTGCAGGAATGTATTAGATCAATTTTTAAAGGAACTAGTTACTTTTGGTTTAGGATGTGTCTCAGAACAAGAAAATGCTTCAAATTATCATAAATTAATCATAGAACAAGTAAAAGTGGTAGATATGGAAGGTAATATGAAATTAGTATATCCTTCAAGAGCAGATTTAAATTTTGCAGAAAATTTTATAACAGTATTACGCGAGTAATAAATTACTGTAAGTAATTAAATTGTTCTTTAATCTTGATCTAATCTGCATTTTTCTTTCTTAATCTTTTTAACTATTATTTTTTTGATTGATAAGTTGTAAATCTAAATTATTTTCATATTATTACTTTTTCATAAATAACACATTATTTTCATATGCCAAATTGTAATTTTTTATTTGTTACTCTGTGAAAATCTTAAGATTAGTATGCAATGTATATAATATTTGCATATTTTATACAGAATCTTTTTGGTATGTATCAATATAATTATATTTTAATCATAATATTTTTATACTGAGATTTGAATCTATGTAAAAATAATAACAATAAGTGTTTTATGTTATGAAAATCAAAAACTGGAAAATAAATATTAAAA"
        String expectedCdsString = "ATGTACTTAATAATAATGACGCTCGATAATTCATGGCATATAATCAAAACTGCAGCTGCAGAGAATAAACATGAATTAGTGTTGTCAGGTCCGGCAATCTCTGAATTGATTCAAAAAAGAGGCTTCGACAAGTCTTTATTTAACCTAGAGCATTTGAATTATTTGAATATTACTCAGACATGTTTACACGAAGTGTCAGAAGAAATTGAAAAATTGCAAAATCTAACAACATTGGTATTGCATTCGAATGAGATTTCGAAGATACCTAGCTCAATCGGGAAATTAGAAAAACTAAAGGTTCTCGATTGCTCTAGGAACAAGTTGACGTCTTTACCAGATGAAATCGGTAAACTTCCACAATTAACAACCATGAACTTCAGTTCAAATTTTTTGAAATCATTACCTACGCAAATTGACAATGTTAAGTTGACTGTTTTGGATCTTTCGAACAATCAGTTTGAAGATTTTCCTGATGTGTGCTATACAGAATTGGTTCATCTTTCTGAAATCTATGTCATGGGAAATCAAATAAAAGAAATTCCTACAATTATAAATCAGCTTCCATCTTTAAAAATAATAAATGTAGCAAACAATAGAATTTCAGTTATTCCAGGTGAGATTGGTGATTGTAATAAACTCAAAGAACTTCAATTAAAAGGAAATCCATTATCAGACAAAAGATTATTAAAATTAGTTGATCAATGTCATAATAAACAAATATTAGAATATGTAAAATTGCATTGTCCTAAACAAGATGGTTCTGTCAATTCAAATTCAAAATCAAAAAAAGGAAAAAAGGTACAAAAATTATCAGAAAGTGAAAATAATGCTAATGTAATGGATAATTTGACACACAAAATGAAAATATTAAAAATGGCAAATGATACACCAGTAATTAAGGTTACAAAATATGTGAAAAATATCAGACCTTACATTGCAGCTTGTATTGTTAGAAATATGAGCTTCACAGAAGATAGTTTTAAAAAATTTATTCAACTTCAAACTAAATTACATGATGGCATATGTGAAAAAAGAAATGCAGCTACTATTGCTACACATGACTTAAAATTGATTACCACTGGAGATTTAACATACACAGCAAAACCACCAAATGAATTAGAAATCAAGCCATTAATGCGCAATAAAGTTTATACTGGTTCTGAACTATTCAAACAGTTACAAACTGAAGCTGATAATTTAAGGAAAGAAAAAAAACGCAATGTATATTCTGGCATTCATAAATATCTTTATCTATTAGAAGGTAAACCTTACTTTCCTTGTTTGTTAGATGCTTCAGAACAAGTTATATCATTTCCACCTATAACGAATAGTGATATTACAAAAATGTCAATAAATACCCAAGCAATATTAATAGAAGTAACCAGTGCTTCATCCTATCAAATTTGCAGGAATGTATTAGATCAATTTTTAAAGGAACTAGTTACTTTTGGTTTAGGATGTGTCTCAGAACAAGAAAATGCTTCAAATTATCATAAATTAATCATAGAACAAGTAAAAGTGGTAGATATGGAAGGTAATATGAAATTAGTATATCCTTCAAGAGCAGATTTAAATTTTGCAGAAAATTTTATAACAGTATTACGCGAGTAA"
        String expectedGenomicString = "AATTAATAGTATTCATTTTTTATCTTTTGATCAAGCTCATATTTATAATTAAATATCAAAATATAATTCATAGATAACATTAAAATTTGTTTATAGTTTAATTCACTAAAATACGTTTATCATTATTGTTTAATCAAATTATATGTGACGGAAACTATCCGTTATATATCTCTACAAAACTTTTATTAGATTTAGGTTATTTGATGTCTCGTCTTAAATTCATTTATTCTTTTCAATCGCAATTTTTAAATTGCATATGTACGTAGATGTCGTTATAATCGTATAACGCATGTTTCAAATTGATCAACCCGGCAAGTAACCTTGAAACTACGTAAATAACATTACCCTTTTATTAAAAATTCTTTTAAATGTACTTAATAATAATGACGCTCGATAATTCATGGCATATAATCAAAACTGCAGCTGCAGAGAATAAACATGAATTAGTGTTGTCAGGTCCGGCAATCTCTGAATTGATTCAAAAAAGAGGCTTCGACAAGTCTTTATTTAACCTAGAGCATTTGAATTATTTGAATATTACTCAGACATGTTTACACGAAGTGTCAGAAGAAATTGAAAAATTGCAAAATCTAACAACATTGGTATTGCATTCGAATGAGATTTCGAAGATACCTAGCTCAATCGGGAAATTAGAAAAACTAAAGGTTCTCGATTGCTCTAGGAACAAGTTGACGTCTTTACCAGATGAAATCGGTAAACTTCCACAATTAACAACCATGAACTTCAGTTCAAATTTTTTGAAATCATTACCTACGCAAATTGACAATGTTAAGTTGACTGTTTTGGATCTTTCGAACAATCAGTTTGAAGATTTTCCTGATGTGTGCTATACAGAATTGGTTCATCTTTCTGAAATCTATGTCATGGGAAATCAAATAAAAGAAATTCCTACAATTATAAATCAGCTTCCATCTTTAAAAATAATAAATGTAGCAAACAATAGAATTTCAGGTAATTTCTTGATAAATGATGCCAAATATATCCAATGTATTTTATATAATTGATTTGATTTTTTTTTCTTTTATATAGTTATTCCAGGTGAGATTGGTGATTGTAATAAACTCAAAGAACTTCAATTAAAAGGAAATCCATTATCAGACAAAAGATTATTAAAATTAGTTGATCAATGTCATAATAAACAAATATTAGAATATGTAAAATTGCATTGTCCTAAACAAGATGGTTCTGTCAATTCAAATTCAAAATCAAAAAAAGGAAAAAAGGTACAAAAATTATCAGAAAGTGAAAATAATGCTAATGTAATGGATAATTTGACACACAAAATGAAAATATTAAAAATGGCAAATGATACACCAGTAATTAAGGTTACAAAATATGTGAAAAATATCAGACCTTACATTGCAGCTTGTATTGTTAGAAATATGAGCTTCACAGAAGATAGTTTTAAAAAATTTATTCAACTTCAAACTAAATTACATGATGGCATATGTGAAAAAAGAAATGCAGCTACTATTGCTACACATGACTTAAAATTGATTACCACTGGTAAATATGAAAAACAATAAAAATAATTCAGATATATAAAAATACATTTCATTAATGTCATGTTTGATATTTAGGAGATTTAACATACACAGCAAAACCACCAAATGAATTAGAAATCAAGCCATTAATGCGCAATAAAGTTTATACTGGTTCTGAACTATTCAAACAGTTACAAACTGAAGCTGATAATTTAAGGAAAGAAAAAAAACGCAATGTATATTCTGGCATTCATAAATATCTTTATCTATTAGAAGGTAAACCTTACTTTCCTTGTTTGTTAGATGCTTCAGAACAAGTTATATCATTTCCACCTATAACGAATAGTGATATTACAAAAATGTCAATAAATACCCAAGCAATATTAATAGAAGTAACCAGTGCTTCATCCTATCAAATTTGCAGGTAATATTAGAATTCATTAATTATAATTAATATTCATTACTAAATTAAATAGTTTTATTAAAAATAATATTAAAATTAATATAAAAAAATTGTTCTGATTATACTATAAAGTAAATTTTTATTTTTTTTTTATTAGGAATGTATTAGATCAATTTTTAAAGGAACTAGTTACTTTTGGTTTAGGATGTGTCTCAGAACAAGAAAATGCTTCAAATTATCATAAATTAATCATAGAACAAGTAAAAGTGGTAGATATGGAAGGTAATATGAAATTAGTATATCCTTCAAGAGCAGATTTAAATTTTGCAGAAAATTTTATAACAGTATTACGCGAGTAATAAATTACTGTAAGTAATTAAATTGTTCTTTAATCTTGATCTAATCTGCATTTTTCTTTCTTAATCTTTTTAACTATTATTTTTTTGATTGATAAGTTGTAAATCTAAATTATTTTCATATTATTACTTTTTCATAAATAACACATTATTTTCATATGCCAAATTGTAATTTTTTATTTGTTACTCTGTGAAAATCTTAAGATTAGTATGCAATGTATATAATATTTGCATATTTTATACAGAATCTTTTTGGTATGTATCAATATAATTATATTTTAATCATAATATTTTTATACTGAGATTTGAATCTATGTAAAAATAATAACAATAAGTGTTTTATGTTATGAAAATCAAAAACTGGAAAATAAATATTAAAA"

        when: "we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 4

        when: "we add a start codon insertion of ATG to the 5'UTR at position 1092930"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40843-RA-00001")

        String peptideString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdnaString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cdsString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)
        String genomicString = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we should have the expected sequences"
        assert peptideString == expectedPeptideString
        assert cdnaString == expectedCdnaString
        assert cdsString == expectedCdsString
        assert genomicString == expectedGenomicString
    }


    void "add deletions to GB40738-RA"() {
        given: "given a gene GB40738-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":874076,\"strand\":-1,\"fmax\":874361},\"name\":\"GB40738-RA\",\"children\":[{\"location\":{\"fmin\":874076,\"strand\":-1,\"fmax\":874091},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":874214,\"strand\":-1,\"fmax\":874252},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":874336,\"strand\":-1,\"fmax\":874361},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":874076,\"strand\":-1,\"fmax\":874361},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addDeletion1String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":874349,\"strand\":1,\"fmax\":874352},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addDeletion2String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":874224,\"strand\":1,\"fmax\":874227},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptide1String = "MILCKCINDGKYSFQRCFIQKSII"
        String expectedCdna1String = "ATGATACTCTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGATGCTTTATTCAGAAGTCAATCATTTGA"
        String expectedCds1String = "ATGATACTCTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGATGCTTTATTCAGAAGTCAATCATTTGA"

        String expectedPeptide2String = "MILCKCINDGKYSFQRFIQKSII"
        String expectedCdna2String = "ATGATACTCTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGCTTTATTCAGAAGTCAATCATTTGA"
        String expectedCds2String = "ATGATACTCTGTAAATGTATAAATGATGGAAAATATTCTTTTCAACGCTTTATTCAGAAGTCAATCATTTGA"

        when: "we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3

        when: "we add a deletion of size 3 to exon 1 at position 874350"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion1String) as JSONObject)

        then: "the deletion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40738-RA-00001")

        String peptide1String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdna1String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cds1String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide1String == expectedPeptide1String
        assert cdna1String == expectedCdna1String
        assert cds1String == expectedCds1String

        when: "we add a deletion of size 3 to exon 2 at position 874225"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion2String) as JSONObject)

        then: "the deletion is successfully added"
        assert SequenceAlteration.count == 2

        when: "we request for the FASTA sequence"
        String peptide2String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdna2String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cds2String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide2String == expectedPeptide2String
        assert cdna2String == expectedCdna2String
        assert cds2String == expectedCds2String

    }

    void "add combination of sequence alterations to GB40821-RA"() {
        given: "given a gene GB40821-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":621650,\"strand\":1,\"fmax\":628275},\"name\":\"GB40821-RA\",\"children\":[{\"location\":{\"fmin\":621650,\"strand\":1,\"fmax\":622270},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":628037,\"strand\":1,\"fmax\":628275},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":621650,\"strand\":1,\"fmax\":622330},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":623090,\"strand\":1,\"fmax\":623213},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":624547,\"strand\":1,\"fmax\":624610},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":624680,\"strand\":1,\"fmax\":624743},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":624885,\"strand\":1,\"fmax\":624927},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":625015,\"strand\":1,\"fmax\":625090},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":627962,\"strand\":1,\"fmax\":628275},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":622270,\"strand\":1,\"fmax\":628037},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addDeletion1String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":621999,\"strand\":1,\"fmax\":622010},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertion2String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"GGGTGCC\",\"location\":{\"fmin\":622274,\"strand\":1,\"fmax\":622274},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addDeletion3String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":622299,\"strand\":1,\"fmax\":622301},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addDeletion4String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":622749,\"strand\":1,\"fmax\":622774},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertion5String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"CCCCCCC\",\"location\":{\"fmin\":623174,\"strand\":1,\"fmax\":623174},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertion6String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"TGA\",\"location\":{\"fmin\":623200,\"strand\":1,\"fmax\":623200},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptide1String = "MEIARIHRYYKTYHFGPRWLLYQYLLTDAKLRNMLDLGPFCGTITFITGLMILILLLYSYMNEKATNSNEKDFQELQKETNKKIPRKKSVADIRPIYNCIHKHLQQTDVFQEKTKKMLCKERLELEILCSKINCINKLLRPEAQTEWRRSKLRKVYYRPINSATSK"
        String expectedCdna1String = "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGAAATCGCAAGGATCCACAGATATTACAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        String expectedCds1String = "ATGGAAATCGCAAGGATCCACAGATATTACAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAA"

        String expectedPeptide2String = "MSWGCQIARIHRYYKTYHFGPRWLLYQYLLTDAKLRNMLDLGPFCGTITFITGLMILILLLYSYMNEKATNSNEKDFQELQKETNKKIPRKKSVADIRPIYNCIHKHLQQTDVFQEKTKKMLCKERLELEILCSKINCINKLLRPEAQTEWRRSKLRKVYYRPINSATSK"
        String expectedCdna2String = "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGGGGTGCCAAATCGCAAGGATCCACAGATATTACAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        String expectedCds2String = "ATGTCATGGGGGTGCCAAATCGCAAGGATCCACAGATATTACAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAA"

        String expectedPeptide3String = "MKNRDMYKSLYRRRDEQQQGEKLKRAHRSLYVKRSSKNCQFFRFSWPVQIHVMLVTIATNYQLRGTKTRCHGGAKSQGSTDIKTYHFGPRWLLYQYLLTDAKLRNMLDLGPFCGTITFITGLMILILLLYSYMNEKATNSNEKDFQELQKETNKKIPRKKSVADIRPIYNCIHKHLQQTDVFQEKTKKMLCKERLELEILCSKINCINKLLRPEAQTEWRRSKLRKVYYRPINSATSK"
        String expectedCdna3String = "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGGGGTGCCAAATCGCAAGGATCCACAGATATTAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        String expectedCds3String = "ATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGGGGTGCCAAATCGCAAGGATCCACAGATATTAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAA"

        String expectedPeptide4String = "MKNRDMYKSLYRRRDEQQQGEKLKRAHRSLYVKRSSKNCQFFRFSWPVQIHVMLVTIATNYQLRGTKTRCHGGAKSQGSTDIKTYHFGPRWLLYQYLLTDAKLRNMLDLGPFCGTITFITGLMILILLLYSYMNEKATNSNEKDFQELQKETNKKIPRKKSVADIRPIYNCIHKHLQQTDVFQEKTKKMLCKERLELEILCSKINCINKLLRPEAQTEWRRSKLRKVYYRPINSATSK"
        String expectedCdna4String = "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGGGGTGCCAAATCGCAAGGATCCACAGATATTAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        String expectedCds4String = "ATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGGGGTGCCAAATCGCAAGGATCCACAGATATTAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAA"

        String expectedPeptide5String = "MKNRDMYKSLYRRRDEQQQGEKLKRAHRSLYVKRSSKNCQFFRFSWPVQIHVMLVTIATNYQLRGTKTRCHGGAKSQGSTDIKTYHFGPRWLLYQYLLTDAKLRNMLDLGPFCGTITFITPPRTHDLDPPPLFIHE"
        String expectedCdna5String = "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGGGGTGCCAAATCGCAAGGATCCACAGATATTAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTCCCCCCCGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        String expectedCds5String = "ATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGGGGTGCCAAATCGCAAGGATCCACAGATATTAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTCCCCCCCGGACTCATGATCTTGATCCTCCTCCTCTATTCATACATGAATGA"

        String expectedPeptide6String = "MKNRDMYKSLYRRRDEQQQGEKLKRAHRSLYVKRSSKNCQFFRFSWPVQIHVMLVTIATNYQLRGTKTRCHGGAKSQGSTDIKTYHFGPRWLLYQYLLTDAKLRNMLDLGPFCGTITFITPPRTHDLDPPP"
        String expectedCdna6String = "AAAAAAACGAATAACCTAATCTAACCTCCTTTATTTCGTCGATTATGATCGAATTATGATCGAAAATATAAATAAATTTCTCGATTATTGCAAAAAAAAATATGAAGAAAATGAAGAAAAGGAATGAAAGAAAATGGAAAATTGAGTAAATAAAAACATATATATGAAAACATGGATACACCGAAATCAATAGCCAATAAAAAACATGATATTACGAGGATTCGCTTCTTGACACGAATCTCTACTTATCGTCGTTGCTTGAATATGCTCCTTTAATTTGTATCGTCTTTCACAACTAATCAAAAATTCCAATATACAACGAATAAATCTCGAAACTTAAAATTTTCAACGTAAAAAATGTATAATGTTAAGATGCTAAGGACGATTTCAAAAATTCAATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGGGGTGCCAAATCGCAAGGATCCACAGATATTAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTCCCCCCCGGACTCATGATCTTGATCCTCCTCCTTGACTATTCATACATGAATGAAAAAGCGACCAATTCGAACGAGAAGGATTTTCAAGAGCTTCAAAAGGAAACAAATAAGAAAATTCCCCGGAAAAAAAGCGTGGCGGACATCAGGCCGATCTACAATTGTATTCATAAACACCTCCAGCAGACCGACGTGTTTCAAGAGAAGACGAAGAAAATGCTTTGCAAGGAACGCTTGGAATTGGAGATTCTGTGCAGTAAAATCAATTGCATCAACAAGCTGTTAAGGCCCGAGGCGCAGACCGAATGGCGGCGGAGCAAGTTACGAAAAGTGTATTATCGTCCTATTAACTCTGCCACGTCGAAGTAATGGCCGACGCCGTGTAACAATGTAATTAACCAAATACAAATGCATCCAATAAAGAACGTACAAATTGCATCGACTTATTACGCGCAGACGCGTTTATGAATTCACGATATTCTTGCACCAAACGTTCCTTTTTTTCTAACCGTGAAGAATTCTTCGTGCACGTTCCACAAATTGTACACTGTATTATTTGCACCCGACACGAAATTGAGCCTGCGTCGAACTGAGAATCGTAGCGGTG"
        String expectedCds6String = "ATGAAAAATCGCGACATGTACAAATCCCTCTATCGAAGACGAGATGAACAACAGCAAGGAGAGAAATTGAAGAGGGCGCATCGATCACTTTATGTCAAACGATCCTCCAAAAACTGTCAGTTTTTTCGATTCTCGTGGCCCGTCCAAATTCACGTGATGCTCGTGACAATAGCGACGAATTATCAGCTTCGCGGGACGAAAACTCGATGTCATGGGGGTGCCAAATCGCAAGGATCCACAGATATTAAAACCTATCATTTTGGGCCACGATGGCTGTTATACCAATATCTGTTGACGGACGCTAAGTTGAGGAATATGCTTGATTTGGGTCCGTTCTGCGGGACCATTACGTTTATAACTCCCCCCCGGACTCATGATCTTGATCCTCCTCCTTGA"

        when: "we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 7

        when: "we add a Deletion of size 11 in 5'UTR at position 622000"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion1String) as JSONObject)

        then: "the deletion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40821-RA-00001")

        String peptide1String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdna1String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cds1String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide1String == expectedPeptide1String
        assert cdna1String == expectedCdna1String
        assert cds1String == expectedCds1String

        when: "we add an Insertion of GGGTGCC in exon 1 at position 622275"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 2

        when: "we request for the FASTA sequence"
        String peptide2String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdna2String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cds2String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide2String == expectedPeptide2String
        assert cdna2String == expectedCdna2String
        assert cds2String == expectedCds2String

        when: "we add a Deletion of size 2 in exon 1 at position 622300"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion3String) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 3

        when: "we request for the FASTA sequence"
        String peptide3String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdna3String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cds3String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide3String == expectedPeptide3String
        assert cdna3String == expectedCdna3String
        assert cds3String == expectedCds3String

        when: "we add a Deletion of 25 in intron at position 622750"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion4String) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 4

        when: "we request for the FASTA sequence"
        String peptide4String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdna4String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cds4String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide4String == expectedPeptide4String
        assert cdna4String == expectedCdna4String
        assert cds4String == expectedCds4String

        when: "we add an Insertion of CCCCCCC in exon 2 at position 623175"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion5String) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 5

        when: "we request for the FASTA sequence"
        String peptide5String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdna5String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cds5String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide5String == expectedPeptide5String
        assert cdna5String == expectedCdna5String
        assert cds5String == expectedCds5String

        when: "we add an Insertion of stop codon TGA in exon 2 at position 623201"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion6String) as JSONObject)

        then: "the insertion is successfully added"
        assert SequenceAlteration.count == 6

        when: "we request for the FASTA sequence"
        String peptide6String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cdna6String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDNA.value)
        String cds6String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide6String == expectedPeptide6String
        assert cdna6String == expectedCdna6String
        assert cds6String == expectedCds6String

    }

    void "add combination of sequence alterations to GB40744-RA"() {
        given: "given a gene GB40744-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":768063},\"name\":\"GB40744-RA\",\"children\":[{\"location\":{\"fmin\":767945,\"strand\":-1,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":763070},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":761542,\"strand\":-1,\"fmax\":763513},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765327,\"strand\":-1,\"fmax\":765472},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":765551,\"strand\":-1,\"fmax\":766176},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":766255,\"strand\":-1,\"fmax\":767133},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767207,\"strand\":-1,\"fmax\":767389},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":767485,\"strand\":-1,\"fmax\":768063},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":763070,\"strand\":-1,\"fmax\":767945},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addDeletion1String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":768024,\"strand\":1,\"fmax\":768034},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertion2String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"AACCC\",\"location\":{\"fmin\":767849,\"strand\":1,\"fmax\":767849},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertion3String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"AAAAAAAAAAA\",\"location\":{\"fmin\":767399,\"strand\":1,\"fmax\":767399},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addDeletion4String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":767324,\"strand\":1,\"fmax\":767354},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addInsertion5String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"CCCGGGA\",\"location\":{\"fmin\":763174,\"strand\":1,\"fmax\":763174},\"type\":{\"name\":\"insertion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedPeptide1String = "MEEMDLGSDESTDKNLHEKSVNRKIESEMIENETEEIKDEFDEEQDKDNDDDDDDDDDDDDEEDADEAEVKILETSLAHNPYDYASHVALINKLQKMGELERLRAAREDMSSKYPLSPDLWLSWMRDEIKLATTIEQKAEVVKLCERAVKDYLAVEVWLEYLQFSIGNMGTEKDAAKNVRHLFERALTDVGLHTIKGAIIWEAFREFEAVLYALIDPLNQAERKEQLERIGNLFKRQLACPLLDMEKTYEEYEAWRHGDGTEAVIDDKIIIGGYNRALSKLNLRLPYEEKIVSAQTENELLDSYKIYLSYEQRNGDPGRITVLYERAITDLSLEMSIWLDYLKYLEENIKIESVLDQVYQRALRNVPWCAKIWQKWIRSYEKWNKSVLEVQTLLENALAAGFSTAEDYRNLWITYLEYLRRKIDRYSTDEGKQLEILRNTFNRACEHLAKSFGLEGDPNCIILQYWARTEAIHANNMEKTRSLWADILSQGHSGTASYWLEYISLERCYGDTKHLRKLFQKALTMVKDWPESIANSWIDFERDEGTLEQMEICEIRTKEKLDKVAEERQKMQQMSNHELSPLQNKKTLKRKQDETGKWKNLGSSPTKITKVEMQIKPKIRESRLNFEKNADSEEQKLKTAPPPGFKMPENEQMEIDNMNEMDDKSTVFISNLDYTASEEEVRNALQPAGPITMFKMIRDYKGRSKGYCYVQLSNIEAIDKALQLDRTPIRGRPMFVSKCDPNRTRGSGFKYSCSLEKNKLFVKGLPVSTTKEDLEEIFKVHGALKEVRIVTYRNGHSKGLAYIEFKDENSAAKALLATDGMKIADKIISVAISQPPERKKVPATEEPLLVKSLGGTTVSRTTFGMPKTLLSMVPRTVKTAATNGSANVPGNGVAPKMNNQDFRNMLLNKK"
        String expectedCds1String = "ATGGAAGAAATGGATTTAGGAAGCGATGAAAGTACCGATAAAAATTTACATGAAAAGTCGGTGAACAGAAAGATCGAATCAGAAATGATTGAAAATGAAACTGAAGAAATTAAGGATGAATTTGATGAAGAACAGGATAAAGATAATGATGATGATGATGATGATGATGATGATGATGATGATGAAGAGGATGCTGATGAAGCAGAAGTTAAAATTTTAGAAACTTCTCTTGCTCACAATCCATATGATTATGCGAGTCATGTAGCTCTTATCAACAAATTACAAAAAATGGGTGAATTAGAACGATTACGCGCTGCCAGAGAAGATATGAGTTCAAAATATCCCTTGAGTCCTGATCTCTGGTTATCTTGGATGCGTGACGAAATTAAATTAGCAACCACCATAGAACAGAAAGCTGAAGTAGTAAAATTATGTGAGCGAGCAGTAAAAGATTATCTTGCTGTAGAAGTATGGTTAGAATATTTGCAATTTAGTATTGGTAATATGGGCACTGAAAAAGATGCAGCAAAAAATGTTAGACATCTCTTTGAAAGAGCATTAACAGATGTTGGATTACATACTATTAAAGGTGCAATAATATGGGAAGCATTTCGAGAGTTTGAAGCTGTGTTATATGCTCTGATTGATCCTTTAAATCAAGCTGAAAGGAAAGAGCAATTAGAACGTATTGGTAACTTATTCAAAAGACAATTAGCTTGTCCTCTTTTAGATATGGAAAAAACATATGAAGAATATGAAGCTTGGCGTCATGGAGATGGTACAGAAGCTGTTATTGATGATAAAATTATTATTGGAGGATATAATCGAGCTCTTTCAAAGCTTAATCTTCGTTTACCTTATGAGGAAAAAATTGTTTCTGCACAAACAGAAAATGAATTATTAGATTCATACAAAATATATTTATCATACGAACAACGTAATGGTGATCCTGGAAGAATCACAGTTTTGTATGAAAGAGCAATCACTGATCTTAGTTTAGAAATGTCAATTTGGCTTGATTACCTTAAATATTTAGAAGAAAATATTAAAATTGAATCAGTTTTAGATCAAGTATATCAAAGAGCTTTAAGAAATGTTCCTTGGTGTGCAAAAATATGGCAAAAATGGATAAGATCATACGAAAAATGGAACAAATCGGTATTAGAAGTTCAAACTTTATTAGAAAATGCTTTAGCAGCTGGATTTTCTACCGCGGAAGATTATCGAAATTTATGGATAACTTATTTGGAATATTTACGACGAAAAATCGATCGTTATTCTACAGACGAAGGAAAACAATTAGAAATATTACGTAATACTTTTAATAGAGCTTGTGAACATTTGGCAAAATCATTTGGCTTAGAAGGAGACCCTAATTGTATTATATTGCAATATTGGGCAAGAACTGAAGCTATACATGCTAATAATATGGAAAAAACTAGATCATTATGGGCTGATATTTTGTCACAAGGACATTCCGGAACAGCATCTTATTGGTTGGAATATATTTCATTAGAAAGATGTTATGGAGATACAAAACATTTGAGGAAATTATTCCAAAAAGCTTTGACCATGGTAAAAGATTGGCCAGAAAGCATAGCAAATTCTTGGATAGATTTTGAACGGGATGAAGGAACATTAGAACAAATGGAAATATGTGAAATACGAACAAAAGAAAAATTAGATAAAGTTGCTGAAGAAAGACAGAAAATGCAACAAATGTCCAATCATGAACTATCACCATTACAAAACAAAAAAACTCTTAAAAGAAAACAAGACGAAACTGGTAAATGGAAAAATTTAGGATCTTCTCCAACAAAAATTACAAAAGTTGAAATGCAAATAAAACCAAAAATTAGAGAAAGTCGTTTAAATTTCGAAAAAAATGCTGATTCTGAAGAACAAAAATTAAAAACTGCGCCTCCGCCTGGTTTTAAAATGCCGGAAAATGAACAAATGGAGATAGATAATATGAATGAAATGGATGACAAAAGTACAGTTTTTATAAGTAATTTAGATTACACAGCAAGTGAAGAAGAAGTTAGAAATGCTTTACAACCAGCAGGACCAATTACAATGTTTAAAATGATACGAGATTATAAAGGACGTAGTAAAGGATATTGTTATGTGCAACTCAGTAATATAGAAGCAATTGATAAAGCTCTACAGTTGGATAGAACTCCTATAAGAGGGCGGCCAATGTTTGTTTCAAAGTGCGATCCCAACAGAACACGAGGATCTGGATTTAAATACAGCTGTTCTCTTGAGAAAAACAAGCTTTTTGTTAAAGGACTTCCGGTATCAACGACAAAAGAAGATCTTGAAGAAATTTTCAAAGTTCACGGAGCATTGAAGGAAGTCCGTATAGTTACTTACCGTAATGGCCATTCTAAAGGGTTGGCTTACATCGAATTTAAGGACGAGAACAGTGCCGCGAAGGCACTTCTGGCCACTGATGGAATGAAAATTGCCGACAAAATAATTAGTGTAGCCATAAGCCAACCACCTGAACGCAAGAAAGTACCAGCGACTGAAGAACCTCTCTTAGTTAAGTCTCTAGGTGGTACTACAGTAAGCAGAACTACATTTGGTATGCCCAAAACATTATTATCTATGGTACCTCGTACTGTCAAAACTGCTGCTACTAATGGCAGTGCAAATGTGCCTGGGAATGGTGTTGCTCCAAAAATGAATAATCAAGATTTTAGAAATATGTTACTGAATAAAAAGTAA"

        String expectedPeptide2String = "MGELERLRAAREDMSSKYPLSPDLWLSWMRDEIKLATTIEQKAEVVKLCERAVKDYLAVEVWLEYLQFSIGNMGTEKDAAKNVRHLFERALTDVGLHTIKGAIIWEAFREFEAVLYALIDPLNQAERKEQLERIGNLFKRQLACPLLDMEKTYEEYEAWRHGDGTEAVIDDKIIIGGYNRALSKLNLRLPYEEKIVSAQTENELLDSYKIYLSYEQRNGDPGRITVLYERAITDLSLEMSIWLDYLKYLEENIKIESVLDQVYQRALRNVPWCAKIWQKWIRSYEKWNKSVLEVQTLLENALAAGFSTAEDYRNLWITYLEYLRRKIDRYSTDEGKQLEILRNTFNRACEHLAKSFGLEGDPNCIILQYWARTEAIHANNMEKTRSLWADILSQGHSGTASYWLEYISLERCYGDTKHLRKLFQKALTMVKDWPESIANSWIDFERDEGTLEQMEICEIRTKEKLDKVAEERQKMQQMSNHELSPLQNKKTLKRKQDETGKWKNLGSSPTKITKVEMQIKPKIRESRLNFEKNADSEEQKLKTAPPPGFKMPENEQMEIDNMNEMDDKSTVFISNLDYTASEEEVRNALQPAGPITMFKMIRDYKGRSKGYCYVQLSNIEAIDKALQLDRTPIRGRPMFVSKCDPNRTRGSGFKYSCSLEKNKLFVKGLPVSTTKEDLEEIFKVHGALKEVRIVTYRNGHSKGLAYIEFKDENSAAKALLATDGMKIADKIISVAISQPPERKKVPATEEPLLVKSLGGTTVSRTTFGMPKTLLSMVPRTVKTAATNGSANVPGNGVAPKMNNQDFRNMLLNKK"
        String expectedCds2String = "ATGGGTGAATTAGAACGATTACGCGCTGCCAGAGAAGATATGAGTTCAAAATATCCCTTGAGTCCTGATCTCTGGTTATCTTGGATGCGTGACGAAATTAAATTAGCAACCACCATAGAACAGAAAGCTGAAGTAGTAAAATTATGTGAGCGAGCAGTAAAAGATTATCTTGCTGTAGAAGTATGGTTAGAATATTTGCAATTTAGTATTGGTAATATGGGCACTGAAAAAGATGCAGCAAAAAATGTTAGACATCTCTTTGAAAGAGCATTAACAGATGTTGGATTACATACTATTAAAGGTGCAATAATATGGGAAGCATTTCGAGAGTTTGAAGCTGTGTTATATGCTCTGATTGATCCTTTAAATCAAGCTGAAAGGAAAGAGCAATTAGAACGTATTGGTAACTTATTCAAAAGACAATTAGCTTGTCCTCTTTTAGATATGGAAAAAACATATGAAGAATATGAAGCTTGGCGTCATGGAGATGGTACAGAAGCTGTTATTGATGATAAAATTATTATTGGAGGATATAATCGAGCTCTTTCAAAGCTTAATCTTCGTTTACCTTATGAGGAAAAAATTGTTTCTGCACAAACAGAAAATGAATTATTAGATTCATACAAAATATATTTATCATACGAACAACGTAATGGTGATCCTGGAAGAATCACAGTTTTGTATGAAAGAGCAATCACTGATCTTAGTTTAGAAATGTCAATTTGGCTTGATTACCTTAAATATTTAGAAGAAAATATTAAAATTGAATCAGTTTTAGATCAAGTATATCAAAGAGCTTTAAGAAATGTTCCTTGGTGTGCAAAAATATGGCAAAAATGGATAAGATCATACGAAAAATGGAACAAATCGGTATTAGAAGTTCAAACTTTATTAGAAAATGCTTTAGCAGCTGGATTTTCTACCGCGGAAGATTATCGAAATTTATGGATAACTTATTTGGAATATTTACGACGAAAAATCGATCGTTATTCTACAGACGAAGGAAAACAATTAGAAATATTACGTAATACTTTTAATAGAGCTTGTGAACATTTGGCAAAATCATTTGGCTTAGAAGGAGACCCTAATTGTATTATATTGCAATATTGGGCAAGAACTGAAGCTATACATGCTAATAATATGGAAAAAACTAGATCATTATGGGCTGATATTTTGTCACAAGGACATTCCGGAACAGCATCTTATTGGTTGGAATATATTTCATTAGAAAGATGTTATGGAGATACAAAACATTTGAGGAAATTATTCCAAAAAGCTTTGACCATGGTAAAAGATTGGCCAGAAAGCATAGCAAATTCTTGGATAGATTTTGAACGGGATGAAGGAACATTAGAACAAATGGAAATATGTGAAATACGAACAAAAGAAAAATTAGATAAAGTTGCTGAAGAAAGACAGAAAATGCAACAAATGTCCAATCATGAACTATCACCATTACAAAACAAAAAAACTCTTAAAAGAAAACAAGACGAAACTGGTAAATGGAAAAATTTAGGATCTTCTCCAACAAAAATTACAAAAGTTGAAATGCAAATAAAACCAAAAATTAGAGAAAGTCGTTTAAATTTCGAAAAAAATGCTGATTCTGAAGAACAAAAATTAAAAACTGCGCCTCCGCCTGGTTTTAAAATGCCGGAAAATGAACAAATGGAGATAGATAATATGAATGAAATGGATGACAAAAGTACAGTTTTTATAAGTAATTTAGATTACACAGCAAGTGAAGAAGAAGTTAGAAATGCTTTACAACCAGCAGGACCAATTACAATGTTTAAAATGATACGAGATTATAAAGGACGTAGTAAAGGATATTGTTATGTGCAACTCAGTAATATAGAAGCAATTGATAAAGCTCTACAGTTGGATAGAACTCCTATAAGAGGGCGGCCAATGTTTGTTTCAAAGTGCGATCCCAACAGAACACGAGGATCTGGATTTAAATACAGCTGTTCTCTTGAGAAAAACAAGCTTTTTGTTAAAGGACTTCCGGTATCAACGACAAAAGAAGATCTTGAAGAAATTTTCAAAGTTCACGGAGCATTGAAGGAAGTCCGTATAGTTACTTACCGTAATGGCCATTCTAAAGGGTTGGCTTACATCGAATTTAAGGACGAGAACAGTGCCGCGAAGGCACTTCTGGCCACTGATGGAATGAAAATTGCCGACAAAATAATTAGTGTAGCCATAAGCCAACCACCTGAACGCAAGAAAGTACCAGCGACTGAAGAACCTCTCTTAGTTAAGTCTCTAGGTGGTACTACAGTAAGCAGAACTACATTTGGTATGCCCAAAACATTATTATCTATGGTACCTCGTACTGTCAAAACTGCTGCTACTAATGGCAGTGCAAATGTGCCTGGGAATGGTGTTGCTCCAAAAATGAATAATCAAGATTTTAGAAATATGTTACTGAATAAAAAGTAA"

        String expectedPeptide3String = "MGELERLRAAREDMSSKYPLSPDLWLSWMRDEIKLATTIEQKAEVVKLCERAVKDYLAVEVWLEYLQFSIGNMGTEKDAAKNVRHLFERALTDVGLHTIKGAIIWEAFREFEAVLYALIDPLNQAERKEQLERIGNLFKRQLACPLLDMEKTYEEYEAWRHGDGTEAVIDDKIIIGGYNRALSKLNLRLPYEEKIVSAQTENELLDSYKIYLSYEQRNGDPGRITVLYERAITDLSLEMSIWLDYLKYLEENIKIESVLDQVYQRALRNVPWCAKIWQKWIRSYEKWNKSVLEVQTLLENALAAGFSTAEDYRNLWITYLEYLRRKIDRYSTDEGKQLEILRNTFNRACEHLAKSFGLEGDPNCIILQYWARTEAIHANNMEKTRSLWADILSQGHSGTASYWLEYISLERCYGDTKHLRKLFQKALTMVKDWPESIANSWIDFERDEGTLEQMEICEIRTKEKLDKVAEERQKMQQMSNHELSPLQNKKTLKRKQDETGKWKNLGSSPTKITKVEMQIKPKIRESRLNFEKNADSEEQKLKTAPPPGFKMPENEQMEIDNMNEMDDKSTVFISNLDYTASEEEVRNALQPAGPITMFKMIRDYKGRSKGYCYVQLSNIEAIDKALQLDRTPIRGRPMFVSKCDPNRTRGSGFKYSCSLEKNKLFVKGLPVSTTKEDLEEIFKVHGALKEVRIVTYRNGHSKGLAYIEFKDENSAAKALLATDGMKIADKIISVAISQPPERKKVPATEEPLLVKSLGGTTVSRTTFGMPKTLLSMVPRTVKTAATNGSANVPGNGVAPKMNNQDFRNMLLNKK"
        String expectedCds3String = "ATGGGTGAATTAGAACGATTACGCGCTGCCAGAGAAGATATGAGTTCAAAATATCCCTTGAGTCCTGATCTCTGGTTATCTTGGATGCGTGACGAAATTAAATTAGCAACCACCATAGAACAGAAAGCTGAAGTAGTAAAATTATGTGAGCGAGCAGTAAAAGATTATCTTGCTGTAGAAGTATGGTTAGAATATTTGCAATTTAGTATTGGTAATATGGGCACTGAAAAAGATGCAGCAAAAAATGTTAGACATCTCTTTGAAAGAGCATTAACAGATGTTGGATTACATACTATTAAAGGTGCAATAATATGGGAAGCATTTCGAGAGTTTGAAGCTGTGTTATATGCTCTGATTGATCCTTTAAATCAAGCTGAAAGGAAAGAGCAATTAGAACGTATTGGTAACTTATTCAAAAGACAATTAGCTTGTCCTCTTTTAGATATGGAAAAAACATATGAAGAATATGAAGCTTGGCGTCATGGAGATGGTACAGAAGCTGTTATTGATGATAAAATTATTATTGGAGGATATAATCGAGCTCTTTCAAAGCTTAATCTTCGTTTACCTTATGAGGAAAAAATTGTTTCTGCACAAACAGAAAATGAATTATTAGATTCATACAAAATATATTTATCATACGAACAACGTAATGGTGATCCTGGAAGAATCACAGTTTTGTATGAAAGAGCAATCACTGATCTTAGTTTAGAAATGTCAATTTGGCTTGATTACCTTAAATATTTAGAAGAAAATATTAAAATTGAATCAGTTTTAGATCAAGTATATCAAAGAGCTTTAAGAAATGTTCCTTGGTGTGCAAAAATATGGCAAAAATGGATAAGATCATACGAAAAATGGAACAAATCGGTATTAGAAGTTCAAACTTTATTAGAAAATGCTTTAGCAGCTGGATTTTCTACCGCGGAAGATTATCGAAATTTATGGATAACTTATTTGGAATATTTACGACGAAAAATCGATCGTTATTCTACAGACGAAGGAAAACAATTAGAAATATTACGTAATACTTTTAATAGAGCTTGTGAACATTTGGCAAAATCATTTGGCTTAGAAGGAGACCCTAATTGTATTATATTGCAATATTGGGCAAGAACTGAAGCTATACATGCTAATAATATGGAAAAAACTAGATCATTATGGGCTGATATTTTGTCACAAGGACATTCCGGAACAGCATCTTATTGGTTGGAATATATTTCATTAGAAAGATGTTATGGAGATACAAAACATTTGAGGAAATTATTCCAAAAAGCTTTGACCATGGTAAAAGATTGGCCAGAAAGCATAGCAAATTCTTGGATAGATTTTGAACGGGATGAAGGAACATTAGAACAAATGGAAATATGTGAAATACGAACAAAAGAAAAATTAGATAAAGTTGCTGAAGAAAGACAGAAAATGCAACAAATGTCCAATCATGAACTATCACCATTACAAAACAAAAAAACTCTTAAAAGAAAACAAGACGAAACTGGTAAATGGAAAAATTTAGGATCTTCTCCAACAAAAATTACAAAAGTTGAAATGCAAATAAAACCAAAAATTAGAGAAAGTCGTTTAAATTTCGAAAAAAATGCTGATTCTGAAGAACAAAAATTAAAAACTGCGCCTCCGCCTGGTTTTAAAATGCCGGAAAATGAACAAATGGAGATAGATAATATGAATGAAATGGATGACAAAAGTACAGTTTTTATAAGTAATTTAGATTACACAGCAAGTGAAGAAGAAGTTAGAAATGCTTTACAACCAGCAGGACCAATTACAATGTTTAAAATGATACGAGATTATAAAGGACGTAGTAAAGGATATTGTTATGTGCAACTCAGTAATATAGAAGCAATTGATAAAGCTCTACAGTTGGATAGAACTCCTATAAGAGGGCGGCCAATGTTTGTTTCAAAGTGCGATCCCAACAGAACACGAGGATCTGGATTTAAATACAGCTGTTCTCTTGAGAAAAACAAGCTTTTTGTTAAAGGACTTCCGGTATCAACGACAAAAGAAGATCTTGAAGAAATTTTCAAAGTTCACGGAGCATTGAAGGAAGTCCGTATAGTTACTTACCGTAATGGCCATTCTAAAGGGTTGGCTTACATCGAATTTAAGGACGAGAACAGTGCCGCGAAGGCACTTCTGGCCACTGATGGAATGAAAATTGCCGACAAAATAATTAGTGTAGCCATAAGCCAACCACCTGAACGCAAGAAAGTACCAGCGACTGAAGAACCTCTCTTAGTTAAGTCTCTAGGTGGTACTACAGTAAGCAGAACTACATTTGGTATGCCCAAAACATTATTATCTATGGTACCTCGTACTGTCAAAACTGCTGCTACTAATGGCAGTGCAAATGTGCCTGGGAATGGTGTTGCTCCAAAAATGAATAATCAAGATTTTAGAAATATGTTACTGAATAAAAAGTAA"

        String expectedPeptide4String = "MGELERLRAAREDMSSKYPLSPDLWLSWMRDEIKLATTIEQKAEVVKLCERAVKDYLAVEVWLEYLQFSAKNVRHLFERALTDVGLHTIKGAIIWEAFREFEAVLYALIDPLNQAERKEQLERIGNLFKRQLACPLLDMEKTYEEYEAWRHGDGTEAVIDDKIIIGGYNRALSKLNLRLPYEEKIVSAQTENELLDSYKIYLSYEQRNGDPGRITVLYERAITDLSLEMSIWLDYLKYLEENIKIESVLDQVYQRALRNVPWCAKIWQKWIRSYEKWNKSVLEVQTLLENALAAGFSTAEDYRNLWITYLEYLRRKIDRYSTDEGKQLEILRNTFNRACEHLAKSFGLEGDPNCIILQYWARTEAIHANNMEKTRSLWADILSQGHSGTASYWLEYISLERCYGDTKHLRKLFQKALTMVKDWPESIANSWIDFERDEGTLEQMEICEIRTKEKLDKVAEERQKMQQMSNHELSPLQNKKTLKRKQDETGKWKNLGSSPTKITKVEMQIKPKIRESRLNFEKNADSEEQKLKTAPPPGFKMPENEQMEIDNMNEMDDKSTVFISNLDYTASEEEVRNALQPAGPITMFKMIRDYKGRSKGYCYVQLSNIEAIDKALQLDRTPIRGRPMFVSKCDPNRTRGSGFKYSCSLEKNKLFVKGLPVSTTKEDLEEIFKVHGALKEVRIVTYRNGHSKGLAYIEFKDENSAAKALLATDGMKIADKIISVAISQPPERKKVPATEEPLLVKSLGGTTVSRTTFGMPKTLLSMVPRTVKTAATNGSANVPGNGVAPKMNNQDFRNMLLNKK"
        String expectedCds4String = "ATGGGTGAATTAGAACGATTACGCGCTGCCAGAGAAGATATGAGTTCAAAATATCCCTTGAGTCCTGATCTCTGGTTATCTTGGATGCGTGACGAAATTAAATTAGCAACCACCATAGAACAGAAAGCTGAAGTAGTAAAATTATGTGAGCGAGCAGTAAAAGATTATCTTGCTGTAGAAGTATGGTTAGAATATTTGCAATTTAGTGCAAAAAATGTTAGACATCTCTTTGAAAGAGCATTAACAGATGTTGGATTACATACTATTAAAGGTGCAATAATATGGGAAGCATTTCGAGAGTTTGAAGCTGTGTTATATGCTCTGATTGATCCTTTAAATCAAGCTGAAAGGAAAGAGCAATTAGAACGTATTGGTAACTTATTCAAAAGACAATTAGCTTGTCCTCTTTTAGATATGGAAAAAACATATGAAGAATATGAAGCTTGGCGTCATGGAGATGGTACAGAAGCTGTTATTGATGATAAAATTATTATTGGAGGATATAATCGAGCTCTTTCAAAGCTTAATCTTCGTTTACCTTATGAGGAAAAAATTGTTTCTGCACAAACAGAAAATGAATTATTAGATTCATACAAAATATATTTATCATACGAACAACGTAATGGTGATCCTGGAAGAATCACAGTTTTGTATGAAAGAGCAATCACTGATCTTAGTTTAGAAATGTCAATTTGGCTTGATTACCTTAAATATTTAGAAGAAAATATTAAAATTGAATCAGTTTTAGATCAAGTATATCAAAGAGCTTTAAGAAATGTTCCTTGGTGTGCAAAAATATGGCAAAAATGGATAAGATCATACGAAAAATGGAACAAATCGGTATTAGAAGTTCAAACTTTATTAGAAAATGCTTTAGCAGCTGGATTTTCTACCGCGGAAGATTATCGAAATTTATGGATAACTTATTTGGAATATTTACGACGAAAAATCGATCGTTATTCTACAGACGAAGGAAAACAATTAGAAATATTACGTAATACTTTTAATAGAGCTTGTGAACATTTGGCAAAATCATTTGGCTTAGAAGGAGACCCTAATTGTATTATATTGCAATATTGGGCAAGAACTGAAGCTATACATGCTAATAATATGGAAAAAACTAGATCATTATGGGCTGATATTTTGTCACAAGGACATTCCGGAACAGCATCTTATTGGTTGGAATATATTTCATTAGAAAGATGTTATGGAGATACAAAACATTTGAGGAAATTATTCCAAAAAGCTTTGACCATGGTAAAAGATTGGCCAGAAAGCATAGCAAATTCTTGGATAGATTTTGAACGGGATGAAGGAACATTAGAACAAATGGAAATATGTGAAATACGAACAAAAGAAAAATTAGATAAAGTTGCTGAAGAAAGACAGAAAATGCAACAAATGTCCAATCATGAACTATCACCATTACAAAACAAAAAAACTCTTAAAAGAAAACAAGACGAAACTGGTAAATGGAAAAATTTAGGATCTTCTCCAACAAAAATTACAAAAGTTGAAATGCAAATAAAACCAAAAATTAGAGAAAGTCGTTTAAATTTCGAAAAAAATGCTGATTCTGAAGAACAAAAATTAAAAACTGCGCCTCCGCCTGGTTTTAAAATGCCGGAAAATGAACAAATGGAGATAGATAATATGAATGAAATGGATGACAAAAGTACAGTTTTTATAAGTAATTTAGATTACACAGCAAGTGAAGAAGAAGTTAGAAATGCTTTACAACCAGCAGGACCAATTACAATGTTTAAAATGATACGAGATTATAAAGGACGTAGTAAAGGATATTGTTATGTGCAACTCAGTAATATAGAAGCAATTGATAAAGCTCTACAGTTGGATAGAACTCCTATAAGAGGGCGGCCAATGTTTGTTTCAAAGTGCGATCCCAACAGAACACGAGGATCTGGATTTAAATACAGCTGTTCTCTTGAGAAAAACAAGCTTTTTGTTAAAGGACTTCCGGTATCAACGACAAAAGAAGATCTTGAAGAAATTTTCAAAGTTCACGGAGCATTGAAGGAAGTCCGTATAGTTACTTACCGTAATGGCCATTCTAAAGGGTTGGCTTACATCGAATTTAAGGACGAGAACAGTGCCGCGAAGGCACTTCTGGCCACTGATGGAATGAAAATTGCCGACAAAATAATTAGTGTAGCCATAAGCCAACCACCTGAACGCAAGAAAGTACCAGCGACTGAAGAACCTCTCTTAGTTAAGTCTCTAGGTGGTACTACAGTAAGCAGAACTACATTTGGTATGCCCAAAACATTATTATCTATGGTACCTCGTACTGTCAAAACTGCTGCTACTAATGGCAGTGCAAATGTGCCTGGGAATGGTGTTGCTCCAAAAATGAATAATCAAGATTTTAGAAATATGTTACTGAATAAAAAGTAA"

        String expectedPeptide5String = "MGELERLRAAREDMSSKYPLSPDLWLSWMRDEIKLATTIEQKAEVVKLCERAVKDYLAVEVWLEYLQFSAKNVRHLFERALTDVGLHTIKGAIIWEAFREFEAVLYALIDPLNQAERKEQLERIGNLFKRQLACPLLDMEKTYEEYEAWRHGDGTEAVIDDKIIIGGYNRALSKLNLRLPYEEKIVSAQTENELLDSYKIYLSYEQRNGDPGRITVLYERAITDLSLEMSIWLDYLKYLEENIKIESVLDQVYQRALRNVPWCAKIWQKWIRSYEKWNKSVLEVQTLLENALAAGFSTAEDYRNLWITYLEYLRRKIDRYSTDEGKQLEILRNTFNRACEHLAKSFGLEGDPNCIILQYWARTEAIHANNMEKTRSLWADILSQGHSGTASYWLEYISLERCYGDTKHLRKLFQKALTMVKDWPESIANSWIDFERDEGTLEQMEICEIRTKEKLDKVAEERQKMQQMSNHELSPLQNKKTLKRKQDETGKWKNLGSSPTKITKVEMQIKPKIRESRLNFEKNADSEEQKLKTAPPPGFKMPENEQMEIDNMNEMDDKSTVFISNLDYTASEEEVRNALQPAGPITMFKMIRDYKGRSKGYCYVQLSNIEAIDKALQLDRTPIRGRPMFVSKCDPNRTRGSGFKYSCSLEKNKLFVKGLPVSTTKEDLEEIFKVHGALKEVRIVTYRNGHSKGLAYIEFKDENSAAKALLATDGMKIADKIISVAISQPPERKKVPATEEPLLVKSLGGTTVSRTTFGMPKTLLSMVPRTVPGQNCCY"
        String expectedCds5String = "ATGGGTGAATTAGAACGATTACGCGCTGCCAGAGAAGATATGAGTTCAAAATATCCCTTGAGTCCTGATCTCTGGTTATCTTGGATGCGTGACGAAATTAAATTAGCAACCACCATAGAACAGAAAGCTGAAGTAGTAAAATTATGTGAGCGAGCAGTAAAAGATTATCTTGCTGTAGAAGTATGGTTAGAATATTTGCAATTTAGTGCAAAAAATGTTAGACATCTCTTTGAAAGAGCATTAACAGATGTTGGATTACATACTATTAAAGGTGCAATAATATGGGAAGCATTTCGAGAGTTTGAAGCTGTGTTATATGCTCTGATTGATCCTTTAAATCAAGCTGAAAGGAAAGAGCAATTAGAACGTATTGGTAACTTATTCAAAAGACAATTAGCTTGTCCTCTTTTAGATATGGAAAAAACATATGAAGAATATGAAGCTTGGCGTCATGGAGATGGTACAGAAGCTGTTATTGATGATAAAATTATTATTGGAGGATATAATCGAGCTCTTTCAAAGCTTAATCTTCGTTTACCTTATGAGGAAAAAATTGTTTCTGCACAAACAGAAAATGAATTATTAGATTCATACAAAATATATTTATCATACGAACAACGTAATGGTGATCCTGGAAGAATCACAGTTTTGTATGAAAGAGCAATCACTGATCTTAGTTTAGAAATGTCAATTTGGCTTGATTACCTTAAATATTTAGAAGAAAATATTAAAATTGAATCAGTTTTAGATCAAGTATATCAAAGAGCTTTAAGAAATGTTCCTTGGTGTGCAAAAATATGGCAAAAATGGATAAGATCATACGAAAAATGGAACAAATCGGTATTAGAAGTTCAAACTTTATTAGAAAATGCTTTAGCAGCTGGATTTTCTACCGCGGAAGATTATCGAAATTTATGGATAACTTATTTGGAATATTTACGACGAAAAATCGATCGTTATTCTACAGACGAAGGAAAACAATTAGAAATATTACGTAATACTTTTAATAGAGCTTGTGAACATTTGGCAAAATCATTTGGCTTAGAAGGAGACCCTAATTGTATTATATTGCAATATTGGGCAAGAACTGAAGCTATACATGCTAATAATATGGAAAAAACTAGATCATTATGGGCTGATATTTTGTCACAAGGACATTCCGGAACAGCATCTTATTGGTTGGAATATATTTCATTAGAAAGATGTTATGGAGATACAAAACATTTGAGGAAATTATTCCAAAAAGCTTTGACCATGGTAAAAGATTGGCCAGAAAGCATAGCAAATTCTTGGATAGATTTTGAACGGGATGAAGGAACATTAGAACAAATGGAAATATGTGAAATACGAACAAAAGAAAAATTAGATAAAGTTGCTGAAGAAAGACAGAAAATGCAACAAATGTCCAATCATGAACTATCACCATTACAAAACAAAAAAACTCTTAAAAGAAAACAAGACGAAACTGGTAAATGGAAAAATTTAGGATCTTCTCCAACAAAAATTACAAAAGTTGAAATGCAAATAAAACCAAAAATTAGAGAAAGTCGTTTAAATTTCGAAAAAAATGCTGATTCTGAAGAACAAAAATTAAAAACTGCGCCTCCGCCTGGTTTTAAAATGCCGGAAAATGAACAAATGGAGATAGATAATATGAATGAAATGGATGACAAAAGTACAGTTTTTATAAGTAATTTAGATTACACAGCAAGTGAAGAAGAAGTTAGAAATGCTTTACAACCAGCAGGACCAATTACAATGTTTAAAATGATACGAGATTATAAAGGACGTAGTAAAGGATATTGTTATGTGCAACTCAGTAATATAGAAGCAATTGATAAAGCTCTACAGTTGGATAGAACTCCTATAAGAGGGCGGCCAATGTTTGTTTCAAAGTGCGATCCCAACAGAACACGAGGATCTGGATTTAAATACAGCTGTTCTCTTGAGAAAAACAAGCTTTTTGTTAAAGGACTTCCGGTATCAACGACAAAAGAAGATCTTGAAGAAATTTTCAAAGTTCACGGAGCATTGAAGGAAGTCCGTATAGTTACTTACCGTAATGGCCATTCTAAAGGGTTGGCTTACATCGAATTTAAGGACGAGAACAGTGCCGCGAAGGCACTTCTGGCCACTGATGGAATGAAAATTGCCGACAAAATAATTAGTGTAGCCATAAGCCAACCACCTGAACGCAAGAAAGTACCAGCGACTGAAGAACCTCTCTTAGTTAAGTCTCTAGGTGGTACTACAGTAAGCAGAACTACATTTGGTATGCCCAAAACATTATTATCTATGGTACCTCGTACTGTCCCGGGTCAAAACTGCTGCTACTAA"

        when: "we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we expect to see the gene added"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 6

        when: "we add a Deletion of size 10 in 5'UTR at position 768025"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion1String) as JSONObject)

        then: "the deletion is successfully added"
        assert SequenceAlteration.count == 1

        when: "we request for the FASTA sequence"
        MRNA mrna = MRNA.findByName("GB40744-RA-00001")

        String peptide1String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cds1String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide1String == expectedPeptide1String
        assert cds1String == expectedCds1String

        when: "we add an Insertion of AACCC in exon 1 at position 767850"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion2String) as JSONObject)

        then: "the Insertion is successfully added"
        assert SequenceAlteration.count == 2

        when: "we request for the FASTA sequence"
        String peptide2String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cds2String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide2String == expectedPeptide2String
        assert cds2String == expectedCds2String

        when: "we add Insertion of AAAAAAAAAAA in intron at position 767400"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion3String) as JSONObject)

        then: "the Insertion is successfully added"
        assert SequenceAlteration.count == 3

        when: "we request for the FASTA sequence"
        String peptide3String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cds3String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide3String == expectedPeptide3String
        assert cds3String == expectedCds3String

        when: "we add a Deletion of size 30 in exon 2 at position 767325"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion4String) as JSONObject)

        then: "the Insertion is successfully added"
        assert SequenceAlteration.count == 4

        when: "we request for the FASTA sequence"
        String peptide4String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cds4String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide4String == expectedPeptide4String
        assert cds4String == expectedCds4String

        // Test for #442
        when: "we add an Insertion of CCCGGGA in last exon at position 763175"
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertion5String) as JSONObject)

        then: "the Insertion is successfully added"
        assert SequenceAlteration.count == 5

        when: "we request for the FASTA sequence"
        String peptide5String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_PEPTIDE.value)
        String cds5String = sequenceService.getSequenceForFeature(mrna, FeatureStringEnum.TYPE_CDS.value)

        then: "we should have the expected sequences"
        assert peptide5String == expectedPeptide5String
        assert cds5String == expectedCds5String
    }

    /**
     * From #427
     *
     *
     */
    void "add sequence alteration should work on intron and all multi-exon genes"() {

        given: "a Gene GB40788-RA"
        String addTranscriptString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40788-RA\",\"children\":[{\"location\":{\"fmin\":65107,\"fmax\":65286,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":71477,\"fmax\":71651,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":75270,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }:"
        String peptideSequencePlain = "MKDRPHRPYRDHHGQAMPLEEVQGLLLPPSRTGNRGPLTIVQVGKGNGGGGDGGSDLLRLEPPSDLRPTPSPLSETSATLQSDNNDTFSGGVDPRLLLGANTGGDRNTWEGRYRVKEHRRAGKATFQGQVYNFLERPTGWKCFLYHFSV"
        String addSAS1String = "{ \"track\": \"Group1.10\", \"features\": [ { \"location\": { \"fmin\": 71549, \"fmax\": 71549, \"strand\": 1 }, \"type\": {\"name\": \"insertion\", \"cv\": { \"name\":\"sequence\" } }, \"residues\": \"AAAAAAAAAAAAAAAAAAAA\" } ], \"operation\": \"add_sequence_alteration\" }"
        String insertionStringOne = "MKDRPHRPYRDHHGQAMPLEEVQGLLLPPSRTGNRGPLTIVQVGKGNGGGGDGGSDLLRLEPPSDLIFFFFFFGQLRRPCPKRAQHCSPTTMIPLVEVSIHDYCWAQTPGATVTRGRVVTV"
        String addSASIntronString = "{ \"track\": \"Group1.10\", \"features\": [ { \"location\": { \"fmin\": 71199, \"fmax\": 71199, \"strand\": 1 }, \"type\": {\"name\": \"insertion\", \"cv\": { \"name\":\"sequence\" } }, \"residues\": \"GGGGGGGGGGGGGGGGGGGGG\" } ], \"operation\": \"add_sequence_alteration\" }"
        String insertionStringTwo = "MKDRPHRPYRDHHGQAMPLEEVQGLLLPPSRTGNRGPLTIVQVGKGNGGGGDGGSDLLRLEPPSDLIFFFFFFGQLRRPCPKRAQHCSPTTMIPLVEVSIHDYCWAQTPGATVTRGRVVTV"
        String addSADeleteString = "{ \"track\": \"Group1.10\", \"features\": [ { \"location\": { \"fmin\": 71499, \"fmax\": 71509, \"strand\": 1 }, \"type\": {\"name\": \"deletion\", \"cv\": { \"name\":\"sequence\" } } } ], \"operation\": \"add_sequence_alteration\" }"
        String deletionString = "MKDRPHRPYRDHHGQAMPLEEVQGLLLPPSRTGNRGPLTIVQVGKGNGGGGDGGSDLLRLEPPSDLIFFFFFFGQLRRPCPKRAQH"
        String addSASubstitutionString = "{ \"track\": \"Group1.10\", \"features\": [ { \"location\": { \"fmin\": 71624, \"fmax\": 71638, \"strand\": 1 }, \"type\": {\"name\": \"substitution\", \"cv\": { \"name\":\"sequence\" } }, \"residues\": \"TTTTTTTTTTTTTT\" } ], \"operation\": \"add_sequence_alteration\" }"
        String finalSubstitutionString = "MKDRPHRPYRDHHGQAMPLEEVQGLLLPPSRTGNRGPKKKKKVGKGNGGGGDGGSDLLRLEPPSDLIFFFFFFGQLRRPCPKRAQH"


        when: "we add the gene"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)
        MRNA transcript = MRNA.first()
        String residues = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_PEPTIDE.value)

        then: "we expect to see some genomic results and some residues"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3
        assert residues == peptideSequencePlain

        when: "we add an insert sequence alteration to an exon"
        requestHandlingService.addSequenceAlteration(JSON.parse(addSAS1String) as JSONObject)
        transcript = MRNA.first()
        residues = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_PEPTIDE.value)

        then: "we expect the appropriate residues"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3
        assert residues == insertionStringOne

        when: "we add an insert sequence alteration to an intron"
        requestHandlingService.addSequenceAlteration(JSON.parse(addSASIntronString) as JSONObject)
        transcript = MRNA.first()
        residues = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_PEPTIDE.value)

        then: "we expect the same residues"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3
        assert residues == insertionStringTwo

        when: "we add a delete sequence alteration"
        requestHandlingService.addSequenceAlteration(JSON.parse(addSADeleteString) as JSONObject)
        transcript = MRNA.first()
        residues = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_PEPTIDE.value)

        then: "we expect less residues"
        assert residues == deletionString

        when: "we add a substitution sequence alteration"
        requestHandlingService.addSequenceAlteration(JSON.parse(addSASubstitutionString) as JSONObject)
        transcript = MRNA.first()
        residues = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_PEPTIDE.value)

        then: "we expect different residues"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert CDS.count == 1
        assert Exon.count == 3
        assert residues == finalSubstitutionString


    }

    void "when sequence alterations are added at intron-exon boundaries"() {

        given: "when we add a transcript GB40837-RA"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":1037679,\"strand\":1,\"fmax\":1042109},\"name\":\"GB40837-RA\",\"children\":[{\"location\":{\"fmin\":1041232,\"strand\":1,\"fmax\":1042109},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1037679,\"strand\":1,\"fmax\":1037700},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1039030,\"strand\":1,\"fmax\":1039169},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1039487,\"strand\":1,\"fmax\":1039647},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1040347,\"strand\":1,\"fmax\":1040817},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1041080,\"strand\":1,\"fmax\":1042109},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1037679,\"strand\":1,\"fmax\":1041232},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String originalPeptideString = "MLGKLLLMLLIQPSEEMSKQIRMELNENVATRDKDVEVIKEWLSKQPHLPQFDDDYRLLTFLRGCKFSLEKCKKKLDMYFTMRTAIPEFFTNRDVTLPELKEITKIIQIPPLPGLTKNGRRVIVMRGINKDLPTPNVAELMKLVLMIGDVRLKEELMGVAGDVYILDASVATPSHFAKFTPALVKKFLVCVQEAYPVKLKEVHVVNISPLVDTIVNFVKPFIKEKIRNRIFMHSDLNTLYEYIPREILPAEYGGDAGPLQNIHETWIKKLEEYGPWFVEQESIKTNEALRPGKPKTHDDLFGLDGSFRQLVID"
        String originalCdnaString = "ATGTTAGGCAAGCTGCTTCTGATGTTGCTGATCCAACCGTCGGAAGAGATGTCGAAGCAGATTCGCATGGAGTTGAATGAAAATGTGGCGACACGCGATAAGGACGTGGAAGTCATCAAGGAATGGCTATCTAAGCAACCACATTTACCCCAGTTCGATGATGATTACAGATTATTGACGTTTCTTCGAGGTTGTAAATTCTCCTTGGAAAAATGTAAGAAAAAACTGGACATGTATTTCACGATGAGAACCGCAATCCCAGAGTTCTTTACTAATCGAGATGTCACTTTACCAGAACTGAAAGAAATCACTAAAATTATTCAGATTCCTCCATTGCCTGGCTTAACAAAGAATGGACGACGAGTAATTGTAATGCGTGGTATCAACAAGGATCTTCCAACCCCAAACGTGGCAGAATTGATGAAACTGGTTCTAATGATCGGCGACGTACGTTTAAAAGAAGAATTAATGGGAGTCGCAGGAGACGTGTATATTTTAGATGCTAGTGTCGCAACGCCATCTCACTTCGCCAAATTCACACCAGCTCTCGTGAAGAAATTCCTAGTATGCGTGCAAGAGGCATATCCAGTAAAATTGAAAGAGGTGCATGTAGTGAATATTAGTCCTCTGGTCGATACTATCGTCAATTTCGTGAAACCATTCATTAAAGAAAAAATTCGCAACAGAATTTTCATGCATAGTGATTTGAACACTTTATACGAATATATACCTAGGGAAATATTGCCAGCCGAATATGGCGGCGATGCTGGACCTCTACAGAATATACATGAGACCTGGATAAAGAAATTAGAAGAATATGGTCCTTGGTTCGTAGAACAGGAATCAATAAAAACGAACGAAGCACTTCGACCAGGAAAGCCAAAGACTCACGACGACTTATTCGGATTGGACGGATCGTTCCGACAGTTGGTGATCGATTAAACCTAGAAATCTTCTGTCAATGATGACAACTTGATTAATTTTCATCTCGGTGCTAATTTGAAGAAAAATCGCTGGAAAGAATGGAAGAGAGATGAAACATCCGTATCGCAATTTTTTTTTTCCGACATACAATTTGAATATATTAACCTAACCTTTTTATACTTTTTTTAAATCGAAATTAATTTTATAGATATTTTATAGATTTTTAAAAATTTATTTTAATTCCGTGGAAAAAATCTAAATTCTTAAGATAAAATAACTGCCAAAATTTCATAAAGTCAGTATTAGATAATATCCAATTATTAAATTAACTTGAATATTTTTAATACTGCTTAAAGTATAATTCCAAACTTTTTATTGGAAATGCATATTTGTATATGTTTTATATTTTTATAAGAATCTCTATTTATCTTTTATATGCTTGTTTGTTTTATATGTTTATTGATAGATTCTTAACTCTGTATTTTGTAGAATTTTGCGTAGAATAGTTATTTTTCATTCAATTATTTTTTTATTCATTATACAATTGGTTAAAAACGTTAAGAGACATAAATATACAGAATCATTTCGAGTTTATTATAACCTAATTGATCCATTTAAATTTTTTCATTCCAAAGTTCTGAATTCGTGACAATTTCACTCGAATTTTTCCGATCAATTCGAAATTGATAATATCTTTGATATACTTTTAATTGTGAATATTTATAATAAATGCAACTCTTTGTGCGATTAAATAGATGATATTTACTTGGATAAATTCTTGATTATGAATAAATTTTTATAGTTGATATTGACCATATACGAAGGAACATAACCTTAAGAATTTTAAAATAATTTTGATTGTAATTTTAATACACGTTACTTTAAAACTCTGCAT"
        String originalCdsString = "ATGTTAGGCAAGCTGCTTCTGATGTTGCTGATCCAACCGTCGGAAGAGATGTCGAAGCAGATTCGCATGGAGTTGAATGAAAATGTGGCGACACGCGATAAGGACGTGGAAGTCATCAAGGAATGGCTATCTAAGCAACCACATTTACCCCAGTTCGATGATGATTACAGATTATTGACGTTTCTTCGAGGTTGTAAATTCTCCTTGGAAAAATGTAAGAAAAAACTGGACATGTATTTCACGATGAGAACCGCAATCCCAGAGTTCTTTACTAATCGAGATGTCACTTTACCAGAACTGAAAGAAATCACTAAAATTATTCAGATTCCTCCATTGCCTGGCTTAACAAAGAATGGACGACGAGTAATTGTAATGCGTGGTATCAACAAGGATCTTCCAACCCCAAACGTGGCAGAATTGATGAAACTGGTTCTAATGATCGGCGACGTACGTTTAAAAGAAGAATTAATGGGAGTCGCAGGAGACGTGTATATTTTAGATGCTAGTGTCGCAACGCCATCTCACTTCGCCAAATTCACACCAGCTCTCGTGAAGAAATTCCTAGTATGCGTGCAAGAGGCATATCCAGTAAAATTGAAAGAGGTGCATGTAGTGAATATTAGTCCTCTGGTCGATACTATCGTCAATTTCGTGAAACCATTCATTAAAGAAAAAATTCGCAACAGAATTTTCATGCATAGTGATTTGAACACTTTATACGAATATATACCTAGGGAAATATTGCCAGCCGAATATGGCGGCGATGCTGGACCTCTACAGAATATACATGAGACCTGGATAAAGAAATTAGAAGAATATGGTCCTTGGTTCGTAGAACAGGAATCAATAAAAACGAACGAAGCACTTCGACCAGGAAAGCCAAAGACTCACGACGACTTATTCGGATTGGACGGATCGTTCCGACAGTTGGTGATCGATTAA"

        String addDeletion1String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":1039024,\"strand\":1,\"fmax\":1039031},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String deletion1PeptideString = "MSKQIRMELNENVATRDKDVEVIKEWLSKQPHLPQFDDDYRLLTFLRGCKFSLEKCKKKLDMYFTMRTAIPEFFTNRDVTLPELKEITKIIQIPPLPGLTKNGRRVIVMRGINKDLPTPNVAELMKLVLMIGDVRLKEELMGVAGDVYILDASVATPSHFAKFTPALVKKFLVCVQEAYPVKLKEVHVVNISPLVDTIVNFVKPFIKEKIRNRIFMHSDLNTLYEYIPREILPAEYGGDAGPLQNIHETWIKKLEEYGPWFVEQESIKTNEALRPGKPKTHDDLFGLDGSFRQLVID"
        String deletion1CdnaString = "ATGTTAGGCAAGCTGCTTCTGTGTTGCTGATCCAACCGTCGGAAGAGATGTCGAAGCAGATTCGCATGGAGTTGAATGAAAATGTGGCGACACGCGATAAGGACGTGGAAGTCATCAAGGAATGGCTATCTAAGCAACCACATTTACCCCAGTTCGATGATGATTACAGATTATTGACGTTTCTTCGAGGTTGTAAATTCTCCTTGGAAAAATGTAAGAAAAAACTGGACATGTATTTCACGATGAGAACCGCAATCCCAGAGTTCTTTACTAATCGAGATGTCACTTTACCAGAACTGAAAGAAATCACTAAAATTATTCAGATTCCTCCATTGCCTGGCTTAACAAAGAATGGACGACGAGTAATTGTAATGCGTGGTATCAACAAGGATCTTCCAACCCCAAACGTGGCAGAATTGATGAAACTGGTTCTAATGATCGGCGACGTACGTTTAAAAGAAGAATTAATGGGAGTCGCAGGAGACGTGTATATTTTAGATGCTAGTGTCGCAACGCCATCTCACTTCGCCAAATTCACACCAGCTCTCGTGAAGAAATTCCTAGTATGCGTGCAAGAGGCATATCCAGTAAAATTGAAAGAGGTGCATGTAGTGAATATTAGTCCTCTGGTCGATACTATCGTCAATTTCGTGAAACCATTCATTAAAGAAAAAATTCGCAACAGAATTTTCATGCATAGTGATTTGAACACTTTATACGAATATATACCTAGGGAAATATTGCCAGCCGAATATGGCGGCGATGCTGGACCTCTACAGAATATACATGAGACCTGGATAAAGAAATTAGAAGAATATGGTCCTTGGTTCGTAGAACAGGAATCAATAAAAACGAACGAAGCACTTCGACCAGGAAAGCCAAAGACTCACGACGACTTATTCGGATTGGACGGATCGTTCCGACAGTTGGTGATCGATTAAACCTAGAAATCTTCTGTCAATGATGACAACTTGATTAATTTTCATCTCGGTGCTAATTTGAAGAAAAATCGCTGGAAAGAATGGAAGAGAGATGAAACATCCGTATCGCAATTTTTTTTTTCCGACATACAATTTGAATATATTAACCTAACCTTTTTATACTTTTTTTAAATCGAAATTAATTTTATAGATATTTTATAGATTTTTAAAAATTTATTTTAATTCCGTGGAAAAAATCTAAATTCTTAAGATAAAATAACTGCCAAAATTTCATAAAGTCAGTATTAGATAATATCCAATTATTAAATTAACTTGAATATTTTTAATACTGCTTAAAGTATAATTCCAAACTTTTTATTGGAAATGCATATTTGTATATGTTTTATATTTTTATAAGAATCTCTATTTATCTTTTATATGCTTGTTTGTTTTATATGTTTATTGATAGATTCTTAACTCTGTATTTTGTAGAATTTTGCGTAGAATAGTTATTTTTCATTCAATTATTTTTTTATTCATTATACAATTGGTTAAAAACGTTAAGAGACATAAATATACAGAATCATTTCGAGTTTATTATAACCTAATTGATCCATTTAAATTTTTTCATTCCAAAGTTCTGAATTCGTGACAATTTCACTCGAATTTTTCCGATCAATTCGAAATTGATAATATCTTTGATATACTTTTAATTGTGAATATTTATAATAAATGCAACTCTTTGTGCGATTAAATAGATGATATTTACTTGGATAAATTCTTGATTATGAATAAATTTTTATAGTTGATATTGACCATATACGAAGGAACATAACCTTAAGAATTTTAAAATAATTTTGATTGTAATTTTAATACACGTTACTTTAAAACTCTGCAT"
        String deletion1CdsString = "ATGTCGAAGCAGATTCGCATGGAGTTGAATGAAAATGTGGCGACACGCGATAAGGACGTGGAAGTCATCAAGGAATGGCTATCTAAGCAACCACATTTACCCCAGTTCGATGATGATTACAGATTATTGACGTTTCTTCGAGGTTGTAAATTCTCCTTGGAAAAATGTAAGAAAAAACTGGACATGTATTTCACGATGAGAACCGCAATCCCAGAGTTCTTTACTAATCGAGATGTCACTTTACCAGAACTGAAAGAAATCACTAAAATTATTCAGATTCCTCCATTGCCTGGCTTAACAAAGAATGGACGACGAGTAATTGTAATGCGTGGTATCAACAAGGATCTTCCAACCCCAAACGTGGCAGAATTGATGAAACTGGTTCTAATGATCGGCGACGTACGTTTAAAAGAAGAATTAATGGGAGTCGCAGGAGACGTGTATATTTTAGATGCTAGTGTCGCAACGCCATCTCACTTCGCCAAATTCACACCAGCTCTCGTGAAGAAATTCCTAGTATGCGTGCAAGAGGCATATCCAGTAAAATTGAAAGAGGTGCATGTAGTGAATATTAGTCCTCTGGTCGATACTATCGTCAATTTCGTGAAACCATTCATTAAAGAAAAAATTCGCAACAGAATTTTCATGCATAGTGATTTGAACACTTTATACGAATATATACCTAGGGAAATATTGCCAGCCGAATATGGCGGCGATGCTGGACCTCTACAGAATATACATGAGACCTGGATAAAGAAATTAGAAGAATATGGTCCTTGGTTCGTAGAACAGGAATCAATAAAAACGAACGAAGCACTTCGACCAGGAAAGCCAAAGACTCACGACGACTTATTCGGATTGGACGGATCGTTCCGACAGTTGGTGATCGATTAA"

        String addDeletion2String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":1039645,\"strand\":1,\"fmax\":1039648},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String deletion2PeptideString = "MRGINKDLPTPNVAELMKLVLMIGDVRLKEELMGVAGDVYILDASVATPSHFAKFTPALVKKFLVCVQEAYPVKLKEVHVVNISPLVDTIVNFVKPFIKEKIRNRIFMHSDLNTLYEYIPREILPAEYGGDAGPLQNIHETWIKKLEEYGPWFVEQESIKTNEALRPGKPKTHDDLFGLDGSFRQLVID"
        String deletion2CdnaString = "ATGTTAGGCAAGCTGCTTCTGTGTTGCTGATCCAACCGTCGGAAGAGATGTCGAAGCAGATTCGCATGGAGTTGAATGAAAATGTGGCGACACGCGATAAGGACGTGGAAGTCATCAAGGAATGGCTATCTAAGCAACCACATTTACCCCAGTTCGATGATGATTACAGATTATTGACGTTTCTTCGAGGTTGTAAATTCTCCTTGGAAAAATGTAAGAAAAAACTGGACATGTATTTCACGATGAGAACCGCAATCCCAGAGTTCTTTACTAATCGAGATGTCACTTTACCAGAACTGAAAGAAATCACTAAAATTTCAGATTCCTCCATTGCCTGGCTTAACAAAGAATGGACGACGAGTAATTGTAATGCGTGGTATCAACAAGGATCTTCCAACCCCAAACGTGGCAGAATTGATGAAACTGGTTCTAATGATCGGCGACGTACGTTTAAAAGAAGAATTAATGGGAGTCGCAGGAGACGTGTATATTTTAGATGCTAGTGTCGCAACGCCATCTCACTTCGCCAAATTCACACCAGCTCTCGTGAAGAAATTCCTAGTATGCGTGCAAGAGGCATATCCAGTAAAATTGAAAGAGGTGCATGTAGTGAATATTAGTCCTCTGGTCGATACTATCGTCAATTTCGTGAAACCATTCATTAAAGAAAAAATTCGCAACAGAATTTTCATGCATAGTGATTTGAACACTTTATACGAATATATACCTAGGGAAATATTGCCAGCCGAATATGGCGGCGATGCTGGACCTCTACAGAATATACATGAGACCTGGATAAAGAAATTAGAAGAATATGGTCCTTGGTTCGTAGAACAGGAATCAATAAAAACGAACGAAGCACTTCGACCAGGAAAGCCAAAGACTCACGACGACTTATTCGGATTGGACGGATCGTTCCGACAGTTGGTGATCGATTAAACCTAGAAATCTTCTGTCAATGATGACAACTTGATTAATTTTCATCTCGGTGCTAATTTGAAGAAAAATCGCTGGAAAGAATGGAAGAGAGATGAAACATCCGTATCGCAATTTTTTTTTTCCGACATACAATTTGAATATATTAACCTAACCTTTTTATACTTTTTTTAAATCGAAATTAATTTTATAGATATTTTATAGATTTTTAAAAATTTATTTTAATTCCGTGGAAAAAATCTAAATTCTTAAGATAAAATAACTGCCAAAATTTCATAAAGTCAGTATTAGATAATATCCAATTATTAAATTAACTTGAATATTTTTAATACTGCTTAAAGTATAATTCCAAACTTTTTATTGGAAATGCATATTTGTATATGTTTTATATTTTTATAAGAATCTCTATTTATCTTTTATATGCTTGTTTGTTTTATATGTTTATTGATAGATTCTTAACTCTGTATTTTGTAGAATTTTGCGTAGAATAGTTATTTTTCATTCAATTATTTTTTTATTCATTATACAATTGGTTAAAAACGTTAAGAGACATAAATATACAGAATCATTTCGAGTTTATTATAACCTAATTGATCCATTTAAATTTTTTCATTCCAAAGTTCTGAATTCGTGACAATTTCACTCGAATTTTTCCGATCAATTCGAAATTGATAATATCTTTGATATACTTTTAATTGTGAATATTTATAATAAATGCAACTCTTTGTGCGATTAAATAGATGATATTTACTTGGATAAATTCTTGATTATGAATAAATTTTTATAGTTGATATTGACCATATACGAAGGAACATAACCTTAAGAATTTTAAAATAATTTTGATTGTAATTTTAATACACGTTACTTTAAAACTCTGCAT"
        String deletion2CdsString = "ATGCGTGGTATCAACAAGGATCTTCCAACCCCAAACGTGGCAGAATTGATGAAACTGGTTCTAATGATCGGCGACGTACGTTTAAAAGAAGAATTAATGGGAGTCGCAGGAGACGTGTATATTTTAGATGCTAGTGTCGCAACGCCATCTCACTTCGCCAAATTCACACCAGCTCTCGTGAAGAAATTCCTAGTATGCGTGCAAGAGGCATATCCAGTAAAATTGAAAGAGGTGCATGTAGTGAATATTAGTCCTCTGGTCGATACTATCGTCAATTTCGTGAAACCATTCATTAAAGAAAAAATTCGCAACAGAATTTTCATGCATAGTGATTTGAACACTTTATACGAATATATACCTAGGGAAATATTGCCAGCCGAATATGGCGGCGATGCTGGACCTCTACAGAATATACATGAGACCTGGATAAAGAAATTAGAAGAATATGGTCCTTGGTTCGTAGAACAGGAATCAATAAAAACGAACGAAGCACTTCGACCAGGAAAGCCAAAGACTCACGACGACTTATTCGGATTGGACGGATCGTTCCGACAGTTGGTGATCGATTAA"

        String addSubstitution3String = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"residues\":\"CCCC\",\"location\":{\"fmin\":1040815,\"strand\":1,\"fmax\":1040819},\"type\":{\"name\":\"substitution\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String substitution3PeptideString = "MRGINKDLPTPNVAELMKLVLMIGDVRLKEELMGVAGDVYILDASVATPSHFAKFTPALVKKFLVCVQEAYPVKLKEVHVVNISPLVDTIVNFVKPFIKEKIRNRIFMHSDLNTLYEYIPREILPAEYGGDAGPLQNIHQTWIKKLEEYGPWFVEQESIKTNEALRPGKPKTHDDLFGLDGSFRQLVID"
        String substitution3CdnaString = "ATGTTAGGCAAGCTGCTTCTGTGTTGCTGATCCAACCGTCGGAAGAGATGTCGAAGCAGATTCGCATGGAGTTGAATGAAAATGTGGCGACACGCGATAAGGACGTGGAAGTCATCAAGGAATGGCTATCTAAGCAACCACATTTACCCCAGTTCGATGATGATTACAGATTATTGACGTTTCTTCGAGGTTGTAAATTCTCCTTGGAAAAATGTAAGAAAAAACTGGACATGTATTTCACGATGAGAACCGCAATCCCAGAGTTCTTTACTAATCGAGATGTCACTTTACCAGAACTGAAAGAAATCACTAAAATTTCAGATTCCTCCATTGCCTGGCTTAACAAAGAATGGACGACGAGTAATTGTAATGCGTGGTATCAACAAGGATCTTCCAACCCCAAACGTGGCAGAATTGATGAAACTGGTTCTAATGATCGGCGACGTACGTTTAAAAGAAGAATTAATGGGAGTCGCAGGAGACGTGTATATTTTAGATGCTAGTGTCGCAACGCCATCTCACTTCGCCAAATTCACACCAGCTCTCGTGAAGAAATTCCTAGTATGCGTGCAAGAGGCATATCCAGTAAAATTGAAAGAGGTGCATGTAGTGAATATTAGTCCTCTGGTCGATACTATCGTCAATTTCGTGAAACCATTCATTAAAGAAAAAATTCGCAACAGAATTTTCATGCATAGTGATTTGAACACTTTATACGAATATATACCTAGGGAAATATTGCCAGCCGAATATGGCGGCGATGCTGGACCTCTACAGAATATACACCAGACCTGGATAAAGAAATTAGAAGAATATGGTCCTTGGTTCGTAGAACAGGAATCAATAAAAACGAACGAAGCACTTCGACCAGGAAAGCCAAAGACTCACGACGACTTATTCGGATTGGACGGATCGTTCCGACAGTTGGTGATCGATTAAACCTAGAAATCTTCTGTCAATGATGACAACTTGATTAATTTTCATCTCGGTGCTAATTTGAAGAAAAATCGCTGGAAAGAATGGAAGAGAGATGAAACATCCGTATCGCAATTTTTTTTTTCCGACATACAATTTGAATATATTAACCTAACCTTTTTATACTTTTTTTAAATCGAAATTAATTTTATAGATATTTTATAGATTTTTAAAAATTTATTTTAATTCCGTGGAAAAAATCTAAATTCTTAAGATAAAATAACTGCCAAAATTTCATAAAGTCAGTATTAGATAATATCCAATTATTAAATTAACTTGAATATTTTTAATACTGCTTAAAGTATAATTCCAAACTTTTTATTGGAAATGCATATTTGTATATGTTTTATATTTTTATAAGAATCTCTATTTATCTTTTATATGCTTGTTTGTTTTATATGTTTATTGATAGATTCTTAACTCTGTATTTTGTAGAATTTTGCGTAGAATAGTTATTTTTCATTCAATTATTTTTTTATTCATTATACAATTGGTTAAAAACGTTAAGAGACATAAATATACAGAATCATTTCGAGTTTATTATAACCTAATTGATCCATTTAAATTTTTTCATTCCAAAGTTCTGAATTCGTGACAATTTCACTCGAATTTTTCCGATCAATTCGAAATTGATAATATCTTTGATATACTTTTAATTGTGAATATTTATAATAAATGCAACTCTTTGTGCGATTAAATAGATGATATTTACTTGGATAAATTCTTGATTATGAATAAATTTTTATAGTTGATATTGACCATATACGAAGGAACATAACCTTAAGAATTTTAAAATAATTTTGATTGTAATTTTAATACACGTTACTTTAAAACTCTGCAT"
        String substitution3CdsString = "ATGCGTGGTATCAACAAGGATCTTCCAACCCCAAACGTGGCAGAATTGATGAAACTGGTTCTAATGATCGGCGACGTACGTTTAAAAGAAGAATTAATGGGAGTCGCAGGAGACGTGTATATTTTAGATGCTAGTGTCGCAACGCCATCTCACTTCGCCAAATTCACACCAGCTCTCGTGAAGAAATTCCTAGTATGCGTGCAAGAGGCATATCCAGTAAAATTGAAAGAGGTGCATGTAGTGAATATTAGTCCTCTGGTCGATACTATCGTCAATTTCGTGAAACCATTCATTAAAGAAAAAATTCGCAACAGAATTTTCATGCATAGTGATTTGAACACTTTATACGAATATATACCTAGGGAAATATTGCCAGCCGAATATGGCGGCGATGCTGGACCTCTACAGAATATACACCAGACCTGGATAAAGAAATTAGAAGAATATGGTCCTTGGTTCGTAGAACAGGAATCAATAAAAACGAACGAAGCACTTCGACCAGGAAAGCCAAAGACTCACGACGACTTATTCGGATTGGACGGATCGTTCCGACAGTTGGTGATCGATTAA"

        when: "we add the transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we should have 1 MRNA and its proper sequence"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 5

        when: "we add a deletion at an intron-exon boundary at position 1039025"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion1String) as JSONObject)

        then: "we should have the deletion added and the proper sequences"
        assert SequenceAlteration.count == 1

        Transcript transcript = Transcript.findByName("GB40837-RA-00001")
        String deletion1PeptideSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_PEPTIDE.value)
        String deletion1CdnaSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_CDNA.value)
        String deletion1CdsSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_CDS.value)

        assert deletion1PeptideSequence == deletion1PeptideString
        assert deletion1CdnaSequence == deletion1CdnaString
        assert deletion1CdsSequence == deletion1CdsString

        when: "we add a deletion at an exon-intron boundary at position 1039646"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletion2String) as JSONObject)

        then: "we should have the deletion added and the proper sequences"
        assert SequenceAlteration.count == 2

        String deletion2PeptideSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_PEPTIDE.value)
        String deletion2CdnaSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_CDNA.value)
        String deletion2CdsSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_CDS.value)

        assert deletion2PeptideSequence == deletion2PeptideString
        assert deletion2CdnaSequence == deletion2CdnaString
        assert deletion2CdsSequence == deletion2CdsString

        when: "we add a substitution of at intron-exon bounary at position 1040816"
        requestHandlingService.addSequenceAlteration(JSON.parse(addSubstitution3String) as JSONObject)

        then: "we should have the substitution added and the proper sequences"
        assert SequenceAlteration.count == 3

        String substitution3PeptideSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_PEPTIDE.value)
        String substitution3CdnaSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_CDNA.value)
        String substitution3CdsSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_CDS.value)

        assert substitution3PeptideSequence == substitution3PeptideString
        assert substitution3CdnaSequence == substitution3CdnaString
        assert substitution3CdsSequence == substitution3CdsString

    }

    void "when we add a transcript and a sequence alteration, we should be able to get the proper sequence of individual exons"() {
        given: "The GB40843-RA transcript"
        String addTranscriptString = "{\"operation\":\"add_transcript\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":1092561,\"strand\":1,\"fmax\":1095202},\"name\":\"GB40843-RA\",\"children\":[{\"location\":{\"fmin\":1092561,\"strand\":1,\"fmax\":1092941},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1094825,\"strand\":1,\"fmax\":1095202},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1092561,\"strand\":1,\"fmax\":1093530},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1093608,\"strand\":1,\"fmax\":1094085},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1094159,\"strand\":1,\"fmax\":1094487},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1094623,\"strand\":1,\"fmax\":1095202},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1092941,\"strand\":1,\"fmax\":1094825},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String addDeletionString = "{\"operation\":\"add_sequence_alteration\",\"username\":\"deepak.unni3@gmail.com\",\"features\":[{\"location\":{\"fmin\":1094155,\"strand\":1,\"fmax\":1094167},\"type\":{\"name\":\"deletion\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"

        String expectedTranscriptPeptideSequence = "MTLDNSWHIIKTAAAENKHELVLSGPAISELIQKRGFDKSLFNLEHLNYLNITQTCLHEVSEEIEKLQNLTTLVLHSNEISKIPSSIGKLEKLKVLDCSRNKLTSLPDEIGKLPQLTTMNFSSNFLKSLPTQIDNVKLTVLDLSNNQFEDFPDVCYTELVHLSEIYVMGNQIKEIPTIINQLPSLKIINVANNRISVIPGEIGDCNKLKELQLKGNPLSDKRLLKLVDQCHNKQILEYVKLHCPKQDGSVNSNSKSKKGKKVQKLSESENNANVMDNLTHKMKILKMANDTPVIKVTKYVKNIRPYIAACIVRNMSFTEDSFKKFIQLQTKLHDGICEKRNAATIATHDLKLITTDIHSKTTK"
        String expectedTranscriptCdnaSequence = "AATTAATAGTATTCATTTTTTATCTTTTGATCAAGCTCATATTTATAATTAAATATCAAAATATAATTCATAGATAACATTAAAATTTGTTTATAGTTTAATTCACTAAAATACGTTTATCATTATTGTTTAATCAAATTATATGTGACGGAAACTATCCGTTATATATCTCTACAAAACTTTTATTAGATTTAGGTTATTTGATGTCTCGTCTTAAATTCATTTATTCTTTTCAATCGCAATTTTTAAATTGCATATGTACGTAGATGTCGTTATAATCGTATAACGCATGTTTCAAATTGATCAACCCGGCAAGTAACCTTGAAACTACGTAAATAACATTACCCTTTTATTAAAAATTCTTTTAATACTTAATAATAATGACGCTCGATAATTCATGGCATATAATCAAAACTGCAGCTGCAGAGAATAAACATGAATTAGTGTTGTCAGGTCCGGCAATCTCTGAATTGATTCAAAAAAGAGGCTTCGACAAGTCTTTATTTAACCTAGAGCATTTGAATTATTTGAATATTACTCAGACATGTTTACACGAAGTGTCAGAAGAAATTGAAAAATTGCAAAATCTAACAACATTGGTATTGCATTCGAATGAGATTTCGAAGATACCTAGCTCAATCGGGAAATTAGAAAAACTAAAGGTTCTCGATTGCTCTAGGAACAAGTTGACGTCTTTACCAGATGAAATCGGTAAACTTCCACAATTAACAACCATGAACTTCAGTTCAAATTTTTTGAAATCATTACCTACGCAAATTGACAATGTTAAGTTGACTGTTTTGGATCTTTCGAACAATCAGTTTGAAGATTTTCCTGATGTGTGCTATACAGAATTGGTTCATCTTTCTGAAATCTATGTCATGGGAAATCAAATAAAAGAAATTCCTACAATTATAAATCAGCTTCCATCTTTAAAAATAATAAATGTAGCAAACAATAGAATTTCAGTTATTCCAGGTGAGATTGGTGATTGTAATAAACTCAAAGAACTTCAATTAAAAGGAAATCCATTATCAGACAAAAGATTATTAAAATTAGTTGATCAATGTCATAATAAACAAATATTAGAATATGTAAAATTGCATTGTCCTAAACAAGATGGTTCTGTCAATTCAAATTCAAAATCAAAAAAAGGAAAAAAGGTACAAAAATTATCAGAAAGTGAAAATAATGCTAATGTAATGGATAATTTGACACACAAAATGAAAATATTAAAAATGGCAAATGATACACCAGTAATTAAGGTTACAAAATATGTGAAAAATATCAGACCTTACATTGCAGCTTGTATTGTTAGAAATATGAGCTTCACAGAAGATAGTTTTAAAAAATTTATTCAACTTCAAACTAAATTACATGATGGCATATGTGAAAAAAGAAATGCAGCTACTATTGCTACACATGACTTAAAATTGATTACCACTGACATACACAGCAAAACCACCAAATGAATTAGAAATCAAGCCATTAATGCGCAATAAAGTTTATACTGGTTCTGAACTATTCAAACAGTTACAAACTGAAGCTGATAATTTAAGGAAAGAAAAAAAACGCAATGTATATTCTGGCATTCATAAATATCTTTATCTATTAGAAGGTAAACCTTACTTTCCTTGTTTGTTAGATGCTTCAGAACAAGTTATATCATTTCCACCTATAACGAATAGTGATATTACAAAAATGTCAATAAATACCCAAGCAATATTAATAGAAGTAACCAGTGCTTCATCCTATCAAATTTGCAGGAATGTATTAGATCAATTTTTAAAGGAACTAGTTACTTTTGGTTTAGGATGTGTCTCAGAACAAGAAAATGCTTCAAATTATCATAAATTAATCATAGAACAAGTAAAAGTGGTAGATATGGAAGGTAATATGAAATTAGTATATCCTTCAAGAGCAGATTTAAATTTTGCAGAAAATTTTATAACAGTATTACGCGAGTAATAAATTACTGTAAGTAATTAAATTGTTCTTTAATCTTGATCTAATCTGCATTTTTCTTTCTTAATCTTTTTAACTATTATTTTTTTGATTGATAAGTTGTAAATCTAAATTATTTTCATATTATTACTTTTTCATAAATAACACATTATTTTCATATGCCAAATTGTAATTTTTTATTTGTTACTCTGTGAAAATCTTAAGATTAGTATGCAATGTATATAATATTTGCATATTTTATACAGAATCTTTTTGGTATGTATCAATATAATTATATTTTAATCATAATATTTTTATACTGAGATTTGAATCTATGTAAAAATAATAACAATAAGTGTTTTATGTTATGAAAATCAAAAACTGGAAAATAAATATTAAAA"
        String expectedTranscriptCdsSequence = "ATGACGCTCGATAATTCATGGCATATAATCAAAACTGCAGCTGCAGAGAATAAACATGAATTAGTGTTGTCAGGTCCGGCAATCTCTGAATTGATTCAAAAAAGAGGCTTCGACAAGTCTTTATTTAACCTAGAGCATTTGAATTATTTGAATATTACTCAGACATGTTTACACGAAGTGTCAGAAGAAATTGAAAAATTGCAAAATCTAACAACATTGGTATTGCATTCGAATGAGATTTCGAAGATACCTAGCTCAATCGGGAAATTAGAAAAACTAAAGGTTCTCGATTGCTCTAGGAACAAGTTGACGTCTTTACCAGATGAAATCGGTAAACTTCCACAATTAACAACCATGAACTTCAGTTCAAATTTTTTGAAATCATTACCTACGCAAATTGACAATGTTAAGTTGACTGTTTTGGATCTTTCGAACAATCAGTTTGAAGATTTTCCTGATGTGTGCTATACAGAATTGGTTCATCTTTCTGAAATCTATGTCATGGGAAATCAAATAAAAGAAATTCCTACAATTATAAATCAGCTTCCATCTTTAAAAATAATAAATGTAGCAAACAATAGAATTTCAGTTATTCCAGGTGAGATTGGTGATTGTAATAAACTCAAAGAACTTCAATTAAAAGGAAATCCATTATCAGACAAAAGATTATTAAAATTAGTTGATCAATGTCATAATAAACAAATATTAGAATATGTAAAATTGCATTGTCCTAAACAAGATGGTTCTGTCAATTCAAATTCAAAATCAAAAAAAGGAAAAAAGGTACAAAAATTATCAGAAAGTGAAAATAATGCTAATGTAATGGATAATTTGACACACAAAATGAAAATATTAAAAATGGCAAATGATACACCAGTAATTAAGGTTACAAAATATGTGAAAAATATCAGACCTTACATTGCAGCTTGTATTGTTAGAAATATGAGCTTCACAGAAGATAGTTTTAAAAAATTTATTCAACTTCAAACTAAATTACATGATGGCATATGTGAAAAAAGAAATGCAGCTACTATTGCTACACATGACTTAAAATTGATTACCACTGACATACACAGCAAAACCACCAAATGA"
        String expectedTranscriptGenomicSequence = "AATTAATAGTATTCATTTTTTATCTTTTGATCAAGCTCATATTTATAATTAAATATCAAAATATAATTCATAGATAACATTAAAATTTGTTTATAGTTTAATTCACTAAAATACGTTTATCATTATTGTTTAATCAAATTATATGTGACGGAAACTATCCGTTATATATCTCTACAAAACTTTTATTAGATTTAGGTTATTTGATGTCTCGTCTTAAATTCATTTATTCTTTTCAATCGCAATTTTTAAATTGCATATGTACGTAGATGTCGTTATAATCGTATAACGCATGTTTCAAATTGATCAACCCGGCAAGTAACCTTGAAACTACGTAAATAACATTACCCTTTTATTAAAAATTCTTTTAATACTTAATAATAATGACGCTCGATAATTCATGGCATATAATCAAAACTGCAGCTGCAGAGAATAAACATGAATTAGTGTTGTCAGGTCCGGCAATCTCTGAATTGATTCAAAAAAGAGGCTTCGACAAGTCTTTATTTAACCTAGAGCATTTGAATTATTTGAATATTACTCAGACATGTTTACACGAAGTGTCAGAAGAAATTGAAAAATTGCAAAATCTAACAACATTGGTATTGCATTCGAATGAGATTTCGAAGATACCTAGCTCAATCGGGAAATTAGAAAAACTAAAGGTTCTCGATTGCTCTAGGAACAAGTTGACGTCTTTACCAGATGAAATCGGTAAACTTCCACAATTAACAACCATGAACTTCAGTTCAAATTTTTTGAAATCATTACCTACGCAAATTGACAATGTTAAGTTGACTGTTTTGGATCTTTCGAACAATCAGTTTGAAGATTTTCCTGATGTGTGCTATACAGAATTGGTTCATCTTTCTGAAATCTATGTCATGGGAAATCAAATAAAAGAAATTCCTACAATTATAAATCAGCTTCCATCTTTAAAAATAATAAATGTAGCAAACAATAGAATTTCAGGTAATTTCTTGATAAATGATGCCAAATATATCCAATGTATTTTATATAATTGATTTGATTTTTTTTTCTTTTATATAGTTATTCCAGGTGAGATTGGTGATTGTAATAAACTCAAAGAACTTCAATTAAAAGGAAATCCATTATCAGACAAAAGATTATTAAAATTAGTTGATCAATGTCATAATAAACAAATATTAGAATATGTAAAATTGCATTGTCCTAAACAAGATGGTTCTGTCAATTCAAATTCAAAATCAAAAAAAGGAAAAAAGGTACAAAAATTATCAGAAAGTGAAAATAATGCTAATGTAATGGATAATTTGACACACAAAATGAAAATATTAAAAATGGCAAATGATACACCAGTAATTAAGGTTACAAAATATGTGAAAAATATCAGACCTTACATTGCAGCTTGTATTGTTAGAAATATGAGCTTCACAGAAGATAGTTTTAAAAAATTTATTCAACTTCAAACTAAATTACATGATGGCATATGTGAAAAAAGAAATGCAGCTACTATTGCTACACATGACTTAAAATTGATTACCACTGGTAAATATGAAAAACAATAAAAATAATTCAGATATATAAAAATACATTTCATTAATGTCATGTTTGATATACATACACAGCAAAACCACCAAATGAATTAGAAATCAAGCCATTAATGCGCAATAAAGTTTATACTGGTTCTGAACTATTCAAACAGTTACAAACTGAAGCTGATAATTTAAGGAAAGAAAAAAAACGCAATGTATATTCTGGCATTCATAAATATCTTTATCTATTAGAAGGTAAACCTTACTTTCCTTGTTTGTTAGATGCTTCAGAACAAGTTATATCATTTCCACCTATAACGAATAGTGATATTACAAAAATGTCAATAAATACCCAAGCAATATTAATAGAAGTAACCAGTGCTTCATCCTATCAAATTTGCAGGTAATATTAGAATTCATTAATTATAATTAATATTCATTACTAAATTAAATAGTTTTATTAAAAATAATATTAAAATTAATATAAAAAAATTGTTCTGATTATACTATAAAGTAAATTTTTATTTTTTTTTTATTAGGAATGTATTAGATCAATTTTTAAAGGAACTAGTTACTTTTGGTTTAGGATGTGTCTCAGAACAAGAAAATGCTTCAAATTATCATAAATTAATCATAGAACAAGTAAAAGTGGTAGATATGGAAGGTAATATGAAATTAGTATATCCTTCAAGAGCAGATTTAAATTTTGCAGAAAATTTTATAACAGTATTACGCGAGTAATAAATTACTGTAAGTAATTAAATTGTTCTTTAATCTTGATCTAATCTGCATTTTTCTTTCTTAATCTTTTTAACTATTATTTTTTTGATTGATAAGTTGTAAATCTAAATTATTTTCATATTATTACTTTTTCATAAATAACACATTATTTTCATATGCCAAATTGTAATTTTTTATTTGTTACTCTGTGAAAATCTTAAGATTAGTATGCAATGTATATAATATTTGCATATTTTATACAGAATCTTTTTGGTATGTATCAATATAATTATATTTTAATCATAATATTTTTATACTGAGATTTGAATCTATGTAAAAATAATAACAATAAGTGTTTTATGTTATGAAAATCAAAAACTGGAAAATAAATATTAAAA"

        String expectedExonOnePeptideSequence = "MTLDNSWHIIKTAAAENKHELVLSGPAISELIQKRGFDKSLFNLEHLNYLNITQTCLHEVSEEIEKLQNLTTLVLHSNEISKIPSSIGKLEKLKVLDCSRNKLTSLPDEIGKLPQLTTMNFSSNFLKSLPTQIDNVKLTVLDLSNNQFEDFPDVCYTELVHLSEIYVMGNQIKEIPTIINQLPSLKIINVANNRIS"
        String expectedExonOneCdnaSequence = "AATTAATAGTATTCATTTTTTATCTTTTGATCAAGCTCATATTTATAATTAAATATCAAAATATAATTCATAGATAACATTAAAATTTGTTTATAGTTTAATTCACTAAAATACGTTTATCATTATTGTTTAATCAAATTATATGTGACGGAAACTATCCGTTATATATCTCTACAAAACTTTTATTAGATTTAGGTTATTTGATGTCTCGTCTTAAATTCATTTATTCTTTTCAATCGCAATTTTTAAATTGCATATGTACGTAGATGTCGTTATAATCGTATAACGCATGTTTCAAATTGATCAACCCGGCAAGTAACCTTGAAACTACGTAAATAACATTACCCTTTTATTAAAAATTCTTTTAATACTTAATAATAATGACGCTCGATAATTCATGGCATATAATCAAAACTGCAGCTGCAGAGAATAAACATGAATTAGTGTTGTCAGGTCCGGCAATCTCTGAATTGATTCAAAAAAGAGGCTTCGACAAGTCTTTATTTAACCTAGAGCATTTGAATTATTTGAATATTACTCAGACATGTTTACACGAAGTGTCAGAAGAAATTGAAAAATTGCAAAATCTAACAACATTGGTATTGCATTCGAATGAGATTTCGAAGATACCTAGCTCAATCGGGAAATTAGAAAAACTAAAGGTTCTCGATTGCTCTAGGAACAAGTTGACGTCTTTACCAGATGAAATCGGTAAACTTCCACAATTAACAACCATGAACTTCAGTTCAAATTTTTTGAAATCATTACCTACGCAAATTGACAATGTTAAGTTGACTGTTTTGGATCTTTCGAACAATCAGTTTGAAGATTTTCCTGATGTGTGCTATACAGAATTGGTTCATCTTTCTGAAATCTATGTCATGGGAAATCAAATAAAAGAAATTCCTACAATTATAAATCAGCTTCCATCTTTAAAAATAATAAATGTAGCAAACAATAGAATTTCAG"
        String expectedExonOneCdsSequence = "ATGACGCTCGATAATTCATGGCATATAATCAAAACTGCAGCTGCAGAGAATAAACATGAATTAGTGTTGTCAGGTCCGGCAATCTCTGAATTGATTCAAAAAAGAGGCTTCGACAAGTCTTTATTTAACCTAGAGCATTTGAATTATTTGAATATTACTCAGACATGTTTACACGAAGTGTCAGAAGAAATTGAAAAATTGCAAAATCTAACAACATTGGTATTGCATTCGAATGAGATTTCGAAGATACCTAGCTCAATCGGGAAATTAGAAAAACTAAAGGTTCTCGATTGCTCTAGGAACAAGTTGACGTCTTTACCAGATGAAATCGGTAAACTTCCACAATTAACAACCATGAACTTCAGTTCAAATTTTTTGAAATCATTACCTACGCAAATTGACAATGTTAAGTTGACTGTTTTGGATCTTTCGAACAATCAGTTTGAAGATTTTCCTGATGTGTGCTATACAGAATTGGTTCATCTTTCTGAAATCTATGTCATGGGAAATCAAATAAAAGAAATTCCTACAATTATAAATCAGCTTCCATCTTTAAAAATAATAAATGTAGCAAACAATAGAATTTCAG"
        String expectedExonOneGenomicSequence = "AATTAATAGTATTCATTTTTTATCTTTTGATCAAGCTCATATTTATAATTAAATATCAAAATATAATTCATAGATAACATTAAAATTTGTTTATAGTTTAATTCACTAAAATACGTTTATCATTATTGTTTAATCAAATTATATGTGACGGAAACTATCCGTTATATATCTCTACAAAACTTTTATTAGATTTAGGTTATTTGATGTCTCGTCTTAAATTCATTTATTCTTTTCAATCGCAATTTTTAAATTGCATATGTACGTAGATGTCGTTATAATCGTATAACGCATGTTTCAAATTGATCAACCCGGCAAGTAACCTTGAAACTACGTAAATAACATTACCCTTTTATTAAAAATTCTTTTAATACTTAATAATAATGACGCTCGATAATTCATGGCATATAATCAAAACTGCAGCTGCAGAGAATAAACATGAATTAGTGTTGTCAGGTCCGGCAATCTCTGAATTGATTCAAAAAAGAGGCTTCGACAAGTCTTTATTTAACCTAGAGCATTTGAATTATTTGAATATTACTCAGACATGTTTACACGAAGTGTCAGAAGAAATTGAAAAATTGCAAAATCTAACAACATTGGTATTGCATTCGAATGAGATTTCGAAGATACCTAGCTCAATCGGGAAATTAGAAAAACTAAAGGTTCTCGATTGCTCTAGGAACAAGTTGACGTCTTTACCAGATGAAATCGGTAAACTTCCACAATTAACAACCATGAACTTCAGTTCAAATTTTTTGAAATCATTACCTACGCAAATTGACAATGTTAAGTTGACTGTTTTGGATCTTTCGAACAATCAGTTTGAAGATTTTCCTGATGTGTGCTATACAGAATTGGTTCATCTTTCTGAAATCTATGTCATGGGAAATCAAATAAAAGAAATTCCTACAATTATAAATCAGCTTCCATCTTTAAAAATAATAAATGTAGCAAACAATAGAATTTCAG"

        String expectedExonTwoPeptideSequence = "IPGEIGDCNKLKELQLKGNPLSDKRLLKLVDQCHNKQILEYVKLHCPKQDGSVNSNSKSKKGKKVQKLSESENNANVMDNLTHKMKILKMANDTPVIKVTKYVKNIRPYIAACIVRNMSFTEDSFKKFIQLQTKLHDGICEKRNAATIATHDLKLITT"
        String expectedExonTwoCdnaSequence = "TTATTCCAGGTGAGATTGGTGATTGTAATAAACTCAAAGAACTTCAATTAAAAGGAAATCCATTATCAGACAAAAGATTATTAAAATTAGTTGATCAATGTCATAATAAACAAATATTAGAATATGTAAAATTGCATTGTCCTAAACAAGATGGTTCTGTCAATTCAAATTCAAAATCAAAAAAAGGAAAAAAGGTACAAAAATTATCAGAAAGTGAAAATAATGCTAATGTAATGGATAATTTGACACACAAAATGAAAATATTAAAAATGGCAAATGATACACCAGTAATTAAGGTTACAAAATATGTGAAAAATATCAGACCTTACATTGCAGCTTGTATTGTTAGAAATATGAGCTTCACAGAAGATAGTTTTAAAAAATTTATTCAACTTCAAACTAAATTACATGATGGCATATGTGAAAAAAGAAATGCAGCTACTATTGCTACACATGACTTAAAATTGATTACCACTG"
        String expectedExonTwoCdsSequence = "TTATTCCAGGTGAGATTGGTGATTGTAATAAACTCAAAGAACTTCAATTAAAAGGAAATCCATTATCAGACAAAAGATTATTAAAATTAGTTGATCAATGTCATAATAAACAAATATTAGAATATGTAAAATTGCATTGTCCTAAACAAGATGGTTCTGTCAATTCAAATTCAAAATCAAAAAAAGGAAAAAAGGTACAAAAATTATCAGAAAGTGAAAATAATGCTAATGTAATGGATAATTTGACACACAAAATGAAAATATTAAAAATGGCAAATGATACACCAGTAATTAAGGTTACAAAATATGTGAAAAATATCAGACCTTACATTGCAGCTTGTATTGTTAGAAATATGAGCTTCACAGAAGATAGTTTTAAAAAATTTATTCAACTTCAAACTAAATTACATGATGGCATATGTGAAAAAAGAAATGCAGCTACTATTGCTACACATGACTTAAAATTGATTACCACTG"
        String expectedExonTwoGenomicSequence = "TTATTCCAGGTGAGATTGGTGATTGTAATAAACTCAAAGAACTTCAATTAAAAGGAAATCCATTATCAGACAAAAGATTATTAAAATTAGTTGATCAATGTCATAATAAACAAATATTAGAATATGTAAAATTGCATTGTCCTAAACAAGATGGTTCTGTCAATTCAAATTCAAAATCAAAAAAAGGAAAAAAGGTACAAAAATTATCAGAAAGTGAAAATAATGCTAATGTAATGGATAATTTGACACACAAAATGAAAATATTAAAAATGGCAAATGATACACCAGTAATTAAGGTTACAAAATATGTGAAAAATATCAGACCTTACATTGCAGCTTGTATTGTTAGAAATATGAGCTTCACAGAAGATAGTTTTAAAAAATTTATTCAACTTCAAACTAAATTACATGATGGCATATGTGAAAAAAGAAATGCAGCTACTATTGCTACACATGACTTAAAATTGATTACCACTG"

        String expectedExonThreePeptideSequence = "IHSKTTK"
        String expectedExonThreeCdnaSequence = "ACATACACAGCAAAACCACCAAATGAATTAGAAATCAAGCCATTAATGCGCAATAAAGTTTATACTGGTTCTGAACTATTCAAACAGTTACAAACTGAAGCTGATAATTTAAGGAAAGAAAAAAAACGCAATGTATATTCTGGCATTCATAAATATCTTTATCTATTAGAAGGTAAACCTTACTTTCCTTGTTTGTTAGATGCTTCAGAACAAGTTATATCATTTCCACCTATAACGAATAGTGATATTACAAAAATGTCAATAAATACCCAAGCAATATTAATAGAAGTAACCAGTGCTTCATCCTATCAAATTTGCAG"
        String expectedExonThreeCdsSequence = "ACATACACAGCAAAACCACCAAATGA"
        String expectedExonThreeGenomicSequence = "ACATACACAGCAAAACCACCAAATGAATTAGAAATCAAGCCATTAATGCGCAATAAAGTTTATACTGGTTCTGAACTATTCAAACAGTTACAAACTGAAGCTGATAATTTAAGGAAAGAAAAAAAACGCAATGTATATTCTGGCATTCATAAATATCTTTATCTATTAGAAGGTAAACCTTACTTTCCTTGTTTGTTAGATGCTTCAGAACAAGTTATATCATTTCCACCTATAACGAATAGTGATATTACAAAAATGTCAATAAATACCCAAGCAATATTAATAGAAGTAACCAGTGCTTCATCCTATCAAATTTGCAG"

        String expectedExonFourPeptideSequence = ""
        String expectedExonFourCdnaSequence = "GAATGTATTAGATCAATTTTTAAAGGAACTAGTTACTTTTGGTTTAGGATGTGTCTCAGAACAAGAAAATGCTTCAAATTATCATAAATTAATCATAGAACAAGTAAAAGTGGTAGATATGGAAGGTAATATGAAATTAGTATATCCTTCAAGAGCAGATTTAAATTTTGCAGAAAATTTTATAACAGTATTACGCGAGTAATAAATTACTGTAAGTAATTAAATTGTTCTTTAATCTTGATCTAATCTGCATTTTTCTTTCTTAATCTTTTTAACTATTATTTTTTTGATTGATAAGTTGTAAATCTAAATTATTTTCATATTATTACTTTTTCATAAATAACACATTATTTTCATATGCCAAATTGTAATTTTTTATTTGTTACTCTGTGAAAATCTTAAGATTAGTATGCAATGTATATAATATTTGCATATTTTATACAGAATCTTTTTGGTATGTATCAATATAATTATATTTTAATCATAATATTTTTATACTGAGATTTGAATCTATGTAAAAATAATAACAATAAGTGTTTTATGTTATGAAAATCAAAAACTGGAAAATAAATATTAAAA"
        String expectedExonFourCdsSequence = ""
        String expectedExonFourGenomicSequence = "GAATGTATTAGATCAATTTTTAAAGGAACTAGTTACTTTTGGTTTAGGATGTGTCTCAGAACAAGAAAATGCTTCAAATTATCATAAATTAATCATAGAACAAGTAAAAGTGGTAGATATGGAAGGTAATATGAAATTAGTATATCCTTCAAGAGCAGATTTAAATTTTGCAGAAAATTTTATAACAGTATTACGCGAGTAATAAATTACTGTAAGTAATTAAATTGTTCTTTAATCTTGATCTAATCTGCATTTTTCTTTCTTAATCTTTTTAACTATTATTTTTTTGATTGATAAGTTGTAAATCTAAATTATTTTCATATTATTACTTTTTCATAAATAACACATTATTTTCATATGCCAAATTGTAATTTTTTATTTGTTACTCTGTGAAAATCTTAAGATTAGTATGCAATGTATATAATATTTGCATATTTTATACAGAATCTTTTTGGTATGTATCAATATAATTATATTTTAATCATAATATTTTTATACTGAGATTTGAATCTATGTAAAAATAATAACAATAAGTGTTTTATGTTATGAAAATCAAAAACTGGAAAATAAATATTAAAA"

        when: "we add the transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)

        then: "we see the added transcript"
        assert MRNA.count == 1
        assert Exon.count == 4
        MRNA transcript = MRNA.findByName("GB40843-RA-00001")
        List<Exon> exonList = exonService.getSortedExons(transcript, true)

        when: "we add a deletion at position 1094156"
        requestHandlingService.addSequenceAlteration(JSON.parse(addDeletionString) as JSONObject)

        then: "we see the alteration"
        SequenceAlteration.count == 1

        when: "we get sequence of the whole transcript"
        String getTranscriptPeptideSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_PEPTIDE.value)
        String getTranscriptCdnaSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_CDNA.value)
        String getTranscriptCdsSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_CDS.value)
        String getTranscriptGenomicSequence = sequenceService.getSequenceForFeature(transcript, FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we get the expected sequences"
        assert getTranscriptPeptideSequence == expectedTranscriptPeptideSequence
        assert getTranscriptCdnaSequence == expectedTranscriptCdnaSequence
        assert getTranscriptCdsSequence == expectedTranscriptCdsSequence
        assert getTranscriptGenomicSequence == expectedTranscriptGenomicSequence

        when: "we get sequence of exon 1"
        String getExonOnePeptideSequence = sequenceService.getSequenceForFeature(exonList[0], FeatureStringEnum.TYPE_PEPTIDE.value)
        String getExonOneCdnaSequence = sequenceService.getSequenceForFeature(exonList[0], FeatureStringEnum.TYPE_CDNA.value)
        String getExonOneCdsSequence = sequenceService.getSequenceForFeature(exonList[0], FeatureStringEnum.TYPE_CDS.value)
        String getExonOneGenomicSequence = sequenceService.getSequenceForFeature(exonList[0], FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we get the expected sequences"
        assert getExonOnePeptideSequence == expectedExonOnePeptideSequence
        assert getExonOneCdnaSequence == expectedExonOneCdnaSequence
        assert getExonOneCdsSequence == expectedExonOneCdsSequence
        assert getExonOneGenomicSequence == expectedExonOneGenomicSequence

        when: "we get the sequence of exon 2"
        String getExonTwoPeptideSequence = sequenceService.getSequenceForFeature(exonList[1], FeatureStringEnum.TYPE_PEPTIDE.value)
        String getExonTwoCdnaSequence = sequenceService.getSequenceForFeature(exonList[1], FeatureStringEnum.TYPE_CDNA.value)
        String getExonTwoCdsSequence = sequenceService.getSequenceForFeature(exonList[1], FeatureStringEnum.TYPE_CDS.value)
        String getExonTwoGenomicSequence = sequenceService.getSequenceForFeature(exonList[1], FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we get the expected sequences"
        assert getExonTwoPeptideSequence == expectedExonTwoPeptideSequence
        assert getExonTwoCdnaSequence == expectedExonTwoCdnaSequence
        assert getExonTwoCdsSequence == expectedExonTwoCdsSequence
        assert getExonTwoGenomicSequence == expectedExonTwoGenomicSequence

        when: "we get the sequence of exon 3, the affected exon"
        String getExonThreePeptideSequence = sequenceService.getSequenceForFeature(exonList[2], FeatureStringEnum.TYPE_PEPTIDE.value)
        String getExonThreeCdnaSequence = sequenceService.getSequenceForFeature(exonList[2], FeatureStringEnum.TYPE_CDNA.value)
        String getExonThreeCdsSequence = sequenceService.getSequenceForFeature(exonList[2], FeatureStringEnum.TYPE_CDS.value)
        String getExonThreeGenomicSequence = sequenceService.getSequenceForFeature(exonList[2], FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we get the expected sequences"
        assert getExonThreePeptideSequence == expectedExonThreePeptideSequence
        assert getExonThreeCdnaSequence == expectedExonThreeCdnaSequence
        assert getExonThreeCdsSequence == expectedExonThreeCdsSequence
        assert getExonThreeGenomicSequence == expectedExonThreeGenomicSequence

        when: "we get the sequence of exon 4"
        String getExonFourPeptideSequence = sequenceService.getSequenceForFeature(exonList[3], FeatureStringEnum.TYPE_PEPTIDE.value)
        String getExonFourCdnaSequence = sequenceService.getSequenceForFeature(exonList[3], FeatureStringEnum.TYPE_CDNA.value)
        String getExonFourCdsSequence = sequenceService.getSequenceForFeature(exonList[3], FeatureStringEnum.TYPE_CDS.value)
        String getExonFourGenomicSequence = sequenceService.getSequenceForFeature(exonList[3], FeatureStringEnum.TYPE_GENOMIC.value)

        then: "we get the expected sequences"
        assert getExonFourPeptideSequence == expectedExonFourPeptideSequence
        assert getExonFourCdnaSequence == expectedExonFourCdnaSequence
        assert getExonFourCdsSequence == expectedExonFourCdsSequence
        assert getExonFourGenomicSequence == expectedExonFourGenomicSequence
    }

    // TODO
    void "when exons from three isoforms, genes should merge on overlap and split on separation"() {

        given: "Three exons we are turning into three transcripts using Group 1.10 GB40782-RA"
//        String transcript1 = '{"track":"Group1.10","features":[{"location":{"fmin":143521,"fmax":146600,"strand":1},"type":{"cv":{"name":"sequence"},"name":"mRNA"},"name":"GB40798-RA","children":[{"location":{"fmin":143521,"fmax":143793,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":146021,"fmax":146600,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":143521,"fmax":143802,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":144067,"fmax":144447,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":145789,"fmax":146600,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":143793,"fmax":146021,"strand":1},"type":{"cv":{"name":"sequence"},"name":"CDS"}}]}],"operation":"add_transcript"}'
//        String transcript2 = '{"track":"Group1.10","features":[{"location":{"fmin":147460,"fmax":152444,"strand":1},"type":{"cv":{"name":"sequence"},"name":"mRNA"},"name":"GB40799-RA","children":[{"location":{"fmin":147460,"fmax":147608,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":147687,"fmax":147729,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":148511,"fmax":148946,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":152157,"fmax":152444,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":147460,"fmax":147608,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":147687,"fmax":147729,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":148511,"fmax":149770,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":150862,"fmax":151206,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":151594,"fmax":152444,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":148946,"fmax":152157,"strand":1},"type":{"cv":{"name":"sequence"},"name":"CDS"}}]}],"operation":"add_transcript"}'

        String moveExonThereCommand
        String moveExonBackCommand

        when: "we add the three non-overlapping transcripts"
//        JSONObject jsonAddTranscriptObject1 = JSON.parse(transcript1) as JSONObject
//        JSONObject jsonAddTranscriptObject2 = JSON.parse(transcript2) as JSONObject
//        JSONObject returnCommand1 = requestHandlingService.addTranscript(jsonAddTranscriptObject1)
//        JSONObject returnCommand2 = requestHandlingService.addTranscript(jsonAddTranscriptObject2)

        then: "we should have two transcripts and two genes!"
//        assert Gene.count == 3
//        assert MRNA.count == 3

        when: "we move the one so that it overlaps"
        //

        then: "we should have one gene "
//        assert Gene.count == 1
//        assert MRNA.count == 3


        when: "we move the exon back"


        then: "we should have three genes again"
//        assert Gene.count == 3
//        assert MRNA.count == 3

        when: "we move it to overlap one"


        then: "we will have 2 genes and 3 transcripts"


        when: "we move it to overlap the other one"


        then: "we will have the same 2 genes and 3 transcripts, but transcript will belong to the other gene"

    }

    void "when adding a transcript and then a sequence alteration in the intron, should not create non-canonical splice sites"() {

        given: "two transcripts"
        String postiveStrandedTranscript = "{\"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":1277947,\"fmax\":1278834,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40860-RA\",\"children\":[{\"location\":{\"fmin\":1277947,\"fmax\":1277953,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1278346,\"fmax\":1278499,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1278738,\"fmax\":1278834,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1277947,\"fmax\":1278834,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String postiveSequenceAlterationInsertion = "{ \"track\": \"Group1.10\", \"features\": [ { \"location\": { \"fmin\": 1278631, \"fmax\": 1278631, \"strand\": 1 }, \"type\": {\"name\": \"insertion\", \"cv\": { \"name\":\"sequence\" } }, \"residues\": \"ATCGATA\" } ], \"operation\": \"add_sequence_alteration\" }"
        String setExonBoundaryCommand = "{\"track\":\"Group1.10\",\"features\":[{\"uniquename\":\"@EXON_UNIQUENAME@\",\"location\":{\"fmin\":1278294,\"fmax\":1278499}}],\"operation\":\"set_exon_boundaries\"}"
        String setUpstreamSpliceAcceptorCommand = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON_UNIQUENAME@\" } ], \"operation\": \"set_to_upstream_acceptor\"}"

//        String negativeStrandedTranscript = "{\"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":1279597,\"fmax\":1282168,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40718-RA\",\"children\":[{\"location\":{\"fmin\":1279597,\"fmax\":1279727,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1279801,\"fmax\":1280160,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1280402,\"fmax\":1280585,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1280671,\"fmax\":1280886,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1281086,\"fmax\":1281316,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1281385,\"fmax\":1281516,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1281603,\"fmax\":1281827,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1282152,\"fmax\":1282168,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1279597,\"fmax\":1282168,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String negativeStrandedTranscript = "{\"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":1365530,\"fmax\":1367665,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40713-RA\",\"children\":[{\"location\":{\"fmin\":1367417,\"fmax\":1367665,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1365530,\"fmax\":1366050,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1365530,\"fmax\":1366107,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1366199,\"fmax\":1366304,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1367171,\"fmax\":1367665,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1366050,\"fmax\":1367417,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
//        String negativeStrandedSequenceInsertion = "{ \"track\": \"Group1.10\", \"features\": [ { \"location\": { \"fmin\": 1280277, \"fmax\": 1280277, \"strand\": 1 }, \"type\": {\"name\": \"insertion\", \"cv\": { \"name\":\"sequence\" } }, \"residues\": \"GAATC\" } ], \"operation\": \"add_sequence_alteration\" }"
        String negativeStrandedSequenceInsertion = "{ \"track\": \"Group1.10\", \"features\": [ { \"location\": { \"fmin\": 1366134, \"fmax\": 1366134, \"strand\": 1 }, \"type\": {\"name\": \"insertion\", \"cv\": { \"name\":\"sequence\" } }, \"residues\": \"ATAGAC\" } ], \"operation\": \"add_sequence_alteration\" }"

        when: "we add the positive transcript"
        requestHandlingService.addTranscript(JSON.parse(postiveStrandedTranscript) as JSONObject)
        List<Exon> exonList = exonService.getSortedExons(MRNA.first(), true)
        String exonUniqueName = exonList.get(1).uniqueName
        setExonBoundaryCommand = setExonBoundaryCommand.replace("@EXON_UNIQUENAME@", exonUniqueName)
        setUpstreamSpliceAcceptorCommand = setUpstreamSpliceAcceptorCommand.replace("@EXON_UNIQUENAME@", exonUniqueName)

        then: "we see the added transcript"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert SequenceAlteration.count == 0
        assert Insertion.count == 0
        assert MRNA.countByName("GB40860-RA-00001")

        when: "we add a sequence alteration"
        requestHandlingService.addSequenceAlteration(JSON.parse(postiveSequenceAlterationInsertion) as JSONObject)

        then: "we should see the alteration, but no non-canonical splice sites"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert SequenceAlteration.count == 1
        assert Insertion.count == 1
        assert MRNA.countByName("GB40860-RA-00001") == 1

        when: "we set the exon boundary"
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand) as JSONObject)

        then: "we should see a non-canonical splice site show up"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert SequenceAlteration.count == 1
        assert Insertion.count == 1
        assert MRNA.countByName("GB40860-RA-00001") == 1

        when: "we set the upstream splice acceptor"
        requestHandlingService.setAcceptor(JSON.parse(setUpstreamSpliceAcceptorCommand) as JSONObject, true)

        then: "acceptor does not go to a canonical acceptor"
        assert Gene.count == 1
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert SequenceAlteration.count == 1
        assert Insertion.count == 1
        assert MRNA.countByName("GB40860-RA-00001") == 1

        when: "we add the negative transcript"
        requestHandlingService.addTranscript(JSON.parse(negativeStrandedTranscript) as JSONObject)

        then: "assert we have one"
        assert MRNA.countByName("GB40713-RA-00001") == 1
        assert Gene.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Exon.count == 3 + 3
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert SequenceAlteration.count == 1
        assert Insertion.count == 1

        when: "we add an overalapping negative sequence alteration"
        requestHandlingService.addSequenceAlteration(JSON.parse(negativeStrandedSequenceInsertion) as JSONObject)

        then: "we should only have the insertion exist"
        assert MRNA.countByName("GB40713-RA-00001") == 1
        assert Gene.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Exon.count == 3 + 3
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert SequenceAlteration.count == 2
        assert Insertion.count == 2
    }

    void "when a setTranslationStart action is performed on a transcript, there should be a check of overlapping isoforms"() {

        given: "Two transcripts having separate parent gene"
        String transcript1 = "{\"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":958639,\"fmax\":959315},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":953072,\"fmax\":953075},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":949590,\"fmax\":950737},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":949590,\"fmax\":950830},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":951050,\"fmax\":951116},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":951349,\"fmax\":951703},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":952162,\"fmax\":952606},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":952865,\"fmax\":953075},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":958639,\"fmax\":959315},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":950737,\"fmax\":953072},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40735-RA\",\"location\":{\"strand\":-1,\"fmin\":949590,\"fmax\":959315},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String setTranslationStartForTranscript1 = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"location\":{\"fmin\":959297}}],\"track\":\"Group1.10\",\"operation\":\"set_translation_start\"}"
        String transcript2 = "{\"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":956828,\"fmax\":956837},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":958023,\"fmax\":958067},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":958639,\"fmax\":959315},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":958639,\"fmax\":958819},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":958023,\"fmax\":958067},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":956828,\"fmax\":956837},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":958897,\"fmax\":959315},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":958819,\"fmax\":958897},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"fgeneshpp_with_rnaseq_Group1.10_208_mRNA\",\"location\":{\"strand\":-1,\"fmin\":956828,\"fmax\":959315},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String setTranslationStartForTranscript2 = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"location\":{\"fmin\":959297}}],\"track\":\"Group1.10\",\"operation\":\"set_translation_start\"}"

        when: "we add transcript1"
        JSONObject addTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript1) as JSONObject).get("features")

        then: "we should have 1 Gene and 1 MRNA"
        assert Gene.count == 1
        assert MRNA.count == 1
        String transcript1UniqueName = addTranscript1ReturnObject.uniquename
        CDS initialCDS = transcriptService.getCDS(MRNA.findByUniqueName(transcript1UniqueName))
        int initialCDSLength = initialCDS.featureLocation.fmax - initialCDS.featureLocation.fmin

        when: "we set translation start for transcript1"
        setTranslationStartForTranscript1 = setTranslationStartForTranscript1.replace("@UNIQUENAME@", transcript1UniqueName)
        JSONObject setTranslationStartTranscript1ReturnObject = requestHandlingService.setTranslationStart(JSON.parse(setTranslationStartForTranscript1) as JSONObject).get("features")

        then: "we should see a difference in CDS length for transcript1"
        CDS alteredCDS = transcriptService.getCDS(MRNA.findByUniqueName(transcript1UniqueName))
        int alteredCDSLength = alteredCDS.featureLocation.fmax - alteredCDS.featureLocation.fmin
        assert initialCDSLength != alteredCDSLength

        when: "we add transcript2"
        JSONObject addTranscript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript2) as JSONObject).get("features")

        then: "we should have 2 Genes and 2 MRNAs"
        assert Gene.count == 2
        assert MRNA.count == 2
        String transcript2Name = addTranscript2ReturnObject.name
        String transcript2UniqueName = addTranscript2ReturnObject.uniquename
        CDS initialCDS2 = transcriptService.getCDS(MRNA.findByUniqueName(transcript2UniqueName))
        int initialCDSLength2 = initialCDS2.featureLocation.fmax - initialCDS2.featureLocation.fmin

        when: "we set translation start for transcript2"
        setTranslationStartForTranscript2 = setTranslationStartForTranscript2.replace("@UNIQUENAME@", transcript2UniqueName)
        JSONObject setTranslationStartTranscript2ReturnObject = requestHandlingService.setTranslationStart(JSON.parse(setTranslationStartForTranscript2) as JSONObject).get("features")

        then: "we should see a difference in CDS length for transcript2"
        String transcript2ModifiedName = setTranslationStartTranscript2ReturnObject.name
        CDS alteredCDS2 = transcriptService.getCDS(MRNA.findByUniqueName(transcript2UniqueName))
        int alteredCDSLength2 = alteredCDS2.fmax - alteredCDS2.fmin
        assert initialCDSLength2 != alteredCDSLength2

        then: "isoform overlap check should have occured, thus transcript1 and transcript2 shares the same parent"
        Gene transcript1Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript1UniqueName))
        Gene transcript2Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript2UniqueName))
        assert transcript1Gene.uniqueName == transcript2Gene.uniqueName

        then: "Name for transcript2 should have changed, in light of the new CDS overlap"
        assert transcript2Name != transcript2ModifiedName

        then: "the previous gene of transcript2 doesn't exist as it is deleted when it has no connected child features"
        Gene.count == 1
    }

    void "when a setTranslationEnd action is performed on a transcript, there should be a check for overlapping isoforms"() {
        
        given: "two transcripts having separate parent gene"
        String transcript1 = "{\"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":592678,\"fmax\":592731},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":593507,\"fmax\":594164},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":588729,\"fmax\":588910},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":592526,\"fmax\":592731},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":593507,\"fmax\":594164},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":588729,\"fmax\":592678},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40820-RA\",\"location\":{\"strand\":1,\"fmin\":588729,\"fmax\":594164},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String setTranslationStartForTranscript1 = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"location\":{\"fmin\":593550}}],\"track\":\"Group1.10\",\"operation\":\"set_translation_start\"}"
        String setTranslationEndForTranscript1 = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"location\":{\"fmax\":593579}}],\"track\":\"Group1.10\",\"operation\":\"set_translation_end\"}"
        
        String transcript2 = "{\"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":593507,\"fmax\":593733},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}],\"name\":\"au8.g307.t1\",\"location\":{\"strand\":1,\"fmin\":593507,\"fmax\":593733},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String setTranslationStartForTranscript2 = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"location\":{\"fmin\":593508}}],\"track\":\"Group1.10\",\"operation\":\"set_translation_start\"}"
        String setTranslationEndForTranscript2 = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"location\":{\"fmax\":593579}}],\"track\":\"Group1.10\",\"operation\":\"set_translation_end\"}"
        
        when: "we add transcript1"
        JSONObject addTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript1) as JSONObject).get("features")
        
        then: "we should have 1 Gene and 1 MRNA"
        assert Gene.count == 1
        assert MRNA.count == 1
        String transcript1UniqueName = addTranscript1ReturnObject.uniquename
        CDS initialCDS = transcriptService.getCDS(MRNA.findByUniqueName(transcript1UniqueName))
        int initialCDSLength = initialCDS.featureLocation.fmax - initialCDS.featureLocation.fmin
        
        when: "we set translation start and end for transcript1"
        setTranslationStartForTranscript1 = setTranslationStartForTranscript1.replace("@UNIQUENAME@", transcript1UniqueName)
        setTranslationEndForTranscript1 = setTranslationEndForTranscript1.replace("@UNIQUENAME@", transcript1UniqueName)
        requestHandlingService.setTranslationStart(JSON.parse(setTranslationStartForTranscript1) as JSONObject)
        requestHandlingService.setTranslationEnd(JSON.parse(setTranslationEndForTranscript1) as JSONObject)
        
        then: "we should see a difference in CDS length for transcript 1"
        CDS alteredCDS = transcriptService.getCDS(MRNA.findByUniqueName(transcript1UniqueName))
        int alteredCDSLength = alteredCDS.featureLocation.fmax - alteredCDS.featureLocation.fmin
        assert initialCDSLength != alteredCDSLength
        
        when: "we add transcript2"
        JSONObject addTranscript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript2) as JSONObject).get("features")
        
        then: "we should have 2 Genes and 2 MRNAs"
        assert Gene.count == 2
        assert MRNA.count == 2
        String transcript2Name = addTranscript2ReturnObject.name
        String transcript2UniqueName = addTranscript2ReturnObject.uniquename
        CDS transcript2InitialCDS = transcriptService.getCDS(MRNA.findByUniqueName(transcript2UniqueName))
        int transcript2InitialCDSLength = transcript2InitialCDS.featureLocation.fmax - transcript2InitialCDS.featureLocation.fmin
        
        when: "we set translation start and end for transcript2"
        setTranslationStartForTranscript2 = setTranslationStartForTranscript2.replace("@UNIQUENAME@", transcript2UniqueName)
        setTranslationEndForTranscript2 = setTranslationEndForTranscript2.replace("@UNIQUENAME@", transcript2UniqueName)
        requestHandlingService.setTranslationStart(JSON.parse(setTranslationStartForTranscript2) as JSONObject)
        JSONObject setTranslationEndTranscript2ReturnObject = requestHandlingService.setTranslationEnd(JSON.parse(setTranslationEndForTranscript2) as JSONObject).get("features")
        
        then: "isoform overlap check should have occured, thus transcript1 and transcript2 now share the same parent gene"
        String transcript2ModifiedName = setTranslationEndTranscript2ReturnObject.name
        Gene transcript1Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript1UniqueName))
        Gene transcript2Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript2UniqueName))
        assert transcript1Gene.uniqueName == transcript2Gene.uniqueName
        
        then: "Name for transcript2 should have changed, in light of the new CDS overlap"
        assert transcript2Name != transcript2ModifiedName
        
        then: "the previous gene of transcript2 doesn't exist as it is deleted when it has no connected child features"
        Gene.count == 1
    }

    void "when a setExonBoundaries action is performed on a transcript, there should be a check for overlapping isoforms"() {

        given: "two transcripts that overlap but aren't isoforms of each other"
        String transcript1 = "{\"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":775816,\"fmax\":776061},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":772054,\"fmax\":772102},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":772808,\"fmax\":772942},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":773319,\"fmax\":773539},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":773682,\"fmax\":773848},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":774138,\"fmax\":774504},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":774865,\"fmax\":775227},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":775543,\"fmax\":776061},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":772054,\"fmax\":775816},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40831-RA\",\"location\":{\"strand\":1,\"fmin\":772054,\"fmax\":776061},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String transcript2 = "{\"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":775543,\"fmax\":776363},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}],\"name\":\"au8.g323.t1\",\"location\":{\"strand\":1,\"fmin\":775543,\"fmax\":776363},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String setExonBoundary1ForTranscript2 = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"location\":{\"fmin\":774048,\"fmax\":776363}}],\"track\":\"Group1.10\",\"operation\":\"set_exon_boundaries\"}"
        String setExonBoundary2ForTranscript2 = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"location\":{\"fmin\":774787,\"fmax\":776363}}],\"track\":\"Group1.10\",\"operation\":\"set_exon_boundaries\"}"
        
        when: "we add transcript1"
        JSONObject addTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript1) as JSONObject).get("features")
        
        then: "we should have 1 Gene and 1 MRNA"
        assert Gene.count == 1
        assert MRNA.count == 1
        String transcript1Name = addTranscript1ReturnObject.name
        String transcript1UniqueName = addTranscript1ReturnObject.uniquename
        
        when: "we add transcript2"
        JSONObject addTranscript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript2) as JSONObject).get("features")
        
        then: "we should have 2 Gene and 2 MRNA"
        assert Gene.count == 2
        assert MRNA.count == 2
        String transcript2Name = addTranscript2ReturnObject.name
        String transcript2UniqueName = addTranscript2ReturnObject.uniquename
        
        when: "we set exon boundary of transcript2"
        Exon exon = exonService.getSortedExons(MRNA.findByUniqueName(transcript2UniqueName))[0]
        setExonBoundary1ForTranscript2 = setExonBoundary1ForTranscript2.replace("@UNIQUENAME@", exon.uniqueName)
        JSONObject setExonBoundary1ForTranscript2ReturnObject = requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundary1ForTranscript2) as JSONObject).get("features")
        
        then: "transcript2 should now be an isoform of transcript1 indicated by transcript1 and transcript2 having the same parent gene"
        String modifiedTranscript2Name = setExonBoundary1ForTranscript2ReturnObject.name
        String modifiedTranscript2UniqueName = setExonBoundary1ForTranscript2ReturnObject.uniquename
        Gene transcript1Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript1UniqueName))
        Gene transcript2Gene = transcriptService.getGene(MRNA.findByUniqueName(modifiedTranscript2UniqueName))
        assert transcript1Gene.uniqueName == transcript2Gene.uniqueName
        
        then: "name for transcript2 changes to reflect its updated parent gene name as prefix"
        Boolean sameBaseName = false
        if (modifiedTranscript2Name.contains(transcript1Gene.name)
        ) {sameBaseName =
                true}
        assert sameBaseName
        
        when: "we set exon boundary of transcript2 again"
        setExonBoundary2ForTranscript2 = setExonBoundary2ForTranscript2.replace("@UNIQUENAME@", exon.uniqueName)
        JSONObject setExonBoundary2ForTranscript2ReturnObject = requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundary2ForTranscript2) as JSONObject).get("features")
        modifiedTranscript2Name = setExonBoundary2ForTranscript2ReturnObject.name
        modifiedTranscript2UniqueName = setExonBoundary2ForTranscript2ReturnObject.uniquename
        
        then: "transcript2 should NOT be an isoform of transcript1"
        Gene updatedTranscript1Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript1UniqueName))
        Gene updatedTranscript2Gene = transcriptService.getGene(MRNA.findByUniqueName(modifiedTranscript2UniqueName))
        assert updatedTranscript1Gene.uniqueName != updatedTranscript2Gene.uniqueName
    }
    
    void "when a flipStrand action is performed on a transcript, there should be a check for overlapping isoforms"() {
        
        given: "A transcript"
        String transcript = "{\"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":404044},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":405031,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40812-RA\",\"location\":{\"strand\":1,\"fmin\":403882,\"fmax\":405154},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String flipStrandForTranscript = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"flip_strand\"}"
        
        when: "we add the transcript"
        JSONObject addTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript) as JSONObject).get("features")
        
        then: "we have 1 Gene and 1 MRNA"
        assert Gene.count == 1
        assert MRNA.count == 1
        String transcript1Name = addTranscript1ReturnObject.name
        String transcript1UniqueName = addTranscript1ReturnObject.uniquename
        
        when: "we move the transcript to opposite strand"
        String flipStrandForTranscript1 = flipStrandForTranscript.replace("@UNIQUENAME@", transcript1UniqueName)
        JSONObject flipStrandForTranscript1ReturnObject = requestHandlingService.flipStrand(JSON.parse(flipStrandForTranscript1) as JSONObject).get("features")
        
        then: "the transcript should be on the negative strand"
        String flippedTranscript1UniqueName = flipStrandForTranscript1ReturnObject.uniquename
        assert MRNA.findByUniqueName(flippedTranscript1UniqueName).strand == Strand.NEGATIVE.value
        
        when: "we add the same transcript again"
        JSONObject addTranscript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript) as JSONObject).get("features")
        
        then: "we have 2 Genes and 2 MRNA"
        assert Gene.count == 2
        assert MRNA.count == 2
        String transcript2Name = addTranscript2ReturnObject.name
        String transcript2UniqueName = addTranscript2ReturnObject.uniquename
        
        when: "we move transcript2 to the opposite strand"
        String flipStrandForTranscript2 = flipStrandForTranscript.replace("@UNIQUENAME@", transcript2UniqueName)
        JSONObject flipStrandForTranscript2ReturnObject = requestHandlingService.flipStrand(JSON.parse(flipStrandForTranscript2)).get("features")
        
        then: "the transcript should be on the negative strand"
        String flippedTranscript2UniqueName = flipStrandForTranscript2ReturnObject.uniquename
        assert MRNA.findByUniqueName(flippedTranscript2UniqueName).strand == Strand.NEGATIVE.value
        
        then: "transcript2 is now an isoform of transcript1"
        Gene transcript1Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript1UniqueName))
        Gene transcript2Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript2UniqueName))
        assert transcript1Gene.uniqueName == transcript2Gene.uniqueName
    }
    
    void "when a mergeExons action is performed on a transcript, there should be a check for overlapping isoforms"() {
        
        given: "3 transcripts that overlap but aren't isoforms of each other"
        String transcript = "{\"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":729928,\"fmax\":730010},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":730296,\"fmax\":730304},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":729928,\"fmax\":730304},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40827-RA\",\"location\":{\"strand\":1,\"fmin\":729928,\"fmax\":730304},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String mergeExons = "{\"features\":[{\"uniquename\":\"@UNIQUENAME1@\"},{\"uniquename\":\"@UNIQUENAME2@\"}],\"track\":\"Group1.10\",\"operation\":\"merge_exons\"}"
        
        when: "we add transcript1"
        JSONObject addTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript) as JSONObject).get("features")
        
        then: "we should have 1 Gene and 1 MRNA"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2
        String transcript1UniqueName = addTranscript1ReturnObject.uniquename
        
        when: "we merge exon 1 with exon2 of transcript1"
        String exon1UniqueName = exonService.getSortedExons(MRNA.findByUniqueName(transcript1UniqueName))[0].uniqueName
        String exon2UniqueName = exonService.getSortedExons(MRNA.findByUniqueName(transcript1UniqueName))[1].uniqueName
        String mergeExonsForTranscript1 = mergeExons.replace("@UNIQUENAME1@", exon1UniqueName).replace("@UNIQUENAME2@", exon2UniqueName)
        JSONObject mergeExonsForTranscript1ReturnObject = requestHandlingService.mergeExons(JSON.parse(mergeExonsForTranscript1) as JSONObject)
        
        then: "we have a transcript that has only 1 exon"
        assert exonService.getSortedExons(MRNA.findByUniqueName(transcript1UniqueName)).size() == 1
        
        when: "now we add transcript2"
        JSONObject addTranscript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript) as JSONObject).get("features")
        
        then: "we should have 2 Genes and 2 MRNAs"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert Exon.count == 3
        String transcript2UniqueName = addTranscript2ReturnObject.uniquename
        
        then: "even though transcript2 is the same as transcript1 (before merge), they are not considered as isoforms of each other"
        Gene transcript1Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript1UniqueName))
        Gene transcript2Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript2UniqueName))
        assert transcript1Gene.uniqueName != transcript2Gene.uniqueName
        
        when: "we merge exon 1 with exon2 of transcript2"
        exon1UniqueName = exonService.getSortedExons(MRNA.findByUniqueName(transcript2UniqueName))[0].uniqueName
        exon2UniqueName = exonService.getSortedExons(MRNA.findByUniqueName(transcript2UniqueName))[1].uniqueName
        String mergeExonsForTranscript2 = mergeExons.replace("@UNIQUENAME1@", exon1UniqueName).replace("@UNIQUENAME2@", exon2UniqueName)
        JSONObject mergeExonsForTranscript2ReturnObject = requestHandlingService.mergeExons(JSON.parse(mergeExonsForTranscript2) as JSONObject).get("features")
        
        then: "transcript2 should be an isoform of transcript1"
        Gene updatedTranscript2Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript2UniqueName))
        assert transcript1Gene.uniqueName == updatedTranscript2Gene.uniqueName
        assert Gene.count == 1
        assert MRNA.count == 2
        assert MRNA.count == 2
    }
    
    void "when a deleteFeature action is performed on a transcript, there should be a check for overlapping isoforms"() {
        
        given: "two transcripts that are isoforms of each other"
        String transcript1 = "{\"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":787455,\"fmax\":787740},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":871534,\"fmax\":871600},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":871708,\"fmax\":871861},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":873893,\"fmax\":874091},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874214,\"fmax\":874252},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874336,\"fmax\":874787},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874910,\"fmax\":875076},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":787455,\"fmax\":788349},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":789768,\"fmax\":790242},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":791007,\"fmax\":792220},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":793652,\"fmax\":793876},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":806935,\"fmax\":807266},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":828378,\"fmax\":829272},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":847144,\"fmax\":847365},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":859150,\"fmax\":859261},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":871519,\"fmax\":871600},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":871708,\"fmax\":871861},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":873893,\"fmax\":874091},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874214,\"fmax\":874252},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874336,\"fmax\":874787},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":874910,\"fmax\":875076},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":787740,\"fmax\":871534},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"au8.g325.t1\",\"location\":{\"strand\":-1,\"fmin\":787455,\"fmax\":875076},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String transcript2 = "{\"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":845782,\"fmax\":845798},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":847144,\"fmax\":847278},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":845782,\"fmax\":847278},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40739-RA\",\"location\":{\"strand\":-1,\"fmin\":845782,\"fmax\":847278},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String deleteExon = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"delete_feature\"}"
        
        when: "we add transcript1 and transcript2"
        JSONObject addTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript1) as JSONObject).get("features")
        JSONObject addTranscript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript2) as JSONObject).get("features")
        
        then: "we have 1 Gene and 2 MRNAs"
        assert Gene.count == 1
        assert MRNA.count == 2
        String transcript1UniqueName = addTranscript1ReturnObject.uniquename
        String transcript2UniqueName = addTranscript2ReturnObject.uniquename

        when: "we delete the first exon of transcript2"
        Exon firstExonOfTranscript2 = exonService.getSortedExons(MRNA.findByUniqueName(transcript2UniqueName))[0]
        String deleteExonOfTranscript2 = deleteExon.replace("@UNIQUENAME@", firstExonOfTranscript2.uniqueName)
        JSONObject deleteExonOfTranscript2ReturnObject = requestHandlingService.deleteFeature(JSON.parse(deleteExonOfTranscript2) as JSONObject).get("features")
        
        then: "transcript1 and transcript2 are no longer isoforms of each other"
        Gene transcript1Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript1UniqueName))
        Gene transcript2Gene = transcriptService.getGene(MRNA.findByUniqueName(transcript2UniqueName))
        assert transcript1Gene.uniqueName != transcript2Gene.uniqueName
        assert Gene.count == 2
        assert MRNA.count == 2
    }
    
    void "when two transcripts become isoforms of each others, the properties of their respective parent gene should be preserved in the merged gene"() {

        given: "Two transcripts that overlap but are not isoforms of each other"
        String transcript1 = "{\"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":583280,\"fmax\":583605},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":577493,\"fmax\":577643},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":582506,\"fmax\":582677},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":583187,\"fmax\":583605},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":577493,\"fmax\":583280},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40819-RA\",\"location\":{\"strand\":1,\"fmin\":577493,\"fmax\":583605},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String transcript2 = "{\"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":576138,\"fmax\":576168},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":582506,\"fmax\":582677},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":583187,\"fmax\":583605},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":576138,\"fmax\":576168},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":582506,\"fmax\":582677},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":583187,\"fmax\":583194},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":583554,\"fmax\":583605},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":583194,\"fmax\":583554},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"5:geneid_mRNA_CM000054.5_467\",\"location\":{\"strand\":1,\"fmin\":576138,\"fmax\":583605},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String setTranslationStartForTranscript2 = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"location\":{\"fmin\":582521}}],\"track\":\"Group1.10\",\"operation\":\"set_translation_start\"}"

        //String setSymbolOperation = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"symbol\":\"@SYMBOL_VALUE@\"}],\"track\":\"Group1.10\",\"operation\":\"set_symbol\",\"username\":\"demo@demo.com\"}"
        //String setDescriptionOperation = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"description\":\"@DESCRIPTION_VALUE@\"}],\"track\":\"Group1.10\",\"operation\":\"set_description\"}"
        String addDbxrefOperation = "{\"features\":[{\"dbxrefs\":[{\"accession\":\"@XREF_ACCESSION@\",\"db\":\"@XREF_DB@\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"add_non_primary_dbxrefs\"}"
        String addAttributeOperation = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"non_reserved_properties\":[{\"tag\":\"@ATTRIBUTE_TAG@\",\"value\":\"@ATTRIBUTE_VALUE@\"}]}],\"track\":\"Group1.10\",\"operation\":\"add_non_reserved_properties\"}"
        String addPublicationOperation = "{\"features\":[{\"dbxrefs\":[{\"accession\":\"@PUBMED_ACCESSION@\",\"db\":\"PMID\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"add_non_primary_dbxrefs\"}"
        String addGeneOntologyOperation = "{\"features\":[{\"dbxrefs\":[{\"accession\":\"@GO_ACCESSION@\",\"db\":\"GO\"}],\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"add_non_primary_dbxrefs\"}"
        String addCommentOperation = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\",\"comments\":[\"@COMMENT@\"]}],\"track\":\"Group1.10\",\"operation\":\"add_comments\"}"

        when: "we add transcript1 and transcript2"
        JSONObject addTranscript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript1) as JSONObject).get("features")
        JSONObject addTranscript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcript2) as JSONObject).get("features")
        then:"we should see 2 Genes and 2 MRNAs"
        assert Gene.count == 2
        assert MRNA.count == 2
        String transcript1UniqueName = addTranscript1ReturnObject.uniquename
        String transcript2UniqueName = addTranscript2ReturnObject.uniquename
        Gene initialGeneForTranscript1 = transcriptService.getGene(MRNA.findByUniqueName(transcript1UniqueName))
        Gene initialGeneForTranscript2 = transcriptService.getGene(MRNA.findByUniqueName(transcript2UniqueName))

        when: "we add properties to transcript1"
        String addDbxref1ForTranscript1 = addDbxrefOperation.replace("@UNIQUENAME@", initialGeneForTranscript1.uniqueName).replace("@XREF_DB@", "NCBI").replace("@XREF_ACCESSION@", "12937129")
        String addDbxref2ForTranscript1 = addDbxrefOperation.replace("@UNIQUENAME@", initialGeneForTranscript1.uniqueName).replace("@XREF_DB@", "Ensembl").replace("@XREF_ACCESSION@", "ENSG000000000213")
        String addAttribute1ForTranscript1 = addAttributeOperation.replace("@UNIQUENAME@", initialGeneForTranscript1.uniqueName).replace("@ATTRIBUTE_TAG@", "isPredicted").replace("@ATTRIBUTE_VALUE@", "true")
        String addAttribute2ForTranscript1 = addAttributeOperation.replace("@UNIQUENAME@", initialGeneForTranscript1.uniqueName).replace("@ATTRIBUTE_TAG@", "isProteinCoding").replace("@ATTRIBUTE_VALUE@", "true")
        String addPublicationForTranscript1 = addPublicationOperation.replace("@UNIQUENAME@", initialGeneForTranscript1.uniqueName).replace("@PUBMED_ACCESSION@", "8379243")
        String addGeneOntologyForTranscript1 = addGeneOntologyOperation.replace("@UNIQUENAME@", initialGeneForTranscript1.uniqueName).replace("@GO_ACCESSION@", "GO:1902009")
        String addCommentForTranscript1 = addCommentOperation.replace("@UNIQUENAME@", initialGeneForTranscript1.uniqueName).replace("@COMMENT", "This gene is a test gene and created solely for the purpose of this test")

        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addDbxref1ForTranscript1) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addDbxref2ForTranscript1) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(addAttribute1ForTranscript1) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(addAttribute2ForTranscript1) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addPublicationForTranscript1) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addGeneOntologyForTranscript1) as JSONObject)
        requestHandlingService.addComments(JSON.parse(addCommentForTranscript1) as JSONObject)
        
        then:

        "we should have 3 FeatureProperty entities and 4 DBXref entities"
        assert FeatureProperty.count == 3
        assert DBXref.count == 4
        
        when: "we add properties to transcript2"
        String addDbxref1ForTranscript2 = addDbxrefOperation.replace("@UNIQUENAME@", initialGeneForTranscript2.uniqueName).replace("@XREF_DB@", "NCBI").replace("@XREF_ACCESSION@", "83924623")
        String addDbxref2ForTranscript2 = addDbxrefOperation.replace("@UNIQUENAME@", initialGeneForTranscript2.uniqueName).replace("@XREF_DB@", "Ensembl").replace("@XREF_ACCESSION@", "ENSG000000000112")
        String addAttribute1ForTranscript2 = addAttributeOperation.replace("@UNIQUENAME@", initialGeneForTranscript2.uniqueName).replace("@ATTRIBUTE_TAG@", "isAnnotated").replace("@ATTRIBUTE_VALUE@", "false")
        String addAttribute2ForTranscript2 = addAttributeOperation.replace("@UNIQUENAME@", initialGeneForTranscript2.uniqueName).replace("@ATTRIBUTE_TAG@", "isApproved").replace("@ATTRIBUTE_VALUE@", "false")
        String addPublicationForTranscript2 = addPublicationOperation.replace("@UNIQUENAME@", initialGeneForTranscript2.uniqueName).replace("@PUBMED_ACCESSION@", "8923422")
        String addGeneOntologyForTranscript2 = addGeneOntologyOperation.replace("@UNIQUENAME@", initialGeneForTranscript2.uniqueName).replace("@GO_ACCESSION@", "GO:0009372")
        String addCommentForTranscript2 = addCommentOperation.replace("@UNIQUENAME@", initialGeneForTranscript2.uniqueName).replace("@COMMENT", "This gene is a another test gene and created solely for the purpose of this test")

        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addDbxref1ForTranscript2) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addDbxref2ForTranscript2) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(addAttribute1ForTranscript2) as JSONObject)
        requestHandlingService.addNonReservedProperties(JSON.parse(addAttribute2ForTranscript2) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addPublicationForTranscript2) as JSONObject)
        requestHandlingService.addNonPrimaryDbxrefs(JSON.parse(addGeneOntologyForTranscript2) as JSONObject)
        requestHandlingService.addComments(JSON.parse(addCommentForTranscript2) as JSONObject)
        
        then: "we should have 6 FeatureProperty entities and 8 DBXref entities"
        assert FeatureProperty.count == 6
        assert DBXref.count == 8
        def xRefInitialGeneForTranscript1 = initialGeneForTranscript1.getFeatureDBXrefs()
        def fpInitialGeneForTranscript1 = initialGeneForTranscript1.getFeatureProperties()
        def xRefInitialGeneForTranscript2 = initialGeneForTranscript2.getFeatureDBXrefs()
        def fpInitialGeneForTranscript2 = initialGeneForTranscript2.getFeatureProperties()
        def combinedxRefs = (xRefInitialGeneForTranscript1 + xRefInitialGeneForTranscript2).sort()
        def combinedFeatureProperties = (fpInitialGeneForTranscript1 + fpInitialGeneForTranscript2).sort()
        
        when: "we set exon boundary of transcript2"
        setTranslationStartForTranscript2 = setTranslationStartForTranscript2.replace("@UNIQUENAME@", transcript2UniqueName)
        requestHandlingService.setTranslationStart(JSON.parse(setTranslationStartForTranscript2) as JSONObject)
        
        then: "transcript1 and transcript2 should be isoforms of each other and they should have the same parent gene"
        assert Gene.count == 1
        assert MRNA.count == 2
        assert FeatureProperty.count == 7
        assert DBXref.count == 8
        Gene finalGeneForTranscript1 = transcriptService.getGene(MRNA.findByUniqueName(transcript1UniqueName))
        Gene finalGeneForTranscript2 = transcriptService.getGene(MRNA.findByUniqueName(transcript2UniqueName))
        assert finalGeneForTranscript1.uniqueName == finalGeneForTranscript2.uniqueName
        
        then: "all properties of the parent gene for transcript2, before setTranslationStart, should now be properties of current shared gene"
        def xRefForMergedGene = finalGeneForTranscript1.getFeatureDBXrefs()
        def fpForMergedGene = finalGeneForTranscript1.getFeatureProperties()
        assert combinedxRefs == xRefForMergedGene.sort()
        assert combinedFeatureProperties == fpForMergedGene.sort()
    }

    void "split and merge should work for transcripts belonging to same gene"() {
        given: "GB40812-RA"
        String transcriptString = "{\"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":403882,\"fmax\":405154,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40812-RA\",\"children\":[{\"location\":{\"fmin\":403882,\"fmax\":404044,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":405031,\"fmax\":405154,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":403882,\"fmax\":405154,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String setExonBoundaryOperation = "{\"features\":[{\"uniquename\":\"@EXON_UNIQUENAME@\",\"location\":{\"fmin\":@EXON_FMIN@,\"fmax\":@EXON_FMAX@}}],\"track\":\"Group1.10\",\"operation\":\"set_exon_boundaries\"}"
        String splitTranscriptOperation = "{\"features\":[{\"uniquename\":\"@EXON1_UNIQUENAME@\"},{\"uniquename\":\"@EXON2_UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"split_transcript\"}"
        String mergeTranscriptOperation = "{\"features\":[{\"uniquename\":\"@TRANSCRIPT1_UNIQUENAME@\"},{\"uniquename\":\"@TRANSCRIPT2_UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"merge_transcripts\"}"

        when: "we add transcript GB40812-RA twice"
        JSONObject transcript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcriptString) as JSONObject)
        JSONObject transcript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcriptString) as JSONObject)

        then: "we should see 1 gene with 2 transcripts"
        assert Gene.count == 1
        assert MRNA.count == 2
        assert Exon.count == 4

        when: "we split a transcript"
        def mrnas = MRNA.all.sort{ a, b -> a.name <=> b.name }
        Transcript transcript1 = mrnas[0]
        Transcript transcript2 = mrnas[1]
        ArrayList<Exon> exonList = transcriptService.getSortedExons(transcript2)
        String exon1UniqueName = exonList.get(0).uniqueName
        String exon2UniqueName = exonList.get(1).uniqueName

        String splitTranscript2String = splitTranscriptOperation.replace("@EXON1_UNIQUENAME@", exon1UniqueName).replace("@EXON2_UNIQUENAME@", exon2UniqueName)
        JSONObject splitTranscript2ReturnObject = requestHandlingService.splitTranscript(JSON.parse(splitTranscript2String) as JSONObject)

        then: "we should have 1 gene and 3 transcripts"
        assert Gene.count == 1
        assert Transcript.count == 3
        assert Exon.count == 4
        assert CDS.count == 3

        when: "we merge the sub transcripts"
        mrnas = MRNA.all.sort{ a, b -> a.name <=> b.name }
        transcript1 = mrnas[0]
        transcript2 = mrnas[1]
        Transcript transcript3 = mrnas[2]
        String mergeTranscriptsString = mergeTranscriptOperation.replace("@TRANSCRIPT1_UNIQUENAME@", transcript2.uniqueName).replace("@TRANSCRIPT2_UNIQUENAME@", transcript3.uniqueName)
        JSONObject mergeTranscriptReturnObject = requestHandlingService.mergeTranscripts(JSON.parse(mergeTranscriptsString) as JSONObject)

        then: "we should have 1 gene, 1 transcript, 4 exons and 2 CDS"
        assert Gene.count == 1
        assert Transcript.count == 2
        assert Exon.count == 4
        assert CDS.count == 2
    }

    void "for a negative stranded feature, after a split, an undo followed by redo should give the same result as that of a split transcript"() {
        given: "GB40745-RA"
        String transcriptString = "{\"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":733182,\"fmax\":733316},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":731930,\"fmax\":732023},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":731930,\"fmax\":732539},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":732909,\"fmax\":733316},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":732023,\"fmax\":733182},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40745-RA\",\"location\":{\"strand\":-1,\"fmin\":731930,\"fmax\":733316},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String splitTranscriptOperation = "{\"features\":[{\"uniquename\":\"@EXON1_UNIQUENAME@\"},{\"uniquename\":\"@EXON2_UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"split_transcript\"}"
        String undoOperation = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"undo\"}"
        String redoOperation = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"redo\"}"

        when: "we add transcript GB40745-RA twice"
        JSONObject transcript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcriptString) as JSONObject)
        JSONObject transcript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcriptString) as JSONObject)

        then: "we should see 1 gene with 2 transcripts"
        assert Gene.count == 1
        assert MRNA.count == 2
        assert Exon.count == 4
        assert CDS.count == 2

        when: "we split the transcript"
        Transcript transcript1 = MRNA.all[0]
        Transcript transcript2 = MRNA.all[1]
        ArrayList<Exon> exonList = transcriptService.getSortedExons(transcript2)
        String exon1UniqueName = exonList.get(0).uniqueName
        String exon2UniqueName = exonList.get(1).uniqueName

        String splitTranscriptString = splitTranscriptOperation.replace("@EXON1_UNIQUENAME@", exon1UniqueName).replace("@EXON2_UNIQUENAME@", exon2UniqueName)
        JSONObject splitTranscriptReturnObject = requestHandlingService.splitTranscript(JSON.parse(splitTranscriptString) as JSONObject)

        then: "we should have 1 gene and 3 transcripts"
        assert Gene.count == 1
        assert MRNA.count == 3
        assert Exon.count == 4
        assert CDS.count == 3

        when: "we do an undo operation on the transcript"
        String transcriptSplitUndoString = undoOperation.replace("@UNIQUENAME@", transcript2.uniqueName)
        JSONObject transcriptSplitReturnObject = requestHandlingService.undo(JSON.parse(transcriptSplitUndoString) as JSONObject)

        then: "we should have successfully undid the split transcript"
        assert Gene.count == 1
        assert MRNA.count == 2
        assert Exon.count == 4
        assert CDS.count == 2

        when: "we do a redo operation on the transcript"
        String transcriptSplitRedoString = redoOperation.replace("@UNIQUENAME@", transcript2.uniqueName)
        JSONObject transcriptSplitRedoReturnObject = requestHandlingService.redo(JSON.parse(transcriptSplitRedoString) as JSONObject)

        then: "we should have the same outcome as that of split transcript"
        assert Gene.count == 1
        assert MRNA.count == 3
        assert Exon.count == 4
        assert CDS.count == 3

        then: "and there will be no orphan transcripts left behind"

        MRNA.all.each {
            Gene gene = transcriptService.getGene(it)
            assert gene != null
        }
    }

    void "for a positive stranded feature, after a split, an undo followed by redo should give the same result as that of a split transcript"() {
        given: "GB40822-RA"
        String transcriptString = "{\"features\":[{\"children\":[{\"location\":{\"strand\":1,\"fmin\":654601,\"fmax\":654649},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":657084,\"fmax\":657144},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":1,\"fmin\":654601,\"fmax\":657144},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40822-RA\",\"location\":{\"strand\":1,\"fmin\":654601,\"fmax\":657144},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String splitTranscriptOperation = "{\"features\":[{\"uniquename\":\"@EXON1_UNIQUENAME@\"},{\"uniquename\":\"@EXON2_UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"split_transcript\"}"
        String undoOperation = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"undo\"}"
        String redoOperation = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"redo\"}"

        when: "we add transcript GB40822-RA, twice"
        JSONObject transcript1ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcriptString) as JSONObject)
        JSONObject transcript2ReturnObject = requestHandlingService.addTranscript(JSON.parse(transcriptString) as JSONObject)

        then: "we should see 1 gene with 2 transcripts"
        assert Gene.count == 1
        assert MRNA.count == 2
        assert Exon.count == 4
        assert CDS.count == 2

        when: "we split the transcript"
        Transcript transcript1 = MRNA.all[0]
        Transcript transcript2 = MRNA.all[1]
        ArrayList<Exon> exonList = transcriptService.getSortedExons(transcript2)
        String exon1UniqueName = exonList.get(0).uniqueName
        String exon2UniqueName = exonList.get(1).uniqueName

        String splitTranscriptString = splitTranscriptOperation.replace("@EXON1_UNIQUENAME@", exon1UniqueName).replace("@EXON2_UNIQUENAME@", exon2UniqueName)
        JSONObject splitTranscriptReturnObject = requestHandlingService.splitTranscript(JSON.parse(splitTranscriptString) as JSONObject)

        then: "we should have 1 gene and 3 transcripts"
        assert Gene.count == 1
        assert MRNA.count == 3
        assert Exon.count == 4
        assert CDS.count == 3

        when: "we do an undo operation on the transcript"
        String transcriptSplitUndoString = undoOperation.replace("@UNIQUENAME@", transcript2.uniqueName)
        JSONObject transcriptSplitReturnObject = requestHandlingService.undo(JSON.parse(transcriptSplitUndoString) as JSONObject)

        then: "we should have successfully undid the split transcript"
        assert Gene.count == 1
        assert MRNA.count == 2
        assert Exon.count == 4
        assert CDS.count == 2

        when: "we do a redo operation on the transcript"
        String transcriptSplitRedoString = redoOperation.replace("@UNIQUENAME@", transcript2.uniqueName)
        JSONObject transcriptSplitRedoReturnObject = requestHandlingService.redo(JSON.parse(transcriptSplitRedoString) as JSONObject)

        then: "we should have the same outcome as that of split transcript"
        assert Gene.count == 1
        assert MRNA.count == 3
        assert Exon.count == 4
        assert CDS.count == 3

        then: "and there will be no orphan transcripts left behind"

        MRNA.all.each {
            Gene gene = transcriptService.getGene(it)
            assert gene != null
        }
    }

    void "when we add undo a merge transcript operation, isoform overlap rule should be applied consistently"() {
        given: "Two transcripts: GB40769-RA and GB40770-RA"
        String transcript1String = "{\"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":262511,\"fmax\":262558},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":258387,\"fmax\":258402},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":258387,\"fmax\":258492},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":259081,\"fmax\":259225},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":259957,\"fmax\":260090},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":260570,\"fmax\":260697},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":262216,\"fmax\":262558},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":258402,\"fmax\":262511},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40769-RA\",\"location\":{\"strand\":-1,\"fmin\":258387,\"fmax\":262558},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String transcript2String = "{\"features\":[{\"children\":[{\"location\":{\"strand\":-1,\"fmin\":255124,\"fmax\":255153},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":252400,\"fmax\":252408},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":252004,\"fmax\":252303},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":252004,\"fmax\":252303},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":252400,\"fmax\":252462},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":252667,\"fmax\":252814},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":253468,\"fmax\":253601},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":253751,\"fmax\":253878},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":254826,\"fmax\":255153},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"strand\":-1,\"fmin\":252408,\"fmax\":255124},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}],\"name\":\"GB40770-RA\",\"location\":{\"strand\":-1,\"fmin\":252004,\"fmax\":255153},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"}}],\"track\":\"Group1.10\",\"operation\":\"add_transcript\"}"
        String mergeTranscriptOperation = " {\"features\":[{\"uniquename\":\"@TRANSCRIPT1_UNIQUENAME@\"},{\"uniquename\":\"@TRANSCRIPT2_UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"merge_transcripts\"}"
        String undoOperation = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"undo\"}"

        when: "we add transcripts"
        requestHandlingService.addTranscript(JSON.parse(transcript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(transcript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(transcript2String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(transcript2String) as JSONObject)

        then: "we should see 2 genes and 4 transcripts"
        assert Gene.count == 2
        assert MRNA.count == 4

        Transcript transcript1 = MRNA.findByName("GB40769-RA-00001")
        Transcript transcript2 = MRNA.findByName("GB40769-RA-00002")
        Transcript transcript3 = MRNA.findByName("GB40770-RA-00001")
        Transcript transcript4 = MRNA.findByName("GB40770-RA-00002")


        when: "we merge transcript GB40769-RA-00002 with GB40770-RA-00002"
        String mergeTranscriptString = mergeTranscriptOperation.replace("@TRANSCRIPT1_UNIQUENAME@", transcript4.uniqueName).replace("@TRANSCRIPT2_UNIQUENAME@", transcript2.uniqueName)
        JSONObject mergeTranscriptReturnObject = requestHandlingService.mergeTranscripts(JSON.parse(mergeTranscriptString) as JSONObject)

        then: "there should be 2 genes and 3 transcripts"
        assert Gene.count == 2
        assert MRNA.count == 3

        def genes = Gene.all.sort{ a, b -> a.name <=> b.name }
        assert transcriptService.getTranscripts(genes[0]).size() == 2
        assert transcriptService.getTranscripts(genes[1]).size() == 1

        when: "we undo the operation"
        String undoMergeTranscriptString = undoOperation.replace("@UNIQUENAME@", transcript2.uniqueName)
        requestHandlingService.undo(JSON.parse(undoMergeTranscriptString) as JSONObject)

        then: "we should have 2 genes and 4 transcripts"
        assert Gene.count == 2
        assert Transcript.count == 4
        assert MRNA.count == 4
        assert Exon.count == 22
        assert CDS.count == 4

        then: "each of the gene should have two transcripts"
        assert transcriptService.getTranscripts(genes[0]).size() == 2
        assert transcriptService.getTranscripts(genes[1]).size() == 2

    }

    void "when we merge two transcript where transcript1 is not the 5' most transcript, isoform overlap should be applied consistently"() {

        given: "GB40857-RA and GB40858-RA"
        String transcript1String = "{\"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1242150,\"strand\":1,\"fmax\":1247022},\"name\":\"GB40857-RA\",\"children\":[{\"location\":{\"fmin\":1246797,\"strand\":1,\"fmax\":1247022},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1242150,\"strand\":1,\"fmax\":1242169},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1242732,\"strand\":1,\"fmax\":1243098},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1245018,\"strand\":1,\"fmax\":1245488},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1245952,\"strand\":1,\"fmax\":1246251},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1246355,\"strand\":1,\"fmax\":1246459},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1246555,\"strand\":1,\"fmax\":1247022},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1242150,\"strand\":1,\"fmax\":1246797},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String transcript2String = "{\"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1248775,\"strand\":1,\"fmax\":1253496},\"name\":\"GB40858-RA\",\"children\":[{\"location\":{\"fmin\":1248775,\"strand\":1,\"fmax\":1248881},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1248959,\"strand\":1,\"fmax\":1249093},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1250262,\"strand\":1,\"fmax\":1250390},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1252434,\"strand\":1,\"fmax\":1253496},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1248775,\"strand\":1,\"fmax\":1248881},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1248959,\"strand\":1,\"fmax\":1249093},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1250262,\"strand\":1,\"fmax\":1250459},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1250660,\"strand\":1,\"fmax\":1250754},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1250852,\"strand\":1,\"fmax\":1251017},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1251094,\"strand\":1,\"fmax\":1251261},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1251327,\"strand\":1,\"fmax\":1251564},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1251639,\"strand\":1,\"fmax\":1251863},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1251940,\"strand\":1,\"fmax\":1252202},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1252293,\"strand\":1,\"fmax\":1253496},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1250390,\"strand\":1,\"fmax\":1252434},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String transcript3String = "{\"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1245018,\"strand\":1,\"fmax\":1245488},\"name\":\"GB40857-RA\",\"children\":[{\"location\":{\"fmin\":1245018,\"strand\":1,\"fmax\":1245488},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Group1.10\"}"
        String mergeTranscriptOperation = "{\"features\":[{\"uniquename\":\"@TRANSCRIPT1_UNIQUENAME@\"},{\"uniquename\":\"@TRANSCRIPT2_UNIQUENAME@\"}],\"track\":\"Group1.10\",\"operation\":\"merge_transcripts\"}"

        when: "we add transcripts"
        requestHandlingService.addTranscript(JSON.parse(transcript1String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(transcript3String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(transcript2String) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(transcript2String) as JSONObject)

        then: "we should have 4 transcripts, 2 genes"
        assert Gene.count == 2
        assert MRNA.count == 4
        assert CDS.count == 4

        Transcript transcript1 = MRNA.findByName("GB40857-RA-00001")
        Transcript transcript2 = MRNA.findByName("GB40857-RA-00002")
        Transcript transcript3 = MRNA.findByName("GB40858-RA-00001")
        Transcript transcript4 = MRNA.findByName("GB40858-RA-00002")

        when: "we merge GB40857-RA-00002 and GB40858-RA-00002"
        String mergeTranscriptString = mergeTranscriptOperation.replace("@TRANSCRIPT1_UNIQUENAME@", transcript2.uniqueName).replace("@TRANSCRIPT2_UNIQUENAME@", transcript4.uniqueName)
        JSONObject mergeTranscriptReturnObject = requestHandlingService.mergeTranscripts(JSON.parse(mergeTranscriptString) as JSONObject)

        then: "there should be 2 genes and 3 transcripts"
        assert Gene.count == 2
        assert MRNA.count == 3

        List<Gene> geneList = Gene.all.sort { a, b ->
            a.fmin <=> b.fmin
        }
        assert transcriptService.getTranscripts(geneList.get(0)).size() == 1
        assert transcriptService.getTranscripts(geneList.get(1)).size() == 2
    }

}
