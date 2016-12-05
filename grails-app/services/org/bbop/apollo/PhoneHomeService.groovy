package org.bbop.apollo

import grails.transaction.Transactional
import groovy.json.JsonSlurper

@Transactional
class PhoneHomeService {

    def configWrapperService
    def grailsApplication


    /**
     * Only process args if there is a message
     * @param message
     * @param args
     * @return
     */
    def pingServer(String message = null ,Map<String,String> argMap = [:]) {
        if(!configWrapperService.phoneHome) {
            println("Not phonning home")
            return
        }

        try {
            String apiString = configWrapperService.pingUrl
            ServerData.withTransaction{
                if(ServerData.count>1){
                    ServerData.deleteAll(ServerData.all)
                }
                if(ServerData.count==0){
                    new ServerData().save(flush: true,insert:true)
                }
                apiString += "?${PhoneHomeEnum.SERVER.value}="+ServerData.first().name
                apiString += "&${PhoneHomeEnum.ENVIRONMENT.value}="+grails.util.Environment.current.name
                if(message){
                    apiString += "&${PhoneHomeEnum.MESSAGE.value}=${message}"

                    for(k in argMap){
                        apiString += "&${k.key}=${k.value}"
                    }
                }
            }
            log.debug("Phoning home to ${apiString}")
            URL apiUrl = new URL(apiString)
            def responseJson = new JsonSlurper().parse(apiUrl)
            return responseJson
        } catch (e) {
            log.warn("Not phoning phone due to error: "+e.toString())
        }
    }
}
