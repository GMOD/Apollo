package org.bbop.apollo

import grails.transaction.NotTransactional
//import grails.transaction.Transactional
import groovy.json.JsonSlurper

//@Transactional
class PhoneHomeService {

    def configWrapperService

    /**
     * Only process args if there is a message
     * @param message
     * @param args
     * @return
     */
    @NotTransactional
    def pingServer(String message = null ,Map<String,String> argMap = [:]) {
        String apiString = configWrapperService.pingUrl
        if(message){
            apiString += "?message=${message}"

            for(k in argMap){
                apiString += "&${k.key}=${k.value}"
            }
        }
        log.debug("Phoning home to ${apiString}")
        URL apiUrl = new URL(apiString)
        def responseJson = new JsonSlurper().parse(apiUrl)
        return responseJson
    }
}
