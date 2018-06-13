package org.bbop.apollo

import grails.converters.JSON

class Gff3HandlerServiceIntegrationSpec extends AbstractIntegrationSpec{
   
    def gff3HandlerService
    def requestHandlingService


    void "write a GFF3 of a simple gene model"() {


        given: "we create a new gene"
        String json=" { ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235534,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1216850,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1224676,\"fmax\":1224823,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1228682,\"fmax\":1228825,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235237,\"fmax\":1235396,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235487,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1235534,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], ${testCredentials} \"operation\": \"add_transcript\" }"
        String addInsertionString = "{${testCredentials} \"operation\":\"add_sequence_alteration\",\"features\":[{\"non_reserved_properties\": [{\"tag\": \"justification\", \"value\":\"Sanger sequencing\"}], \"residues\":\"GGG\",\"location\":{\"fmin\":208499,\"strand\":1,\"fmax\":208499},\"type\":{\"name\":\"insertion_artifact\",\"cv\":{\"name\":\"sequence\"}}}],${testCredentials} \"track\":\"Group1.10\",\"clientToken\":\"123123\"}"
        String pseudogene = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":433518,\"fmax\":437436,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"pseudogene\"},\"children\":[{\"location\":{\"fmin\":433518,\"fmax\":437436,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"transcript\"},\"name\":\"GB40815-RA\",\"children\":[{\"location\":{\"fmin\":433518,\"fmax\":433570,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":436576,\"fmax\":436641,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":437424,\"fmax\":437436,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":433518,\"fmax\":437436,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}]}], ${testCredentials} \"operation\": \"add_feature\"}"
        String repeat_region = "{ ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":414369,\"fmax\":414600,\"strand\":0},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"repeat_region\"},\"name\":\"GB40814-RA\"}], ${testCredentials} \"operation\": \"add_feature\" }"

        when: "we parse the json"
        requestHandlingService.addTranscript(JSON.parse(json))
        requestHandlingService.addSequenceAlteration(JSON.parse(addInsertionString))
        requestHandlingService.addFeature(JSON.parse(pseudogene))
        requestHandlingService.addFeature(JSON.parse(repeat_region))
        

        then: "We should have at least one new gene"

        log.debug "${Gene.findAll()}"
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
        log.debug "${tempFile.absolutePath}"
        def featuresToWrite = Gene.list(sort: "class") + SequenceAlterationArtifact.findAll() + RepeatRegion.findAll()
        gff3HandlerService.writeFeaturesToText(tempFile.absolutePath,featuresToWrite,".")
        String tempFileText = tempFile.text

        then: "we should get a valid gff3 file"
        log.debug "${tempFileText}"
        def lines = tempFile.readLines()
        assert lines[0] == "##gff-version 3"
        assert lines[2].split("\t")[2] == Gene.cvTerm
        assert lines[2].split("\t")[8].indexOf("Name=GB40856-RA") != -1
        assert lines[3].split("\t")[2] == MRNA.cvTerm
        assert lines[15].split("\t")[2] == Pseudogene.cvTerm
        assert lines[21].split("\t")[2] == InsertionArtifact.cvTerm
        assert lines[21].split("\t")[8].indexOf("justification=Sanger sequencing") != -1
        assert lines[21].split("\t")[8].indexOf("residues=GGG") != -1
        assert lines[23].split("\t")[2] == RepeatRegion.cvTerm

        assert tempFileText.length() > 0
    }
}
