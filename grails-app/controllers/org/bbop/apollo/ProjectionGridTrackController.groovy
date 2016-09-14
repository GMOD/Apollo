package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.ColorGenerator
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Defines REST track here
 * http://gmod.org/wiki/JBrowse_Configuration_Guide#JBrowse_REST_Feature_Store_API
 */
class ProjectionGridTrackController {

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
     * Gets the client token from the referring URL.  It makes the assumption that referer URL is always jbrowse
     * @param referer
     * @return
     */
    def extractClientToken(String referer){

        // referer is always jbrowse
        String rootUrl = createLink("uri":"/")
        int startClientIndex = referer.indexOf(rootUrl)+rootUrl.length()
        int endClientIndex = referer.indexOf("/jbrowse/")
        return referer.substring(startClientIndex,endClientIndex)
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
        sequenceObject.put(org.bbop.apollo.gwt.shared.FeatureStringEnum.CLIENT_TOKEN.value,extractClientToken(referer))
        Integer featureCount = sequenceObject.sequenceList.size()


        MultiSequenceProjection multiSequenceProjection = projectionService.getProjection(sequenceObject)
        Integer range = multiSequenceProjection.getLength()

        JSONObject jsonObject = new JSONObject()
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
        String referer = request.getHeader("Referer")
        Integer start = Integer.parseInt(params.start)
        Integer end = Integer.parseInt(params.end)
        String sequenceName = params.sequenceName
        Integer endIndex = sequenceName.indexOf("}:") + 1
        JSONObject sequenceObject = JSON.parse(sequenceName.substring(0, endIndex)) as JSONObject
        sequenceObject.put(org.bbop.apollo.gwt.shared.FeatureStringEnum.CLIENT_TOKEN.value,extractClientToken(referer))
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
        String referer = request.getHeader("Referer")

        Integer endIndex = sequenceName.indexOf("}:") + 1
        JSONObject sequenceObject = JSON.parse(sequenceName.substring(0, endIndex)) as JSONObject
        sequenceObject.put(org.bbop.apollo.gwt.shared.FeatureStringEnum.CLIENT_TOKEN.value,extractClientToken(referer))
        MultiSequenceProjection multiSequenceProjection = projectionService.getProjection(sequenceObject)

        JSONObject jsonObject = requestHandlingService.createJSONFeatureContainer()
        List<ProjectionSequence> projectionSequences = multiSequenceProjection.getReverseProjectionSequences(start, end)

        int range = end - start
        int stepsPerView = 10
        double buffer = 1d/stepsPerView
        int step = Math.round(buffer * range)
        println "STEP: ${step}"


        projectionSequences.each{ ProjectionSequence projectionSequence  ->

            println "projection: ${projectionSequence.toJSONObject() as JSON}"

           Integer index = projectionSequence.order
//        // TODO: show if
            JSONObject region = new JSONObject(
                    type: 'region',
                    start: projectionSequence.offset,
                    end: projectionSequence.length+ projectionSequence.offset,
                    name: projectionSequence.name,
                    label: projectionSequence.name,
                    color: ColorGenerator.getColorForIndex(index),
                    uniqueID: projectionSequence.name + sequenceObject.toString()+index,
                    sequence: projectionSequence.toJSONObject()
            )
            jsonObject.features.add(region)
            JSONObject regionRight = new JSONObject(
                    type: 'region-right',
                    start: projectionSequence.length + projectionSequence.offset,
                    end: projectionSequence.length + projectionSequence.offset,
                    name: projectionSequence.name,
                    label: projectionSequence.name,
                    color: ColorGenerator.getColorForIndex(index),
                    uniqueID: projectionSequence.name + sequenceObject.toString()+index,
//                    data: sequenceObject,
                    sequence: projectionSequence.toJSONObject()
            )
            jsonObject.features.add(regionRight)

            // TODO: remove if LegendTrack removed
//            JSONObject tickRight = new JSONObject(
//                    type: 'grid-right',
//                    start: projectionSequence.unprojectedLength + projectionSequence.offset,
//                    end: projectionSequence.unprojectedLength + projectionSequence.offset,
//                    name: projectionSequence.unprojectedLength + projectionSequence.offset,
//                    label: projectionSequence.unprojectedLength + projectionSequence.offset,
//                    color: getColorForIndex(index),
//                    uniqueID: (projectionSequence.unprojectedLength + projectionSequence.offset) + sequenceObject.toString(),
//                    data: sequenceObject
//            )
//            jsonObject.features.add(tickRight)
//            for(int i = projectionSequence.start ; i < projectionSequence.end ; i+=step){
//                int value = multiSequenceProjection.projectReverseValue(i+projectionSequence.offset)
//                JSONObject feature = new JSONObject(
//                        type: 'grid',
//                        start: i + projectionSequence.offset,
//                        end: i + projectionSequence.offset ,
//                        name: value,
//                        label: value,
//                        color: getColorForIndex(index),
//                        uniqueID: value + sequenceObject.toString(),
//                        data: sequenceObject
//                )
//                jsonObject.features.add(feature)
//            }
        }


        render jsonObject
    }
}
