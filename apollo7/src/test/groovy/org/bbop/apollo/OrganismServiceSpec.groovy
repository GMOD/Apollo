package org.bbop.apollo

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import spock.lang.Specification

class OrganismServiceSpec extends Specification implements ServiceUnitTest<OrganismService>, DataTest {

    def setup() {
    }

    def cleanup() {
    }

    void "find a blat DB"() {

        when: "if we provide an empty directory"
        String tempPath1 = File.createTempDir().absolutePath
        String blatDb1 = service.findBlatDB(tempPath1)

        then: "we should return null"
        assert blatDb1 == null

        when: "we create a directory with the proper stuff"
        File tempPath2 = File.createTempDir()
        File searchDB = new File(tempPath2.absolutePath + "/" + FeatureStringEnum.SEARCH_DATABASE_DATA.value)
        assert searchDB.mkdir()
        File searchFile = new File(searchDB.absolutePath + "/testfile.2bit")
        assert searchFile.createNewFile()
        String blatDb2 = service.findBlatDB(tempPath2.absolutePath)

        then: "we should find the appropriate file"
        assert blatDb2 == searchFile.absolutePath
    }
}
