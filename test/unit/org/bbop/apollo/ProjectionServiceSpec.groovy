package org.bbop.apollo

import grails.test.mixin.TestFor
import org.bbop.apollo.projection.Coordinate
import org.bbop.apollo.projection.DiscontinuousProjection
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
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

    void "serialize a sequence list"(){

        given: "a projection with two limited sequences and two folded regions"
        MultiSequenceProjection projection = new MultiSequenceProjection()
        List<ProjectionSequence> projectionSequenceList = new ArrayList<>()
        ProjectionSequence projectionSequence1 = new ProjectionSequence(
                start: 7,
                end: 120,
                name: 'Group1',
                organism: 'Bug',
                order: 0
        )
        ProjectionSequence projectionSequence2 = new ProjectionSequence(
                start: 30,
                end: 150,
                name: 'Group2',
                organism: 'Bug',
                order: 1
        )
        projectionSequenceList.addAll([projectionSequence1,projectionSequence2])
        projection.addProjectionSequences(projectionSequenceList)
        projection.addInterval( 12,15,projectionSequence1 )
        projection.addInterval( 30,50,projectionSequence1 )
        projection.addInterval( 30,130,projectionSequence2 )
        projection.addInterval( 135,150,projectionSequence2 )

        when: "we serialize to JSON objects"
        JSONObject projectionJsonObject = service.convertToJsonFromProjection(projection)
        JSONArray projectionSequenceListArray = projectionJsonObject.getJSONArray(org.bbop.apollo.gwt.shared.FeatureStringEnum.SEQUENCE_LIST.value)
        JSONObject firstProjection = projectionSequenceListArray.getJSONObject(0)
        JSONObject lastProjection = projectionSequenceListArray.getJSONObject(1)

        then: "we should see the appropriate number and type of objects in the JSON Array"
        assert projectionSequenceListArray.size()==2
        firstProjection.name == "Group1"
        firstProjection.start == 7
        firstProjection.end == 120
        firstProjection.organism == "Bug"
        firstProjection.order == 0

        lastProjection.name == "Group2"
        lastProjection.start == 30
        lastProjection.end == 150
        lastProjection.organism == "Bug"
        lastProjection.order == 1


        when: "de-serialize it"
        println "json ${projectionJsonObject}"
        MultiSequenceProjection retrievedProjection = service.convertToProjectionFromJson(projectionJsonObject)
        List<ProjectionSequence> retrievedProjectionSequenceList = retrievedProjection.getProjectedSequences()
        ProjectionSequence firstSequence = retrievedProjectionSequenceList.first()
        DiscontinuousProjection firstDiscontinuousProjection = retrievedProjection.getProjectionForSequence(firstSequence)
        List<Coordinate> firstCoordinates = firstDiscontinuousProjection.getCoordinates()

        ProjectionSequence lastSequence = retrievedProjectionSequenceList.last()
        DiscontinuousProjection lastDiscontinuousProjection = retrievedProjection.getProjectionForSequence(lastSequence)
        List<Coordinate> lastCoordinates = lastDiscontinuousProjection.getCoordinates()

        then: "we should have created the exact same object"
        assert retrievedProjectionSequenceList.size()==2
        assert firstSequence.start == projectionSequence1.start
        assert firstSequence.end == projectionSequence1.end
        assert firstSequence.name == projectionSequence1.name
        assert firstSequence.organism == projectionSequence1.organism
        assert firstSequence.order == 0
        assert lastSequence.start == projectionSequence2.start
        assert lastSequence.end == projectionSequence2.end
        assert lastSequence.name == projectionSequence2.name
        assert lastSequence.organism == projectionSequence2.organism
        assert lastSequence.order == 1

        assert firstCoordinates.size()==2
        assert firstCoordinates.first().min==12
        assert firstCoordinates.first().max==15
        assert firstCoordinates.last().min==30
        assert firstCoordinates.last().max==50
        assert lastCoordinates.size()==2
        assert lastCoordinates.first().min==30
        assert lastCoordinates.first().max==130
        assert lastCoordinates.last().min==135
        assert lastCoordinates.last().max==150


    }
}
