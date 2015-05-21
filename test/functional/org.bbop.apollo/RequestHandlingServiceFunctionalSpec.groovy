package org.bbop.apollo



class RequestHandlginServiceFunctionalSpec extends IntegrationSpec {

    @Shared
    def grailsApplication

    def setup() {
    }

    def cleanup() {
    }

    void "create a merge an exon"() {
        given:
        RestBuilder rest = new RestBuilder()

        when:
//        RestResponse response = rest.post("http://localhost:8080/${grailsApplication.metadata.'app.name'}/books") {
//            json([
//                    title: "title2"
//            ])
//        }

        then:
        response.status == 200
//        response.json.title == "title2"
        Book.count == 2
    }
}