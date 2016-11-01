package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Ignore

class AssemblageControllerIntegrationSpec extends AbstractIntegrationSpec {

    def requestHandlingService
    def projectionService
    def assemblageController

    def setup() {

        setupDefaultUserOrg()

        Organism organism = Organism.first()
        organism.directory = "test/integration/resources/sequences/honeybee-tracks/"
        organism.save(failOnError: true, flush: true)
        new Sequence(
                length: 75085
                , seqChunkSize: 20000
                , start: 0
                , end: 75085
                , organism: organism
                , name: "Group11.4"
        ).save(failOnError: true)

        new Sequence(
                length: 78258
                , seqChunkSize: 20000
                , start: 0
                , end: 78258
                , organism: organism
                , name: "GroupUn87"
        ).save(failOnError: true)

    }


}
