package org.bbop.apollo.gwt.shared.projection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nathandunn on 11/10/15.
 *
 * This is made for mapping lf-X . . in a set of sequences where the prior sequences
 * could be made of un non-chunked data.
 */

public class ProjectionChunkList {

    public List<ProjectionChunk> projectionChunkList = new ArrayList<>();

    public void addChunk(ProjectionChunk projectionChunk){
        projectionChunkList.add(projectionChunk);
    }

    public ProjectionChunk findProjectChunkForIndex(Integer index){
        for(ProjectionChunk projectionChunk : projectionChunkList){
            if(index >= projectionChunk.chunkArrayOffset && index <= projectionChunk.numChunks + projectionChunk.chunkArrayOffset){
                return projectionChunk;
            }
        }
        return null;
    }

    ProjectionChunk findProjectChunkForName(String sequenceName) {
        for(ProjectionChunk projectionChunk : projectionChunkList){
            if(projectionChunk.sequence.equals(sequenceName)){
                return projectionChunk;
            }
        }
        return null;
    }
}
