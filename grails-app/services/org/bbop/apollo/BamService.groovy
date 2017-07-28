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
    def processProjection(JSONArray featuresArray, MultiSequenceProjection projection, BAMFileReader samReader, int start, int end,File sourceFile) {

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
//        println "bam header ${header?.getTextHeader()}"
//        println "header properties ${header?.properties}"

        println "start / end ${start} / ${end}"
        println "iterator in bam service ${samRecordList.size()}"

//        while(samRecordIterator.hasNext()){
        for(SAMRecord samRecord in samRecordList){
//            println "TRY"
//            final SAMRecord rec = .next()
//            println "RECORD: ${samRecord.getSAMString()}"
            JSONObject jsonObject = new JSONObject()

            jsonObject.source = sourceFile.name



            jsonObject.start = projection.projectValue(samRecord.start)
            jsonObject.end = projection.projectValue(samRecord.end)
            if(jsonObject.start > jsonObject.end){
                Long tmp = jsonObject.start
                jsonObject.start = jsonObject.end
                jsonObject.end = tmp
            }

            jsonObject.name = samRecord.readName


            if(jsonObject.name=='ctgA_19043_19563_1:0:0_1:0:0_18a0'){
                samRecord.properties.each {
                    println "properly ${it.key} -> ${it.value}"
                }
                samRecord.attributes.each {
                    println "SAMRecord tag: ${it.tag} -> ${it.value}"
                }
            }


            jsonObject.cigar = samRecord.cigarString
            jsonObject.length_on_ref = samRecord.readLength


            samRecord.getAttributes().each { attribute ->
                if(attribute.value instanceof Character){
                    jsonObject[attribute.tag] = attribute.value.toString()
                }
                else{
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
            jsonObject.unmapped = samRecord.readUnmappedFlag
            jsonObject.duplicate = samRecord.duplicateReadFlag
            jsonObject.secondary_alignment = samRecord.secondaryOrSupplementary
            jsonObject.seq_reverse_complemented = samRecord.readNegativeStrandFlag // I thikn this is correct
            jsonObject.strand = samRecord.readNegativeStrandFlag ? -1 : 1 // I thikn this is correct


            jsonObject.remove("class")

            featuresArray.add(jsonObject)
        }

        return featuresArray

    }

}
