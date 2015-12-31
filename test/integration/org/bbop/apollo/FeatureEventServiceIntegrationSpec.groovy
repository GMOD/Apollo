package org.bbop.apollo

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.bbop.apollo.history.FeatureOperation
import org.codehaus.groovy.grails.web.json.JSONObject

class FeatureEventServiceIntegrationSpec extends AbstractIntegrationSpec{

    def requestHandlingService
    def projectionService

    def setup() {
        FeatureEvent.deleteAll(FeatureEvent.all)
        Feature.deleteAll(Feature.all)
        setupDefaultUserOrg()
        projectionService.clearProjections()
//        Organism organism = new Organism(
//                directory: "test/integration/resources/sequences/honeybee-Group1.10/"
//                ,commonName: "sampleAnimal"
//        ).save(flush: true)
//        Sequence sequence = new Sequence(
//                length: 1405242
//                , seqChunkSize: 20000
//                , start: 0
//                , organism: organism
//                , end: 1405242
//                , name: "Group1.10"
//        ).save()
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
        splitString = splitString.replace("@EXON_1@",exon1UniqueName)
        splitString = splitString.replace("@EXON_2@",exon2UniqueName)
        JSONObject splitJsonObject = requestHandlingService.splitTranscript(JSON.parse(splitString))

        then: "we should have two of everything now"
        assert Exon.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Gene.count == 2


        when: "when we undo transcript A"
        String transcript1UniqueName = MRNA.findByName("GB40736-RA-00001").uniqueName
        String transcript2UniqueName = MRNA.findByName("GB40736-RAa-00001").uniqueName
        undoString1 = undoString1.replace("@TRANSCRIPT_1@",transcript1UniqueName)
        undoString2 = undoString2.replace("@TRANSCRIPT_2@",transcript2UniqueName)
        redoString1 = redoString1.replace("@TRANSCRIPT_1@",transcript1UniqueName)
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
        def mrnas = MRNA.all.sort(){ a,b -> a.name <=> b.name }
        assert mrnas[0].name == "GB40736-RA-00001"
        assert mrnas[1].name == "GB40736-RAa-00001"


        when: "we merge the transcript"
        def allFeatures = Feature.all
        String transcript1UniqueName = mrnas[0].uniqueName
        String transcript2UniqueName = mrnas[1].uniqueName
        mergeString = mergeString.replaceAll("@TRANSCRIPT_1@",transcript1UniqueName)
        mergeString = mergeString.replaceAll("@TRANSCRIPT_2@",transcript2UniqueName)
        redoString1 = redoString1.replaceAll("@TRANSCRIPT_1@",transcript1UniqueName)
        redoString2 = redoString2.replaceAll("@TRANSCRIPT_2@",transcript2UniqueName)
        JSONObject mergeJsonObject = requestHandlingService.mergeTranscripts(JSON.parse(mergeString))
        FeatureEvent currentFeatureEvent = FeatureEvent.findByCurrent(true)
        undoString = undoString.replaceAll("@TRANSCRIPT_1@",currentFeatureEvent.uniqueName)
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

}
