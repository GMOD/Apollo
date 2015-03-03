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
@Mock([Sequence,Gene,MRNA,Exon,CDS,Feature,FeatureLocation,FeatureRelationship,FeatureRelationshipService ])
class Gff3HandlerServiceSpec extends Specification {
    

    def setup() {
    }

    def cleanup() {
    }
   
    void "write a simple gene"() {
        
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
        
        List<Feature> featuresToWrite = new ArrayList<>()
        featuresToWrite.add(gene)

        File tempFile = File.createTempFile("asdf",".gff3")
        
        then: "We should have at least one new gene"
        assert Gene.count == 1
        println "${tempFile.absolutePath}"
        service.writeFeaturesToText(tempFile.absolutePath,featuresToWrite,".")
        String tempFileText = tempFile.text
        //println "===> Finally the output: ${tempFileText}"
       
        assert tempFileText.length()>0
        println tempFileText
        
        assert tempFileText == "##gff-version 3\n##sequence-region Group-1.10 201 1000\nGroup-1.10\t.\tGene\t201\t1000\t\t+\t\tName=Bob;ID=abc123\n###\n"
    }
    
    @Ignore
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
        
        MRNA mrna = new MRNA(
                name: "Bob-mRNA",
                uniqueName: "abc123-mRNA"
        ).save()
        FeatureLocation mrnaFeatureLocation = new FeatureLocation(
                fmin: 200,
                fmax: 1000,
                feature: mrna,
                sequence: refSequence,
                strand: 1
        ).save()
        mrna.addToFeatureLocations(mrnaFeatureLocation)
        
        Exon exonOne = new Exon(
                name: "exon1",
                uniqueName: "Bob-mRNA-exon1"
        ).save()
        FeatureLocation exonOneFeatureLocation = new FeatureLocation(
                fmin: 220,
                fmax: 400,
                feature: exonOne,
                sequence: refSequence,
                strand: 1
        ).save()
        exonOne.addToFeatureLocations(exonOneFeatureLocation)

        Exon exonTwo = new Exon(
                name: "exon2",
                uniqueName: "Bob-mRNA-exon2"
        ).save()
        FeatureLocation exonTwoFeatureLocation = new FeatureLocation(
                fmin: 500,
                fmax: 750,
                feature: exonTwo,
                sequence: refSequence,
                strand: 1
        ).save()
        exonTwo.addToFeatureLocations(exonTwoFeatureLocation)

        Exon exonThree = new Exon(
                name: "exon3",
                uniqueName: "Bob-mRNA-exon3"
        ).save()
        FeatureLocation exonThreeFeatureLocation = new FeatureLocation(
                fmin: 900,
                fmax: 1000,
                feature: exonThree,
                sequence: refSequence,
                strand: 1
        ).save()
        exonThree.addToFeatureLocations(exonThreeFeatureLocation)
        
        CDS cdsOne = new CDS(
                name: "cds1",
                uniqueName: "Bob-mRNA-cds1"
        ).save()
        FeatureLocation cdsOneFeatureLocation = new FeatureLocation(
                fmin: 220,
                fmax: 400,
                feature: cdsOne,
                sequence: refSequence,
                strand: 1
        ).save()
        cdsOne.addToFeatureLocations(cdsOneFeatureLocation)

        CDS cdsTwo = new CDS(
                name: "cds2",
                uniqueName: "Bob-mRNA-cds2"
        ).save()
        FeatureLocation cdsTwoFeatureLocation = new FeatureLocation(
                fmin: 500,
                fmax: 750,
                feature: cdsTwo,
                sequence: refSequence,
                strand: 1
        ).save()
        cdsTwo.addToFeatureLocations(cdsTwoFeatureLocation)

        CDS cdsThree = new CDS(
                name: "cds3",
                uniqueName: "Bob-mRNA-cds3"
        ).save()
        FeatureLocation cdsThreeFeatureLocation = new FeatureLocation(
                fmin: 900,
                fmax: 1000,
                feature: cdsThree,
                sequence: refSequence,
                strand: 1
        ).save()
        cdsThree.addToFeatureLocations(cdsThreeFeatureLocation)
        
        // connecting gene structure heirarchy
        FeatureRelationship mrnaFeatureRelationship = new FeatureRelationship(
                parentFeature: gene,
                childFeature: mrna
        ).save()
        
        FeatureRelationship exonOneFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna,
                childFeature: exonOne
        ).save()
        FeatureRelationship exonTwoFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna,
                childFeature: exonTwo
        ).save()
        FeatureRelationship exonThreeFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna,
                childFeature: exonThree
        ).save()

        FeatureRelationship cdsOneFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna,
                childFeature: cdsOne
        ).save()
        FeatureRelationship cdsTwoFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna,
                childFeature: cdsTwo
        ).save()
        FeatureRelationship cdsThreeFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna,
                childFeature: cdsThree
        ).save()
        
        gene.addToParentFeatureRelationships(mrnaFeatureRelationship)
        mrna.addToChildFeatureRelationships(mrnaFeatureRelationship)

        mrna.addToParentFeatureRelationships(exonOneFeatureRelationship)
        exonOne.addToChildFeatureRelationships(exonOneFeatureRelationship)
        
        mrna.addToParentFeatureRelationships(exonTwoFeatureRelationship)
        exonTwo.addToChildFeatureRelationships(exonTwoFeatureRelationship)
        
        mrna.addToParentFeatureRelationships(exonThreeFeatureRelationship)
        exonThree.addToChildFeatureRelationships(exonThreeFeatureRelationship)
        
        
        mrna.addToParentFeatureRelationships(cdsOneFeatureRelationship)
        cdsOne.addToChildFeatureRelationships(cdsOneFeatureRelationship)

        mrna.addToParentFeatureRelationships(cdsTwoFeatureRelationship)
        cdsTwo.addToChildFeatureRelationships(cdsTwoFeatureRelationship)

        mrna.addToParentFeatureRelationships(cdsThreeFeatureRelationship)
        cdsThree.addToChildFeatureRelationships(cdsThreeFeatureRelationship)
        
        println "Statistics:\nExon: ${Exon.count}\nMRNA:${MRNA.count}\nCDS:${CDS.count}\nGene:${Gene.count}"
        println "Gene parent features: ${gene.parentFeatureRelationships}"
        println "Gene child features: ${gene.childFeatureRelationships}"
        println "mRNA parent features: ${mrna.parentFeatureRelationships}"
        println "mRNA child features: ${mrna.childFeatureRelationships}"
        List<Feature> featuresToWrite = new ArrayList<>()
        featuresToWrite.add(mrna)

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
