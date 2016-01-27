package org.bbop.apollo

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.history.FeatureOperation
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

class FeatureEventServiceIntegrationSpec extends IntegrationSpec {

    def requestHandlingService
    def exonService
    def featureEventService

    protected JSONObject createJSONFeatureContainer(JSONObject... features) throws JSONException {
        JSONObject jsonFeatureContainer = new JSONObject();
        JSONArray jsonFeatures = new JSONArray();
        jsonFeatureContainer.put(FeatureStringEnum.FEATURES.value, jsonFeatures);
        for (JSONObject feature : features) {
            jsonFeatures.put(feature);
        }
        return jsonFeatureContainer;
    }

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
        FeatureEvent.deleteAll(FeatureEvent.all)
        Feature.deleteAll(Feature.all)
    }

    void "we can undo and redo a transcript split"() {

        given: "transcript data"
        String jsonString = "{\"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40736-RA\",\"children\":[{\"location\":{\"fmin\":938708,\"fmax\":938770,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":939570,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String splitString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON_1@\" }, { \"uniquename\": \"@EXON_2@\" } ], \"operation\": \"split_transcript\" }"
        String undoString1 = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"undo\", \"count\": 1}"
        String undoString2 = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_2@\" } ], \"operation\": \"undo\", \"count\": 1}"
        String redoString1 = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"redo\", \"count\": 1}"

        when: "we insert a transcript"
        JSONObject returnObject = requestHandlingService.addTranscript(JSON.parse(jsonString) as JSONObject)

        then: "we have a transcript"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1


        when: "we split the transcript"
        String exon1UniqueName = Exon.all[0].uniqueName
        String exon2UniqueName = Exon.all[1].uniqueName
        splitString = splitString.replace("@EXON_1@", exon1UniqueName)
        splitString = splitString.replace("@EXON_2@", exon2UniqueName)
        JSONObject splitJsonObject = requestHandlingService.splitTranscript(JSON.parse(splitString))

        then: "we should have two of everything now"
        assert Exon.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Gene.count == 2


        when: "when we undo transcript A"
        String transcript1UniqueName = MRNA.findByName("GB40736-RA-00001").uniqueName
        String transcript2UniqueName = MRNA.findByName("GB40736-RAa-00001").uniqueName
        undoString1 = undoString1.replace("@TRANSCRIPT_1@", transcript1UniqueName)
        undoString2 = undoString2.replace("@TRANSCRIPT_2@", transcript2UniqueName)
        redoString1 = redoString1.replace("@TRANSCRIPT_1@", transcript1UniqueName)
        requestHandlingService.undo(JSON.parse(undoString1))

        then: "we should have the original transcript"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1

        when: "when we redo transcript"
        def allFeatures = Feature.all
        requestHandlingService.redo(JSON.parse(redoString1))
        allFeatures = Feature.all

        then: "we should have two transcripts"
        assert Exon.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Gene.count == 2


        when: "when we undo transcript B"
        requestHandlingService.undo(JSON.parse(undoString2))
        allFeatures = Feature.all
        def allFeatureEvents = FeatureEvent.all

        then: "we should have the original transcript"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1

        when: "when we redo transcript"
        requestHandlingService.redo(JSON.parse(redoString1))

        then: "we should have two transcripts"
        assert Exon.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Gene.count == 2

    }


    void "we can undo and redo a merge transcript"() {

        given: "transcript data"
        String addTranscriptString1 = "{\"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":938708,\"fmax\":938770,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40736-RA\",\"children\":[{\"location\":{\"fmin\":938708,\"fmax\":938770,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String addTranscriptString2 = "{\"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":939570,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40736-RA\",\"children\":[{\"location\":{\"fmin\":939570,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String mergeString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" }, { \"uniquename\": \"@TRANSCRIPT_2@\" } ], \"operation\": \"merge_transcripts\" }"
        String undoString = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"undo\", \"count\": 1}"
        String redoString1 = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"redo\", \"count\": 1}"
        String redoString2 = "{ \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_2@\" } ], \"operation\": \"redo\", \"count\": 1}"

        when: "we insert two transcripts"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString1))
        requestHandlingService.addTranscript(JSON.parse(addTranscriptString2))

        then: "we have a transcript"
        assert Exon.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Gene.count == 2
        assert FeatureEvent.count == 2
        def mrnas = MRNA.all.sort() { a, b -> a.name <=> b.name }
        assert mrnas[0].name == "GB40736-RA-00001"
        assert mrnas[1].name == "GB40736-RAa-00001"


        when: "we merge the transcript"
        def allFeatures = Feature.all
        String transcript1UniqueName = mrnas[0].uniqueName
        String transcript2UniqueName = mrnas[1].uniqueName
        mergeString = mergeString.replaceAll("@TRANSCRIPT_1@", transcript1UniqueName)
        mergeString = mergeString.replaceAll("@TRANSCRIPT_2@", transcript2UniqueName)
        redoString1 = redoString1.replaceAll("@TRANSCRIPT_1@", transcript1UniqueName)
        redoString2 = redoString2.replaceAll("@TRANSCRIPT_2@", transcript2UniqueName)
        JSONObject mergeJsonObject = requestHandlingService.mergeTranscripts(JSON.parse(mergeString))
        FeatureEvent currentFeatureEvent = FeatureEvent.findByCurrent(true)
        undoString = undoString.replaceAll("@TRANSCRIPT_1@", currentFeatureEvent.uniqueName)
        allFeatures = Feature.all

        then: "we should have two of everything now"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1
        assert FeatureEvent.count == 3
        assert FeatureEvent.countByCurrent(true) == 1
        assert FeatureEvent.findByCurrent(true).operation == FeatureOperation.MERGE_TRANSCRIPTS


        when: "when we undo transcript A"
        def allFeatureEvents = FeatureEvent.all
        requestHandlingService.undo(JSON.parse(undoString))

        then: "we should have the original transcript"
        assert Exon.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Gene.count == 2
        assert FeatureEvent.count == 3

        when: "when we redo transcript on 1"
        requestHandlingService.redo(JSON.parse(redoString1))
        allFeatures = Feature.all

        then: "we should have two transcripts"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1
        assert FeatureEvent.count == 3


        when: "when we undo transcript B"
        requestHandlingService.undo(JSON.parse(undoString))
        allFeatures = Feature.all
        allFeatureEvents = FeatureEvent.all

        then: "we should have the original transcript"
        assert Exon.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Gene.count == 2
        assert FeatureEvent.count == 3

        when: "when we redo transcript on 2"
        requestHandlingService.redo(JSON.parse(redoString2))

        then: "we should have two transcripts"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1

    }

    /**
     * https://github.com/GMOD/Apollo/issues/792
     */
    void "should handle merge, change on upstream / RHS gene, and undo"() {

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
        assert allFmin == featureLocation.fmin
        assert oldFmax == featureLocation.fmax

        when: "we make changes to an exon on gene 1"
        MRNA secondMRNA = MRNA.findByName("GB40788-RA-00001")
        exonList = exonService.getSortedExons(secondMRNA, true)
        exonUniqueName = exonList.first().uniqueName
        setExonBoundaryCommand = setExonBoundaryCommand.replace("@EXON_UNIQUENAME@", exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand) as JSONObject)
        exon = Exon.findByUniqueName(exonUniqueName)
        featureLocation = exon.featureLocation


        then: "a change was made!"
        assert allFmin == featureLocation.fmin
        assert newFmax == featureLocation.fmax
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
        getHistoryString = getHistoryString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", MRNA.first().uniqueName)
        List<List<FeatureEvent>> history1 = featureEventService.getHistory(uniqueName1)
        List<List<FeatureEvent>> history2 = featureEventService.getHistory(uniqueName2)
        historyContainer = featureEventService.generateHistory(historyContainer, (JSON.parse(getHistoryString) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
        JSONArray featuresArray = historyContainer.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONArray historyArray = featuresArray.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value)


        then: "we should see 3 events"
        assert 3 == historyArray.size()
        assert 0 == history2.size()
        assert 3 == history1.size()


        when: "we retrieve the arrays"
        def oldestProject = historyArray.getJSONObject(0)
        def middleProject = historyArray.getJSONObject(1)
        def recentProject = historyArray.getJSONObject(2)
        def oldestHistory = history1.get(0)
        def middleHistory = history1.get(1)
        def recentHistory = history1.get(2)

        then:
        // not sure if it should be
        assert recentHistory.first().operation==FeatureOperation.MERGE_TRANSCRIPTS
        assert oldestHistory.first().operation==FeatureOperation.ADD_TRANSCRIPT
        assert oldestHistory.size()==1
        assert middleHistory.size()==2
        assert recentHistory.size()==1
        assert 1 == historyArray.getJSONObject(0).getJSONArray(FeatureStringEnum.FEATURES.value).size()
        assert FeatureOperation.ADD_TRANSCRIPT.name() == oldestProject.getString("operation")
        assert 1 == recentProject.getJSONArray(FeatureStringEnum.FEATURES.value).size()
        assert FeatureOperation.MERGE_TRANSCRIPTS.name() == recentProject.getString("operation")
        assert 1 == middleProject.getJSONArray(FeatureStringEnum.FEATURES.value).size()
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
        assert allFmin == featureLocation.fmin
        assert newFmax == featureLocation.fmax

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
}
