package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.bbop.apollo.history.FeatureOperation
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Ignore
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
        FeatureEvent f1 = new FeatureEvent(operation: FeatureOperation.ADD_FEATURE, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 7, current: false).save(failOnError: true)
        FeatureEvent f2 = new FeatureEvent(operation: FeatureOperation.SPLIT_TRANSCRIPT, parentId: f1.id, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 6, current: false).save(failOnError: true)
        f1.childId = f2.id
        FeatureEvent f3 = new FeatureEvent(operation: FeatureOperation.SET_TRANSLATION_END, parentId: f2.id, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 5, current: false).save(failOnError: true)
        f2.childId = f3.id
        FeatureEvent f4 = new FeatureEvent(operation: FeatureOperation.SET_READTHROUGH_STOP_CODON, parentId: f3.id, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 4, current: false).save(failOnError: true)
        f3.childId = f4.id
        FeatureEvent f5 = new FeatureEvent(operation: FeatureOperation.SET_BOUNDARIES, parentId: f4.id, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 3, current: true).save(failOnError: true)
        f4.childId = f5.id
        FeatureEvent f6 = new FeatureEvent(operation: FeatureOperation.ADD_EXON, parentId: f5.id, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 2, current: false).save(failOnError: true)
        f5.childId = f6.id
        FeatureEvent f7 = new FeatureEvent(operation: FeatureOperation.MERGE_TRANSCRIPTS, parentId: f6.id, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 1, current: false).save(failOnError: true)
        f1.save()
        f2.save()
        f3.save()
        f4.save()
        f5.save()
        f6.save()
        f7.save()
    }

    def cleanup() {
        FeatureEvent.deleteAll(FeatureEvent.all)
    }

    @Ignore
    void "make sure we sort okay for previous events from most current"() {
        when: "we query the past events"
        FeatureEvent featureEvent = FeatureEvent.findByUniqueName(classUniqueName, [sort: "dateCreated", order: "desc", max: 1, offset: 1])
        then: "we should see an add_exon event"
        assert FeatureEvent.count == 7
        assert featureEvent.operation == FeatureOperation.ADD_EXON
        when: "we query the last event"
        featureEvent = FeatureEvent.findByUniqueName(classUniqueName, [sort: "dateCreated", order: "desc", max: 1, offset: 0])
        then: "we should see merge transct"
        assert featureEvent.operation == FeatureOperation.MERGE_TRANSCRIPTS
    }

    void "make sure we sort okay for future events from the last "() {
        when: "we query the past events"
        FeatureEvent featureEvent = FeatureEvent.findByUniqueName(classUniqueName, [sort: "dateCreated", order: "asc", max: 1, offset: 1])
        then: "we should see split transcript event"
        assert FeatureEvent.count == 7
        assert featureEvent.operation == FeatureOperation.SPLIT_TRANSCRIPT
        when: "we query the first event"
        featureEvent = FeatureEvent.findByUniqueName(classUniqueName, [sort: "dateCreated", order: "asc", max: 1, offset: 0])
        then: "we should see add feature"
        assert featureEvent.operation == FeatureOperation.ADD_FEATURE
    }

    void "lets get the current index"() {
        when: "we have multiple feature events"
        FeatureEvent f4 = new FeatureEvent(
                operation: FeatureOperation.ADD_FEATURE
                , name: "Gene123"
                , uniqueName: "AAAA"
                , current: false
                , dateCreated: new Date() - 1
        ).save()
        FeatureEvent f3 = new FeatureEvent(
                operation: FeatureOperation.ADD_TRANSCRIPT
                , name: "Gene123"
                , uniqueName: "AAAA"
                , childId: f4.id
                , current: false
                , dateCreated: new Date() - 2
        ).save()
        FeatureEvent f2 = new FeatureEvent(
                operation: FeatureOperation.SPLIT_TRANSCRIPT
                , name: "Gene123"
                , uniqueName: "AAAA"
                , childId: f3.id
                , current: true
                , dateCreated: new Date() - 3
        ).save()
        // this is the first one!
        FeatureEvent f1 = new FeatureEvent(
                operation: FeatureOperation.MERGE_TRANSCRIPTS
                , name: "Gene123"
                , childId: f2.id
                , uniqueName: "AAAA"
                , current: false
                , dateCreated: new Date() - 4
        ).save()
        f4.parentId = f3.id
        f4.save()
        f3.parentId = f2.id
        f3.save()
        f2.parentId = f1.id
        f2.save()

        List<FeatureEvent> mostRecentFeatureEventList = FeatureEvent.findAllByUniqueName("AAAA", [sort: "dateCreated", order: "asc"])
        List<FeatureEvent> currentFeatureEventList = FeatureEvent.findAllByUniqueNameAndCurrent("AAAA", true, [sort: "dateCreated", order: "asc"])


        then: "we should have 4 valid events"
        assert FeatureEvent.countByUniqueName("AAAA") == 4
        assert mostRecentFeatureEventList.size() == 4
        assert mostRecentFeatureEventList.get(0).operation == FeatureOperation.ADD_FEATURE
        assert currentFeatureEventList.size() == 1
        assert currentFeatureEventList.get(0).operation == FeatureOperation.SPLIT_TRANSCRIPT

        when: "we find the current index"
        int currentIndex = service.getCurrentFeatureEventIndex("AAAA")

        then: "it should match the current index"
        assert currentIndex == 1

    }

    void "adding feature events using tree-style feature-events"() {

        given: "a transcript with a unique name"
        String name = "sox9a-0001"
        String uniqueName = "abc123"

        when: "we add a feature event"
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT, name, uniqueName, new JSONObject(), new JSONObject(), new JSONObject(), null)
        List<List<FeatureEvent>> featureEventList = service.getHistory(uniqueName)

        then: "we should see a feature event"
        assert 1 == FeatureEvent.countByUniqueName(uniqueName)
        assert featureEventList.size() == 1


        when: "we add another feature event"
        service.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, name, uniqueName, new JSONObject(), new JSONObject(), new JSONObject(), null)
        featureEventList = service.getHistory(uniqueName)

        then: "we should see two feature events, with the second one current and the prior one before"
        assert featureEventList.size() == 2
        assert 2 == FeatureEvent.countByUniqueName(uniqueName)
        assert featureEventList[1][0].current
        assert featureEventList[1][0].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert !featureEventList[0][0].current
        assert featureEventList[0][0].operation == FeatureOperation.ADD_TRANSCRIPT


        when: "we add a third feature event"
        service.addNewFeatureEvent(FeatureOperation.SET_TRANSLATION_START, name, uniqueName, new JSONObject(), new JSONObject(), new JSONObject(), null)
        featureEventList = service.getHistory(uniqueName)

        then: "we should see three feature events, with the third one current and the prior two before"
        assert featureEventList.size() == 3
        assert 3 == FeatureEvent.countByUniqueName(uniqueName)

        assert featureEventList[2][0].operation == FeatureOperation.SET_TRANSLATION_START
        assert featureEventList[2][0].current
        assert featureEventList[1][0].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert !featureEventList[1][0].current
        assert featureEventList[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert !featureEventList[0][0].current

        when: "if we make the second one current"
        service.setTransactionForFeature(uniqueName, 1)
        featureEventList = service.getHistory(uniqueName)

        then: "we should see one in front and one behind"
        assert featureEventList.size() == 3
        assert 3 == FeatureEvent.countByUniqueName(uniqueName)
        assert featureEventList[2][0].operation == FeatureOperation.SET_TRANSLATION_START
        assert !featureEventList[2][0].current
        assert featureEventList[1][0].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert featureEventList[1][0].current
        assert featureEventList[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert !featureEventList[0][0].current

        when: "we add another feature event"
        service.addNewFeatureEvent(FeatureOperation.SPLIT_EXON, name, uniqueName, new JSONObject(), new JSONObject(), new JSONObject(), null)
        featureEventList = service.getHistory(uniqueName)


        then: "the last one disappears"
        assert featureEventList.size() == 3
        assert 3 == FeatureEvent.countByUniqueName(uniqueName)
        assert featureEventList[2][0].operation == FeatureOperation.SPLIT_EXON
        assert featureEventList[2][0].current
        assert featureEventList[1][0].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert !featureEventList[1][0].current
        assert featureEventList[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert !featureEventList[0][0].current

        when: "we set the first one current"
        service.setTransactionForFeature(uniqueName, 0)
        assert 1 == FeatureEvent.countByUniqueNameAndCurrent(uniqueName, true)

        featureEventList = service.getHistory(uniqueName)

        then: "the first one will be current"
        assert featureEventList.size() == 3
        assert 3 == FeatureEvent.countByUniqueName(uniqueName)
        assert featureEventList[2][0].operation == FeatureOperation.SPLIT_EXON
        assert !featureEventList[2][0].current
        assert featureEventList[1][0].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert !featureEventList[1][0].current
        assert featureEventList[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert featureEventList[0][0].current

    }

    void "feature events with splits can be undone"() {

        given: "add 1 transcripts"
        String name1 = "sox9a-0001"
        String name2 = "sox9b-0001"
        String uniqueName1 = "aaaaaa"
        String uniqueName2 = "bbbbbb"

        when: "we add a feature event"
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT, name1, uniqueName1, new JSONObject(), new JSONObject(), new JSONObject(), null)
        List<List<FeatureEvent>> featureEventList1 = service.getHistory(uniqueName1)

        then: "we should see a feature event"
        assert 1 == FeatureEvent.countByUniqueName(uniqueName1)
        assert featureEventList1.size() == 1

        when: "we do an operation"
        service.addNewFeatureEvent(FeatureOperation.SET_TRANSLATION_ENDS, name1, uniqueName1, new JSONObject(), new JSONObject(), new JSONObject(), null)
        featureEventList1 = service.getHistory(uniqueName1)

        then: "we should see an extra operation"
        assert 2 == FeatureEvent.countByUniqueName(uniqueName1)
        assert featureEventList1.size() == 2
        assert !featureEventList1[0][0].current
        assert featureEventList1[1][0].current

        when: "let's split this feature event!"
        JSONArray newJsonArray = new JSONArray()
        newJsonArray.add(new JSONObject())
        newJsonArray.add(new JSONObject())
        service.addSplitFeatureEvent(name1, uniqueName1, name2, uniqueName2, new JSONObject(), new JSONObject(), newJsonArray, null)
        featureEventList1 = service.getHistory(uniqueName1)
        List<List<FeatureEvent>> featureEventList2 = service.getHistory(uniqueName2)
        FeatureEvent currentFeature = service.findCurrentFeatureEvent(uniqueName2)[0]
        List<List<FeatureEvent>> previousEvents = service.findPreviousFeatureEvents(currentFeature)
        List<List<FeatureEvent>> futureEvents = service.findFutureFeatureEvents(currentFeature)

        then: "we should see two feature events, with the second one current and the prior one before"
        assert 3 == FeatureEvent.countByUniqueName(uniqueName1)
        assert 1 == FeatureEvent.countByUniqueName(uniqueName2)
        assert featureEventList1.size() == 3
        assert featureEventList2.size() == 3

        assert 0 == futureEvents.size()
        assert 2 == previousEvents.size()
        assert previousEvents.get(0).first().operation == FeatureOperation.ADD_TRANSCRIPT
        assert previousEvents.get(1).first().operation == FeatureOperation.SET_TRANSLATION_ENDS

        assert featureEventList2.size() == 3
        assert 3 == service.getHistory(uniqueName1).size()
        assert 3 == service.getHistory(uniqueName2).size()
        assert featureEventList1[2][0].current
        assert featureEventList1[2][0].operation == FeatureOperation.SPLIT_TRANSCRIPT
        assert !featureEventList1[1][0].current
        assert featureEventList1[1][0].operation == FeatureOperation.SET_TRANSLATION_ENDS
        assert !featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT


        assert featureEventList2[2][0].current
        assert featureEventList2[2][0].operation == FeatureOperation.SPLIT_TRANSCRIPT
        assert !featureEventList2[1][0].current
        assert featureEventList2[1][0].operation == FeatureOperation.SET_TRANSLATION_ENDS
        assert !featureEventList2[0][0].current
        assert featureEventList2[0][0].operation == FeatureOperation.ADD_TRANSCRIPT


        when: "we add another event to 2"
        service.addNewFeatureEvent(FeatureOperation.FLIP_STRAND, name2, uniqueName2, new JSONObject(), new JSONObject(), new JSONObject(), null)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)
        futureEvents = service.findFutureFeatureEvents(service.findCurrentFeatureEvent(uniqueName1)[0])
        previousEvents = service.findPreviousFeatureEvents(service.findCurrentFeatureEvent(uniqueName1)[0])

        then: "we have 2 on 2"
        assert 0 == futureEvents.size()
        assert 2 == previousEvents.size()

        when: "we test the other sizde"
        def currentFeatureEvent2 = service.findCurrentFeatureEvent(uniqueName2)
        def currentFeatureEvent1 = service.findCurrentFeatureEvent(uniqueName1)
        futureEvents = service.findFutureFeatureEvents(currentFeatureEvent2[0])
        previousEvents = service.findPreviousFeatureEvents(currentFeatureEvent2[0])

        then: "we have 3 on 1"
        assert currentFeatureEvent1 == currentFeatureEvent2
        assert currentFeatureEvent2[0].uniqueName == uniqueName1
        assert currentFeatureEvent2[1].uniqueName == uniqueName2
        assert 0 == futureEvents.size()
        assert 2 == previousEvents.size()
        assert 4 == featureEventList1.size()
        assert 4 == featureEventList2.size()

        when: "we test the second index"
        futureEvents = service.findFutureFeatureEvents(currentFeatureEvent2[1])
        previousEvents = service.findPreviousFeatureEvents(currentFeatureEvent2[1])

        then: "we have 3 on 1"
        assert currentFeatureEvent1 == currentFeatureEvent2
        assert 0 == futureEvents.size()
        assert 3 == previousEvents.size()

        assert featureEventList1 == featureEventList2

        assert featureEventList2[0].size() == 1
        assert !featureEventList2[0][0].current
        assert featureEventList2[0][0].operation == FeatureOperation.ADD_TRANSCRIPT

        assert featureEventList2[1].size() == 1
        assert !featureEventList2[1][0].current
        assert featureEventList2[1][0].operation == FeatureOperation.SET_TRANSLATION_ENDS

        assert featureEventList2[2].size() == 2
        assert featureEventList2[2][0].operation == FeatureOperation.SPLIT_TRANSCRIPT
        assert featureEventList2[2][1].operation == FeatureOperation.SPLIT_TRANSCRIPT
        assert ((featureEventList2[2][0].current && !featureEventList2[2][1].current) || (!featureEventList2[2][0].current && featureEventList2[2][1].current))

        assert featureEventList2[3].size() == 1
        assert featureEventList2[3][0].current
        assert featureEventList2[3][0].operation == FeatureOperation.FLIP_STRAND

        // note: if we revert to 0 . . it disappears!
        when: "when we revert 2 back on transcript 2 to setting exon boundaries"
        FeatureEvent newActiveFeatureEvent = service.setTransactionForFeature(uniqueName2, 1)[0]
        featureEventList2 = service.getHistory(uniqueName2)
        featureEventList1 = service.getHistory(uniqueName1)
        List<FeatureEvent> thisCurrentFeatureEvents = service.findCurrentFeatureEvent(uniqueName1)
        previousEvents = service.findPreviousFeatureEvents(thisCurrentFeatureEvents)
        futureEvents = service.findFutureFeatureEvents(thisCurrentFeatureEvents)

        then: "we have 3 on 1"
        assert thisCurrentFeatureEvents.size() == 1
        assert thisCurrentFeatureEvents[0].operation == FeatureOperation.SET_TRANSLATION_ENDS
        assert service.findCurrentFeatureEvent(uniqueName2)[0].operation == FeatureOperation.SET_TRANSLATION_ENDS
        assert 2 == futureEvents.size()
        assert 1 == previousEvents.size()

        assert previousEvents[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert futureEvents[0][0].operation == FeatureOperation.SPLIT_TRANSCRIPT
        assert futureEvents[0][1].operation == FeatureOperation.SPLIT_TRANSCRIPT
        assert futureEvents[1][0].operation == FeatureOperation.FLIP_STRAND

        assert 4 == featureEventList1.size()  // we can fast-forward all the way up through 2 and the split
        assert 4 == featureEventList2.size()

        assert !featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert featureEventList1[0][0].uniqueName == uniqueName1

        assert featureEventList1[1][0].current
        assert featureEventList1[1][0].operation == FeatureOperation.SET_TRANSLATION_ENDS
        assert featureEventList1[1][0].uniqueName == uniqueName1

        assert !featureEventList1[2][0].current
        assert !featureEventList1[2][1].current
        assert featureEventList1[2][0].operation == FeatureOperation.SPLIT_TRANSCRIPT
        assert featureEventList1[2][1].operation == FeatureOperation.SPLIT_TRANSCRIPT

        assert !featureEventList1[3][0].current
        assert featureEventList1[3][0].operation == FeatureOperation.FLIP_STRAND


        when: "we go forward on 1 (2 does not exist anymore unless we go forward)"
        List<FeatureEvent> currentFeatureEvents = service.setTransactionForFeature(uniqueName1, 2)
        assert currentFeatureEvents.size() == 2
        assert currentFeatureEvents[0].current
        assert currentFeatureEvents[1].current
        assert currentFeatureEvents[0].operation == FeatureOperation.SPLIT_TRANSCRIPT
        assert currentFeatureEvents[1].operation == FeatureOperation.SPLIT_TRANSCRIPT
        featureEventList2 = service.getHistory(uniqueName2)
        featureEventList1 = service.getHistory(uniqueName1)
        def currentFeatureEvents1 = service.findCurrentFeatureEvent(uniqueName1)
        def currentFeatureEvents2 = service.findCurrentFeatureEvent(uniqueName2)
        def currentFeatureEventUniqueName1 = currentFeatureEvents1.find(){ it.uniqueName == uniqueName1}
        def currentFeatureEventUniqueName2 = currentFeatureEvents2.find(){ it.uniqueName == uniqueName2}
//        currentFeatureEvents = service.findCurrentFeatureEvent(uniqueName2)
//        int uniqueName1Index = currentFeatureEvents[0].uniqueName == uniqueName1 ? 0 : 1


        then: "since 2 is further then 1, it should stop on the most recent for both"
        assert 2 == currentFeatureEvents1.size()
        assert 2 == currentFeatureEvents2.size()
        assert currentFeatureEvents1==currentFeatureEvents2
        assert uniqueName1 == currentFeatureEventUniqueName1.uniqueName
        assert uniqueName2 == currentFeatureEventUniqueName2.uniqueName

        assert !featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert featureEventList1[0][0].uniqueName == uniqueName1

        assert !featureEventList1[1][0].current
        assert featureEventList1[1][0].operation == FeatureOperation.SET_TRANSLATION_ENDS
        assert featureEventList1[1][0].uniqueName == uniqueName1

        assert featureEventList1[2][0].current
        assert featureEventList1[2][1].current
        assert featureEventList1[2][0].operation == FeatureOperation.SPLIT_TRANSCRIPT
        assert featureEventList1[2][1].operation == FeatureOperation.SPLIT_TRANSCRIPT

        assert !featureEventList1[3][0].current
        assert featureEventList1[3][0].operation == FeatureOperation.FLIP_STRAND

        assert 0 == service.findFutureFeatureEvents(currentFeatureEventUniqueName1).size()
        assert 2 == service.findPreviousFeatureEvents(currentFeatureEventUniqueName1).size()
        assert 1 == service.findFutureFeatureEvents(currentFeatureEventUniqueName2).size()
        assert 2 == service.findPreviousFeatureEvents(currentFeatureEventUniqueName2).size()

        assert 4 == featureEventList1.size()
        assert 4 == featureEventList2.size()
        // note: I am uniqueName2 split explicitly (by default everywhere else I'm grabbing uniqueName1)
        assert featureEventList1 == featureEventList2

        when: "we go all the way forward on 2"
        def current = service.setTransactionForFeature(uniqueName2, 3)
        featureEventList2 = service.getHistory(uniqueName2)
        featureEventList1 = service.getHistory(uniqueName1)
        currentFeatureEvents = service.findCurrentFeatureEvent(uniqueName2)
        currentFeatureEvent1 = null
        currentFeatureEvent2 = null
        currentFeatureEvent1 = currentFeatureEvents.find(){ it.uniqueName==uniqueName1}
        currentFeatureEvent2 = currentFeatureEvents.find(){ it.uniqueName==uniqueName2}


        then: "no change on 1, 2 goes to flip strand"
        assert currentFeatureEvents.size()==2
        assert currentFeatureEvent1.uniqueName == uniqueName1
        assert currentFeatureEvent2.uniqueName == uniqueName2
        assert 0 == service.findFutureFeatureEvents(currentFeatureEvent1).size()
        assert 2 == service.findPreviousFeatureEvents(currentFeatureEvent1).size()
        assert 0 == service.findFutureFeatureEvents(currentFeatureEvent2).size()
        assert 3 == service.findPreviousFeatureEvents(currentFeatureEvent2).size()

        assert 4 == featureEventList1.size()
        assert 4 == featureEventList2.size()

        assert featureEventList1 == featureEventList2
        assert !featureEventList2[0][0].current
        assert featureEventList2[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert !featureEventList2[1][0].current
        assert featureEventList2[1][0].operation == FeatureOperation.SET_TRANSLATION_ENDS

        assert featureEventList2[2][0].operation == FeatureOperation.SPLIT_TRANSCRIPT
        assert featureEventList2[2][1].operation == FeatureOperation.SPLIT_TRANSCRIPT
        assert ((featureEventList2[2][0].current && !featureEventList2[2][1].current) || (!featureEventList2[2][0].current && featureEventList2[2][1].current))

        assert featureEventList2[3][0].current
        assert featureEventList2[3][0].operation == FeatureOperation.FLIP_STRAND

    }

    void "feature events with merges can be undone"() {

        given: "add 2 transcripts"
        String name1 = "sox9a-0001"
        String name2 = "sox9b-0001"
        String uniqueName1 = "aaaaaa"
        String uniqueName2 = "bbbbbb"

        when: "we add 2 feature events"
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT, name1, uniqueName1, new JSONObject(), new JSONObject(), new JSONObject(), null)
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT, name2, uniqueName2, new JSONObject(), new JSONObject(), new JSONObject(), null)
        List<List<FeatureEvent>> featureEventList1 = service.getHistory(uniqueName1)
        List<List<FeatureEvent>> featureEventList2 = service.getHistory(uniqueName2)

        then: "we should see a feature event"
        assert 1 == FeatureEvent.countByUniqueName(uniqueName1)
        assert featureEventList1.size() == 1
        assert 1 == FeatureEvent.countByUniqueName(uniqueName2)
        assert featureEventList2.size() == 1

        when: "we do an operation"
        service.addNewFeatureEvent(FeatureOperation.SET_TRANSLATION_ENDS, name1, uniqueName1, new JSONObject(), new JSONObject(), new JSONObject(), null)
        featureEventList1 = service.getHistory(uniqueName1)

        then: "we should see an extra operation"
        assert 2 == FeatureEvent.countByUniqueName(uniqueName1)
        assert featureEventList1.size() == 2
        assert !featureEventList1[0][0].current
        assert featureEventList1[1][0].current

        when: "let's merge feature events"
        JSONArray oldJsonArray = new JSONArray()
//        newJsonArray.add(new JSONObject())
        oldJsonArray.add(new JSONObject())
        service.addMergeFeatureEvent(name1, uniqueName1, name2, uniqueName2, new JSONObject(), oldJsonArray, new JSONObject(), null)
        featureEventList2 = service.getHistory(uniqueName2)
        featureEventList1 = service.getHistory(uniqueName1)
        // TODO: not sure if this is accurate, or possible
//        FeatureEvent currentFeature = service.findCurrentFeatureEvent(uniqueName2)[0]
        List<FeatureEvent> currentFeatureEvents = service.findCurrentFeatureEvent(uniqueName1)
        FeatureEvent currentFeature = currentFeatureEvents[0]

        then: "we should see one feature events, with the second one current and the prior one before"
        assert 3 == FeatureEvent.countByUniqueName(uniqueName1)
        assert 1 == FeatureEvent.countByUniqueName(uniqueName2)
        assert featureEventList1.size() == 3
        assert featureEventList1 == featureEventList2

        assert 0 == service.findFutureFeatureEvents(currentFeature).size()
        assert 2 == service.findPreviousFeatureEvents(currentFeature).size()

        assert featureEventList1==featureEventList2
        assert 3 == service.getHistory(uniqueName1).size()
        assert !featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert featureEventList1[1].size() == 2
        for (fe in featureEventList1[1]) {
            assert !fe.current
            assert fe.operation == FeatureOperation.ADD_TRANSCRIPT || fe.operation == FeatureOperation.SET_TRANSLATION_ENDS
        }
        assert featureEventList1[2][0].current
        assert featureEventList1[2][0].operation == FeatureOperation.MERGE_TRANSCRIPTS


        when: "we add another event to 1 (2 no longer is accessible)"
        service.addNewFeatureEvent(FeatureOperation.FLIP_STRAND, name1, uniqueName1, new JSONObject(), new JSONObject(), new JSONObject(), null)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)

        then: "we have 3 on 1 and 2 on 2"
        assert 0 == service.findFutureFeatureEvents(service.findCurrentFeatureEvent(uniqueName1)[0]).size()
        assert 3 == service.findPreviousFeatureEvents(service.findCurrentFeatureEvent(uniqueName1)[0]).size()
        assert 4 == featureEventList1.size()

        assert !featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert featureEventList1[1].size() == 2
        for (fe in featureEventList1[1]) {
            assert !fe.current
            assert fe.operation == FeatureOperation.ADD_TRANSCRIPT || fe.operation == FeatureOperation.SET_TRANSLATION_ENDS
        }
        assert !featureEventList1[2][0].current
        assert featureEventList1[2][0].operation == FeatureOperation.MERGE_TRANSCRIPTS
        assert featureEventList1[3][0].current
        assert featureEventList1[3][0].operation == FeatureOperation.FLIP_STRAND

        // note: if we revert to 0 . . it disappears!
        when: "when we revert 2 back on transcript 2 to setting exon boundaries"
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)

        then: "it should be active on the split transcript event for both"
        assert 4 == featureEventList1.size()  // we can fast-forward all the way up through 2 and the split
        assert featureEventList1==featureEventList2

        assert featureEventList1[3][0].current
        assert featureEventList1[3][0].operation == FeatureOperation.FLIP_STRAND
        assert !featureEventList1[2][0].current
        assert featureEventList1[2][0].operation == FeatureOperation.MERGE_TRANSCRIPTS
        assert featureEventList1[1].size() == 2
        for (fe in featureEventList1[1]) {
            assert !fe.current
            assert fe.operation == FeatureOperation.ADD_TRANSCRIPT || fe.operation == FeatureOperation.SET_TRANSLATION_ENDS
        }
        assert !featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert featureEventList1[0][0].uniqueName == uniqueName1


        when: "we go forward on 1 we should re-merge, goes beyond 2, so just stops at end"
        currentFeatureEvents = service.setTransactionForFeature(uniqueName1, 2)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)


        then: "since 2 is further then 1, it should stop on the most recent for both, but one disappears"
        assert featureEventList1.size() == 4
        assert featureEventList1==featureEventList2
        assert currentFeatureEvents.size() == 1
        assert currentFeatureEvents[0].current
        assert 1 == service.findFutureFeatureEvents(service.findCurrentFeatureEvent(uniqueName1)[0]).size()
        assert 2 == service.findPreviousFeatureEvents(service.findCurrentFeatureEvent(uniqueName1)[0]).size()
        assert 4 == featureEventList1.size()


        assert !featureEventList1[3][0].current
        assert featureEventList1[3][0].operation == FeatureOperation.FLIP_STRAND
        assert featureEventList1[2][0].current
        assert featureEventList1[2][0].operation == FeatureOperation.MERGE_TRANSCRIPTS
        assert featureEventList1[1].size() == 2
        for (fe in featureEventList1[1]) {
            assert !fe.current
            assert fe.operation == FeatureOperation.ADD_TRANSCRIPT || fe.operation == FeatureOperation.SET_TRANSLATION_ENDS
        }
        assert !featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT



        when: "we go all the way forward on 2"
        currentFeatureEvents = service.setTransactionForFeature(uniqueName1, 3)
        featureEventList1 = service.getHistory(uniqueName1)


        then: "no change on 1, 2 goes to flip strand"
        assert featureEventList1.size() == 4
        assert featureEventList1==featureEventList2
        assert currentFeatureEvents.size() == 1
        assert currentFeatureEvents[0].current
        assert 0 == service.findFutureFeatureEvents(service.findCurrentFeatureEvent(uniqueName1)[0]).size()
        assert 3 == service.findPreviousFeatureEvents(service.findCurrentFeatureEvent(uniqueName1)[0]).size()
        assert 4 == featureEventList1.size()


        assert featureEventList1[3][0].current
        assert featureEventList1[3][0].operation == FeatureOperation.FLIP_STRAND
        assert !featureEventList1[2][0].current
        assert featureEventList1[2][0].operation == FeatureOperation.MERGE_TRANSCRIPTS
        assert featureEventList1[1].size() == 2
        for (fe in featureEventList1[1]) {
            assert !fe.current
            assert fe.operation == FeatureOperation.ADD_TRANSCRIPT || fe.operation == FeatureOperation.SET_TRANSLATION_ENDS
        }
        assert !featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT


    }

    void "undo merge after an action on the other side"() {

        given: "add 2 transcripts"
        String name1 = "sox9a-0001"
        String name2 = "sox9b-0001"
        String uniqueName1 = "aaaaaa"
        String uniqueName2 = "bbbbbb"

        when: "we add 2 feature events"
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT, name1, uniqueName1, new JSONObject(), new JSONObject(), new JSONObject(), null)
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT, name2, uniqueName2, new JSONObject(), new JSONObject(), new JSONObject(), null)
        List<List<FeatureEvent>> featureEventList1 = service.getHistory(uniqueName1)
        List<List<FeatureEvent>> featureEventList2 = service.getHistory(uniqueName2)

        then: "we should see a feature event"
        assert 1 == FeatureEvent.countByUniqueName(uniqueName1)
        assert featureEventList1.size() == 1
        assert 1 == FeatureEvent.countByUniqueName(uniqueName2)
        assert featureEventList2.size() == 1

        when: "we do an operation"
        service.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, name2, uniqueName2, new JSONObject(), new JSONObject(), new JSONObject(), null)
        featureEventList2 = service.getHistory(uniqueName2)

        then: "we should see an extra operation"
        assert 2 == FeatureEvent.countByUniqueName(uniqueName2)
        assert 1 == FeatureEvent.countByUniqueName(uniqueName1)
        assert featureEventList1.size() == 1
        assert featureEventList1[0][0].current
        assert featureEventList2.size() == 2
        assert !featureEventList2[0][0].current
        assert featureEventList2[1][0].current

        when: "let's merge feature events"
        JSONArray oldJsonArray = new JSONArray()
        oldJsonArray.add(new JSONObject())
        service.addMergeFeatureEvent(name1, uniqueName1, name2, uniqueName2, new JSONObject(), oldJsonArray, new JSONObject(), null)
        Integer featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        Integer featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)

        Map<String, Map<Long, FeatureEvent>> featureEventMap = service.extractFeatureEventGroup(uniqueName1)
        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueNameAndCurrent(uniqueName1, true)
        FeatureEvent currentFeature = featureEventList.first()

        List<List<FeatureEvent>> previousFeatureEvents = service.findPreviousFeatureEvents(currentFeature)

        then: "check our data structures"
        assert featureIndex1 == 2
        assert featureIndex2 == 2
        assert featureEventList.size() == 1
        assert featureEventMap.size() == 2
        assert featureEventMap.values().first().size() == 2
        assert featureEventMap.values().last().size() == 2
        assert previousFeatureEvents.size() == 2

        when: "we get the current feature event"
        List<FeatureEvent> currentFeatureEventArray = service.findCurrentFeatureEvent(uniqueName1)

        then: "it should not be null"
        // this hsould return
        assert currentFeatureEventArray != null
        assert currentFeatureEventArray.size() == 1
        assert currentFeatureEventArray.first().operation == FeatureOperation.MERGE_TRANSCRIPTS

        when: "we get the histories"
        featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)
        List<List<FeatureEvent>> futureEvents = service.findFutureFeatureEvents(currentFeatureEventArray)
        List<List<FeatureEvent>> previousEvents = service.findPreviousFeatureEvents(currentFeatureEventArray)

        then: "we should see one feature events, with the second one current and the prior one before"
        assert 2 == featureIndex1
        assert 2 == featureIndex2
        assert featureEventList1.size() == 3
        assert featureEventList1==featureEventList2
        assert 2 == FeatureEvent.countByUniqueName(uniqueName2)
        assert 2 == FeatureEvent.countByUniqueName(uniqueName1)
        assert 0 == futureEvents.size()
        assert 2 == previousEvents.size()
        assert previousEvents[0].size() == 1
        assert previousEvents[1].size() == 2

        assert featureEventList1[2].size() == 1
        assert featureEventList1[2][0].current
        assert featureEventList1[2][0].operation == FeatureOperation.MERGE_TRANSCRIPTS


        assert featureEventList1[1].size() == 2
        for (fe in featureEventList1[1]) {
            assert !fe.current
            assert fe.operation == FeatureOperation.ADD_TRANSCRIPT || fe.operation == FeatureOperation.SET_EXON_BOUNDARIES
        }


        assert featureEventList1[0].size() == 1
        assert !featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT


        when: "when we undo one (current index is 2)"
        service.setTransactionForFeature(uniqueName1, 1)
        currentFeatureEventArray = service.findCurrentFeatureEvent(uniqueName1)

        then: "we should get both back as current, with set exon boundary"
        assert currentFeatureEventArray != null
        assert currentFeatureEventArray.size() == 2
        assert currentFeatureEventArray.last().operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert currentFeatureEventArray.first().operation == FeatureOperation.ADD_TRANSCRIPT

        when: "we get the histories"
        featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)
        FeatureEvent addTranscriptFeatureEvent = currentFeatureEventArray.first()
        FeatureEvent exonBoundariesFeatureEvent = currentFeatureEventArray.last()
        futureEvents = service.findFutureFeatureEvents(exonBoundariesFeatureEvent)
        previousEvents = service.findPreviousFeatureEvents(exonBoundariesFeatureEvent)

        then: "we verify that we are at the right place"
        assert addTranscriptFeatureEvent.operation == FeatureOperation.ADD_TRANSCRIPT
        assert addTranscriptFeatureEvent.uniqueName == uniqueName1
        assert exonBoundariesFeatureEvent.operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert exonBoundariesFeatureEvent.uniqueName == uniqueName2
        assert 1 == featureIndex1
        assert 1 == featureIndex2
        assert featureEventList1.size() == 3 // this captures everything
        assert featureEventList1[0].size() == 1
        assert featureEventList1[1].size() == 2
        assert featureEventList1[2].size() == 1
        assert featureEventList2.size() == 3 // this captures only the parts in its own history
        assert featureEventList2[0].size() == 1
        assert featureEventList2[1].size() == 2
        assert featureEventList2[2].size() == 1
        assert 2 == FeatureEvent.countByUniqueName(uniqueName2)
        assert 2 == FeatureEvent.countByUniqueName(uniqueName1)
        assert 1 == futureEvents.size()
        assert 1 == previousEvents.size()
        assert previousEvents[0].size() == 1
        assert futureEvents[0].size() == 1

        assert 3 == service.getHistory(uniqueName2).size()
        assert 3 == service.getHistory(uniqueName1).size()

        assert featureEventList1[2].size() == 1
        assert !featureEventList1[2][0].current
        assert featureEventList1[2][0].operation == FeatureOperation.MERGE_TRANSCRIPTS


        assert featureEventList1[1].size() == 2
        for (fe in featureEventList1[1]) {
            assert fe.current
            assert fe.operation == FeatureOperation.ADD_TRANSCRIPT || fe.operation == FeatureOperation.SET_EXON_BOUNDARIES
        }


        assert featureEventList1[0].size() == 1
        assert !featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT




        when: "when we undo again (current index is 1)"
        service.setTransactionForFeature(uniqueName2, 0)
        currentFeatureEventArray = service.findCurrentFeatureEvent(uniqueName2)
        FeatureEvent featureEvent1 = currentFeatureEventArray.find(){ it.uniqueName==uniqueName1}
        FeatureEvent featureEvent2 = currentFeatureEventArray.find(){ it.uniqueName==uniqueName2}

        then: "we should get both back as current, with set exon boundary"
        assert featureEvent1.uniqueName==uniqueName1
        assert featureEvent2.uniqueName==uniqueName2
        assert currentFeatureEventArray != null
        assert currentFeatureEventArray.size() == 2
        for(fe in currentFeatureEventArray){
            assert fe.operation==FeatureOperation.ADD_TRANSCRIPT
            assert fe.current
//            fe.operation==FeatureOperation.SET_EXON_BOUNDARIES
        }
