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
            if(projectionChunk.getChunkID().equals(index)){
                return projectionChunk;
            }
            if(index >= projectionChunk.getChunkArrayOffset() && index <= projectionChunk.getNumChunks()+ projectionChunk.getChunkArrayOffset()){
                return projectionChunk;
            }
        }
        return null;
    }

    ProjectionChunk findProjectChunkForName(String sequenceName) {
        for(ProjectionChunk projectionChunk : projectionChunkList){
            if(projectionChunk.getSequence().equals(sequenceName)){
                return projectionChunk;
            }
        }
        return null;
    }
}
