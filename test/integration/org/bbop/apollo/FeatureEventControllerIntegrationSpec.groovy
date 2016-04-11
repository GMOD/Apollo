package org.bbop.apollo

import grails.test.spock.IntegrationSpec

class FeatureEventControllerIntegrationSpec extends IntegrationSpec {


    def setup() {
        Organism organism = new Organism(
                directory: "test/integration/resources/sequences/honeybee-Group1.10/"
                , commonName: "sampleAnimal"
        ).save(flush: true)
        Sequence sequence = new Sequence(
                length: 1405242
                , seqChunkSize: 20000
                , start: 0
                , organism: organism
                , end: 1405242
                , name: "Group1.10"
        ).save()
    }

    def cleanup() {
    }

    void "test changes"() {

        given: "A feature controller"
        def fc = new FeatureEventController()

        when: "When we run through the changes method"
        def model = fc.changes(10)

        then: "It does not create an error"
        println model
    }



}
