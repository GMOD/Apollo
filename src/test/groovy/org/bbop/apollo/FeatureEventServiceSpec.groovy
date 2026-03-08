package org.bbop.apollo

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.bbop.apollo.history.FeatureOperation
import spock.lang.Specification

class FeatureEventServiceSpec extends Specification implements ServiceUnitTest<FeatureEventService>, DataTest {

    Class[] getDomainClassesToMock() {
        [FeatureEvent, User]
    }

    Date today = new Date()
    String classUniqueName = "uniqueName"

    def setup() {
        FeatureEvent f1 = new FeatureEvent(operation: FeatureOperation.ADD_FEATURE, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 7, current: false).save(failOnError: true)
        FeatureEvent f2 = new FeatureEvent(operation: FeatureOperation.SPLIT_TRANSCRIPT, parentId: f1.id, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 6, current: false).save(failOnError: true)
        f1.childId = f2.id
        FeatureEvent f3 = new FeatureEvent(operation: FeatureOperation.SET_TRANSLATION_END, parentId: f2.id, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 5, current: false).save(failOnError: true)
        f2.childId = f3.id
        FeatureEvent f4 = new FeatureEvent(operation: FeatureOperation.SET_READTHROUGH_STOP_CODON, parentId: f3.id, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 4, current: false).save(failOnError: true)
        f3.childId = f4.id
        FeatureEvent f5 = new FeatureEvent(operation: FeatureOperation.SET_EXON_BOUNDARIES, parentId: f4.id, name: "Gene123", uniqueName: classUniqueName, dateCreated: today - 3, current: true).save(failOnError: true)
        f4.childId = f5.id

        f1.save()
        f2.save()
        f3.save()
        f4.save()
    }

    def cleanup() {
    }

    void "get current feature event"() {
        when: "we get the current feature event"
        List<FeatureEvent> currentFeatureEvents = service.findCurrentFeatureEvent(classUniqueName)

        then: "we should get the current one"
        assert currentFeatureEvents != null
        assert currentFeatureEvents.size() == 1
        assert currentFeatureEvents[0].current
        assert currentFeatureEvents[0].operation == FeatureOperation.SET_EXON_BOUNDARIES
    }

    void "get history for feature"() {
        when: "we get the history"
        List<List<FeatureEvent>> history = service.getHistory(classUniqueName)

        then: "we should see the previous events"
        assert history != null
        assert history.size() == 5
    }

    void "get previous feature event"() {
        when: "we get the previous feature event"
        List<FeatureEvent> currentFeatureEvents = service.findCurrentFeatureEvent(classUniqueName)
        FeatureEvent currentFeatureEvent = currentFeatureEvents[0]
        List<List<FeatureEvent>> previousFeatureEvents = service.findPreviousFeatureEvents(currentFeatureEvent)

        then: "we should get the previous ones"
        assert previousFeatureEvents != null
        assert previousFeatureEvents.size() == 4
        assert previousFeatureEvents[0][0].operation == FeatureOperation.ADD_FEATURE
    }
}
