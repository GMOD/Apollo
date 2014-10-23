package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(AnnotationEditorController)
@Mock([Sequence,Genome,Feature])
class AnnotationEditorControllerSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "get_features"() {

        when: "converts properly"
        Sequence sequence = new Sequence(name: "chromosome7").save()
        params.operation="get_features"
        params.track="chromosome7"
        def jsonReturn = controller.handleOperation(params)
        then:
        println jsonReturn
        assert jsonReturn != null

    }
}
