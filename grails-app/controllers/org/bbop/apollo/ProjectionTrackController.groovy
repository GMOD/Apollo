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

    /**
     *
     {
     "bins":  [ 51, 50, 58, 63, 57, 57, 65, 66, 63, 61,
     56, 49, 50, 47, 39, 38, 54, 41, 50, 71,
     61, 44, 64, 60, 42
     ],
     "stats": {
     "basesPerBin": 200,
     "max": 88
     }
     }
     * @return
     */
    def regionFeatureDensities(){
        println "regionFeatureDensities params: ${params}"
        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        render jsonObject
    }

    /**
     * {

     "featureDensity": 0.02,

     "featureCount": 234235,

     "scoreMin": 87,
     "scoreMax": 87,
     "scoreMean": 42,
     "scoreStdDev": 2.1
     }
     * @return
     */
    def statsGlobal(){
        println "stats global params: ${params}"
        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        jsonObject.featureDensity=1
        jsonObject.featureCount=1
        jsonObject.scoreMin=1
        jsonObject.scoreMax=1
        jsonObject.scoreMean=1
        jsonObject.scoreStdDev=1
        render jsonObject
    }

    /**
     * Same as statsGlobal, but only for the region
     * @return
     */
    def statsRegion(){
        println "stats region params: ${params}"
        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        jsonObject.featureDensity=1
        jsonObject.featureCount=1
        jsonObject.scoreMin=1
        jsonObject.scoreMax=1
        jsonObject.scoreMean=1
        jsonObject.scoreStdDev=1
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
