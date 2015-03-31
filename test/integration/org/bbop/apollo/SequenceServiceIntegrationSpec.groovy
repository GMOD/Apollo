package org.bbop.apollo

import grails.test.spock.IntegrationSpec
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class SequenceServiceIntegrationSpec extends IntegrationSpec {
    
    def requestHandlingService
    def SequenceService
    
    def setup() {
        Sequence sequence = new Sequence(
                length: 1405242
                ,refSeqFile: "adsf"
                ,seqChunkPrefix: "Group1.10-"
                ,seqChunkSize: 20000
                ,start: 0
                ,end: 1405242
                // from (honeybee f78/c6f/0c
                ,sequenceDirectory: "test/integration/resources/sequences/honeybee-Group1.10/"
                ,name: "Group1.10"
        ).save()
    }

    def cleanup() {
    }

    void "add a simple gene model and get its sequence"() {
        
        given: "a simple gene model with 1 mRNA, 1 exon and 1 CDS"
        String jsonString = "{ \"track\": \"Annotations-Group1.10\", \"features\": [{\"location\":{\"fmin\":1248797,\"fmax\":1249052,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40722-RA\",\"children\":[{\"location\":{\"fmin\":1248797,\"fmax\":1249052,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1248797,\"fmax\":1249052,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        
        when: "the gene model is added"
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "we should see the appropriate model"
        assert Sequence.count == 1
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 1
        assert CDS.count == 1
        assert FeatureLocation.count == 4 + FlankingRegion.count
        assert FeatureRelationship.count == 3
        for(seq in Sequence.all) {
            println "=:::> seq: ${seq.getSequenceChunks()}"
            
        }
        
        when: "A request is sent for the CDS sequence of the mRNA"
        String getSequenceString = "{\"operation\":\"get_sequence\",\"features\":[{\"uniquename\":\"@UNIQUENAME@\"}],\"track\":\"Annotations-Group1.10\",\"type\":\"@SEQUENCE_TYPE@\"}"
        String uniqueName = MRNA.findByName("GB40722-RA-00001").uniqueName
        getSequenceString = getSequenceString.replaceAll("@UNIQUENAME@", uniqueName)
        getSequenceString = getSequenceString.replaceAll("@SEQUENCE_TYPE@", "PEPTIDE")
        JSONObject commandObject = JSON.parse(getSequenceString) as JSONObject
        JSONObject getSequenceReturnObject = sequenceService.getSequenceForFeature(commandObject)
        
        then: "we should get back the proper CDS sequence"
        println "===> return Object: ${getSequenceReturnObject}"
        assert getSequenceReturnObject.residues != null
    }
}
