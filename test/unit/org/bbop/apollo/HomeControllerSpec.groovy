package org.bbop.apollo

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class HomeControllerSpec extends Specification {



    def setup() {
    }

    def cleanup() {
    }

    void "get method name"() {

        given: "A home controller"
        HomeController homeController = new HomeController()
        String methodClassNAme = "org.bbop.apollo.Test.Timerabcd123"

        when: "we get the method and class name"
        String methodName = homeController.getMethodName(methodClassNAme)
        String className = homeController.getClassName(methodClassNAme)

        then: "we assert that they are correct"
        assert methodName=="abcd123"
        assert className=="Test"
    }
}
