package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.web.JSONBuilder
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.parser.JSONParser
import spock.lang.Ignore
import spock.lang.Specification

//import org.codehaus.groovy.grails.web.json.parser.JSONParser
//import org.json.JSONObject
/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(AnnotationEditorController)
@Mock([Sequence, Genome, Feature, FeatureLocation,Organism,FeatureService])
class AnnotationEditorControllerSpec extends Specification {



    def setup() {
        controller.featureService = Mock(FeatureService)
//        Sequence sequence = new Sequence(
//                name: "chromosome7"
////                , sequenceType: "scaffold"
////                , sequenceCV: "contig"
//        ).save(failOnError: true)

        Sequence sequence = new Sequence(
//                organism: organism
                length: 3
                ,refSeqFile: "adsf"
                ,seqChunkPrefix: "asdf"
                ,seqChunkSize: 3
                ,start: 5
                ,end: 8
//                ,dataDirectory: "adsfads"
                ,sequenceDirectory: "asdfadsf"
                ,name: "chromosome7"
        ).save(failOnError: true)

        Feature feature = new Feature(
                name: "abc123"
                ,uniqueName: "uniqueAbac123"
        ).save(failOnError: true)

        FeatureLocation featureLocation = new FeatureLocation(
                feature: feature
                , sequence: sequence
                , fmin: 100
                , fmax: 200
        ).save(failOnError: true)
        feature.addToFeatureLocations(featureLocation)
        sequence.addToFeatureLocations(featureLocation)
    }

    def cleanup() {
    }

    // TODO: move this to an integration test
    @Ignore
    void "get_features"() {

        when: "converts properly"
        def builder = new JSONBuilder()
        params.data = builder.build {
            operation = "get_features"
            track = "chromosome7"
        }.toString()
        println "Feature.count: " + Feature.count

        controller.getFeatures()

        then: "we should have some features "

        String responseString = controller.response.contentAsString
        JSONParser parser = new JSONParser(new StringReader(responseString))
        JSONObject jsonObject = (JSONObject) parser.parseJSON()
        assert jsonObject.get("operation") == "get_features"
        assert jsonObject.get("track") == "chromosome7"
        println jsonObject.getJSONArray("features")
        JSONArray array = jsonObject.getJSONArray("features")
        println "array ${array}"
        assert 1 == array.size()
        JSONObject element = array.getJSONObject(0)
        element.get("name") == "abc123"


    }
}
