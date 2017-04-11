package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.bbop.apollo.authenticator.RemoteUserAuthenticatorService
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(User)
@Mock([User])
class UserSpec extends Specification {


    void "when I add metadata I can retrieve it again"() {

        given: "A user"
        String randomPassword1 = "RandomPassword1"
        String randomPassword2 = "RandomPassword2"
        User user = new User(
                username: "bobjones",
                passwordHash: "abc123",
                firstName: "Bob",
                lastName: "Jones",
        ).save(flush: true, failOnError: true, insert: true)


        when: "I add metadata to the user"
        user.addMetaData(RemoteUserAuthenticatorService.INTERNAL_PASSWORD, randomPassword1)
        User otherUser = User.first()

        then: "I should see it"
        assert User.count == 1
        assert otherUser.getMetaData(RemoteUserAuthenticatorService.INTERNAL_PASSWORD) == randomPassword1

        when: "we change it"
        user.addMetaData(RemoteUserAuthenticatorService.INTERNAL_PASSWORD, randomPassword2)
        JSONObject returnedObject = user.getMetaDataObject()
        otherUser = User.first()

        then: "we should see the change"
        assert User.count == 1
        assert otherUser.getMetaData(RemoteUserAuthenticatorService.INTERNAL_PASSWORD) == randomPassword2
        assert returnedObject.keySet().size() == 1
        assert returnedObject.get(RemoteUserAuthenticatorService.INTERNAL_PASSWORD) == randomPassword2

        when: "We remove it, it should not be there anymore"
        String value = user.removeMetaData(RemoteUserAuthenticatorService.INTERNAL_PASSWORD)
        otherUser = User.first()
        returnedObject = user.getMetaDataObject()

        then: "should reflect it"
        assert User.count == 1
        assert returnedObject.keySet().size() == 0
        assert otherUser.getMetaData(RemoteUserAuthenticatorService.INTERNAL_PASSWORD) == null



    }

}
