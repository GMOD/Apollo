package org.bbop.apollo.geneProduct

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.bbop.apollo.Feature
import org.bbop.apollo.FeatureLocation
import org.bbop.apollo.FeatureRelationship
import org.bbop.apollo.Sequence
import spock.lang.Specification

class GeneProductServiceSpec extends Specification implements ServiceUnitTest<GeneProductService>, DataTest {

    Class[] getDomainClassesToMock() {
        [Feature, GeneProduct]
    }

    def setup() {
    }

    def cleanup() {
    }

    void "test parsing input"() {
        given: "given an input string"
        String inputString = "rank=1;term=trans prod 1;db_xref=transprodref1:1111;evidence=ECO:0000318;alternate=true;note=[];based_on=['transprod1wtih2:2222','transprod1with1:1111'];last_updated=2020-03-12 12:01:01.382;date_created=2020-03-12 12:01:01.38,rank=2;term=trans prod 2;db_xref=transref2:2222;evidence=ECO:0000315;alternate=false;note=[];based_on=['trandprod2:33333'];last_updated=2020-03-12 12:01:28.469;date_created=2020-03-12 12:01:28.469"

        when: "we format the string"
        List<GeneProduct> geneProducts = service.convertGff3StringToGeneProducts(inputString)

        then: "we should be able to"
        assert geneProducts.size() == 2
    }

    void "test parsing URL-encoded values"() {
        given: "a GFF3 string with percent-encoded special characters in values"
        String inputString = "term=ATP%20synthase%20subunit;db_xref=UniProtKB%3AQ8I3E7;evidence=ECO%3A0000318;note=contains%20a%20colon%3A%20important"

        when:
        List<GeneProduct> geneProducts = service.convertGff3StringToGeneProducts(inputString)

        then:
        assert geneProducts.size() == 1
        assert geneProducts[0].productName == "ATP synthase subunit"
        assert geneProducts[0].reference == "UniProtKB:Q8I3E7"
        assert geneProducts[0].evidenceRef == "ECO:0000318"
        assert geneProducts[0].notesArray == "contains a colon: important"
    }
}
