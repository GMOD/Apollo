package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum

class FastaHandlerServiceIntegrationSpec extends AbstractIntegrationSpec{

    def requestHandlingService 
    def fastaHandlerService

    void "write a fasta of a simple gene model"() {
        given: "we create a new gene"
        String json=" { ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235534,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1216850,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1224676,\"fmax\":1224823,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1228682,\"fmax\":1228825,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235237,\"fmax\":1235396,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235487,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1235534,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"

        when: "we parse the json"
        requestHandlingService.addTranscript(JSON.parse(json))


        then: "We should have at least one new gene"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert CDS.count == 1

        


        when: "we write the feature to test"
        File tempFile = File.createTempFile("output", ".gff3")
        tempFile.deleteOnExit()
        log.debug "${tempFile.absolutePath}"
        fastaHandlerService.writeFeatures(Gene.findAll(), FeatureStringEnum.TYPE_PEPTIDE.value, ["name"] as Set, tempFile.path, FastaHandlerService.Mode.WRITE, FastaHandlerService.Format.TEXT)
        String tempFileText = tempFile.text

        then: "we should get a valid fasta file"
        assert tempFileText.length() > 0
        log.debug "${tempFileText}"
        def residues=""
        def lines = tempFile.readLines().each { line->
            if(line.indexOf(">")!=0) {
                residues+=line
            }
        }
        assert residues=="MARDIHRQSLRTEQPSGLDTGGVRFELSRALDLWARNSKLTFQEVNSDRADILVYFHRGYHGDGYPFDGRGQILAHAFFPGRDRGGDVHFDEEEIWLLQGDNNEEGTSLFAVAAHEFGHSLGLAHSSVPGALMYPWYQGLSSNYELPEDDRHGIQQMYEINQDIFFFIFFSHD"
        assert lines[0].indexOf("GB40856-RA-00001")!=-1
    }

}
