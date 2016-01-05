package org.bbop.apollo

import grails.test.mixin.TestFor
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.TranslationTable
import org.bbop.apollo.sequence.SequenceTranslationHandler
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

        given:
        SequenceTranslationHandler handler = new SequenceTranslationHandler()

        when: "we read a translation table"
        // be something with STOPS, etc.
        TranslationTable translationTable = handler.getDefaultTranslationTable()

        then: "we should get the correct results"
        assert translationTable!=null
        assert translationTable.startCodons.size()==1
        assert translationTable.stopCodons.size()==3
        assert translationTable.alternateTranslationTable.size()==1
        assert translationTable.translationTable.size()==64
        assert translationTable.alternateTranslationTable.size()==1
    }


    void "can I read in translation tables"() {

        given:
        File file = new File("web-app/translation_tables/ncbi_11_translation_table.txt")

        when: "we read a translation table"
        TranslationTable translationTable = SequenceTranslationHandler.readTable(file)

        then: "we should get the correct results"
        assert translationTable.startCodons.size()==1+6
        assert translationTable.stopCodons.size()==3
        assert translationTable.translationTable.size()==64 // start codons are existing translations
        assert translationTable.alternateTranslationTable.size()==1
    }

    void "is the init behavior correct?"() {

        given:
        SequenceTranslationHandler handler = new SequenceTranslationHandler()

        when: "we read a translation table"
        // be something with STOPS, etc.
        TranslationTable translationTable = handler.getTranslationTableForGeneticCode("2")

        then: "we should get the correct results"
        assert translationTable!=null
        assert translationTable.startCodons.size()==1+1-1
        assert translationTable.stopCodons.size()==3+2-1
        assert translationTable.translationTable.size()==64
        assert translationTable.alternateTranslationTable.size()==1-1
    }

}
