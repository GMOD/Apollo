package org.bbop.apollo
/**
 * Created by Deepak on 3/9/15.
 */
public class GFF3Entry {

    private String seqId;
    private String source;
    private String type;
    private int start;
    private int end;
    private String score;
    private String strand;
    private String phase;
    private Map<String, String> attributes;

    public GFF3Entry(String seqId, String source, String type, int start, int end, String score, String strand, String phase) {
        this.seqId = seqId;
        this.source = source;
        this.type = type;
        this.start = start;
        this.end = end;
        this.score = score;
        this.strand = strand;
        this.phase = phase;
        this.attributes = new HashMap<String, String>();
    }

    public String getSeqId() {
        return seqId;
    }

    public void setSeqId(String seqId) {
        this.seqId = seqId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getStrand() {
        return strand;
    }

    public void setStrand(String strand) {
        this.strand = strand;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("%s\t%s\t%s\t%d\t%d\t%s\t%s\t%s\t", getSeqId(), getSource(), getType(), getStart(), getEnd(), getScore(), getStrand(), getPhase()));
        Iterator<Map.Entry<String, String>> iter = attributes.entrySet().iterator();
        if (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            buf.append(entry.getKey());
            buf.append("=");
            buf.append(entry.getValue());
            while (iter.hasNext()) {
                entry = iter.next();
                buf.append(";");
                buf.append(entry.getKey());
                buf.append("=");
                buf.append(entry.getValue());
            }
        }
        return buf.toString();
    }
}