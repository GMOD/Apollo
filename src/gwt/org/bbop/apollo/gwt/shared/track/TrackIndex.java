package org.bbop.apollo.gwt.shared.track;

/**
 * Created by nathandunn on 12/2/15.
 */
public class TrackIndex {
    // index locations . . .
    private Integer start;
    private Integer end;
    private Integer source;
    private Integer strand;
    private Integer phase;
    private Integer type;
    private Integer score;
    private Integer chunk;
    private Integer id;
    private Integer subFeaturesColumn;
    private Integer name ;
    private Integer alias;

    private Integer seqId;
    private Integer classIndex;


    private Integer sublistColumn;// unclear if this has a column . . I think its just the last column . . or just implies "chunk"

    // set from intake
    private String trackName;
    private String organism;


    public void fixCoordinates() {
        start = start == 0 ? null : start;
        end = end == 0 ? null : end;
        source = source == 0 ? null : source;
        strand = strand == 0 ? null : strand;
        phase = phase == 0 ? null : phase;
        type = type == 0 ? null : type;
        score = score == 0 ? null : score;
        chunk = chunk == 0 ? null : chunk;
        id = id == 0 ? null : id;
        subFeaturesColumn = subFeaturesColumn == 0 ? null : subFeaturesColumn;
        sublistColumn = sublistColumn == 0 ? null : sublistColumn;
    }

    public Boolean hasChunk() {
        return chunk != null && chunk > 0;
    }

    public Boolean hasSubFeatures() {
        return subFeaturesColumn != null && subFeaturesColumn > 0;
    }

    public Boolean hasSubList() {
        return sublistColumn != null && sublistColumn > 0;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public Integer getStrand() {
        return strand;
    }

    public void setStrand(Integer strand) {
        this.strand = strand;
    }

    public Integer getPhase() {
        return phase;
    }

    public void setPhase(Integer phase) {
        this.phase = phase;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getChunk() {
        return chunk;
    }

    public void setChunk(Integer chunk) {
        this.chunk = chunk;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSubFeaturesColumn() {
        return subFeaturesColumn;
    }

    public void setSubFeaturesColumn(Integer subFeaturesColumn) {
        this.subFeaturesColumn = subFeaturesColumn;
    }

    public Integer getSublistColumn() {
        return sublistColumn;
    }

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


    public Integer getSeqId() {
        return seqId;
    }

    public void setSeqId(Integer seqId) {
        this.seqId = seqId;
    }

    public Integer getClassIndex() {
        return classIndex;
    }

    public void setClassIndex(Integer classIndex) {
        this.classIndex = classIndex;
    }

    public Integer getName() {
        return name;
    }

    public void setName(Integer name) {
        this.name = name;
    }

    public Integer getAlias() {
        return alias;
    }

    public void setAlias(Integer alias) {
        this.alias = alias;
    }
}
