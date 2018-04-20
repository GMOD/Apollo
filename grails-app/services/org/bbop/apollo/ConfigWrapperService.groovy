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

    String getTranscriptOverlapper() {
        return grailsApplication.config.apollo.transcript_overlapper
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
        List<String> sites = new ArrayList<String>()
        grailsApplication.config.apollo.splice_donor_sites.each {
            sites.add(it.toLowerCase())
        }
        return sites
    }

    List<String> getSpliceAcceptorSites(){
        List<String> sites = new ArrayList<String>()
        grailsApplication.config.apollo.splice_acceptor_sites.each {
            sites.add(it.toLowerCase())
        }
        return sites
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

    def exportSubFeatureAttrs() {
        return grailsApplication.config.apollo.export_subfeature_attrs
    }

    def getCommonDataDirectory() {
        return grailsApplication.config.apollo.common_data_directory
    }

    def hasChadoDataSource() {
        if (grailsApplication.config.dataSource_chado) {
            return true
        }
        return false
    }

    def isPostgresChadoDataSource() {
        if (hasChadoDataSource()) {
            if (grailsApplication.config.dataSource_chado.url.contains('jdbc:postgresql')) {
                return true
            }
        }
        return false
    }

    def getChadoExportFastaForSequence() {
        return grailsApplication.config.apollo.chado_export_fasta_for_sequence
    }

    def getChadoExportFastaForCds() {
        return grailsApplication.config.apollo.chado_export_fasta_for_cds
    }

    def getAuthentications() {
        grailsApplication.config.apollo.authentications
    }

    def getPingUrl() {
        Boolean phoneHome =  grailsApplication.config.apollo.phone.phoneHome
        if(phoneHome){
            String urlString = grailsApplication.config.apollo.phone.url
            urlString += grailsApplication.config.apollo.phone.bucketPrefix
            urlString += grailsApplication.metadata['app.version']
            urlString += "/"
            urlString += grailsApplication.config.apollo.phone.fileName
            urlString = urlString.toLowerCase()
            return urlString
        }
        return null
    }

    Boolean getPhoneHome() {
        return grailsApplication.config.apollo.phone.phoneHome
    }

    def getExtraTabs(){
        return grailsApplication.config.apollo.extraTabs
    }

    boolean getOnlyOwnersDelete(){
        return grailsApplication.config.apollo.only_owners_delete
    }

    boolean getNativeTrackSelectorDefaultOn(){
        return grailsApplication.config.apollo.native_track_selector_default_on
    }
}
