package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(SequenceController)
@Mock([Sequence,User,Genome,Organism,FeatureLocation,SequenceService])
class SequenceControllerSpec extends Specification {

    def populateValidParams(params) {
        assert params != null
        params["name"] = 'someValidName'
        params["seqChunkSize"] = 100
        params["start"] = 100
        params["end"] = 200
        params["length"] = 100
    }

}
