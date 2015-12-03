package org.bbop.apollo.projection

/**
 * Created by nathandunn on 12/2/15.
 */
class TrackIndex {
    // pulled
    Integer start
    Integer end
    String source

    // need to pull
    Integer strand
    Integer phase
    Integer type
    Integer seqId
    Double score
    Integer chunk
    Integer id
    Integer subFeaturesColumn
    Integer sublistColumn

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

    Boolean hasSubList() {
        return sublistColumn && sublistColumn>0
    }

    Boolean hasSubFeatures() {
        return subFeaturesColumn && subFeaturesColumn>0
    }
}
