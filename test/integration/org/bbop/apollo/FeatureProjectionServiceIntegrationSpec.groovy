package org.bbop.apollo

import grails.converters.JSON
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Ignore

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
        String addTranscriptString = "{${testCredentials} \"organism\":${Organism.first().id},\"track\":\"GroupUn87\",\"features\":[{\"location\":{\"fmin\":29396,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53498-RA\",\"children\":[{\"location\":{\"fmin\":30271,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29396,\"fmax\":29403,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29927,\"fmax\":30329,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":29396,\"fmax\":30271,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String getFeaturesString = "{${testCredentials} \"track\":{\"id\":6723, \"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"
        String getFeaturesInFeaturesViewString = "{${testCredentials} \"track\":{\"name\":\"GB53498-RA (GroupUn87)\", \"padding\":0, \"start\":29396, \"end\":30329, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":29396, \"end\":30329, \"feature\":{\"name\":\"GB53498-RA\"}}]},\"operation\":\"get_features\"}"

        when: "You add a transcript via JSON"
        JSONObject addTranscriptJsonObject = JSON.parse(addTranscriptString) as JSONObject
        JSONObject getFeaturesObject = JSON.parse(getFeaturesString) as JSONObject

        then: "there should be no features"
        assert Feature.count == 0
        assert FeatureLocation.count == 0
        assert Sequence.count == 3
        JSONArray mrnaArray = addTranscriptJsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == mrnaArray.size()
        assert 4 == getCodingArray(addTranscriptJsonObject).size()



        when: "you parse add a transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(addTranscriptJsonObject)

        then: "You should see that transcript"
        assert Preference.count == 1
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1
        def allFeatures = Feature.all

        // this is the new part
        assert FeatureLocation.count == 5
        assert Feature.count == 5


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
        assert 29396 + 75085 ==firstLocation.getInt(FeatureStringEnum.FMIN.value)
        assert 29403 + 75085  ==firstLocation.getInt(FeatureStringEnum.FMAX.value)

        assert 1==secondLocation.getInt(FeatureStringEnum.STRAND.value)
        assert 29396 + 75085 ==secondLocation.getInt(FeatureStringEnum.FMIN.value)
        assert 30271 + 75085  ==secondLocation.getInt(FeatureStringEnum.FMAX.value)

        assert 1==thirdLocation.getInt(FeatureStringEnum.STRAND.value)
        assert 29928 + 75085-1==thirdLocation.getInt(FeatureStringEnum.FMIN.value)
        assert 30329 + 75085  ==thirdLocation.getInt(FeatureStringEnum.FMAX.value)


        when: "we get the features within the set sequence"
        featureObject = requestHandlingService.getFeatures(JSON.parse(getFeaturesInFeaturesViewString) as JSONObject)
        returnedFeatureObject = getCodingArray(featureObject)
        println "retrieved features object ${featureObject as JSON}"
        println "returned feature object ${returnedFeatureObject as JSON}"
        aLocation = returnedFeatureObject.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)
        bLocation = returnedFeatureObject.getJSONObject(1).getJSONObject(FeatureStringEnum.LOCATION.value)
        cLocation = returnedFeatureObject.getJSONObject(2).getJSONObject(FeatureStringEnum.LOCATION.value)
        locationObjects = [aLocation,bLocation,cLocation].sort(true){ a,b ->
            a.getInt(FeatureStringEnum.FMIN.value) <=> b.getInt(FeatureStringEnum.FMIN.value) ?: a.getInt(FeatureStringEnum.FMAX.value) <=> b.getInt(FeatureStringEnum.FMAX.value)
        }
        firstLocation = locationObjects[0]
        secondLocation = locationObjects[1]
        thirdLocation = locationObjects[2]
        println "first location ${firstLocation as JSON}"
        println "second location ${secondLocation as JSON}"
        println "third location ${thirdLocation as JSON}"
        println "aLocation ${aLocation as JSON}"
        println "bLocation ${bLocation as JSON}"
        println "cLocation ${cLocation as JSON}"
        int overallFmin = 29396
        int overallFmax = 30329

        then: "we should have features in the proper place"
        assert returnedFeatureObject.size()==3
        // should be
        // exon 1 (at transcript start 5')
        assert firstLocation.fmin == 29396-overallFmin
        assert firstLocation.fmax == 29403-overallFmin

        // CDS (at transcript start 5', almost to 5')
        assert secondLocation.fmin == 29396-overallFmin
        assert secondLocation.fmax == 30271-overallFmin

        // exon 2 (at transcript end 3')
        assert thirdLocation.fmin == 29927-overallFmin
        assert thirdLocation.fmax == 30329-overallFmin
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
//        assert locationObject.sequence == "GroupUn87"
//        assert locationObject.sequence == [{"id":1852,"start":0,"name":"Group11.4","length":75085,"end":75085},{"id":1853,"start":0,"name":"GroupUn87","length":78258,"end":78258}]
        assert locationObject.sequence.startsWith("[{")
        assert locationObject.sequence.endsWith("}]")
        assert locationObject.sequence.contains("\"name\":\"Group11.4\"")
        assert locationObject.sequence.contains("\"name\":\"GroupUn87\"")
        assert locationObject.sequence.indexOf("\"name\":\"Group11.4\"") <  locationObject.sequence.indexOf("\"name\":\"GroupUn87\"")
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
        String setExonBoundaryCommand1 = "{ ${testCredentials} \"track\":{\"id\":6688, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":45455,\"fmax\":79565}}],\"operation\":\"set_exon_boundaries\"}"
        String setExonBoundaryCommand2 = "{ ${testCredentials} \"track\":{\"id\":6688, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":45455,\"fmax\":45575}}],\"operation\":\"set_exon_boundaries\"}"
        String getFeaturesString = "{ ${testCredentials} \"track\":{\"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"operation\":\"get_features\"}"
        String getFeaturesStringUn87 = "{ ${testCredentials} \"track\":{\"name\":\"GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"
        String getFeaturesString11_4 = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"operation\":\"get_features\"}"
        String getFeaturesStringReverse = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"

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

        when: "we retrieve features on one Un87"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)


        then: "we should only see locations on Un87"
        assert locationJsonObject.fmin==45455
        assert locationJsonObject.fmax==78258

        when: "we retrieve features on one Group11.4"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesString11_4) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should only see locations on Group11.4"
        assert locationJsonObject.fmin==0
        assert locationJsonObject.fmax==79565-78258

