package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import htsjdk.samtools.*
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
    def processSequence(JSONArray featuresArray, String sequenceName, SamReader bamFileReader, int start, int end) {
        SAMRecordIterator samRecordIterator = bamFileReader.query(sequenceName, start, end, false)

        Integer actualStart = start
        Integer actualStop = end


        for (SAMRecord samRecord in samRecordIterator) {

            List<AlignmentBlock> alignmentBlocks = samRecord.alignmentBlocks
            println "cigar string: ${samRecord.cigarString}"
            Cigar cigar = samRecord.getCigar()
            println "cigar elements size: ${cigar.cigarElements.size()}"
            for (CigarElement cigarElement in cigar.cigarElements) {
                println "cigar element ${cigarElement.toString()}"
            }
            println "alignment block size: ${alignmentBlocks.size()}"
            for (AlignmentBlock alignmentBlock in alignmentBlocks) {
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
    def processProjection(JSONArray featuresArray, MultiSequenceProjection projection, SamReader samReader, int start, int end) {

        ProjectionSequence projectionSequence = projection.getProjectedSequences().first()
        String sequenceName = projectionSequence.name

        int actualStart = projection.unProjectValue(start)
        int actualEnd = projection.unProjectValue(end)
        if(actualStart > actualEnd){
            println "flipping"
            Long tmp = actualStart
            actualStart = actualEnd
            actualEnd = tmp
        }
//        SAMRecordIterator samRecordIterator = bamFileReader.query(sequenceName, start, end, false)
//        samReader.query()
        // TODO: pull from cache
        def samRecordList = samReader.query(sequenceName,actualStart,actualEnd,false).toList()
//        SAMRecordIterator samRecordIterator = samReader.iterator()

        final SAMFileHeader header = samReader.getFileHeader()
        println "bam header ${header?.getTextHeader()}"

        println "start / end ${start} / ${end}"
        println "iterator in bam service ${samRecordList.size()}"

//        while(samRecordIterator.hasNext()){
        for(SAMRecord samRecord in samRecordList){
//            println "TRY"
//            final SAMRecord rec = .next()
//            println "RECORD: ${samRecord.getSAMString()}"
            JSONObject jsonObject = new JSONObject()
            jsonObject.start = projection.projectValue(samRecord.start)
            jsonObject.end = projection.projectValue(samRecord.end)
            if(jsonObject.start > jsonObject.end){
                Long tmp = jsonObject.start
                jsonObject.start = jsonObject.end
                jsonObject.end = tmp
            }
            jsonObject.name = samRecord.header.SAMString
////                jsonObject.type = samRecord.get
////                jsonObject.position = samRecord.get // just reference sequence already there?
            jsonObject.cigar = samRecord.cigarString
            samRecord.getAttributes().each { attribute ->
                jsonObject[attribute.tag] = attribute.value
            }
            jsonObject.baseQualityString = samRecord.baseQualityString
            jsonObject.readSequence = samRecord.readString
            featuresArray.add(jsonObject)
        }

//        Integer actualStart = start
//        Integer actualStop = end


//        for (SAMRecord samRecord in samRecordIterator) {
//            List<AlignmentBlock> alignmentBlocks = samRecord.alignmentBlocks
//            println "cigar string: ${samRecord.cigarString}"
//            Cigar cigar = samRecord.getCigar()
//            println "cigar elements size: ${cigar.cigarElements.size()}"
//            for (CigarElement cigarElement in cigar.cigarElements) {
//                println "cigar element ${cigarElement.toString()}"
//            }
//            println "alignment block size: ${alignmentBlocks.size()}"
//            for (AlignmentBlock alignmentBlock in alignmentBlocks) {
//                JSONObject feature = new JSONObject()
//                println "alignment block: ${alignmentBlock.toString()}"
//                feature.start = alignmentBlock.readStart
//                feature.end = alignmentBlock.readStart + alignmentBlock.length
//                println "feature ${feature as JSON}"
////                featuresArray.add(feature)
//            }
//        }

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
//        println "feature array ${featuresArray as JSON}"

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
