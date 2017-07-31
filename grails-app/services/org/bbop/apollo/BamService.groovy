package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional
import htsjdk.samtools.BAMFileReader
import htsjdk.samtools.Cigar
import htsjdk.samtools.CigarElement
import htsjdk.samtools.CigarOperator
import htsjdk.samtools.SAMRecord
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
    def processProjection(JSONArray featuresArray, MultiSequenceProjection projection, BAMFileReader samReader, int start, int end, File sourceFile) {

        ProjectionSequence projectionSequence = projection.getProjectedSequences().first()
        String sequenceName = projectionSequence.name

        int actualStart = projection.unProjectValue(start)
        int actualEnd = projection.unProjectValue(end)
        if (actualStart > actualEnd) {
            println "flipping"
            Long tmp = actualStart
            actualStart = actualEnd
            actualEnd = tmp
        }

//        SAMRecordIterator samRecordIterator = bamFileReader.query(sequenceName, start, end, false)
//        samReader.query()
        // TODO: pull from cache
        def samRecordList = samReader.query(sequenceName, actualStart, actualEnd, false).toList()
//        println "ASDFQUERY"
//        SAMRecordIterator samRecordIterator = samReader.iterator()

//        final SAMFileHeader header = samReader.getFileHeader()
//        println "bam header ${header?.getTextHeader()}"
//        println "header properties ${header?.properties}"

//        println "start / end ${start} / ${end}"
//        println "iterator in bam service ${samRecordList.size()}"

//        while(samRecordIterator.hasNext()){
        for (SAMRecord samRecord in samRecordList.sort() { a, b -> a.start <=> b.start }) {
//            println "TRY"
//            final SAMRecord rec = .next()
//            println "RECORD: ${samRecord.getSAMString()}"
            JSONObject jsonObject = new JSONObject()

            jsonObject.source = sourceFile.name



            jsonObject.start = projection.projectValue(samRecord.start)
            jsonObject.end = projection.projectValue(samRecord.end)
            if (jsonObject.start > jsonObject.end) {
                Long tmp = jsonObject.start
                jsonObject.start = jsonObject.end
                jsonObject.end = tmp
            }

            jsonObject.name = samRecord.readName
            jsonObject.qc_failed = samRecord.readFailsVendorQualityCheckFlag
//            println "getting TLEN"
//            jsonObject.multi_segment = samRecord.alignmentBlocks?.size() > 1

//            jsonObject.template_length = samRecord.cigarLength
//            for (block in samRecord.alignmentBlocks) {
////                jsonObject.template_length = samRecord.alignmentBlocks?.size()
//                jsonObject.template_length = samRecord.mateAlignmentStart
//            }
//            println "GOT TLEN"


            if (jsonObject.name == 'ctgA_19043_19563_1:0:0_1:0:0_18a0') {
                samRecord.properties.each {
                    println "properly ${it.key} -> ${it.value}"
                }
                samRecord.attributes.each {
                    println "SAMRecord tag: ${it.tag} -> ${it.value}"
                }
            }


            jsonObject.length_on_ref = samRecord.readLength


            samRecord.getAttributes().each { attribute ->
                if (attribute.value instanceof Character) {
                    jsonObject[attribute.tag] = attribute.value.toString()
                } else {
                    jsonObject[attribute.tag] = attribute.value
                }
//                switch (attribute.tag){
//                    case 'XT':
//                        jsonObject[attribute.tag] = attribute.value.toString()
//                        break
//                    default:
//                        jsonObject[attribute.tag] = attribute.value
//                }
            }
            jsonObject.qual = samRecord.baseQualities.join(" ")
            jsonObject.seq = samRecord.readString

            jsonObject.score = samRecord.mappingQuality
            jsonObject.type = "match"

            jsonObject.seq_length = samRecord.readString.length()
            jsonObject.duplicate = samRecord.duplicateReadFlag
            jsonObject.unmapped = samRecord.readUnmappedFlag
            jsonObject.secondary_alignment = samRecord.secondaryOrSupplementary
            jsonObject.seq_reverse_complemented = samRecord.readNegativeStrandFlag // I thikn this is correct
            jsonObject.strand = samRecord.readNegativeStrandFlag ? -1 : 1 // I thikn this is correct


            jsonObject.remove("class")


            jsonObject.cigar = samRecord.cigarString
//
            JSONArray subfeatsArray = new JSONArray()
//            // should turn elements into array, but
//            // e.g., 100M should be [100,M]
////            for(AlignmentBlock alignmentBlock in samRecord.alignmentBlocks){
//////                alignmentBlock.
////                JSONObject subFeature = new JSONObject()
//////                subFeature.type = cigarElement.operator.toString()
////                subFeature.start = alignmentBlock.readStart
////                subFeature.end = alignmentBlock.readStart + alignmentBlock.length
////                subFeature.strand = jsonObject.strand
//////                subFeature.cigar_op = samRecord.cigarString
////                subfeatsArray.add(subFeature)
////            }
            jsonObject.mismatches = calculateMismatches(samRecord.cigar)
            Integer min = jsonObject.start
            Integer max = null
//            println "cigar string ${samRecord.cigarString}"
            for (CigarElement cigarElement in samRecord.cigar.cigarElements) {
//                println "OP: '${cigarElement.operator.toString()}'"
                // parse matches for subfeatures
                switch (cigarElement.operator) {
                    case CigarOperator.M:
                    case CigarOperator.D:
                    case CigarOperator.N:
                    case CigarOperator.EQ:
                    case CigarOperator.X:
                        max = min + cigarElement.length
                        break
                    case CigarOperator.I:
                        max = min
                        break
                    case CigarOperator.P:
                    case CigarOperator.H:
                    case CigarOperator.S:
                        break
                }

                if (max) {
                    if (cigarElement.operator != CigarOperator.N) {
                        JSONObject subFeature = new JSONObject()
                        subFeature.type = cigarElement.operator.toString()
                        subFeature.start = min
                        subFeature.end = max
                        subFeature.strand = jsonObject.strand
                        subFeature.cigar_op = samRecord.cigarString
                        subfeatsArray.add(subFeature)
                    }
                    min = max
                }

            }

            // get the

            jsonObject.subfeatures = subfeatsArray


            featuresArray.add(jsonObject)
        }

        return featuresArray

    }

    def calculateMismatches(Cigar cigarElements) {

        JSONArray mismatches = new JSONArray()
        int currentOffset = 0

        for (CigarElement cigarElement in cigarElements) {
            switch (cigarElement.operator){
                case CigarOperator.I:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            ,type: "insertion"
                            ,base: String.valueOf(cigarElement.length)
                            ,length: 1
                    ))
                    break
                case CigarOperator.D:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            ,type: "deletion"
                            ,base: '*'
                            ,length: cigarElement.length
                    ))
                    break
                case CigarOperator.N:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            ,type: "skip"
                            ,base: 'N'
                            ,length: cigarElement.length
                    ))
                    break
                case CigarOperator.X:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            ,type: "mismatch"
                            ,base: 'N'
                            ,length: cigarElement.length
                    ))
                    break
                case CigarOperator.H:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            ,type: "hardclip"
                            ,base: 'H'+cigarElement.length
                            ,length: 1
                    ))
                    break
                case CigarOperator.S:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            ,type: "softclip"
                            ,base: 'S'+cigarElement.length
                            ,cliplen: cigarElement.length
                            ,length: 1
                    ))
                    break
            }

            switch (cigarElement.operator){
                case CigarOperator.I:
                case CigarOperator.S:
                case CigarOperator.H:
                    break
                default:
                    currentOffset += cigarElement.length
            }

        }

        return mismatches
    }
}
