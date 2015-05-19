package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.mock.interceptor.MockFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(FeatureTypeService)
@Mock([Feature,FeatureType])
class FeatureTypeServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "can add a feature type"() {

        given: "no feature types"
        assert FeatureType.count==0

        when: "we add a Feature Type"
        service.createFeatureTypeForFeature(Gene.class,Gene.alternateCvTerm)
        FeatureType featureType = FeatureType.first()

        then: "we should have one"
        assert FeatureType.count==1
        assert featureType.ontologyId == Gene.ontologyId
        assert featureType.name == Gene.cvTerm
        assert featureType.type == "sequence"
    }
}
