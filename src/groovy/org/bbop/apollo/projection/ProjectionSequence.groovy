package org.bbop.apollo.projection

/**
 * Created by nathandunn on 9/24/15.
 */
class ProjectionSequence implements Comparable<org.bbop.apollo.projection.ProjectionSequence>{

    String id
    String name
    String organism

    Integer order  // what order this should be processed as
    Integer offset  = 0
    Integer originalOffset = 0 // original incoming coordinates
    List<String> features // a list of Features  // dfeault is a single entry ALL . . if empty then all


    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ProjectionSequence that = (ProjectionSequence) o

        if (id != that.id) return false
        if (name != that.name) return false
//        if (offset != that.offset) return false
//        if (order != that.order) return false
        if (organism != that.organism) return false

        return true
    }

    int hashCode() {
        int result
        result = (id != null ? id.hashCode() : 0)
        result = 31 * result + (name != null ? name.hashCode() : 0)
        result = 31 * result + (organism != null ? organism.hashCode() : 0)
//        result = 31 * result + (order != null ? order.hashCode() : 0)
//        result = 31 * result + (offset != null ? offset.hashCode() : 0)
        return result
    }

    @Override
    int compareTo(ProjectionSequence o) {
        id <=> o.id
        name <=> o.name
        order <=> o.order
//        organism <=> o.organism
    }
}
