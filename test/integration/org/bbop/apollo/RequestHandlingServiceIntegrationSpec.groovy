package org.bbop.apollo

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class RequestHandlingServiceIntegrationSpec extends IntegrationSpec {

    def requestHandlingService
    def featureRelationshipService
    def exonService

    def setup() {
        Sequence sequence = new Sequence(
                length: 1405242
                ,refSeqFile: "adsf"
                ,seqChunkPrefix: "Group1.10-"
                ,seqChunkSize: 20000
                ,start: 0
                ,end: 1405242
                // from (honeybee f78/c6f/0c
                ,sequenceDirectory: "test/integration/resources/sequences/honeybee-Group1.10/"
                ,name: "Group1.10"
        ).save()
        
        
    }

    def cleanup() {
//        Sequence.deleteAll(Sequence.all)
//        Feature.withTransaction {
//            FeatureLocation.executeUpdate("delete from FeatureLocation ")
//            FeatureRelationship.executeUpdate("delete from FeatureRelationship ")
//            Feature.executeUpdate("delete from Feature ")
//            SequenceChunk.executeUpdate("delete from SequenceChunk ")
//            Sequence.first().sequenceChunks?.clear()
//            Sequence.first().save(flush: true )
////            Sequence.executeUpdate("delete from Sequence ")
//        }
////
////        assert Sequence.count == 0
//        assert Feature.count == 0
//        assert FeatureLocation.count == 0
//        assert FeatureRelationship.count == 0

//        Feature.deleteAll(Feature.all)
//        Exon.deleteAll(Exon.all)
//        Gene.deleteAll(Gene.all)
//        MRNA.deleteAll(MRNA.all)
//        .deleteAll(MRNA.all)
    }

    void "add transcript with UTR"() {

        given: "a transcript with a UTR"
        String jsonString = " { \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235534,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1216850,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1224676,\"fmax\":1224823,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1228682,\"fmax\":1228825,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235237,\"fmax\":1235396,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235487,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1235534,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        
        String validJSONString = "{\"features\":[{\"location\":{\"fmin\":1216824,\"strand\":1,\"fmax\":1235616},\"parent_type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235237,\"strand\":1,\"fmax\":1235396},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"@TRANSCRIPT_NAME@\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1216824,\"strand\":1,\"fmax\":1216850},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"0992325F0DD2290AB58EA37ECF2DA2E7\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1235487,\"strand\":1,\"fmax\":1235616},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"1C091FE87A8133803A69887F38FBDC4C\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209542,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1224676,\"strand\":1,\"fmax\":1224823},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"6D2E15D6DA759C523B79B96795927CAF\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1228682,\"strand\":1,\"fmax\":1228825},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"99C2A027C87DBDBC5536503D5C38F21C\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1216824,\"strand\":1,\"fmax\":1235534},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"994B96C6594F5DB1B6C836E6E0EDE2A6\",\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"}],\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"5A8C864885BC71606E120322CE0EC28C\",\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209602,\"parent_id\":\"8B9E9AC4D0DB90464F26B2F77A1E09B4\"}]}"
                

        when: "You add a transcript via JSON"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        JSONObject correctJsonReturnObject = JSON.parse(validJSONString) as JSONObject

        then: "there should be no features"
        assert  Feature.count == 0
        assert  FeatureLocation.count == 0
        assert  Sequence.count == 1
        JSONArray mrnaArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1==mrnaArray.size()
        assert 7==getCodingArray(jsonObject).size()



        when: "you parse add a transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)



        then: "You should see that transcript"
        assert  Sequence.count == 1
        // there are 6 exons, but 2 of them overlap . . . so this is correct
        assert  Exon.count == 5
        assert  CDS.count == 1
        assert  MRNA.count == 1
        assert  Gene.count == 1
        def allFeatures = Feature.all
       
        // this is the new part
        assert FlankingRegion.count==10
        assert  FeatureLocation.count == 18
        assert  Feature.count == 18


        JSONArray returnedCodingArray = getCodingArray(returnObject)
        JSONArray validCodingArray = getCodingArray(correctJsonReturnObject)
        assert returnedCodingArray.size()==validCodingArray.size()
        