//        when: "we retrieve features on the reverse group"
//        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringReverse) as JSONObject).features
//        JSONObject locationJsonObject1 = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)
//        JSONObject locationJsonObject2 = retrievedFeatures.getJSONObject(1).getJSONObject(FeatureStringEnum.LOCATION.value)
//
//        then: "we should see features on the reverse group"
//        assert retrievedFeatures.size()==2
//        assert locationJsonObject.fmin==0
//        assert locationJsonObject.fmax==79565-78258

        when: "we move the exon boundary BACK across a scaffold"
        setExonBoundaryCommand2 = setExonBoundaryCommand2.replaceAll("@EXON_UNIQUE_NAME@",exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand2) as JSONObject)

        then: "we should have one transcript across two sequences"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==1
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert Exon.first().featureLocations.size()==1
        assert MRNA.first().featureLocations.size()==1
        assert Gene.first().featureLocations.size()==1
        assert CDS.first().featureLocations.size()==1  // is just in the first sequence
        assert FeatureLocation.count==1+1+1+1
        assert mrnaGb53499.featureLocations[0].sequence==sequenceGroupUn87
    }

    void "Add a transcript to the three prime side side and correctly calculate splice sites and then move the exons onto the entire other side"(){

        given: "if we create a transcript in the latter half of a combined scaffold it should not have any non-canonical splice sites"
        // with a front-facing GroupUn87
//        String transcript11_4GB52238 = "{${testCredentials}  \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258},{\"name\":\"Group11.4\",\"start\":0,\"end\":75085}],\"start\":0,\"end\":153343,\"label\":\"GroupUn87::Group11.4\"},\"features\":[{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":88515,\"fmax\":88560,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":90979,\"fmax\":91311,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91491,\"fmax\":91619,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91963,\"fmax\":92630,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":93674,\"fmax\":94485,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":94657,\"fmax\":94735,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":95538,\"fmax\":95744,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96476,\"fmax\":96712,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96819,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
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
        assert mrnaGb53499.featureLocations[0].sequence==sequenceGroupUn87

        when: "we set the exon boundary across a scaffold"
        setExonBoundaryCommand1 = setExonBoundaryCommand1.replaceAll("@EXON_UNIQUE_NAME@",exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand1) as JSONObject)

        then: "we should have one transcript across two sequences"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==1
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
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert FeatureLocation.count==1+1+1+1
        assert mrnaGb53499.featureLocations[0].sequence==sequenceGroup11_4
    }

    void "We can merge transcripts across two scaffolds"() {

        given: "if we create transcripts from two genes and merge them"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String transcript11_4GB52238 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258},{\"name\":\"Group11.4\",\"start\":0,\"end\":75085}],\"start\":0,\"end\":153343,\"label\":\"GroupUn87::Group11.4\"},\"features\":[{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":88515,\"fmax\":88560,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":90979,\"fmax\":91311,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91491,\"fmax\":91619,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91963,\"fmax\":92630,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":93674,\"fmax\":94485,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":94657,\"fmax\":94735,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":95538,\"fmax\":95744,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96476,\"fmax\":96712,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96819,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
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
        assert CDS.first().featureLocations.size()==1
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert FeatureLocation.count==(1+1)*2 + 1  + (1+9) // 2 for each, except for Exon and CDS

    }

    void "I can set the exon boundary on the RHS of a transcript with two exons"(){

        given: "if we create a transcript in the latter half of a combined scaffold it should not have any non-canonical splice sites"
        // with a front-facing GroupUn87
        String transcriptUn87Gb53497 = "{${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"features\":[{\"location\":{\"fmin\":85596,\"fmax\":101804,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53497-RA\",\"children\":[{\"location\":{\"fmin\":85596,\"fmax\":85624,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":101736,\"fmax\":101804,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":85596,\"fmax\":101804,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"

        String setExonBoundaryCommand1 = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":85483,\"fmax\":85624}}],\"operation\":\"set_exon_boundaries\"}"
        String getFeaturesStringUn87 = "{ ${testCredentials} \"track\":{\"name\":\"GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"
        String getFeaturesString11_4 = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"operation\":\"get_features\"}"
        String getFeaturesString = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"
        String getFeaturesStringReverse = "{ ${testCredentials} \"track\":{\"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"operation\":\"get_features\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53497 ) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        MRNA mrnaGb53497 = MRNA.findByName("GB53497-RA-00001")
        String exonUniqueName = Exon.all.sort(){ it.fmin }.first().uniqueName

        then: "we should have a gene  with NO NonCanonical splice sites"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==2
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert FeatureLocation.count==1+1+1+2
        assert mrnaGb53497.featureLocations.size()==1
        assert Gene.first().featureLocations.size() == 1
        assert mrnaGb53497.featureLocations[0].sequence==sequenceGroupUn87
        assert Gene.first().firstFeatureLocation.sequence.name =="GroupUn87"

        when: "we set the exon boundary across a scaffold"
        setExonBoundaryCommand1 = setExonBoundaryCommand1.replaceAll("@EXON_UNIQUE_NAME@",exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand1) as JSONObject)
        JSONArray retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesString) as JSONObject).features
        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should have one transcript across two sequences"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==2
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert Exon.first().featureLocations.size()==1
        assert MRNA.first().featureLocations.size()==1
        assert Gene.first().featureLocations.size()==1
        assert CDS.first().featureLocations.size()==1  // is just in the first sequence
        assert FeatureLocation.count==1+1+1+2
        assert Exon.first().firstFeatureLocation.sequence.name =="GroupUn87"
        assert Exon.last().firstFeatureLocation.sequence.name =="GroupUn87"
        assert CDS.first().firstFeatureLocation.sequence.name =="GroupUn87"
        // should be the same for all
        assert Gene.first().firstFeatureLocation.sequence.name =="GroupUn87"
        assert Gene.first().featureLocations.size() == 1
        assert locationJsonObject.fmin==85483
        assert locationJsonObject.fmax==101804

        when: "we retrieve features on one Un87"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)


        then: "we should only see locations on Un87"
        assert retrievedFeatures.size()==1
        assert locationJsonObject.fmin==10398
        assert locationJsonObject.fmax==26719

        when: "we retrieve features on one Group11.4"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesString11_4) as JSONObject).features

        then: "we should only see locations on Group11.4"
        assert retrievedFeatures.size()==0

        when: "we retrieve features on one Un87::11.4"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringReverse) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)


        then: "we should only see locations on Un87"
        assert retrievedFeatures.size()==1
        assert locationJsonObject.fmin==10398
        assert locationJsonObject.fmax==26719
    }

    void "I can set the exon boundary when in small feature only view mode"(){

        given: "if we create a transcript in the latter half of a combined scaffold it should not have any non-canonical splice sites"
        // with a front-facing GroupUn87
        String transcriptUn87Gb53497 = "{${testCredentials} \"track\":{\"id\":30373, \"name\":\"GroupUn87\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"features\":[{\"location\":{\"fmin\":10511,\"fmax\":26719,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53497-RA\",\"children\":[{\"location\":{\"fmin\":10511,\"fmax\":10539,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":26651,\"fmax\":26719,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":10511,\"fmax\":26719,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String getFeaturesStringUn87 = "{ ${testCredentials} \"track\":{\"id\":30373, \"name\":\"GroupUn87\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"
        Integer bookmarkStart = 100

        String getFeaturesStringUn87InProjection = "{ ${testCredentials} \"track\":{\"name\":\"GB53497-RA (GroupUn87)\", \"padding\":0, \"start\":${bookmarkStart}, \"end\":43810, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":${bookmarkStart}, \"end\":43810, \"feature\":{\"name\":\"GB53497-RA\"}}]},\"operation\":\"get_features\"}"
//        String setExonBoundaryCommand1 = "{ ${testCredentials} \"track\":{\"name\":\"GB53497-RA (GroupUn87)\", \"padding\":0, \"start\":9682, \"end\":26746, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":${bookmarkStart}, \"end\":43810, \"feature\":{\"name\":\"GB53497-RA\"}}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":26651,\"fmax\":26884}}],\"operation\":\"set_exon_boundaries\"}"
        String setExonBoundaryCommand1 = "{ ${testCredentials} \"track\":{\"name\":\"GB53497-RA (GroupUn87)\", \"padding\":0, \"start\":9682, \"end\":26919, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":${bookmarkStart}, \"end\":43810, \"feature\":{\"name\":\"GB53497-RA\"}}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":26651,\"fmax\":26884}}],\"operation\":\"set_exon_boundaries\"}"


        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53497 ) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        MRNA mrnaGb53497 = MRNA.findByName("GB53497-RA-00001")
        String exonUniqueName = Exon.all.sort(){ it.fmin }.last().uniqueName
        def bookmarks = Bookmark.findAllBySequenceListIlike("%GroupUn87%")
        Bookmark bookmark = bookmarks.first()

        then: "we should have a gene  with NO NonCanonical splice sites when getting features on the full scaffold"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==2
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert FeatureLocation.count==1+1+1+2
        assert mrnaGb53497.featureLocations.size()==1
        assert Gene.first().featureLocations.size() == 1
        assert mrnaGb53497.featureLocations[0].sequence==sequenceGroupUn87
        assert Gene.first().firstFeatureLocation.sequence.name =="GroupUn87"
        assert bookmarks.size()==1
        assert bookmark.start == 0
        assert bookmark.end == 78258
        assert bookmark.sequenceList.contains("78258")


        when: "we get features in the full scaffold everything should be the same"
        JSONArray retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87) as JSONObject).features
        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should get them placed within the same locations"
        assert locationJsonObject.fmin==10511
        assert locationJsonObject.fmax==26719

        when: "we get features in the partial scaffold everything should be the same"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87InProjection) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)
        bookmarks = Bookmark.findAllBySequenceListIlike("%GroupUn87%")
        bookmark = Bookmark.findBySequenceListIlikeAndEnd("%GroupUn87%",43810)

        then: "we should get them placed within the same locations"
        assert bookmark!=null
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==2
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==0
        assert Exon.first().featureLocations.size()==1
        assert MRNA.first().featureLocations.size()==1
        assert Gene.first().featureLocations.size()==1
        assert CDS.first().featureLocations.size()==1  // is just in the first sequence
        assert FeatureLocation.count==1+1+1+2
        assert Exon.first().firstFeatureLocation.sequence.name =="GroupUn87"
        assert Exon.last().firstFeatureLocation.sequence.name =="GroupUn87"
        assert CDS.first().firstFeatureLocation.sequence.name =="GroupUn87"
        // should be the same for all
        assert Gene.first().firstFeatureLocation.sequence.name =="GroupUn87"
        assert Gene.first().featureLocations.size() == 1
        assert locationJsonObject.fmin==10511 - bookmarkStart
        assert locationJsonObject.fmax==26719 - bookmarkStart
        assert Bookmark.countBySequenceListIlike("%GroupUn87%")==2
        assert bookmark.start == bookmarkStart
        assert bookmark.end == 43810


        when: "we set the exon boundary within the features"
        setExonBoundaryCommand1 = setExonBoundaryCommand1.replaceAll("@EXON_UNIQUE_NAME@",exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand1) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87InProjection) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should have one transcript across two sequences"
        assert MRNA.count==1
        assert Gene.count==1
        assert CDS.count==1
        assert Exon.count==2
        assert NonCanonicalFivePrimeSpliceSite.count==0
        assert NonCanonicalThreePrimeSpliceSite.count==1
        assert Exon.first().featureLocations.size()==1
        assert MRNA.first().featureLocations.size()==1
        assert Gene.first().featureLocations.size()==1
        assert CDS.first().featureLocations.size()==1  // is just in the first sequence
        assert FeatureLocation.count==1+1+1+2+1
        assert Exon.first().firstFeatureLocation.sequence.name =="GroupUn87"
        assert Exon.last().firstFeatureLocation.sequence.name =="GroupUn87"
        assert CDS.first().firstFeatureLocation.sequence.name =="GroupUn87"
        // should be the same for all
        assert Gene.first().firstFeatureLocation.sequence.name =="GroupUn87"
        assert Gene.first().featureLocations.size() == 1
        assert locationJsonObject.fmin==10511 - bookmarkStart
