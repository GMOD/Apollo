package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(RequestHandlingService)
@Mock([Sequence, FeatureLocation, Feature,MRNA])
class RequestHandlingServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    def "something to test"(){
        
        
    }
}
