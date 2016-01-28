package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class CvLinkCount implements Serializable {

    String cvName
    String relationName
    String relationCvName
    Long numLinks

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append cvName
        builder.append relationName
        builder.append relationCvName
        builder.append numLinks
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append cvName, other.cvName
        builder.append relationName, other.relationName
        builder.append relationCvName, other.relationCvName
        builder.append numLinks, other.numLinks
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["cvName", "relationName", "relationCvName", "numLinks"]
        version false
    }

    static constraints = {
        cvName nullable: true
        relationName nullable: true, maxSize: 1024
        relationCvName nullable: true
        numLinks nullable: true
    }
}
