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

    // set from intake
    String trackName
    String organism
    int classIndex
}
