package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class CvCvtermCount implements Serializable {

    String name
    Long numTermsExclObs

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append name
        builder.append numTermsExclObs
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append name, other.name
        builder.append numTermsExclObs, other.numTermsExclObs
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["name", "numTermsExclObs"]
        version false
    }

    static constraints = {
        name nullable: true
        numTermsExclObs nullable: true
    }
}
