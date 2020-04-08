package org.bbop.apollo.geneProduct

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.bbop.apollo.Feature
import org.bbop.apollo.FeatureLocation
import org.bbop.apollo.FeatureRelationship
import org.bbop.apollo.Sequence
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(GeneProductService)
@Mock([Feature, GeneProduct])
class GeneProductServiceSpec extends Specification {


    def setup() {
    }

    def cleanup() {
        Sequence.deleteAll(Sequence.all)
        FeatureRelationship.deleteAll(FeatureRelationship.all)
        FeatureLocation.deleteAll(FeatureLocation.all)
        Feature.deleteAll(Feature.all)
    }


    void "test parsing input"(){
        given: "given an inputt string"
        String inputString = "rank=1;term=trans prod 1;db_xref=transprodref1:1111;evidence=ECO:0000318;alternate=true;note=[];based_on=['transprod1wtih2:2222','transprod1with1:1111'];last_updated=2020-03-12 12:01:01.382;date_created=2020-03-12 12:01:01.38,rank=2;term=trans prod 2;db_xref=transref2:2222;evidence=ECO:0000315;alternate=false;note=[];based_on=['trandprod2:33333'];last_updated=2020-03-12 12:01:28.469;date_created=2020-03-12 12:01:28.469"

        when: "we format the string"
        List<GeneProduct> geneProducts = service.convertGff3StringToGeneProducts(inputString)

        then: "we should be able to"
        assert geneProducts.size()==2
    }
}