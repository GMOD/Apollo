package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class CvCvtermCountWithObs implements Serializable {

    String name
    Long numTermsInclObs

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append name
        builder.append numTermsInclObs
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append name, other.name
        builder.append numTermsInclObs, other.numTermsInclObs
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["name", "numTermsInclObs"]
        version false
    }

    static constraints = {
        name nullable: true
        numTermsInclObs nullable: true
    }
}
