package org.bbop.apollo.gwt.shared.projection;

/**
 * Created by nathandunn on 10/10/16.
 */
public class Coordinate implements Comparable<Coordinate>{
    private Long min;
    private Long max;
    private ProjectionSequence sequence;

    public Coordinate(){}

    public Coordinate(Long min,Long max){
        this.min = min ;
        this.max = max ;
    }

    public Coordinate(Long min,Long max,ProjectionSequence sequence){
        this.min = min ;
        this.max = max ;
        this.sequence = sequence ;
    }

    @Override
    public int compareTo(Coordinate o) {
        if(min.compareTo(o.min)!=0){
            return min.compareTo(o.min);
        }
        if(max.compareTo(o.max)!=0){
            return max.compareTo(o.max);
        }
        return 0 ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coordinate that = (Coordinate) o;

        if (!min.equals(that.min)) return false;
        if (!max.equals(that.max)) return false;
        return sequence != null ? sequence.equals(that.sequence) : that.sequence == null;

    }

//    boolean equals(Object o) {
//        if (this.is(o)) return true
//        if (!(o instanceof Coordinate)) return false
//
//        Coordinate that = (Coordinate) o
//
//        if (max != that.max) return false
//        if (min != that.min) return false
//        if(sequence && that.sequence){
//            return sequence == that.sequence
//        }
//
//        return true
//    }

    @Override
    public int hashCode() {
        int result;
        result = (min != null ? min.hashCode() : 0);
        result = 31 * result + (max != null ? max.hashCode() : 0);
        result = 31 * result + ( sequence != null ? sequence.hashCode() : 0);
        return result;
    }


//    @Override
//    public String toString() {
//        return "Coordinate{" +
//                "min=" + min +
//                ", max=" + max +
//                sequence ? ", sequence=" + sequence : "" +
//                '}';
//    }

    Boolean isValid() {
        return min>=0 && max>=0;
    }

    Long getLength(){
        return Math.abs(max - min);
    }

    void addOffset(Long offset){
        min = min+offset;
        max = max+offset;
    }

    public Long getMin() {
        return min;
    }

    public void setMin(Long min) {
        this.min = min;
    }

    public Long getMax() {
        return max;
    }

    public void setMax(Long max) {
        this.max = max;
    }

    public ProjectionSequence getSequence() {
        return sequence;
    }

    public void setSequence(ProjectionSequence sequence) {
        this.sequence = sequence;
    }
}
