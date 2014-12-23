package org.bbop.apollo

import grails.converters.JSON
import grails.web.JSONBuilder

class AnnotatorController {

    def index() {
        Organism.all.each {
            println it.commonName
        }
    }

    def demo() {
    }

    def findAnnotationsForSequence() {

        // execute a single query to minimize IO
        List<Feature,FeatureRelationship,FeatureRelationship> allFeatures = Feature.executeQuery("select f,p,c from Feature f left join f.parentFeatureRelationships p left join f.childFeatureRelationships c")
        println "printing first 1 ${allFeatures.get(0) as JSON}"
        println "printing second 1 ${allFeatures.get(1) as JSON}"
        println "printing third 1 ${allFeatures.get(2) as JSON}"
//        def allFeatures = Feature.list(fetch:[parentFeatureRelationships.childFeature: 'join'])
//        println "other features: ${allFeatures.size()}"
//        println "other features data : ${allFeatures as JSON}"

//        def allFeatureRelationships = FeatureRelationship.executeQuery("select p from FeatureRelationship fr left join fr.parentFeature p left join fr.childFeature c")
//        println "all feature releatinship ${allFeatureRelationships as JSON}"

        def builder = new JSONBuilder()

        // just the genes
        def topLevelFeatureSet = allFeatures.findAll(){
            it[0] instanceof Feature && it[0]?.childFeatureRelationships?.size()==0
        }

        // we only want the feature here
       def topLevelFeatures = topLevelFeatureSet.collect(){
           it[0]
       }

        println "GEne data : ${topLevelFeatures as JSON}"

        def result = builder.build {
            topLevelFeatures
//            categories = ['a', 'b', 'c']
//            title = "Hello JSON"
//            information = {
//                pages = 10
//            }
        }
        // 1 - put top level data into



//        def topLevelFeatures = allFeatures.findAll() {
////            it?.childFeatureRelationships?.size() == 0
//            true
//        }

//        render(contentType: "application/json") {
//            topLevelFeatures
//        }

        for(int i =0 ;  i < 20 ; i++) println "HERE"
        println result
        render result
    }

    def what(String data) {
        println params
        println data
        def dataObject = JSON.parse(data)
        println dataObject
        println dataObject.thekey

        render dataObject.thekey
    }

    def search(String data) {
        println params
        println data
        def dataObject = JSON.parse(data)
        println dataObject
        println dataObject.query

        // do stuff
        String result = ['pax6a-001', 'pax6a-002']

        dataObject.result = result

        println "return object ${dataObject} vs ${dataObject as JSON}"

        render dataObject as JSON
    }
}
