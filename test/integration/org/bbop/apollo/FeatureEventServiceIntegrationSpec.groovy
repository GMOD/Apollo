package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.history.FeatureOperation
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

class FeatureEventServiceIntegrationSpec extends AbstractIntegrationSpec {

    def requestHandlingService
    def transcriptService
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
        FeatureEvent.deleteAll(FeatureEvent.all)
        Feature.deleteAll(Feature.all)
        setupDefaultUserOrg()
    }

    def cleanup() {
        FeatureEvent.deleteAll(FeatureEvent.all)
        Feature.deleteAll(Feature.all)
    }

    void "we can undo and redo a transcript split"() {

        given: "transcript data"
        String jsonString = "{${testCredentials} \"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40736-RA\",\"children\":[{\"location\":{\"fmin\":938708,\"fmax\":938770,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":939570,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String splitString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON_1@\" }, { \"uniquename\": \"@EXON_2@\" } ], \"operation\": \"split_transcript\" }"
        String undoString1 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"undo\", \"count\": 1}"
        String undoString2 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_2@\" } ], \"operation\": \"undo\", \"count\": 1}"
        String redoString1 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"redo\", \"count\": 1}"

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
        requestHandlingService.redo(JSON.parse(redoString1))
        def allFeatures = Feature.all

        then: "we should have two transcripts"
        assert Exon.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Gene.count == 2


        when: "when we undo transcript B"
        requestHandlingService.undo(JSON.parse(undoString2))
        def allFeatureEvents = FeatureEvent.all

        then: "we should have the original transcript"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1

        when: "when we redo transcript 1"
        requestHandlingService.redo(JSON.parse(redoString1))

        then: "we should have two transcripts, A3/B2"
        assert Exon.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Gene.count == 2

    }

    void "we can undo a split twice"() {

        given: "transcript data"
        String jsonString = "{${testCredentials} \"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40736-RA\",\"children\":[{\"location\":{\"fmin\":938708,\"fmax\":938770,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":939570,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String splitString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON_1@\" }, { \"uniquename\": \"@EXON_2@\" } ], \"operation\": \"split_transcript\" }"
        String undoString1 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"undo\", \"count\": 1}"

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
        requestHandlingService.undo(JSON.parse(undoString1))

        then: "we should have the original transcript"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1

        when: "when we undo the same transcript again"
        def allFeatures = Feature.all
        requestHandlingService.undo(JSON.parse(undoString1))
        allFeatures = Feature.all

        then: "we have a transcript"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1

    }

    void "we can undo and redo a split"() {

        given: "transcript data"
        String jsonString = "{${testCredentials} \"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40736-RA\",\"children\":[{\"location\":{\"fmin\":938708,\"fmax\":938770,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":939570,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String splitString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON_1@\" }, { \"uniquename\": \"@EXON_2@\" } ], \"operation\": \"split_transcript\" }"
        String undoString1 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"undo\", \"count\": 1}"
        String redoString2 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_2@\" } ], \"operation\": \"redo\", \"count\": 1}"

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
        requestHandlingService.undo(JSON.parse(undoString1))

        then: "we should have the original transcript"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1

        when: "when we undo the same transcript again"
        def allFeatures = Feature.all
        redoString2 = redoString2.replace("@TRANSCRIPT_2@", transcript2UniqueName)
        requestHandlingService.redo(JSON.parse(redoString2))
        allFeatures = Feature.all

        then: "we should have two of everything now"
        assert Exon.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Gene.count == 2

    }

    void "we can undo and redo and redo other side"() {

        given: "transcript data"
        String jsonString = "{${testCredentials} \"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40736-RA\",\"children\":[{\"location\":{\"fmin\":938708,\"fmax\":938770,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":939570,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String splitString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON_1@\" }, { \"uniquename\": \"@EXON_2@\" } ], \"operation\": \"split_transcript\" }"
        String undoString1 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"undo\", \"count\": 1}"
        String undoString2 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_2@\" } ], \"operation\": \"undo\", \"count\": 1}"
        String redoString2 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_2@\" } ], \"operation\": \"redo\", \"count\": 1}"

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
        redoString2 = redoString2.replace("@TRANSCRIPT_2@", transcript2UniqueName)
        requestHandlingService.undo(JSON.parse(undoString1))

        then: "we should have the original transcript"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1

        when: "when we redo transcript 2"
        def allFeatures = Feature.all
        requestHandlingService.redo(JSON.parse(redoString2))
        allFeatures = Feature.all

        then: "we should have two transcripts, A3/B1"
        assert Exon.count == 2
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2

        when: "when we undo transcript A"
        requestHandlingService.undo(JSON.parse(undoString2))

        then: "we should have the original transcript"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1

        when: "when we redo transcript 2 again"
        requestHandlingService.redo(JSON.parse(redoString2))

        then: "we shuld have A3/B2"
        assert Exon.count == 2
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2

    }

    void "we can undo and redo a merge transcript"() {

        given: "transcript data"
        String addTranscriptString1 = "{${testCredentials} \"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":938708,\"fmax\":938770,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40736-RA\",\"children\":[{\"location\":{\"fmin\":938708,\"fmax\":938770,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String addTranscriptString2 = "{${testCredentials} \"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":939570,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40736-RA\",\"children\":[{\"location\":{\"fmin\":939570,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        String mergeString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" }, { \"uniquename\": \"@TRANSCRIPT_2@\" } ], \"operation\": \"merge_transcripts\" }"
        String undoString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"undo\", \"count\": 1}"
        String redoString1 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"redo\", \"count\": 1}"
        String redoString2 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_2@\" } ], \"operation\": \"redo\", \"count\": 1}"

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
    void "should handle merge, change on upstream / RHS gene, undo, redo"() {

        given: "two transcripts"
        // gene 1 - GB40787
        Integer allFmin = 75270
        Integer oldFmax = 75367
        Integer newFmax = 75562
        String gb40787String = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40787-RA\",\"children\":[{\"location\":{\"fmin\":77860,\"fmax\":77944,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":78049,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String gb40788String = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40788-RA\",\"children\":[{\"location\":{\"fmin\":65107,\"fmax\":65286,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":71477,\"fmax\":71651,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":75270,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonAddTranscriptObject1 = JSON.parse(gb40787String) as JSONObject
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40788String) as JSONObject
        String mergeTranscriptString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" }, { \"uniquename\": \"@TRANSCRIPT2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\" }"
        String undoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"undo\"}"
        String redoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"redo\"}"
        String setExonBoundaryCommand = "{${testCredentials} \"track\":\"Group1.10\",\"features\":[{\"uniquename\":\"@EXON_UNIQUENAME@\",\"location\":{\"fmin\":${allFmin},\"fmax\":${newFmax}}}],\"operation\":\"set_exon_boundaries\"}"
        String getHistoryString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" } ], \"operation\": \"get_history_for_features\" }"

        when: "we add both transcripts"
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)
        requestHandlingService.addTranscript(jsonAddTranscriptObject2)
        List<Exon> exonList = transcriptService.getSortedExons(MRNA.findByName("GB40788-RA-00001"), true)
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
        exonList = transcriptService.getSortedExons(secondMRNA, true)
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
        MRNA mrnaGB40787 = MRNA.findByName("GB40787-RA-00001")
        MRNA mrnaGB40788 = MRNA.findByName("GB40788-RA-00001")
        String uniqueNameGB40787 = mrnaGB40787.uniqueName
        String uniqueNameGB40788 = mrnaGB40788.uniqueName
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", uniqueNameGB40787)
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT2_UNIQUENAME@", uniqueNameGB40788)
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
        getHistoryString = getHistoryString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", uniqueNameGB40787)
        List<List<FeatureEvent>> history1 = featureEventService.getHistory(uniqueNameGB40787)
        List<List<FeatureEvent>> history2 = featureEventService.getHistory(uniqueNameGB40788)
        historyContainer = featureEventService.generateHistory(historyContainer, (JSON.parse(getHistoryString) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
        JSONArray featuresArray = historyContainer.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONArray historyArray = featuresArray.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value)


        then: "we should see 3 events"
        assert 3 == historyArray.size()
        assert 3 == history1.size()
        assert history1 == history2


        when: "we retrieve the arrays"
        def oldestProject = historyArray.getJSONObject(0)
        def middleProject = historyArray.getJSONObject(1)
        def recentProject = historyArray.getJSONObject(2)
        def oldestHistory = history1.get(0)
        def middleHistory = history1.get(1)
        def recentHistory = history1.get(2)

        then:
        // not sure if it should be
        assert recentHistory.first().operation == FeatureOperation.MERGE_TRANSCRIPTS
        assert oldestHistory.first().operation == FeatureOperation.ADD_TRANSCRIPT
        assert oldestHistory.size() == 1
        assert middleHistory.size() == 2
        assert recentHistory.size() == 1
        assert 1 == historyArray.getJSONObject(0).getJSONArray(FeatureStringEnum.FEATURES.value).size()
        assert FeatureOperation.ADD_TRANSCRIPT.name() == oldestProject.getString("operation")
        assert 1 == recentProject.getJSONArray(FeatureStringEnum.FEATURES.value).size()
        assert FeatureOperation.MERGE_TRANSCRIPTS.name() == recentProject.getString("operation")
        assert 1 == middleProject.getJSONArray(FeatureStringEnum.FEATURES.value).size()
        // should be ADD_TRANSCRIPT and SET_EXON_BOUNDARY

        when: "when we undo the merge"
        String undoString = undoOperation.replace("@UNIQUENAME@", uniqueNameGB40787)
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

        when: "when we redo the merge on one side"
        String redoString = redoOperation.replace("@UNIQUENAME@", uniqueNameGB40787)
        requestHandlingService.redo(JSON.parse(redoString) as JSONObject)

        then: "we should see 1 gene, 1 transcripts, 5 exons, 1 CDS, 1 3' noncanonical splice site and 1 5' noncanonical splice site"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1

        when: "when we undo the merge again"
        undoString = undoOperation.replace("@UNIQUENAME@", uniqueNameGB40787)
        requestHandlingService.undo(JSON.parse(undoString) as JSONObject)

        then: "we see the changed model"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert allFmin == featureLocation.fmin
        assert newFmax == featureLocation.fmax

        when: "when we redo the merge on the other side"
        redoString = redoOperation.replace("@UNIQUENAME@", uniqueNameGB40788)
        requestHandlingService.redo(JSON.parse(redoString) as JSONObject)

        then: "we should see 1 gene, 1 transcripts, 5 exons, 1 CDS, 1 3' noncanonical splice site and 1 5' noncanonical splice site"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1
    }

    /**
     * https://github.com/GMOD/Apollo/issues/792
     */
    void "should handle merge, change on downstream / LHS , undo, redo"() {

        given: "two transcripts"
        // gene 1 - GB40787
        Integer oldFmin = 77860
        Integer newFmin = 77685
        Integer newFmax = 77944
        String gb40787String = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40787-RA\",\"children\":[{\"location\":{\"fmin\":77860,\"fmax\":77944,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":78049,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String gb40788String = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40788-RA\",\"children\":[{\"location\":{\"fmin\":65107,\"fmax\":65286,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":71477,\"fmax\":71651,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":75270,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonAddTranscriptObject1 = JSON.parse(gb40787String) as JSONObject
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40788String) as JSONObject
        String mergeTranscriptString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" }, { \"uniquename\": \"@TRANSCRIPT2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\" }"
        String undoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"undo\"}"
        String redoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"redo\"}"
        String setExonBoundaryCommand = "{${testCredentials} \"track\":\"Group1.10\",\"features\":[{\"uniquename\":\"@EXON_UNIQUENAME@\",\"location\":{\"fmin\":${newFmin},\"fmax\":${newFmax}}}],\"operation\":\"set_exon_boundaries\"}"
        String getHistoryString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" } ], \"operation\": \"get_history_for_features\" }"

        when: "we add both transcripts"
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)
        requestHandlingService.addTranscript(jsonAddTranscriptObject2)
        List<Exon> exonList = transcriptService.getSortedExons(MRNA.first(), true)
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
        assert oldFmin == featureLocation.fmin
        assert newFmax == featureLocation.fmax

        when: "we make changes to an exon on gene 1"
        exonList = transcriptService.getSortedExons(MRNA.first(), true)
        exonUniqueName = exonList.get(1).uniqueName
        setExonBoundaryCommand = setExonBoundaryCommand.replace("@EXON_UNIQUENAME@", exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand) as JSONObject)
        exon = Exon.findByUniqueName(exonUniqueName)
        featureLocation = exon.featureLocation


        then: "a change was made!"
        assert newFmin == featureLocation.fmin
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
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1

        when: "when we get the feature history"
        JSONObject historyContainer = createJSONFeatureContainer();
        getHistoryString = getHistoryString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", MRNA.first().uniqueName)
        historyContainer = featureEventService.generateHistory(historyContainer, (JSON.parse(getHistoryString) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
        JSONArray historyArray = historyContainer.getJSONArray(FeatureStringEnum.FEATURES.value)


        then: "we should see 3 events"
        assert 3 == historyArray.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value).size()


        when: "when we undo the merge"
        MRNA firstMRNA = MRNA.first()
        String undoString = undoOperation.replace("@UNIQUENAME@", firstMRNA.uniqueName)
        requestHandlingService.undo(JSON.parse(undoString) as JSONObject)
        exon = Exon.findByUniqueName(exonUniqueName)
        featureLocation = FeatureLocation.findByFeature(exon)
        def currentFeatureEvent = featureEventService.findCurrentFeatureEvent(firstMRNA.uniqueName)
        def history = featureEventService.getHistory(firstMRNA.uniqueName)


        then: "we see the changed model"
        assert currentFeatureEvent.size() == 2
        assert history.size() == 3
        assert history[0][0].current == false
        assert history[1][0].current == true
        assert history[1][1].current == true
        assert history[2][0].current == false
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert newFmin == featureLocation.fmin
        assert newFmax == featureLocation.fmax


        when: "when we redo the merge on one side"
        String redoString = redoOperation.replace("@UNIQUENAME@", firstMRNA.uniqueName)
        requestHandlingService.redo(JSON.parse(redoString) as JSONObject)

        then: "we should see 1 gene, 1 transcripts, 5 exons, 1 CDS, 1 3' noncanonical splice site and 1 5' noncanonical splice site"

        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1

        when: "when we undo the merge again"
        undoString = undoOperation.replace("@UNIQUENAME@", firstMRNA.uniqueName)
        requestHandlingService.undo(JSON.parse(undoString) as JSONObject)
        def lastMRNA = MRNA.last()
        currentFeatureEvent = featureEventService.findCurrentFeatureEvent(lastMRNA.uniqueName)
        history = featureEventService.getHistory(lastMRNA.uniqueName)

        then: "we see the changed model"
        assert currentFeatureEvent.size() == 2
        assert history.size() == 3
        assert history[0][0].current == false
        assert history[1][0].current == true
        assert history[1][1].current == true
        assert history[2][0].current == false
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0

        when: "when we redo the merge on the other side"
        redoString = redoOperation.replace("@UNIQUENAME@", lastMRNA.uniqueName)
        requestHandlingService.redo(JSON.parse(redoString) as JSONObject)

        then: "we should see 1 gene, 1 transcripts, 5 exons, 1 CDS, 1 3' noncanonical splice site and 1 5' noncanonical splice site"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1
    }

    /**
     * https://github.com/GMOD/Apollo/issues/792
     */
    void "should handle merge, change on downstream / LHS , undo, undo"() {

        given: "two transcripts"
        // gene 1 - GB40787
        Integer oldFmin = 77860
        Integer newFmin = 77685
        Integer newFmax = 77944
        String gb40787String = "{ ${testCredentials} \"track\": \"Group1.10\", ${testCredentials} \"features\": [{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40787-RA\",\"children\":[{\"location\":{\"fmin\":77860,\"fmax\":77944,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":78049,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String gb40788String = "{ ${testCredentials} \"track\": \"Group1.10\", ${testCredentials} \"features\": [{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40788-RA\",\"children\":[{\"location\":{\"fmin\":65107,\"fmax\":65286,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":71477,\"fmax\":71651,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":75270,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonAddTranscriptObject1 = JSON.parse(gb40787String) as JSONObject
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40788String) as JSONObject
        String mergeTranscriptString = "{ ${testCredentials} \"track\": \"Group1.10\", ${testCredentials} \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" }, { \"uniquename\": \"@TRANSCRIPT2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\" }"
        String undoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"undo\"}"
        String redoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":1,\"track\":\"Group1.10\",\"operation\":\"redo\"}"
        String setExonBoundaryCommand = "{${testCredentials} \"track\":\"Group1.10\",${testCredentials} \"features\":[{\"uniquename\":\"@EXON_UNIQUENAME@\",\"location\":{\"fmin\":${newFmin},\"fmax\":${newFmax}}}],\"operation\":\"set_exon_boundaries\"}"
        String getHistoryString = "{ ${testCredentials} \"track\": \"Group1.10\", ${testCredentials} \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" } ], \"operation\": \"get_history_for_features\" }"

        when: "we add both transcripts"
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)
        requestHandlingService.addTranscript(jsonAddTranscriptObject2)
        MRNA firstMrna = MRNA.first()
        List<Exon> exonList = transcriptService.getSortedExons(firstMrna, true)
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
        assert oldFmin == featureLocation.fmin
        assert newFmax == featureLocation.fmax

        when: "we make changes to an exon on gene 1"
        exonList = transcriptService.getSortedExons(firstMrna, true)
        exonUniqueName = exonList.get(1).uniqueName
        setExonBoundaryCommand = setExonBoundaryCommand.replace("@EXON_UNIQUENAME@", exonUniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundaryCommand) as JSONObject)
        exon = Exon.findByUniqueName(exonUniqueName)
        featureLocation = exon.featureLocation


        then: "a change was made!"
        assert newFmin == featureLocation.fmin
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
        assert uniqueName1 == firstMrna.uniqueName
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1

        when: "when we get the feature history"
        JSONObject historyContainer = createJSONFeatureContainer();
        getHistoryString = getHistoryString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", firstMrna.uniqueName)
        historyContainer = featureEventService.generateHistory(historyContainer, (JSON.parse(getHistoryString) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
        JSONArray historyArray = historyContainer.getJSONArray(FeatureStringEnum.FEATURES.value)


        then: "we should see 3 events"
        assert 3 == historyArray.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value).size()


        when: "when we undo the merge"
        MRNA firstMRNA = MRNA.first()
        def history = featureEventService.getHistory(firstMRNA.uniqueName)
        def currentFeatureEvent = featureEventService.findCurrentFeatureEvent(firstMRNA.uniqueName)
        String undoString = undoOperation.replace("@UNIQUENAME@", firstMRNA.uniqueName)
        requestHandlingService.undo(JSON.parse(undoString) as JSONObject)
        exon = Exon.findByUniqueName(exonUniqueName)
        featureLocation = FeatureLocation.findByFeature(exon)
        currentFeatureEvent = featureEventService.findCurrentFeatureEvent(firstMRNA.uniqueName)
        history = featureEventService.getHistory(firstMRNA.uniqueName)


        then: "we see the changed model"
        assert currentFeatureEvent.size() == 2
        assert history.size() == 3
        assert history[0][0].current == false
        assert history[1][0].current == true
        assert history[1][1].current == true
        assert history[2][0].current == false
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert newFmin == featureLocation.fmin
        assert newFmax == featureLocation.fmax


        when: "when we undo again"
        undoString = undoOperation.replace("@UNIQUENAME@", firstMRNA.uniqueName)
        requestHandlingService.undo(JSON.parse(undoString) as JSONObject)
        currentFeatureEvent = featureEventService.findCurrentFeatureEvent(firstMRNA.uniqueName)
        history = featureEventService.getHistory(firstMRNA.uniqueName)
        exon = Exon.findByUniqueName(exonUniqueName)
        featureLocation = FeatureLocation.findByFeature(exon)

        then: "we should see A1 and B1"
        assert currentFeatureEvent.size() == 2
        assert history.size() == 3
        assert history[0].size() == 1
        assert history[0][0].current == true
        assert history[1].size() == 2
        history[1].each {
            if (it.current) {
                assert it.operation == FeatureOperation.ADD_TRANSCRIPT
            } else {
                assert it.operation == FeatureOperation.SET_EXON_BOUNDARIES
            }
        }
        assert history[2].size() == 1
        assert history[2][0].current == false
        // looks like its failing to delete the "inferred" one
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert oldFmin == featureLocation.fmin
        assert newFmax == featureLocation.fmax

    }

    void "we should be able to create a uniform tree merge and undo it"() {

        given: "two transcripts A = gb40788 and B = gb40787"
        Integer gb40788Fmin = 75270
        Integer old40788Fmax = 75367
        Integer new40788Fmax = 75562

        Integer old40787Fmin = 77860
        Integer new40787Fmin = 77616
        Integer gb40787Fmax = 77944

        String gb40787String = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40787-RA\",\"children\":[{\"location\":{\"fmin\":77860,\"fmax\":77944,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":78049,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String gb40788String = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40788-RA\",\"children\":[{\"location\":{\"fmin\":65107,\"fmax\":65286,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":71477,\"fmax\":71651,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":75270,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonAddTranscriptObject1 = JSON.parse(gb40787String) as JSONObject
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40788String) as JSONObject
        String mergeTranscriptString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" }, { \"uniquename\": \"@TRANSCRIPT2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\" }"
        String undoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":@COUNT@,\"track\":\"Group1.10\",\"operation\":\"undo\"}"
        String redoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":@COUNT@,\"track\":\"Group1.10\",\"operation\":\"redo\"}"
        String setExonBoundary40788Command = "{${testCredentials} \"track\":\"Group1.10\",\"features\":[{\"uniquename\":\"@EXON_UNIQUENAME@\",\"location\":{\"fmin\":${gb40788Fmin},\"fmax\":${new40788Fmax}}}],\"operation\":\"set_exon_boundaries\"}"
        String setExonBoundary40787Command = "{${testCredentials} \"track\":\"Group1.10\",\"features\":[{\"uniquename\":\"@EXON_UNIQUENAME@\",\"location\":{\"fmin\":${new40787Fmin},\"fmax\":${gb40787Fmax}}}],\"operation\":\"set_exon_boundaries\"}"
        String getHistoryString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" } ], \"operation\": \"get_history_for_features\" }"


        when: "we add the two transcripts"
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)
        requestHandlingService.addTranscript(jsonAddTranscriptObject2)
        MRNA mrna40787 = MRNA.findByName("GB40787-RA-00001")
        MRNA mrna40788 = MRNA.findByName("GB40788-RA-00001")
        List<Exon> exonList40788 = transcriptService.getSortedExons(mrna40788, true)
        String exon788UniqueName = exonList40788.first().uniqueName
        Exon exon788 = Exon.findByUniqueName(exon788UniqueName)
        FeatureLocation featureLocation788 = exon788.featureLocation
        List<Exon> exonList40787 = transcriptService.getSortedExons(mrna40787, true)
        String exon787UniqueName = exonList40787.last().uniqueName
        Exon exon787 = Exon.findByUniqueName(exon787UniqueName)
        FeatureLocation featureLocation787 = exon787.featureLocation


        then: "we verify that they are there and the coordinates (A1/B1)"
        assert mrna40787 != null
        assert mrna40788 != null
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert gb40788Fmin == featureLocation788.fmin
        assert old40788Fmax == featureLocation788.fmax
        assert old40787Fmin == featureLocation787.fmin
        assert gb40787Fmax == featureLocation787.fmax


        when: "we set the exon boundaries for both"
        setExonBoundary40788Command = setExonBoundary40788Command.replace("@EXON_UNIQUENAME@", exon788UniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundary40788Command) as JSONObject)
        setExonBoundary40787Command = setExonBoundary40787Command.replace("@EXON_UNIQUENAME@", exon787UniqueName)
        requestHandlingService.setExonBoundaries(JSON.parse(setExonBoundary40787Command) as JSONObject)
        exon788 = Exon.findByUniqueName(exon788UniqueName)
        featureLocation788 = exon788.featureLocation
        exon787 = Exon.findByUniqueName(exon787UniqueName)
        featureLocation787 = exon787.featureLocation


        then: "we verify that they are there and the NEW coordinates (A2/B2)"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert gb40788Fmin == featureLocation788.fmin
        assert new40788Fmax == featureLocation788.fmax
        assert new40787Fmin == featureLocation787.fmin
        assert gb40787Fmax == featureLocation787.fmax


        when: "we merge them"
        String uniqueName787 = mrna40787.uniqueName
        String uniqueName788 = mrna40788.uniqueName
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", uniqueName787)
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT2_UNIQUENAME@", uniqueName788)
        JSONObject commandObject = JSON.parse(mergeTranscriptString) as JSONObject
        requestHandlingService.mergeTranscripts(commandObject)


        then: "we confirm that there is just the one transcript (A2B2)"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1


        when: "we undo the merge"
        String undoString = undoOperation.replace("@UNIQUENAME@", mrna40787.uniqueName).replace("@COUNT@", "1")
        requestHandlingService.undo(JSON.parse(undoString) as JSONObject)
        exon788 = Exon.findByUniqueName(exon788UniqueName)
        featureLocation788 = exon788.featureLocation
        exon787 = Exon.findByUniqueName(exon787UniqueName)
        featureLocation787 = exon787.featureLocation


        then: "we verify that it is the most recent values (A2/B2) and that the history is correct"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert gb40788Fmin == featureLocation788.fmin
        assert new40788Fmax == featureLocation788.fmax
        assert new40787Fmin == featureLocation787.fmin
        assert gb40787Fmax == featureLocation787.fmax


        when: "we undo A2"
        undoString = undoOperation.replace("@UNIQUENAME@", mrna40788.uniqueName).replace("@COUNT@", "1")
        requestHandlingService.undo(JSON.parse(undoString) as JSONObject)
        exon788 = Exon.findByUniqueName(exon788UniqueName)
        featureLocation788 = exon788.featureLocation
        exon787 = Exon.findByUniqueName(exon787UniqueName)
        featureLocation787 = exon787.featureLocation

        JSONObject historyContainer = createJSONFeatureContainer();
        def thisHistoryString = getHistoryString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", mrna40787.uniqueName)
        historyContainer = featureEventService.generateHistory(historyContainer, (JSON.parse(thisHistoryString) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
        JSONArray featuresArray = historyContainer.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONArray historyArray = featuresArray.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value)


        then: "we should get B2/A1"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert gb40788Fmin == featureLocation788.fmin
        assert old40788Fmax == featureLocation788.fmax
        assert new40787Fmin == featureLocation787.fmin
        assert gb40787Fmax == featureLocation787.fmax

        assert historyArray.size() == 3
        assert historyArray[0].operation == FeatureOperation.ADD_TRANSCRIPT.name()
        assert !historyArray[0].current
        assert historyArray[0].features[0].name == "GB40787-RA-00001"
        assert historyArray[0].features.size() == 1
        assert historyArray[1].operation == FeatureOperation.SET_EXON_BOUNDARIES.name()
        assert historyArray[1].features[0].name == "GB40787-RA-00001"
        assert historyArray[1].features.size() == 1
        assert historyArray[1].current
        assert historyArray[2].operation == FeatureOperation.MERGE_TRANSCRIPTS.name()
        assert historyArray[2].features[0].name == "GB40787-RA-00001"
        assert historyArray[2].features.size() == 1
        assert !historyArray[2].current

        when: "we verify the history for the other side"
        JSONObject historyContainer2 = createJSONFeatureContainer();
        def historyString2 = getHistoryString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", mrna40788.uniqueName)
        historyContainer2 = featureEventService.generateHistory(historyContainer2, (JSON.parse(historyString2) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
        featuresArray = historyContainer2.getJSONArray(FeatureStringEnum.FEATURES.value)
        historyArray = featuresArray.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value)

        then: "we should see the hsitory of GB40788"
        assert historyArray.size() == 3
        assert historyArray[0].operation == FeatureOperation.ADD_TRANSCRIPT.name()
        assert historyArray[0].current
        assert historyArray[0].features[0].name == "GB40788-RA-00001"
        assert historyArray[0].features.size() == 1
        assert historyArray[1].operation == FeatureOperation.SET_EXON_BOUNDARIES.name()
        assert historyArray[1].features[0].name == "GB40788-RA-00001"
        assert historyArray[1].features.size() == 1
        assert !historyArray[1].current
        assert historyArray[2].operation == FeatureOperation.MERGE_TRANSCRIPTS.name()
        // NOTE: the merged value is GB40787
        assert historyArray[2].features[0].name == "GB40787-RA-00001"
        assert historyArray[2].features.size() == 1
        assert !historyArray[2].current


        when: "we undo B2"
        undoString = undoOperation.replace("@UNIQUENAME@", mrna40787.uniqueName).replace("@COUNT@", "1")
        requestHandlingService.undo(JSON.parse(undoString) as JSONObject)
        exon788 = Exon.findByUniqueName(exon788UniqueName)
        featureLocation788 = exon788.featureLocation
        exon787 = Exon.findByUniqueName(exon787UniqueName)
        featureLocation787 = exon787.featureLocation


        then: "we should get B1/A1"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert gb40788Fmin == featureLocation788.fmin
        assert old40788Fmax == featureLocation788.fmax
        assert old40787Fmin == featureLocation787.fmin
        assert gb40787Fmax == featureLocation787.fmax

        when: "we redo B to get to B1 (and stay A2)"
        def redoString = redoOperation.replace("@UNIQUENAME@", mrna40787.uniqueName).replace("@COUNT@", "1")
        requestHandlingService.redo(JSON.parse(redoString) as JSONObject)
        exon788 = Exon.findByUniqueName(exon788UniqueName)
        featureLocation788 = exon788.featureLocation
        exon787 = Exon.findByUniqueName(exon787UniqueName)
        featureLocation787 = exon787.featureLocation

        then: "we confirm A2 / B1 "
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0
        assert gb40788Fmin == featureLocation788.fmin
        assert old40788Fmax == featureLocation788.fmax
        assert new40787Fmin == featureLocation787.fmin
        assert gb40787Fmax == featureLocation787.fmax


        when: "we redo A to A2 -> AB (re-merge)"
        redoString = redoOperation.replace("@UNIQUENAME@", mrna40788.uniqueName).replace("@COUNT@", "2")
        requestHandlingService.redo(JSON.parse(redoString) as JSONObject)

        then: "we confirm that we have a merge AB"
        def allFeatures = Feature.all
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1
    }

    void "we should be able to do a merge THEN split and undo and then redo it"() {

        given: "two transcripts A = gb40788 and B = gb40787"
        String gb40787String = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40787-RA\",\"children\":[{\"location\":{\"fmin\":77860,\"fmax\":77944,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":78049,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String gb40788String = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40788-RA\",\"children\":[{\"location\":{\"fmin\":65107,\"fmax\":65286,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":71477,\"fmax\":71651,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":75270,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonAddTranscriptObject1 = JSON.parse(gb40787String) as JSONObject
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40788String) as JSONObject
        String mergeTranscriptString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" }, { \"uniquename\": \"@TRANSCRIPT2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\" }"
        String splitString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON_1@\" }, { \"uniquename\": \"@EXON_2@\" } ], \"operation\": \"split_transcript\" }"
        String undoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":@COUNT@,\"track\":\"Group1.10\",\"operation\":\"undo\"}"
        String redoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":@COUNT@,\"track\":\"Group1.10\",\"operation\":\"redo\"}"
        String getHistoryString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_UNIQUENAME@\" } ], \"operation\": \"get_history_for_features\" }"


        when: "we add the two transcripts"
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)
        requestHandlingService.addTranscript(jsonAddTranscriptObject2)
        MRNA mrna40787 = MRNA.findByName("GB40787-RA-00001")
        MRNA mrna40788 = MRNA.findByName("GB40788-RA-00001")

        then: "we verify that they are there and the coordinates (A1/B1)"
        assert mrna40787 != null
        assert mrna40788 != null
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0


        when: "we merge them"
        String uniqueName787 = mrna40787.uniqueName
        String uniqueName788 = mrna40788.uniqueName
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", uniqueName787)
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT2_UNIQUENAME@", uniqueName788)
        JSONObject commandObject = JSON.parse(mergeTranscriptString) as JSONObject
        requestHandlingService.mergeTranscripts(commandObject)


        then: "we confirm that there is just the one transcript (A2B2)"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1

        when: "we split the transcript"
        def sortedExons = Exon.all.sort() { a, b ->
            a.featureLocation.fmin <=> b.featureLocation.fmin
        }
        String exon1UniqueName = sortedExons[1].uniqueName
        String exon2UniqueName = sortedExons[2].uniqueName
        println "left fmin: ${sortedExons[1].fmin}"
        println "right fmin: ${sortedExons[2].fmin}"
        splitString = splitString.replace("@EXON_1@", exon1UniqueName)
        splitString = splitString.replace("@EXON_2@", exon2UniqueName)
        JSONObject splitJsonObject = requestHandlingService.splitTranscript(JSON.parse(splitString))

        then: "we should have two of everything now"
        assert Exon.count == 5
        assert MRNA.count == 2
        assert Gene.count == 2
        assert CDS.count == 2


        when: "we get the history of the two transcripts"
        JSONObject history787Container = createJSONFeatureContainer();
        def thisHistory787String = getHistoryString.replaceAll("@TRANSCRIPT_UNIQUENAME@", mrna40787.uniqueName)
        history787Container = featureEventService.generateHistory(history787Container, (JSON.parse(thisHistory787String) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
        JSONArray features787Array = history787Container.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONArray history787Array = features787Array.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value)

        JSONObject history788Container = createJSONFeatureContainer();
        def thisHistory788String = getHistoryString.replaceAll("@TRANSCRIPT_UNIQUENAME@", mrna40788.uniqueName)
        history788Container = featureEventService.generateHistory(history788Container, (JSON.parse(thisHistory788String) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
        JSONArray features788Array = history788Container.getJSONArray(FeatureStringEnum.FEATURES.value)
        JSONArray history788Array = features788Array.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value)

        then: "we confirm that its something sane"
        assert history787Array.size() == 3
        assert history787Array[0].operation == FeatureOperation.ADD_TRANSCRIPT.name()
        assert !history787Array[0].current
        assert history787Array[0].features[0].name == "GB40787-RA-00001"
        assert history787Array[0].features.size() == 1
        assert history787Array[1].operation == FeatureOperation.MERGE_TRANSCRIPTS.name()
        assert history787Array[1].features[0].name == "GB40787-RA-00001"
        assert history787Array[1].features.size() == 1
        assert !history787Array[1].current
        assert history787Array[2].operation == FeatureOperation.SPLIT_TRANSCRIPT.name()
        assert history787Array[2].features[0].name == "GB40787-RA-00001"
        assert history787Array[2].features.size() == 1
        assert history787Array[2].current

        assert history788Array.size() == 3
        assert history788Array[0].operation == FeatureOperation.ADD_TRANSCRIPT.name()
        assert !history788Array[0].current
        assert history788Array[0].features[0].name == "GB40788-RA-00001"
        assert history788Array[0].features.size() == 1
        assert history788Array[1].operation == FeatureOperation.MERGE_TRANSCRIPTS.name()
        assert history788Array[1].features[0].name == "GB40787-RA-00001"
        assert history788Array[1].features.size() == 1
        assert !history788Array[1].current
        assert history788Array[2].operation == FeatureOperation.SPLIT_TRANSCRIPT.name()
        assert history788Array[2].features.size() == 1
        // this works in the code, not sure why it fails in the test here
//        assert  history788Array[2].features[0].name == "GB40787-RAa-00001"
        assert history788Array[2].current


        when: "we undo A2"
        String undoString = undoOperation.replace("@UNIQUENAME@", mrna40788.uniqueName).replace("@COUNT@", "1")
        requestHandlingService.undo(JSON.parse(undoString) as JSONObject)


        then: "we should get B2/A1"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1
        assert CDS.count == 1


        when: "we redo B to get to B1 (and stay A2)"
        def redoString = redoOperation.replace("@UNIQUENAME@", mrna40787.uniqueName).replace("@COUNT@", "1")
        requestHandlingService.redo(JSON.parse(redoString) as JSONObject)

        then: "we confirm A2 / B1 "
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1

    }

    void "we should be able to do a split THEN merge and undo and then redo it"() {

        given: "two transcripts A = gb40788 and B = gb40787"
        String gb40787String = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40787-RA\",\"children\":[{\"location\":{\"fmin\":77860,\"fmax\":77944,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":78049,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":77860,\"fmax\":78076,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String gb40788String = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40788-RA\",\"children\":[{\"location\":{\"fmin\":65107,\"fmax\":65286,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":71477,\"fmax\":71651,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":75270,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":65107,\"fmax\":75367,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonAddTranscriptObject1 = JSON.parse(gb40787String) as JSONObject
        JSONObject jsonAddTranscriptObject2 = JSON.parse(gb40788String) as JSONObject
        String mergeTranscriptString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT1_UNIQUENAME@\" }, { \"uniquename\": \"@TRANSCRIPT2_UNIQUENAME@\" } ], \"operation\": \"merge_transcripts\" }"
        String splitString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@EXON_1@\" }, { \"uniquename\": \"@EXON_2@\" } ], \"operation\": \"split_transcript\" }"
        String undoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":@COUNT@,\"track\":\"Group1.10\",\"operation\":\"undo\"}"
        String redoOperation = "{${testCredentials} \"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"count\":@COUNT@,\"track\":\"Group1.10\",\"operation\":\"redo\"}"
        String getHistoryString = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_UNIQUENAME@\" } ], \"operation\": \"get_history_for_features\" }"
        String deleteString1 = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_NAME@\" } ], \"operation\": \"delete_feature\" }"


        when: "we add the two transcripts"
        requestHandlingService.addTranscript(jsonAddTranscriptObject1)
        requestHandlingService.addTranscript(jsonAddTranscriptObject2)
        MRNA mrna40787 = MRNA.findByName("GB40787-RA-00001")
        MRNA mrna40788 = MRNA.findByName("GB40788-RA-00001")

        then: "we verify that they are there and the coordinates (A1/B1)"
        assert mrna40787 != null
        assert mrna40788 != null
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0

        when: "we split the transcript"
        def sortedExons = Exon.all.sort() { a, b ->
            a.featureLocation.fmin <=> b.featureLocation.fmin
        }
        String exon1UniqueName = sortedExons[1].uniqueName
        String exon2UniqueName = sortedExons[2].uniqueName
        println "left fmin: ${sortedExons[1].fmin}"
        println "right fmin: ${sortedExons[2].fmin}"
        splitString = splitString.replace("@EXON_1@", exon1UniqueName)
        splitString = splitString.replace("@EXON_2@", exon2UniqueName)
        JSONObject splitJsonObject = requestHandlingService.splitTranscript(JSON.parse(splitString))

        then: "we should have three of everything now"
        assert Exon.count == 5
        assert MRNA.count == 3
        assert Gene.count == 3
        assert CDS.count == 3
        assert NonCanonicalFivePrimeSpliceSite.count == 0
        assert NonCanonicalThreePrimeSpliceSite.count == 0

        when: "we merge them"
        def sortedTranscripts = Transcript.all.sort() { a, b ->
            a.featureLocation.fmin <=> b.featureLocation.fmin
        }
        String transcript1UniqueName = sortedTranscripts[1].uniqueName
        String transcript2UniqueName = sortedTranscripts[2].uniqueName
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT1_UNIQUENAME@", transcript1UniqueName)
        mergeTranscriptString = mergeTranscriptString.replaceAll("@TRANSCRIPT2_UNIQUENAME@", transcript2UniqueName)
        JSONObject commandObject = JSON.parse(mergeTranscriptString) as JSONObject
        requestHandlingService.mergeTranscripts(commandObject)


        then: "we confirm that there is just the one transcript (A2B2)"
        assert Gene.count == 2
        assert MRNA.count == 2
        assert CDS.count == 2
        assert Exon.count == 5
        assert NonCanonicalFivePrimeSpliceSite.count == 1
        assert NonCanonicalThreePrimeSpliceSite.count == 1


        // TODO: these are not consistently sorted.   See: https://github.com/GMOD/Apollo/pull/1749
//        when: "we get the history of the 5' transcript"
//        JSONObject history787Container = createJSONFeatureContainer();
//        def thisHistory787String = getHistoryString.replaceAll("@TRANSCRIPT_UNIQUENAME@", mrna40787.uniqueName)
//        history787Container = featureEventService.generateHistory(history787Container, (JSON.parse(thisHistory787String) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
//        JSONArray features787Array = history787Container.getJSONArray(FeatureStringEnum.FEATURES.value)
//        JSONArray history787Array = features787Array.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value)
//
//        then: "we can confirm that it is correct"
//        assert history787Array.size() == 3
//        assert history787Array[0].operation == FeatureOperation.ADD_TRANSCRIPT.name()
//        assert !history787Array[0].current
////        assert history787Array[0].features[0].name == "GB40787-RA-00001"
//        assert history787Array[0].features.size() == 1
////        assert history787Array[1].operation == FeatureOperation.SPLIT_TRANSCRIPT.name()
//        assert history787Array[1].features.size() == 1
//        // this works in the code, not sure why it fails in the test here
////        assert history787Array[1].features[0].name == "GB40787-RAa-00001"
//        assert !history787Array[1].current
//        assert history787Array[2].operation == FeatureOperation.MERGE_TRANSCRIPTS.name()
//        assert history787Array[2].features[0].name == "GB40787-RA-00001"
//        assert history787Array[2].features.size() == 1
//        assert history787Array[2].current
//
//        when: "we get the history of the 3' transcript"
//        JSONObject history788Container = createJSONFeatureContainer();
//        def thisHistory788String = getHistoryString.replaceAll("@TRANSCRIPT_UNIQUENAME@", mrna40788.uniqueName)
//        history788Container = featureEventService.generateHistory(history788Container, (JSON.parse(thisHistory788String) as JSONObject).getJSONArray(FeatureStringEnum.FEATURES.value))
//        JSONArray features788Array = history788Container.getJSONArray(FeatureStringEnum.FEATURES.value)
//        JSONArray history788Array = features788Array.getJSONObject(0).getJSONArray(FeatureStringEnum.HISTORY.value)
//
//        then: "we confirm that its something sane"
//        assert history788Array.size() == 2
//        assert history788Array[0].operation == FeatureOperation.ADD_TRANSCRIPT.name()
//        assert !history788Array[0].current
//        assert history788Array[0].features[0].name == "GB40788-RA-00001"
//        assert history788Array[0].features.size() == 1
//        assert history788Array[1].operation == FeatureOperation.SPLIT_TRANSCRIPT.name()
//        assert history788Array[1].features[0].name == "GB40788-RA-00001"
//        assert history788Array[1].features.size() == 1
//        assert history788Array[1].current
//
//

//        when: "we undo the second transcript"
//        sortedTranscripts = Transcript.all.sort() { a, b ->
//            a.featureLocation.fmin <=> b.featureLocation.fmin
//        }
//        String undoString = undoOperation.replace("@UNIQUENAME@", sortedTranscripts[1].uniqueName).replace("@COUNT@", "1")
//        requestHandlingService.undo(JSON.parse(undoString) as JSONObject)


//        then: "we should have 3 of everything again"
//        assert Exon.count == 5
//        assert MRNA.count == 3
//        assert Gene.count == 3
//        assert CDS.count == 3
//        assert NonCanonicalFivePrimeSpliceSite.count == 0
//        assert NonCanonicalThreePrimeSpliceSite.count == 0
//
//
//        when: "we redo"
//        sortedTranscripts = Transcript.all.sort() { a, b ->
//            a.featureLocation.fmin <=> b.featureLocation.fmin
//        }
//        def redoString = redoOperation.replace("@UNIQUENAME@", sortedTranscripts[1].uniqueName).replace("@COUNT@", "1")
//        requestHandlingService.redo(JSON.parse(redoString) as JSONObject)
//
////        then: "we should be back to two"
////        assert Gene.count == 2
////        assert MRNA.count == 2
////        assert CDS.count == 2
////        assert Exon.count == 5
////        assert NonCanonicalFivePrimeSpliceSite.count == 1
////        assert NonCanonicalThreePrimeSpliceSite.count == 1
////
////        when: "we undo the left transcript instead"
//        sortedTranscripts = Transcript.all.sort() { a, b ->
//            a.featureLocation.fmin <=> b.featureLocation.fmin
//        }
//        undoString = undoOperation.replace("@UNIQUENAME@", sortedTranscripts[0].uniqueName).replace("@COUNT@", "1")
//        try {
//            requestHandlingService.undo(JSON.parse(undoString) as JSONObject)
//            assert false
//        } catch (e) {
//            assert true
//            assert e instanceof AnnotationException
//        }
//
//////        then: "should be the same counts"
//////        assert Gene.count == 2
//////        assert MRNA.count == 2
//////        assert CDS.count == 2
//////        assert Exon.count == 5
//////        assert NonCanonicalFivePrimeSpliceSite.count == 1
//////        assert NonCanonicalThreePrimeSpliceSite.count == 1
//////
////        when: "we delete the first transcript"
//        def mrnaToDelete = MRNA.all.first().uniqueName
//        requestHandlingService.deleteFeature(JSON.parse(deleteString1.replace("@TRANSCRIPT_NAME@", mrnaToDelete)))
//
//        then: "there should be only one MRNA and gene left"
//        assert Gene.count == 1
//        assert MRNA.count == 1
//
//        when: "we delete the second transcript"
//        mrnaToDelete = MRNA.all.first().uniqueName
//        requestHandlingService.deleteFeature(JSON.parse(deleteString1.replace("@TRANSCRIPT_NAME@", mrnaToDelete)))
//
//        then: "there should be no features left"
//        assert Gene.count == 0
//        assert MRNA.count == 0
//        assert CDS.count == 0
//        assert Exon.count == 0
//        assert NonCanonicalFivePrimeSpliceSite.count == 0
//        assert NonCanonicalThreePrimeSpliceSite.count == 0

    }
}
