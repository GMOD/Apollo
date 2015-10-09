package org.bbop.apollo

import grails.converters.JSON
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.sequence.Strand
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(FeatureService)
@Mock([Sequence, FeatureLocation, Feature,MRNA])
class FeatureServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "convert JSON to Feature Location"() {

        when: "We have a valid json object"
        JSONObject jsonObject = new JSONObject()
        Sequence sequence = new Sequence(
                name: "Chr3",
                seqChunkSize: 20,
                start: 1,
                end: 100,
                length: 99,
        ).save(failOnError: true)
        jsonObject.put(FeatureStringEnum.FMIN.value, 73)
        jsonObject.put(FeatureStringEnum.FMAX.value, 113)
        jsonObject.put(FeatureStringEnum.STRAND.value, Strand.POSITIVE.value)


        then: "We should return a valid FeatureLocation"
        FeatureLocation featureLocation = service.convertJSONToFeatureLocation(jsonObject, sequence)
        assert featureLocation.sequence.name == "Chr3"
        assert featureLocation.fmin == 73
        assert featureLocation.fmax == 113
        assert featureLocation.strand == Strand.POSITIVE.value


    }

    void "convert JSON to Ontology ID"() {
        when: "We hav a json object of type"
        JSONObject json = JSON.parse("{name:exon, cv:{name:sequence}}")

        then: "We should be able to infer the ontology ID"
        String ontologyId = service.convertJSONToOntologyId(json)
        assert ontologyId != null
        assert ontologyId == Exon.ontologyId
    }


    
    
    
}
