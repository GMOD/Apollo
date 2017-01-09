package org.bbop.apollo

import grails.converters.JSON
import grails.test.mixin.TestFor
import org.bbop.apollo.gwt.shared.projection.Coordinate
import org.bbop.apollo.gwt.shared.projection.DiscontinuousProjection
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ProjectionService)
class ProjectionServiceSpec extends Specification {


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
        JSONArray projectionSequenceListArray = projectionJsonObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value)
        JSONObject firstProjection = projectionSequenceListArray.getJSONObject(0)
        JSONObject lastProjection = projectionSequenceListArray.getJSONObject(1)

        then: "we should see the appropriate number and type of objects in the JSON Array"
        assert projectionSequenceListArray.size()==2
        assert firstProjection.name == "Group1"
        assert firstProjection.start == 7
        assert firstProjection.end == 120
        assert firstProjection.organism == "Bug"
        assert firstProjection.order == 0
        assert firstProjection.feature != null
        assert firstProjection.location.size() == 2
        assert firstProjection.location[0].fmin==30
        assert firstProjection.location[0].fmax==50
        assert firstProjection.location[1].fmin==12
        assert firstProjection.location[1].fmax==15

        assert lastProjection.name == "Group2"
        assert lastProjection.start == 30
        assert lastProjection.end == 150
        assert lastProjection.organism == "Bug"
        assert lastProjection.order == 1
        assert lastProjection.feature != null
        assert lastProjection.location.size() == 2
        assert lastProjection.location[0].fmin==135
        assert lastProjection.location[0].fmax==150
        assert lastProjection.location[1].fmin==30
        assert lastProjection.location[1].fmax==130


        when: "de-serialize it"
        println "json ${projectionJsonObject}"
        MultiSequenceProjection retrievedProjection = service.convertToProjectionFromJson(projectionJsonObject)
        List<ProjectionSequence> retrievedProjectionSequenceList = retrievedProjection.getProjectedSequences()
        ProjectionSequence firstSequence = retrievedProjectionSequenceList.first()
        DiscontinuousProjection firstDiscontinuousProjection = retrievedProjection.getProjectionForSequence(firstSequence)
        Collection<Coordinate> firstCoordinates = firstDiscontinuousProjection.getCoordinates()

        ProjectionSequence lastSequence = retrievedProjectionSequenceList.last()
        DiscontinuousProjection lastDiscontinuousProjection = retrievedProjection.getProjectionForSequence(lastSequence)
        Collection<Coordinate> lastCoordinates = lastDiscontinuousProjection.getCoordinates()

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
        assert firstCoordinates.first().min==30L
        assert firstCoordinates.first().max==50L
        assert firstCoordinates.last().min==12L
        assert firstCoordinates.last().max==15L
        assert lastCoordinates.size()==2
        assert lastCoordinates.first().min==135L
        assert lastCoordinates.first().max==150L
        assert lastCoordinates.last().min==30L
        assert lastCoordinates.last().max==130L


    }


    void "split projection from service"(){
        given: "a projection"
        MultiSequenceProjection projection = new MultiSequenceProjection()
        ProjectionSequence projectionSequence1 = new ProjectionSequence(
                start: 52803,
                end: 59012,
                name: 'Group11.4',
                organism: 'Bug',
                order: 0
        )
        projection.addProjectionSequences([projectionSequence1])


        when: "we add a coordinate"
        projection.addInterval(52803L,57034L,projectionSequence1)

        then: "we should see it"
        assert projection.size()==1

        when: "we add a coordinate"
        projection.addInterval(58420l,59012l,projectionSequence1)

        then: "we should see it"
        assert projection.size()==2

    }

    void "generate name from refSeq"(){
        given: "a json object"
        String jsonString = "{seqChunkSize: 20000, length: 1382403, name: \"Group1.1\", start: 0, end: 1382403}"
//        String expectedString = "{\"id\":9796,\"name\":\"Group1.1\",\"description\":\"Group1.1\",\"padding\":0,\"start\":0,\"end\":1382403,\"sequenceList\":[{\"name\":\"Group1.1\",\"start\":0,\"end\":1382403,\"reverse\":false}]}:97510..378397"
        String expectedString = "{\"seqChunkSize\":20000,\"length\":1382403,\"name\":\"Group1.1\",\"start\":0,\"end\":1382403,\"sequenceList\":[{\"seqChunkSize\":20000,\"length\":1382403,\"name\":\"Group1.1\",\"start\":0,\"end\":1382403}]}:0..1382403"

        when:"we parse the string"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        String name = service.generateNameForObjcet(jsonObject)

        then: "we should get the appropriate name"
        assert name==expectedString

    }


}
