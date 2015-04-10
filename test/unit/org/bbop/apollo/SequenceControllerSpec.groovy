package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(SequenceController)
@Mock([Sequence,User,Genome,Organism,FeatureLocation,SequenceService])
class SequenceControllerSpec extends Specification {

    def populateValidParams(params) {
        assert params != null
        // TODO: Populate valid properties like...
        params["name"] = 'someValidName'
//        params["sequenceType"] = "sequence:scaffold"
//        params["sequenceCV"] = "sequence:scaffold"
        params["refSeqsFile"] = "sequence:scaffold"
        params["seqChunkPrefix"] = "sequence:scaffold"
        params["seqChunkSize"] = 100
        params["start"] = 100
        params["end"] = 200
        params["length"] = 100
        params["dataDirectory"] = "sequence:scaffold"
        params["sequenceDirectory"] = "sequence:scaffold"
    }

}
