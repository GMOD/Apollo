package org.bbop.apollo

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class FeatureRelationshipServiceSpec extends Specification implements ServiceUnitTest<FeatureRelationshipService>, DataTest {

    Class[] getDomainClassesToMock() {
        [FeatureRelationship, Feature, Gene, MRNA]
    }

    def setup() {
    }

    def cleanup() {
    }

    void "parents for feature"() {
        when: "A feature has parents"
        Gene gene = new Gene(
                name: "Gene1"
                , uniqueName: "Gene1"
        ).save(failOnError: true)
        MRNA mrna = new MRNA(
                name: "MRNA"
                , uniqueName: "MRNA"
        ).save(failOnError: true)
        FeatureRelationship fr = new FeatureRelationship(
                parentFeature: gene
                , childFeature: mrna
        ).save(failOnError: true)
        mrna.addToChildFeatureRelationships(fr)
        gene.addToParentFeatureRelationships(fr)

        then: "it should have parents"
        assert FeatureRelationship.count == 1
        List<Feature> parents = service.getParentsForFeature(mrna, Gene.ontologyId)
        assert parents.size() == 1
        Feature gene2 = parents.get(0)
        assert gene == gene2

        List<Feature> children = service.getChildrenForFeatureAndTypes(gene, MRNA.ontologyId)
        assert children.size() == 1
        Feature mrna2 = children.get(0)
        assert mrna == mrna2

        when: "we get a single parent for an ontology id"
        Feature parent = service.getParentForFeature(mrna, Gene.ontologyId)

        then: "we should find a valid parent"
        assert parent != null

        when: "we get a single parent for NO ontology id"
        Feature parent2 = service.getParentForFeature(mrna)

        then: "we should *STILL* find a valid parent"
        assert parent2 != null
    }
}
