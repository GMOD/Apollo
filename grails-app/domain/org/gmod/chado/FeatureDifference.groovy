package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class FeatureDifference implements Serializable {

    Integer subjectId
    Integer objectId
    Short srcfeatureId
    Integer fmin
    Integer fmax
    Integer strand

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append subjectId
        builder.append objectId
        builder.append srcfeatureId
        builder.append fmin
        builder.append fmax
        builder.append strand
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append subjectId, other.subjectId
        builder.append objectId, other.objectId
        builder.append srcfeatureId, other.srcfeatureId
        builder.append fmin, other.fmin
        builder.append fmax, other.fmax
        builder.append strand, other.strand
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["subjectId", "objectId", "srcfeatureId", "fmin", "fmax", "strand"]
        version false
    }

    static constraints = {
        subjectId nullable: true
        objectId nullable: true
        srcfeatureId nullable: true
        fmin nullable: true
        fmax nullable: true
        strand nullable: true
    }
}
