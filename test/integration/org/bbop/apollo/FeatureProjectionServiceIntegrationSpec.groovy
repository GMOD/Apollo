package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.MultiSequenceProjection
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Ignore

class FeatureProjectionServiceIntegrationSpec extends AbstractIntegrationSpec {

    def requestHandlingService
    def featureService
    def projectionService

    def setup() {

        setupDefaultUserOrg()

        Organism organism = Organism.first()
        organism.directory = "test/integration/resources/sequences/honeybee-tracks/"
        organism.save(failOnError: true, flush: true)
        new Sequence(
                length: 75085
                , seqChunkSize: 20000
                , start: 0
                , end: 75085
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
        assert 3 == returnedFeatureObject.size()
        println "returned feature object ${returnedFeatureObject as JSON}"

        when: "we the get opbject"
        JSONObject aLocation = returnedFeatureObject.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)
        JSONObject bLocation = returnedFeatureObject.getJSONObject(1).getJSONObject(FeatureStringEnum.LOCATION.value)
        JSONObject cLocation = returnedFeatureObject.getJSONObject(2).getJSONObject(FeatureStringEnum.LOCATION.value)

        List<JSONObject> locationObjects = [aLocation, bLocation, cLocation].sort(true) { a, b ->
            a.getInt(FeatureStringEnum.FMIN.value) <=> b.getInt(FeatureStringEnum.FMIN.value) ?: a.getInt(FeatureStringEnum.FMAX.value) <=> b.getInt(FeatureStringEnum.FMAX.value)
        }
        JSONObject firstLocation = locationObjects[0]
        JSONObject secondLocation = locationObjects[1]
        JSONObject thirdLocation = locationObjects[2]


        then: "we should have reasonable locations based on length or previously projected feature arrays . . . "
        assert 1 == firstLocation.getInt(FeatureStringEnum.STRAND.value)
        assert 29396 + 75085 + org.bbop.apollo.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == firstLocation.getInt(FeatureStringEnum.FMIN.value)
        assert 29403 + 75085 + org.bbop.apollo.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == firstLocation.getInt(FeatureStringEnum.FMAX.value)

        assert 1 == secondLocation.getInt(FeatureStringEnum.STRAND.value)
        assert 29396 + 75085 + org.bbop.apollo.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == secondLocation.getInt(FeatureStringEnum.FMIN.value)
        assert 30271 + 75085 + org.bbop.apollo.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == secondLocation.getInt(FeatureStringEnum.FMAX.value)

        assert 1 == thirdLocation.getInt(FeatureStringEnum.STRAND.value)
        assert 29928 + 75085 - 1+ org.bbop.apollo.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH == thirdLocation.getInt(FeatureStringEnum.FMIN.value)
        assert 30329 + 75085 + org.bbop.apollo.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH== thirdLocation.getInt(FeatureStringEnum.FMAX.value)


        when: "we get the features within the set sequence"
        featureObject = requestHandlingService.getFeatures(JSON.parse(getFeaturesInFeaturesViewString) as JSONObject)
        returnedFeatureObject = getCodingArray(featureObject)
        println "retrieved features object ${featureObject as JSON}"
        println "returned feature object ${returnedFeatureObject as JSON}"
        aLocation = returnedFeatureObject.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)
        bLocation = returnedFeatureObject.getJSONObject(1).getJSONObject(FeatureStringEnum.LOCATION.value)
        cLocation = returnedFeatureObject.getJSONObject(2).getJSONObject(FeatureStringEnum.LOCATION.value)
        locationObjects = [aLocation, bLocation, cLocation].sort(true) { a, b ->
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
        assert returnedFeatureObject.size() == 3
        // should be
        // exon 1 (at transcript start 5')
        assert firstLocation.fmin == 29396 - overallFmin
        assert firstLocation.fmax == 29403 - overallFmin

        // CDS (at transcript start 5', almost to 5')
        assert secondLocation.fmin == 29396 - overallFmin
        assert secondLocation.fmax == 30271 - overallFmin

        // exon 2 (at transcript end 3')
        assert thirdLocation.fmin == 29927 - overallFmin
        assert thirdLocation.fmax == 30329 - overallFmin
    }

//    @IgnoreRest
    void "add transcript to second contiguous sequence"() {
        given: "a transcript add operation"
        String addTranscriptString = "{${testCredentials}\"organism\":${Organism.first().id}, \"track\":{\"start\":0,\"end\":${78258 + 75085},\"padding\":0, \"projection\":\"None\", \"referenceTrack\":[\"Official Gene Set v3.2\"], \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"features\":[{\"location\":{\"fmin\":85051,\"fmax\":85264,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53496-RA\",\"children\":[{\"location\":{\"fmin\":85051,\"fmax\":85264,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":85051,\"fmax\":85264,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String getFeaturesString = "{${testCredentials} \"organism\":${Organism.first().id},\"track\":{\"start\":0,\"end\":${78258 + 75085},\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"},{\"name\":\"GroupUn87\"}], \"label\":\"Group11.4::GroupUn87\"},\"operation\":\"get_features\"}"

        when: "we add the transcript "
        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)
        JSONObject otherReturnObject = requestHandlingService.getFeatures(JSON.parse(getFeaturesString) as JSONObject)
        JSONArray featuresArray = otherReturnObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONObject locationObject = featuresArray.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)


        then: "we should get a gene added to the appropriate space"
        assert featuresArray.size() == 1
        assert Gene.count == 1
        assert MRNA.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        MRNA.countByName("GB53496-RA-00001") == 1
//        assert locationObject.sequence == "GroupUn87"
//        assert locationObject.sequence == [{"id":1852,"start":0,"name":"Group11.4","length":75085,"end":75085},{"id":1853,"start":0,"name":"GroupUn87","length":78258,"end":78258}]
        assert locationObject.sequence.startsWith("[{")
        assert locationObject.sequence.endsWith("}]")
        assert locationObject.sequence.contains("\"name\":\"Group11.4\"")
        assert locationObject.sequence.contains("\"name\":\"GroupUn87\"")
        assert locationObject.sequence.indexOf("\"name\":\"Group11.4\"") < locationObject.sequence.indexOf("\"name\":\"GroupUn87\"")
        assert MRNA.first().featureLocations.first().fmin == 85051 - Sequence.findByName("Group11.4").length+ org.bbop.apollo.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert MRNA.first().featureLocations.first().fmax == 85264 - Sequence.findByName("Group11.4").length+ org.bbop.apollo.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH

        // the features array should be relative to the contiguous sequences
        assert locationObject.fmin == 85051+ org.bbop.apollo.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert locationObject.fmax == 85264+ org.bbop.apollo.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH

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

