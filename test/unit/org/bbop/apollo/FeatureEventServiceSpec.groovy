package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.bbop.apollo.history.FeatureOperation
import org.codehaus.groovy.grails.web.json.JSONObject
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
        new FeatureEvent ( operation: FeatureOperation.ADD_FEATURE ,childUniqueName: uniqueName, name:"Gene123",uniqueName: uniqueName ,dateCreated: today-7 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SPLIT_TRANSCRIPT,childUniqueName: uniqueName,parentUniqueName: uniqueName, name:"Gene123",uniqueName: uniqueName  ,dateCreated: today-6 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_TRANSLATION_END,childUniqueName: uniqueName,parentUniqueName: uniqueName,name:"Gene123",uniqueName: uniqueName  ,dateCreated: today-5 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_READTHROUGH_STOP_CODON,childUniqueName: uniqueName,parentUniqueName: uniqueName,name:"Gene123",uniqueName: uniqueName  ,dateCreated: today-4 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_BOUNDARIES,childUniqueName: uniqueName,parentUniqueName: uniqueName,name:"Gene123",uniqueName: uniqueName ,dateCreated: today-3 ,current: true).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.ADD_EXON,childUniqueName: uniqueName,parentUniqueName: uniqueName,name:"Gene123",uniqueName: uniqueName  ,dateCreated: today-2 ,current: false).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.MERGE_TRANSCRIPTS,parentUniqueName: uniqueName,name:"Gene123",uniqueName: uniqueName  ,dateCreated: today-1 ,current: false).save(failOnError:true)
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
                ,parentUniqueName: "AAAA"
        ).save()
        new FeatureEvent(
                operation: FeatureOperation.ADD_TRANSCRIPT
                ,name: "Gene123"
                ,uniqueName: "AAAA"
                ,childUniqueName: "AAAA"
                ,parentUniqueName: "AAAA"
                ,current: false
                ,dateCreated: new Date()-2
        ).save()
        new FeatureEvent(
                operation: FeatureOperation.SPLIT_TRANSCRIPT
                ,name: "Gene123"
                ,uniqueName: "AAAA"
                ,childUniqueName: "AAAA"
                ,parentUniqueName: "AAAA"
                ,current: true
                ,dateCreated: new Date()-3
        ).save()
        new FeatureEvent(
                operation: FeatureOperation.MERGE_TRANSCRIPTS
                ,name: "Gene123"
                ,childUniqueName: "AAAA"
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

    void "if we use the service to do insertions"(){

        given:"a transcript with a unique name"
        String name = "sox9a-0001"
        String uniqueName = "abc123"

        when: "we add a feature event"
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT,name,uniqueName,new JSONObject(),new JSONObject(),new JSONObject(),null)
        List<FeatureEvent> featureEventList = service.getHistory(uniqueName)

        then: "we should see a feature event"
        assert 1==FeatureEvent.countByUniqueName(uniqueName)
        assert featureEventList.size()==1


        when: "we add another feature event"
        service.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES,name,uniqueName,new JSONObject(),new JSONObject(),new JSONObject(),null)
        featureEventList = service.getHistory(uniqueName)

        then: "we should see two feature events, with the second one current and the prior one before"
        assert featureEventList.size()==2
        assert 2==FeatureEvent.countByUniqueName(uniqueName)
        assert featureEventList.get(1).current
        assert featureEventList.get(1).operation==FeatureOperation.SET_EXON_BOUNDARIES
        assert !featureEventList.get(0).current
        assert featureEventList.get(0).operation==FeatureOperation.ADD_TRANSCRIPT


        when: "we add a third feature event"
        service.addNewFeatureEvent(FeatureOperation.SET_TRANSLATION_START,name,uniqueName,new JSONObject(),new JSONObject(),new JSONObject(),null)
        featureEventList = service.getHistory(uniqueName)

        then: "we should see three feature events, with the third one current and the prior two before"
        assert featureEventList.size()==3
        assert 3==FeatureEvent.countByUniqueName(uniqueName)

        assert featureEventList.get(2).operation==FeatureOperation.SET_TRANSLATION_START
        assert featureEventList.get(2).current
        assert featureEventList.get(1).operation==FeatureOperation.SET_EXON_BOUNDARIES
        assert !featureEventList.get(1).current
        assert featureEventList.get(0).operation==FeatureOperation.ADD_TRANSCRIPT
        assert !featureEventList.get(0).current

        when: "if we make the second one current"
        service.setTransactionForFeature(uniqueName,1)
        featureEventList = service.getHistory(uniqueName)

        then: "we should see one in front and one behind"
        assert featureEventList.size()==3
        assert 3==FeatureEvent.countByUniqueName(uniqueName)
        assert featureEventList.get(2).operation==FeatureOperation.SET_TRANSLATION_START
        assert !featureEventList.get(2).current
        assert featureEventList.get(1).operation==FeatureOperation.SET_EXON_BOUNDARIES
        assert featureEventList.get(1).current
        assert featureEventList.get(0).operation==FeatureOperation.ADD_TRANSCRIPT
        assert !featureEventList.get(0).current

        when: "we add another feature event"
        service.addNewFeatureEvent(FeatureOperation.SPLIT_EXON,name,uniqueName,new JSONObject(),new JSONObject(),new JSONObject(),null)
        featureEventList = service.getHistory(uniqueName)


        then: "the last one disappears"
        assert featureEventList.size()==3
        assert 3==FeatureEvent.countByUniqueName(uniqueName)
        assert featureEventList.get(2).operation==FeatureOperation.SPLIT_EXON
        assert featureEventList.get(2).current
        assert featureEventList.get(1).operation==FeatureOperation.SET_EXON_BOUNDARIES
        assert !featureEventList.get(1).current
        assert featureEventList.get(0).operation==FeatureOperation.ADD_TRANSCRIPT
        assert !featureEventList.get(0).current

        when: "we set the first one current"
        service.setTransactionForFeature(uniqueName,0)
        assert 1==FeatureEvent.countByUniqueNameAndCurrent(uniqueName,true)

        featureEventList = service.getHistory(uniqueName)

        then: "the first one will be current"
        assert featureEventList.size()==3
        assert 3==FeatureEvent.countByUniqueName(uniqueName)
        assert featureEventList.get(2).operation==FeatureOperation.SPLIT_EXON
        assert !featureEventList.get(2).current
        assert featureEventList.get(1).operation==FeatureOperation.SET_EXON_BOUNDARIES
        assert !featureEventList.get(1).current
        assert featureEventList.get(0).operation==FeatureOperation.ADD_TRANSCRIPT
        assert featureEventList.get(0).current

    }



}
