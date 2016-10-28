package org.bbop.apollo.gwt.shared.projection;

/**
 * Created by nathandunn on 12/2/15.
 */
class TrackIndex {
    // index locations . . .
    private Integer start;
    private Integer end;
    private Integer source;
    private Integer strand;
    private Integer phase;
    private Integer type;
    private Integer seqId;
    private Integer score;
    private Integer chunk;
    private Integer id;
    private Integer subFeaturesColumn;

    private Integer sublistColumn ;// unclear if this has a column . . I think its just the last column . . or just implies "chunk"

    // set from intake
    private String trackName;
    private String organism;
    private Integer classIndex;

//    void fixCoordinates() {
//        properties.each {
//            if(it.value instanceof Integer && it.value==0){
//                it.value = null
//            }
//        }
//    }

    Boolean hasChunk() {
        return chunk>0;
//        return sublistColumn && sublistColumn>0
    }

    Boolean hasSubFeatures() {
        return subFeaturesColumn!=null && subFeaturesColumn>0;
    }

    Boolean hasSubList() {
        return sublistColumn!=null && sublistColumn > 0;
    }

}
