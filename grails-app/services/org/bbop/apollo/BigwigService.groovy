package org.bbop.apollo

import edu.unc.genomics.Interval
import edu.unc.genomics.io.BigWigFileReader
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
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
            globalFeature.put(FeatureStringEnum.START.value, i)
            Integer endStep = i + stepSize
            globalFeature.put(FeatureStringEnum.END.value, endStep)

            if (i < values.length && values[i] < max && values[i] > min) {
                globalFeature.put(FeatureStringEnum.SCORE.value, values[i])
                featuresArray.add(globalFeature)
            }
        }
        return featuresArray
    }

    @NotTransactional
    def processProjection(JSONArray featuresArray, MultiSequenceProjection projection, BigWigFileReader bigWigFileReader, int start, int end) {
        Integer actualStart = start
        Integer actualStop = end
        int maxInView = 500

        int stepSize = maxInView < (actualStop - actualStart) ? (actualStop - actualStart) / maxInView : 1


        Integer offset = 0
        Integer order = 0
        for (ProjectionSequence projectionSequence in projection.sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            // recalculate start and stop for each sequence
            Integer calculatedStart = start < offset ? 0 : start + offset
            Integer calculatedStop = end
            calculateFeatureArray(featuresArray, calculatedStart, calculatedStop, stepSize, bigWigFileReader, projection, projectionSequence)
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
        float[] values = innerContig.values
        for (Integer i = actualStart; i < actualStop; i += stepSize) {
            JSONObject globalFeature = new JSONObject()
            globalFeature.put(FeatureStringEnum.START.value, i + projectionSequence.projectedOffset)
            Integer endStep = i + stepSize
            globalFeature.put(FeatureStringEnum.END.value, endStep + projectionSequence.projectedOffset)
            Integer projectedIncrement = projection.unProjectValue(i)

            if (projectedIncrement < values.length && values[projectedIncrement] < max && values[projectedIncrement] > min) {
                globalFeature.put(FeatureStringEnum.SCORE.value, values[projectedIncrement])
                featuresArray.add(globalFeature)
            }
        }
        return featuresArray
    }
}
