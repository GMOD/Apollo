package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

class AnnotatorController {

    def featureService

    def index() {
        Organism.all.each {
            println it.commonName
        }
    }

    def demo() {
    }

    private JSONObject createJSONFeatureContainer(JSONObject... features) throws JSONException {
        JSONObject jsonFeatureContainer = new JSONObject();
        JSONArray jsonFeatures = new JSONArray();
        jsonFeatureContainer.put(FeatureStringEnum.FEATURES.value, jsonFeatures);
        for (JSONObject feature : features) {
            jsonFeatures.put(feature);
        }
        return jsonFeatureContainer;
    }

    def findAnnotationsForSequence() {

        JSONObject returnObject = createJSONFeatureContainer()
        // execute a single query to minimize IO
        // necessary to do all 3?
        // TODO: should just be a simple call?
        List<Feature> allFeatures = Feature.executeQuery("select f from Feature f ")

        // just the genes
        def topLevelFeatureList = allFeatures.findAll(){
            it?.childFeatureRelationships?.size()==0
        }



        for(Feature feature in topLevelFeatureList){
            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature,false));
        }

        for(int i =0 ;  i < 20 ; i++) println "HERE"
        render returnObject

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
