package org.bbop.apollo

import grails.converters.JSON
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.bbop.apollo.sequence.Strand
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(FeatureService)
@Mock([Sequence, FeatureLocation, Feature,MRNA])
class FeatureServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "convert JSON to Feature Location"() {

        when: "We have a valid json object"
        JSONObject jsonObject = new JSONObject()
        Sequence sequence = new Sequence(name: "Chr3", seqChunkPrefix: "abc", seqChunkSize: 20, start: 1, end: 100, length: 99, sequenceDirectory: "/tmp").save(failOnError: true)
        jsonObject.put(FeatureStringEnum.FMIN.value, 73)
        jsonObject.put(FeatureStringEnum.FMAX.value, 113)
        jsonObject.put(FeatureStringEnum.STRAND.value, Strand.POSITIVE.value)


        then: "We should return a valid FeatureLocation"
        FeatureLocation featureLocation = service.convertJSONToFeatureLocation(jsonObject, sequence)
        assert featureLocation.sequence.name == "Chr3"
        assert featureLocation.fmin == 73
        assert featureLocation.fmax == 113
        assert featureLocation.strand == Strand.POSITIVE.value


    }

    void "convert JSON to Ontology ID"() {
        when: "We hav a json object of type"
        JSONObject json = JSON.parse("{name:exon, cv:{name:sequence}}")

        then: "We should be able to infer the ontology ID"
        String ontologyId = service.convertJSONToOntologyId(json)
        assert ontologyId != null
        assert ontologyId == Exon.ontologyId
    }
    
    void "convert JSON to Features"(){
        
        given: "a set string and existing sequence, when we have a complicated mRNA as JSON"
        String jsonString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235534,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1216850,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1224676,\"fmax\":1224823,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1228682,\"fmax\":1228825,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235237,\"fmax\":1235396,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235487,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1235534,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        Sequence sequence = new Sequence(
                length: 3
                ,refSeqFile: "adsf"
                ,seqChunkPrefix: "asdf"
                ,seqChunkSize: 3
                ,start: 5
                ,end: 8
                ,sequenceDirectory: "asdfadsf"
                ,name: "Group-1.10"
        ).save(failOnError: true)

        when: "we parse it"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        
        then: "is is a valid object"
        assert jsonObject!=null 
        JSONArray jsonArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert jsonArray.size()==1
        JSONObject mRNAJsonObject = jsonArray.getJSONObject(0)
        JSONArray childArray = jsonArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert childArray.size()==7

        when: "we convert it to a feature"
        Feature feature = service.convertJSONToFeature(mRNAJsonObject,sequence)
       
        then: "it should convert it to the same feature"
        assert feature!=null
        feature.ontologyId == MRNA.ontologyId
        
    }
}