//        assert locationJsonObject.fmax==26884 - bookmarkStart
        assert locationJsonObject.fmax==26884
        assert Bookmark.countBySequenceListIlike("%GroupUn87%")==2
        assert bookmark.start == bookmarkStart
        assert bookmark.end == 43810

        when: "we retrieve features on one Un87"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)


        then: "we should only see locations on Un87"
        assert retrievedFeatures.size()==1
        assert locationJsonObject.fmin==10511
        assert locationJsonObject.fmax==26884 + bookmarkStart // we retrieve using a different location
        assert Bookmark.countBySequenceListIlike("%GroupUn87%")==2
        assert bookmark.start == bookmarkStart
        assert bookmark.end == 43810


    }


    void "We can view transcripts across two scaffolds and get all feature and trackList data"() {

        given: "if we create transcripts from two genes and merge them"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String transcript11_4GB52238 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258},{\"name\":\"Group11.4\",\"start\":0,\"end\":75085}],\"start\":0,\"end\":153343,\"label\":\"GroupUn87::Group11.4\"},\"features\":[{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":88515,\"fmax\":88560,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":90979,\"fmax\":91311,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91491,\"fmax\":91619,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91963,\"fmax\":92630,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":93674,\"fmax\":94485,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":94657,\"fmax\":94735,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":95538,\"fmax\":95744,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96476,\"fmax\":96712,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96819,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String getFeaturesString = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"

        // TODO: create actual projeciton getFeatures
        String getFeaturesInProjectionString = "{ ${testCredentials} \"track\":{\"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"operation\":\"get_features\"}"

        // get features in projection
//        String getFeaturesStringUn87 = "{ ${testCredentials} \"track\":{\"name\":\"GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"
//        String getFeaturesString11_4 = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"operation\":\"get_features\"}"

        when: "we add two transcripts"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499) as JSONObject)
        requestHandlingService.addTranscript(JSON.parse(transcript11_4GB52238) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        Sequence sequenceGroup11_4 = Sequence.findByName("Group11.4")
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")
        MRNA mrnaGb52238 = MRNA.findByName("GB52238-RA-00001")


        then: "we verify that we have two transcripts, one on each scaffold"
        assert MRNA.count == 2
        assert Gene.count == 2
        assert CDS.count == 2
        assert Exon.count == 1 + 9
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 2 + 2 + 2 + 1 + 9 // one for each
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87
        assert mrnaGb52238.featureLocations[0].sequence == sequenceGroup11_4

        when: "we get all of the features, unprojected from both scaffolds"
        JSONArray retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesString) as JSONObject).features
