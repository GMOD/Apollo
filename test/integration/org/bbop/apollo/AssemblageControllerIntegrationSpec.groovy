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


    void "should be able to fold two transcripts "() {

        given: "add transcript and split exon string"
        String addTranscriptGb52236BigExonString = "{ ${testCredentials} \"track\":{\"id\":31503, \"name\":\"Group11.4\", \"padding\":0, \"start\":0, \"end\":75085, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"features\":[{\"location\":{\"fmin\":52853,\"fmax\":58962,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB52236-RA\",\"children\":[{\"location\":{\"fmin\":58860,\"fmax\":58962,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":52853,\"fmax\":55971,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":52853,\"fmax\":56051,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":56605,\"fmax\":56984,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":58470,\"fmax\":58698,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":58800,\"fmax\":58962,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":55971,\"fmax\":58860,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}],\"operation\":\"add_transcript\"}"
        String getFeatures11_4String = "{ ${testCredentials} \"track\":{\"id\":31503, \"name\":\"Group11.4\", \"padding\":0, \"start\":0, \"end\":75085, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":0, \"end\":75085}]},\"operation\":\"get_features\"}"
//        String getFeatures11_4ProjectedString = "{ ${testCredentials} \"track\":{\"name\":\"GB52238-RA (Group11.4)::GB52236-RA (Group11.4)\", \"padding\":0, \"start\":10257, \"end\":77558, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}},{\"name\":\"Group11.4\", \"start\":52653, \"end\":59162, \"feature\":{\"name\":\"GB52236-RA\"}}]},\"operation\":\"get_features\"}"
        String foldCommandString = "{\"sequence\":{\"id\":8177, \"name\":\"Group11.4GB52236-RA-00001 Group11.4\", \"description\":\"GB52236-RA-00001 (Group11.4)\", \"padding\":0, \"start\":52803, \"end\":59012, \"sequenceList\":[{\"name\":\"Group11.4\", \"start\":52803, \"end\":59012, \"reverse\":false, \"feature\":{\"fmin\":52803, \"start\":52803, \"name\":\"GB52236-RA-00001\", \"end\":59012, \"fmax\":59012, \"parent_id\":\"Group11.4\"}, \"location\":[{\"fmin\":52803, \"strand\":-1, \"fmax\":59012}]}]}, \"features\":[{\"uniquename\":\"@EXON_UNIQUENAME_1@\"},{\"uniquename\":\"@EXON_UNIQUENAME_2@\"}]}"

        when: "we add the transcript and get the feature"
        requestHandlingService.addTranscript(JSON.parse(addTranscriptGb52236BigExonString))
        JSONObject featuresObject = requestHandlingService.getFeatures(JSON.parse(getFeatures11_4String))
        JSONArray featuresArray = featuresObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        then: "we should see the entire feature"
        assert 1 == featuresArray.size()
        assert 8 == featuresArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value).size() + featuresArray.getJSONObject(1).getJSONArray(FeatureStringEnum.CHILDREN.value).size()
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 4
        assert CDS.count == 1


        when: "we add the transcript and get the projected feature"
        featuresObject = assemblageController.getFeatures(JSON.parse(foldCommandString))
        featuresArray = featuresObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        then: "we should see the entire feature"
//        assert 2 == featuresArray.size()
//        assert 15 == featuresArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value).size() + featuresArray.getJSONObject(1).getJSONArray(FeatureStringEnum.CHILDREN.value).size()

    }
}
