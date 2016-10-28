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

    public void fixCoordinates() {
//        properties.each {
//            if(it.value instanceof Integer && it.value==0){
//                it.value = null
//            }
//        }
    }

    public Boolean hasChunk() {
        return chunk>0;
//        return sublistColumn && sublistColumn>0
    }

    public Boolean hasSubFeatures() {
        return subFeaturesColumn!=null && subFeaturesColumn>0;
    }

    public Boolean hasSubList() {
        return sublistColumn!=null && sublistColumn > 0;
    }

    public Integer getStart() {
        return start == 0 ? null : start ;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end == 0 ? null : end ;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    public Integer getSource() {
        return source == 0 ? null : source ;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public Integer getStrand() {
        return strand == 0 ? null : strand ;
    }

    public void setStrand(Integer strand) {
        this.strand = strand;
    }

    public Integer getPhase() {
        return phase == 0 ? null : phase ;
    }

    public void setPhase(Integer phase) {
        this.phase = phase;
    }

    public Integer getType() {
        return type == 0 ? null : type ;
    }

    public void setType(Integer type) {
        this.type = type;
    }

//    public Integer getSeqId() {
//        return seqId == 0 ? null : seqId ;
//    }

    public void setSeqId(Integer seqId) {
        this.seqId = seqId;
    }

    public Integer getScore() {
        return score == 0 ? null : score ;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getChunk() {
        return chunk == 0 ? null : chunk ;
    }

    public void setChunk(Integer chunk) {
        this.chunk = chunk;
    }

    public Integer getId() {
        return id == 0 ? null : id ;
    }

    public void setId(Integer id) {
        this.id = id;
    }

//    public Integer getSubFeaturesColumn() {
//        return subFeaturesColumn;
//    }

    public void setSubFeaturesColumn(Integer subFeaturesColumn) {
        this.subFeaturesColumn = subFeaturesColumn;
    }

//    public Integer getSublistColumn() {
//        return sublistColumn;
//    }

    public void setSublistColumn(Integer sublistColumn) {
        this.sublistColumn = sublistColumn;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

//    public Integer getClassIndex() {
//        return classIndex;
//    }

    public void setClassIndex(Integer classIndex) {
        this.classIndex = classIndex;
    }
}
