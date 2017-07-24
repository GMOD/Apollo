package org.bbop.apollo

import edu.unc.genomics.SAMEntry
import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import htsjdk.samtools.AlignmentBlock
import htsjdk.samtools.BAMFileReader
import htsjdk.samtools.Cigar
import htsjdk.samtools.CigarElement
import htsjdk.samtools.SAMRecord
import htsjdk.samtools.SAMRecordIterator
import htsjdk.samtools.SamReader
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class BamService {


    /**
     * matching DraggableAlignments.js
     */
    @NotTransactional
    def processSequence(JSONArray featuresArray, String sequenceName, SamReader  bamFileReader, int start, int end) {
        SAMRecordIterator samRecordIterator= bamFileReader.query(sequenceName,start,end,false)

        Integer actualStart = start
        Integer actualStop = end


        for(SAMRecord samRecord in samRecordIterator){

            List<AlignmentBlock> alignmentBlocks = samRecord.alignmentBlocks
            println "cigar string: ${samRecord.cigarString}"
            Cigar cigar = samRecord.getCigar()
            println "cigar elements size: ${cigar.cigarElements.size()}"
            for(CigarElement cigarElement in cigar.cigarElements){
                println "cigar element ${cigarElement.toString()}"
            }
            println "alignment block size: ${alignmentBlocks.size()}"
            for(AlignmentBlock alignmentBlock in alignmentBlocks){
                JSONObject feature = new JSONObject()
                println "alignment block: ${alignmentBlock.toString()}"
                feature.start = alignmentBlock.readStart
                feature.end = alignmentBlock.readStart + alignmentBlock.length
                println "feature ${feature as JSON}"
//                featuresArray.add(feature)
            }
        }

//        int maxInView = 500
//        int stepSize = maxInView < (actualStop - actualStart) ? (actualStop - actualStart) / maxInView : 1
//
//
//        for (Integer i = actualStart; i < actualStop; i += stepSize) {
//            JSONObject globalFeature = new JSONObject()
//            globalFeature.put("start", i)
//            Integer endStep = i + stepSize
//            globalFeature.put("end", endStep)
//
//            if (i < values.length && values[i] < max && values[i] > min) {
//                // TODO: this should be th mean value
//                globalFeature.put("score", values[i])
//                featuresArray.add(globalFeature)
//            }
//        }
        return featuresArray
    }

    @NotTransactional
    def processProjection(JSONArray featuresArray, MultiSequenceProjection projection, SamReader bamFileReader , int start, int end) {

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

//        Map<Integer,Integer> lengthMap = new TreeMap<>()
//        for (ProjectionSequence projectionSequence in projection.sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
//            lengthMap.put(projectionSequence.order,projection.sequenceDiscontinuousProjectionMap.get(projectionSequence).length)
//            Collection<SAMEntry> samEntries = bamFileReader.query(projectionSequence.name,start,end)
////            realEnd += bamFileReader.getChrStop(projectionSequence.name)
//        }
//
//        Integer actualStart = start
//        Integer actualStop = end
//
//        Integer offset = 0
//        Integer order = 0
//        for (ProjectionSequence projectionSequence in projection.sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
//            // recalculate start and stop for each sequence
//            Integer calculatedStart = actualStart + offset
//            Integer calculatedStop = lengthMap.get(order)
//            Integer ratio = ((calculatedStop-calculatedStart) / ( (float) actualStop-actualStart)) / (float) maxInView
//            stepSize = maxInView < (calculatedStop - calculatedStart) ? (calculatedStop- calculatedStart) / maxInView : 1
//            calculateFeatureArray(featuresArray, calculatedStart, calculatedStop, stepSize, bamFileReader, projection, projectionSequence)
//
//            offset = lengthMap.get(order)+1
//            ++order
//        }
//        }
        return featuresArray

    }

    def calculateFeatureArray(JSONArray featuresArray, int actualStart, int actualStop, int stepSize, BAMFileReader bamFileReader, MultiSequenceProjection projection, ProjectionSequence projectionSequence) {
//        for (Integer i = actualStart; i < actualStop; i += stepSize) {
//            JSONObject globalFeature = new JSONObject()
//            Integer endStep = i + stepSize
//            globalFeature.put("start", i+projectionSequence.projectedOffset)
//            globalFeature.put("end", endStep+projectionSequence.projectedOffset)
//            Integer originalStart = projection.unProjectValue(i)
//            Integer originalEnd = projection.unProjectValue(endStep)
//            Collection<SAMEntry> samEntries = bamFileReader.query(projectionSequence.name, originalStart, originalEnd)
////            Integer value = innerContig.mean()
//////                ProjectionSequence startProjectionSequence = projection.getProjectionSequence(reverseStart)
//////                ProjectionSequence endProjectionSequence = projection.getProjectionSequence(reverseEnd)
//////                Integer value
//////                if(startProjectionSequence.name==endProjectionSequence.name){
//////                    edu.unc.genomics.Contig innerContig = bigWigFileReader.query(startProjectionSequence.name, reverseStart, reverseEnd)
//////                    value = innerContig.mean()
//////                }
//////                else{
//////                    Integer firstChromStop = bigWigFileReader.getChrStop(startProjectionSequence.name)
//////                    Integer lastChromStart = bigWigFileReader.getChrStart(endProjectionSequence.name)
//////                    edu.unc.genomics.Contig innerConti1 = bigWigFileReader.query(startProjectionSequence.name, reverseStart, firstChromStop)
//////                    edu.unc.genomics.Contig innerConti2 = bigWigFileReader.query(endProjectionSequence.name, lastChromStart, reverseEnd)
//////                    value = (innerConti1.total() + innerConti2.total()) / ((float) (innerConti1.actualNumberOfValues() + innerConti2.actualNumberOfValues()))
//////                }
////
////            if (value >= 0) {
//////                        // TODO: this should be th mean value
////                globalFeature.put("score", value)
////            } else {
////                globalFeature.put("score", 0)
////            }
//            featuresArray.add(globalFeature)
//        }
        return featuresArray
    }
}
