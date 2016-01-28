package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class GffMeta implements Serializable {

    String name
    String hostname
    Date starttime

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append name
        builder.append hostname
        builder.append starttime
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append name, other.name
        builder.append hostname, other.hostname
        builder.append starttime, other.starttime
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["name", "hostname", "starttime"]
        version false
    }

    static constraints = {
        name nullable: true, maxSize: 100
        hostname nullable: true, maxSize: 100
    }
}
