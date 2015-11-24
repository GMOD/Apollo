package org.bbop.apollo.projection

/**
 * A description of features
 * Created by nathandunn on 9/24/15.
 */
class ProjectionDescription {

    List<String> referenceTrack // typically one
    List<ProjectionSequence> sequenceList // an ordered array of sequences or ALL . . .if empty then all
    String projection
    Integer padding // the padding around the reference
    String organism // name of the organism

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ProjectionDescription that = (ProjectionDescription) o

        if (organism != that.organism) return false
        if (padding != that.padding) return false
        if (referenceTrack != that.referenceTrack) return false
        if (sequenceList != that.sequenceList) return false
        if (projection != that.projection) return false

        return true
    }

    int hashCode() {
        int result
        result = (referenceTrack != null ? referenceTrack.hashCode() : 0)
        result = 31 * result + sequenceList.hashCode()
        result = 31 * result + projection.hashCode()
        result = 31 * result + padding.hashCode()
        result = 31 * result + organism.hashCode()
        return result
    }
}
