package org.gmod.chado

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

class GffIntervalStats implements Serializable {

    String typeid
    Integer srcfeatureId
    Integer bin
    Integer cumCount

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append typeid
        builder.append srcfeatureId
        builder.append bin
        builder.append cumCount
        builder.toHashCode()
    }

    boolean equals(other) {
        if (other == null) return false
        def builder = new EqualsBuilder()
        builder.append typeid, other.typeid
        builder.append srcfeatureId, other.srcfeatureId
        builder.append bin, other.bin
        builder.append cumCount, other.cumCount
        builder.isEquals()
    }

    static mapping = {
        datasource "chado"
        id composite: ["typeid", "srcfeatureId", "bin", "cumCount"]
        version false
    }

    static constraints = {
        typeid maxSize: 1024
//		bin unique: ["srcfeature_id", "typeid"]
        bin unique: ["srcfeatureId", "typeid"]
    }
}
