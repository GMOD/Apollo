package org.bbop.apollo

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class RequestHandlingServiceIntegrationSpec extends IntegrationSpec {

    def requestHandlingService
    def featureRelationshipService

    def setup() {
        Sequence sequence = new Sequence(
                length: 123600
                ,refSeqFile: "adsf"
                ,seqChunkPrefix: "test-residues1-"
                ,seqChunkSize: 1000
                ,start: 0
                ,end: 123600
                ,sequenceDirectory: "test/integration/resources/sequences"
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

        when: "You add a transcript via JSON"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        then: "there should be no features"
        assert  Feature.count == 0
        assert  FeatureLocation.count == 0
        assert  Sequence.count == 1
        JSONArray mrnaArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1==mrnaArray.size()
        JSONArray codingArray = mrnaArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 7==codingArray.size()



        when: "you parse add a transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)



        then: "You should see that transcript"
        assert  Sequence.count == 1
        // there are 6 exons, but 2 of them overlap . . . so this is correct
        assert  Exon.count == 5
        assert  CDS.count == 1
        assert  MRNA.count == 1
        assert  Gene.count == 1
        assert  Feature.count == 8
        assert  FeatureLocation.count == 8
//        assert "ADD"==returnObject.getString("operation")
//        assert Gene.count == 1
//        assert Gene.first().name=="Bob1"

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
        assert  Feature.count == 4
        assert  FeatureLocation.count == 4
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
    

    void "adding a transcript that returns a missing feature location in the mRNA"(){
        
        given: "a input JSON string"
        String jsonString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":976735,\"fmax\":995721,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB42183-RA\",\"children\":[{\"location\":{\"fmin\":995216,\"fmax\":995721,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":976735,\"fmax\":976888,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":992139,\"fmax\":992559,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":992748,\"fmax\":993041,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":993307,\"fmax\":995721,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":976735,\"fmax\":995216,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"

        when: "we parse the string"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        then: "we get a valid json object and no features"
        assert Feature.count == 0

        when: "we add it to a UTR"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "we should get a transcript back" // we currently get nothing
        assert Feature.count == 7
        assert returnObject.getString('operation')=="ADD"
        assert returnObject.getBoolean('sequenceAlterationEvent')==false
        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1==featuresArray.size()
        JSONObject mrna = featuresArray.getJSONObject(0)
        assert "GB42183-RA-00001"==mrna.getString(FeatureStringEnum.NAME.value)
        JSONArray children = mrna.getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 5==children.size()
        for(int i = 0 ; i < 5 ; i++){
            JSONObject codingObject = children.get(i)
            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
            assert locationObject!=null
        }

    }
    
    void "adding another transcript with UTR fails to add GB42152-RA"(){
        given: "a input JSON string"
        String jsonString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":561645,\"fmax\":566383,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB42152-RA\",\"children\":[{\"location\":{\"fmin\":566169,\"fmax\":566383,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":561645,\"fmax\":562692,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":561645,\"fmax\":564771,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":564936,\"fmax\":565087,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":565410,\"fmax\":565655,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":566040,\"fmax\":566383,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":562692,\"fmax\":566169,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"

        when: "we parse the string"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject

        then: "we get a valid json object and no features"
        assert Feature.count == 0

        when: "we add it to a UTR"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "we should get a transcript back" // we currently get nothing
        assert Feature.count == 7
//        println returnObject as JSON
        assert returnObject.getString('operation')=="ADD"
        assert returnObject.getBoolean('sequenceAlterationEvent')==false
        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1==featuresArray.size()
        JSONObject mrna = featuresArray.getJSONObject(0)
        assert "GB42152-RA-00001"==mrna.getString(FeatureStringEnum.NAME.value)
        JSONArray children = mrna.getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert 5==children.size()
        for(int i = 0 ; i < 5 ; i++){
            JSONObject codingObject = children.get(i)
            JSONObject locationObject = codingObject.getJSONObject(FeatureStringEnum.LOCATION.value)
            assert locationObject!=null
        }

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
        assert Feature.count == 5
//        println returnObject as JSON
        assert returnObject.getString('operation')=="ADD"
        assert returnObject.getBoolean('sequenceAlterationEvent')==false
        JSONArray featuresArray = returnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1==featuresArray.size()
        JSONObject mrna = featuresArray.getJSONObject(0)
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

        then: "we get a valid json object and no features"
        assert Feature.count == 5
        
        when: "we add the exon explicitly"
        JSONObject returnedAfterExonObject = requestHandlingService.addExon(exonJsonObject)

        then: "we should see an exon added"
        assert returnedAfterExonObject!=null
        println Feature.count
        assert Feature.count > 5


    }
    
}
