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

        int maxInView = 1000
        Integer realEnd = 0

        Map<Integer, Integer> lengthMap = new TreeMap<>()
        for (ProjectionSequence projectionSequence in projection.sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            lengthMap.put(projectionSequence.order, projection.sequenceDiscontinuousProjectionMap.get(projectionSequence).length)
            realEnd += bigWigFileReader.getChrStop(projectionSequence.name)
        }

        Integer offset = 0
        Integer order = 0
        for (ProjectionSequence projectionSequence in projection.sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            // recalculate start and stop for each sequence
            Integer calculatedStart = start < offset ? 0 : start + offset
            Integer calculatedStop = end > projectionSequence.length + offset ? lengthMap.get(order) : end
            Integer stepSize = maxInView < (calculatedStop - calculatedStart) ? (calculatedStop - calculatedStart) / maxInView : 1
            calculateFeatureArray(featuresArray, calculatedStart, calculatedStop, stepSize, bigWigFileReader, projection, projectionSequence)
            offset = lengthMap.get(order) + 1
            ++order
        }
        return featuresArray
    }

    def calculateFeatureArray(JSONArray featuresArray, int actualStart, int actualStop, int stepSize, BigWigFileReader bigWigFileReader, MultiSequenceProjection projection, ProjectionSequence projectionSequence) {
        for (Integer i = actualStart; i < actualStop; i += stepSize) {
            JSONObject globalFeature = new JSONObject()
            Integer endStep = i + stepSize
            globalFeature.put("start", i + projectionSequence.projectedOffset)
            globalFeature.put("end", endStep + projectionSequence.projectedOffset)
            Integer originalStart = projection.unProjectValue(i)
            Integer originalEnd = projection.unProjectValue(endStep)
            edu.unc.genomics.Contig innerContig = bigWigFileReader.query(projectionSequence.name, originalStart, originalEnd)
            Integer value = innerContig.mean()

            globalFeature.put("score", value >= 1 ? value : 1)
            featuresArray.add(globalFeature)
        }
    }
}
