package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class DbDbxrefCount implements Serializable {

    String name
    Long numDbxrefs

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append name
        builder.append numDbxrefs
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append name, other.name
        builder.append numDbxrefs, other.numDbxrefs
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["name", "numDbxrefs"]
        version false
    }

    static constraints = {
        name nullable: true
        numDbxrefs nullable: true
    }
}
