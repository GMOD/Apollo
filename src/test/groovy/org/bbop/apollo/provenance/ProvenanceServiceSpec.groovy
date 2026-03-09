package org.bbop.apollo.provenance

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.bbop.apollo.Feature
import org.bbop.apollo.Provenance
import spock.lang.Specification

class ProvenanceServiceSpec extends Specification implements ServiceUnitTest<ProvenanceService>, DataTest {

    Class[] getDomainClassesToMock() {
        [Provenance, Feature]
    }

    void "test parsing basic input"() {
        given:
        String inputString = "rank=1;field=source;db_xref=PMID:123456;evidence=ECO:0000318;note=[];based_on=[]"

        when:
        List<Provenance> provenances = service.convertGff3StringToProvenances(inputString)

        then:
        provenances.size() == 1
        provenances[0].field == "source"
        provenances[0].reference == "PMID:123456"
        provenances[0].evidenceRef == "ECO:0000318"
    }

    void "test parsing URL-encoded values"() {
        given: "a GFF3 string with percent-encoded special characters in values"
        String inputString = "rank=1;field=source;db_xref=PMID%3A123456;evidence=ECO%3A0000318;note=curator%20note%20here"

        when:
        List<Provenance> provenances = service.convertGff3StringToProvenances(inputString)

        then:
        provenances.size() == 1
        provenances[0].reference == "PMID:123456"
        provenances[0].evidenceRef == "ECO:0000318"
        provenances[0].notesArray == "curator note here"
    }
}
