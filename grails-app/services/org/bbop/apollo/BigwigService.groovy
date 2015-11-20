package org.bbop.apollo

import edu.unc.genomics.io.BigWigFileReader
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.projection.DiscontinuousProjection
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionChunk
import org.bbop.apollo.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import edu.unc.genomics.Interval
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

        int maxInView = 500

        Integer realStart = 0
        Integer realEnd = 0
        int stepSize = 1

//        for (ProjectionChunk projectionChunk in projection.projectionChunkList.projectionChunkList) {
//            realEnd += bigWigFileReader.getChrStop(projectionChunk.sequence)
//        }
//
//        for (ProjectionSequence projectionSequence in projection.sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
//            realEnd += bigWigFileReader.getChrStop(projectionSequence.name)
//        }

        Map<Integer,Integer> lengthMap = new TreeMap<>()
        for (ProjectionSequence projectionSequence in projection.sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            lengthMap.put(projectionSequence.order,projection.sequenceDiscontinuousProjectionMap.get(projectionSequence).length)
            realEnd += bigWigFileReader.getChrStop(projectionSequence.name)
        }

        Integer actualStart = start
        Integer actualStop = end

        Integer offset = 0
        Integer order = 0
        for (ProjectionSequence projectionSequence in projection.sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            // recalculate start and stop for each sequence
            Integer calculatedStart = actualStart + offset
            Integer calculatedStop = lengthMap.get(order)
            Integer ratio = ((calculatedStop-calculatedStart) / ( (float) actualStop-actualStart)) / (float) maxInView
            stepSize = maxInView < (calculatedStop - calculatedStart) ? (calculatedStop- calculatedStart) / maxInView : 1
            calculateFeatureArray(featuresArray, calculatedStart, calculatedStop, stepSize, bigWigFileReader, projection, projectionSequence)

            offset = lengthMap.get(order)+1
            ++order
        }
//        }
        return featuresArray

    }

    def calculateFeatureArray(JSONArray featuresArray, int actualStart, int actualStop, int stepSize, BigWigFileReader bigWigFileReader, MultiSequenceProjection projection, ProjectionSequence projectionSequence) {
        for (Integer i = actualStart; i < actualStop; i += stepSize) {
            JSONObject globalFeature = new JSONObject()
            Integer endStep = i + stepSize
            globalFeature.put("start", i+projectionSequence.offset)
            globalFeature.put("end", endStep+projectionSequence.offset)
            Integer reverseStart = projection.projectReverseValue(i)
            Integer reverseEnd = projection.projectReverseValue(endStep)
            edu.unc.genomics.Contig innerContig = bigWigFileReader.query(projectionSequence.name, reverseStart, reverseEnd)
            Integer value = innerContig.mean()
//                ProjectionSequence startProjectionSequence = projection.getProjectionSequence(reverseStart)
//                ProjectionSequence endProjectionSequence = projection.getProjectionSequence(reverseEnd)
//                Integer value
//                if(startProjectionSequence.name==endProjectionSequence.name){
//                    edu.unc.genomics.Contig innerContig = bigWigFileReader.query(startProjectionSequence.name, reverseStart, reverseEnd)
//                    value = innerContig.mean()
//                }
//                else{
//                    Integer firstChromStop = bigWigFileReader.getChrStop(startProjectionSequence.name)
//                    Integer lastChromStart = bigWigFileReader.getChrStart(endProjectionSequence.name)
//                    edu.unc.genomics.Contig innerConti1 = bigWigFileReader.query(startProjectionSequence.name, reverseStart, firstChromStop)
//                    edu.unc.genomics.Contig innerConti2 = bigWigFileReader.query(endProjectionSequence.name, lastChromStart, reverseEnd)
//                    value = (innerConti1.total() + innerConti2.total()) / ((float) (innerConti1.actualNumberOfValues() + innerConti2.actualNumberOfValues()))
//                }

            if (value >= 0) {
//                        // TODO: this should be th mean value
                globalFeature.put("score", value)
            } else {
                globalFeature.put("score", 0)
            }
            featuresArray.add(globalFeature)
        }
    }
}
