package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class FeaturesetMeets implements Serializable {

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
    }

    static constraints = {
        subjectId nullable: true
        objectId nullable: true
    }
}
