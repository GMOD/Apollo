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
        params["abbreviation"] = 'ZF'
        params["genus"] = 'Danio'
        params["species"] = 'rerio'
        params["commonName"] = 'Zebrafish'
        params["directory"] = '/opt/apollo/organism1/jbrowse/data'
    }


}
