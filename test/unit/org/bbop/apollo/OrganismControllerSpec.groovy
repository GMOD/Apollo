package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(OrganismController)
@Mock(Organism)
class OrganismControllerSpec extends Specification {

    def populateValidParams(params) {
        assert params != null
        // TODO: Populate valid properties like...
        params["genus"] = 'Apis'
        params["species"] = 'mellifera'
        params["commonName"] = 'Honeybee'
        params["directory"] = 'test/integration/resources/sequences/honeybee-Group1.10/'
    }


}
