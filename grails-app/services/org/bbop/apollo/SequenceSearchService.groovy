package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class SequenceSearchService {

    def configWrapperService
    def serviceMethod() {

    }

    def searchSequence(JSONObject input) {
        configWrapperService.getSequenceSearchTools()

    }
}
