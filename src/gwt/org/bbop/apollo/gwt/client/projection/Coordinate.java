package org.bbop.apollo.gwt.client.projection;

/**
 * Created by nathandunn on 10/10/16.
 */
public class Coordinate implements Comparable<Coordinate>{
    private Integer min;
    private Integer max;
    private ProjectionSequence sequence;

    public Coordinate(Integer min,Integer max){
        this.min = min ;
        this.max = max ;
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

    Integer getLength(){
        return Math.abs(max - min);
    }

    void addOffset(Integer offset){
        min = min+offset;
        max = max+offset;
    }
}
