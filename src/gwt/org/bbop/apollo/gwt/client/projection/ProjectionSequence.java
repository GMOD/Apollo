package org.bbop.apollo.gwt.client.projection;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.List;

/**
 * Created by nathandunn on 10/10/16.
 */
public class ProjectionSequence implements Comparable<ProjectionSequence>{
    private String id;
    private String name;
    private String organism;
    private Integer order ; // what order this should be processed as
    private Integer offset = 0;  // projected offset from originalOffset
    private Integer originalOffset = 0; // original incoming coordinates . .  0 implies order = 0, >0 implies that order > 0
    private List<String> features;// a list of Features  // default is a single entry ALL . . if empty then all
    private Integer unprojectedLength = 0;
    private Integer start;
    private Integer end;
    private Boolean reverse = false ;// this is the reverse complement value of the projection sequence

    @Override
    public boolean equals(Object o) {
//        if (this.is(o)) return true;
        if (!(o instanceof ProjectionSequence)) return false;
        ProjectionSequence that = (ProjectionSequence) o;
        if (!end.equals(that.end)) return false;
        if (!name.equals(that.name)) return false;
        if (!organism.equals(that.organism)) return false;
        if (!start.equals(that.start)) return false;
        return true;
    }

    Integer getOriginalOffsetStart(){
        return start + originalOffset;
    }

    Integer getOriginalOffsetEnd(){
        return end + originalOffset;
    }

    @Override
    public int hashCode() {
        int result;
        result = start.hashCode();
        result = 31 * result + end.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (organism != null ? organism.hashCode() : 0);
        return result;
    }

    Integer getLength() {
        return end - start;
    }

    @Override
    public int compareTo(ProjectionSequence o) {
        int test = order.compareTo(o.order);
        if (test != 0) {;
            return test;
        };
        return name.compareTo(o.name);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getOriginalOffset() {
        return originalOffset;
    }

    public void setOriginalOffset(Integer originalOffset) {
        this.originalOffset = originalOffset;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public Integer getUnprojectedLength() {
        return unprojectedLength;
    }

    public void setUnprojectedLength(Integer unprojectedLength) {
        this.unprojectedLength = unprojectedLength;
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

    public Boolean getReverse() {
        return reverse;
    }

    public void setReverse(Boolean reverse) {
        this.reverse = reverse;
    }
}
