package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Ignore
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
//@TestMixin(GrailsUnitTestMixin)
@TestFor(Gff3HandlerService)
@Mock([Gene,Feature])
class Gff3HandlerServiceSpec extends Specification {
    


    def setup() {
    }

    def cleanup() {
    }

    @Ignore
    void "write a simple exon"() {


        when: "we create a new gene"
        Gene gene = new Gene(
                name: "Bob"
                ,uniqueName: "abc123"
        )
        gene.save()
        List<Feature> featuresToWrite = new ArrayList<>()
        featuresToWrite.add(gene)
        File tempFile = File.createTempFile("asdf",".gff3")
        tempFile.deleteOnExit()

        
        then: "We should have at least one new gene"
        assert Gene.count ==1
        service.writeFeaturesToText(tempFile.absolutePath,featuresToWrite,".")
        
        
        String tempFileText = tempFile.text
        
        assert tempFileText.length()>0
        println tempFileText

    }
}
