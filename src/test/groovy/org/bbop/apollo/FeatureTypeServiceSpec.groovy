package org.bbop.apollo

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class FeatureTypeServiceSpec extends Specification implements ServiceUnitTest<FeatureTypeService>, DataTest {

    Class[] getDomainClassesToMock() {
        [Feature, FeatureType]
    }

    def setup() {
    }

    def cleanup() {
    }

    void "can add a feature type"() {

        given: "no feature types"
        assert FeatureType.count == 0

        when: "we add a Feature Type"
        service.createFeatureTypeForFeature(Gene.class, Gene.alternateCvTerm)
        FeatureType featureType = FeatureType.first()

        then: "we should have one"
        assert FeatureType.count == 1
        assert featureType.ontologyId == Gene.ontologyId
        assert featureType.name == Gene.cvTerm
        assert featureType.type == "sequence"
    }
}