    void "Add a transcript to the three prime side side and correctly calculate splice sites and then set the exon boundaries and return them"() {

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
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
//        Sequence sequenceGroup11_4 = Sequence.findByName("Group11.4")
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")
        String exonUniqueName = Exon.first().uniqueName

        then: "we should have a gene  with NO NonCanonical splice sites"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87

        when: "we set the exon boundary across a scaffold"
        setExonBoundaryCommand1 = setExonBoundaryCommand1.replaceAll("@EXON_UNIQUE_NAME@", exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand1) as JSONObject)
        JSONArray retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesString) as JSONObject).features
        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should have one transcript across two sequences"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert Exon.first().featureLocations.size() == 2
        assert MRNA.first().featureLocations.size() == 2
        assert Gene.first().featureLocations.size() == 2
        assert CDS.first().featureLocations.size() == 1  // is just in the first sequence
        assert FeatureLocation.count == (1 + 1 + 1) * 2 + 1   // same as above , but they are all split into two
        assert Exon.first().featureLocations.sort() { it.rank }[0].sequence.name == "GroupUn87"
        assert Exon.first().featureLocations.sort() { it.rank }[1].sequence.name == "Group11.4"
        assert CDS.first().featureLocations[0].sequence.name == "GroupUn87"
        // should be the same for all
        assert Gene.first().featureLocations.sort() { it.rank }[0].sequence.name == "GroupUn87"
        assert Gene.first().featureLocations.sort() { it.rank }[1].sequence.name == "Group11.4"
        assert locationJsonObject.fmin == 45455
        assert locationJsonObject.fmax == 79565

        when: "we retrieve features on one Un87"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)


        then: "we should only see locations on Un87"
        assert locationJsonObject.fmin == 45455
        assert locationJsonObject.fmax == 78258

        when: "we retrieve features on one Group11.4"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesString11_4) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should only see locations on Group11.4"
        assert locationJsonObject.fmin == 0
        assert locationJsonObject.fmax + org.bbop.apollo.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH== 79565 - 78258

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
        setExonBoundaryCommand2 = setExonBoundaryCommand2.replaceAll("@EXON_UNIQUE_NAME@", exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand2) as JSONObject)

        then: "we should have one transcript across two sequences"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert Exon.first().featureLocations.size() == 1
        assert MRNA.first().featureLocations.size() == 1
        assert Gene.first().featureLocations.size() == 1
        assert CDS.first().featureLocations.size() == 1  // is just in the first sequence
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87
    }

    void "Add a transcript to the three prime side side and correctly calculate splice sites and then move the exons onto the entire other side"() {

        given: "if we create a transcript in the latter half of a combined scaffold it should not have any non-canonical splice sites"
        // with a front-facing GroupUn87
//        String transcript11_4GB52238 = "{${testCredentials}  \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258},{\"name\":\"Group11.4\",\"start\":0,\"end\":75085}],\"start\":0,\"end\":153343,\"label\":\"GroupUn87::Group11.4\"},\"features\":[{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":88515,\"fmax\":88560,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":90979,\"fmax\":91311,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91491,\"fmax\":91619,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91963,\"fmax\":92630,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":93674,\"fmax\":94485,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":94657,\"fmax\":94735,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":95538,\"fmax\":95744,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96476,\"fmax\":96712,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96819,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String setExonBoundaryCommand1 = "{ ${testCredentials} \"track\":{\"id\":6688, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":45455,\"fmax\":79565}}],\"operation\":\"set_exon_boundaries\"}"
        String setExonBoundaryCommand2 = "{ ${testCredentials} \"track\":{\"id\":6688, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":79500,\"fmax\":79565}}],\"operation\":\"set_exon_boundaries\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        Sequence sequenceGroup11_4 = Sequence.findByName("Group11.4")
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")
        String exonUniqueName = Exon.first().uniqueName

        then: "we should have a gene  with NO NonCanonical splice sites"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87

        when: "we set the exon boundary across a scaffold"
        setExonBoundaryCommand1 = setExonBoundaryCommand1.replaceAll("@EXON_UNIQUE_NAME@", exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand1) as JSONObject)

        then: "we should have one transcript across two sequences"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert Exon.first().featureLocations.size() == 2
        assert MRNA.first().featureLocations.size() == 2
        assert Gene.first().featureLocations.size() == 2
        assert CDS.first().featureLocations.size() == 1 // not sure about this
        assert FeatureLocation.count == (1 + 1 + 1) * 2 + 1  // same as above , but they are all split into two
        assert Exon.first().featureLocations.sort() { it.rank }[0].sequence.name == "GroupUn87"
        assert Exon.first().featureLocations.sort() { it.rank }[1].sequence.name == "Group11.4"
        // should be the same for all
        assert Gene.first().featureLocations.sort() { it.rank }[0].sequence.name == "GroupUn87"
        assert Gene.first().featureLocations.sort() { it.rank }[1].sequence.name == "Group11.4"

        when: "we move the LHS exon to the other scaffold"
        setExonBoundaryCommand2 = setExonBoundaryCommand2.replaceAll("@EXON_UNIQUE_NAME@", exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand2) as JSONObject)

        then: "we should have one transcript across two sequences"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroup11_4
    }


    void "I can set the exon boundary on the RHS of a transcript with two exons"() {

        given: "if we create a transcript in the latter half of a combined scaffold it should not have any non-canonical splice sites"
        // with a front-facing GroupUn87
        String transcriptUn87Gb53497 = "{${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"features\":[{\"location\":{\"fmin\":85596,\"fmax\":101804,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53497-RA\",\"children\":[{\"location\":{\"fmin\":85596,\"fmax\":85624,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":101736,\"fmax\":101804,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":85596,\"fmax\":101804,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"

        String setExonBoundaryCommand1 = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":85483,\"fmax\":85624}}],\"operation\":\"set_exon_boundaries\"}"
        String getFeaturesStringUn87 = "{ ${testCredentials} \"track\":{\"name\":\"GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"
        String getFeaturesString11_4 = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"operation\":\"get_features\"}"
        String getFeaturesString = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"
        String getFeaturesStringReverse = "{ ${testCredentials} \"track\":{\"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"operation\":\"get_features\"}"

        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53497) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        MRNA mrnaGb53497 = MRNA.findByName("GB53497-RA-00001")
        String exonUniqueName = Exon.all.sort() { it.fmin }.first().uniqueName

        then: "we should have a gene  with NO NonCanonical splice sites"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 1 + 1 + 1 + 2
        assert mrnaGb53497.featureLocations.size() == 1
        assert Gene.first().featureLocations.size() == 1
        assert mrnaGb53497.featureLocations[0].sequence == sequenceGroupUn87
        assert Gene.first().firstFeatureLocation.sequence.name == "GroupUn87"

        when: "we set the exon boundary across a scaffold"
        setExonBoundaryCommand1 = setExonBoundaryCommand1.replaceAll("@EXON_UNIQUE_NAME@", exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand1) as JSONObject)
        JSONArray retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesString) as JSONObject).features
        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)
        def allFeatures = Feature.all
        def allFeatureLocations = FeatureLocation.all

        then: "we should have one transcript across two sequences"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert Exon.first().featureLocations.size() == 1
        assert MRNA.first().featureLocations.size() == 1
        assert Gene.first().featureLocations.size() == 1
        assert CDS.first().featureLocations.size() == 1  // is just in the first sequence
        assert FeatureLocation.count == 1 + 1 + 1 + 2
        assert Exon.first().firstFeatureLocation.sequence.name == "GroupUn87"
        assert Exon.last().firstFeatureLocation.sequence.name == "GroupUn87"
        assert CDS.first().firstFeatureLocation.sequence.name == "GroupUn87"
        // should be the same for all
        assert Gene.first().firstFeatureLocation.sequence.name == "GroupUn87"
        assert Gene.first().featureLocations.size() == 1
        assert locationJsonObject.fmin == 85483
        assert locationJsonObject.fmax == 101804

        when: "we retrieve features on one Un87"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)


        then: "we should only see locations on Un87"
        assert retrievedFeatures.size() == 1
        assert locationJsonObject.fmin == 10398
        assert locationJsonObject.fmax == 26719

        when: "we retrieve features on one Group11.4"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesString11_4) as JSONObject).features

        then: "we should only see locations on Group11.4"
        assert retrievedFeatures.size() == 0

        when: "we retrieve features on one Un87::11.4"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringReverse) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)


        then: "we should only see locations on Un87"
        assert retrievedFeatures.size() == 1
        assert locationJsonObject.fmin == 10398
        assert locationJsonObject.fmax == 26719
    }

    void "I can set the exon boundary when in small feature only view mode"() {

        given: "if we create a transcript in the latter half of a combined scaffold it should not have any non-canonical splice sites"
        // with a front-facing GroupUn87
        String transcriptUn87Gb53497 = "{${testCredentials} \"track\":{\"id\":30373, \"name\":\"GroupUn87\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"features\":[{\"location\":{\"fmin\":10511,\"fmax\":26719,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53497-RA\",\"children\":[{\"location\":{\"fmin\":10511,\"fmax\":10539,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":26651,\"fmax\":26719,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":10511,\"fmax\":26719,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String getFeaturesStringUn87 = "{ ${testCredentials} \"track\":{\"id\":30373, \"name\":\"GroupUn87\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"operation\":\"get_features\"}"
        Integer assemblageStart = 100

        String getFeaturesStringUn87InProjection = "{ ${testCredentials} \"track\":{\"name\":\"GB53497-RA (GroupUn87)\", \"padding\":0, \"start\":${assemblageStart}, \"end\":43810, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":${assemblageStart}, \"end\":43810, \"feature\":{\"name\":\"GB53497-RA\"}}]},\"operation\":\"get_features\"}"
//        String setExonBoundaryCommand1 = "{ ${testCredentials} \"track\":{\"name\":\"GB53497-RA (GroupUn87)\", \"padding\":0, \"start\":9682, \"end\":26746, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":${assemblageStart}, \"end\":43810, \"feature\":{\"name\":\"GB53497-RA\"}}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":26651,\"fmax\":26884}}],\"operation\":\"set_exon_boundaries\"}"
        String setExonBoundaryCommand1 = "{ ${testCredentials} \"track\":{\"name\":\"GB53497-RA (GroupUn87)\", \"padding\":0, \"start\":9682, \"end\":26919, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":${assemblageStart}, \"end\":43810, \"feature\":{\"name\":\"GB53497-RA\"}}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME@\",\"location\":{\"fmin\":26651,\"fmax\":26884}}],\"operation\":\"set_exon_boundaries\"}"


        when: "we add a transcript"
        requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53497) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        MRNA mrnaGb53497 = MRNA.findByName("GB53497-RA-00001")
        String exonUniqueName = Exon.all.sort() { it.fmin }.last().uniqueName
        def assemblages = Assemblage.findAllBySequenceListIlike("%GroupUn87%")
        Assemblage assemblage = assemblages.first()

        then: "we should have a gene  with NO NonCanonical splice sites when getting features on the full scaffold"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 1 + 1 + 1 + 2
        assert mrnaGb53497.featureLocations.size() == 1
        assert Gene.first().featureLocations.size() == 1
        assert mrnaGb53497.featureLocations[0].sequence == sequenceGroupUn87
        assert Gene.first().firstFeatureLocation.sequence.name == "GroupUn87"
        assert assemblages.size() == 1
        assert assemblage.start == 0
        assert assemblage.end == 78258
        assert assemblage.sequenceList.contains("78258")


        when: "we get features in the full scaffold everything should be the same"
        JSONArray retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87) as JSONObject).features
        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should get them placed within the same locations"
        assert locationJsonObject.fmin == 10511
        assert locationJsonObject.fmax == 26719

        when: "we get features in the partial scaffold everything should be the same"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87InProjection) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)
        assemblages = Assemblage.findAllBySequenceListIlike("%GroupUn87%")
        assemblage = Assemblage.findBySequenceListIlikeAndEnd("%GroupUn87%", 43810)

        then: "we should get them placed within the same locations"
        assert assemblage != null
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert Exon.first().featureLocations.size() == 1
        assert MRNA.first().featureLocations.size() == 1
        assert Gene.first().featureLocations.size() == 1
        assert CDS.first().featureLocations.size() == 1  // is just in the first sequence
        assert FeatureLocation.count == 1 + 1 + 1 + 2
        assert Exon.first().firstFeatureLocation.sequence.name == "GroupUn87"
        assert Exon.last().firstFeatureLocation.sequence.name == "GroupUn87"
        assert CDS.first().firstFeatureLocation.sequence.name == "GroupUn87"
        // should be the same for all
        assert Gene.first().firstFeatureLocation.sequence.name == "GroupUn87"
        assert Gene.first().featureLocations.size() == 1
        assert locationJsonObject.fmin == 10511 - assemblageStart
        assert locationJsonObject.fmax == 26719 - assemblageStart
        assert Assemblage.countBySequenceListIlike("%GroupUn87%") == 2
        assert assemblage.start == assemblageStart
        assert assemblage.end == 43810


        when: "we set the exon boundary within the features"
        setExonBoundaryCommand1 = setExonBoundaryCommand1.replaceAll("@EXON_UNIQUE_NAME@", exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand1) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87InProjection) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should have one transcript across two sequences"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert Exon.first().featureLocations.size() == 1
        assert MRNA.first().featureLocations.size() == 1
        assert Gene.first().featureLocations.size() == 1
        assert CDS.first().featureLocations.size() == 1  // is just in the first sequence
        assert FeatureLocation.count == 1 + 1 + 1 + 2 + 1
        assert Exon.first().firstFeatureLocation.sequence.name == "GroupUn87"
        assert Exon.last().firstFeatureLocation.sequence.name == "GroupUn87"
        assert CDS.first().firstFeatureLocation.sequence.name == "GroupUn87"
        // should be the same for all
        assert Gene.first().firstFeatureLocation.sequence.name == "GroupUn87"
        assert Gene.first().featureLocations.size() == 1
        assert locationJsonObject.fmin == 10511 - assemblageStart
