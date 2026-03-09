package org.bbop.apollo.go

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.bbop.apollo.Feature
import spock.lang.Specification

class GoAnnotationServiceSpec extends Specification implements ServiceUnitTest<GoAnnotationService>, DataTest {

    Class[] getDomainClassesToMock() {
        [GoAnnotation, Feature]
    }

    void "test parsing basic input"() {
        given:
        String inputString = "rank=1;aspect=P;term=GO:0006099;db_xref=PMID:123456;evidence=ECO:0000318;note=[];based_on=[]"

        when:
        List<GoAnnotation> goAnnotations = service.convertGff3StringToGoAnnotations(inputString)

        then:
        goAnnotations.size() == 1
        goAnnotations[0].aspect == "P"
        goAnnotations[0].goRef == "GO:0006099"
        goAnnotations[0].reference == "PMID:123456"
        goAnnotations[0].evidenceRef == "ECO:0000318"
    }

    void "test parsing URL-encoded values"() {
        given: "a GFF3 string with percent-encoded special characters in values"
        String inputString = "rank=1;aspect=P;term=GO%3A0006099;db_xref=PMID%3A123456;evidence=ECO%3A0000318;note=contains%20a%20note"

        when:
        List<GoAnnotation> goAnnotations = service.convertGff3StringToGoAnnotations(inputString)

        then:
        goAnnotations.size() == 1
        goAnnotations[0].aspect == "P"
        goAnnotations[0].goRef == "GO:0006099"
        goAnnotations[0].reference == "PMID:123456"
        goAnnotations[0].evidenceRef == "ECO:0000318"
        goAnnotations[0].notesArray == "contains a note"
    }
}
