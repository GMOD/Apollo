package org.bbop.apollo

class PhoneHomeServiceIntegrationSpec extends AbstractIntegrationSpec {

    def phoneHomeService

    void "test ping"() {
        when: "we ping the server"
        def json = phoneHomeService.pingServer()

        then: "we should get an empty response"
        assert "{}" == json.toString()
    }

}