//        assert locationJsonObject.fmax==26884 - assemblageStart
        assert locationJsonObject.fmax == 26884
        assert Assemblage.countBySequenceListIlike("%GroupUn87%") == 2
        assert assemblage.start == assemblageStart
        assert assemblage.end == 43810

        when: "we retrieve features on one Un87"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesStringUn87) as JSONObject).features
        locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)


        then: "we should only see locations on Un87"
        assert retrievedFeatures.size() == 1
        assert locationJsonObject.fmin == 10511
        assert locationJsonObject.fmax == 26884 + assemblageStart // we retrieve using a different location
        assert Assemblage.countBySequenceListIlike("%GroupUn87%") == 2
        assert assemblage.start == assemblageStart
        assert assemblage.end == 43810


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
        assert retrievedFeatures.size() == 2

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
        assert retrievedFeatures.size() == 2
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
        assert mrnaGb53499v2 != null
        assert Gene.count == 1
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == (1 + 1 + 1) * 2 + 1
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
        assert mrnaGb53499v2 != null
        assert Gene.count == 1
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == (1 + 1 + 1) * 2 + 1
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
        assert Exon.count == (1 + 9) * 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 4 + 2 + 4 + (1 + 9) * 2 // one for each
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87
        assert mrnaGb52238.featureLocations[0].sequence == sequenceGroup11_4

        when: "we get all of the features with projected features"
        JSONArray retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features
//        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should see both features"
        assert retrievedFeatures.size() == 4
//        assert retrievedFeatures.getJSONObject(0).name == mrnaGb53499.name
//        assert retrievedFeatures.getJSONObject(2).name == mrnaGb52238.name
        assert retrievedFeatures.getJSONObject(0).location.fmin > 0
        assert retrievedFeatures.getJSONObject(0).location.fmax > 0
        assert retrievedFeatures.getJSONObject(1).location.fmin > 0
        assert retrievedFeatures.getJSONObject(1).location.fmax > 0
        assert retrievedFeatures.getJSONObject(2).location.fmin > 0
        assert retrievedFeatures.getJSONObject(2).location.fmax > 0
        assert retrievedFeatures.getJSONObject(3).location.fmin > 0
        assert retrievedFeatures.getJSONObject(3).location.fmax > 0
    }

    /**
     *
     "We can add a transcript in a projection and set the exon boundary on both the 3' and 5' sides"() {*/
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
        setExonBoundaryUn87Gb53499 = setExonBoundaryUn87Gb53499.replaceAll("@EXON_UNIQUE_NAME_1@", exonUniqueName1)
        setExonBoundaryUn87Gb53499_v2 = setExonBoundaryUn87Gb53499_v2.replaceAll("@EXON_UNIQUE_NAME_1@", exonUniqueName1)
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
        assert retrievedFeatures.size() == 1
        assert retrievedFeatures.getJSONObject(0).location.fmin > 0
        assert retrievedFeatures.getJSONObject(0).location.fmax > 0

        when: "we add the other exon"
        JSONObject addTransriptResponse11_45Un87Gb2238 = requestHandlingService.addTranscript(JSON.parse(transcript11_4GB52238) as JSONObject)
        List<Exon> exonList = Exon.all.sort() { it.featureLocations.first().fmin }
        String exonUniqueName2 = exonList.get(0).uniqueName
        setExonBoundaryUn87Gb53499Across = setExonBoundaryUn87Gb53499Across.replaceAll("@EXON_UNIQUE_NAME_1@", exonUniqueName1)
        MRNA mrnaGb52238 = MRNA.findByName("GB52238-RA-00001")
        setExonBoundary11_4GB52238 = setExonBoundary11_4GB52238.replaceAll("@EXON_UNIQUE_NAME_2@", exonUniqueName2)
        setExonBoundary11_4GB52238ACross = setExonBoundary11_4GB52238ACross.replaceAll("@EXON_UNIQUE_NAME_2@", exonUniqueName2)


        then: "we verify that we have two transcripts, one on each scaffold"
        assert MRNA.count == 2
        assert Gene.count == 2
        assert CDS.count == 2
        assert Exon.count == (1 + 9)
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == 2 + 2 + 2 + (1 + 9) // one for each, was 19 (
        assert mrnaGb53499.featureLocations[0].sequence == sequenceGroupUn87
        assert mrnaGb52238.featureLocations[0].sequence == sequenceGroup11_4

        when: "we get all of the features with projected features"
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features
//        JSONObject locationJsonObject = retrievedFeatures.getJSONObject(0).getJSONObject(FeatureStringEnum.LOCATION.value)

        then: "we should see both features"
        assert retrievedFeatures.size() == 2
