package org.bbop.apollo.projection

/**
 * A description of features
 * Created by nathandunn on 9/24/15.
 */
class ProjectionDescription {

    List<String> referenceTracks // typically one
    List<ProjectionSequence> sequenceList // an ordered array of sequences or ALL . . .if empty then all
    String type
    Integer padding // the padding around the reference
    String organism // name of the organism

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ProjectionDescription that = (ProjectionDescription) o

        if (organism != that.organism) return false
        if (padding != that.padding) return false
        if (referenceTracks != that.referenceTracks) return false
        if (sequenceList != that.sequenceList) return false
        if (type != that.type) return false

        return true
    }

    int hashCode() {
        int result
        result = (referenceTracks != null ? referenceTracks.hashCode() : 0)
        result = 31 * result + sequenceList.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + padding.hashCode()
        result = 31 * result + organism.hashCode()
        return result
    }
}
