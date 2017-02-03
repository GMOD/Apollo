package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Ignore

class OrganismServiceIntegrationSpec extends AbstractIntegrationSpec{

    def organismService
    def requestHandlingService


    @Ignore
    void "deleteAllFeaturesFromOrganism"() {

        given: "a transcript with a UTR"
        String jsonString = " { ${testCredentials} \"track\": \"Group1.10\", \"features\": [{\"location\":{\"fmin\":1216824,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235534,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1216850,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1224676,\"fmax\":1224823,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1228682,\"fmax\":1228825,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235237,\"fmax\":1235396,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1235487,\"fmax\":1235616,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}},{\"location\":{\"fmin\":1216824,\"fmax\":1235534,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"CDS\"}}]}], \"operation\": \"add_transcript\" }"
        String validJSONString = "{\"features\":[{\"location\":{\"fmin\":1216824,\"strand\":1,\"fmax\":1235616},\"parent_type\":{\"name\":\"gene\",\"cv\":{\"name\":\"sequence\"}},\"name\":\"GB40856-RA\",\"children\":[{\"location\":{\"fmin\":1235237,\"strand\":1,\"fmax\":1235396},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"@TRANSCRIPT_NAME@\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1216824,\"strand\":1,\"fmax\":1216850},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"0992325F0DD2290AB58EA37ECF2DA2E7\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1235487,\"strand\":1,\"fmax\":1235616},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"1C091FE87A8133803A69887F38FBDC4C\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209542,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1224676,\"strand\":1,\"fmax\":1224823},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"6D2E15D6DA759C523B79B96795927CAF\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1228682,\"strand\":1,\"fmax\":1228825},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"99C2A027C87DBDBC5536503D5C38F21C\",\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"},{\"location\":{\"fmin\":1216824,\"strand\":1,\"fmax\":1235534},\"parent_type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"994B96C6594F5DB1B6C836E6E0EDE2A6\",\"type\":{\"name\":\"CDS\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209540,\"parent_id\":\"5A8C864885BC71606E120322CE0EC28C\"}],\"properties\":[{\"value\":\"demo\",\"type\":{\"name\":\"owner\",\"cv\":{\"name\":\"feature_property\"}}}],\"uniquename\":\"5A8C864885BC71606E120322CE0EC28C\",\"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},\"date_last_modified\":1425583209602,\"parent_id\":\"8B9E9AC4D0DB90464F26B2F77A1E09B4\"}]}"


        when: "You add a transcript via JSON"
        JSONObject jsonObject = JSON.parse(jsonString) as JSONObject
        JSONObject returnObject = requestHandlingService.addTranscript(jsonObject)

        then: "there should be no features"
        assert Organism.count == 1
        assert Sequence.count == 1
        assert Organism.first().sequences?.size() == 1
        // there are 6 exons, but 2 of them overlap . . . so this is correct
        assert Exon.count == 5
        assert CDS.count == 1
        assert MRNA.count == 1
        assert Gene.count == 1
        assert FeatureEvent.count == 1

        // this is the new part
        assert FeatureLocation.count == 8
        assert Feature.count == 8

        when: "we call delete all for organism"
        int deleted = organismService.deleteAllFeaturesForOrganism(Organism.first())


        then: "we should not have any features left"
//        assert 1==deleted
        assert 1==Sequence.count
        assert 1==Organism.count
        assert null==Sequence.first()?.featureLocations
        assert 0==FeatureLocation.count
        assert 0==FeatureRelationship.count
        assert 0==Feature.count
        assert 0==FeatureEvent.count

    }
}
