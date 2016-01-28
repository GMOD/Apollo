package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class CvLeaf implements Serializable {

    Integer cvId
    Integer cvtermId

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append cvId
        builder.append cvtermId
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append cvId, other.cvId
        builder.append cvtermId, other.cvtermId
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["cvId", "cvtermId"]
        version false
    }

    static constraints = {
        cvId nullable: true
        cvtermId nullable: true
    }
}
