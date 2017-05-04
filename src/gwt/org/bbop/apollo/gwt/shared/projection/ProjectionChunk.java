package org.bbop.apollo.gwt.shared.projection;

/**
 * This an object that maps the projected chunk requested to the actual chunk.
 *
 *
 *
 * Created by nathandunn on 11/10/15.
 */
public class ProjectionChunk {

    /**
     * what is the sequence name
     */
    private String sequence = null;
    /**
     * how many chunks are there in this group (e.g., if I request a single chunk starting at 3, if the offset is 2, and the numChunks = 3, then I would request 1,2,3 on the backend.
     */
    private Integer numChunks = 0;

    /**
     * used to request the original ID, but I think this might be incorrect.
     */
    private Integer chunkID = null ;

    /**
     * what is the LAST bp of the prior sequence.
     */
    private Long sequenceOffset = 0L;

    /**
     *  if I have chunks 50 and 52 . . . they are probably 1 and 3 . . but have to map to the right sequence
     */
    private Integer chunkArrayOffset = 0;

    public Integer getChunkID() {
        return chunkID;
    }

    public void setChunkID(Integer chunkID) {
        this.chunkID = chunkID;
    }

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
