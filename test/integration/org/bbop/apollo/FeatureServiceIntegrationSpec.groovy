package org.bbop.apollo

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.history.FeatureOperation
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

class FeatureServiceIntegrationSpec extends IntegrationSpec {
    
    def featureService
    def requestHandlingService
    def exonService
    def featureEventService

    def setup() {
        Organism organism = new Organism(
                directory: "test/integration/resources/sequences/honeybee-Group1.10/"
                , commonName: "sampleAnimal"
        ).save(flush: true)
        Sequence sequence = new Sequence(
                length: 1405242
                , seqChunkSize: 20000
                , start: 0
                , organism: organism
                , end: 1405242
                , name: "Group1.10"
        ).save()
    }

    def cleanup() {
    }

    void "convert JSON to Features"(){

        given: "a set string and existing sequence, when we have a complicated mRNA as JSON"
        String jsonString = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235534,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1216850,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1224676,\"fmax\":1224823,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1228682,\"fmax\":1228825,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235237,\"fmax\":1235396,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235487,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1235534,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"

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
        Feature feature = featureService.convertJSONToFeature(mRNAJsonObject,Sequence.first())

        then: "it should convert it to the same feature"
        assert feature!=null
        feature.ontologyId == MRNA.ontologyId

    }

    /**
     * https://github.com/GMOD/Apollo/issues/792
     */
    void "should handle merge, change on downstream / LHS , and undo"(){

        given: "two transcripts"
        // gene 1 - GB40787
        Integer oldFmin = 77860
        Integer newFmin = 77685
        Integer newFmax = 77944
        String gb40787String = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40787-RA\",\"children\":[{\"location\":{\"fmin\":77860,\"fmax\":77944,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":78049,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String gb40788String = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40788-RA\",\"children\":[{\"location\":{\"fmin\":65107,\"fmax\":65286,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":71477,\"fmax\":71651,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":75270,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonAddTranscriptObject1 = JSON.parse(gb40787String) as JSONObject
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40788String) as JSONObject
        String mergeTranscriptString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" }, { \"uniquename\": \"@TRANSCRIPT2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\" }"
        String undoOperation = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"undo\"}"
        String setExonBoundaryCommand = "{\"track\":\"Group1.10\",\"features\":[{\"uniquename\":\"@EXON_UNIQUENAME@\",\"location\":{\"fmin\":${newFmin},\"fmax\":${newFmax}}}],\"operation\":\"set_exon_boundaries\"}"
        String getHistoryString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" } ], \"operation\": \"get_history_for_features\" }"

        when: "we add both transcripts"
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)
        requestHandlingService.addTranscript(jsonAddTranscriptObject2)
        List<Exon> exonList = exonService.getSortedExons(MRNA.first(), true)
        String exonUniqueName = exonList.get(1).uniqueName
        Exon exon = Exon.findByUniqueName(exonUniqueName)
        FeatureLocation featureLocation = exon.featureLocation


        then: "we should see 2 genes, 2 transcripts, 5 exons, 2 CDS, no noncanonical splice sites"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert oldFmin==featureLocation.fmin
        assert newFmax==featureLocation.fmax

        when: "we make changes to an exon on gene 1"
        exonList = exonService.getSortedExons(MRNA.first(), true)
        exonUniqueName = exonList.get(1).uniqueName
        setExonBoundaryCommand = setExonBoundaryCommand.replace("@EXON_UNIQUENAME@",exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand) as JSONObject)
        exon = Exon.findByUniqueName(exonUniqueName)
        featureLocation = exon.featureLocation


        then: "a change was made!"
        assert newFmin==featureLocation.fmin
        assert newFmax==featureLocation.fmax
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0


        when: "we merge the transcripts"
        String uniqueName1 = MRNA.findByName("GB40787-RA-00001").uniqueName
        String uniqueName2 = MRNA.findByName("GB40788-RA-00001").uniqueName
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", uniqueName1)
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT2_UNIQUENAME@", uniqueName2)
        JSONObject commandObject = JSON.parse(mergeTranscriptString) as JSONObject
        requestHandlingService.mergeTranscripts(commandObject)

        then: "we should see 1 gene, 1 transcripts, 5 exons, 1 CDS, 1 3' noncanonical splice site and 1 5' noncanonical splice site"
        def allFeatures = Feature.all
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1

        when: "when we get the feature history"
        JSONObject historyContainer = createJSONFeatureContainer();
        getHistoryString = getHistoryString.replaceAll("@TRANSCRIPT1_UNIQUENAME@",MRNA.first().uniqueName)
        historyContainer = featureEventService.generateHistory(historyContainer,(JSON.parse(getHistoryString) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
        JSONArray historyArray = historyContainer.getJSONArray(FeatureStringEnum.FEATURES.value)


        then: "we should see 3 events"
        assert 3==historyArray.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value).size()


        when: "when we undo the merge"
        String transcriptSplitUndoString = undoOperation.replace("@UNIQUENAME@", MRNA.first().uniqueName)
        requestHandlingService.undo(JSON.parse(transcriptSplitUndoString) as JSONObject)
        exon = Exon.findByUniqueName(exonUniqueName)
        featureLocation = FeatureLocation.findByFeature(exon)


        then: "we see the changed model"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert newFmin==featureLocation.fmin
        assert newFmax==featureLocation.fmax

    }

    protected JSONObject createJSONFeatureContainer(JSONObject... features) throws JSONException {
        JSONObject jsonFeatureContainer = new JSONObject();
        JSONArray jsonFeatures = new JSONArray();
        jsonFeatureContainer.put(FeatureStringEnum.FEATURES.value, jsonFeatures);
        for (JSONObject feature : features) {
            jsonFeatures.put(feature);
        }
        return jsonFeatureContainer;
    }
