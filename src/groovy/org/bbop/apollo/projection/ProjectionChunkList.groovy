package org.bbop.apollo.projection

/**
 * Created by nathandunn on 11/10/15.
 *
 * This is made for mapping lf-X . . in a set of sequences where the prior sequences
 * could be made of un non-chunked data.
 */

class ProjectionChunkList {

    List<ProjectionChunk> projectionChunkList = []

    void addChunk(ProjectionChunk projectionChunk){
        projectionChunkList.add(projectionChunk)
    }

    ProjectionChunk findProjectChunkForIndex(Integer index){
        Integer counter = 0
        for(ProjectionChunk projectionChunk : projectionChunkList){
            if(index >= counter && index < projectionChunk.numChunks + counter){
                return projectionChunk
            }
//            counter += projectionChunk.end
            ++counter
        }
        return null
    }
}