//        assert retrievedFeatures.getJSONObject(0).name == mrnaGb53499.name
//        assert retrievedFeatures.getJSONObject(1).name == mrnaGb52238.name
        assert retrievedFeatures.getJSONObject(0).location.fmin > 0
        assert retrievedFeatures.getJSONObject(0).location.fmax > 0
        assert retrievedFeatures.getJSONObject(1).location.fmin > 0
        assert retrievedFeatures.getJSONObject(1).location.fmax > 0

        when: "we set the exon boundaries on the 5' side"
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryUn87Gb53499_v2) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features

        then: "we should still have valid locations "
        assert retrievedFeatures.getJSONObject(0).location.fmin > 0
        assert retrievedFeatures.getJSONObject(0).location.fmax > 0
        assert retrievedFeatures.getJSONObject(1).location.fmin > 0
        assert retrievedFeatures.getJSONObject(1).location.fmax > 0

        when: "we set the exon boundaries on the 3' side"
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundary11_4GB52238) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features

        then: "we should still have valid locations "
        assert retrievedFeatures.getJSONObject(0).location.fmin > 0
        assert retrievedFeatures.getJSONObject(0).location.fmax > 0
        assert retrievedFeatures.getJSONObject(1).location.fmin > 0
        assert retrievedFeatures.getJSONObject(1).location.fmax > 0

        when: "we set the exon boundaries on the 5' side across to the 3' side"
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryUn87Gb53499Across) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features
        def mrnas = MRNA.all

        then: "we should still have valid locations "
        assert MRNA.count == 2
        assert Gene.count == 2
        assert CDS.count == 2
        assert Exon.count == (1 + 9)
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
//        assert FeatureLocation.count == 2 + 2 + 2 + (1 + 9) // one for each
        assert retrievedFeatures.size() == 2
        assert retrievedFeatures.getJSONObject(0).location.fmin == 632
        assert retrievedFeatures.getJSONObject(0).location.fmax == 9059
        assert retrievedFeatures.getJSONObject(1).location.fmin == 200 // 45455 - 45255
        assert retrievedFeatures.getJSONObject(1).location.fmax == 682 //

        when: "we set the exon boundaries on the 3' side across to the 5' side"
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundary11_4GB52238ACross) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features

        then: "we should still have valid locations "
        assert MRNA.count == 2
        assert Gene.count == 2
        assert CDS.count == 2
        assert Exon.count == (1 + 9)
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert retrievedFeatures.getJSONObject(0).location.fmin > 0
        assert retrievedFeatures.getJSONObject(0).location.fmax > 0
        assert retrievedFeatures.getJSONObject(1).location.fmin > 0
        assert retrievedFeatures.getJSONObject(1).location.fmax > 0
    }

    @Ignore
    void "We can merge two transcript in a projection between the 3' and 5' sides"() {

        given: "if we create transcripts from two genes and merge them"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"id\":31085, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":200,\"fmax\":320,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String transcript11_4GB52238 = "{${testCredentials} \"track\":{\"id\":31085, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"location\":{\"fmin\":720,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":720,\"fmax\":765,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":3184,\"fmax\":3516,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":3696,\"fmax\":3824,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":4168,\"fmax\":4835,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":5879,\"fmax\":6690,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":6862,\"fmax\":6940,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":7743,\"fmax\":7949,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":8681,\"fmax\":8917,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":9024,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":720,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String mergeExonCommand = "{ ${testCredentials} \"track\": {\"id\":31240, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]}, \"features\": [ { \"uniquename\": \"@TRANSCRIPT_UNIQUE_NAME_1@\" }, { \"uniquename\": \"@TRANSCRIPT_UNIQUE_NAME_2@\" } ], \"operation\": \"merge_transcripts\"}"

        // TODO: create actual projeciton getFeatures
        String getFeaturesInProjectionString2 = "{${testCredentials} \"track\":{\"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"operation\":\"get_features\"}"

        when: "we add two transcripts"
        JSONObject addTransriptResponseUn87Gb53499 = requestHandlingService.addTranscript(JSON.parse(transcriptUn87Gb53499) as JSONObject)
        JSONObject addTransriptResponse11_45Un87Gb2238 = requestHandlingService.addTranscript(JSON.parse(transcript11_4GB52238) as JSONObject)
        Sequence sequenceGroupUn87 = Sequence.findByName("GroupUn87")
        Sequence sequenceGroup11_4 = Sequence.findByName("Group11.4")
        MRNA mrnaGb53499 = MRNA.findByName("GB53499-RA-00001")
        MRNA mrnaGb52238 = MRNA.findByName("GB52238-RA-00001")
        mergeExonCommand = mergeExonCommand.replaceAll("@TRANSCRIPT_UNIQUE_NAME_1@", mrnaGb53499.uniqueName)
        mergeExonCommand = mergeExonCommand.replaceAll("@TRANSCRIPT_UNIQUE_NAME_2@", mrnaGb52238.uniqueName)


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
        assert retrievedFeatures.size() == 2
        assert retrievedFeatures.getJSONObject(0).location.fmin > 0
        assert retrievedFeatures.getJSONObject(0).location.fmax > 0
        assert retrievedFeatures.getJSONObject(1).location.fmin > 0
        assert retrievedFeatures.getJSONObject(1).location.fmax > 0

        when: "we set the exon boundaries on the 5' side"
        requestHandlingService.mergeTranscripts(JSON.parse(mergeExonCommand) as JSONObject)
        retrievedFeatures = requestHandlingService.getFeatures(JSON.parse(getFeaturesInProjectionString2) as JSONObject).features

        then: "we should still have valid locations "
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == (1 + 9)
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert retrievedFeatures.getJSONObject(0).location.fmin > 0
        assert retrievedFeatures.getJSONObject(0).location.fmax > 0
        assert retrievedFeatures.size() == 1
//        assert retrievedFeatures.getJSONObject(1).location.fmin >0
//        assert retrievedFeatures.getJSONObject(1).location.fmax >0
        // 15 or 16 depending on if CDS has two Feature Locations
        assert FeatureLocation.count == (MRNA.count + Gene.count) * 2 + CDS.count + (Exon.count) // one for each

    }

    void "we can set the longest ORF and set the exon boundary and the CDS is smaller than the transcript before and after"() {

        given: "an add transcript string"
        String addTranscriptString = "{${testCredentials} \"track\":{\"id\":31240, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"location\":{\"fmin\":7743,\"fmax\":7949,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":7743,\"fmax\":7949,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String setExonBoundary = "{${testCredentials} \"track\":{\"id\":31240, \"name\":\"GB53499-RA (GroupUn87)::GB52238-RA (Group11.4)\", \"padding\":0, \"start\":45455, \"end\":64171, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"uniquename\":\"@EXON_UNIQUE_NAME_1@\",\"location\":{\"fmin\":6878,\"fmax\":7949}}],\"operation\":\"set_exon_boundaries\"}"


        when: "we add a transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)
        CDS cds = CDS.first()
        FeatureLocation cdsFeatureLocation = CDS.first().firstFeatureLocation
        FeatureLocation transcriptFeatureLocation = Transcript.first().firstFeatureLocation
        FeatureLocation exonFeatureLocation = Exon.first().firstFeatureLocation
        FeatureLocation geneFeatureLocation = Gene.first().firstFeatureLocation
        Integer preFmin = cdsFeatureLocation.fmin
        Integer preFmax = cdsFeatureLocation.fmax
        setExonBoundary = setExonBoundary.replaceAll("@EXON_UNIQUE_NAME_1@", Exon.first().uniqueName)


        then: "we should see that correct number of components and that the CDS is smaller than the exon and transcript (which should be identical)"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert cds.featureLocations.size() == 1
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert transcriptFeatureLocation.fmin == geneFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == geneFeatureLocation.fmax
        assert transcriptFeatureLocation.fmin == exonFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == exonFeatureLocation.fmax
        assert cdsFeatureLocation.fmin > exonFeatureLocation.fmin
        assert cdsFeatureLocation.fmax < exonFeatureLocation.fmax



        when: "we set the exon boundary"
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundary) as JSONObject)
        cdsFeatureLocation = CDS.first().firstFeatureLocation
        transcriptFeatureLocation = Transcript.first().firstFeatureLocation
        exonFeatureLocation = Exon.first().firstFeatureLocation
        geneFeatureLocation = Gene.first().firstFeatureLocation


        then: "we should see that correct number of components and that the CDS is smaller than the exon and transcript (which should be identical)"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert cds.featureLocations.size() == 1
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert transcriptFeatureLocation.fmin == geneFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == geneFeatureLocation.fmax
        assert transcriptFeatureLocation.fmin == exonFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == exonFeatureLocation.fmax
        assert cdsFeatureLocation.fmin > exonFeatureLocation.fmin
        assert cdsFeatureLocation.fmax < exonFeatureLocation.fmax
        // should have shifted and not stayed the same
        assert preFmin != cdsFeatureLocation.fmin
        assert preFmax != cdsFeatureLocation.fmax

    }


    void "Add exon to projected transcript"() {
        given: "an add transcript string"
        String addTranscriptString = "{${testCredentials} \"track\":{\"id\":31239, \"name\":\"GB52238-RA (Group11.4)\", \"padding\":0, \"start\":10257, \"end\":18596, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]},\"features\":[{\"location\":{\"fmin\":8161,\"fmax\":8397,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":8161,\"fmax\":8397,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String addExonString = "{${testCredentials}  \"track\": {\"id\":31239, \"name\":\"GB52238-RA (Group11.4)\", \"padding\":0, \"start\":10257, \"end\":18596, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}}]}, \"features\": [ {\"uniquename\": \"@TRANSCRIPT_UNIQUE_NAME_1@\"}, {\"location\":{\"fmin\":8504,\"fmax\":8539,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}], \"operation\": \"add_exon\"}"


        when: "we add a transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)
        CDS cds = CDS.first()
        FeatureLocation cdsFeatureLocation = CDS.first().firstFeatureLocation
        FeatureLocation transcriptFeatureLocation = Transcript.first().firstFeatureLocation
        FeatureLocation exonFirstFeatureLocation = Exon.first().firstFeatureLocation
        FeatureLocation geneFeatureLocation = Gene.first().firstFeatureLocation
        Integer preFmin = cdsFeatureLocation.fmin
        Integer preFmax = cdsFeatureLocation.fmax
        addExonString = addExonString.replaceAll("@TRANSCRIPT_UNIQUE_NAME_1@", MRNA.first().uniqueName)


        then: "we should see that correct number of components and that the CDS is smaller than the exon and transcript (which should be identical)"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert cds.featureLocations.size() == 1
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert transcriptFeatureLocation.fmin == geneFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == geneFeatureLocation.fmax
        assert transcriptFeatureLocation.fmin == exonFirstFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == exonFirstFeatureLocation.fmax
        assert cdsFeatureLocation.fmin > exonFirstFeatureLocation.fmin
        assert cdsFeatureLocation.fmax == exonFirstFeatureLocation.fmax



        when: "we set the exon boundary"
        requestHandlingService.addExon(JSON.parse(addExonString) as JSONObject)
        cdsFeatureLocation = CDS.first().firstFeatureLocation
        transcriptFeatureLocation = Transcript.first().firstFeatureLocation
//        exonFeatureLocation = Exon.first().firstFeatureLocation
        geneFeatureLocation = Gene.first().firstFeatureLocation
        List<Exon> exons = Exon.all.sort() { a, b ->
            a.firstFeatureLocation.fmin <=> b.firstFeatureLocation.fmin
        }
        exonFirstFeatureLocation = exons.first().firstFeatureLocation
        FeatureLocation exonLastFeatureLocation = exons.last().firstFeatureLocation


        then: "we should see that correct number of components and that the CDS is smaller than the exon and transcript (which should be identical)"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 2
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert cds.featureLocations.size() == 1
        assert cds.firstFeatureLocation.fmin > 0
        assert cds.firstFeatureLocation.fmax > 0
        assert cds.firstFeatureLocation.fmin < cds.firstFeatureLocation.fmax
        assert FeatureLocation.count == 1 + 1 + 1 + 2  // each exon should have only one
        assert transcriptFeatureLocation.fmin == geneFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == geneFeatureLocation.fmax
        assert transcriptFeatureLocation.fmin == exonFirstFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == exonLastFeatureLocation.fmax
        assert cdsFeatureLocation.fmin > exonFirstFeatureLocation.fmin
        assert cdsFeatureLocation.fmax < exonLastFeatureLocation.fmax
        // should have shifted and not stayed the same

    }

//    @IgnoreRest
    void "we can add exons across scaffolds left to right"() {
        given: "an add transcript string"
        String addTranscriptString = "{${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"features\":[{\"location\":{\"fmin\":53392,\"fmax\":56055,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52239-RA\",\"children\":[{\"location\":{\"fmin\":53392,\"fmax\":56055,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String addExonString = "{${testCredentials} \"track\": {\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]}, \"features\": [ {\"uniquename\": \"@TRANSCRIPT_UNIQUE_NAME_1@\"}, {\"location\":{\"fmin\":85596,\"fmax\":85624,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}], \"operation\": \"add_exon\"}"

        when: "we add a transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)
        CDS cds = CDS.first()
        FeatureLocation cdsFirstFeatureLocation = CDS.first().firstFeatureLocation
        FeatureLocation transcriptFeatureLocation = Transcript.first().firstFeatureLocation
        FeatureLocation exonFirstFeatureLocation = Exon.first().firstFeatureLocation
        FeatureLocation geneFeatureLocation = Gene.first().firstFeatureLocation
        Integer preFmin = cdsFirstFeatureLocation.fmin
        Integer preFmax = cdsFirstFeatureLocation.fmax
        addExonString = addExonString.replaceAll("@TRANSCRIPT_UNIQUE_NAME_1@", MRNA.first().uniqueName)


        then: "we should see that correct number of components and that the CDS is smaller than the exon and transcript (which should be identical)"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert cds.featureLocations.size() == 1
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert transcriptFeatureLocation.fmin == geneFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == geneFeatureLocation.fmax
        assert transcriptFeatureLocation.fmin == exonFirstFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == exonFirstFeatureLocation.fmax
        assert cdsFirstFeatureLocation.fmin > exonFirstFeatureLocation.fmin
        assert cdsFirstFeatureLocation.fmax == exonFirstFeatureLocation.fmax



        when: "we add the exon"
        requestHandlingService.addExon(JSON.parse(addExonString) as JSONObject)
        def cdses = CDS.all
        cdsFirstFeatureLocation = CDS.first().firstFeatureLocation
        FeatureLocation cdsLastFeatureLocation = CDS.first().lastFeatureLocation
        transcriptFeatureLocation = Transcript.first().firstFeatureLocation
        FeatureLocation transcriptLastFeatureLocation = Transcript.last().lastFeatureLocation
        geneFeatureLocation = Gene.first().firstFeatureLocation
        FeatureLocation geneLastFeatureLocation = Gene.first().lastFeatureLocation
        def map = [:]
        Transcript.first().featureLocations.sort() { a, b ->
            a.rank <=> b.rank
        }.eachWithIndex { FeatureLocation it, int index ->
            map.put(it.sequence.name, index)
        }


        List<Exon> exons = Exon.all.sort() { a, b ->
            map.get(a.firstFeatureLocation.sequence.name) <=> map.get(b.firstFeatureLocation.sequence.name)
        }
        exonFirstFeatureLocation = exons.first().firstFeatureLocation
        FeatureLocation exonLastFeatureLocation = exons.last().firstFeatureLocation


        then: "we should see that correct number of components and that the CDS is smaller than the exon and transcript (which should be identical)"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 2
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert CDS.first().featureLocations.size() == 2
        assert cdsFirstFeatureLocation.fmin == 55798
//        assert cdsFirstFeatureLocation.fmax == 56055
        assert cdsFirstFeatureLocation.fmax == cdsFirstFeatureLocation.sequence.length
//        assert cdsFirstFeatureLocation.fmin == 55798
//        assert cdsFirstFeatureLocation.fmax == cdsFirstFeatureLocation.sequence.length
        assert cdsLastFeatureLocation.fmin == 0
        assert cdsLastFeatureLocation.fmax == 85618 - cdsFirstFeatureLocation.sequence.length
        assert FeatureLocation.count == (MRNA.count + Gene.count + CDS.count) * 2 + Exon.count + NonCanonicalFivePrimeSpliceSite.count
        assert transcriptFeatureLocation.fmin == geneFeatureLocation.fmin
        assert transcriptLastFeatureLocation.fmax == geneLastFeatureLocation.fmax
        assert transcriptFeatureLocation.fmin == exonFirstFeatureLocation.fmin
        assert transcriptLastFeatureLocation.fmax == exonLastFeatureLocation.fmax
        assert cdsFirstFeatureLocation.fmin > exonFirstFeatureLocation.fmin
        // last exon only exists on the last sequence, so fmax only represents that sequence
        assert cdsFirstFeatureLocation.fmax < exonLastFeatureLocation.fmax + exonFirstFeatureLocation.sequence.length
        assert cdsFirstFeatureLocation.fmin > 0
        assert cdsFirstFeatureLocation.fmax > 0
        assert cdsFirstFeatureLocation.fmin < cdsFirstFeatureLocation.fmax

        assert cdsLastFeatureLocation.fmax < exonLastFeatureLocation.fmax + exonFirstFeatureLocation.sequence.length
        assert cdsLastFeatureLocation.fmin == 0
        assert cdsLastFeatureLocation.fmax > 0
        assert cdsLastFeatureLocation.fmin < cdsLastFeatureLocation.fmax
//        assert preFmin != cdsFeatureLocation.fmin
//        assert preFmax != cdsFeatureLocation.fmax
        // should have shifted and not stayed the same
    }

//    @IgnoreRest
    void "we can add exons across scaffolds in a projection left to right"() {
        given: "an add transcript string"
        String addTranscriptString = "{${testCredentials} \"track\":{\"id\":31285, \"name\":\"GB52238-RA (Group11.4)::GB53499-RA (GroupUn87)\", \"padding\":0, \"start\":10257, \"end\":64171, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}},{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}}]},\"features\":[{\"location\":{\"fmin\":8504,\"fmax\":8539,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":8504,\"fmax\":8539,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String addExonString = "{${testCredentials} \"track\": {\"id\":31285, \"name\":\"GB52238-RA (Group11.4)::GB53499-RA (GroupUn87)\", \"padding\":0, \"start\":10257, \"end\":64171, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}},{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}}]}, \"features\": [ {\"uniquename\": \"@TRANSCRIPT_UNIQUE_NAME_1@\"}, {\"location\":{\"fmin\":8939,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}], \"operation\": \"add_exon\"}"

        when: "we add a transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(addTranscriptString) as JSONObject)
        CDS cds = CDS.first()
        FeatureLocation cdsFeatureLocation = CDS.first().firstFeatureLocation
        FeatureLocation transcriptFeatureLocation = Transcript.first().firstFeatureLocation
        FeatureLocation exonFirstFeatureLocation = Exon.first().firstFeatureLocation
        FeatureLocation geneFeatureLocation = Gene.first().firstFeatureLocation
        Integer preFmin = cdsFeatureLocation.fmin
        Integer preFmax = cdsFeatureLocation.fmax
        addExonString = addExonString.replaceAll("@TRANSCRIPT_UNIQUE_NAME_1@", MRNA.first().uniqueName)


        then: "we should see that correct number of components and that the CDS is smaller than the exon and transcript (which should be identical)"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert cds.featureLocations.size() == 1
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert transcriptFeatureLocation.fmin == geneFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == geneFeatureLocation.fmax
        assert transcriptFeatureLocation.fmin == exonFirstFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == exonFirstFeatureLocation.fmax
//        assert cdsFeatureLocation.fmin == exonFirstFeatureLocation.fmin
//        assert cdsFeatureLocation.fmax < exonFirstFeatureLocation.fmax


        when: "we add the exon"
        requestHandlingService.addExon(JSON.parse(addExonString) as JSONObject)
        cdsFeatureLocation = CDS.first().firstFeatureLocation
        transcriptFeatureLocation = Transcript.first().firstFeatureLocation
        FeatureLocation transcriptLastFeatureLocation = Transcript.last().lastFeatureLocation
        geneFeatureLocation = Gene.first().firstFeatureLocation
        FeatureLocation geneLastFeatureLocation = Gene.first().lastFeatureLocation
        def map = [:]
        Transcript.first().featureLocations.sort() { a, b ->
            a.rank <=> b.rank
        }.eachWithIndex { FeatureLocation it, int index ->
            map.put(it.sequence.name, index)
        }


        List<Exon> exons = Exon.all.sort() { a, b ->
            map.get(a.firstFeatureLocation.sequence.name) <=> map.get(b.firstFeatureLocation.sequence.name)
        }
        exonFirstFeatureLocation = exons.first().firstFeatureLocation
        FeatureLocation exonLastFeatureLocation = exons.last().lastFeatureLocation
        def allFeatures = Feature.all
        def allFeatureLoations = FeatureLocation.all


        then: "we should see that correct number of components and that the CDS is smaller than the exon and transcript (which should be identical)"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 2
        assert MRNA.first().featureLocations.size() == 2
        assert Gene.first().featureLocations.size() == 2
        assert Exon.first().featureLocations.size() == 1
        assert Exon.last().featureLocations.size() == 1
        assert CDS.first().featureLocations.size() == 2
        assert cdsFeatureLocation.fmin > 0
        assert cdsFeatureLocation.fmax > 0
        assert cdsFeatureLocation.fmin < cdsFeatureLocation.fmax


        assert transcriptFeatureLocation.fmin == 18561
        assert transcriptFeatureLocation.fmax == 75085
        assert transcriptLastFeatureLocation.fmin == 0
        assert transcriptLastFeatureLocation.fmax == 45575
        assert exonFirstFeatureLocation.fmin == geneFeatureLocation.fmin
        assert geneFeatureLocation.fmin == 18561
        assert geneFeatureLocation.fmax == 75085
        assert geneLastFeatureLocation.fmin == 0
        assert geneLastFeatureLocation.fmax == 45575
        assert exonFirstFeatureLocation.fmin == geneFeatureLocation.fmin
        assert exonFirstFeatureLocation.fmax < geneFeatureLocation.fmax
        assert exonFirstFeatureLocation.sequence == geneFeatureLocation.sequence
        assert exonLastFeatureLocation.fmin > geneLastFeatureLocation.fmin
        assert exonLastFeatureLocation.fmax == geneLastFeatureLocation.fmax
        assert exonLastFeatureLocation.sequence == geneLastFeatureLocation.sequence

        assert transcriptFeatureLocation.fmin == geneFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == geneFeatureLocation.fmax
        assert transcriptLastFeatureLocation.fmin == geneLastFeatureLocation.fmin
        assert transcriptLastFeatureLocation.fmax == geneLastFeatureLocation.fmax

        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 2

        assert FeatureLocation.count == (MRNA.count + Gene.count + CDS.count) * 2 + Exon.count + NonCanonicalFivePrimeSpliceSite.count + NonCanonicalThreePrimeSpliceSite.count
//        assert cdsFeatureLocation.fmin > exonFirstFeatureLocation.fmin
//        assert cdsFeatureLocation.fmax < exonLastFeatureLocation.fmax
//        assert preFmin != cdsFeatureLocation.fmin
//        assert preFmax != cdsFeatureLocation.fmax
    }

    void "we can add exons across scaffolds in a projection right to left"() {
        given: "an add transcript string"
        String addGB53449TranscriptString = "{${testCredentials} \"track\":{\"id\":35587, \"name\":\"GB52238-RA (Group11.4)::GB53499-RA (GroupUn87)\", \"padding\":0, \"start\":10257, \"end\":64171, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}},{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}}]},\"features\":[{\"location\":{\"fmin\":8939,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":8939,\"fmax\":9059,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String addGB52238ExonString = "{${testCredentials}  \"track\": {\"id\":35587, \"name\":\"GB52238-RA (Group11.4)::GB53499-RA (GroupUn87)\", \"padding\":0, \"start\":10257, \"end\":64171, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}},{\"name\":\"GroupUn87\", \"start\":45255, \"end\":45775, \"feature\":{\"name\":\"GB53499-RA\"}}]}, \"features\": [ {\"uniquename\": \"@TRANSCRIPT_UNIQUE_NAME_1@\"}, {\"location\":{\"fmin\":8504,\"fmax\":8539,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}], \"operation\": \"add_exon\"}"

        when: "we add a transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(addGB53449TranscriptString) as JSONObject)
        CDS cds = CDS.first()
        FeatureLocation cdsFirstFeatureLocation = CDS.first().firstFeatureLocation
        FeatureLocation transcriptFeatureLocation = Transcript.first().firstFeatureLocation
        FeatureLocation exonFirstFeatureLocation = Exon.first().firstFeatureLocation
        FeatureLocation geneFeatureLocation = Gene.first().firstFeatureLocation
        Integer preFmin = cdsFirstFeatureLocation.fmin
        Integer preFmax = cdsFirstFeatureLocation.fmax
        addGB52238ExonString = addGB52238ExonString.replaceAll("@TRANSCRIPT_UNIQUE_NAME_1@", MRNA.first().uniqueName)


        then: "we should see that correct number of components and that the CDS is smaller than the exon and transcript (which should be identical)"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert cds.featureLocations.size() == 1
        assert FeatureLocation.count == 1 + 1 + 1 + 1
        assert transcriptFeatureLocation.fmin == geneFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == geneFeatureLocation.fmax
        assert transcriptFeatureLocation.fmin == exonFirstFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == exonFirstFeatureLocation.fmax
//        assert cdsFeatureLocation.fmin == exonFirstFeatureLocation.fmin
//        assert cdsFeatureLocation.fmax < exonFirstFeatureLocation.fmax


        when: "we add the exon"
        requestHandlingService.addExon(JSON.parse(addGB52238ExonString) as JSONObject)
        cdsFirstFeatureLocation = CDS.first().firstFeatureLocation
        transcriptFeatureLocation = Transcript.first().firstFeatureLocation
        FeatureLocation transcriptLastFeatureLocation = Transcript.last().lastFeatureLocation
        cdsFirstFeatureLocation = CDS.first().firstFeatureLocation
        FeatureLocation cdsLastFeatureLocation = CDS.first().lastFeatureLocation
        geneFeatureLocation = Gene.first().firstFeatureLocation
        FeatureLocation geneLastFeatureLocation = Gene.first().lastFeatureLocation
        def map = [:]
        Transcript.first().featureLocations.sort() { a, b ->
            a.rank <=> b.rank
        }.eachWithIndex { FeatureLocation it, int index ->
            map.put(it.sequence.name, index)
        }


        List<Exon> exons = Exon.all.sort() { a, b ->
            map.get(a.firstFeatureLocation.sequence.name) <=> map.get(b.firstFeatureLocation.sequence.name)
        }
        exonFirstFeatureLocation = exons.first().firstFeatureLocation
        FeatureLocation exonLastFeatureLocation = exons.last().lastFeatureLocation
        def allFeatures = Feature.all
        def allFeatureLocations = FeatureLocation.all


        then: "we should see that correct number of components and that the CDS is smaller than the exon and transcript (which should be identical)"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 2
        assert MRNA.first().featureLocations.size() == 2
        assert Gene.first().featureLocations.size() == 2
        assert Exon.first().featureLocations.size() == 1
        assert Exon.last().featureLocations.size() == 1
        assert CDS.first().featureLocations.size() == 2
//        assert cdsFirstFeatureLocation.fmin == 18561
        assert cdsFirstFeatureLocation.fmin >= geneFeatureLocation.fmin
        assert cdsFirstFeatureLocation.fmin == 18596
        assert cdsFirstFeatureLocation.fmax == cdsFirstFeatureLocation.sequence.length
        assert cdsFirstFeatureLocation.fmin < cdsFirstFeatureLocation.fmax
        assert cdsLastFeatureLocation.fmin == 0
        assert cdsLastFeatureLocation.fmax <= geneLastFeatureLocation.fmax
        assert cdsLastFeatureLocation.fmax == 45575


        assert geneFeatureLocation.fmin == 18561
        assert geneFeatureLocation.fmax == 75085
        assert geneLastFeatureLocation.fmin == 0
        assert geneLastFeatureLocation.fmax == 45575
        assert exonFirstFeatureLocation.fmin == geneFeatureLocation.fmin
        assert exonFirstFeatureLocation.fmax < geneFeatureLocation.fmax
        assert exonFirstFeatureLocation.sequence == geneFeatureLocation.sequence
        assert exonLastFeatureLocation.fmin > geneLastFeatureLocation.fmin
        assert exonLastFeatureLocation.fmax == geneLastFeatureLocation.fmax
        assert exonLastFeatureLocation.sequence == geneLastFeatureLocation.sequence

        assert transcriptFeatureLocation.fmin == geneFeatureLocation.fmin
        assert transcriptFeatureLocation.fmax == geneFeatureLocation.fmax
        assert transcriptLastFeatureLocation.fmin == geneLastFeatureLocation.fmin
        assert transcriptLastFeatureLocation.fmax == geneLastFeatureLocation.fmax

        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert NonCanonicalFivePrimeSpliceSite.count == 2

        assert FeatureLocation.count == (MRNA.count + Gene.count + CDS.count) * 2 + Exon.count + NonCanonicalFivePrimeSpliceSite.count + NonCanonicalThreePrimeSpliceSite.count
//        assert cdsFeatureLocation.fmin > exonFirstFeatureLocation.fmin
//        assert cdsFeatureLocation.fmax < exonLastFeatureLocation.fmax
//        assert preFmin != cdsFeatureLocation.fmin
//        assert preFmax != cdsFeatureLocation.fmax
    }

//    @Ignore
    void "We can merge transcripts across two scaffolds in a projetion"() {

        given: "if we create transcripts from two genes and merge them"
        String transcriptUn87Gb53499 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB53499-RA\",\"children\":[{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":45455,\"fmax\":45575,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String transcript11_4GB52238 = "{${testCredentials} \"track\":{\"sequenceList\":[{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258},{\"name\":\"Group11.4\",\"start\":0,\"end\":75085}],\"start\":0,\"end\":153343,\"label\":\"GroupUn87::Group11.4\"},\"features\":[{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":88515,\"fmax\":88560,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":90979,\"fmax\":91311,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91491,\"fmax\":91619,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":91963,\"fmax\":92630,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":93674,\"fmax\":94485,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":94657,\"fmax\":94735,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":95538,\"fmax\":95744,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96476,\"fmax\":96712,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":96819,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":88515,\"fmax\":96854,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String mergeCommand = "{ ${testCredentials}  \"track\": {\"id\":6688, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":78258, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]}, \"features\": [ { \"uniquename\": \"@EXON1_UNIQUENAME@\" }, { \"uniquename\": \"@EXON2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\"  }"

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


        when: "we merge the two transcripts"
        mergeCommand = mergeCommand.replaceAll("@EXON1_UNIQUENAME@", mrnaGb52238.uniqueName)
        mergeCommand = mergeCommand.replaceAll("@EXON2_UNIQUENAME@", mrnaGb53499.uniqueName)
        requestHandlingService.mergeTranscripts(JSON.parse(mergeCommand) as JSONObject)
        def allFeatureLocations = FeatureLocation.all
        def allFeatures = Feature.all

        then: "we should have one transcript across two sequences"
        assert MRNA.count == 1
        assert Gene.count == 1
        assert CDS.count == 1
        assert Exon.count == 1 + 9
        assert MRNA.first().featureLocations.size() == 2
        assert Gene.first().featureLocations.size() == 2
        assert CDS.first().featureLocations.size() == 2
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert FeatureLocation.count == (MRNA.count + Gene.count + CDS.count) * 2 + (1 + 9) // 2 for each, except for Exon and CDS

    }

//    @IgnoreRest
    void "adding transcript that cross a scaffold"() {

        given: "an add transcript string of one exon each from 11.4 (GB52339) and Un87 (GB53947)"
        String addGB52339LastExonGB53947FirstExonTranscriptString = "{ ${testCredentials} \"track\":{\"name\":\"Group11.4::GroupUn87\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085},{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258}]},\"features\":[{\"location\":{\"fmin\":53392,\"fmax\":85624,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"children\":[{\"location\":{\"fmin\":85596,\"fmax\":85624,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":53392,\"fmax\":56055,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"

        when: "we add the transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(addGB52339LastExonGB53947FirstExonTranscriptString))

        then: "we expect to see it across two scaffolds"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert CDS.count == 1
        assert Exon.count == 2
        assert Gene.first().featureLocations.size() == 2
        assert MRNA.first().featureLocations.size() == 2
        assert Exon.first().featureLocations.size() == 1
        assert Exon.last().featureLocations.size() == 1

    }

    void "adding larger transcript across a scaffold"() {

        given: "an add transcript string of full transcripts from 11.4 (GB52339) and Un87 (GB53947)"
        String addGB52339LastExonGB53947FullTranscriptString = "{ ${testCredentials} \"track\":{\"id\":33091, \"name\":\"GB52239-RA (Group11.4)::GB53497-RA (GroupUn87)\", \"padding\":0, \"start\":18905, \"end\":82774, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":18705, \"end\":56255, \"feature\":{\"name\":\"GB52239-RA\"}},{\"name\":\"GroupUn87\", \"start\":10311, \"end\":26919, \"feature\":{\"name\":\"GB53497-RA\"}}]},\"features\":[{\"location\":{\"fmin\":200,\"fmax\":53958,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"children\":[{\"location\":{\"fmin\":34842,\"fmax\":37350,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":200,\"fmax\":328,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":8443,\"fmax\":8861,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":8935,\"fmax\":8986,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":11169,\"fmax\":16432,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":17602,\"fmax\":18004,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":30204,\"fmax\":30359,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":30490,\"fmax\":30678,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":31200,\"fmax\":31230,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":31608,\"fmax\":31711,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":31835,\"fmax\":31997,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":33667,\"fmax\":33900,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":34281,\"fmax\":34493,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":34687,\"fmax\":37350,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":37750,\"fmax\":37778,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":53890,\"fmax\":53958,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"

        when: "we add the transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(addGB52339LastExonGB53947FullTranscriptString))

        then: "we expect to see it across two scaffolds"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert CDS.count == 1
        assert Exon.count == 2 + 13
        assert Gene.first().featureLocations.size() == 2
        assert MRNA.first().featureLocations.size() == 2
        assert Exon.first().featureLocations.size() == 1
        assert Exon.last().featureLocations.size() == 1

    }

    void "convert JSON to Feature Location"() {

        when: "We have a valid json object"
        JSONObject jsonObject = new JSONObject()
        Sequence sequence = new Sequence(
                name: "Chr3",
                seqChunkSize: 20,
                start: 0,
                end: 199,
                length: 200,
                organism: Organism.first()
        ).save(failOnError: true)
        Assemblage assemblage = new Assemblage(
                name: "Assemblage name"
                , sequenceList: "[{\"name\":\"Chr3\",start:0,end:199,length:200}]"
                , organism: Organism.first()
                , start: 0
                , end: 99
        ).save(failOnError: true)
        jsonObject.put(FeatureStringEnum.FMIN.value, 73)
        jsonObject.put(FeatureStringEnum.FMAX.value, 113)
        jsonObject.put(FeatureStringEnum.STRAND.value, org.bbop.apollo.sequence.Strand.POSITIVE.value)
        MultiSequenceProjection projection = projectionService.createMultiSequenceProjection(assemblage)
        org.bbop.apollo.projection.ProjectionSequence projectionSequence = projection.getProjectionSequence("Chr3", Organism.first())


        then: "We should return a valid FeatureLocation"
//        FeatureLocation featureLocation = service.convertJSONToFeatureLocation(jsonObject, sequence,0)
        FeatureLocation featureLocation = featureService.convertJSONToFeatureLocation(jsonObject, projection, projectionSequence)
        assert featureLocation.sequence.name == "Chr3"
        assert featureLocation.fmin == 73
        assert featureLocation.fmax == 113
        assert featureLocation.strand == org.bbop.apollo.sequence.Strand.POSITIVE.value
    }

    void "make intron in 3 prime"() {

        given: "add transcript and make intron string"
        String addTranscriptGb52239BigExonString = "{ ${testCredentials} \"track\":{\"id\":27979, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":108132,\"fmax\":113395,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52239-RA\",\"children\":[{\"location\":{\"fmin\":108132,\"fmax\":113395,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String makeIntronString = "{ ${testCredentials} \"track\": {\"id\":27979, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]}, \"features\": [ { \"uniquename\": \"@EXON_UNIQUE_NAME_1@\", \"location\": { \"fmin\": 111258 } } ], \"operation\": \"make_intron\" }"

        when: "we add the transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptGb52239BigExonString))

        then: "we should have a single exon transcript"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 1
        assert CDS.count == 1

        when: "we make an intron in the middle of one"
        makeIntronString = makeIntronString.replace("@EXON_UNIQUE_NAME_1@", Exon.first().uniqueName)
        requestHandlingService.makeIntron(JSON.parse(makeIntronString))

        then: "we should expect to see two exons"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2
        assert CDS.count == 1
    }

    void "split exon in 3 prime"() {

        given: "add transcript and split exon string"
        String addTranscriptGb52239BigExonString = "{ ${testCredentials} \"track\":{\"id\":27979, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":108132,\"fmax\":113395,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52239-RA\",\"children\":[{\"location\":{\"fmin\":108132,\"fmax\":113395,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String splitExonString = "{ ${testCredentials} \"track\": {\"id\":27979, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]}, \"features\": [ { \"uniquename\": \"@EXON_UNIQUE_NAME_1@\", \"location\": { \"fmax\": 111258, \"fmin\": 111259 } } ], \"operation\": \"split_exon\", \"clientToken\":\"1986001742624362051356493373\" }\"u0000\"]\t"

        when: "we add the transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptGb52239BigExonString))

        then: "we should have a single exon transcript"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 1
        assert CDS.count == 1

        when: "we make an intron in the middle of one"
        splitExonString = splitExonString.replace("@EXON_UNIQUE_NAME_1@", Exon.first().uniqueName)
        requestHandlingService.splitExon(JSON.parse(splitExonString))

        then: "we should expect to see two exons"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2
        assert CDS.count == 1
    }

    void "make intron in projected 3 prime"() {
        given: "add transcript and make intron string and merge again"
        String addTranscriptGb52239BigExonString = "{ ${testCredentials} \"track\":{\"id\":27979, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":108132,\"fmax\":113395,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52239-RA\",\"children\":[{\"location\":{\"fmin\":108132,\"fmax\":113395,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String makeIntronString = "{ ${testCredentials}  \"track\": {\"id\":33720, \"name\":\"GB53497-RA (GroupUn87)::GB52239-RA (Group11.4)\", \"padding\":0, \"start\":10511, \"end\":82774, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":10311, \"end\":26919, \"feature\":{\"name\":\"GB53497-RA\"}},{\"name\":\"Group11.4\", \"start\":18705, \"end\":56255, \"feature\":{\"name\":\"GB52239-RA\"}}]}, \"features\": [ { \"uniquename\": \"@EXON_UNIQUE_NAME_1@\", \"location\": { \"fmin\": 30399 } } ], \"operation\": \"make_intron\"}"
        String mergeExonString = "{ ${testCredentials}  \"track\": {\"id\":33720, \"name\":\"GB53497-RA (GroupUn87)::GB52239-RA (Group11.4)\", \"padding\":0, \"start\":10511, \"end\":82774, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":10311, \"end\":26919, \"feature\":{\"name\":\"GB53497-RA\"}},{\"name\":\"Group11.4\", \"start\":18705, \"end\":56255, \"feature\":{\"name\":\"GB52239-RA\"}}]}, \"features\": [ { \"uniquename\": \"@EXON_UNIQUE_NAME_1@\" }, { \"uniquename\": \"@EXON_UNIQUE_NAME_2@\" } ], \"operation\": \"merge_exons\"}"

        when: "we add the transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptGb52239BigExonString))

        then: "we should have a single exon transcript"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 1
        assert CDS.count == 1

        when: "we make an intron in the middle of one"
        makeIntronString = makeIntronString.replace("@EXON_UNIQUE_NAME_1@", Exon.first().uniqueName)
        requestHandlingService.makeIntron(JSON.parse(makeIntronString))

        then: "we should expect to see two exons"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2
        assert CDS.count == 1

        when: "merge exons"
        mergeExonString = mergeExonString.replace("@EXON_UNIQUE_NAME_1@", Exon.first().uniqueName)
        mergeExonString = mergeExonString.replace("@EXON_UNIQUE_NAME_2@", Exon.last().uniqueName)
        requestHandlingService.mergeExons(JSON.parse(mergeExonString))

        then: "we should expect to see one exon again"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 1
        assert CDS.count == 1
        assert Exon.first().featureLocations.size() == 1
        assert Exon.first().firstFeatureLocation.fmin >= Gene.first().firstFeatureLocation.fmin
        assert Exon.first().firstFeatureLocation.fmax <= Gene.first().firstFeatureLocation.fmax
    }

    void "split exon in projected 3 prime and merge again"() {

        given: "add transcript and split exon string"
        String addTranscriptGb52239BigExonString = "{ ${testCredentials} \"track\":{\"id\":27979, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":108132,\"fmax\":113395,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52239-RA\",\"children\":[{\"location\":{\"fmin\":108132,\"fmax\":113395,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String splitExonString = "{ ${testCredentials}  \"track\": {\"id\":33720, \"name\":\"GB53497-RA (GroupUn87)::GB52239-RA (Group11.4)\", \"padding\":0, \"start\":10511, \"end\":82774, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":10311, \"end\":26919, \"feature\":{\"name\":\"GB53497-RA\"}},{\"name\":\"Group11.4\", \"start\":18705, \"end\":56255, \"feature\":{\"name\":\"GB52239-RA\"}}]}, \"features\": [ { \"uniquename\": \"@EXON_UNIQUE_NAME_1@\", \"location\": { \"fmax\": 30199, \"fmin\": 30200 } } ], \"operation\": \"split_exon\"}"
        String mergeExonString = "{ ${testCredentials}  \"track\": {\"id\":33720, \"name\":\"GB53497-RA (GroupUn87)::GB52239-RA (Group11.4)\", \"padding\":0, \"start\":10511, \"end\":82774, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":10311, \"end\":26919, \"feature\":{\"name\":\"GB53497-RA\"}},{\"name\":\"Group11.4\", \"start\":18705, \"end\":56255, \"feature\":{\"name\":\"GB52239-RA\"}}]}, \"features\": [ { \"uniquename\": \"@EXON_UNIQUE_NAME_1@\" }, { \"uniquename\": \"@EXON_UNIQUE_NAME_2@\" } ], \"operation\": \"merge_exons\"}"

        when: "we add the transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptGb52239BigExonString))

        then: "we should have a single exon transcript"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 1
        assert CDS.count == 1

        when: "we make an intron in the middle of one"
        splitExonString = splitExonString.replace("@EXON_UNIQUE_NAME_1@", Exon.first().uniqueName)
        requestHandlingService.splitExon(JSON.parse(splitExonString))

        then: "we should expect to see two exons"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 2
        assert CDS.count == 1

        when: "merge exons"
        mergeExonString = mergeExonString.replace("@EXON_UNIQUE_NAME_1@", Exon.first().uniqueName)
        mergeExonString = mergeExonString.replace("@EXON_UNIQUE_NAME_2@", Exon.last().uniqueName)
        requestHandlingService.mergeExons(JSON.parse(mergeExonString))

        then: "we should expect to see one exon again"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 1
        assert CDS.count == 1
        assert Exon.first().featureLocations.size() == 1
        assert Exon.first().firstFeatureLocation.fmin >= Gene.first().firstFeatureLocation.fmin
        assert Exon.first().firstFeatureLocation.fmax <= Gene.first().firstFeatureLocation.fmax
    }

    void "setting translation startand end should work when projected"() {

        given: "add transcript and split exon string"
        String addTranscriptGb52239BigExonString = "{ ${testCredentials} \"track\":{\"id\":27979, \"name\":\"GroupUn87::Group11.4\", \"padding\":0, \"start\":0, \"end\":153343, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":0, \"end\":78258},{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":108132,\"fmax\":113395,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52239-RA\",\"children\":[{\"location\":{\"fmin\":108132,\"fmax\":113395,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String setTranslationStartString = "{ ${testCredentials} \"track\": {\"id\":33720, \"name\":\"GB53497-RA (GroupUn87)::GB52239-RA (Group11.4)\", \"padding\":0, \"start\":10511, \"end\":82774, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":10311, \"end\":26919, \"feature\":{\"name\":\"GB53497-RA\"}},{\"name\":\"Group11.4\", \"start\":18705, \"end\":56255, \"feature\":{\"name\":\"GB52239-RA\"}}]}, \"features\": [ { \"uniquename\": \"@TRANSCRIPT_UNIQUE_NAME_1@\", \"location\": { \"fmin\": 28736 } } ], \"operation\": \"set_translation_start\"}"
        String setTranslationEndString = "{ ${testCredentials} \"track\": {\"id\":33720, \"name\":\"GB53497-RA (GroupUn87)::GB52239-RA (Group11.4)\", \"padding\":0, \"start\":10511, \"end\":82774, \"sequenceList\":[{\"name\":\"GroupUn87\", \"start\":10311, \"end\":26919, \"feature\":{\"name\":\"GB53497-RA\"}},{\"name\":\"Group11.4\", \"start\":18705, \"end\":56255, \"feature\":{\"name\":\"GB52239-RA\"}}]}, \"features\": [ { \"uniquename\": \"@TRANSCRIPT_UNIQUE_NAME_1@\", \"location\": { \"fmax\": 32030 } } ], \"operation\": \"set_translation_end\"}"

        when: "we add the transcript"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptGb52239BigExonString))
        int preCdsFmin = CDS.first().firstFeatureLocation.fmin
        int preCdsFmax = CDS.first().firstFeatureLocation.fmax

        then: "we should have a single exon transcript"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 1
        assert CDS.count == 1
        assert CDS.first().featureLocations.size() == 1
        assert preCdsFmin < preCdsFmax

        when: "we should set the translation start"
        setTranslationStartString = setTranslationStartString.replace("@TRANSCRIPT_UNIQUE_NAME_1@", MRNA.first().uniqueName)
        requestHandlingService.setTranslationStart(JSON.parse(setTranslationStartString))

        then: "we should have a single exon"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 1
        assert CDS.count == 1
        assert CDS.first().featureLocations.size() == 1
        assert CDS.first().firstFeatureLocation.fmin > preCdsFmin
        assert CDS.first().firstFeatureLocation.fmax == preCdsFmax



        when: "we should set the translation end"
        setTranslationEndString = setTranslationEndString.replace("@TRANSCRIPT_UNIQUE_NAME_1@", MRNA.first().uniqueName)
        requestHandlingService.setTranslationEnd(JSON.parse(setTranslationEndString))

        then: "it should also be smaller as well"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 1
        assert CDS.count == 1
        assert CDS.first().featureLocations.size() == 1
        assert CDS.first().firstFeatureLocation.fmin > preCdsFmin
        assert CDS.first().firstFeatureLocation.fmax < preCdsFmax
    }


    void "should be able to view two transcripts "() {

        given: "add transcript and split exon string"
        String addTranscriptGb52238BigExonString = "{ ${testCredentials} \"track\":{\"id\":31503, \"name\":\"Group11.4\", \"padding\":0, \"start\":0, \"end\":75085, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":10257,\"fmax\":18596,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52238-RA\",\"children\":[{\"location\":{\"fmin\":10257,\"fmax\":10302,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":12721,\"fmax\":13053,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":13233,\"fmax\":13361,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":13705,\"fmax\":14372,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":15416,\"fmax\":16227,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":16399,\"fmax\":16477,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":17280,\"fmax\":17486,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":18218,\"fmax\":18454,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":18561,\"fmax\":18596,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":10257,\"fmax\":18596,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\",\"clientToken\":\"662454866988883791014947489\"}"
        String addTranscriptGb52236BigExonString = "{ ${testCredentials} \"track\":{\"id\":31503, \"name\":\"Group11.4\", \"padding\":0, \"start\":0, \"end\":75085, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":52853,\"fmax\":58962,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52236-RA\",\"children\":[{\"location\":{\"fmin\":58860,\"fmax\":58962,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":52853,\"fmax\":55971,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":52853,\"fmax\":56051,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":56605,\"fmax\":56984,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":58470,\"fmax\":58698,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":58800,\"fmax\":58962,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":55971,\"fmax\":58860,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String getFeatures11_4String = "{ ${testCredentials} \"track\":{\"id\":31503, \"name\":\"Group11.4\", \"padding\":0, \"start\":0, \"end\":75085, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"operation\":\"get_features\"}"
        String getFeatures11_4ProjectedString = "{ ${testCredentials} \"track\":{\"name\":\"GB52238-RA (Group11.4)::GB52236-RA (Group11.4)\", \"padding\":0, \"start\":10257, \"end\":77558, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}},{\"name\":\"Group11.4\", \"start\":52653, \"end\":59162, \"feature\":{\"name\":\"GB52236-RA\"}}]},\"operation\":\"get_features\"}"

        when: "we add the transcript and get the feature"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptGb52238BigExonString))
        requestHandlingService.addTranscript(JSON.parse(addTranscriptGb52236BigExonString))
        JSONObject featuresObject = requestHandlingService.getFeatures(JSON.parse(getFeatures11_4String))
        JSONArray featuresArray = featuresObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        then: "we should see the entire feature"
        assert 2 == featuresArray.size()
        assert 15 == featuresArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value).size() + featuresArray.getJSONObject(1).getJSONArray(FeatureStringEnum.CHILDREN.value).size()
        assert Gene.count == 2
        assert MRNA.count == 2
        assert Exon.count == 13
        assert CDS.count == 2


        when: "we add the transcript and get the projected feature"
        featuresObject = requestHandlingService.getFeatures(JSON.parse(getFeatures11_4ProjectedString))
        featuresArray = featuresObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        then: "we should see the entire feature"
        assert 2 == featuresArray.size()
        assert 15 == featuresArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value).size() + featuresArray.getJSONObject(1).getJSONArray(FeatureStringEnum.CHILDREN.value).size()

    }
}
