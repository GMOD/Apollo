package org.bbop.apollo

import grails.transaction.Transactional
import groovy.json.JsonSlurper

@Transactional
class PhoneHomeService {

    def configWrapperService
    def grailsApplication


    def startPhoneHomeServer(){
        Integer timer =   24 * 60 * 60 * 1000
        new Timer().schedule({
//            def map = ["numUsers":User.count.toString(),"numAnnotations": Feature.count.toString(),"numOrganisms": Organism.count.toString()]
            def map = [:]
            pingServer("running",map)
        } as TimerTask, 1000, timer)
    }

    /**
     * Only process args if there is a message
     * @param message
     * @param args
     * @return
     */
    def pingServer(String message = null ,Map<String,String> argMap = [:]) {
        String apiString = configWrapperService.pingUrl
        ServerData.withTransaction{
            if(ServerData.count>1){
                ServerData.deleteAll(ServerData.all)
            }
            if(ServerData.count==0){
                new ServerData().save(flush: true,insert:true)
            }
            apiString += "?server="+ServerData.first().name
            apiString += "&environment="+grails.util.Environment.current.name
            if(message){
                apiString += "&message=${message}"

                for(k in argMap){
                    apiString += "&${k.key}=${k.value}"
                }
            }
        }
        log.debug("Phoning home to ${apiString}")
        println("Phoning home to ${apiString}")
        URL apiUrl = new URL(apiString)
        def responseJson = new JsonSlurper().parse(apiUrl)
        return responseJson
    }
}
