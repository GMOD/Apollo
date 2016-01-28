package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class FeatureDistance implements Serializable {

    Integer subjectId
    Integer objectId
    Integer srcfeatureId
    Short subjectStrand
    Short objectStrand
    Integer distance

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append subjectId
        builder.append objectId
        builder.append srcfeatureId
        builder.append subjectStrand
        builder.append objectStrand
        builder.append distance
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
        builder.append distance, other.distance
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["subjectId", "objectId", "srcfeatureId", "subjectStrand", "objectStrand", "distance"]
        version false
    }

    static constraints = {
        subjectId nullable: true
        objectId nullable: true
        srcfeatureId nullable: true
        subjectStrand nullable: true
        objectStrand nullable: true
        distance nullable: true
    }
}
