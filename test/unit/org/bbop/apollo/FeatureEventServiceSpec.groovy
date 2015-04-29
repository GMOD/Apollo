package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.bbop.apollo.history.FeatureOperation
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(FeatureEventService)
@Mock([FeatureEvent])
class FeatureEventServiceSpec extends Specification {

    Date today = new Date()
    String uniqueName = "uniqueName"

    // create 5 FeatureEvents
    def setup() {


        new FeatureEvent ( operation: FeatureOperation.ADD_FEATURE ,uniqueName: uniqueName ,dateCreated: today-7 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SPLIT_TRANSCRIPT,uniqueName: uniqueName  ,dateCreated: today-6 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_TRANSLATION_END,uniqueName: uniqueName  ,dateCreated: today-5 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_READTHROUGH_STOP_CODON,uniqueName: uniqueName  ,dateCreated: today-4 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_BOUNDARIES,uniqueName: uniqueName ,dateCreated: today-3 ,current: true).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.ADD_EXON,uniqueName: uniqueName  ,dateCreated: today-2 ,current: false).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.MERGE_TRANSCRIPTS,uniqueName: uniqueName  ,dateCreated: today-1 ,current: false).save(failOnError:true)
    }

    def cleanup() {
    }

    void "make sure we sort okay for previous events from most current"() {
        when: "we query the past events"
        FeatureEvent featureEvent = FeatureEvent.findByUniqueName(uniqueName,[sort:"dateCreated",order:"desc",max:1,offset:1])
        then:"we should see an add_exon event"
        assert FeatureEvent.count==7
        assert featureEvent.operation==FeatureOperation.ADD_EXON
        when: "we query the last event"
        featureEvent = FeatureEvent.findByUniqueName(uniqueName,[sort:"dateCreated",order:"desc",max:1,offset:0])
        then:"we should see merge transct"
        assert featureEvent.operation==FeatureOperation.MERGE_TRANSCRIPTS
    }

    void "make sure we sort okay for future events from the last "() {
        when: "we query the past events"
        FeatureEvent featureEvent = FeatureEvent.findByUniqueName(uniqueName,[sort:"dateCreated",order:"asc",max:1,offset:1])
        then:"we should see split transcript event"
        assert FeatureEvent.count==7
        assert featureEvent.operation==FeatureOperation.SPLIT_TRANSCRIPT
        when: "we query the first event"
        featureEvent = FeatureEvent.findByUniqueName(uniqueName,[sort:"dateCreated",order:"asc",max:1,offset:0])
        then:"we should see add feature"
        assert featureEvent.operation==FeatureOperation.ADD_FEATURE
    }
}
