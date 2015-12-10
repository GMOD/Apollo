package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.MultiSequenceProjection
import org.bbop.apollo.projection.ProjectionSequence
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class RefSeqProjectorService {

    def projectionService
    def sequenceService

    @NotTransactional
    String projectTrack(JSONArray refSeqJsonObject, MultiSequenceProjection projection, Organism currentOrganism, String refererLoc) {

        JSONArray projectedArray = new JSONArray()

        for (int i = 0; i < refSeqJsonObject.size(); i++) {

            JSONObject sequenceValue = refSeqJsonObject.getJSONObject(i)

            String sequenceName = sequenceValue.getString("name")
            if (projection && projection.containsSequence(sequenceName, sequenceValue.id, currentOrganism)) {
                Integer projectedSequenceLength = projection.findProjectSequenceLength(sequenceName)
                sequenceValue.put("length", projectedSequenceLength)
                sequenceValue.put("end", projectedSequenceLength)
                sequenceValue.put("name", refererLoc)
                projectedArray = mergeRefseqProjections(projectedArray, sequenceValue)
            } else {
                log.debug "projeciton does not contain sequence ${sequenceName}"
            }
        }

        if (projection) {
            return projectedArray.toString()
        } else {
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
        Integer projectedEnd = chunkSize * (chunkNumber + 1)

        // determine the current "offsets" based on the chunk
        Integer unprojectedStart = projection.projectReverseValue(projectedStart)
        Integer unprojectedEnd = projection.projectReverseValue(projectedEnd)

        if(unprojectedEnd<0){
//            unprojectedEnd = projection.maxCoordinate.max+unprojectedStart
            unprojectedEnd = projection.getMaxCoordinate().getMax()
        }
        Integer startOffset = unprojectedStart - projectedStart


        ProjectionSequence startSequence = projection.getReverseProjectionSequence(projectedStart)
        ProjectionSequence endSequence = projection.getReverseProjectionSequence(projectedEnd)
        endSequence = endSequence ?: projection.getLastSequence()

        // determine files to read for cu
        String unprojectedString = ""
        if ( startSequence.name == endSequence.name) {
            unprojectedString += sequenceService.getRawResiduesFromSequence(Sequence.findByName(startSequence.name), unprojectedStart - startSequence.originalOffset - startOffset, unprojectedEnd - endSequence.originalOffset)
        }
        // TODO: handle intermediate sequences?
        else {
            def stringList = []
            stringList <<  sequenceService.getRawResiduesFromSequence(Sequence.findByName(startSequence.name), unprojectedStart - startSequence.originalOffset)
            stringList <<  sequenceService.getRawResiduesFromSequence(Sequence.findByName(endSequence.name), 0, unprojectedEnd - endSequence.originalOffset)
            unprojectedString = stringList.join("")
        }

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
                return returnString
            default:
                return unprojectedString

        }


    }
}