//        assert "ADD"==returnObject.getString("operation")
//        assert Gene.count == 1
//        assert Gene.first().name=="Bob1"

    }

    JSONArray getCodingArray(JSONObject jsonObject) {
        JSONArray mrnaArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1==mrnaArray.size()
        return mrnaArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value)
    }

    void "add a transcript which is a single exon needs to translate correctly"(){
        
        given: "the input string "
        String jsonString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB42152-RA\",\"children\":[{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}], \"operation\": \"add_transcript\" }"

        when: "You add a transcript via JSON"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        then: "there should be no features"
        assert  Feature.count == 0
        assert  FeatureLocation.count == 0
        assert  Sequence.count == 1
        JSONArray mrnaArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1==mrnaArray.size()
        JSONArray codingArray = mrnaArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 1==codingArray.size()
        
        when: "it gets added"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        
        then: "we should see the appropriate stuff"
        assert  Sequence.count == 1
        // there are 6 exons, but 2 of them overlap . . . so this is correct
        assert  CDS.count == 1
        assert  MRNA.count == 1
        assert  Gene.count == 1
        assert  Exon.count == 1
        int flankingCount = FlankingRegion.count
        assert  Feature.count == 4+flankingCount
        assert  FeatureLocation.count == 4+flankingCount
        assert  FeatureRelationship.count == 3

        Gene gene = Gene.first()
        assert featureRelationshipService.getParentForFeature(gene)==null
        assert featureRelationshipService.getChildren(gene).size()==1
        MRNA mrna = featureRelationshipService.getChildForFeature(gene,MRNA.ontologyId)
        assert mrna.id == MRNA.first().id
        List<Feature> childFeatureRelationships =  featureRelationshipService.getParentsForFeature(mrna)
        assert 1==childFeatureRelationships.size()
        Feature parentFeature = featureRelationshipService.getParentForFeature(mrna)
        assert parentFeature!=null
        assert featureRelationshipService.getParentForFeature(mrna).id==gene.id
        // should be an exon and a CDS . . .
        assert featureRelationshipService.getChildren(mrna).size()==2
        Exon exon = featureRelationshipService.getChildForFeature(mrna,Exon.ontologyId)
        CDS cds = featureRelationshipService.getChildForFeature(mrna,CDS.ontologyId)
        assert exon!=null
        assert cds!=null
//        MRNA mrna = featureRelationshipService.getChildForFeature(mrna)

    }

    /**
     * TODO: note, this sequence is for 1.1
     */
