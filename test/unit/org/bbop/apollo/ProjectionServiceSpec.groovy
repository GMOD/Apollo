package org.bbop.apollo

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ProjectionService)
class ProjectionServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "get track name"() {
        when: "if we have a trackData name"
        String trackDataFileName = "/opt/apollo/honeybee/data/tracks/Official Gene Set v3.2/Group1.1/trackData.json"

        then: "we get out the track name and session name"
        assert "Official Gene Set v3.2"==service.getTrackName(trackDataFileName)
    }

    void "get sequence name"() {
        when: "if we have a trackData name"
        String trackDataFileName = "/opt/apollo/honeybee/data/tracks/Official Gene Set v3.2/Group1.1/trackData.json"

        then: "we get out the track name and session name"
        assert "Group1.1"==service.getSequenceName(trackDataFileName)
    }
}
