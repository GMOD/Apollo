package org.bbop.apollo

class FeatureRelationshipServiceIntegrationSpec extends AbstractIntegrationSpec{
    
    def featureRelationshipService

    void "delete all parents and features"(){

        given: "a gene and a transcript"
        Gene gene = new Gene(name: "asdf",uniqueName: "asdf").save()
        MRNA mrna = new MRNA(name: "rman",uniqueName: "mrna").save()

        when: "we attach the two"
        featureRelationshipService.addChildFeature(gene,mrna,false)

        then: "they should be attached"
        Gene.count ==1
        MRNA.count ==1
        FeatureRelationship.count ==1

        when: "we delete from the gene"
        featureRelationshipService.deleteFeatureAndChildren(gene)

        then: "there should be nothing left"
        Gene.count ==0
        MRNA.count ==0
        FeatureRelationship.count ==0
    }

    void "delete all parents and features on multiple levels"(){

        given: "a gene and a transcript"
        Gene gene = new Gene(name: "asdf",uniqueName: "asdf").save()
        MRNA mrna = new MRNA(name: "rman",uniqueName: "mrna").save()
        Exon exon = new Exon(name: "exonname",uniqueName: "exon-unique").save()

        when: "we attach the two"
        featureRelationshipService.addChildFeature(gene,mrna,false)
        featureRelationshipService.addChildFeature(mrna,exon,false)

        then: "they should be attached"
        Gene.count ==1
        MRNA.count ==1
        Exon.count ==1
        FeatureRelationship.count ==2
        Feature.count ==3

        when: "we delete from the gene"
        featureRelationshipService.deleteFeatureAndChildren(gene)

        then: "there should be nothing left"
        Gene.count ==0
        MRNA.count ==0
        Exon.count ==0
        FeatureRelationship.count ==0
        Feature.count ==0
    }

    void "delete all parents and features for multiple children"(){

        given: "a gene and a transcript"
        Gene gene = new Gene(name: "asdf",uniqueName: "asdf").save()
        MRNA mrna = new MRNA(name: "rman",uniqueName: "mrna").save()
        MRNA mrna2 = new MRNA(name: "rman2",uniqueName: "mrna2").save()

        when: "we attach the two"
        featureRelationshipService.addChildFeature(gene,mrna,false)
        featureRelationshipService.addChildFeature(gene,mrna2,false)

        then: "they should be attached"
        Gene.count ==1
        MRNA.count ==2
        FeatureRelationship.count ==2

        when: "we delete from the gene"
        featureRelationshipService.deleteFeatureAndChildren(gene)

        then: "there should be nothing left"
        Gene.count ==0
        MRNA.count ==0
        FeatureRelationship.count ==0
    }
}
