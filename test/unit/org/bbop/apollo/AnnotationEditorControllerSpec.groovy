package org.bbop.apollo

import grails.converters.JSON
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.web.JSONBuilder
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.parser.JSONParser
import org.json.JSONObject
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(AnnotationEditorController)
@Mock([Sequence, Genome, Feature, FeatureLocation,FeatureService,Organism])
class AnnotationEditorControllerSpec extends Specification {

    def setup() {
        Sequence sequence = new Sequence(
                name: "chromosome7"
                , sequenceType: "scaffold"
                , sequenceCV: "contig"
        ).save(failOnError: true)

        Feature feature = new Feature(
                name: "abc123"
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

    void "get_features"() {

        when: "converts properly"
        def builder = new JSONBuilder()
        params.data = builder.build {
            operation = "get_features"
            track = "chromosome7"
        }.toString()

        controller.getFeatures()

        then: "we should have some features "

        String responseString = controller.response.contentAsString
        JSONElement jsonObject = JSON.parse(responseString)
        assert jsonObject.getAt("operation") == "get_features"
        assert jsonObject.getAt("track") == "chromosome7"
        println jsonObject.getAt("features")
        JSONArray array = jsonObject.getAt("features")
        assert 1 == array.size()
        JSONElement element = array.get(0)
        element.getAt("name") == "abc123"


    }

    // TODO: move to integration tests

//    void "add_transcript"() {
//
//        when: "we send JSON towards our controller to add a transcript"
//        Organism organism = new Organism(
//                genus: "Danio"
//                ,species: "rerio"
//                ,abbreviation: "ZF"
//                ,commonName: "Zebrafish"
//        ).save(failOnError: true)
//        Sequence sequence = new Sequence(name:  "Annotations-Group1.1",organism: organism).save(failOnError: true)
//        JSONBuilder builder = new JSONBuilder()
////        JSON json = builder.build
//        String jsonString = ('{ "track": "Annotations-Group1.1", "features": [{"location":{"fmin":88364,"fmax":88550,"strand":-1},"type":{"cv":{"name":"sequence"},"name":"mRNA"},"name":"geneid_mRNA_CM000054.5_3","children":[{"location":{"fmin":88364,"fmax":88550,"strand":-1},"type":{"cv":{"name":"sequence"},"name":"exon"}}]}], "operation": "add_transcript" }' )
//        JSONElement rootJsonObject = JSON.parse(jsonString)
//        println rootJsonObject
//
////        def map = {
////            track: "Annotations-Group1.1"
////            features:
////            [{
////                 location {
////                     fmin 88364
////                     fmax 88550
////                     strand - 1
////                 }
////                 type {
////                     cv { name "sequence" }
////                     name "mRNA"
////                 }
////                 name "geneid_mRNA_CM000054.5_3"
////                 children [
////                         {
////                             location {
////                         fmin 88364
////                         fmax 88550
////                         strand - 1
////                         }
////                     type {
////                         cv {
////                             name "sequence"
////                         }
////                         name "exon"
////                     }
////                 }]
////             }]
////            operation "add_transcript"
////        }
//
////        JSON jsonObject = map as JSON
////        println "MAP: " + jsonObject
//
//        params.data = jsonString
//
//        controller.addTranscript()
//
//        then: "It should add a transcript"
//        assert true
//
//    }

}
