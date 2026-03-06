package org.bbop.apollo

import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.TranslationTable
import spock.lang.Specification

class TranslationTableSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "is the default behavior correct?"() {

        given:
        SequenceTranslationHandler handler = new SequenceTranslationHandler()

        when: "we read a translation table"
        TranslationTable translationTable = handler.getDefaultTranslationTable()

        then: "we should get the correct results"
        assert translationTable != null
        assert translationTable.startCodons.size() == 1
        assert translationTable.stopCodons.size() == 3
        assert translationTable.alternateTranslationTable.size() == 1
        assert translationTable.translationTable.size() == 64
        assert translationTable.alternateTranslationTable.size() == 1
    }


    void "can I read in translation tables"() {

        given:
        File file = new File("src/main/webapp/translation_tables/ncbi_11_translation_table.txt")

        when: "we read a translation table"
        TranslationTable translationTable = SequenceTranslationHandler.readTable(file)

        then: "we should get the correct results"
        assert translationTable.startCodons.size() == 1 + 6
        assert translationTable.stopCodons.size() == 3
        assert translationTable.translationTable.size() == 64
        assert translationTable.alternateTranslationTable.size() == 1
    }

    void "is the init behavior correct?"() {

        given:
        SequenceTranslationHandler handler = new SequenceTranslationHandler()

        when: "we read a translation table"
        TranslationTable translationTable = handler.getTranslationTableForGeneticCode("2", "src/main/webapp")

        then: "we should get the correct results"
        assert translationTable != null
        assert translationTable.startCodons.size() == 1 + 1 - 1
        assert translationTable.stopCodons.size() == 3 + 2 - 1
        assert translationTable.translationTable.size() == 64
        assert translationTable.alternateTranslationTable.size() == 1 - 1
    }
}
