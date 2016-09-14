package org.bbop.apollo.projection

/**
 * Created by nathandunn on 12/2/15.
 */
class TrackIndex {
    // index locations . . .
    Integer start
    Integer end
    Integer source
    Integer strand
    Integer phase
    Integer type
    Integer seqId
    Integer score
    Integer chunk
    Integer id
    Integer subFeaturesColumn

    Integer sublistColumn // unclear if this has a column . . I think its just the last column . . or just implies "chunk"

    // set from intake
    String trackName
    String organism
    Integer classIndex

    def fixCoordinates() {
        properties.each {
            if(it.value instanceof Integer && it.value==0){
                it.value = null
            }
        }
    }

    Boolean hasChunk() {
        return chunk>0
//        return sublistColumn && sublistColumn>0
    }

    Boolean hasSubFeatures() {
        return subFeaturesColumn && subFeaturesColumn>0
    }

    Boolean hasSubList() {
        return sublistColumn && sublistColumn > 0
    }

}
