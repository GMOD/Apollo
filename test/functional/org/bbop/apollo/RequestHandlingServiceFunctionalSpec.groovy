package org.bbop.apollo

import geb.spock.GebSpec
import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse


//class RequestHandlingServiceFunctionalSpec extends IntegrationSpec {
class RequestHandlingServiceFunctionalSpec extends GebSpec {

//    @Shared
//    def grailsApplication

    def setup() {
    }

    def cleanup() {
    }

    /**
     * Please note . . this has to work in a naive environment where "/apollo" may not be configured or runnable.
     */
    void "can we hit a basic request"() {
        given:
        RestBuilder rest = new RestBuilder()

        when:
        def a = 3
//        RestResponse response = rest.post("http://localhost:8080/apollo/organism/findAllOrganisms?admin=true") {
//            json([
//                    username  : "ndunn@me.com"
//                    , password: "demo"
//            ])
//        }

        then:
        assert 1>0
//        response.status == 200
//        println "json ${response.json}"
        // realize that this is not admin
    }


//    void "moving exon boundaries should accurately"() {
//        given:
//        RestBuilder rest = new RestBuilder()
//
//        when:
//        JSONObject payload = new JSONObject()
//        RestResponse response = rest.post("http://localhost:8080/apollo/annotationEditor/addTranscript") {
//            json( payload )
//        }
//
//        then:
//        response.status == 200
//        println "json ${response.json}"
//        // realize that this is not admin
//
//        when: "we have an exon"
//    }

}
