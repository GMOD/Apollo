package org.bbop.apollo

import grails.converters.JSON
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(FeatureService)
class FeatureServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "convertJSON to Ontology ID"() {

        when: "We hav a json object of type"
        JSONObject json = JSON.parse("[name:exon, cv:[name:sequence]]")


        then: "We should be able to infer the ontology ID"
        String ontologyId = service.convertJSONToOntologyId(json)
        assert ontologyId != null
        assert ontologyId == Exon.ontologyId
        
    }
}
