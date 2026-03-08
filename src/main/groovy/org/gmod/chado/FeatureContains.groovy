package org.gmod.chado

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class FeatureContains implements Serializable {
    // Note: This domain class is not part of any Chado module
    // Probably an artifact of the reverse engineering script.
    // TODO: Remove if not needed.
    Integer subjectId
    Integer objectId

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append subjectId
        builder.append objectId
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append subjectId, other.subjectId
        builder.append objectId, other.objectId
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["subjectId", "objectId"]
        version false
        optimisticLock 'none'
    }

    static constraints = {
        subjectId nullable: true
        objectId nullable: true
    }
}
