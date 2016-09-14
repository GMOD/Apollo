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
        for(ProjectionChunk projectionChunk : projectionChunkList){
            if(index >= projectionChunk.chunkArrayOffset && index <= projectionChunk.numChunks + projectionChunk.chunkArrayOffset){
                return projectionChunk
            }
        }
        return null
    }

    ProjectionChunk findProjectChunkForName(String sequenceName) {
        for(ProjectionChunk projectionChunk : projectionChunkList){
            if(projectionChunk.sequence==sequenceName){
                return projectionChunk
            }
        }
        return null
    }
}
