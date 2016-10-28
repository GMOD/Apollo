package org.bbop.apollo.gwt.shared.projection;

/**
 * Created by nathandunn on 11/10/15.
 */
public class ProjectionChunk {

    // what is the sequence name
    String sequence = null;
//    Integer start
    // how many chunks are there in this group
    Integer numChunks = 0;

    // what is the LAST bp of the prior sequence
    Long sequenceOffset = 0l;

    // if I have chunks 50 and 52 . . . they are probably 1 and 3 . . but have to map to the right sequene
    Integer chunkArrayOffset = 0;

    public void addChunk(){
        ++numChunks;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public Integer getNumChunks() {
        return numChunks;
    }

    public void setNumChunks(Integer numChunks) {
        this.numChunks = numChunks;
    }

    public Long getSequenceOffset() {
        return sequenceOffset;
    }

    public void setSequenceOffset(Long sequenceOffset) {
        this.sequenceOffset = sequenceOffset;
    }

    public Integer getChunkArrayOffset() {
        return chunkArrayOffset;
    }

    public void setChunkArrayOffset(Integer chunkArrayOffset) {
        this.chunkArrayOffset = chunkArrayOffset;
    }
}
