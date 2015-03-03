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
        Sequence.deleteAll(Sequence.all)
        FeatureRelationship.executeUpdate("delete from FeatureRelationship ")
        FeatureLocation.executeUpdate("delete from FeatureLocation ")
        println "delete Feature: " + Feature.executeUpdate("delete from Feature ")
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
//        println "reurn object ${returnObject}"
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
        println "return Object ${returnObject}"

        
        then: "we should see the appropriate stuff"
//        println "reurn object ${returnObject}"
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
        println "gene ${gene.name}"
        assert featureRelationshipService.getParentForFeature(gene)==null
        assert featureRelationshipService.getChildren(gene).size()==1
        MRNA mrna = featureRelationshipService.getChildForFeature(gene,MRNA.ontologyId)
        assert mrna.id == MRNA.first().id
        List<Feature> childFeatureRelationships =  featureRelationshipService.getParentsForFeature(mrna)
        println "child feature relationships: ${childFeatureRelationships.size()}"
        childFeatureRelationships.each {
            println "parent: ${it.name} ${it.cvTerm} ${it.ontologyId}"
        }
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
}