//        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should see both features"
        assert retrievedFeatures.size()==2

        when: "we get all of the features with projected features"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString) as JSONObject).features
//        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should see both features"
        assert MRNA.count == 2
        assert Gene.count == 2
        assert CDS.count == 2
        assert Exon.count == 1 + 9
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 2 + 2 + 2 + 1 + 9 // one for each
        assert retrievedFeatures.size()==2
    }

    void "Add a transcript add an isoform as well"() {

        given: "if we create transcripts from one gene"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"

        when: "we add one transcript"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")

        then: "we assert that we created one"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87

        when: "we add another one in projection"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499) as JSONObject)
        MRNA mrnaGb53499v2 = MRNA.findByName("GB53499-RA-00002")

        then: "we assert that we created an isoform"
        assert mrnaGb53499v2!=null
        assert Gene.count == 1
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == (1 + 1 + 1)*2 + 1
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87
    }

    // same as above
    void "Add a transcript when projecting around it and add the isoform as well"() {

        given: "if we create transcripts from one gene"
        String transcriptUn87Gb53499InProjection = "{${testCredentials} \"track\":{\"id\":30621, \"name\":\"GB53499-RA (GroupUn87)\", \"padding\":0, \"start\":45455, \"end\":45575, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}}]},\"features\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
//        String getFeaturesString = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"
//        String getFeaturesInProjectionString = "{ ${testCredentials} \"track\":{\"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"operation\":\"get_features\"}"

        when: "we add one transcript"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499InProjection) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")

        then: "we assert that we created one"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87

        when: "we add another one in projection"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499InProjection) as JSONObject)
        MRNA mrnaGb53499v2 = MRNA.findByName("GB53499-RA-00002")

        then: "we assert that we created an isoform"
        assert mrnaGb53499v2!=null
        assert Gene.count == 1
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == (1 + 1 + 1)*2 + 1
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87
    }

    void "We can view transcripts across two scaffolds in a projection and get all feature data"() {

        given: "if we create transcripts from two genes and merge them"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"id\":31085, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String transcript11_4GB52238 = "{${testCredentials} \"track\":{\"id\":31085, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"location\":{\"fmin\":720,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":720,\"fmax\":765,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":3184,\"fmax\":3516,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":3696,\"fmax\":3824,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":4168,\"fmax\":4835,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":5879,\"fmax\":6690,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":6862,\"fmax\":6940,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":7743,\"fmax\":7949,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":8681,\"fmax\":8917,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":9024,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":720,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
//        String getFeaturesString = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"

        // TODO: create actual projeciton getFeatures
        String getFeaturesInProjectionString2 = "{${testCredentials} \"track\":{\"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"operation\":\"get_features\"}"


        when: "we add two transcripts"
        JSONObject addTransriptResponseUn87Gb53499 = requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499) as JSONObject)
        JSONObject addTransriptResponseUn87Gb53499_iso1 = requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499) as JSONObject)
        JSONObject addTransriptResponse11_45Un87Gb2238 = requestHandlingService.addTranscript(JSON.parse(transcript11_4GB52238) as JSONObject)
        JSONObject addTransriptResponse11_45Un87Gb2238_iso1 = requestHandlingService.addTranscript(JSON.parse(transcript11_4GB52238) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        Sequence sequenceGroup11_4 = Sequence.findByName("Group11.4")
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")
        MRNA mrnaGb52238 = MRNA.findByName("GB52238-RA-00001")


        then: "we verify that we have two transcripts, one on each scaffold"
        assert MRNA.count == 4
        assert Gene.count == 2
        assert CDS.count == 4
        assert Exon.count == (1 + 9)*2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 4 + 2 + 4 + (1 + 9)*2 // one for each
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87
        assert mrnaGb52238.featureLocations[0].sequence == sequenceGroup11_4

        when: "we get all of the features with projected features"
        JSONArray retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features
//        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should see both features"
        assert retrievedFeatures.size()==4
//        assert retrievedFeatures.getJSONObject(0).name == mrnaGb53499.name
//        assert retrievedFeatures.getJSONObject(2).name == mrnaGb52238.name
        assert retrievedFeatures.getJSONObject(0).location.fmin >0
        assert retrievedFeatures.getJSONObject(0).location.fmax >0
        assert retrievedFeatures.getJSONObject(1).location.fmin >0
        assert retrievedFeatures.getJSONObject(1).location.fmax >0
        assert retrievedFeatures.getJSONObject(2).location.fmin >0
        assert retrievedFeatures.getJSONObject(2).location.fmax >0
        assert retrievedFeatures.getJSONObject(3).location.fmin >0
        assert retrievedFeatures.getJSONObject(3).location.fmax >0
    }

    /**
     *
     "We can add a transcript in a projection and set the exon boundary on both the 3' and 5' sides"() {
     */
    void "Set exon boundary in projection across"() {

        given: "if we create transcripts from two genes and merge them"
//        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"id\":31085, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        // add within a single projection
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"id\":31510, \"name\":\"GB53499-RA (GroupUn87)\", \"padding\":0, \"start\":45455, \"end\":45575, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}}]},\"features\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
//        String setExonBoundaryUn87Gb53499 = "{ ${testCredentials} \"track\":{\"id\":31240, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME_1@\",\"location\":{\"fmin\":200,\"fmax\":413}}],\"operation\":\"set_exon_boundaries\"}"
        String setExonBoundaryUn87Gb53499 = "{ ${testCredentials} \"track\":{\"id\":31510, \"name\":\"GB53499-RA (GroupUn87)\", \"padding\":0, \"start\":45455, \"end\":45575, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME_1@\",\"location\":{\"fmin\":200,\"fmax\":398}}],\"operation\":\"set_exon_boundaries\"}"

        String setExonBoundaryUn87Gb53499_v2 = "{ ${testCredentials} \"track\":{\"id\":31240, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME_1@\",\"location\":{\"fmin\":200,\"fmax\":500}}],\"operation\":\"set_exon_boundaries\"}"
        String setExonBoundaryUn87Gb53499Across = "{ ${testCredentials} \"track\":{\"id\":31240, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME_1@\",\"location\":{\"fmin\":200,\"fmax\":682}}],\"operation\":\"set_exon_boundaries\"}"

        String transcript11_4GB52238 = "{${testCredentials} \"track\":{\"id\":31085, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"location\":{\"fmin\":720,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":720,\"fmax\":765,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":3184,\"fmax\":3516,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":3696,\"fmax\":3824,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":4168,\"fmax\":4835,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":5879,\"fmax\":6690,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":6862,\"fmax\":6940,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":7743,\"fmax\":7949,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":8681,\"fmax\":8917,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":9024,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":720,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String setExonBoundary11_4GB52238 = "{ ${testCredentials} \"track\":{\"id\":31240, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME_2@\",\"location\":{\"fmin\":632,\"fmax\":765}}],\"operation\":\"set_exon_boundaries\"}"
        String setExonBoundary11_4GB52238ACross = "{ ${testCredentials} \"track\":{\"id\":31240, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME_2@\",\"location\":{\"fmin\":415,\"fmax\":765}}],\"operation\":\"set_exon_boundaries\"}"

        // TODO: create actual projeciton getFeatures
        String getFeaturesInProjectionString2 = "{${testCredentials} \"track\":{\"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"operation\":\"get_features\"}"


        when: "we add one transcript"
        JSONObject addTransriptResponseUn87Gb53499 = requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499) as JSONObject)
        Exon exon1 = Exon.first()
        String exonUniqueName1 = exon1.uniqueName
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        Sequence sequenceGroup11_4 = Sequence.findByName("Group11.4")
        setExonBoundaryUn87Gb53499 = setExonBoundaryUn87Gb53499.replaceAll("@EXON_UNIQUE_NAME_1@",exonUniqueName1)
        setExonBoundaryUn87Gb53499_v2 = setExonBoundaryUn87Gb53499_v2.replaceAll("@EXON_UNIQUE_NAME_1@",exonUniqueName1)
        JSONArray retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features

        then: "we should see the simple transcript"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 4
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87


        when: "we move that exon in projection"
        def returnExon = requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryUn87Gb53499) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features


        then: "it should have worked"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 4
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87
        assert retrievedFeatures.size()==1
        assert retrievedFeatures.getJSONObject(0).location.fmin >0
        assert retrievedFeatures.getJSONObject(0).location.fmax >0

        when: "we add the other exon"
        JSONObject addTransriptResponse11_45Un87Gb2238 = requestHandlingService.addTranscript(JSON.parse(transcript11_4GB52238) as JSONObject)
        List<Exon> exonList = Exon.all.sort(){ it.featureLocations.first().fmin }
        String exonUniqueName2 = exonList.get(0).uniqueName
        setExonBoundaryUn87Gb53499Across = setExonBoundaryUn87Gb53499Across.replaceAll("@EXON_UNIQUE_NAME_1@",exonUniqueName1)
        MRNA mrnaGb52238 = MRNA.findByName("GB52238-RA-00001")
        setExonBoundary11_4GB52238 = setExonBoundary11_4GB52238.replaceAll("@EXON_UNIQUE_NAME_2@",exonUniqueName2)
        setExonBoundary11_4GB52238ACross = setExonBoundary11_4GB52238ACross.replaceAll("@EXON_UNIQUE_NAME_2@",exonUniqueName2)


        then: "we verify that we have two transcripts, one on each scaffold"
        assert MRNA.count == 2
        assert Gene.count == 2
        assert CDS.count == 2
        assert Exon.count == (1 + 9)
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 2 + 2 + 2 + (1 + 9) // one for each
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87
        assert mrnaGb52238.featureLocations[0].sequence == sequenceGroup11_4

        when: "we get all of the features with projected features"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features
