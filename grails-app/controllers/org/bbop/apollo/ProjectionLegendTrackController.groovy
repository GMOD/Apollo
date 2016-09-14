package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Defines REST track here
 * http://gmod.org/wiki/JBrowse_Configuration_Guide#JBrowse_REST_Feature_Store_API
 */
class ProjectionLegendTrackController {

    def projectionService
    def trackService
    def requestHandlingService

    def index() {}

    /**
     *{"bins":  [ 51, 50, 58, 63, 57, 57, 65, 66, 63, 61,
     56, 49, 50, 47, 39, 38, 54, 41, 50, 71,
     61, 44, 64, 60, 42
     ],
     "stats": {"basesPerBin": 200,
     "max": 88}}* @return
     */
    def regionFeatureDensities() {
        println "regionFeatureDensities params: ${params}"
        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        render jsonObject
    }

    /**
     *{"featureDensity": 0.02,

     "featureCount": 234235,

     "scoreMin": 87,
     "scoreMax": 87,
     "scoreMean": 42,
     "scoreStdDev": 2.1}* @return
     */
    def statsGlobal() {
        println "stats global params: ${params}"
        String referer = request.getHeader("Referer")
        String refererLoc = trackService.extractLocation(referer)
        String sequenceName = refererLoc
        Integer endIndex = sequenceName.indexOf("}:") + 1

        JSONObject sequenceObject = JSON.parse(sequenceName.substring(0, endIndex)) as JSONObject
        Integer featureCount = sequenceObject.sequenceList.size()
        MultiSequenceProjection multiSequenceProjection = projectionService.getProjection(sequenceObject)
        Integer range = multiSequenceProjection.getLength()

        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        jsonObject.featureCount= featureCount
        jsonObject.featureDensity = featureCount / (range) * 1.0
//        jsonObject.featureCount=1
//        jsonObject.scoreMin=1
//        jsonObject.scoreMax=1
//        jsonObject.scoreMean=1
//        jsonObject.scoreStdDev=1
        render jsonObject
    }

    /**
     * Same as statsGlobal, but only for the region
     * @return
     */
    def statsRegion() {
        println "stats region params: ${params}"
        Integer start = Integer.parseInt(params.start)
        Integer end = Integer.parseInt(params.end)
        String sequenceName = params.sequenceName
        Integer endIndex = sequenceName.indexOf("}:") + 1
        JSONObject sequenceObject = JSON.parse(sequenceName.substring(0, endIndex)) as JSONObject
        MultiSequenceProjection multiSequenceProjection = projectionService.getProjection(sequenceObject)
        List<ProjectionSequence> projectionSequences = multiSequenceProjection.getReverseProjectionSequences(start, end)
        Integer featureCount = projectionSequences.size()

        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        jsonObject.featureCount= featureCount
        jsonObject.featureDensity = featureCount / (end - start) * 1.0
//        jsonObject.scoreMin=1
//        jsonObject.scoreMax=1
//        jsonObject.scoreMean=1
//        jsonObject.scoreStdDev=1
        render jsonObject
    }

    def features() {

        println "features params: ${params}"
        String sequenceName = params.sequenceName
        Integer start = Integer.parseInt(params.start)
        Integer end = Integer.parseInt(params.end)

        Integer endIndex = sequenceName.indexOf("}:") + 1
        JSONObject sequenceObject = JSON.parse(sequenceName.substring(0, endIndex)) as JSONObject
        MultiSequenceProjection multiSequenceProjection = projectionService.getProjection(sequenceObject)

        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        List<ProjectionSequence> projectionSequences = multiSequenceProjection.getReverseProjectionSequences(start, end)
        projectionSequences.each { ProjectionSequence projectionSequence ->
//        // TODO: show if
//            def thisStart = start < projectionSequence.start + offset ? start : projectionSequence.start + offset
//            def thisEnd = end > projectionSequence.end+ offset ? end : projectionSequence.end + offset
//            def thisStart = start > projectionSequence.start + offset ? start : projectionSequence.start + offset
//            def thisEnd = end < projectionSequence.end+ offset ? end : projectionSequence.end + offset
            JSONObject feature = new JSONObject(
                    start: projectionSequence.start + projectionSequence.offset,
                    end: projectionSequence.end + + projectionSequence.offset ,
                    name: projectionSequence.name,
                    type: "exon",
                    label: projectionSequence.name,
                    uniqueID: projectionSequence.name + sequenceObject.toString() + projectionSequence.order,
                    order: projectionSequence.order
            )
            jsonObject.features.add(feature)
        }


        render jsonObject
    }
}