//    void "adding a transcript that returns a missing feature location in the mRNA"(){
//
//        given: "a input JSON string"
//        String jsonString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":976735,\"fmax\":995721,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB42183-RA\",\"children\":[{\"location\":{\"fmin\":995216,\"fmax\":995721,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":976735,\"fmax\":976888,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":992139,\"fmax\":992559,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":992748,\"fmax\":993041,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":993307,\"fmax\":995721,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":976735,\"fmax\":995216,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
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
//        String jsonString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":561645,\"fmax\":566383,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB42152-RA\",\"children\":[{\"location\":{\"fmin\":566169,\"fmax\":566383,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":561645,\"fmax\":562692,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":561645,\"fmax\":564771,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":564936,\"fmax\":565087,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":565410,\"fmax\":565655,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":566040,\"fmax\":566383,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":562692,\"fmax\":566169,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
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
////        println returnObject as JSON
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
    
    void "add a transcript with UTR"(){
        
        given: "a valid JSON gtring"
//        String validInputString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":1287738,\"fmax\":1289338,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40717-RA\",\"children\":[{\"location\":{\"fmin\":1289034,\"fmax\":1289338,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1287738,\"fmax\":1288189,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1287738,\"fmax\":1288308,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1288385,\"fmax\":1288491,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1288554,\"fmax\":1288630,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1288942,\"fmax\":1289338,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1288189,\"fmax\":1289034,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String apollo2InputString = "{\"operation\":\"add_transcript\",\"features\":[{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1289338},\"name\":\"GB40717-RA\",\"children\":[{\"location\":{\"fmin\":1289034,\"strand\":-1,\"fmax\":1289338},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1288189},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1288308},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1288385,\"strand\":-1,\"fmax\":1288491},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1288554,\"strand\":-1,\"fmax\":1288630},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1288942,\"strand\":-1,\"fmax\":1289338},\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}}},{\"location\":{\"fmin\":1288189,\"strand\":-1,\"fmax\":1289034},\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}}}],\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}}}],\"track\":\"Annotations-Group1.10\"}"

        String validResponseString ="{\"operation\":\"ADD\",\"sequenceAlterationEvent\":false,\"features\":[{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1289338},\"parent_type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}},\"name\":\"GB40717-RA\",\"children\":[{\"location\":{\"fmin\":1288942,\"strand\":-1,\"fmax\":1289338},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"57ED4B570156EED14312AFB1DC306F2B\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720165,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1288554,\"strand\":-1,\"fmax\":1288630},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"47FBDA96DC15CE1E17E27BD31FB22FE1\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720162,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1287738,\"strand\":-1,\"fmax\":1288308},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"CBDF08580305EE5199AA49E1B69283FC\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720164,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1288189,\"strand\":-1,\"fmax\":1289034},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"8673F11418891E01DF749A5607D5AE36\",\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720162,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"},{\"location\":{\"fmin\":1288385,\"strand\":-1,\"fmax\":1288491},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"30D14467AF67DCBD987DC99B6400F171\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720162,\"parent_id\":\"15495DE1AEB1F224FD04CDB1AF67C166\"}],\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"15495DE1AEB1F224FD04CDB1AF67C166\",\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425585720165,\"parent_id\":\"B74B31812E992AB75B5E741AC6A3158A\"}]}"
        
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

        given: "a input JSON string"
        String jsonString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":219994,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40772-RA\",\"children\":[{\"location\":{\"fmin\":222109,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":220044,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":222081,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":222109,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String exonString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [ {\"uniquename\": \"@TRANSCRIPT_NAME@\"}, {\"location\":{\"fmin\":218197,\"fmax\":218447,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}], \"operation\": \"add_exon\" }"
        String validExonString = "{\"features\":[{\"location\":{\"fmin\":218197,\"strand\":-1,\"fmax\":222245},\"parent_type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}},\"name\":\"GB40772-RA\",\"children\":[{\"location\":{\"fmin\":219994,\"strand\":-1,\"fmax\":222109},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"45F17D57F6025D3508087E86126E2285\",\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499882556,\"parent_id\":\"@TRANSCRIPT_NAME@\"},{\"location\":{\"fmin\":222081,\"strand\":-1,\"fmax\":222245},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"E7C8734B188875CEE3EC78690FE3F656\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499879959,\"parent_id\":\"@TRANSCRIPT_NAME@\"},{\"location\":{\"fmin\":218197,\"strand\":-1,\"fmax\":218447},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"uniquename\":\"5BD40EB6906EE9E744C099A3E9F84163\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499882554,\"parent_id\":\"@TRANSCRIPT_NAME@\"},{\"location\":{\"fmin\":218447,\"strand\":-1,\"fmax\":218447},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"@TRANSCRIPT_NAME@-non_canonical_three_prive_splice_site-218447\",\"type\":{\"name\":\"non_canonical_three_prime_splice_site\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499882557,\"parent_id\":\"@TRANSCRIPT_NAME@\"},{\"location\":{\"fmin\":219994,\"strand\":-1,\"fmax\":220044},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"380947D69FD4CBE555F9AC46596178CD\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499879958,\"parent_id\":\"@TRANSCRIPT_NAME@\"},{\"location\":{\"fmin\":219994,\"strand\":-1,\"fmax\":219994},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"@TRANSCRIPT_NAME@-non_canonical_five_prive_splice_site-219994\",\"type\":{\"name\":\"non_canonical_five_prime_splice_site\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499882556,\"parent_id\":\"@TRANSCRIPT_NAME@\"}],\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"@TRANSCRIPT_NAME@\",\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425499882556,\"parent_id\":\"176745425D3DA350EEFBD5A150554210\"}]}"

        when: "we parse the string"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        then: "we get a valid json object and no features"
        assert Feature.count == 0

        when: "we add the first transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "we should get a transcript back" // we currently get nothing
//        println returnObject as JSON
        assert returnObject.getString('operation')=="ADD"
        assert returnObject.getBoolean('sequenceAlterationEvent')==false
        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1==featuresArray.size()
        JSONObject mrna = featuresArray.getJSONObject(0)
        assert Gene.count==1
        assert MRNA.count==1
        // we are losing an exon somewhere!
        assert Exon.count==2
        assert CDS.count==1
//        assert NonCanonicalFivePrimeSpliceSite.count==1
//        assert NonCanonicalThreePrimeSpliceSite.count==1
//        assert Feature.count == 5
        assert "GB40772-RA-00001"==mrna.getString(FeatureStringEnum.NAME.value)
        String transcriptUniqueName = mrna.getString(FeatureStringEnum.UNIQUENAME.value)
        JSONArray children = mrna.getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 3==children.size()
        for(int i = 0 ; i < 3 ; i++){
            JSONObject codingObject = children.get(i)
            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
            assert locationObject!=null
        }
        

        when: "we parse the string"
        exonString = exonString.replaceAll("@TRANSCRIPT_NAME@",transcriptUniqueName)
        JSONObject exonJsonObject = JSON.parse(exonString) as JSONObject
        JSONObject validExonJsonObject = JSON.parse(validExonString) as JSONObject
//
//        then: "we get a valid json object and no features"
//        assert Feature.count == 7
        
//        when: "we add the exon explicitly"
        JSONObject returnedAfterExonObject = requestHandlingService.addExon(exonJsonObject)

        then: "we should see an exon added"
        assert returnedAfterExonObject!=null
        println Feature.count
        assert Feature.count > 5
        JSONArray returnFeaturesArray = returnedAfterExonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert returnFeaturesArray.size()==1
        JSONObject mRNAObject = returnFeaturesArray.get(0)
        assert mRNAObject.getString(FeatureStringEnum.NAME.value)=="GB40772-RA-00001"
        JSONArray childrenArray = mRNAObject.getJSONArray(FeatureStringEnum.CHILDREN.value)
        def allFeatures = Feature.all
        assert Gene.count==1
        assert MRNA.count==1
        // we are losing an exon somewhere!
        assert Exon.count==3
        assert CDS.count==1
        assert NonCanonicalFivePrimeSpliceSite.count==1
        assert NonCanonicalThreePrimeSpliceSite.count==1
        assert childrenArray.size()==6
        


    }

    void "flip strand on an existing transcript"() {

        given: "a input JSON string"
        String jsonString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":219994,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40772-RA\",\"children\":[{\"location\":{\"fmin\":222109,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":220044,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":222081,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":222109,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String commandString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_NAME@\" } ], \"operation\": \"flip_strand\" }"

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
        println Feature.count
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
        println Feature.count
        assert Feature.count > 5
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
    
    void "delete an entire transcript"(){

        given: "a input JSON string"
        String jsonString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":219994,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40772-RA\",\"children\":[{\"location\":{\"fmin\":222109,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":220044,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":222081,\"fmax\":222245,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":219994,\"fmax\":222109,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String commandString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_NAME@\" } ], \"operation\": \"delete_feature\" }:\n" +
                "Response Headersview source"

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
        // there are just the flanng regions
        assert Feature.count == 4
        JSONArray returnFeaturesArray = returnedAfterExonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert returnFeaturesArray.size() == 0
    }


    
    void "splitting an exon should work and handle CDS calculations properly"(){

        given: "a input JSON string"
        String jsonAddTranscriptString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":202764,\"fmax\":205331,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40806-RA\",\"children\":[{\"location\":{\"fmin\":202764,\"fmax\":203242,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":204576,\"fmax\":205331,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":202764,\"fmax\":205331,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String commandString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [ { \"uniquename\": \"@EXON_NAME@\", \"location\": { \"fmax\": 204749, \"fmin\": 204750 } } ], \"operation\": \"split_exon\" }"
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
        assert sortedExons.size()==2
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
        assert CDS.first().featureLocation.fmin == MRNA.first().featureLocation.fmin
        assert CDS.first().featureLocation.fmax < MRNA.first().featureLocation.fmax


    }
}
