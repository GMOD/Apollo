package org.bbop.apollo

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class Gff3HandlerServiceSpec extends Specification implements ServiceUnitTest<Gff3HandlerService>, DataTest {

    Class[] getDomainClassesToMock() {
        [Sequence, Gene, MRNA, Exon, CDS, Feature, FeatureLocation, FeatureRelationship]
    }

    def setup() {
        new Sequence(
                length: 3
                , seqChunkSize: 3
                , start: 5
                , end: 8
                , name: "Group-1.10"
        ).save()
    }

    def cleanup() {
        Sequence.deleteAll(Sequence.all)
        FeatureRelationship.deleteAll(FeatureRelationship.all)
        FeatureLocation.deleteAll(FeatureLocation.all)
        Feature.deleteAll(Feature.all)
    }

    void "test new date format"() {
        given: "a date and expected result"
        String expectedOutput1 = "2001-02-03"

        when: "we format the string"
        Calendar calendar = new GregorianCalendar()
        calendar.set(Calendar.YEAR, 2001)
        calendar.set(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 3)

        String outputDateString = service.formatDate(calendar.time)

        then: "we should be able to"
        assert expectedOutput1 == outputDateString
    }
}
