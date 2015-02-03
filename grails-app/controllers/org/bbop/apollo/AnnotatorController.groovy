package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.gwt.shared.SharedStuff
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

class AnnotatorController {

    def featureService
    def requestHandlingService

    def index() {
        String uuid = UUID.randomUUID().toString()
        Organism.all.each {
            println it.commonName
        }
        [userKey:uuid]
    }

    def demo() {
    }

    /**
     * updates shallow properties of gene / feature
     * @return
     */
    @Transactional
    def updateFeature() {
        println "updating feature ${params.data}"
        def data = JSON.parse(params.data.toString()) as JSONObject
        println "uqnieuname 2: ${data.uniquename}"
        println "rendered data ${data as JSON}"
        Feature feature = Feature.findByUniqueName(data.uniquename)
        println "foiund feature: "+feature

        feature.name = data.name
        feature.symbol = data.symbol
        feature.description = data.description

        feature.save(flush: true, failOnError: true)

        println "saved!! "

        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        if (feature instanceof Gene) {
            List<Feature> childFeatures = feature.parentFeatureRelationships*.childFeature
            for (childFeature in childFeatures) {
                JSONObject jsonFeature = featureService.convertFeatureToJSON(childFeature, false)
                updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature)
            }
        } else {
            JSONObject jsonFeature = featureService.convertFeatureToJSON(feature, false)
            updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature)
        }

        Sequence sequence = feature?.featureLocation?.sequence

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: updateFeatureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
                , sequenceAlterationEvent: false
        )
        requestHandlingService.fireAnnotationEvent(annotationEvent)

        render updateFeatureContainer
    }


    def updateFeatureLocation() {
        println "updating exon ${params.data}"
        def data = JSON.parse(params.data.toString()) as JSONObject
        println "uqnieuname 2: ${data.uniquename}"
        println "rendered data ${data as JSON}"
        Feature exon = Feature.findByUniqueName(data.uniquename)
        exon.featureLocation.fmin = data.fmin
        exon.featureLocation.fmax = data.fmax
        exon.featureLocation.strand = data.strand
        exon.save(flush: true, failOnError: true)

        // need to grant the parent feature to force a redraw
        Feature parentFeature = exon.childFeatureRelationships*.parentFeature.first()

        JSONObject jsonFeature = featureService.convertFeatureToJSON(parentFeature, false)
        JSONObject updateFeatureContainer = createJSONFeatureContainer();
        updateFeatureContainer.getJSONArray(FeatureStringEnum.FEATURES.value).put(jsonFeature)

        Sequence sequence = exon?.featureLocation?.sequence
        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: updateFeatureContainer
                , sequence: sequence
                , operation: AnnotationEvent.Operation.UPDATE
                , sequenceAlterationEvent: false
        )
        requestHandlingService.fireAnnotationEvent(annotationEvent)

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

    def findAnnotationsForSequence(String sequenceName) {
        JSONObject returnObject = createJSONFeatureContainer()

        List<Feature> allFeatures = Feature.executeQuery("select f from Feature f join f.featureLocations fl join fl.sequence s where s.name = :sequenceName",[sequenceName: sequenceName])

        // just the genes
        def topLevelFeatureList = allFeatures.findAll() {
            it?.childFeatureRelationships?.size() == 0
        }

        for (Feature feature in topLevelFeatureList) {
            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(feature, false));
        }

        // TODO: do checks here

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
