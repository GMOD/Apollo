package org.bbop.apollo

import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import grails.test.spock.IntegrationSpec
import spock.lang.Shared


class RequestHandlingServiceFunctionalSpec extends IntegrationSpec {

//    @Shared
//    def grailsApplication

    def setup() {
    }

    def cleanup() {
    }

    void "create a merge an exon"() {
        given:
        RestBuilder rest = new RestBuilder()

        when:
//        RestResponse response = rest.post("http://localhost:8080/${grailsApplication.metadata.'app.name'}/organism/findAllOrganisms") {
            RestResponse response = rest.post("http://localhost:8080/apollo/organism/findAllOrganisms") {
            json([
                    username: "ndunn@me.com"
                    ,password: "demo"
            ])
        }

        then:
        response.status == 200
        println "json ${response.json}"
//        response.json.title == "title2"
//        Book.count == 2
    }
}