package org.bbop.apollo

import grails.test.mixin.TestFor
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.TranslationTable
import org.bbop.apollo.sequence.TranslationTableReader
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
class TranslationTableSpec extends Specification {


    def setup() {
    }

    def cleanup() {
    }

    // if we init with "default" does that work?
    void "is the default behavior correct?"() {

    }

    void "can I read in translation tables"() {

        given:
        File file = new File("web-app/translation_tables/ncbi_11_translation_table.txt")

        when: "we read a translation table"
        TranslationTable translationTable = TranslationTableReader.readTable(file)


        then: "we should get the correct results"
        assert true==false
    }

    void "is the init behavior correct?"() {

        given:
        SequenceTranslationHandler handler = new SequenceTranslationHandler()

        when: "we read a translation table"
        // be something with STOPS, etc.
        TranslationTable translationTable = handler.getTranslationTableForGeneticCode(5)


        then: "we should get the correct results"
        assert translationTable!=null
        assert true==false
    }
}
