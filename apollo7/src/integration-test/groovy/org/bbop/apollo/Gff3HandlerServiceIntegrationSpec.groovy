package org.bbop.apollo

import grails.converters.JSON
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration

@Integration
@Rollback
class Gff3HandlerServiceIntegrationSpec extends AbstractIntegrationSpec {

    def gff3HandlerService
    def requestHandlingService

    void "write a GFF3 of a simple gene model"() {

        given: "we create a new gene"
        setupTestData()
        String json = " { ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235534,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1216850,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1224676,\"fmax\":1224823,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1228682,\"fmax\":1228825,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235237,\"fmax\":1235396,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235487,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1235534,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], ${testCredentials} \"operation\": \"add_transcript\" }"
        String addInsertionString = "{${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"non_reserved_properties\": [{\"tag\": \"justification\", \"value\":\"Sanger sequencing\"}], \"residues\":\"GGG\",\"location\":{\"fmin\":208499,\"strand\":1,\"fmax\":208499},\"type\":{\"name\":\"insertion_artifact\",\"cv\":{\"name\":\"sequence\"}}}],${testCredentials} \"track\":\"Group1.10\",\"clientToken\":\"123123\"}"
        String pseudogene = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":433518,\"fmax\":437436,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"pseudogene\"},\"children\":[{\"location\":{\"fmin\":433518,\"fmax\":437436,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"transcript\"},\"name\":\"GB40815-RA\",\"children\":[{\"location\":{\"fmin\":433518,\"fmax\":433570,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":436576,\"fmax\":436641,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":437424,\"fmax\":437436,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":433518,\"fmax\":437436,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}]}], ${testCredentials} \"operation\": \"add_feature\"}"
        String repeat_region = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":414369,\"fmax\":414600,\"strand\":0},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"repeat_region\"},\"name\":\"GB40814-RA\"}], ${testCredentials} \"operation\": \"add_feature\" }"

        when: "we parse the json"
        requestHandlingService.addTranscript(JSON.parse(json))
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString))
        requestHandlingService.addFeature(JSON.parse(pseudogene))
        requestHandlingService.addFeature(JSON.parse(repeat_region))

        then: "We should have at least one new gene"
        assert Gene.count == 2
        assert MRNA.count == 1
        assert RepeatRegion.count == 1
        assert Pseudogene.count == 1
        assert Exon.count == 8
        assert CDS.count == 1
        assert SequenceAlterationArtifact.count == 1

        when: "we write the feature to test"
        File tempFile = File.createTempFile("output", ".gff3")
        tempFile.deleteOnExit()
        def featuresToWrite = Gene.findAll() + SequenceAlterationArtifact.findAll() + RepeatRegion.findAll()
        gff3HandlerService.writeFeaturesToText(tempFile.absolutePath, featuresToWrite, ".")
        String tempFileText = tempFile.text
        def lines = tempFile.readLines()

        then: "we should get a valid gff3 file"
        assert lines[0] == "##gff-version 3"
        assert tempFileText.length() > 0
        assert tempFileText.contains(Gene.cvTerm)
        assert tempFileText.contains(MRNA.cvTerm)
        assert tempFileText.contains(Pseudogene.cvTerm)
        assert tempFileText.contains(InsertionArtifact.cvTerm)
        assert tempFileText.contains(RepeatRegion.cvTerm)
        assert tempFileText.contains("Name=GB40856-RA")
        assert tempFileText.contains("justification=Sanger sequencing")
        assert tempFileText.contains("residues=GGG")
    }
}
