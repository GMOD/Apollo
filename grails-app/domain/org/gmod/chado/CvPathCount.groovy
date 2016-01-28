package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class CvPathCount implements Serializable {

    String cvName
    String relationName
    String relationCvName
    Long numPaths

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append cvName
        builder.append relationName
        builder.append relationCvName
        builder.append numPaths
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append cvName, other.cvName
        builder.append relationName, other.relationName
        builder.append relationCvName, other.relationCvName
        builder.append numPaths, other.numPaths
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["cvName", "relationName", "relationCvName", "numPaths"]
        version false
    }

    static constraints = {
        cvName nullable: true
        relationName nullable: true, maxSize: 1024
        relationCvName nullable: true
        numPaths nullable: true
    }
}
