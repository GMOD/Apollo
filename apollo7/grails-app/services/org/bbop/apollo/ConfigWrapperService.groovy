package org.bbop.apollo

import grails.config.Config
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import org.bbop.apollo.sequence.Overlapper
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.TranslationTable

// BACKWARDS INCOMPATIBILITY: All config access changed from
// grailsApplication.config.apollo.X (dot-notation on ConfigObject)
// to grailsApplication.config.getProperty('apollo.X', Type) (Spring Config)
@Transactional
class ConfigWrapperService {

    GrailsApplication grailsApplication

    private Config getConfig() {
        grailsApplication.config
    }

    String getWebRootDir() {
        return "./"
    }

    Boolean useCDS() {
        return config.getProperty('apollo.use_cds_for_new_transcripts', Boolean, false)
    }

    Boolean getCountAnnotations() {
        return config.getProperty('apollo.count_annotations', Boolean, true)
    }

    Boolean getAddMergedComment() {
        return config.getProperty('apollo.add_merged_comment', Boolean, true)
    }

    String getTranscriptOverlapper() {
        return config.getProperty('apollo.transcript_overlapper', String, 'CDS')
    }

    String getFaToTwobitExe() {
        return config.getProperty('apollo.fa_to_twobit_exe', String, '/usr/local/bin/faToTwoBit')
    }

    TranslationTable getTranslationTable() {
        return SequenceTranslationHandler.getTranslationTableForGeneticCode(getTranslationCode(), getWebRootDir())
    }

    String getTranslationCode() {
        return config.getProperty('apollo.get_translation_code', String, '1')
    }

    Boolean hasDbxrefs() {
        return config.getProperty('apollo.feature_has_dbxrefs', Boolean, true)
    }

    Boolean hasAttributes() {
        return config.getProperty('apollo.feature_has_attributes', Boolean, true)
    }

    Boolean hasPubmedIds() {
        return config.getProperty('apollo.feature_has_pubmed_ids', Boolean, true)
    }

    Boolean hasGoIds() {
        return config.getProperty('apollo.feature_has_go_ids', Boolean, true)
    }

    Boolean hasComments() {
        return config.getProperty('apollo.feature_has_comments', Boolean, true)
    }

    Boolean hasStatus() {
        return config.getProperty('apollo.feature_has_status', Boolean, true)
    }

    List<String> getSpliceDonorSites() {
        List<String> configured = config.getProperty('apollo.splice_donor_sites', List, ['GT'])
        return configured.collect { it.toLowerCase() }
    }

    List<String> getSpliceAcceptorSites() {
        List<String> configured = config.getProperty('apollo.splice_acceptor_sites', List, ['AG'])
        return configured.collect { it.toLowerCase() }
    }

    int getDefaultMinimumIntronSize() {
        return config.getProperty('apollo.default_minimum_intron_size', Integer, 1)
    }

    def getSequenceSearchTools() {
        return config.getProperty('apollo.sequence_search_tools', Map, [:])
    }

    def getDataAdapterTools() {
        return config.getProperty('apollo.data_adapters', List, [])
    }

    def exportSubFeatureAttrs() {
        return config.getProperty('apollo.export_subfeature_attrs', Boolean, false)
    }

    String getCommonDataDirectory() {
        return config.getProperty('apollo.common_data_directory', String, 'apollo_data')
    }

    def hasChadoDataSource() {
        return config.getProperty('dataSource_chado.url', String) != null
    }

    def isPostgresChadoDataSource() {
        if (hasChadoDataSource()) {
            String url = config.getProperty('dataSource_chado.url', String, '')
            return url.contains('jdbc:postgresql')
        }
        return false
    }

    def getChadoExportFastaForSequence() {
        return config.getProperty('apollo.chado_export_fasta_for_sequence', Boolean, false)
    }

    def getChadoExportFastaForCds() {
        return config.getProperty('apollo.chado_export_fasta_for_cds', Boolean, false)
    }

    def getAuthentications() {
        return config.getProperty('apollo.authentications', List, [])
    }

    def storeOrigId() {
        return config.getProperty('apollo.store_orig_id', Boolean, true)
    }

    def getExtraTabs() {
        return config.getProperty('apollo.extraTabs', List, [])
    }

    boolean getOnlyOwnersDelete() {
        return config.getProperty('apollo.only_owners_delete', Boolean, false)
    }

    boolean getNativeTrackSelectorDefaultOn() {
        return config.getProperty('apollo.native_track_selector_default_on', Boolean, false)
    }

    String getGff3Source() {
        return config.getProperty('apollo.gff3.source', String, '.')
    }

    boolean getCalculateNonCanonicalSpliceSites() {
        return config.getProperty('apollo.calculate_non_canonical_splice_sites', Boolean, true)
    }
}
