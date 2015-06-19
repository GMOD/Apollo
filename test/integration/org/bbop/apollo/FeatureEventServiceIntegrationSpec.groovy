package org.bbop.apollo

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.json.JSONObject

class FeatureEventServiceIntegrationSpec extends IntegrationSpec {

    def requestHandlingService

    def setup() {
        Organism organism = new Organism(
                directory: "/tmp"
                ,commonName: "sampleAnimal"
        ).save(flush: true)
        Sequence sequence = new Sequence(
                length: 1405242
                , refSeqFile: "adsf"
                , seqChunkPrefix: "Group1.10-"
                , seqChunkSize: 20000
                , start: 0
                , organism: organism
                , end: 1405242
                // from (honeybee f78/c6f/0c
                , sequenceDirectory: "test/integration/resources/sequences/honeybee-Group1.10/"
                , name: "Group1.10"
        ).save()
    }

    def cleanup() {
    }

    void "we can undo and redo a transcript split"() {

        given: "transcript data"
        String jsonString = "{\"track\":\"Annotations-Group1.10\",\"features\":[{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40736-RA\",\"children\":[{\"location\":{\"fmin\":938708,\"fmax\":938770,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":939570,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":938708,\"fmax\":939601,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String splitString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [ { \"uniquename\": \"@EXON_1@\" }, { \"uniquename\": \"@EXON_2@\" } ], \"operation\": \"split_transcript\" }"
        String undoString1 = "{ \"track\": \"Annotations-Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"undo\", \"count\": 1}"
        String undoString2 = "{ \"track\": \"Annotations-Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_2@\" } ], \"operation\": \"undo\", \"count\": 1}"
        String redoString1 = "{ \"track\": \"Annotations-Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_1@\" } ], \"operation\": \"redo\", \"count\": 1}"
        String redoString2 = "{ \"track\": \"Annotations-Group1.10\", \"features\": [ { \"uniquename\": \"@TRANSCRIPT_2@\" } ], \"operation\": \"redo\", \"count\": 1}"

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
        def allFeatures = Feature.all
        undoString1 = undoString1.replace("@TRANSCRIPT_1@",transcript1UniqueName)
        undoString2 = undoString2.replace("@TRANSCRIPT_2@",transcript2UniqueName)
        redoString1 = redoString1.replace("@TRANSCRIPT_1@",transcript1UniqueName)
        redoString2 = redoString2.replace("@TRANSCRIPT_2@",transcript2UniqueName)
        requestHandlingService.undo(JSON.parse(undoString1))

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


        when: "when we undo transcript B"
        requestHandlingService.undo(JSON.parse(undoString2))

        then: "we should have the original transcript"
        assert Exon.count == 2
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1

        when: "when we redo transcript"
        requestHandlingService.redo(JSON.parse(redoString2))

        then: "we should have two transcripts"
        assert Exon.count == 2
        assert CDS.count == 2
        assert MRNA.count == 2
        assert Gene.count == 2

    }
}
