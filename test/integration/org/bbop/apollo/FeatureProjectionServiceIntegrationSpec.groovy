package org.bbop.apollo

import grails.converters.JSON
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class FeatureProjectionServiceIntegrationSpec extends AbstractIntegrationSpec{
    
    def requestHandlingService

    def setup() {

        setupDefaultUserOrg()

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

    void "add transcript and view in second-place in the projection"() {

        given: "a transcript"
//        String jsonString = "{${testCredentials} \"organism\":${Organism.first().id},\"track\":{\"start\":0,\"end\":${78258+75085},\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"}], \"label\":\"GroupUn87\"},\"features\":[{\"location\":{\"fmin\":29396,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53498-RA\",\"children\":[{\"location\":{\"fmin\":30271,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29396,\"fmax\":29403,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29927,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29396,\"fmax\":30271,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String jsonString = "{${testCredentials} \"organism\":${Organism.first().id},\"track\":\"GroupUn87\",\"features\":[{\"location\":{\"fmin\":29396,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53498-RA\",\"children\":[{\"location\":{\"fmin\":30271,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29396,\"fmax\":29403,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29927,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29396,\"fmax\":30271,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
//        String getFeaturesString = "{${testCredentials} \"organism\":${Organism.first().id},\"track\":{\"start\":0,\"end\":${78258+75085},\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"operation\":\"get_features\"}"
        String getFeaturesString = "{${testCredentials} \"track\":{\"id\":6723, \"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"

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
        assert 29396 + 75085 ==firstLocation.getInt(FeatureStringEnum.FMIN.value)
//         29403 + ??
        // 29403 + 75085 ??
//        assert 93600==firstLocation.getInt(FeatureStringEnum.FMAX.value)
        assert 29403 + 75085  ==firstLocation.getInt(FeatureStringEnum.FMAX.value)

        assert 1==secondLocation.getInt(FeatureStringEnum.STRAND.value)
        // 29928 + 64197
        // 29928 + 75085
        assert 29396 + 75085 ==secondLocation.getInt(FeatureStringEnum.FMIN.value)
        // 30329 + 64197
        // 30329 + 75085
//        assert 94526==secondLocation.getInt(FeatureStringEnum.FMAX.value)
        assert 30271 + 75085  ==secondLocation.getInt(FeatureStringEnum.FMAX.value)

        assert 1==thirdLocation.getInt(FeatureStringEnum.STRAND.value)
        // 29928 + 64197
        // 29928 + 75085
        assert 29928 + 75085-1==thirdLocation.getInt(FeatureStringEnum.FMIN.value)
        // 30329 + 64197
        // 30329 + 75085
//        assert 94526==thirdLocation.getInt(FeatureStringEnum.FMAX.value)
        assert 30329 + 75085  ==thirdLocation.getInt(FeatureStringEnum.FMAX.value)
    }


    void "add transcript to second contiguous sequence"(){
        given: "a transcript add operation"
        String addTranscriptString = "{${testCredentials}\"organism\":${Organism.first().id}, \"track\":{\"start\":0,\"end\":${78258+75085},\"padding\":0, \"projection\":\"None\", \"referenceTrack\":[\"Official Gene Set v3.2\"], \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"features\":[{\"location\":{\"fmin\":85051,\"fmax\":85264,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53496-RA\",\"children\":[{\"location\":{\"fmin\":85051,\"fmax\":85264,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":85051,\"fmax\":85264,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String getFeaturesString = "{${testCredentials} \"organism\":${Organism.first().id},\"track\":{\"start\":0,\"end\":${78258+75085},\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"operation\":\"get_features\"}"

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
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        MRNA.countByName("GB53496-RA-00001")==1
        assert locationObject.sequence == "GroupUn87"
        assert MRNA.first().featureLocations.first().fmin == 85051 - Sequence.findByName("Group11.4").length
        assert MRNA.first().featureLocations.first().fmax == 85264 - Sequence.findByName("Group11.4").length

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

    void "Add a transcript to the three prime side side and correctly calculate splice sites and then set the exon boundaries and return them"(){

        given: "if we create a transcript in the latter half of a combined scaffold it should not have any non-canonical splice sites"
        // with a front-facing GroupUn87
//        String transcript11_4GB52238 = "{${testCredentials}  \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258},{\"name\":\"Group11.4\",\"start\":0,\"end\":75085}],\"start\":0,\"end\":153343,\"label\":\"GroupUn87::Group11.4\"},\"features\":[{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":88515,\"fmax\":88560,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":90979,\"fmax\":91311,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91491,\"fmax\":91619,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91963,\"fmax\":92630,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":93674,\"fmax\":94485,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":94657,\"fmax\":94735,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":95538,\"fmax\":95744,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96476,\"fmax\":96712,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96819,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        // TODO: create proper exon command
        String setExonBoundaryCommand1 = "{ ${testCredentials} \"track\":{\"id\":6688, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":45455,\"fmax\":79565}}],\"operation\":\"set_exon_boundaries\"}"
        String setExonBoundaryCommand2 = "{ ${testCredentials} \"track\":{\"id\":6688, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":45455,\"fmax\":45575}}],\"operation\":\"set_exon_boundaries\"}"
        String getFeaturesString = "{ ${testCredentials} \"track\":{\"id\":17715, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"operation\":\"get_features\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499 ) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
//        Sequence sequenceGroup11_4 = Sequence.findByName("Group11.4")
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")
        String exonUniqueName = Exon.first().uniqueName

        then: "we should have a gene  with NO NonCanonical splice sites"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==1
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert FeatureLocation.count==1+1+1+1
        assert mrnaGb53499.featureLocations[0].sequence==sequenceGroupUn87

        when: "we set the exon boundary across a scaffold"
        setExonBoundaryCommand1 = setExonBoundaryCommand1.replaceAll("@EXON_UNIQUE_NAME@",exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand1) as JSONObject)
        JSONArray retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesString) as JSONObject).features
        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should have one transcript across two sequences"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==1
        // TODO: not sure if this is exactly correct, but one of them should be 0
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert Exon.first().featureLocations.size()==2
        assert MRNA.first().featureLocations.size()==2
        assert Gene.first().featureLocations.size()==2
        assert CDS.first().featureLocations.size()==1  // is just in the first sequence
        assert FeatureLocation.count==(1+1+1)*2 + 1   // same as above , but they are all split into two
        assert Exon.first().featureLocations.sort(){ it.rank }[0].sequence.name =="GroupUn87"
        assert Exon.first().featureLocations.sort(){ it.rank }[1].sequence.name =="Group11.4"
        assert CDS.first().featureLocations[0].sequence.name =="GroupUn87"
        // should be the same for all
        assert Gene.first().featureLocations.sort(){ it.rank }[0].sequence.name =="GroupUn87"
        assert Gene.first().featureLocations.sort(){ it.rank }[1].sequence.name =="Group11.4"
        assert locationJsonObject.fmin==45455
        assert locationJsonObject.fmax==79565

        when: "we move the exon boundary BACK across a scaffold"
        setExonBoundaryCommand2 = setExonBoundaryCommand2.replaceAll("@EXON_UNIQUE_NAME@",exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand2) as JSONObject)

        then: "we should have one transcript across two sequences"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==1
        // TODO: not sure if this is exactly correct, but one of them should be 0
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert FeatureLocation.count==1+1+1+1
        assert mrnaGb53499.featureLocation.sequence==sequenceGroupUn87
    }

    void "Add a transcript to the three prime side side and correctly calculate splice sites and then move the exons onto the entire other side"(){

        given: "if we create a transcript in the latter half of a combined scaffold it should not have any non-canonical splice sites"
        // with a front-facing GroupUn87
//        String transcript11_4GB52238 = "{${testCredentials}  \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258},{\"name\":\"Group11.4\",\"start\":0,\"end\":75085}],\"start\":0,\"end\":153343,\"label\":\"GroupUn87::Group11.4\"},\"features\":[{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":88515,\"fmax\":88560,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":90979,\"fmax\":91311,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91491,\"fmax\":91619,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91963,\"fmax\":92630,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":93674,\"fmax\":94485,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":94657,\"fmax\":94735,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":95538,\"fmax\":95744,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96476,\"fmax\":96712,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96819,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        // TODO: create proper exon command
        String setExonBoundaryCommand1 = "{ ${testCredentials} \"track\":{\"id\":6688, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":45455,\"fmax\":79565}}],\"operation\":\"set_exon_boundaries\"}"
        String setExonBoundaryCommand2 = "{ ${testCredentials} \"track\":{\"id\":6688, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":79500,\"fmax\":79565}}],\"operation\":\"set_exon_boundaries\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499 ) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        Sequence sequenceGroup11_4 = Sequence.findByName("Group11.4")
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")
        String exonUniqueName = Exon.first().uniqueName

        then: "we should have a gene  with NO NonCanonical splice sites"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==1
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert FeatureLocation.count==1+1+1+1
        assert mrnaGb53499.featureLocation.sequence==sequenceGroupUn87

        when: "we set the exon boundary across a scaffold"
        setExonBoundaryCommand1 = setExonBoundaryCommand1.replaceAll("@EXON_UNIQUE_NAME@",exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand1) as JSONObject)

        then: "we should have one transcript across two sequences"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==1
        // TODO: not sure if this is exactly correct, but one of them should be 0
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert Exon.first().featureLocations.size()==2
        assert MRNA.first().featureLocations.size()==2
        assert Gene.first().featureLocations.size()==2
        assert CDS.first().featureLocations.size()==1 // not sure about this
        assert FeatureLocation.count==(1+1+1)*2 + 1  // same as above , but they are all split into two
        assert Exon.first().featureLocations.sort(){ it.rank }[0].sequence.name =="GroupUn87"
        assert Exon.first().featureLocations.sort(){ it.rank }[1].sequence.name =="Group11.4"
        // should be the same for all
        assert Gene.first().featureLocations.sort(){ it.rank }[0].sequence.name =="GroupUn87"
        assert Gene.first().featureLocations.sort(){ it.rank }[1].sequence.name =="Group11.4"

        when: "we move the LHS exon to the other scaffold"
        setExonBoundaryCommand2 = setExonBoundaryCommand2.replaceAll("@EXON_UNIQUE_NAME@",exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand2) as JSONObject)

        then: "we should have one transcript across two sequences"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==1
        // TODO: not sure if this is exactly correct, but one of them should be 0
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert FeatureLocation.count==1+1+1+1
        assert mrnaGb53499.featureLocations[0].sequence==sequenceGroup11_4
    }

    void "We can merge transcripts across two scaffolds"() {

        given: "if we create transcripts from two genes and merge them"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String transcript11_4GB52238 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258},{\"name\":\"Group11.4\",\"start\":0,\"end\":75085}],\"start\":0,\"end\":153343,\"label\":\"GroupUn87::Group11.4\"},\"features\":[{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":88515,\"fmax\":88560,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":90979,\"fmax\":91311,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91491,\"fmax\":91619,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91963,\"fmax\":92630,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":93674,\"fmax\":94485,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":94657,\"fmax\":94735,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":95538,\"fmax\":95744,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96476,\"fmax\":96712,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96819,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        // TODO: create the merge command
        String mergeCommand = "{ ${testCredentials}  \"track\": {\"id\":6688, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]}, \"features\": [ { \"uniquename\": \"@EXON1_UNIQUENAME@\" }, { \"uniquename\": \"@EXON2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\"  }"

        when: "we add two transcripts"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499)as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(transcript11_4GB52238)as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        Sequence sequenceGroup11_4 = Sequence.findByName("Group11.4")
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")
        MRNA mrnaGb52238 = MRNA.findByName("GB52238-RA-00001")


        then: "we verify that we have two transcripts, one on each scaffold"
        assert MRNA.count==2
        assert Gene.count==2
        assert CDS.count==2
        assert Exon.count==1+9
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert FeatureLocation.count==2+2+2+1+9 // one for each
        assert mrnaGb53499.featureLocations[0].sequence==sequenceGroupUn87
        assert mrnaGb52238.featureLocations[0].sequence==sequenceGroup11_4

        when: "we merge the two transcripts"
        mergeCommand = mergeCommand.replaceAll("@EXON1_UNIQUENAME@",mrnaGb52238.uniqueName)
        mergeCommand = mergeCommand.replaceAll("@EXON2_UNIQUENAME@",mrnaGb53499.uniqueName)
        requestHandlingService.mergeTranscripts(JSON.parse(mergeCommand) as JSONObject)

        then: "we should have one transcript across two sequences"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==1+9
        assert MRNA.first().featureLocations.size()==2
        assert Gene.first().featureLocations.size()==2
//        assert CDS.first().featureLocations.size()==2
        // TODO: maybe this is incorrect, might be one after the merge
        assert FeatureLocation.count==(1+1+1)*2 + (1+9) // 2 for each, except for Exon
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0

    }

}
