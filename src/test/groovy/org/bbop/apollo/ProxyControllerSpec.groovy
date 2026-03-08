package org.bbop.apollo

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.bbop.apollo.gwt.shared.PermissionEnum
import spock.lang.Specification

class ProxyControllerSpec extends Specification implements ControllerUnitTest<ProxyController>, DataTest {

    Class[] getDomainClassesToMock() {
        [Proxy]
    }

    def setup() {
        controller.permissionService = [
                checkPermissions: { PermissionEnum perm -> true }
        ]
    }

    void "Test the index action returns the correct model"() {
        when: "The index action is executed"
        controller.index()

        then: "The model is correct"
        model.proxyInstanceCount == 0
    }

    void "Test the save action correctly persists an instance"() {
        when: "The save action is executed with a valid instance"
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'POST'
        def proxy = new Proxy(
                referenceUrl: 'http://someValidName.com',
                targetUrl: 'http://someOtherValidName.com',
                active: true
        )
        controller.save(proxy)

        then: "A redirect is issued"
        response.redirectedUrl != null
        Proxy.count() == 1
    }

    void "Test that the delete action deletes an instance if it exists"() {
        when: "A domain instance is created"
        request.contentType = FORM_CONTENT_TYPE
        request.method = 'DELETE'
        def proxy = new Proxy(
                referenceUrl: 'http://someValidName.com',
                targetUrl: 'http://someOtherValidName.com',
                active: true
        ).save(flush: true)

        then: "It exists"
        Proxy.count() == 1

        when: "The domain instance is passed to the delete action"
        controller.delete(proxy)

        then: "The instance is deleted"
        Proxy.count() == 0
    }
}
