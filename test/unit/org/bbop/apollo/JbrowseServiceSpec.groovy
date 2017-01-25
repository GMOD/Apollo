package org.bbop.apollo

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(JbrowseService)
class JbrowseServiceSpec extends Specification {

    String path1 = "yeast/include/myTrackMetaData.csv"
    String path2 = "/opt/apollo/yeast/"
    String path2a = "/opt/apollo/yeast"
    String path3 = "/opt/apollo/"
    String path4 = "/opt/apollo/critter"
    String finalPath = "/opt/apollo/yeast/include/myTrackMetaData.csv"

    def setup() {

    }

    def cleanup() {
    }

    void "test overlapping paths"() {

        when: "we try to do overlapping"

        then: "assert behaviors"
        assert service.hasOverlappingDirectory(path2,path1)
        assert service.hasOverlappingDirectory(path2a,path1)
        assert !service.hasOverlappingDirectory(path3,path1)
        assert !service.hasOverlappingDirectory(path4,path1)
    }

    void "test fixing paths"() {
        when: "we try to do overlapping"

        then: "assert behaviors"
        assert finalPath == service.fixOverlappingPath(path2,path1)
        assert finalPath == service.fixOverlappingPath(path2a,path1)
    }
}
