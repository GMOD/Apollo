package org.bbop.apollo

import grails.transaction.NotTransactional
//import grails.transaction.Transactional
import groovy.json.JsonSlurper

//@Transactional
class PhoneHomeService {

    def configWrapperService

    @NotTransactional
    def pingServer() {
        String apiString = configWrapperService.pingUrl
        log.debug("Phoning home to ${apiString}")
        URL apiUrl = new URL(apiString)
        def responseJson = new JsonSlurper().parse(apiUrl)
        return responseJson
    }
}
