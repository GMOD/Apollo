package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class FeatureProjectionServiceIntegrationSpec extends AbstractIntegrationSpec{
    
    def projectionService
    def requestHandlingService

    def setup() {
        setupDefaultUserOrg()
        projectionService.clearProjections()
    }

    def cleanup() {
    }

    void "add transcript and view in second-place in the projection"() {

        given: "a transcript"
        projectionService.clearProjections()
        String jsonString = "{\"track\":{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"}], \"label\":\"GroupUn87\"},\"features\":[{\"location\":{\"fmin\":29396,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53498-RA\",\"children\":[{\"location\":{\"fmin\":30271,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29396,\"fmax\":29403,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29927,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29396,\"fmax\":30271,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String getFeaturesString = "{\"track\":{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"operation\":\"get_features\"}"
        Organism organism = Organism.first()
        organism.directory = "test/integration/resources/sequences/honeybee-tracks/"
        organism.save(failOnError: true, flush: true)
        new Sequence(
                length: 75085
                , seqChunkSize: 20000
                , start: 0
                , end:  75085
                , organism: organism
                , name: "Group11.4"
        ).save(failOnError: true)

        new Sequence(
                length: 78258
                , seqChunkSize: 20000
                , start: 0
                , end: 78258
                , organism: organism
                , name: "GroupUn87"
        ).save(failOnError: true)


        when: "You add a transcript via JSON"

        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        JSONObject getFeaturesObject = JSON.parse(getFeaturesString) as JSONObject

        then: "there should be no features"
        assert Feature.count == 0
        assert FeatureLocation.count == 0
        assert Sequence.count == 3
        JSONArray mrnaArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == mrnaArray.size()
        assert 4 == getCodingArray(jsonObject).size()



        when: "you parse add a transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "You should see that transcript"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1
        def allFeatures = Feature.all

        // this is the new part
        assert FeatureLocation.count == 5
        assert Feature.count == 5

        JSONArray returnedCodingArray = getCodingArray(returnObject)


        when: "we get the features across multiple sequences"
        JSONObject featureObject = requestHandlingService.getFeatures(getFeaturesObject)

        then: "we should have features in the proper place"
        JSONArray returnedFeatureObject = getCodingArray(featureObject)
        assert 3==returnedFeatureObject.size()
        println "returned feature object ${returnedFeatureObject as JSON}"

        when: "we the get opbject"
        JSONObject aLocation = returnedFeatureObject.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)
        JSONObject bLocation = returnedFeatureObject.getJSONObject(1).getJSONObject(FeatureStringEnum.LOCATION.value)
        JSONObject cLocation = returnedFeatureObject.getJSONObject(2).getJSONObject(FeatureStringEnum.LOCATION.value)

        List<JSONObject> locationObjects = [aLocation,bLocation,cLocation].sort(true){ a,b ->
            a.getInt(FeatureStringEnum.FMIN.value) <=> b.getInt(FeatureStringEnum.FMIN.value) ?: a.getInt(FeatureStringEnum.FMAX.value) <=> b.getInt(FeatureStringEnum.FMAX.value)
        }
        JSONObject firstLocation = locationObjects[0]
        JSONObject secondLocation = locationObjects[1]
        JSONObject thirdLocation = locationObjects[2]


        then: "we should have reasonable locations based on length or previously projected feature arrays . . . "
        assert 1==firstLocation.getInt(FeatureStringEnum.STRAND.value)
        // 29397 + 64197 ??
        // 29397 + 75085 ??
//        assert (29397 + 64197) ==firstLocation.getInt(FeatureStringEnum.FMIN.value)
        assert 29396 + 75085 + 1==firstLocation.getInt(FeatureStringEnum.FMIN.value)
//         29403 + ??
        // 29403 + 75085 ??
//        assert 93600==firstLocation.getInt(FeatureStringEnum.FMAX.value)
        assert 29403 + 75085 + 1 ==firstLocation.getInt(FeatureStringEnum.FMAX.value)

        assert 1==secondLocation.getInt(FeatureStringEnum.STRAND.value)
        // 29928 + 64197
        // 29928 + 75085
        assert 29396 + 75085 + 1==secondLocation.getInt(FeatureStringEnum.FMIN.value)
        // 30329 + 64197
        // 30329 + 75085
//        assert 94526==secondLocation.getInt(FeatureStringEnum.FMAX.value)
        assert 30271 + 75085 + 1 ==secondLocation.getInt(FeatureStringEnum.FMAX.value)

        assert 1==thirdLocation.getInt(FeatureStringEnum.STRAND.value)
        // 29928 + 64197
        // 29928 + 75085
        assert 29928 + 75085==thirdLocation.getInt(FeatureStringEnum.FMIN.value)
        // 30329 + 64197
        // 30329 + 75085
//        assert 94526==thirdLocation.getInt(FeatureStringEnum.FMAX.value)
        assert 30329 + 75085 + 1 ==thirdLocation.getInt(FeatureStringEnum.FMAX.value)

    }
}
