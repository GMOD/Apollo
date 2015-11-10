package org.bbop.apollo.projection

/**
 * Created by nathandunn on 11/10/15.
 */
class ProjectionChunk {

    String sequence = null
//    Integer start
    Integer numChunks = 0
    Integer sequenceOffset = 0
    Integer chunkArrayOffset = 0


    void addChunk(){
        ++numChunks
    }
}
