package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import htsjdk.samtools.BAMFileReader
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
            Long tmp = actualStart
            actualStart = actualEnd
            actualEnd = tmp
        }

        def samRecordList = samReader.query(sequenceName, actualStart, actualEnd, false).toList()
        for (SAMRecord samRecord in samRecordList.sort() { a, b -> a.start <=> b.start }) {
            JSONObject jsonObject = new JSONObject()

            // flag mappings:
            // FROM:
            // https://github.com/GMOD/jbrowse/blob/master/src/JBrowse/Store/SeqFeature/BAM/LazyFeature.js#L399-L412
            // TO:
            // https://github.com/broadinstitute/htsjdk/blob/master/src/main/java/htsjdk/samtools/SAMFlag.java#L34-L45

            jsonObject.name = samRecord.readName
            jsonObject.seq = samRecord.readString
            jsonObject.seq_length = samRecord.readString.length()
            jsonObject.seq_reverse_complemented = samRecord.readNegativeStrandFlag
            jsonObject.secondary_alignment = samRecord.notPrimaryAlignmentFlag
            jsonObject.supplementary_alignment = samRecord.supplementaryAlignmentFlag
            jsonObject.qc_failed = samRecord.readFailsVendorQualityCheckFlag
            jsonObject.duplicate = samRecord.duplicateReadFlag


            jsonObject.unmapped = samRecord.readUnmappedFlag
            jsonObject.multi_segment_template = samRecord.readPairedFlag


            if (!jsonObject.unmapped) {
                jsonObject.start = projection.projectValue(samRecord.start)
                jsonObject.end = projection.projectValue(samRecord.end)
                jsonObject.strand = samRecord.readNegativeStrandFlag ? -1 : 1 // I thikn this is correct
                jsonObject.score = samRecord.mappingQuality
                jsonObject.qual = samRecord.baseQualities.join(" ")

//                jsonObject.mq= samRecord.cigarString // ?
                jsonObject.cigar = samRecord.cigarString
                jsonObject.length_on_ref = samRecord.readLength

                jsonObject.type = "match"

                if (jsonObject.start > jsonObject.end) {
                    Long tmp = jsonObject.start
                    jsonObject.start = jsonObject.end
                    jsonObject.end = tmp
                    jsonObject.strand = -jsonObject.strand
                }

            }
            if (jsonObject.multi_segment_template) {
                jsonObject.multi_segment_all_correctly_aligned = samRecord.properPairFlag
                jsonObject.multi_segment_next_segment_unmapped = samRecord.mateUnmappedFlag
                jsonObject.multi_segment_next_segment_reversed = samRecord.mateNegativeStrandFlag
                jsonObject.multi_segment_first = samRecord.firstOfPairFlag
                jsonObject.multi_segment_last = samRecord.secondOfPairFlag

                // uses next_ref_id
//                jsonObject.next_segment_position= samRecord.secondOfPairFlag
            }

            // reverse strand moved to negative strand
            jsonObject.remove("class")


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

            jsonObject.subfeatures = calculateMatches(projection, samRecord)

            // TODO: add projection
            jsonObject.mismatches = calculateMismatches(samRecord)
            if (jsonObject.MD) {
                handleMdMismatch(jsonObject,samRecord)
            }

            featuresArray.add(jsonObject)
        }

        return featuresArray

    }

    def handleMdMismatch(JSONObject featureObject,SAMRecord samRecord) {

        JSONArray filteredMismatches = new JSONArray()
        featureObject.mismatches.each {
           if(  ! (it.type=='deletion' || it.type=='mismatch') ){
               filteredMismatches.add(it)
           }
        }
        return handleMdMismatch(featureObject,filteredMismatches,samRecord)
    }

    def handleMdMismatch(JSONObject featureObject,JSONArray mismatches,SAMRecord samRecord) {
        println "handling mismatch ${mismatches as JSON}"
        println "handling feature mismatch ${featureObject as JSON}"
        def mismatchRecords = new JSONArray();
        String mdString = featureObject.MD
        // match token as either

//        https://github.com/vsbuffalo/devnotes/wiki/The-MD-Tag-in-BAM-Files
//        https://github.com/GMOD/jbrowse/blob/master/src/JBrowse/Store/SeqFeature/_MismatchesMixin.js

        // if the mdString
//        mdString.findAll(/(\d+|\^[a-z]+|[a-z])/).each { token ->
//
//        }
//        JSONObject jsonObject = new JSONObject(
//                start: 0,
//                base: '',
//                length: 0,
//                type: 'mismatch'
//        )
        return mismatches
    }

    def calculateMatches(MultiSequenceProjection projection, SAMRecord samRecord) {
        // TODO: put in a separate method
        JSONArray subfeatsArray = new JSONArray()
        if (!samRecord.cigar) {
            return subfeatsArray
        }

        Integer min = samRecord.start
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
                    subFeature.strand = samRecord.readNegativeStrandFlag ? -1 : 1 // I thikn this is correct


                    subFeature.start = projection.projectValue(subFeature.start)
                    subFeature.end = projection.projectValue(subFeature.end)
                    if (subFeature.start > subFeature.end) {
                        Long tmp = subFeature.start
                        subFeature.start = subFeature.end
                        subFeature.end = tmp
                        subFeature.strand = -subFeature.strand
                    }


                    subFeature.cigar_op = samRecord.cigarString
                    subfeatsArray.add(subFeature)
                }
                min = max
            }

        }
        return subfeatsArray
    }

    def calculateMismatches(SAMRecord samRecord) {

        def cigarElements = samRecord.cigar.cigarElements
        JSONArray mismatches = new JSONArray()
        int currentOffset = 0

        for (CigarElement cigarElement in cigarElements) {
            switch (cigarElement.operator) {
                case CigarOperator.I:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "insertion"
                            , base: String.valueOf(cigarElement.length)
                            , length: 1
                    ))
                    break
                case CigarOperator.D:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "deletion"
                            , base: '*'
                            , length: cigarElement.length
                    ))
                    break
                case CigarOperator.N:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "skip"
                            , base: 'N'
                            , length: cigarElement.length
                    ))
                    break
                case CigarOperator.X:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "mismatch"
                            , base: 'N'
                            , length: cigarElement.length
                    ))
                    break
                case CigarOperator.H:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "hardclip"
                            , base: 'H' + cigarElement.length
                            , length: 1
                    ))
                    break
                case CigarOperator.S:
                    mismatches.add(new JSONObject(
                            start: currentOffset
                            , type: "softclip"
                            , base: 'S' + cigarElement.length
                            , cliplen: cigarElement.length
                            , length: 1
                    ))
                    break
            }

            switch (cigarElement.operator) {
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
