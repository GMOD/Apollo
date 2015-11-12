package org.bbop.apollo.projection

/**
 * Created by nathandunn on 11/10/15.
 */
class ProjectionChunk {

    // what is the sequence name
    String sequence = null
//    Integer start
    // how many chunks are there in this group
    Integer numChunks = 0

    // what is the LAST bp of the prior sequence
    Integer sequenceOffset = 0

    // if I have chunks 50 and 52 . . . they are probably 1 and 3 . . but have to map to the right sequene
    Integer chunkArrayOffset = 0


    void addChunk(){
        ++numChunks
    }
}
