package org.bbop.apollo.projection

/**
 * Both the min/max should be treated as inclusive coordinates
 * Created by Nathan Dunn on 8/24/15.
 */
class Coordinate implements Comparable<Coordinate>{

    Integer min
    Integer max
//    String sequence
//    String organism


    @Override
    int compareTo(Coordinate o) {
        min <=> o.min ?: max <=> o.max
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof Coordinate)) return false

        Coordinate that = (Coordinate) o

        if (max != that.max) return false
        if (min != that.min) return false

        return true
    }

    int hashCode() {
        int result
        result = (min != null ? min.hashCode() : 0)
        result = 31 * result + (max != null ? max.hashCode() : 0)
        return result
    }


    @Override
    public String toString() {
        return "Coordinate{" +
                "min=" + min +
                ", max=" + max +
//                sequence ? ", sequence=" + sequence : "" +
//                organism ? ", organism=" + organism : "" +
                '}';
    }

    Boolean isValid() {
        return min>=0 && max>=0
    }

    Integer getLength(){
        return Math.abs(max - min)
    }

    void addOffset(Integer offset){
        min = min+offset
        max = max+offset
    }
}
