package org.bbop.apollo

import grails.test.spock.IntegrationSpec

class FeatureRelationshipServiceIntegrationSpec extends IntegrationSpec {
    
    def featureRelationshipService

    def setup() {
    }

    def cleanup() {
    }

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
