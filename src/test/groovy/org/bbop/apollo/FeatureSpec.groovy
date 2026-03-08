package org.bbop.apollo

import grails.testing.gorm.DataTest
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class FeatureSpec extends Specification implements DomainUnitTest<Feature>, DataTest {

    Class[] getDomainClassesToMock() {
        [Feature, FeatureLocation, Sequence]
    }

    def setup() {
        Sequence sequence = new Sequence(
                name: "Chr1"
                , start: 1
                , end: 1013
                , length: 1013
                , seqChunkSize: 50
        ).save(failOnError: true)

        Feature feature1 = new Feature(
                name: "Sox9a"
                , uniqueName: "ABC123"
                , sequenceLength: 17
        ).save(failOnError: true)

        FeatureLocation featureLocation = new FeatureLocation(
                fmin: 13
                , fmax: 77
                , feature: feature1
                , sequence: sequence
        ).save()

        feature1.addToFeatureLocations(featureLocation)
        feature1.save()
    }

    def cleanup() {
    }

    void "test feature clone copy"() {

        when: "If I clone a feature"
        Feature feature1 = Feature.first()
        Feature feature2 = feature1.generateClone()
        feature2.save()

        then: "It should be identical in all properties but the id and uniquename and relationships"
        assert Feature.count == 2
        assert FeatureLocation.count == 1
        assert Sequence.count == 1

        assert feature1.name == feature2.name
        assert feature1.uniqueName == feature2.uniqueName
        assert feature1.featureLocations.size() == feature2.featureLocations.size()
        assert feature1.featureLocations.size() == 1

        FeatureLocation featureLocation1 = feature1.featureLocation
        FeatureLocation featureLocation2 = feature2.featureLocation

        assert featureLocation1.fmin == featureLocation2.fmin
        assert featureLocation1.sequence == featureLocation2.sequence
        assert featureLocation1.fmax == featureLocation2.fmax
    }

    void "can I insert a feature with the same id?"() {
        when: "I create a feature"
        Feature feature = Feature.first()
        Long id = feature.id

        then: "should be a total of one valid feature"
        assert id != null
        assert Feature.count == 1
        assert feature != null
        assert feature.name == "Sox9a"

        when: "we delete that feature "
        feature.delete()

        then: "we have no features"
        assert Feature.count == 0

        when: "we create a feature when the same id"
        Feature feature1 = new Feature(
                name: "Sox9a"
                , uniqueName: "ABC123"
                , sequenceLength: 17
        )
        feature1.id = id
        feature1.save(failOnError: true)

        then: "should all be the same"
        assert feature1.id == id
        assert Feature.count == 1
        assert feature1.name == "Sox9a"
        assert feature != null
    }
}
