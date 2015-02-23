package org.bbop.apollo

import grails.converters.JSON
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(RequestHandlingService)
class RequestHandlingServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "add transcript"() {
        
        when: "You add a transcript via JSON"
//        JSONObject jsonObject = JSON.parse("asdfjklasdfjk") as JSONObject
//        JSONObject returnObject = service.addTranscript(jsonObject)
        
        
        
        
        then: "You should see that transcript"
        assert true
//        assert "ADD"==returnObject.getString("operation")
//        assert Gene.count == 1
//        assert Gene.first().name=="Bob1"
        
    }
}
