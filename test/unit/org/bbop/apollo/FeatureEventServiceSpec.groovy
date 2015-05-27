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
        new FeatureEvent ( operation: FeatureOperation.ADD_FEATURE ,name:"Gene123",uniqueName: uniqueName ,dateCreated: today-7 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SPLIT_TRANSCRIPT,name:"Gene123",uniqueName: uniqueName  ,dateCreated: today-6 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_TRANSLATION_END,name:"Gene123",uniqueName: uniqueName  ,dateCreated: today-5 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_READTHROUGH_STOP_CODON,name:"Gene123",uniqueName: uniqueName  ,dateCreated: today-4 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_BOUNDARIES,name:"Gene123",uniqueName: uniqueName ,dateCreated: today-3 ,current: true).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.ADD_EXON,name:"Gene123",uniqueName: uniqueName  ,dateCreated: today-2 ,current: false).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.MERGE_TRANSCRIPTS,name:"Gene123",uniqueName: uniqueName  ,dateCreated: today-1 ,current: false).save(failOnError:true)
    }

    def cleanup() {
        FeatureEvent.deleteAll(FeatureEvent.all)
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

    void "lets get the current index"(){
        when: "we have multiple feature events"
        new FeatureEvent(
                operation: FeatureOperation.ADD_FEATURE
                ,name: "Gene123"
                ,uniqueName: "AAAA"
                ,current: false
                ,dateCreated: new Date()-1
        ).save()
        new FeatureEvent(
                operation: FeatureOperation.ADD_TRANSCRIPT
                ,name: "Gene123"
                ,uniqueName: "AAAA"
                ,current: false
                ,dateCreated: new Date()-2
        ).save()
        new FeatureEvent(
                operation: FeatureOperation.SPLIT_TRANSCRIPT
                ,name: "Gene123"
                ,uniqueName: "AAAA"
                ,current: true
                ,dateCreated: new Date()-3
        ).save()
        new FeatureEvent(
                operation: FeatureOperation.MERGE_TRANSCRIPTS
                ,name: "Gene123"
                ,uniqueName: "AAAA"
                ,current: false
                ,dateCreated: new Date()-4
        ).save()
        List<FeatureEvent> mostRecentFeatureEventList = FeatureEvent.findAllByUniqueName("AAAA",[sort:"dateCreated",order:"asc"])
        List<FeatureEvent> currentFeatureEventList = FeatureEvent.findAllByUniqueNameAndCurrent("AAAA",true,[sort:"dateCreated",order:"asc"])


        then: "we should have 4 valid events"
        assert FeatureEvent.countByUniqueName("AAAA")==4
        assert mostRecentFeatureEventList.size()==4
        assert mostRecentFeatureEventList.get(0).operation==FeatureOperation.ADD_FEATURE
        assert currentFeatureEventList.size()==1
        assert currentFeatureEventList.get(0).operation==FeatureOperation.SPLIT_TRANSCRIPT

        when: "we find the current index"
        int currentIndex = service.getCurrentFeatureEventIndex("AAAA")

        then: "it should match the current index"
        assert currentIndex==2

    }

}
