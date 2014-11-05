package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(FeatureRelationshipService)
@Mock([FeatureRelationship,Feature,Gene,MRNA])
class FeatureRelationshipServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "parents for feature"() {
        when: "A feature has parents"
        Gene gene = new Gene(
                name: "Gene1"
                ,uniqueName: "Gene1"
        ).save(failOnError: true)
        MRNA mrna = new MRNA(
                name: "MRNA"
                ,uniqueName: "MRNA"
        ).save(failOnError: true)
        new FeatureRelationship(
                objectFeature: gene
                ,subjectFeature: mrna
        ).save(failOnError: true)

        then: "it should have parents"
        FeatureRelationship.count==1
        List<Feature> parents = service.getParentsForFeature(mrna,Gene.ontologyId)
        parents.size() ==1
        Gene gene2 = parents.get(0)
        gene == gene2
    }
}
