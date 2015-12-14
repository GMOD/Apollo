package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(Gff3HandlerService)
@Mock([Sequence,Gene,MRNA,Exon,CDS,Feature,FeatureLocation,FeatureRelationship,FeatureRelationshipService ])
class Gff3HandlerServiceSpec extends Specification {
    

    def setup() {
        new Sequence(
                length: 3
                ,seqChunkSize: 3
                ,start: 5
                ,end: 8
                ,name: "Group-1.10"
        ).save()
    }

    def cleanup() {
        Sequence.deleteAll(Sequence.all)
        FeatureRelationship.deleteAll(FeatureRelationship.all)
        FeatureLocation.deleteAll(FeatureLocation.all)
        Feature.deleteAll(Feature.all)
    }
 

    void "test new date format"(){
        given: "a date and expected result"
        String expectedOutput1 = "2001-02-03"

        when: "we format the string"
        Calendar calendar = new GregorianCalendar()
        calendar.set(Calendar.YEAR,2001)
        calendar.set(Calendar.MONTH,1) // for February
        calendar.set(Calendar.DAY_OF_MONTH,3)

        String outputDateString = service.formatDate(calendar.time)

        then: "we should be able to"
        assert expectedOutput1 == outputDateString
    }
}
