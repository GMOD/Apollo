package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.bbop.apollo.history.FeatureOperation
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(FeatureEventService)
@Mock([FeatureEvent])
class FeatureEventServiceSpec extends Specification {

    Date today = new Date()
    String classUniqueName = "uniqueName"

    // create 5 FeatureEvents
    def setup() {
        new FeatureEvent ( operation: FeatureOperation.ADD_FEATURE ,childUniqueName: classUniqueName, name:"Gene123",uniqueName: classUniqueName ,dateCreated: today-7 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SPLIT_TRANSCRIPT,childUniqueName: classUniqueName,parentUniqueName: classUniqueName, name:"Gene123",uniqueName: classUniqueName  ,dateCreated: today-6 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_TRANSLATION_END,childUniqueName: classUniqueName,parentUniqueName: classUniqueName,name:"Gene123",uniqueName: classUniqueName  ,dateCreated: today-5 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_READTHROUGH_STOP_CODON,childUniqueName: classUniqueName,parentUniqueName: classUniqueName,name:"Gene123",uniqueName: classUniqueName  ,dateCreated: today-4 ,current: false ).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.SET_BOUNDARIES,childUniqueName: classUniqueName,parentUniqueName: classUniqueName,name:"Gene123",uniqueName: classUniqueName ,dateCreated: today-3 ,current: true).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.ADD_EXON,childUniqueName: classUniqueName,parentUniqueName: classUniqueName,name:"Gene123",uniqueName: classUniqueName  ,dateCreated: today-2 ,current: false).save(failOnError:true)
        new FeatureEvent ( operation: FeatureOperation.MERGE_TRANSCRIPTS,parentUniqueName: classUniqueName,name:"Gene123",uniqueName: classUniqueName  ,dateCreated: today-1 ,current: false).save(failOnError:true)
    }

    def cleanup() {
        FeatureEvent.deleteAll(FeatureEvent.all)
    }

    void "make sure we sort okay for previous events from most current"() {
        when: "we query the past events"
        FeatureEvent featureEvent = FeatureEvent.findByUniqueName(classUniqueName,[sort:"dateCreated",order:"desc",max:1,offset:1])
        then:"we should see an add_exon event"
        assert FeatureEvent.count==7
        assert featureEvent.operation==FeatureOperation.ADD_EXON
        when: "we query the last event"
        featureEvent = FeatureEvent.findByUniqueName(classUniqueName,[sort:"dateCreated",order:"desc",max:1,offset:0])
        then:"we should see merge transct"
        assert featureEvent.operation==FeatureOperation.MERGE_TRANSCRIPTS
    }

    void "make sure we sort okay for future events from the last "() {
        when: "we query the past events"
        FeatureEvent featureEvent = FeatureEvent.findByUniqueName(classUniqueName,[sort:"dateCreated",order:"asc",max:1,offset:1])
        then:"we should see split transcript event"
        assert FeatureEvent.count==7
        assert featureEvent.operation==FeatureOperation.SPLIT_TRANSCRIPT
        when: "we query the first event"
        featureEvent = FeatureEvent.findByUniqueName(classUniqueName,[sort:"dateCreated",order:"asc",max:1,offset:0])
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

    void "adding feature events using tree-style feature-events"(){

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

    void "feature events with splits can be undone"(){

        given:"add 2 transcripts"
        String name1 = "sox9a-0001"
        String name2 = "sox9b-0001"
        String uniqueName1 = "aaaaaa"
        String uniqueName2 = "bbbbbb"

        when: "we add 2 a feature events"
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT,name1,uniqueName1,new JSONObject(),new JSONObject(),new JSONObject(),null)
        List<FeatureEvent> featureEventList1 = service.getHistory(uniqueName1)

        then: "we should see a feature event"
        assert 1==FeatureEvent.countByUniqueName(uniqueName1)
        assert featureEventList1.size()==1

        when: "we do an operation"
        service.addNewFeatureEvent(FeatureOperation.SET_TRANSLATION_ENDS,name1,uniqueName1,new JSONObject(),new JSONObject(),new JSONObject(),null)
        featureEventList1 = service.getHistory(uniqueName1)

        then: "we should see an extra operation"
        assert 2==FeatureEvent.countByUniqueName(uniqueName1)
        assert featureEventList1.size()==2
        assert !featureEventList1.get(0).current
        assert featureEventList1.get(1).current

        when: "let's split this feature event!"
        service.addSplitFeatureEvent(name1,uniqueName1,name2,uniqueName2,new JSONObject(),new JSONObject(),new JSONArray(),null)
        featureEventList1 = service.getHistory(uniqueName1)
        List<FeatureEvent> featureEventList2 = service.getHistory(uniqueName2)
        FeatureEvent currentFeature = service.findCurrentFeatureEvent(uniqueName2)

        then: "we should see two feature events, with the second one current and the prior one before"
        assert 3==FeatureEvent.countByUniqueName(uniqueName1)
        assert 1==FeatureEvent.countByUniqueName(uniqueName2)
        assert featureEventList1.size()==3

        assert 0==service.findAllFutureFeatureEvents(currentFeature).size()
        assert 2==service.findAllPreviousFeatureEvents(currentFeature).size()

        assert featureEventList2.size()==3
        assert 3==service.getHistory(uniqueName1).size()
        assert 3==service.getHistory(uniqueName2).size()
        assert featureEventList1.get(2).current
        assert featureEventList1.get(2).operation==FeatureOperation.SPLIT_TRANSCRIPT
        assert !featureEventList1.get(1).current
        assert featureEventList1.get(1).operation==FeatureOperation.SET_TRANSLATION_ENDS
        assert !featureEventList1.get(0).current
        assert featureEventList1.get(0).operation==FeatureOperation.ADD_TRANSCRIPT


        assert featureEventList2.get(2).current
        assert featureEventList2.get(2).operation==FeatureOperation.SPLIT_TRANSCRIPT
        assert !featureEventList2.get(1).current
        assert featureEventList2.get(1).operation==FeatureOperation.SET_TRANSLATION_ENDS
        assert !featureEventList2.get(0).current
        assert featureEventList2.get(0).operation==FeatureOperation.ADD_TRANSCRIPT


//        when: "we add another event to 1"
//        service.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES,name1,uniqueName1,new JSONObject(),new JSONObject(),new JSONObject(),null)
//
//
//        then: "we have 3 on 1 and 1 on 2, but 2 will have show the common add_transcript hsitory"
//        assert 4==service.getHistory(uniqueName1).size()
//        assert 3==service.getHistory(uniqueName2).size()


        when: "we add another event to 2"
        service.addNewFeatureEvent(FeatureOperation.FLIP_STRAND,name2,uniqueName2,new JSONObject(),new JSONObject(),new JSONObject(),null)
        currentFeature = service.findCurrentFeatureEvent(uniqueName2)
        featureEventList2 = service.getHistory(uniqueName2)


        then: "we have 3 on 1 and 2 on 2"
        assert 0==service.findAllFutureFeatureEvents(service.findCurrentFeatureEvent(uniqueName1)).size()
        assert 2==service.findAllPreviousFeatureEvents(service.findCurrentFeatureEvent(uniqueName1)).size()
        assert 3==service.getHistory(uniqueName1).size()

        assert 0==service.findAllFutureFeatureEvents(service.findCurrentFeatureEvent(uniqueName2)).size()
        assert 3==service.findAllPreviousFeatureEvents(service.findCurrentFeatureEvent(uniqueName2)).size()
        assert 4==service.getHistory(uniqueName2).size()
        assert featureEventList2.get(3).current
        assert featureEventList2.get(3).operation==FeatureOperation.FLIP_STRAND
        assert !featureEventList2.get(2).current
        assert featureEventList2.get(2).operation==FeatureOperation.SPLIT_TRANSCRIPT
        assert !featureEventList2.get(1).current
        assert featureEventList2.get(1).operation==FeatureOperation.SET_TRANSLATION_ENDS
        assert !featureEventList2.get(0).current
        assert featureEventList2.get(0).operation==FeatureOperation.ADD_TRANSCRIPT


        // note: if we revert to 0 . . it disappears!
        when: "when we revert 2, it should reflect that"
        FeatureEvent newActiveFeatureEvent = service.setTransactionForFeature(uniqueName2,2)
        println "new active feature event ${newActiveFeatureEvent}"
        featureEventList2 = service.getHistory(uniqueName2)

        then: "if we go forward on 2 in another direction, it should reflect the forward removal"
        assert !featureEventList2.get(3).current
        assert featureEventList2.get(3).operation==FeatureOperation.FLIP_STRAND
        assert featureEventList2.get(2).current
        assert featureEventList2.get(2).operation==FeatureOperation.SPLIT_TRANSCRIPT
        assert !featureEventList2.get(1).current
        assert featureEventList2.get(1).operation==FeatureOperation.SET_TRANSLATION_ENDS
        assert !featureEventList2.get(0).current
        assert featureEventList2.get(0).operation==FeatureOperation.ADD_TRANSCRIPT


        when: "we revert 2 back to setting exon boundaries"

        then: "it should reflect that, deleting 2 from history"


        when: "we go forward again, all the way (0) on 2, 1 does not go as far, so we just go as far as we can on that one"

        then: "they both go back to thei most recent state?"


        when: "we revert 1 back to setting exon boundaries"

        then: "it should reflect that, deleting 1 from history"


        when: "we go forward in another direction (set translation end)"

        then: "2 goes away, 1 reflects the new history"

    }

    void "feature events with merges can be undone"(){

        when: "we add two transcripts"

        then: "and we merge them"
    }

}
