package org.bbop.apollo

import edu.unc.genomics.Interval
import edu.unc.genomics.io.BigWigFileReader
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class BigwigService {


    @NotTransactional
    def processSequence(JSONArray featuresArray, String sequenceName, BigWigFileReader bigWigFileReader, int start, int end) {
        Integer chrStart = bigWigFileReader.getChrStart(sequenceName)
        Integer chrStop = bigWigFileReader.getChrStop(sequenceName)

        double mean = bigWigFileReader.mean()
        double max = bigWigFileReader.max()
        double min = bigWigFileReader.min()

        Integer actualStart = start
        Integer actualStop = end
        int maxInView = 500

        int stepSize = maxInView < (actualStop - actualStart) ? (actualStop - actualStart) / maxInView : 1

        Interval interval = new Interval(sequenceName, chrStart, chrStop)
//        Interval interval = new Interval(sequenceName, start, end)
        edu.unc.genomics.Contig contig = bigWigFileReader.query(interval)
        float[] values = contig.values

        for (Integer i = actualStart; i < actualStop; i += stepSize) {
            JSONObject globalFeature = new JSONObject()
            globalFeature.put("start", i)
            Integer endStep = i + stepSize
            globalFeature.put("end", endStep)

            if (i < values.length && values[i] < max && values[i] > min) {
                // TODO: this should be th mean value
                globalFeature.put("score", values[i])
                featuresArray.add(globalFeature)
            }
        }
        return featuresArray
    }

    @NotTransactional
    def processProjection(JSONArray featuresArray, MultiSequenceProjection projection, BigWigFileReader bigWigFileReader, int start, int end) {

        // TODO: to support assemblage
//        Integer realEnd = 0
//        Map<Integer, Long> lengthMap = new TreeMap<>()
//        for (ProjectionSequence projectionSequence in projection.sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
//            lengthMap.put(projectionSequence.order, projection.sequenceDiscontinuousProjectionMap.get(projectionSequence).length)
//            println "start: " + bigWigFileReader.getChrStart(projectionSequence.name)
//            realEnd += bigWigFileReader.getChrStop(projectionSequence.name)
//        }

        Integer actualStart = start
        Integer actualStop = end
        int maxInView = 500

        int stepSize = maxInView < (actualStop - actualStart) ? (actualStop - actualStart) / maxInView : 1


        Integer offset = 0
        Integer order = 0
        for (ProjectionSequence projectionSequence in projection.sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            // recalculate start and stop for each sequence
            Integer calculatedStart = start < offset ? 0 : start + offset
//            Integer calculatedStop = end > projectionSequence.length + offset ? lengthMap.get(order) : end
            Integer calculatedStop = end
//            Integer stepSize = maxInView < (calculatedStop - calculatedStart) ? (calculatedStop - calculatedStart) / maxInView : 1
            calculateFeatureArray(featuresArray, calculatedStart, calculatedStop, stepSize, bigWigFileReader, projection, projectionSequence)
//            offset = lengthMap.get(order) + 1
            ++order
        }
        return featuresArray
    }

    def calculateFeatureArray(JSONArray featuresArray, int actualStart, int actualStop, int stepSize, BigWigFileReader bigWigFileReader, MultiSequenceProjection projection, ProjectionSequence projectionSequence) {
        Integer chrStart = bigWigFileReader.getChrStart(projectionSequence.name)
        Integer chrStop = bigWigFileReader.getChrStop(projectionSequence.name)
        double min = bigWigFileReader.min()
        double max = bigWigFileReader.max()
        Interval interval = new Interval(projectionSequence.name, chrStart,chrStop)
        edu.unc.genomics.Contig innerContig = bigWigFileReader.query(interval)
//        edu.unc.genomics.Contig innerContig = bigWigFileReader.query(projectionSequence.name, actualStart, actualStop)
        float[] values = innerContig.values
        for (Integer i = actualStart; i < actualStop; i += stepSize) {
            JSONObject globalFeature = new JSONObject()
            globalFeature.put("start", i + projectionSequence.projectedOffset)
            Integer endStep = i + stepSize
            globalFeature.put("end", endStep + projectionSequence.projectedOffset)
//            Integer originalStart = projection.unProjectValue(i)
//            Integer originalEnd = projection.unProjectValue(endStep)
            Integer projectedIncrement = projection.unProjectValue(i)

            if (projectedIncrement < values.length && values[projectedIncrement] < max && values[projectedIncrement] > min) {
                // TODO: this should be th mean value
                globalFeature.put("score", values[projectedIncrement])
                featuresArray.add(globalFeature)
            }
        }
        return featuresArray
    }
}
