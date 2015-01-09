package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.event.AnnotationEvent
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

class AnnotatorController {

    def featureService
    def requestHandlingService

    def index() {
        Organism.all.each {
            println it.commonName
        }
    }

    def demo() {
    }

    /**
     * updates shallow properties of gene / feature
     * @return
     */
    def updateGene(){
        println "updating gene ${params.data}"
        def data = JSON.parse(params.data.toString()) as JSONObject
        println "uqnieuname 2: ${data.uniquename}"
        println "rendered data ${data as JSON}"
        Feature feature = Feature.findByUniqueName(data.uniquename)
        feature.name = data.name
        feature.symbol.value = data.symbol
        feature.description.value = data.description
        feature.save(flush: true,failOnError: true)

        JSONObject jsonFeature = featureService.convertFeatureToJSON(feature,false)
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature)

        Sequence sequence = feature?.featureLocation?.sequence
        if(sequence){
            AnnotationEvent annotationEvent = new AnnotationEvent(
                    features: updateFeatureContainer
                    , sequence: sequence
                    , operation: AnnotationEvent.Operation.UPDATE
            )
            requestHandlingService.fireAnnotationEvent(annotationEvent)
        }

        render updateFeatureContainer
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
