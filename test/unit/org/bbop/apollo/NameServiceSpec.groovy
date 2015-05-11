package org.bbop.apollo

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(NameService)
class NameServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "letter padding strategy should work"() {

        when: "we have 1"
        LetterPaddingStrategy letterPaddingStrategy = new LetterPaddingStrategy()

        then: "assert a"
        assert "a" == letterPaddingStrategy.pad(0)
        assert "b" == letterPaddingStrategy.pad(1)
        assert "c" == letterPaddingStrategy.pad(2)

    }
}
