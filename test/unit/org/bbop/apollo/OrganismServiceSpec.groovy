package org.bbop.apollo

import grails.test.mixin.TestFor
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(OrganismService)
class OrganismServiceSpec extends Specification {

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
        File searchDB = new File(tempPath2.absolutePath+"/"+ FeatureStringEnum.SEARCH_DATABASE_DATA.value)
        assert searchDB.mkdir()
        File searchFile = new File(searchDB.absolutePath+"/testfile.2bit")
        assert searchFile.createNewFile()
        String blatDb2 = service.findBlatDB(tempPath2.absolutePath)

        then: "we should find the appropriate file"
        assert blatDb2==searchFile.absolutePath



    }



    
    
    
}
