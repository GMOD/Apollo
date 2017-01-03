package org.bbop.apollo

import grails.converters.JSON
import groovy.json.JsonBuilder
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class VariantEffectTrackController {

    def features() {
        String sequenceName = params.id
        println params
        if (params.format && params.format != "" && params.format != " ") {
            sequenceName = sequenceName + "." + params.format
        }
        Sequence sequence = Sequence.findByName(sequenceName)
        Integer start = Integer.parseInt(params.start)
        Integer end = Integer.parseInt(params.end)
        def features = SequenceAlteration.executeQuery(
                "SELECT DISTINCT sa FROM SequenceAlteration sa JOIN sa.featureLocations fl WHERE fl.fmin >=:queryFmin AND fl.fmax <=:queryFmax AND fl.sequence =:querySequence AND sa.alterationType =:queryAlterationType",
                [queryFmin: start, queryFmax: end, querySequence: sequence, queryAlterationType: FeatureStringEnum.VARIANT.value])

        JSONObject returnJson = new JSONObject()
        returnJson.features = new JSONArray()
        for (SequenceAlteration feature : features) {
            for (Allele allele : feature.alternateAlleles) {
                for (VariantEffect variantEffect : allele.variantEffects) {
                    if (variantEffect.metadata) {
                        JSONObject transcriptJsonObject = JSON.parse(variantEffect.metadata) as JSONObject
                        JSONObject newJsonObject = new JSONObject()
                        newJsonObject.uniqueId = transcriptJsonObject.get(FeatureStringEnum.UNIQUENAME.value)
                        newJsonObject.start = transcriptJsonObject.getJSONObject(FeatureStringEnum.LOCATION.value).get(FeatureStringEnum.FMIN.value)
                        newJsonObject.end = transcriptJsonObject.getJSONObject(FeatureStringEnum.LOCATION.value).get(FeatureStringEnum.FMAX.value)
                        newJsonObject.ref = transcriptJsonObject.get(FeatureStringEnum.SEQUENCE.value)
                        newJsonObject.strand = transcriptJsonObject.getJSONObject(FeatureStringEnum.LOCATION.value).get(FeatureStringEnum.STRAND.value)
                        newJsonObject.name = transcriptJsonObject.get(FeatureStringEnum.NAME.value)
                        newJsonObject.type = transcriptJsonObject.getJSONObject(FeatureStringEnum.TYPE.value).get(FeatureStringEnum.NAME.value)
                        newJsonObject.description = "Variant Effect of ${feature.name} on ${transcriptJsonObject.get(FeatureStringEnum.NAME.value)}"
                        newJsonObject.subfeatures = new JSONArray()
                        for (JSONObject eachChildObject : transcriptJsonObject.getJSONArray(FeatureStringEnum.CHILDREN.value)) {
                            JSONObject newChildJsonObject = new JSONObject()
                            newChildJsonObject.type = eachChildObject.getJSONObject(FeatureStringEnum.TYPE.value).get(FeatureStringEnum.NAME.value)
                            newChildJsonObject.start = eachChildObject.getJSONObject(FeatureStringEnum.LOCATION.value).get(FeatureStringEnum.FMIN.value)
                            newChildJsonObject.end = eachChildObject.getJSONObject(FeatureStringEnum.LOCATION.value).get(FeatureStringEnum.FMAX.value)
                            newChildJsonObject.strand = eachChildObject.getJSONObject(FeatureStringEnum.LOCATION.value).get(FeatureStringEnum.STRAND.value)
                            newJsonObject.getJSONArray("subfeatures").add(newChildJsonObject)
                        }
                        returnJson.getJSONArray("features").add(newJsonObject)
                    }
                }
            }
        }

        println "VariantEffectTrack::features() returning with JSONObject: ${returnJson.toString()}"
        render returnJson.toString()
    }

    def globalStats() {
        render ([] as JSON)
    }

}
