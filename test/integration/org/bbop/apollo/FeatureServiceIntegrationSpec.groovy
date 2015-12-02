package org.bbop.apollo

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class FeatureServiceIntegrationSpec extends AbstractIntegrationSpec{
    
    def featureService
    def bookmarkService
    def projectionService

    def setup() {
        setupDefaultUserOrg()
        projectionService.clearProjections()
//        Sequence sequence = new Sequence(
//                length: 3
//                ,seqChunkSize: 3
//                ,start: 5
//                ,end: 8
//                ,name: "Group1.10"
//        ).save(failOnError: true)
    }

    def cleanup() {
    }

    void "convert JSON to Features"(){

        given: "a set string and existing sequence, when we have a complicated mRNA as JSON"
        String jsonString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235534,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1216850,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1224676,\"fmax\":1224823,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1228682,\"fmax\":1228825,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235237,\"fmax\":1235396,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235487,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1235534,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"

        when: "we parse it"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        Bookmark bookmark = bookmarkService.generateBookmarkForSequence(Sequence.first())

        then: "is is a valid object"
        assert jsonObject!=null
        JSONArray jsonArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert jsonArray.size()==1
        JSONObject mRNAJsonObject = jsonArray.getJSONObject(0)
        JSONArray childArray = jsonArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value)
        assert childArray.size()==7

        when: "we convert it to a feature"
//        Feature feature = featureService.convertJSONToFeature(mRNAJsonObject,Sequence.first())
        Feature feature = featureService.convertJSONToFeature(mRNAJsonObject,bookmark)

        then: "it should convert it to the same feature"
        assert feature!=null
        feature.ontologyId == MRNA.ontologyId

    }
}
