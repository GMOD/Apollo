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

    }

    def cleanup() {
    }

    void "add transcript and view in second-place in the projection"() {

        given: "a transcript"
        projectionService.clearProjections()
        String jsonString = "{\"track\":{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"}], \"label\":\"GroupUn87\"},\"features\":[{\"location\":{\"fmin\":29396,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53498-RA\",\"children\":[{\"location\":{\"fmin\":30271,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29396,\"fmax\":29403,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29927,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29396,\"fmax\":30271,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String getFeaturesString = "{\"track\":{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"operation\":\"get_features\"}"

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


    void "add transcript to second contiguous sequence"(){
        given: "a transcript add operation"
        String addTranscriptString = "{\"track\":{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":[\"Official Gene Set v3.2\"], \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"features\":[{\"location\":{\"fmin\":85051,\"fmax\":85264,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53496-RA\",\"children\":[{\"location\":{\"fmin\":85051,\"fmax\":85264,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":85051,\"fmax\":85264,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String getFeaturesString = "{\"track\":{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"operation\":\"get_features\"}"

        when: "we add the transcript "
        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)
        JSONObject otherReturnObject = requestHandlingService.getFeatures(JSON.parse(getFeaturesString) as JSONObject)
        JSONArray featuresArray = otherReturnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject locationObject = featuresArray.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)


        then: "we should get a gene added to the appropriate space"
        assert featuresArray.size()==1
        assert Gene.count == 1
        assert MRNA.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        MRNA.countByName("GB53496-RA-00001")==1
        assert locationObject.sequence == "GroupUn87"
        assert MRNA.first().featureLocations.first().fmin == 85051 - Sequence.findByName("Group11.4").length - 1
        assert MRNA.first().featureLocations.first().fmax == 85264 - Sequence.findByName("Group11.4").length - 1

        // the features array should be relative to the contiguous sequences
        assert locationObject.fmin == 85051
        assert locationObject.fmax == 85264

    }

//    void "add transcript to second contiguous sequence with folding"(){
//        given: "a transcript add operation"
//        Integer start = 15734
//        Integer end = 15947  // this is what is sent . . . , but this is "exclusive"
//        Integer padding = 0
//        String addTranscriptString = "{\"track\":{\"padding\":${padding}, \"projection\":\"Exon\", \"referenceTrack\":[\"Official Gene Set v3.2\"], \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"features\":[{\"location\":{\"fmin\":${start},\"fmax\":${end},\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53496-RA\",\"children\":[{\"location\":{\"fmin\":${start},\"fmax\":${end},\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
//        String getFeaturesString = "{\"track\":{\"padding\":${padding}, \"projection\":\"Exon\", \"referenceTrack\":[\"Official Gene Set v3.2\"], \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"operation\":\"get_features\"}"
//
//        when: "we add the transcript "
//        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)
//        JSONObject otherReturnObject = requestHandlingService.getFeatures(JSON.parse(getFeaturesString) as JSONObject)
//        JSONArray featuresArray = otherReturnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
//        JSONObject locationObject = featuresArray.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)
//        def features = Feature.all
//        def featureLocations = FeatureLocation.all
//        MRNA firstMRNA = MRNA.first()
//
//
//        then: "we should get a gene added to the appropriate space"
//        assert featuresArray.size()==1
//        assert Gene.count == 1
//        assert MRNA.count == 1
//        assert CDS.count == 1
//        assert Exon.count == 1
//        MRNA.countByName("GB53496-RA-00001")==1
//        assert locationObject.sequence == "GroupUn87"
//        // the features array should be relative to the contiguous sequences
//        // 9966   (shows up as 9967, but this is the stored coordinate)
//        assert 9966 == 85051 - Sequence.findByName("Group11.4").length
//        assert firstMRNA.featureLocations.first().fmin == 85051 - Sequence.findByName("Group11.4").length
//        // 9966 + 213 = 10179 (inclusive), stored this way, returned this way, sent this way
//        assert 10179 == 85264 - Sequence.findByName("Group11.4").length  // add one for exclusive max
//        assert firstMRNA.featureLocations.first().fmax == 85264 - Sequence.findByName("Group11.4").length  // add one for exclusive max
//        assert locationObject.fmin == start
//        assert locationObject.fmax == end
//
//    }
//
//    void "add transcript to second contiguous sequence with folding and padding"(){
//        given: "a transcript add operation"
//        Integer start = 16974
//        Integer end = 17187
//        Integer padding = 20
//        String addTranscriptString = "{\"track\":{\"padding\":${padding}, \"projection\":\"Exon\", \"referenceTrack\":[\"Official Gene Set v3.2\"], \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"features\":[{\"location\":{\"fmin\":${start},\"fmax\":${end},\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53496-RA\",\"children\":[{\"location\":{\"fmin\":${start},\"fmax\":${end},\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
//        String getFeaturesString = "{\"track\":{\"padding\":${padding}, \"projection\":\"Exon\", \"referenceTrack\":[\"Official Gene Set v3.2\"], \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"operation\":\"get_features\"}"
//
//        when: "we add the transcript "
//        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)
//        JSONObject otherReturnObject = requestHandlingService.getFeatures(JSON.parse(getFeaturesString) as JSONObject)
//        JSONArray featuresArray = otherReturnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
//        JSONObject locationObject = featuresArray.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)
//        def features = Feature.all
//        def featureLocations = FeatureLocation.all
//
//
//        then: "we should get a gene added to the appropriate space"
//        assert featuresArray.size()==1
//        assert Gene.count == 1
//        assert MRNA.count == 1
//        assert CDS.count == 1
//        assert Exon.count == 1
//        MRNA.countByName("GB53496-RA-00001")==1
//        assert locationObject.sequence == "GroupUn87"
//        // the features array should be relative to the contiguous sequences
//        assert locationObject.fmin == 16974
//        assert locationObject.fmax == 17187
//
//        assert MRNA.first().featureLocations.first().fmin == 85051 - Sequence.findByName("Group11.4").length - 1
//        assert MRNA.first().featureLocations.first().fmax == 85264 - Sequence.findByName("Group11.4").length - 1
//
//
//
//    }
}
