package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class CvRootMview implements Serializable {

    String name
    Integer cvtermId
    Integer cvId
    String cvName

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append name
        builder.append cvtermId
        builder.append cvId
        builder.append cvName
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append name, other.name
        builder.append cvtermId, other.cvtermId
        builder.append cvId, other.cvId
        builder.append cvName, other.cvName
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["name", "cvtermId", "cvId", "cvName"]
        version false
    }

    static constraints = {
        name nullable: true, maxSize: 1024
        cvtermId nullable: true
        cvId nullable: true
        cvName nullable: true
    }
}
