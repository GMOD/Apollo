package org.bbop.apollo

import grails.test.spock.IntegrationSpec

class Gff3HandlerServiceIntegrationSpec extends IntegrationSpec {
   
    def gff3HandlerService

    def setup() {
        Sequence refSequence = new Sequence(
                length: 3000000
                ,seqChunkSize: 3000000
                ,start: 1
                ,end: 3000000
                ,name: "Group-1.10"
        ).save()
    }

    def cleanup() {
    }

    void "write a GFF3 of a simple gene model"() {


        when: "we create a new gene"
        Sequence refSequence = Sequence.first()
        Gene gene = new Gene(
                name: "Bob"
                ,uniqueName: "abc123"
                ,id: 1001
        ).save(flush: true)


        FeatureLocation geneFeatureLocation = new FeatureLocation(
                feature: gene
                ,fmin: 200
                ,fmax: 1000
                ,strand: 1
                ,sequence: refSequence
        ).save()

        gene.addToFeatureLocations(geneFeatureLocation)

        MRNA mrna = new MRNA(
                name: "Bob-mRNA"
                ,uniqueName: "abc123-mRNA"
                ,id: 100
        ).save(flush: true, failOnError: true)

        FeatureRelationship mrnaFeatureRelationship = new FeatureRelationship(
                childFeature: mrna
                ,parentFeature: gene
        ).save()

        FeatureLocation mrnaFeatureLocation = new FeatureLocation(
                fmin: 200
                ,fmax: 1000
                ,feature: mrna
                ,sequence: refSequence
                ,strand: 1
        ).save()
        mrna.addToFeatureLocations(mrnaFeatureLocation)

        Exon exonOne = new Exon(
                name: "exon1"
                ,uniqueName: "Bob-mRNA-exon1"
        ).save()
        FeatureLocation exonOneFeatureLocation = new FeatureLocation(
                fmin: 220
                ,fmax: 400
                ,feature: exonOne
                ,sequence: refSequence
                ,strand: 1
        ).save()
        exonOne.addToFeatureLocations(exonOneFeatureLocation)

        Exon exonTwo = new Exon(
                name: "exon2"
                ,uniqueName: "Bob-mRNA-exon2"
        ).save()
        FeatureLocation exonTwoFeatureLocation = new FeatureLocation(
                fmin: 500
                ,fmax: 750
                ,feature: exonTwo
                ,sequence: refSequence
                ,strand: 1
        ).save()
        exonTwo.addToFeatureLocations(exonTwoFeatureLocation)

        Exon exonThree = new Exon(
                name: "exon3"
                ,uniqueName: "Bob-mRNA-exon3"
        ).save()
        FeatureLocation exonThreeFeatureLocation = new FeatureLocation(
                fmin: 900
                ,fmax: 1000
                ,feature: exonThree
                ,sequence: refSequence
                ,strand: 1
        ).save()
        exonThree.addToFeatureLocations(exonThreeFeatureLocation)

        CDS cdsOne = new CDS(
                name: "cds1"
                ,uniqueName: "Bob-mRNA-cds1"
        ).save()
        FeatureLocation cdsOneFeatureLocation = new FeatureLocation(
                fmin: 220
                ,fmax: 400
                ,feature: cdsOne
                ,sequence: refSequence
                ,strand: 1
        ).save()
        cdsOne.addToFeatureLocations(cdsOneFeatureLocation)

        CDS cdsTwo = new CDS(
                name: "cds2"
                ,uniqueName: "Bob-mRNA-cds2"
        ).save()
        FeatureLocation cdsTwoFeatureLocation = new FeatureLocation(
                fmin: 500
                ,fmax: 750
                ,feature: cdsTwo
                ,sequence: refSequence
                ,strand: 1
        ).save()
        cdsTwo.addToFeatureLocations(cdsTwoFeatureLocation)

        CDS cdsThree = new CDS(
                name: "cds3"
                ,uniqueName: "Bob-mRNA-cds3"
        ).save()
        FeatureLocation cdsThreeFeatureLocation = new FeatureLocation(
                fmin: 900
                ,fmax: 1000
                ,feature: cdsThree
                ,sequence: refSequence
                ,strand: 1
        ).save()
        cdsThree.addToFeatureLocations(cdsThreeFeatureLocation)

        FeatureRelationship exonOneFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna
                ,childFeature: exonOne
        ).save()
        FeatureRelationship exonTwoFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna
                ,childFeature: exonTwo
        ).save()
        FeatureRelationship exonThreeFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna
                ,childFeature: exonThree
        ).save()

        FeatureRelationship cdsOneFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna
                ,childFeature: cdsOne
        ).save()
        FeatureRelationship cdsTwoFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna
                ,childFeature: cdsTwo
        ).save()
        FeatureRelationship cdsThreeFeatureRelationship = new FeatureRelationship(
                parentFeature: mrna
                ,childFeature: cdsThree
        ).save()
        
        FeatureLocation.all.each { featureLocation->
            refSequence.addToFeatureLocations(featureLocation)
        }

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

        List<Feature> featuresToWrite = new ArrayList<>()
        featuresToWrite.add(gene)

        File tempFile = File.createTempFile("asdf", ".gff3")
        tempFile.deleteOnExit()

        then: "We should have at least one new gene"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 3
        assert CDS.count == 3
        assert FeatureRelationship.count == 7
        assert FeatureLocation.count == 8
        Gene thisGene = Gene.first()
        assert thisGene.parentFeatureRelationships.size() == 1
        assert !thisGene.childFeatureRelationships

        MRNA thisMRNA = MRNA.first()
        assert thisMRNA.childFeatureRelationships.size() == 1
        assert thisMRNA.parentFeatureRelationships.size() == 6

        when: "we write the feature to test"
        log.debug "${tempFile.absolutePath}"
        gff3HandlerService.writeFeaturesToText(tempFile.absolutePath,featuresToWrite,".")
        String tempFileText = tempFile.text

        then: "we should get a valid gff3 file"
        assert tempFileText.length() > 0
    }
}
