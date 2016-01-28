package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class AllFeatureNames implements Serializable {

    Integer featureId
    String name
    Integer organismId
    Serializable searchableName

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append featureId
        builder.append name
        builder.append organismId
        builder.append searchableName
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append featureId, other.featureId
        builder.append name, other.name
        builder.append organismId, other.organismId
        builder.append searchableName, other.searchableName
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["featureId", "name", "organismId", "searchableName"]
        version false
    }

    static constraints = {
        featureId nullable: true
        name nullable: true
        organismId nullable: true
        searchableName nullable: true
    }
}
