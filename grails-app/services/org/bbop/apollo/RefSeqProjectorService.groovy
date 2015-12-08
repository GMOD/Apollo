package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.projection.MultiSequenceProjection
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class RefSeqProjectorService {

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
                projectedArray = mergeRefseqProjections(projectedArray,sequenceValue)
            }
            else{
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
     *
     {"seqChunkSize": 20000, "start": 0,
     "name": "{\"projection\":\"None\", \"padding\":50, \"referenceTrack\":\"Official Gene Set v3.2\", \"sequences\":[{\"name\":\"Group5.7\"},{\"name\":\"Group9.2\"}]}",
     "length": 456816, "end": 456816}]
     *
     *  Ignore chunkSize, start, name
     *
     *  sum: length / end
     *
     * @param jsonArray
     * @param refSeq   JSONObject to add
     * @return
     */
    private static JSONArray mergeRefseqProjections(JSONArray projectedArray, JSONObject refSeq) {
        if (projectedArray.size() == 0) {
            projectedArray.add(refSeq)
        } else if (projectedArray.size() == 1) {
            JSONObject existingObject = projectedArray.getJSONObject(0)
            Integer existingLength = existingObject.getInt("length")
            Integer nextLength = refSeq.getInt("length")

            existingObject.put("length",existingLength+nextLength)
            existingObject.put("end",existingLength+nextLength)
        } else {
            throw new RuntimeException("wrong number of input projected arrays ${projectedArray?.size()}: ${projectedArray as JSON} . .. ${refSeq as JSON}")
        }
        return projectedArray
    }
}
