package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class FeatureIntersection implements Serializable {
    // Note: This domain class is not part of any Chado module
    // Probably an artifact of the reverse engineering script.
    // TODO: Remove if not needed.
    Integer subjectId
    Integer objectId
    Integer srcfeatureId
    Short subjectStrand
    Short objectStrand
    Integer fmin
    Integer fmax

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append subjectId
        builder.append objectId
        builder.append srcfeatureId
        builder.append subjectStrand
        builder.append objectStrand
        builder.append fmin
        builder.append fmax
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append subjectId, other.subjectId
        builder.append objectId, other.objectId
        builder.append srcfeatureId, other.srcfeatureId
        builder.append subjectStrand, other.subjectStrand
        builder.append objectStrand, other.objectStrand
        builder.append fmin, other.fmin
        builder.append fmax, other.fmax
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["subjectId", "objectId", "srcfeatureId", "subjectStrand", "objectStrand", "fmin", "fmax"]
        version false
    }

    static constraints = {
        subjectId nullable: true
        objectId nullable: true
        srcfeatureId nullable: true
        subjectStrand nullable: true
        objectStrand nullable: true
        fmin nullable: true
        fmax nullable: true
    }
}