/**
     * https://github.com/GMOD/Apollo/issues/792
     */
    void "should handle merge, change on upstream / RHS gene, and undo"(){

        given: "two transcripts"
        // gene 1 - GB40787
        Integer allFmin = 75270
        Integer oldFmax = 75367
        Integer newFmax = 75562
        String gb40787String = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40787-RA\",\"children\":[{\"location\":{\"fmin\":77860,\"fmax\":77944,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":78049,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String gb40788String = "{ \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40788-RA\",\"children\":[{\"location\":{\"fmin\":65107,\"fmax\":65286,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":71477,\"fmax\":71651,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":75270,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonAddTranscriptObject1 = JSON.parse(gb40787String) as JSONObject
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40788String) as JSONObject
        String mergeTranscriptString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" }, { \"uniquename\": \"@TRANSCRIPT2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\" }"
        String undoOperation = "{\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"undo\"}"
        String setExonBoundaryCommand = "{\"track\":\"Group1.10\",\"features\":[{\"uniquename\":\"@EXON_UNIQUENAME@\",\"location\":{\"fmin\":${allFmin},\"fmax\":${newFmax}}}],\"operation\":\"set_exon_boundaries\"}"
        String getHistoryString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" } ], \"operation\": \"get_history_for_features\" }"

        when: "we add both transcripts"
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)
        requestHandlingService.addTranscript(jsonAddTranscriptObject2)
        List<Exon> exonList = exonService.getSortedExons(MRNA.findByName("GB40788-RA-00001"), true)
        String exonUniqueName = exonList.first().uniqueName
        Exon exon = Exon.findByUniqueName(exonUniqueName)
        FeatureLocation featureLocation = exon.featureLocation


        then: "we should see 2 genes, 2 transcripts, 5 exons, 2 CDS, no noncanonical splice sites"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert allFmin==featureLocation.fmin
        assert oldFmax==featureLocation.fmax

        when: "we make changes to an exon on gene 1"
        MRNA secondMRNA = MRNA.findByName("GB40788-RA-00001")
        exonList = exonService.getSortedExons(secondMRNA, true)
        exonUniqueName = exonList.first().uniqueName
        setExonBoundaryCommand = setExonBoundaryCommand.replace("@EXON_UNIQUENAME@",exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand) as JSONObject)
        exon = Exon.findByUniqueName(exonUniqueName)
        featureLocation = exon.featureLocation


        then: "a change was made!"
        assert allFmin==featureLocation.fmin
        assert newFmax==featureLocation.fmax
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0


        when: "we merge the transcripts"
        String uniqueName1 = MRNA.findByName("GB40787-RA-00001").uniqueName
        String uniqueName2 = MRNA.findByName("GB40788-RA-00001").uniqueName
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", uniqueName1)
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT2_UNIQUENAME@", uniqueName2)
        JSONObject commandObject = JSON.parse(mergeTranscriptString) as JSONObject
        requestHandlingService.mergeTranscripts(commandObject)

        then: "we should see 1 gene, 1 transcripts, 5 exons, 1 CDS, 1 3' noncanonical splice site and 1 5' noncanonical splice site"
        def allFeatures = Feature.all
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1

        when: "when we get the feature history"
        JSONObject historyContainer = createJSONFeatureContainer();
        getHistoryString = getHistoryString.replaceAll("@TRANSCRIPT1_UNIQUENAME@",MRNA.first().uniqueName)
        historyContainer = featureEventService.generateHistory(historyContainer,(JSON.parse(getHistoryString) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
        JSONArray featuresArray = historyContainer.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONArray historyArray= featuresArray.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value)


        then: "we should see 3 events"
        assert 2==historyArray.size()

        when: "we retrieve the arrays"
        def recentProject = historyArray.getJSONObject(0)
        def middleProject = historyArray.getJSONObject(1)
        def oldestProject = historyArray.getJSONObject(2)

        then:
        assert 1==historyArray.getJSONObject(0).getJSONArray(FeatureStringEnum.FEATURES.value).size()
        assert FeatureOperation.ADD_TRANSCRIPT.name()==recentProject.getString("operation")
        assert 1==recentProject.getJSONArray(FeatureStringEnum.FEATURES.value).size()
        assert FeatureOperation.MERGE_TRANSCRIPTS.name()==oldestProject.getString("operation")
        assert 1==middleProject.getJSONArray(FeatureStringEnum.FEATURES.value).size()
        // should be ADD_TRANSCRIPT and SET_EXON_BOUNDARY

        when: "when we undo the merge"
        String undoString = undoOperation.replace("@UNIQUENAME@", MRNA.first().uniqueName)
        requestHandlingService.undo(JSON.parse(undoString) as JSONObject)
        exon = Exon.findByUniqueName(exonUniqueName)
        featureLocation = FeatureLocation.findByFeature(exon)


        then: "we see the changed model"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert allFmin==featureLocation.fmin
        assert newFmax==featureLocation.fmax

    }
}