//        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should see both features"
        assert retrievedFeatures.size()==2
//        assert retrievedFeatures.getJSONObject(0).name == mrnaGb53499.name
//        assert retrievedFeatures.getJSONObject(1).name == mrnaGb52238.name
        assert retrievedFeatures.getJSONObject(0).location.fmin >0
        assert retrievedFeatures.getJSONObject(0).location.fmax >0
        assert retrievedFeatures.getJSONObject(1).location.fmin >0
        assert retrievedFeatures.getJSONObject(1).location.fmax >0

        when: "we set the exon boundaries on the 5' side"
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryUn87Gb53499_v2) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features

        then: "we should still have valid locations "
        assert retrievedFeatures.getJSONObject(0).location.fmin >0
        assert retrievedFeatures.getJSONObject(0).location.fmax >0
        assert retrievedFeatures.getJSONObject(1).location.fmin >0
        assert retrievedFeatures.getJSONObject(1).location.fmax >0

        when: "we set the exon boundaries on the 3' side"
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundary11_4GB52238) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features

        then: "we should still have valid locations "
        assert retrievedFeatures.getJSONObject(0).location.fmin >0
        assert retrievedFeatures.getJSONObject(0).location.fmax >0
        assert retrievedFeatures.getJSONObject(1).location.fmin >0
        assert retrievedFeatures.getJSONObject(1).location.fmax >0

        when: "we set the exon boundaries on the 5' side across to the 3' side"
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryUn87Gb53499Across) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features

        then: "we should still have valid locations "
        assert retrievedFeatures.getJSONObject(0).location.fmin >0
        assert retrievedFeatures.getJSONObject(0).location.fmax >0
        assert retrievedFeatures.getJSONObject(1).location.fmin >0
        assert retrievedFeatures.getJSONObject(1).location.fmax >0

        when: "we set the exon boundaries on the 3' side across to the 5' side"
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundary11_4GB52238ACross) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features

        then: "we should still have valid locations "
        assert retrievedFeatures.getJSONObject(0).location.fmin >0
        assert retrievedFeatures.getJSONObject(0).location.fmax >0
        assert retrievedFeatures.getJSONObject(1).location.fmin >0
        assert retrievedFeatures.getJSONObject(1).location.fmax >0
    }

    @Ignore
    void "We can merge two transcript in a projection between the 3' and 5' sides"() {

        given: "if we create transcripts from two genes and merge them"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"id\":31085, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String transcript11_4GB52238 = "{${testCredentials} \"track\":{\"id\":31085, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"location\":{\"fmin\":720,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":720,\"fmax\":765,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":3184,\"fmax\":3516,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":3696,\"fmax\":3824,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":4168,\"fmax\":4835,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":5879,\"fmax\":6690,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":6862,\"fmax\":6940,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":7743,\"fmax\":7949,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":8681,\"fmax\":8917,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":9024,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":720,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String mergeExonCommand ="{ ${testCredentials} \"track\": {\"id\":31240, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]}, \"features\": [ { \"uniquename\": \"@EXON_UNIQUE_NAME_1@\" }, { \"uniquename\": \"@EXON_UNIQUE_NAME_2@\" } ], \"operation\": \"merge_transcripts\"}"

        // TODO: create actual projeciton getFeatures
        String getFeaturesInProjectionString2 = "{${testCredentials} \"track\":{\"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"operation\":\"get_features\"}"

        when: "we add two transcripts"
        JSONObject addTransriptResponseUn87Gb53499 = requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499) as JSONObject)
        Exon exon1 = Exon.first()
        String exonUniqueName1 = exon1.uniqueName
        JSONObject addTransriptResponse11_45Un87Gb2238 = requestHandlingService.addTranscript(JSON.parse(transcript11_4GB52238) as JSONObject)
        List<Exon> exonList = Exon.all.sort(){ it.featureLocations.first().fmin }
        String exonUniqueName2 = exonList.get(0).uniqueName
        mergeExonCommand = mergeExonCommand.replaceAll("@EXON_UNIQUE_NAME_1@",exonUniqueName1)
        mergeExonCommand = mergeExonCommand.replaceAll("@EXON_UNIQUE_NAME_2@",exonUniqueName2)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        Sequence sequenceGroup11_4 = Sequence.findByName("Group11.4")
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")
        MRNA mrnaGb52238 = MRNA.findByName("GB52238-RA-00001")


        then: "we verify that we have two transcripts, one on each scaffold"
        assert MRNA.count == 2
        assert Gene.count == 2
        assert CDS.count == 2
        assert Exon.count == (1 + 9)
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 2 + 2 + 2 + (1 + 9) // one for each
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87
        assert mrnaGb52238.featureLocations[0].sequence == sequenceGroup11_4

        when: "we get all of the features with projected features"
        JSONArray retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features

        then: "we should see both features"
        assert retrievedFeatures.size()==2
        assert retrievedFeatures.getJSONObject(0).location.fmin >0
        assert retrievedFeatures.getJSONObject(0).location.fmax >0
        assert retrievedFeatures.getJSONObject(1).location.fmin >0
        assert retrievedFeatures.getJSONObject(1).location.fmax >0

        when: "we set the exon boundaries on the 5' side"
        requestHandlingService.mergeTranscripts(JSON.parse(mergeExonCommand) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features

        then: "we should still have valid locations "
        assert retrievedFeatures.getJSONObject(0).location.fmin >0
        assert retrievedFeatures.getJSONObject(0).location.fmax >0
        assert retrievedFeatures.getJSONObject(1).location.fmin >0
        assert retrievedFeatures.getJSONObject(1).location.fmax >0
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == (1 + 9)
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 2 + 2 + 2 + (1 + 9) // one for each

    }
}
