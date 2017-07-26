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
    def processSequence(JSONArray featuresArray, String sequenceName, BAMFileReader samReader, int start, int end) {
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
        return featuresArray
    }

    @NotTransactional
    def processProjection(JSONArray featuresArray, MultiSequenceProjection projection, BAMFileReader samReader, int start, int end) {

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
//        println "ASDFQUERY"
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
            jsonObject.cigar = samRecord.cigarString
            samRecord.getAttributes().each { attribute ->
                jsonObject[attribute.tag] = attribute.value
            }
            jsonObject.baseQualityString = samRecord.baseQualityString
            jsonObject.readSequence = samRecord.readString
            featuresArray.add(jsonObject)
        }

        return featuresArray

    }

}
