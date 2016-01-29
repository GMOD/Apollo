package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.sequence.Overlapper
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.TranslationTable

/**
 * TODO:  move all of this stuff to a database
 */
@Transactional
class ConfigWrapperService {

    def grailsApplication

    Boolean useCDS() {
        return grailsApplication.config.apollo.use_cds_for_new_transcripts
    }

    TranslationTable getTranslationTable() {
        return SequenceTranslationHandler.getTranslationTableForGeneticCode(getTranslationCode())
    }

    String getTranslationCode(){
        return grailsApplication.config.apollo.get_translation_code.toString()
    }

    Boolean hasDbxrefs(){
        return grailsApplication.config.apollo.feature_has_dbxrefs
    }
    Boolean hasAttributes(){
        return grailsApplication.config.apollo.feature_has_attributes
    }

    Boolean hasPubmedIds(){
        return grailsApplication.config.apollo.feature_has_pubmed_ids
    }
    Boolean hasGoIds(){
        return grailsApplication.config.apollo.feature_has_go_ids
    }
    Boolean hasComments(){
        return grailsApplication.config.apollo.feature_has_comments
    }
    Boolean hasStatus(){
        return grailsApplication.config.apollo.feature_has_status
    }

    List<String> getSpliceDonorSites(){
        return grailsApplication.config.apollo.splice_donor_sites
    }

    List<String> getSpliceAcceptorSites(){
        return grailsApplication.config.apollo.splice_acceptor_sites
    }

    int getDefaultMinimumIntronSize() {
        return grailsApplication.config.apollo.default_minimum_intron_size
    }

    def getSequenceSearchTools() {
        return grailsApplication.config.apollo.sequence_search_tools
    }

    def getDataAdapterTools() {
        return grailsApplication.config.apollo.data_adapters
    }

}
