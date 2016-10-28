package org.bbop.apollo.gwt.shared.projection;


import java.util.List;

/**
 * Created by nathandunn on 10/10/16.
 */
public class ProjectionSequence implements Comparable<ProjectionSequence>{
    private String id;
    private String name;
    private String organism;
    private Integer order ; // what order this should be processed as
    private Long offset = 0l;  // projected offset from originalOffset
    private Long originalOffset = 0l; // original incoming coordinates . .  0 implies order = 0, >0 implies that order > 0
    private List<String> features;// a list of Features  // default is a single entry ALL . . if empty then all
    private Long unprojectedLength = 0l;
    private Long start;
    private Long end;
    private Boolean reverse = false ;// this is the reverse complement value of the projection sequence


    public Long getOriginalOffsetStart(){
        return start + originalOffset;
    }

    public Long getOriginalOffsetEnd(){
        return end + originalOffset;
    }


    public Long getLength() {
        return end - start;
    }

    @Override
    public int compareTo(ProjectionSequence o) {
        int test =  0 ;
        if(order!=null && o.order !=null){
            test = order.compareTo(o.order);
            if (test != 0) {
                return test;
            }
        }
        if(name !=null && o.name !=null){
            test = name.compareTo(o.name);
            if (test != 0) {
                return test;
            }
        }
        test = start.compareTo(o.start);
        if (test != 0) {
            return test;
        }
        test = end.compareTo(o.end);
        if (test != 0) {
            return test;
        }
        return 0 ;
    }

        @Override
    public boolean equals(Object o) {

        if(this==o) return true;
        if (!(o instanceof ProjectionSequence)) return false;
        ProjectionSequence that = (ProjectionSequence) o;
        if (!end.equals(that.end)) return false;
        if (!name.equals(that.name)) return false;
//        if (!organism.equals(that.organism)) return false;
        if (!start.equals(that.start)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = start.hashCode();
        result = 31 * result + end.hashCode();
        result = 31 * result + name.hashCode();
//        result = 31 * result + (organism != null ? organism.hashCode() : 0);
        return result;
    }
//
//    @Override
//    public int compareTo(ProjectionSequence o) {
//        int test = order.compareTo(o.order);
//        if (test != 0) {
//            return test;
//        }
//        test = name.compareTo(o.name);
//        if (test != 0) {
//            return test;
//        }
//        test = start.compareTo(o.start);
//        if (test != 0) {
//            return test;
//        }
//        test = end.compareTo(o.end);
//        if (test != 0) {
//            return test;
//        }
//        return 0 ;
//    }

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

    public void setOrder(Integer  order) {
        this.order = order;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Long getOriginalOffset() {
        return originalOffset;
    }

    public void setOriginalOffset(Long originalOffset) {
        this.originalOffset = originalOffset;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public Long getUnprojectedLength() {
        return unprojectedLength;
    }

    public void setUnprojectedLength(Long unprojectedLength) {
        this.unprojectedLength = unprojectedLength;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public Boolean getReverse() {
        return reverse;
    }

    public void setReverse(Boolean reverse) {
        this.reverse = reverse;
    }

    @Override
    public String toString() {
        return "ProjectionSequence{" +
                "name='" + name + '\'' +
                ", order=" + order +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
