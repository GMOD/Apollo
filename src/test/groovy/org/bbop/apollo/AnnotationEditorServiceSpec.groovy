package org.bbop.apollo

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class AnnotationEditorServiceSpec extends Specification implements ServiceUnitTest<AnnotationEditorService>, DataTest {

    def setup() {
    }

    def cleanup() {
    }

    def "clean json"() {
        given: "a valid sequence to translate"
        String inputJSON = "\"{\\\"track\\\":\\\"chrI\\\",\\\"features\\\":[{\\\"location\\\":{\\\"fmin\\\":62840,\\\"fmax\\\":65405,\\\"strand\\\":1},\\\"type\\\":{\\\"cv\\\":{\\\"name\\\":\\\"sequence\\\"},\\\"name\\\":\\\"mRNA\\\"},\\\"name\\\":\\\"YAL041W\\\",\\\"children\\\":[{\\\"location\\\":{\\\"fmin\\\":62840,\\\"fmax\\\":65405,\\\"strand\\\":1},\\\"type\\\":{\\\"cv\\\":{\\\"name\\\":\\\"sequence\\\"},\\\"name\\\":\\\"exon\\\"}}]}],\\\"operation\\\":\\\"add_transcript\\\",\\\"clientToken\\\":\\\"9137872062024864552562677444\\\"}\""
        String validJSON = "{\"track\":\"chrI\",\"features\":[{\"location\":{\"fmin\":62840,\"fmax\":65405,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"YAL041W\",\"children\":[{\"location\":{\"fmin\":62840,\"fmax\":65405,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\",\"clientToken\":\"9137872062024864552562677444\"}"

        when: "the sequence string gets here"
        String outputJSON = service.cleanJSONString(inputJSON)

        then: "it should be good"
        assert validJSON == outputJSON
    }
}
