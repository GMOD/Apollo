package org.bbop.apollo

import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Defines REST track here
 * http://gmod.org/wiki/JBrowse_Configuration_Guide#JBrowse_REST_Feature_Store_API
 */
class ProjectionTrackController {

    def projectionService
    def requestHandlingService

    def index() {}

    def regionFeatureDensities(){
        println "regionFeatureDensities params: ${params}"
        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        render jsonObject
    }

    def statsGlobal(){
        println "stats global params: ${params}"
        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        render jsonObject
    }

    def statsRegion(){
        println "stats region params: ${params}"
        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        render jsonObject
    }

    def features(){

        println "features params: ${params}"
//        println "id: ${id}"
//        println "start: ${start}"
//        println "end: ${end}"

        JSONObject features1 = new JSONObject(start:123,end:456,name:"region1",type:"MRNA",label:"first label",Id:"abc123",unique_name:"def567")
        JSONObject features2 = new JSONObject(start:789,end:1012,name:"region2")

        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer(features1,features2)



        render jsonObject
    }
}
