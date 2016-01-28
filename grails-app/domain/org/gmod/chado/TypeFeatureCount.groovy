package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class TypeFeatureCount implements Serializable {

    String type
    Long numFeatures

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append type
        builder.append numFeatures
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append type, other.type
        builder.append numFeatures, other.numFeatures
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["type", "numFeatures"]
        version false
    }

    static constraints = {
        type nullable: true, maxSize: 1024
        numFeatures nullable: true
    }
}
