package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.Coordinate
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class RefSeqProjectorService {

    def projectionService
    def sequenceService

    @NotTransactional
    String projectRefSeq(JSONArray refSeqJsonObject, MultiSequenceProjection projection, Organism currentOrganism, String refererLoc) {

        if (projection) {
            JSONArray projectedArray = new JSONArray()
            // in this case, we just want a single object
            JSONObject sequenceObject = refSeqJsonObject ? refSeqJsonObject.first(): new JSONObject()

            sequenceObject.start = 0
            sequenceObject.end =  projection.length
            sequenceObject.length =  projection.length
            sequenceObject.name = refererLoc

//            for (int i = 0; i < refSeqJsonObject.size(); i++) {
//
//                JSONObject sequenceValue = refSeqJsonObject.getJSONObject(i)
//
//                String sequenceName = sequenceValue.getString("name")
//                if (projection && projection.containsSequence(sequenceName, sequenceValue.id, currentOrganism)) {
//                    Integer projectedSequenceLength = projection.findProjectSequenceLength(sequenceName)
//                    sequenceValue.put("length", projectedSequenceLength)
//                    sequenceValue.put("end", projectedSequenceLength)
//                    sequenceValue.put("start", 0)
//                    sequenceValue.put("name", refererLoc)
//                    projectedArray = mergeRefseqProjections(projectedArray, sequenceValue)
//                }
//            }
            projectedArray.add(sequenceObject)
            return projectedArray.toString()
        }
        else {
            return refSeqJsonObject.toString()
        }
    }

    /**
     [{"seqChunkSize": 20000, "start": 0,
     "name": "{\"projection\":\"None\", \"padding\":50, \"referenceTrack\":\"Official Gene Set v3.2\", \"sequences\":[{\"name\":\"Group5.7\"},{\"name\":\"Group9.2\"}]}",
     "length": 471578, "end": 471578},
     *{"seqChunkSize": 20000, "start": 0,
     "name": "{\"projection\":\"None\", \"padding\":50, \"referenceTrack\":\"Official Gene Set v3.2\", \"sequences\":[{\"name\":\"Group5.7\"},{\"name\":\"Group9.2\"}]}",
     "length": 456816, "end": 456816}]
     *
     *  Ignore chunkSize, start, name
     *
     *  sum: length / end
     *
     * @param jsonArray
     * @param refSeq JSONObject to add
     * @return
     */
    private static JSONArray mergeRefseqProjections(JSONArray projectedArray, JSONObject refSeq) {
        if (projectedArray.size() == 0) {
            projectedArray.add(refSeq)
        } else if (projectedArray.size() == 1) {
            JSONObject existingObject = projectedArray.getJSONObject(0)
            Integer existingLength = existingObject.getInt("length")
            Integer nextLength = refSeq.getInt("length")

            existingObject.put("length", existingLength + nextLength)
            existingObject.put("end", existingLength + nextLength)
        } else {
            throw new RuntimeException("wrong number of input projected arrays ${projectedArray?.size()}: ${projectedArray as JSON} . .. ${refSeq as JSON}")
        }
        return projectedArray
    }

    @Transactional
    String projectSequence(String dataFileName, Organism currentOrganism) {
        // Set content size
        // fileName
//                Group1.22-18.txt
        String putativeSequencePathName = sequenceService.getSequencePathName(dataFileName)
        JSONObject projectionSequenceObject = (JSONObject) JSON.parse(putativeSequencePathName)
        if (!projectionSequenceObject.organism) {
            projectionSequenceObject.organism = currentOrganism.commonName
        }

        JSONArray sequenceArray = projectionSequenceObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value)
        List<Sequence> sequenceStrings = new ArrayList<>()
        for (int i = 0; i < sequenceArray.size(); i++) {
            JSONObject sequenceObject = sequenceArray.getJSONObject(i)
            sequenceStrings.add(Sequence.findByName(sequenceObject.name))
        }

        MultiSequenceProjection projection = projectionService.getProjection(projectionSequenceObject)
        String chunkFileName = sequenceService.getChunkSuffix(dataFileName)
        Integer chunkNumber = Integer.parseInt(chunkFileName.substring(1, chunkFileName.indexOf(".txt")))
//                String sequenceDirectory = dataDirectory + "/seq"

        // so based on the "chunk" we need to retrieve the appropriate data set and then re-project it.
        // for this sequence that chunk that starts with "7" for that sequence would correspond to (and most likely be more than one file):

        // TODO: should do this for each, but for now this is okay
        Integer chunkSize = sequenceStrings.first().seqChunkSize
        Integer projectedStart = chunkSize * chunkNumber
        Integer projectedEnd = chunkSize * (chunkNumber + 1) // this an exclusive end

        // determine the current "offsets" based on the chunk
        Coordinate unprojectedCoordinate = projection.projectReverseCoordinate(projectedStart, projectedEnd)

        // if it projects off the edge of known space .  . we just take it to the maximum in the projection realm . . .
        if (unprojectedCoordinate.max < 0) {
            unprojectedCoordinate.max = projection.getMaxCoordinate().max + 1
        }

        if (!unprojectedCoordinate.isValid()) {
            throw new AnnotationException("No valid coordinate was found for ${projectedStart}-${projectedEnd}")
        }

        Integer unprojectedStart = unprojectedCoordinate.min
        Integer unprojectedEnd = unprojectedCoordinate.max

        Integer startOffset = unprojectedStart - projectedStart

//        ProjectionSequence startSequence = projection.getReverseProjectionSequence(projectedStart)
//        ProjectionSequence endSequence = projection.getReverseProjectionSequence(projectedEnd)
//        endSequence = endSequence ?: projection.getLastSequence()
        List<ProjectionSequence> sequences = projection.getReverseProjectionSequences(projectedStart, projectedEnd)

        // determine files to read for cu
        def stringList = []
        Integer index = 0
        for (ProjectionSequence projectionSequence in sequences) {
            Sequence sequence = Sequence.findByName(projectionSequence.name)
            // start case
            // could be only one, any portion
            Integer startIndex, endIndex
            if (index == 0) {
                if (sequences.size() == 1) {
                    startIndex = unprojectedStart - projectionSequence.originalOffset - startOffset
                    endIndex = unprojectedEnd - projectionSequence.originalOffset
//                    stringList << sequenceService.getRawResiduesFromSequence(sequence, unprojectedStart - startSequence.originalOffset - startOffset, unprojectedEnd - endSequence.originalOffset)
                } else {
                    startIndex = unprojectedStart - projectionSequence.originalOffset - startOffset
//                    endIndex = projection.getMaxCoordinate(projectionSequence).max
                    endIndex = sequence.length
//                    stringList << sequenceService.getRawResiduesFromSequence(sequence, unprojectedStart - startSequence.originalOffset - startOffset, unprojectedEnd - endSequence.originalOffset)
                }
            }
            // end case
            // implied at least 2, so the start will always be 0
            // ends with the end sequence
            else if (index == sequences.size() - 1) {
                startIndex = 0
//                endIndex = unprojectedEnd - projectionSequence.originalOffset
                endIndex = sequence.length
//                stringList << sequenceService.getRawResiduesFromSequence(sequence, 0, unprojectedEnd - endSequence.originalOffset )
            }
            // middle case
            else {
                startIndex = 0
//                endIndex = projection.getMaxCoordinate(projectionSequence).max
                endIndex = sequence.length
//                stringList << sequenceService.getRawResiduesFromSequence(sequence, 0, sequence.length)
            }
            stringList << sequenceService.getRawResiduesFromSequence(sequence, startIndex, endIndex)
            ++index
        }
//        if ( startSequence.name == endSequence.name) {
//            unprojectedString += sequenceService.getRawResiduesFromSequence(Sequence.findByName(startSequence.name), unprojectedStart - startSequence.originalOffset - startOffset, unprojectedEnd - endSequence.originalOffset)
//        }
//        // TODO: handle intermediate sequences?
//        else {
//            def stringList = []
//            stringList <<  sequenceService.getRawResiduesFromSequence(Sequence.findByName(startSequence.name), unprojectedStart - startSequence.originalOffset)
//            stringList <<  sequenceService.getRawResiduesFromSequence(Sequence.findByName(endSequence.name), 0, unprojectedEnd - endSequence.originalOffset)
//            unprojectedString = stringList.join("")
//        }

        String unprojectedString = stringList.join("")

        // TODO: cache the response for this "unique" file
//                Date lastModifiedDate = getLastModifiedDate(files);
//                String dateString = formatLastModifiedDate(files);
//                String eTag = createHash(putativeSequencePathName,(long) projectedString.bytes.length, (long) lastModifiedDate.time);
//                response.setHeader("ETag", eTag);
//                response.setHeader("Last-Modified", dateString);

        // re-project the string
//                String projectedString = projection.projectSequence(unprojectedString,unprojectedStart,unprojectedEnd,0)
        switch (projection.projectionDescription.projection.toUpperCase()) {
            case "EXON":
//                String returnString = projection.projectSequence(unprojectedString, 0, unprojectedString.length() - 1, unprojectedStart-projectedStart)
                String returnString = projection.projectSequence(unprojectedString, 0, unprojectedString.length() - 1, 0)
//                String returnString = projection.projectSequence(unprojectedString, unprojectedStart, unprojectedEnd, 0)
                return returnString
            default:
                return unprojectedString

        }


    }
}
