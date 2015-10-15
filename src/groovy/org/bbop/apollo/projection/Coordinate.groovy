package org.bbop.apollo.projection

/**
 * Created by ndunn on 8/24/15.
 */
class Coordinate implements Comparable<Coordinate>{

    Integer min
    Integer max


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
}
