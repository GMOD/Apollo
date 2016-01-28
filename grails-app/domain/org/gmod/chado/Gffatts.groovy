package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class Gffatts implements Serializable {

    Integer featureId
    String type
    String attribute

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append featureId
        builder.append type
        builder.append attribute
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append featureId, other.featureId
        builder.append type, other.type
        builder.append attribute, other.attribute
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["featureId", "type", "attribute"]
        version false
    }

    static constraints = {
        featureId nullable: true
        type nullable: true
        attribute nullable: true
    }
}