//        assert featureEvent2.operation == FeatureOperation.ADD_TRANSCRIPT
//        assert featureEvent2.uniqueName == uniqueName2


        when: "we get the histories"
        featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)
        futureEvents = service.findFutureFeatureEvents(featureEvent2)
        previousEvents = service.findPreviousFeatureEvents(featureEvent2)

        then: "we verify that we are at the right place"
//        assert currentFeatureEventArray.first().operation == FeatureOperation.ADD_TRANSCRIPT
//        assert currentFeatureEventArray.first().uniqueName == uniqueName2
        assert 1 == featureIndex1
        assert 0 == featureIndex2
        assert featureEventList1==featureEventList2
        assert featureEventList1.size() == 3 // this captures everything
        assert featureEventList1[0].size() == 1
        assert featureEventList1[1].size() == 2
        assert featureEventList1[2].size() == 1
        assert 2 == FeatureEvent.countByUniqueName(uniqueName2)
        assert 2 == FeatureEvent.countByUniqueName(uniqueName1)
        assert 2 == futureEvents.size()
        assert 0 == previousEvents.size()
        assert futureEvents[0].size() == 1 // this ignores the history of the other until we remerge
        assert futureEvents[1].size() == 1


        assert featureEventList1[2].size() == 1
        assert !featureEventList1[2][0].current
        assert featureEventList1[2][0].operation == FeatureOperation.MERGE_TRANSCRIPTS


        assert featureEventList1[1].size() == 2
        for (fe in featureEventList1[1]) {
            if(!fe.current){
                assert fe.operation==FeatureOperation.SET_EXON_BOUNDARIES
            }
            else{
                assert fe.operation==FeatureOperation.ADD_TRANSCRIPT
            }
        }


        assert featureEventList1[0].size() == 1
        assert featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
    }

    void "undo merges independently after merge on symmetric tree"() {

        given: "add 2 transcripts"
        String name1 = "sox9a-0001"
        String name2 = "sox9b-0001"
        String uniqueName1 = "aaaaaa"
        String uniqueName2 = "bbbbbb"

        when: "we add 2 feature events"
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT, name1, uniqueName1, new JSONObject(), new JSONObject(), new JSONObject(), null)
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT, name2, uniqueName2, new JSONObject(), new JSONObject(), new JSONObject(), null)
        List<List<FeatureEvent>> featureEventList1 = service.getHistory(uniqueName1)
        List<List<FeatureEvent>> featureEventList2 = service.getHistory(uniqueName2)

        then: "we should see a feature event"
        assert 1 == FeatureEvent.countByUniqueName(uniqueName1)
        assert featureEventList1.size() == 1
        assert 1 == FeatureEvent.countByUniqueName(uniqueName2)
        assert featureEventList2.size() == 1

        when: "we we add exons to each and merge them"
        service.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, name2, uniqueName2, new JSONObject(), new JSONObject(), new JSONObject(), null)
        service.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, name1, uniqueName1, new JSONObject(), new JSONObject(), new JSONObject(), null)
        JSONArray oldJsonArray = new JSONArray()
        oldJsonArray.add(new JSONObject())
        service.addMergeFeatureEvent(name2, uniqueName2, name1, uniqueName1, new JSONObject(), oldJsonArray, new JSONObject(), null)
        Integer featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        Integer featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)

        Map<String, Map<Long, FeatureEvent>> featureEventMap = service.extractFeatureEventGroup(uniqueName1)
        List<FeatureEvent> featureEventList = FeatureEvent.findAllByUniqueNameAndCurrent(uniqueName2, true)
        FeatureEvent currentFeature = featureEventList.first()

        then: "check our data structures"
        assert featureIndex1 == 2
        assert featureIndex2 == 2
        assert featureEventList.size() == 1
        assert featureEventMap.size() == 2

        when: "we get the current feature event"
        List<FeatureEvent> currentFeatureEventArray = service.findCurrentFeatureEvent(uniqueName1)

        then: "it should not be null"
        // this should return
        assert currentFeatureEventArray.size() == 1
        assert currentFeatureEventArray.first().operation == FeatureOperation.MERGE_TRANSCRIPTS

        when: "we get the histories"
        featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)
        List<List<FeatureEvent>> futureEvents = service.findFutureFeatureEvents(currentFeatureEventArray.first())
        List<List<FeatureEvent>> previousEvents = service.findPreviousFeatureEvents(currentFeatureEventArray.first())

        then: "we should see one feature events, with the second one current and the prior one before"
        assert currentFeatureEventArray.first()
        assert 2 == featureIndex1
        assert 2 == featureIndex2
        assert featureEventList1.size() == 3
        assert featureEventList1==featureEventList2

        assert featureEventList1[2].size() == 1
        assert featureEventList1[2][0].current
        assert featureEventList1[2][0].operation == FeatureOperation.MERGE_TRANSCRIPTS

        assert featureEventList1[1].size() == 2
        assert featureEventList1[1][0].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert featureEventList1[1][1].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert !featureEventList1[1][0].current
        assert !featureEventList1[1][1].current

        assert featureEventList1[0].size() == 2
        assert !featureEventList1[0][0].current
        assert featureEventList1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert !featureEventList1[0][1].current
        assert featureEventList1[0][1].operation == FeatureOperation.ADD_TRANSCRIPT

        assert 2 == FeatureEvent.countByUniqueName(uniqueName1)
        assert 3 == FeatureEvent.countByUniqueName(uniqueName2)

        assert 0 == futureEvents.size()
        assert 2 == previousEvents.size()
        assert previousEvents[0].size() == 2
        assert previousEvents[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert previousEvents[0][1].operation == FeatureOperation.ADD_TRANSCRIPT
        assert previousEvents[1].size() == 2
        assert previousEvents[1][0].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert previousEvents[1][1].operation == FeatureOperation.SET_EXON_BOUNDARIES


        when: "when we undo merge on A2B2 (current index is 2)"
        service.setTransactionForFeature(uniqueName1, 1)
        currentFeatureEventArray = service.findCurrentFeatureEvent(uniqueName1)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)
        featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)
        // just evaluate one side of this
        futureEvents = service.findFutureFeatureEvents(currentFeatureEventArray.first())
        previousEvents = service.findPreviousFeatureEvents(currentFeatureEventArray.first())

        then: "we should get both back as current, with set exon boundary (A2B2)"
        assert currentFeatureEventArray != null
        assert currentFeatureEventArray.size() == 2
        assert currentFeatureEventArray.last().operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert currentFeatureEventArray.first().operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert featureEventList1==featureEventList2

        then: "we verify that we are at the right place"
        assert 1 == featureIndex1
        assert 1 == featureIndex2
        assert featureEventList1.size() == 3 // this captures everything
        assert featureEventList1[0].size() == 2
        assert featureEventList1[1].size() == 2
        assert featureEventList1[2].size() == 1
        assert featureEventList1.size() == 3 // this captures only the parts in its own history
        assert 2 == FeatureEvent.countByUniqueName(uniqueName1)
        assert 3 == FeatureEvent.countByUniqueName(uniqueName2)
        assert 1 == futureEvents.size()
        assert 1 == previousEvents.size()
        assert previousEvents[0].size() == 1
        assert futureEvents[0].size() == 1
        assert previousEvents[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert futureEvents[0][0].operation == FeatureOperation.MERGE_TRANSCRIPTS



        when: "when we undo again (current index is 1) (A2B1)"
        service.setTransactionForFeature(uniqueName2, 0)
        currentFeatureEventArray = service.findCurrentFeatureEvent(uniqueName2)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)
        featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)
        // just evaluate one side of this
        FeatureEvent fe1 = currentFeatureEventArray.find(){ it.uniqueName == uniqueName1}
        FeatureEvent fe2 = currentFeatureEventArray.find(){ it.uniqueName == uniqueName2}
        List<List<FeatureEvent>> futureEvents2 = service.findFutureFeatureEvents(fe2)
        List<List<FeatureEvent>> previousEvents2 = service.findPreviousFeatureEvents(fe2)
        List<List<FeatureEvent>> futureEvents1 = service.findFutureFeatureEvents(fe1)
        List<List<FeatureEvent>> previousEvents1 = service.findPreviousFeatureEvents(fe1)

        then: "we should get both back as current, with set exon boundary (A2B2)"
        assert featureEventList1==featureEventList2
        assert currentFeatureEventArray != null
        assert currentFeatureEventArray.size() == 2
        assert fe2.operation == FeatureOperation.ADD_TRANSCRIPT
        assert fe1.operation == FeatureOperation.SET_EXON_BOUNDARIES

        then: "we verify that we are at the right place WRT 2"
        assert 1 == featureIndex1
        assert 0 == featureIndex2
        assert featureEventList1.size() == 3 // this captures everything
        assert featureEventList1[0].size() == 2
        assert featureEventList1[1].size() == 2
        assert featureEventList1[2].size() == 1
        assert featureEventList1.size() == 3 // this captures only the parts in its own history
        assert 2 == FeatureEvent.countByUniqueName(uniqueName1)
        assert 3 == FeatureEvent.countByUniqueName(uniqueName2)
        assert 2 == futureEvents2.size()
        assert 0 == previousEvents2.size()
        assert futureEvents2[0].size() == 1
        assert futureEvents2[1].size() == 1
        assert futureEvents2[0][0].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert futureEvents2[1][0].operation == FeatureOperation.MERGE_TRANSCRIPTS
        assert 1 == futureEvents1.size()
        assert 1 == previousEvents1.size()
        assert futureEvents1[0].size() == 1
        assert previousEvents1[0].size() == 1
        assert previousEvents1[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert futureEvents1[0][0].operation == FeatureOperation.MERGE_TRANSCRIPTS

        when: "when we undo again (current index is 1) (A1B1)"
        service.setTransactionForFeature(uniqueName1, 0)
        currentFeatureEventArray = service.findCurrentFeatureEvent(uniqueName1)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)
        featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)
        // just evaluate one side of this
        fe1 = currentFeatureEventArray.find(){ it.uniqueName == uniqueName1}
        fe2 = currentFeatureEventArray.find(){ it.uniqueName == uniqueName2}
        futureEvents2 = service.findFutureFeatureEvents(fe2)
        previousEvents2 = service.findPreviousFeatureEvents(fe2)
        futureEvents1 = service.findFutureFeatureEvents(fe1)
        previousEvents1 = service.findPreviousFeatureEvents(fe1)

        then: "we should get both back as current, with set exon boundary (A2B2)"
        assert featureEventList1==featureEventList2
        assert currentFeatureEventArray != null
        assert currentFeatureEventArray.size() == 2
        assert fe2.operation == FeatureOperation.ADD_TRANSCRIPT
        assert fe1.operation == FeatureOperation.ADD_TRANSCRIPT

        then: "we verify that we are at the right place"
        assert 0 == featureIndex1
        assert 0 == featureIndex2
        assert featureEventList1.size() == 3 // this captures everything
        assert featureEventList1[0].size() == 2
        assert featureEventList1[1].size() == 2
        assert featureEventList1[2].size() == 1
        assert featureEventList1.size() == 3 // this captures only the parts in its own history
        assert 2 == FeatureEvent.countByUniqueName(uniqueName1)
        assert 3 == FeatureEvent.countByUniqueName(uniqueName2)
        assert 2 == futureEvents1.size()
        assert 0 == previousEvents1.size()
        assert 2 == futureEvents2.size()
        assert 0 == previousEvents2.size()
        assert futureEvents1[0].size() == 1
        assert futureEvents1[1].size() == 1
        assert futureEvents1[0][0].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert futureEvents1[1][0].operation == FeatureOperation.MERGE_TRANSCRIPTS
        assert futureEvents2[0].size() == 1
        assert futureEvents2[1].size() == 1
        assert futureEvents2[0][0].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert futureEvents2[1][0].operation == FeatureOperation.MERGE_TRANSCRIPTS
    }

    void "undo merges independently after merge on symmetric tree with different current indices"() {

        given: "add 2 transcripts"
        String name1 = "sox9a-0001"
        String name2 = "sox9b-0001"
        String uniqueName1 = "aaaaaa"
        String uniqueName2 = "bbbbbb"

        when: "we add 2 feature events"
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT, name1, uniqueName1, new JSONObject(), new JSONObject(), new JSONObject(), null)
        service.addNewFeatureEvent(FeatureOperation.ADD_TRANSCRIPT, name2, uniqueName2, new JSONObject(), new JSONObject(), new JSONObject(), null)
        List<List<FeatureEvent>> featureEventList1 = service.getHistory(uniqueName1)
        List<List<FeatureEvent>> featureEventList2 = service.getHistory(uniqueName2)
        service.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, name2, uniqueName2, new JSONObject(), new JSONObject(), new JSONObject(), null)
        service.addNewFeatureEvent(FeatureOperation.SET_EXON_BOUNDARIES, name1, uniqueName1, new JSONObject(), new JSONObject(), new JSONObject(), null)
        JSONArray oldJsonArray = new JSONArray()
        oldJsonArray.add(new JSONObject())
        service.addMergeFeatureEvent(name2, uniqueName2, name1, uniqueName1, new JSONObject(), oldJsonArray, new JSONObject(), null)
        Integer featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        Integer featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)
        List<FeatureEvent> currentFeatureEventArray = service.findCurrentFeatureEvent(uniqueName1)

        then: "it should not be null"
        // this should return
        assert currentFeatureEventArray.size() == 1
        assert currentFeatureEventArray.first().operation == FeatureOperation.MERGE_TRANSCRIPTS



        when: "when we undo merge to get to A1, B2 so that they will have separate indices"
        service.setTransactionForFeature(uniqueName1, 0)
        service.setTransactionForFeature(uniqueName2, 1)
        currentFeatureEventArray = service.findCurrentFeatureEvent(uniqueName1)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)
        featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)
        FeatureEvent currentFeatureEvent1 = currentFeatureEventArray.find(){it.uniqueName==uniqueName1}
        FeatureEvent currentFeatureEvent2 = currentFeatureEventArray.find(){it.uniqueName==uniqueName2}
        // just evaluate one side of this
        List<List<FeatureEvent>> futureEvents1 = service.findFutureFeatureEvents(currentFeatureEvent1)
        List<List<FeatureEvent>> previousEvents1 = service.findPreviousFeatureEvents(currentFeatureEvent1)
        List<List<FeatureEvent>> futureEvents2 = service.findFutureFeatureEvents(currentFeatureEvent2)
        List<List<FeatureEvent>> previousEvents2 = service.findPreviousFeatureEvents(currentFeatureEvent2)

        then: "we should get both back as current, with set exon boundary (A2B2)"
        assert currentFeatureEventArray != null
        assert currentFeatureEventArray.size() == 2
        assert currentFeatureEvent1.operation == FeatureOperation.ADD_TRANSCRIPT
        assert currentFeatureEvent2.operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert featureEventList1==featureEventList2

        then: "we verify that we are at the right place"
        assert 0 == featureIndex1
        assert 1 == featureIndex2
        assert featureEventList1.size() == 3 // this captures everything
        assert featureEventList1[0].size() == 2
        assert featureEventList1[1].size() == 2
        assert featureEventList1[2].size() == 1
        assert 2 == FeatureEvent.countByUniqueName(uniqueName1)
        assert 3 == FeatureEvent.countByUniqueName(uniqueName2)
        assert 2 == futureEvents1.size()
        assert 0 == previousEvents1.size()
        assert 1 == futureEvents2.size()
        assert 1 == previousEvents2.size()

        assert futureEvents1[0].size() == 1
        assert futureEvents1[1].size() == 1
        assert futureEvents1[0][0].operation == FeatureOperation.SET_EXON_BOUNDARIES
        assert futureEvents1[1][0].operation == FeatureOperation.MERGE_TRANSCRIPTS

        assert futureEvents2[0].size() == 1
        assert previousEvents2[0].size() == 1
        assert previousEvents2[0][0].operation == FeatureOperation.ADD_TRANSCRIPT
        assert futureEvents2[0][0].operation == FeatureOperation.MERGE_TRANSCRIPTS


        when: "when we redo on A1 so A1 B2 -> AB"
        service.setTransactionForFeature(uniqueName1, 2)
        featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)
        currentFeatureEventArray = service.findCurrentFeatureEvent(uniqueName2)
        featureEventList1 = service.getHistory(uniqueName1)
        featureEventList2 = service.getHistory(uniqueName2)
        featureIndex1 = service.getCurrentFeatureEventIndex(uniqueName1)
        featureIndex2 = service.getCurrentFeatureEventIndex(uniqueName2)
        // just evaluate one side of this
//        currentFeatureEvent1 =currentFeatureEventArray.find(){ it.uniqueName == uniqueName1}
        currentFeatureEvent2= currentFeatureEventArray.find(){ it.uniqueName == uniqueName2}
        futureEvents2 = service.findFutureFeatureEvents(currentFeatureEvent2)
        previousEvents2 = service.findPreviousFeatureEvents(currentFeatureEvent2)
//        futureEvents1 = service.findFutureFeatureEvents(currentFeatureEvent1)
//        previousEvents1 = service.findPreviousFeatureEvents(currentFeatureEvent1)

        then: "this should force a merge (AB)"
        assert featureIndex1==2
        assert featureIndex2==2
        assert currentFeatureEventArray.size() == 1
        assert currentFeatureEventArray.first().uniqueName==uniqueName2
//        assert 0 == futureEvents1.size()
//        assert 2 == previousEvents1.size()
        assert 0 == futureEvents2.size()
        assert 2 == previousEvents2.size()
        assert featureEventList1==featureEventList2
        assert currentFeatureEvent2.operation == FeatureOperation.MERGE_TRANSCRIPTS

    }

}
