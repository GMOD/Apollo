package org.bbop.apollo

import grails.transaction.Transactional

/**
 * TODO:  move all of this stuff to a database
 */
@Transactional
class ConfigWrapperService {

    def grailsApplication

    Boolean useCDS() {
        return grailsApplication.config.apollo.use_cds_for_new_transcripts
    }
}
