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
@Mock([Gene,Feature,FeatureLocation,Sequence])
class Gff3HandlerServiceSpec extends Specification {
    


    def setup() {
    }

    def cleanup() {
    }

    @Ignore // - commenting out to work on the unit test
    void "write a GFF3 of a simple gene model"() {


        when: "we create a new gene"
        Gene gene = new Gene(
                name: "Bob"
                ,uniqueName: "abc123"
        ).save()
        
        Sequence refSequence = new Sequence(
                length: 3
                ,refSeqFile: "adsf"
                ,seqChunkPrefix: "asdf"
                ,seqChunkSize: 3
                ,start: 5
                ,end: 8
                ,sequenceDirectory: "asdfadsf"
                ,name: "Group-1.10"
        ).save()

        FeatureLocation geneFeatureLocation = new FeatureLocation(
                feature: gene,
                fmin: 200,
                fmax: 1000,
                strand: 1,
                sequence: refSequence
        ).save()
        
        gene.addToFeatureLocations(geneFeatureLocation)
        gene.save()
        println gene.toString()
        List<Feature> featuresToWrite = new ArrayList<>()
        featuresToWrite.add(gene)
        File tempFile = File.createTempFile("asdf",".gff3")
        
        //tempFile.deleteOnExit()
        
        
        then: "We should have at least one new gene"
        assert Gene.count == 1
        println "${tempFile.absolutePath}"
        service.writeFeaturesToText(tempFile.absolutePath,featuresToWrite,".")
        

        String tempFileText = tempFile.text
        println "===> Finally the output: ${tempFileText}"
        assert tempFileText.length()>0
        println tempFileText

    }
}
