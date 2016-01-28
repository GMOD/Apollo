package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class CvRoot implements Serializable {

    Integer cvId
    Integer rootCvtermId

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append cvId
        builder.append rootCvtermId
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append cvId, other.cvId
        builder.append rootCvtermId, other.rootCvtermId
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["cvId", "rootCvtermId"]
        version false
    }

    static constraints = {
        cvId nullable: true
        rootCvtermId nullable: true
    }
}
